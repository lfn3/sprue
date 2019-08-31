(ns sprue.java-interop-util)

(defn ^"[Ljava.lang.String;" coll-str-arr [strs] (into-array String strs))
(defn ^"[Ljava.lang.String;" str-arr [& strs] (coll-str-arr strs))

