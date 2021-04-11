(ns com.ben-allred.audiophile.api.services.serdes.protocol)

(defprotocol ISerde
  (mime-type [_])
  (serialize [this value opts])
  (deserialize [this value opts]))
