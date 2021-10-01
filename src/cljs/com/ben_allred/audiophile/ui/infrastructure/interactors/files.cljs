(ns com.ben-allred.audiophile.ui.infrastructure.interactors.files
  (:require
    [com.ben-allred.audiophile.ui.api.views.protocols :as vp]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.api.forms.standard :as form]
    [com.ben-allred.audiophile.ui.api.forms.query-params :as form.qp]
    [com.ben-allred.audiophile.ui.api.forms.submittable :as form.sub]
    [com.ben-allred.vow.core :as v]))

(deftype FilesViewInteractor [*comment *qp]
  vp/ICommentsViewInteractor
  (comment-form [_ file-id file-version-id]
    (let [val {:comment/file-version-id file-version-id
               :comment/with-selection? true
               :comment/selection       [0 0]
               :file/id                 file-id}]
      (form.sub/create *comment
                       (form/create val (constantly nil))
                       {:form/reset-to val})))
  (on-comment-created [_ cb]
    (fn [vow]
      (v/peek vow cb nil)))

  vp/IQueryParamsViewInteractor
  (qp-form [_ val]
    (form.qp/create (form/create val)
                    *qp))
  (update-qp! [_ val]
    (forms/update-qp! *qp merge val)))

(defn interactor [{:keys [*comment *qp]}]
  (->FilesViewInteractor *comment *qp))
