(ns sprue.java-output
  (:require [clojure.spec.alpha :as s]
            [sprue.java-interop-util :as jiu]
            [sprue.util :as util]
            [clojure.string :as str])
  (:import (com.squareup.javapoet TypeSpec TypeSpec$Builder ClassName AnnotationSpec JavaFile FieldSpec CodeBlock FieldSpec$Builder MethodSpec MethodSpec$Builder)
           (javax.lang.model.element Modifier)))

(defn ^ClassName poet-class-name [package name & more-name-parts]
  (ClassName/get package name (jiu/coll-str-arr more-name-parts)))

(defn convert-type [type]
  (if (vector? type)
    (let [[package class] type]
      (poet-class-name package class))
    type))

(defn ^AnnotationSpec generated-annotation []
  (-> (poet-class-name "javax.annotation.processing" "Generated")
      (AnnotationSpec/builder)
      (.addMember "value" "$S" (jiu/str-arr "net.lfn3.sprue.java_output.clj"))
      (.build)))

(defn find-flags [flags name]
  (->> flags (filter (comp (partial = name) :name))))

(defn find-optional-flag [flags name]
  (let [filtered (-> flags (find-flags name))]
    (if (seq (rest filtered))
      (throw (ex-info (str "More than one flag with name " name " found") {:flags flags}))
      (first filtered))))

(defn find-flag [flags name]
  (-> flags (find-flags name) (util/highlander (str "Expected exactly one flag with name " name " in flags"))))

