(ns audiophile.backend.api.repositories.comments.queries
  (:require
    [audiophile.backend.api.repositories.common :as crepos]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.infrastructure.db.common :as cdb]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.db.models.tables :as tbl]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]))

(defn ^:private access* [user-id]
  (-> tbl/projects
      (models/select* [:and [:= :user-teams.user-id user-id]])
      (models/join tbl/user-teams [:= :user-teams.team-id :projects.team-id])))

(defn ^:private access-file-version [file-version-id user-id]
  (-> user-id
      access*
      (models/join tbl/files [:= :files.project-id :projects.id])
      (models/join tbl/file-versions [:= :file-versions.file-id :files.id])
      (update :where conj [:= :file-versions.id file-version-id])
      (assoc :select [1])))

(defn ^:private has-file-clause [file-id user-id]
  [:exists (-> tbl/file-versions
               (models/select* [:and
                                [:= :file-versions.id :comments.file-version-id]
                                [:= :files.id file-id]
                                [:= :user-teams.user-id user-id]])
               (models/join tbl/files [:= :files.id :file-versions.file-id])
               (models/join tbl/projects [:= :projects.id :files.project-id])
               (models/join tbl/user-teams [:= :user-teams.team-id :projects.team-id])
               (assoc :select [1]))])

(defn ^:private nest-commenter [row]
  (let [selector (comp #{"commenter"} namespace)
        commenter (maps/select row selector)]
    (-> row
        (maps/select (complement selector))
        (assoc :comment/commenter commenter))))

(defn ^:private select-for-file-query [file-id opts]
  (let [{file-version-id :file-version/id} opts]
    (-> tbl/comments
        (models/select* (when file-version-id
                          [:= :comments.file-version-id file-version-id]))
        (models/and-where [:= :comments.comment-id nil])
        (models/and-where (has-file-clause file-id
                                           (:user/id opts))))))

(defn ^:private select-for-file-join [query]
  (-> query
      (models/join (models/select-fields tbl/user-events #{})
                   [:and
                    [:= :user-events.model-id :comments.id]
                    [:= :user-events.event-type "comment/created"]])
      (models/left-join (models/alias tbl/users :commenter)
                        [:= :user-events.emitted-by :commenter.id])
      (models/order-by [:comments.created-at :desc])))


(defn ^:private select-for-file* [query executor opts]
  (repos/execute! executor
                  query
                  (assoc opts
                         :model-fn (crepos/->model-fn tbl/comments)
                         :result-xform (map nest-commenter))))

(defn select-for-file [executor file-id opts]
  (-> file-id
      (select-for-file-query opts)
      select-for-file-join
      (select-for-file* executor opts)))

(defn insert-comment-access? [executor comment opts]
  (cdb/access? executor (access-file-version (:comment/file-version-id comment)
                                             (:user/id opts))))

(defn insert-comment! [executor comment _]
  (-> executor
      (repos/execute! (models/insert-into tbl/comments comment))
      colls/only!
      :id))

(defn find-event-comment [executor comment-id]
  (-> executor
      (repos/execute! (models/select-by-id* tbl/comments comment-id)
                      {:model-fn (crepos/->model-fn tbl/comments)})
      colls/only!))
