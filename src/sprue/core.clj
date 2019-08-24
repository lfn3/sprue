(ns sprue.core
  (:import [com.squareup.kotlinpoet TypeSpec FileSpec KModifier FunSpec FunSpec$Builder TypeSpec$Builder PropertySpec]
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

(def sample-config {:template {:package "net.lfn3.sprue"}
                    :classes  [{:name   "Test"
                                :fields [{:name "name"
                                          :type String}]}]})

(defn build [{:keys [template classes] :as config}]
  (->> classes
       (map (partial merge template))
       (map make-data-class)))

(defn str-class [config]
  (let [fs (FileSpec/builder "net.lfn3.sprue" "Sprue")]
    (->> config
         (build)
         (map #(.addType fs %1))
         (dorun))
    (let [sw (StringWriter.)]
      (.writeTo (.build fs) sw)
      (str sw))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (print (str-class sample-config)))