(defn ^TypeSpec$Builder class-builder [name flags]
  (let [builder (TypeSpec/classBuilder name)
        base-class-flag (find-optional-flag flags :base-type)
        implements-flags (find-flags flags :implements)]
    (->> implements-flags
         (map #(.addSuperinterface builder (convert-type (:interface-type %1))))
         (dorun))
    (-> builder
        (.addModifiers (into-array [Modifier/PUBLIC]))
        (.addAnnotation (generated-annotation))
        (cond-> base-class-flag (.superclass (convert-type (:base-type base-class-flag)))))))
(defn is-const-field? [field]
  (->> (:flags field)
       (map :name)
       (filter (partial = :const))
       (seq)
       (nil?)
       (not)))

(defn ^CodeBlock super-call [fields]
  (let [format-str (str "super(" (->> (repeat (count fields) "$N")
                                      (str/join ",$W"))
                        ")")]
    (CodeBlock/of format-str (to-array (map :name fields)))))

(defn add-parameter [^MethodSpec$Builder method-builder {:keys [type name] :as field}]
  (.addParameter method-builder (convert-type type) name (jiu/empty-arr Modifier)))

(defn get-supertype-fields [fields flags]
  (if-let [flag (find-optional-flag flags :base-type)]
    (filter (comp (:super-ctor-fields flag) :name) fields)
    nil))

(defn get-instance-fields [fields flags]
  (let [flag (find-optional-flag flags :base-type)
        super-field-names (:super-ctor-fields flag)]
    (if (seq super-field-names)
      (filter (comp not super-field-names :name) fields)
      fields)))

(defn ^CodeBlock set-field [field]
  (CodeBlock/of "this.$1N = $1N" (jiu/str-arr (:name field))))

(defn add-constructor [class-builder fields flags]
  (let [ctor-builder (-> (MethodSpec/constructorBuilder)
                         (.addModifiers (into-array [Modifier/PUBLIC])))
        super-fields (get-supertype-fields fields flags)
        fields-to-be-set (get-instance-fields fields flags)]
    (->> fields (map (partial add-parameter ctor-builder)) (dorun))
    (when super-fields (.addStatement ctor-builder (super-call fields)))
    (->> fields-to-be-set
         (map set-field)
         (map #(.addStatement ctor-builder %1))
         (dorun))
    (.addMethod class-builder (.build ctor-builder))))

(defn ^CodeBlock string-literal-codeblock [str]
  (CodeBlock/of "$S" (jiu/str-arr str)))

(defn ^FieldSpec$Builder field-spec-builder [name type & modifiers]
  (FieldSpec/builder (convert-type type)
                     name
                     (into-array Modifier modifiers)))

(def jackson-name-annotation-type (convert-type ["com.fasterxml.jackson.annotation" "JsonProperty"]))
(defn ^AnnotationSpec jackson-name-annotation [name]
  (-> (AnnotationSpec/builder jackson-name-annotation-type)
      (.addMember "value" "$L" (jiu/str-arr name))
      (.build)))

(def swagger-api-model-prop-annotation-type (convert-type ["io.swagger.annotations" "ApiModelProperty"]))
(defn ^AnnotationSpec swagger-value-annotation [type]
  (-> (AnnotationSpec/builder swagger-api-model-prop-annotation-type)
      (.addMember "dataType" "$S" (jiu/str-arr (str type)))
      (.build)))

(def ^AnnotationSpec nullable-annotation (-> (convert-type ["javax.annotation" "Nullable"])
                                             (AnnotationSpec/builder)
                                             (.build)))

(defn add-field [class-builder {:keys [name type flags] :as field}]
  (let [initializer-flag (find-optional-flag flags :initializer)
        is-const-field? (is-const-field? field)
        ;TODO: try and remove or clean this up?
        _ (when (and is-const-field? (not initializer-flag))
            (throw (ex-info (str "Field " name " is const but does not have an initializer") {:field field})))
        modifiers (if is-const-field?
                    [Modifier/PUBLIC Modifier/STATIC Modifier/FINAL]
                    [Modifier/PRIVATE Modifier/FINAL])
        field-spec-builder (apply field-spec-builder name type modifiers)
        serialize-field-name-as-flag (find-optional-flag flags :serialize-field-name-as)
        serialize-field-value-as-flag (find-optional-flag flags :serialize-field-value-as)
        optional-flag (find-optional-flag flags :optional)
        field-spec (-> field-spec-builder
                       ;TODO: stop initializer assuming the passed in value is a string
                       (cond-> initializer-flag (.initializer (string-literal-codeblock (:value initializer-flag)))
                               serialize-field-name-as-flag (.addAnnotation (jackson-name-annotation
                                                                              (:value serialize-field-name-as-flag)))
                               serialize-field-value-as-flag (.addAnnotation (swagger-value-annotation
                                                                               (:value serialize-field-value-as-flag)))
                               optional-flag (.addAnnotation nullable-annotation))
                       (.build))]
    (.addField class-builder field-spec)))

(defn add-fields [class-builder fields]
  (->> fields
       (map (partial add-field class-builder))
       (dorun))
  class-builder)

(defn add-equals-lines [builder fields]
  (let [code-str (str (->> fields
                           (map :name)
                           (map #(str "Objects.equals(this." %1 ", that." %1 ")"))
                           (str/join " &&\n\t"))
                      ";\n")]
    (.addCode builder code-str (jiu/str-arr))))

(defn add-equals-method [class-builder name has-superclass? fields]
  ;TODO could be improved if we know a field is a) nullable and b) primitive
  ;TODO special case big decimal
  (when (seq fields)
   (let [method-spec (-> (MethodSpec/methodBuilder "equals")
                         (.returns Boolean/TYPE)
                         (.addModifiers (into-array [Modifier/PUBLIC]))
                         (.addParameter Object "o" (into-array [Modifier/PUBLIC]))
                         (.addStatement "if (o == null || getClass() != o.getClass()) return false" (jiu/str-arr))
                         (.addStatement "if (this == o) return true" (jiu/str-arr))
                         (.addStatement "$1L that = ($1L) o" (jiu/str-arr name))
                         (.addCode "return " (jiu/str-arr))
                         (cond-> has-superclass? (.addCode "super.equals(that) &&\n\t" (jiu/str-arr)))
                         (add-equals-lines fields)
                         (.build))]
     (.addMethod class-builder method-spec)))
  class-builder)

(defn add-hashcode-method [class-builder fields]
  (when (seq fields)
   (let [methodSpec
         (-> (MethodSpec/methodBuilder "hashcode")
             (.returns Integer/TYPE)
             (.addModifiers (into-array [Modifier/PUBLIC]))
             (.addCode (str "return Objects.hash(" (str/join ", " (map :name fields)) ");\n") (jiu/str-arr))
             (.build))]
     (.addMethod class-builder methodSpec)))
  class-builder)

(defn field-to-string-line [field]
  (str "Object.toString(" (:name field) ")"))

(defn add-to-string-method [class-builder name has-superclass? fields]
  (let [method-builder (-> "toString"
                           (MethodSpec/methodBuilder)
                           (.addModifiers (into-array [Modifier/PUBLIC]))
                           (.returns String)
                           (.addCode "return $S" (jiu/str-arr (str name "{"))))]
    (when (seq fields)
     (.addCode method-builder
               (->> fields
                    (map field-to-string-line)
                    (str/join " + \", \" +\n\t")
                    (str " +\n\t"))
               (jiu/str-arr)))
    (.addMethod class-builder (-> method-builder
                                  (.addCode " + $S" (jiu/str-arr "}"))
                                  (cond-> has-superclass? (.addCode " +\n\tsuper.toString()" (jiu/str-arr)))
                                  (.addCode ";\n" (jiu/str-arr))
                                  (.build)))))

(defn getter-for [{:keys [name type] :as field}]
  (-> (str "get" (Character/toUpperCase (first name)) (.substring name 1))
      (MethodSpec/methodBuilder)
      (.addModifiers (into-array [Modifier/PUBLIC]))
      (.returns (convert-type type))
      (.addCode (str "return this." name ";\n") (jiu/str-arr))
      (.build)))

(defn add-getter-method [class-builder field]
  (.addMethod class-builder (getter-for field)))

(defn add-getter-methods [class-builder fields]
  (->> fields
       (map (partial add-getter-method class-builder))
       (dorun))
  class-builder)

(defn ^JavaFile render-to-javafile [{:keys [name namespace fields flags]}]
  (let [non-const-fields (filter (comp not is-const-field?) fields)
        const-fields (filter is-const-field? fields)
        instance-fields (get-instance-fields non-const-fields flags)
        has-superclass? (find-optional-flag flags :base-type)
        class-def (-> (class-builder name flags)
                      (add-fields const-fields)
                      (add-fields instance-fields)
                      (add-constructor non-const-fields flags)
                      (add-getter-methods instance-fields)
                      (add-equals-method name
                                         has-superclass?
                                         instance-fields)
                      (add-hashcode-method instance-fields)
                      (add-to-string-method name has-superclass? instance-fields)
                      (.build))]
    (-> (str namespace \. name)
        (JavaFile/builder class-def)
        (.build))))

(s/fdef render-to-javafile
        :args (s/cat :type :sprue.input.normalized/type)
        :ret (partial instance? JavaFile))