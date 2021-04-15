(ns com.ben-allred.audiophile.common.services.forms.protocols)

(defprotocol IChange
  (init! [this value])
  (update! [this path value]))

(defprotocol ITrack
  (visit! [this] [this path])
  (visited? [this] [this path])
  (changed? [this path]))

(defprotocol IValidate
  (errors [this]))
