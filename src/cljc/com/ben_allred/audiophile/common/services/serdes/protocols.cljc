(ns com.ben-allred.audiophile.common.services.serdes.protocols)

(defprotocol ISerde
  (mime-type [_])
  (serialize [this value opts])
  (deserialize [this value opts]))

(extend-protocol ISerde
  nil
  (mime-type [_]
    nil)
  (serialize [_ value _]
    value)
  (deserialize [_ value _]
    value))
