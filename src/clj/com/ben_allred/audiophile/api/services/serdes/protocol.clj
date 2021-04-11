(ns com.ben-allred.audiophile.api.services.serdes.protocol)

(defprotocol ISerde
  (serialize [this value opts])
  (deserialize [this value opts]))
