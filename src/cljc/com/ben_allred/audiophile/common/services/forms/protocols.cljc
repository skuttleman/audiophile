(ns com.ben-allred.audiophile.common.services.forms.protocols)

(defprotocol IChange
  (init! [this] [this value] "reset the form to the specified value or the value used to create the form (when no reset value is supplied)")
  (update! [this path value] "update the path of data into the form"))

(defprotocol ITrack
  (visit! [this] [this path] "mark the form or a specific path into the form as visited")
  (visited? [this] [this path] "has the form or a path into the form been visited?"))

(defprotocol IValidate
  (errors [this] "apply the validator fn to the current model of the form and return any errors
                  - should return a data structure congruent with the model
                  - should return nil when no errors are present"))
