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

(defn make-id-class [{:keys [^String name extends]}]
  (let [fields [{:name "id"
                 :type Long/TYPE}]
        [extend-package extends-name] extends
        builder (doto (TypeSpec/classBuilder name)
                  (.primaryConstructor (build-ctor fields))
                  (.superclass (ClassName. extend-package extends-name (into-array String [])))
                  (.addSuperclassConstructorParameter "id" (into-array Object [])))]
    (->> fields
         (map (partial add-property builder))
         (dorun))
    (.build builder)))

(defmulti make-class :type)
(defmethod make-class :data [class-config] (make-data-class class-config))
(defmethod make-class :id [class-config] (make-id-class class-config))

(defn id-for [{:keys [name package] :as class-config}]
  {:name    (str name "Id")
   :type    :id
   :package package})

(defn with-ids [classes]
  (concat classes (map id-for classes)))

(def type-keys #{:id :data})

(defn merge-all-template [templates]
  (->> type-keys
       (map (fn [k] [k (merge (:all templates) (get templates k))]))
       (into {})))

(defn merge-templates [templates classes]
  (->> classes
       (map #(merge %1 (get templates (:type %1))))))

(def sample-templates (merge-all-template {:all {:package "net.lfn3.sprue"}
                                           :id  {:extends ["net.lfn3.sprue" "Id"]}}))

(def sample-classes (->> [{:name   "Test"
                           :type   :data
                           :fields [{:name "name"
                                     :type String}]}]
                         (merge-templates sample-templates)
                         (with-ids)
                         (merge-templates sample-templates)))

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
