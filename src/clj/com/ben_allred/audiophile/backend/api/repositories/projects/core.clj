(ns com.ben-allred.audiophile.backend.api.repositories.projects.core
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.projects.protocols :as pp]))

(defn find-by-project-id
  ([accessor project-id]
   (find-by-project-id accessor project-id nil))
  ([accessor project-id opts]
   (pp/find-by-project-id accessor project-id opts)))

(defn select-for-user
  ([accessor user-id]
   (select-for-user accessor user-id nil))
  ([accessor user-id opts]
   (pp/select-for-user accessor user-id opts)))

(defn insert-project!
  ([accessor project]
   (insert-project! accessor project nil))
  ([accessor project opts]
   (pp/insert-project! accessor project opts)))

(defn find-event-project [accessor project-id]
  (pp/find-event-project accessor project-id))

(defn project-created!
  ([accessor user-id project]
   (project-created! accessor user-id project nil))
  ([accessor user-id project ctx]
   (pp/project-created! accessor user-id project ctx)))
