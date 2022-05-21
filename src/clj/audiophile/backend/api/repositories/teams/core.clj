(ns audiophile.backend.api.repositories.teams.core
  (:require
    [audiophile.backend.api.repositories.teams.protocols :as pt]))

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

(defn insert-team-access?
  ([accessor team]
   (insert-team-access? accessor team nil))
  ([accessor team opts]
   (pt/insert-team-access? accessor team opts)))

(defn insert-team!
  ([accessor team]
   (insert-team! accessor team nil))
  ([accessor team opts]
   (pt/insert-team! accessor team opts)))

(defn find-event-team [accessor team-id]
  (pt/find-event-team accessor team-id))
