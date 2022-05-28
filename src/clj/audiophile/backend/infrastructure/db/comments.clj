(ns audiophile.backend.infrastructure.db.comments
  (:require
    [audiophile.backend.api.repositories.comments.protocols :as pc]
    [audiophile.backend.api.repositories.common :as crepos]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.infrastructure.db.common :as cdb]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]))

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

(defn ^:private nest-commenter [row]
  (let [selector (comp #{"commenter"} namespace)
        commenter (maps/select row selector)]
    (-> row
        (maps/select (complement selector))
        (assoc :comment/commenter commenter))))

(deftype CommentsRepoExecutor [executor comments projects files file-versions user-events user-teams users]
  pc/ICommentsExecutor
  (select-for-file [_ file-id {file-version-id :file-version/id :as opts}]
    (repos/execute! executor
                    (-> comments
                        (models/select* (when file-version-id
                                          [:= :comments.file-version-id file-version-id]))
                        (models/and-where [:= :comments.comment-id nil])
                        (models/and-where (has-file-clause projects
                                                           files
                                                           file-versions
                                                           user-teams
                                                           file-id
                                                           (:user/id opts)))
                        (models/join (models/select-fields user-events #{})
                                     [:and
                                      [:= :user-events.model-id :comments.id]
                                      [:= :user-events.event-type "comment/created"]])
                        (models/left-join (models/alias users :commenter)
                                          [:= :user-events.emitted-by :commenter.id])
                        (models/order-by [:comments.created-at :desc]))
                    (assoc opts
                           :model-fn (crepos/->model-fn comments)
                           :result-xform (map nest-commenter))))
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
  [{:keys [comments files file-versions projects user-events user-teams users]}]
  (fn [executor]
    (->CommentsRepoExecutor executor comments projects files file-versions user-events user-teams users)))
