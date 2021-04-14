(ns com.ben-allred.audiophile.common.services.serdes.protocols)

(defprotocol ISerde
  (mime-type [_])
  (serialize [this value opts])
  (deserialize [this value opts]))
