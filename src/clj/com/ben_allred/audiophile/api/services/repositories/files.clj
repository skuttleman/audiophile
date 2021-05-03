(ns com.ben-allred.audiophile.api.services.repositories.files
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.api.services.repositories.entities.sql :as sql]
    [com.ben-allred.audiophile.api.services.repositories.projects :as projects]
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [integrant.core :as ig]))

(defmacro ^:private with-unexceptional [fut & body]
  `(let [future# (future ~fut)]
     (try (let [result# (do ~@body)]
            @future#
            result#)
          (catch Throwable ex#
            (future-cancel future#)
            (throw ex#)))))

(deftype FileRepository [tx arts-ent file-vers-ent files-ent]
  prepos/ITransact
  (transact! [_ f]
    (repos/transact! tx (fn [executor opts]
                          (f executor (assoc opts
                                             :entity/artifacts arts-ent
                                             :entity/files files-ent
                                             :entity/file-versions file-vers-ent))))))

(defmethod ig/init-key ::repo [_ {:keys [artifacts file-versions files tx]}]
  (->FileRepository tx artifacts file-versions files))

(defn create-artifact
  "Save an artifact to the repository and upload the content to the kv store.
   If write to kv store fails, repository will be rolled back. Otherwise, cleanup TBD"
  [repo artifact user-id]
  (repos/transact! repo (fn [executor {store :store/kv entity :entity/artifacts}]
                          (let [key (str "artifacts/" (uuids/random))]
                            (with-unexceptional (repos/put! store
                                                            key
                                                            (:tempfile artifact)
                                                            {:content-type (:content-type artifact)
                                                             :metadata     {:filename (:filename artifact)}})
                              (let [data (-> artifact
                                             (select-keys #{:content-type :filename})
                                             (assoc :uri (repos/uri store key)
                                                    :content-size (:size artifact)
                                                    :created-by user-id))]
                                {:artifact/id (->> data
                                                   (entities/insert-into entity)
                                                   (repos/execute! executor)
                                                   first
                                                   :id)}))))))

(defn ^:private exists* [project-id user-id]
  [:exists {:select [:id]
            :from   [:projects]
            :join   [:user-teams [:= :projects.team-id :user-teams.team-id]]
            :where  [:and [:= :projects.id project-id]
                     [:= :user-teams.user-id user-id]]}])

(defn ^:private ->query [clause]
  (fn [{files-ent :entity/files}]
    (-> files-ent
        (update :fields (partial filter #{:id :name}))
        (entities/select* clause)
        (update :select into [[:fv.id "version/id"]
                              [:fv.name "version/name"]
                              [:fv.artifact-id "version/artifact-id"]
                              [:version.created-at "version/created-at"]])
        (assoc :join [[{:select   [:file-versions.file-id
                                   [(sql/max :file-versions.created-at) :created-at]]
                        :from     [:file-versions]
                        :group-by [:file-versions.file-id]}
                       :version]
                      [:= :version.file-id :files.id]
                      [:file-versions :fv]
                      [:and
                       [:= :fv.file-id :version.file-id]
                       [:= :fv.created-at :version.created-at]]]))))

(defn ^:private query* [executor opts file-id]
  (-> ((->query [:= :files.id file-id]) opts)
      (->> (repos/execute! executor))
      colls/only!))

(defn query-many
  "Query files for a project"
  [repo project-id user-id]
  (repos/transact! repo repos/->exec! repos/execute! (->query (exists* project-id user-id))))

(defn create-file
  "Save a new file with a version to the repository."
  [repo project-id file user-id]
  (repos/transact! repo (fn [executor {files-ent :entity/files file-vers-ent :entity/file-versions :as opts}]
                          (projects/access! executor project-id user-id)
                          (let [file-id (-> files-ent
                                            (entities/insert-into {:name       (:file/name file)
                                                                   :project-id project-id
                                                                   :idx        {:select [(sql/max :idx)]
                                                                                :from   [:files]
                                                                                :where  [:= :files.project-id project-id]}
                                                                   :created-by user-id})
                                            (->> (repos/execute! executor))
                                            first
                                            :id)]
                            (-> file-vers-ent
                                (entities/insert-into {:file-id     file-id
                                                       :artifact-id (:artifact/id file)
                                                       :name        (:version/name file)
                                                       :created-by  user-id})
                                (->> (repos/execute! executor)))
                            (query* executor opts file-id)))))

(defn create-file-version [repo project-id file-id version user-id]
  (repos/transact! repo (fn [executor {file-vers-ent :entity/file-versions :as opts}]
                          (projects/access! executor project-id user-id)
                          (-> file-vers-ent
                              (entities/insert-into {:artifact-id (:artifact/id version)
                                                     :file-id     file-id
                                                     :name        (:version/name version)
                                                     :created-by  user-id})
                              (->> (repos/execute! executor)))
                          (query* executor opts file-id))))
