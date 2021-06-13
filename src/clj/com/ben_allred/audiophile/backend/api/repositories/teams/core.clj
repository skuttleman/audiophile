(ns com.ben-allred.audiophile.backend.api.repositories.teams.core
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.teams.protocols :as pt]))

(defn find-by-team-id
  ([accessor team-id]
   (find-by-team-id accessor team-id nil))
  ([accessor team-id opts]
   (pt/find-by-team-id accessor team-id opts)))

(defn select-team-members
  ([accessor team-id]
   (select-team-members accessor team-id nil))
  ([accessor team-id opts]
   (pt/select-team-members accessor team-id opts)))

(defn select-for-user
  ([accessor user-id]
   (select-for-user accessor user-id nil))
  ([accessor user-id opts]
   (pt/select-for-user accessor user-id opts)))

(defn insert-team!
  ([accessor team]
   (insert-team! accessor team nil))
  ([accessor team opts]
   (pt/insert-team! accessor team opts)))

(defn find-event-team [accessor team-id]
  (pt/find-event-team accessor team-id))

(defn team-created!
  ([accessor user-id team]
   (team-created! accessor user-id team nil))
  ([accessor user-id team ctx]
   (pt/team-created! accessor user-id team ctx)))
