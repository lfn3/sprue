(ns sprue.util)

(defn highlander
  ([coll] (highlander coll "Expected exactly one element in coll"))
  ([coll err-msg]
   (if-let [ret (and (not (seq (rest coll)))
                     (first coll))]
     ret
     (throw (ex-info err-msg {:coll coll})))))
