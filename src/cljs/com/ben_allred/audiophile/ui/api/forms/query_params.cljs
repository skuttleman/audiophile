(ns com.ben-allred.audiophile.ui.api.forms.query-params
  (:require
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.core.forms.protocols :as pforms]))

(defn ^:private qp-updater [qp path value]
  (let [pre-path (butlast path)
        k (last path)
        updater (if (some? value)
                  (fn [m] (assoc m k value))
                  (fn [m] (dissoc m k)))]
    (case (count path)
      0 value
      1 (updater qp)
      (update-in qp pre-path updater))))

(deftype QueryParamsLinkedForm [*form *qp]
  pforms/IInit
  (init! [_ value]
    (pforms/init! *form value)
    (forms/update-qp! *qp empty))

  pforms/IAttempt
  (attempt! [_]
    (pforms/attempt! *form))
  (attempted? [_]
    (pforms/attempted? *form))
  (attempting? [_]
    (pforms/attempting? *form))

  pforms/IChange
  (change! [_ path value]
    (forms/update-qp! *qp qp-updater path value)
    (pforms/change! *form path value))
  (changed? [_]
    (pforms/changed? *form))
  (changed? [_ path]
    (pforms/changed? *form path))

  pforms/ITrack
  (touch! [_]
    (pforms/touch! *form))
  (touch! [_ path]
    (pforms/touch! *form path))
  (touched? [_]
    (pforms/touched? *form))
  (touched? [_ path]
    (pforms/touched? *form path))

  pforms/IValidate
  (errors [_]
    (pforms/errors *form))

  IDeref
  (-deref [_]
    @*form))

(defn create
  "Creates a form by combining a form and [[ILinkRoute]] to keep query-params in sync with the form state."
  [*form *qp]
  (->QueryParamsLinkedForm *form *qp))
