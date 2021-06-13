(ns com.ben-allred.audiophile.backend.api.repositories.files.core
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.files.protocols :as pf]))

(defn insert-artifact!
  ([accessor artifact]
   (insert-artifact! accessor artifact nil))
  ([accessor artifact opts]
   (pf/insert-artifact! accessor artifact opts)))

(defn find-by-artifact-id
  ([accessor artifact-id]
   (find-by-artifact-id accessor artifact-id nil))
  ([accessor artifact-id opts]
   (pf/find-by-artifact-id accessor artifact-id opts)))

(defn insert-file!
  ([accessor file]
   (insert-file! accessor file nil))
  ([accessor file opts]
   (pf/insert-file! accessor file opts)))

(defn find-by-file-id
  ([accessor file-id]
   (find-by-file-id accessor file-id nil))
  ([accessor file-id opts]
   (pf/find-by-file-id accessor file-id opts)))

(defn select-for-project
  ([accessor project-id]
   (select-for-project accessor project-id nil))
  ([accessor project-id opts]
   (pf/select-for-project accessor project-id opts)))

(defn insert-version!
  ([accessor version]
   (insert-version! accessor version nil))
  ([accessor version opts]
   (pf/insert-version! accessor version opts)))

(defn artifact-created!
  ([accessor user-id artifact]
   (artifact-created! accessor user-id artifact nil))
  ([accessor user-id artifact ctx]
   (pf/artifact-created! accessor user-id artifact ctx)))

(defn file-created!
  ([accessor user-id file]
   (file-created! accessor user-id file nil))
  ([accessor user-id file ctx]
   (pf/file-created! accessor user-id file ctx)))

(defn version-created!
  ([accessor user-id version]
   (version-created! accessor user-id version nil))
  ([accessor user-id version ctx]
   (pf/version-created! accessor user-id version ctx)))

(defn find-event-artifact [accessor artifact-id]
  (pf/find-event-artifact accessor artifact-id))

(defn find-event-file [accessor file-id]
  (pf/find-event-file accessor file-id))

(defn find-event-version [accessor version-id]
  (pf/find-event-version accessor version-id))
