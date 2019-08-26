(ns sprue.core
  (:import [com.squareup.kotlinpoet TypeSpec FileSpec KModifier FunSpec FunSpec$Builder TypeSpec$Builder PropertySpec ClassName]
           (java.io StringWriter))
  (:gen-class))

(defn k-mods [& modifiers] (into-array KModifier modifiers))

(defn add-ctor-param [^FunSpec$Builder ctor-builder {:keys [^String name type] :as field}]
  (.addParameter ctor-builder name type (k-mods)))

(defn build-ctor [fields]
  (let [builder (FunSpec/constructorBuilder)]
    (->> fields (map (partial add-ctor-param builder)) (dorun))
    (.build builder)))

(defn prop-spec [{:keys [^String name type] :as field}]
  (-> (PropertySpec/builder name type (k-mods))
      (.initializer name (into-array []))
      (.build)))

(defn add-property [^TypeSpec$Builder type-builder field]
  (.addProperty type-builder (prop-spec field)))

(defn make-data-class [{:keys [^String name fields]}]
  (let [builder (doto (TypeSpec/classBuilder name)
                  (.primaryConstructor (build-ctor fields))
                  (.addModifiers (k-mods KModifier/DATA)))]
    (->> fields
         (map (partial add-property builder))
         (dorun))
    (.build builder)))

(defn ^ClassName poet-class-name [^String package ^String name & more-name-parts]
  (ClassName. package name (into-array String more-name-parts)))

(defn make-id-class [{:keys [^String name extends]}]
  (let [fields [{:name "id"
                 :type Long/TYPE}]
        [extend-package extends-name] extends
        builder (doto (TypeSpec/classBuilder name)
                  (.primaryConstructor (build-ctor fields))
                  (.superclass (poet-class-name extend-package extends-name))
                  (.addSuperclassConstructorParameter "id" (into-array Object [])))]
    (->> fields
         (map (partial add-property builder))
         (dorun))
    (.build builder)))

(defmulti make-class :type)
(defmethod make-class :data [class-config] (make-data-class class-config))
(defmethod make-class :id [class-config] (make-id-class class-config))

(defn id-name [class-name] (str class-name "Id"))

(defn id-for [{:keys [name package] :as class-config}]
  {:name    (id-name name)
   :type    :id
   :package package})

(defn with-id-classes [classes] (concat classes (map id-for classes)))

(defn ided-class-for [{:keys [name package] :as class-config}]
  {:name   (str "Ided" name)
   :type   :data
   :fields (cons {:name "id"
                  :type (poet-class-name package (id-name name))}
                 (get class-config :fields []))})

(def type-keys #{:id :data})

(defn merge-all-template [templates]
  (->> type-keys
       (map (fn [k] [k (merge (:all templates) (get templates k))]))
       (into {})))

(defn merge-templates [templates class] (merge class (get templates (:type class))))

(def package "net.lfn3.sprue")

(def sample-templates (merge-all-template {:all {:package package}
                                           :id  {:extends ["net.lfn3.sprue" "Id"]}}))

(def sample-class (merge-templates sample-templates
                                   {:name   "Test"
                                    :type   :data
                                    :fields [{:name "name"
                                              :type String}]}))

(def sample-classes (->> [sample-class]
                         (with-id-classes)
                         (concat [(ided-class-for sample-class)])
                         (map (partial merge-templates sample-templates))))

(defn ^FileSpec wrap-in-file [{:keys [package name] :as class-config}]
  (.build (doto (FileSpec/builder package name)
            (.addType (make-class class-config)))))

(defn write-to-str [^FileSpec fs]
  (let [sw (StringWriter.)]
    (.write sw (str "//" (.getName fs) ".kt\n"))
    (.writeTo fs sw)
    (str sw)))

(defn str-classes [classes]
  (->> classes (map wrap-in-file) (map write-to-str) (apply str)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (print (str-classes sample-classes)))
