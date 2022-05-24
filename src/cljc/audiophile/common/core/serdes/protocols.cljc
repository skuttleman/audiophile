(ns audiophile.common.core.serdes.protocols)

(defprotocol ISerde
  "Utility for converting internally represented data into a type compatible with a boundary."
  (serialize [this value opts]
    "Serialize the data")
  (deserialize [this value opts]
    "Deserialize the data"))

(defprotocol IMime
  "A serialized type's associated mime-type, when applicable."
  (mime-type [_] "The mime-type"))
