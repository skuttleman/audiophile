(ns com.ben-allred.audiophile.common.core.utils.dates
  #?(:clj
     (:import
       (javax.xml.bind DatatypeConverter))))

(defn parse [s]
  #?(:clj  (.getTime (DatatypeConverter/parseDateTime s))
     :cljs (js/Date. s)))
