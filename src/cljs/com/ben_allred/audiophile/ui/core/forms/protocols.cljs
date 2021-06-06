(ns com.ben-allred.audiophile.ui.core.forms.protocols)

(defprotocol IInit
  "Can initialize or re-initialize itself to an internally defined immutable value."
  (init! [this] [this value]
         "When no value is supplied, resets itself back to its internal initial state.
          When a value is supplied, resets itself such that the supplied value is
          the new initial state."))

(defprotocol IAttempt
  "Tracks whether the form has been attempted"
  (attempt! [this] "Mark the entire entity as \"attempted\".")
  (attempted? [this] "Has the entity been \"attempted\"?")
  (attempting? [this] "Is the entity currently being \"attempted\"."))

(defprotocol IChange
  "Can change the value of itself at a leaf node."
  (change! [this path value] "Update the path of data into the entity.")
  (changed? [this] [this path] "Has the entity or a specific leaf node at a path been \"changed\"?"))

(defprotocol ITrack
  "Like `[[IChange]], but it \"touches\" a leaf value at a path."
  (touch! [this] [this path] "Mark the entire entity or a leaf node at a path as \"touched\".")
  (touched? [this] [this path] "Has the entity or a specific leaf node at a path been \"touched\"?"))

(defprotocol IValidate
  "Apply an associated validator fn to the internal model and return the results"
  (errors [this]
          "Apply the validator fn to the current model of the form and return any errors
           - should return a data structure congruent with the nesting of the model
           - should return nil when no errors are present"))

(defprotocol ILinkRoute
  "Links to navigation updates"
  (update-qp! [this f]
              "Takes a function that takes a map of current query params and returns updated query params"))
