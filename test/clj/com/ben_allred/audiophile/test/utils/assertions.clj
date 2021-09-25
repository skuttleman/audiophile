(ns com.ben-allred.audiophile.test.utils.assertions)

(defn ^:private assert* [m val f]
  (for [[k v] m]
    `(if (fn? ~v)
       ~(f (list v `(get ~val ~k)))
       ~(f (list '= v `(get ~val ~k))))))

(defmacro is? [m val]
  (list* `do (assert* m val #(list 'is %))))

(defmacro has? [m vals]
  (let [_val (gensym "val")]
    (list 'is `(loop [[~_val :as vals#] ~vals]
                 (if (empty? vals#)
                   false
                   (or (and ~@(assert* m _val identity))
                       (recur (rest vals#))))))))
