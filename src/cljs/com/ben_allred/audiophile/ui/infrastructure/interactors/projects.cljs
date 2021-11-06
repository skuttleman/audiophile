(ns com.ben-allred.audiophile.ui.infrastructure.interactors.projects
  (:require
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.domain.validations.core :as val]
    [com.ben-allred.audiophile.common.domain.validations.specs :as specs]
    [com.ben-allred.audiophile.ui.api.forms.standard :as form]
    [com.ben-allred.audiophile.ui.api.forms.submittable :as form.sub]
    [com.ben-allred.audiophile.ui.api.views.protocols :as vp]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defmethod form.sub/internal->remote ::file
  [_ data]
  (merge data (:artifact/details data)))

(defn ^:private with-artifact [form]
  (cond-> form
    (and (vector? form)
         (= :artifact/id (first form)))
    (->> (conj [:map])
         (conj [:artifact/details]))))

(def ^:private validator
  (val/validator {:spec specs/project:create}))

(def ^:private file-validator
  (val/validator {:spec (with-meta (colls/postwalk with-artifact specs/file:create)
                                   {:missing-keys {:file/name        "track name is required"
                                                   :version/name     "version name is required"
                                                   :artifact/details "file is required"}})}))

(def ^:private version-validator
  (val/validator {:spec (with-meta (colls/postwalk with-artifact specs/version:create)
                                   {:missing-keys {:version/name     "version name is required"
                                                   :artifact/details "file is required"}})}))

(deftype ProjectsViewInteractor [*file *file-version *files *projects nav ->project-cb]
  vp/IProjectsViewInteractor
  (project-form [_ options]
    (form.sub/create *projects (form/create {:project/team-id (ffirst options)}
                                            validator)))
  (on-project-created [_ cb]
    (fn [vow]
      (v/peek vow (->project-cb cb) nil)))

  vp/IFilesViewInteractor
  (file-form [_ project-id]
    (form.sub/create ::file
                     *file
                     (form/create nil file-validator)
                     {:nav/params {:params {:project/id project-id}}}))
  (on-file-created [_ project-id cb]
    (fn [vow]
      (v/peek vow
              (fn [e]
                (res/request! *files {:nav/params {:params {:project/id project-id}}})
                (when cb (cb e)))
              nil)))

  vp/IVersionViewInteractor
  (version-form [_ project-id file-id]
    (form.sub/create ::file
                     *file-version
                     (form/create nil version-validator)
                     {:nav/params {:params {:file/id    file-id
                                            :project/id project-id}}}))
  (on-version-created [_ project-id cb]
    (fn [vow]
      (v/peek vow
              (fn [_]
                (res/request! *files {:nav/params {:params {:project/id project-id}}})
                (when cb (cb nil)))
              nil))))

(defn interactor [{:keys [*file *file-version *files *projects nav ->project-cb]}]
  (->ProjectsViewInteractor *file *file-version *files *projects nav ->project-cb))
