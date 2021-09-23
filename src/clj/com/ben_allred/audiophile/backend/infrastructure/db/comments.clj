(ns com.ben-allred.audiophile.backend.infrastructure.db.comments
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.comments.protocols :as pc]
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.db.common :as cdb]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private access* [projects user-teams user-id]
  (-> projects
      (models/select* [:and [:= :user-teams.user-id user-id]])
      (models/join user-teams [:= :user-teams.team-id :projects.team-id])))

(defn ^:private access-file-version [projects files file-versions user-teams file-version-id user-id]
  (-> projects
      (access* user-teams user-id)
      (models/join files [:= :files.project-id :projects.id])
      (models/join file-versions [:= :file-versions.file-id :files.id])
      (update :where conj [:= :file-versions.id file-version-id])
      (assoc :select [1])))

(defn ^:private has-file-clause [projects files file-versions user-teams file-id user-id]
  [:exists (-> file-versions
               (models/select* [:and
                                [:= :file-versions.id :comments.file-version-id]
                                [:= :files.id file-id]
                                [:= :user-teams.user-id user-id]])
               (models/join files [:= :files.id :file-versions.file-id])
               (models/join projects [:= :projects.id :files.project-id])
               (models/join user-teams [:= :user-teams.team-id :projects.team-id])
               (assoc :select [1]))])

(deftype CommentsRepoExecutor [executor comments projects files file-versions user-teams users]
  pc/ICommentsExecutor
  (select-for-file [_ file-id opts]
    (repos/execute! executor
                    (models/select* comments (has-file-clause projects
                                                              files
                                                              file-versions
                                                              user-teams
                                                              file-id
                                                              (:user/id opts)))
                    (assoc opts :model-fn (crepos/->model-fn comments))))
  (insert-comment-access? [_ comment opts]
    (cdb/access? executor (access-file-version projects
                                               files
                                               file-versions
                                               user-teams
                                               (:comment/file-version-id comment)
                                               (:user/id opts))))
  (insert-comment! [_ comment _]
    (-> executor
        (repos/execute! (models/insert-into comments comment))
        colls/only!
        :id))
  (find-event-comment [_ comment-id]
    (-> executor
        (repos/execute! (models/select-by-id* comments comment-id)
                        {:model-fn (crepos/->model-fn comments)})
        colls/only!)))

(defn ->comment-executor
  "Factory function for creating [[CommentsRepoExecutor]] which provide access to the comment repository."
  [{:keys [comments files file-versions projects user-teams users]}]
  (fn [executor]
    (->CommentsRepoExecutor executor comments projects files file-versions user-teams users)))

(deftype Executor [executor pubsub]
  pc/ICommentsExecutor
  (select-for-file [_ file-id opts]
    (pc/select-for-file executor file-id opts))
  (insert-comment-access? [_ comment opts]
    (pc/insert-comment-access? executor comment opts))
  (insert-comment! [_ comment opts]
    (pc/insert-comment! executor comment opts))
  (find-event-comment [_ comment-id]
    (pc/find-event-comment executor comment-id))

  pc/ICommentsEventEmitter
  (comment-created! [_ user-id comment ctx]
    (ps/emit-event! pubsub user-id (:comment/id comment) :comment/created comment ctx))

  pint/IEmitter
  (command-failed! [_ model-id opts]
    (ps/command-failed! pubsub model-id opts)))

(defn ->executor
  "Factory function for creating [[Executor]] which aggregates [[CommentsEventEmitter]]
   and [[CommentsRepoExecutor]]."
  [{:keys [->comment-executor pubsub]}]
  (fn [executor]
    (->Executor (->comment-executor executor) pubsub)))
