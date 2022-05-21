(ns audiophile.common.core.utils.uuids
  #?(:clj
     (:import
       (java.util UUID))))

(def regex #"(?i)[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")

(defn uuid-str?
  "is the string a canonical UUID representation"
  [s]
  (and (string? s)
       (boolean (re-matches regex s))))

(defn ->uuid
  "coerce argument into a UUID. v can be a string or uuid. returns nil when passed nil"
  [v]
  (when v
    (if (uuid? v)
      v
      #?(:clj  (UUID/fromString v)
         :cljs (uuid v)))))

(defn random
  "generate a random UUID"
  []
  #?(:clj  (UUID/randomUUID)
     :cljs (random-uuid)))
