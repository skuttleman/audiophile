(ns ^:integration audiophile.test.integration.file-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer [are deftest is testing]]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.serdes.protocols :as pserdes]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.test.integration.common :as int]
    [audiophile.test.integration.common.http :as ihttp]
    [audiophile.test.utils.assertions :as assert]))

(deftest upload-artifact-test
  (testing "POST /api/artifacts"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated"
          (let [user (int/lookup-user system "joe@example.com")
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/upload (io/resource "empty.mp3"))
                             (ihttp/post system :routes.api/artifact)
                             (ihttp/as-async system handler))]
            (testing "returns a the artifact details"
              (is (http/success? response)))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/upload (io/resource "empty.mp3"))
                             (ihttp/post system :routes.api/artifact)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest fetch-all-files-test
  (testing "GET /api/projects/:project-id/files"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated as a user with files"
          (let [user (int/lookup-user system "joe@example.com")
                project-id (:project/id (int/lookup-project system "Project Seed"))
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system
                                        :routes.api/projects:id.files
                                        {:params {:project/id project-id}})
                             handler)]
            (testing "returns files for the project"
              (is (http/success? response))
              (assert/is? {:file/name       "File Seed"
                           :file/project-id project-id
                           :version/name    "File Version Seed"}
                          (-> response
                              (get-in [:body :data])
                              colls/only!))))

          (testing "and when requesting a project with no files"
            (let [user (int/lookup-user system "joe@example.com")
                  response (-> {}
                               (ihttp/login system user)
                               (ihttp/get system
                                          :routes.api/projects:id.files
                                          {:params {:project/id (uuids/random)}})
                               handler)]
              (testing "returns no files"
                (is (http/success? response))
                (is (empty? (get-in response [:body :data])))))))

        (testing "when authenticated as a user with no files"
          (let [user {:user/id (uuids/random)}
                project-id (:project/id (int/lookup-project system "Project Seed"))
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system
                                        :routes.api/projects:id.files
                                        {:params {:project/id project-id}})
                             handler)]
            (testing "returns no files"
              (is (http/success? response))
              (is (empty? (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [project-id (:project/id (int/lookup-project system "Project Seed"))
                response (-> {}
                             (ihttp/get system
                                        :routes.api/projects:id.files
                                        {:params {:project/id project-id}})
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest create-files-test
  (testing "POST /api/projects/:project-id/files"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated as a user with a project"
          (let [user (int/lookup-user system "joe@example.com")
                project-id (:project/id (int/lookup-project system "Project Seed"))
                artifact-id (:artifact/id (int/lookup-artifact system "example.mp3"))
                response (-> {:file/name    "file name"
                              :version/name "version name"
                              :artifact/id  artifact-id}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system
                                         :routes.api/projects:id.files
                                         {:params {:project/id project-id}})
                             (ihttp/as-async system handler))]
            (testing "creates the file"
              (is (http/success? response))
              (assert/is? {:file/id         uuid?
                           :file-version/id uuid?}
                          (get-in response [:body :data])))

            (testing "and when querying for project files"
              (let [response (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system
                                            :routes.api/projects:id.files
                                            {:params {:project/id project-id}})
                                 handler)]
                (testing "includes the new file"
                  (assert/has? {:file/name       "file name"
                                :file/project-id project-id
                                :version/name    "version name"}
                               (get-in response [:body :data])))))))

        (testing "when authenticated as a user with no projects"
          (let [user {:user/id (uuids/random)}
                project-id (:project/id (int/lookup-project system "Project Seed"))
                artifact-id (:artifact/id (int/lookup-artifact system "example.mp3"))
                response (-> {:file/name    "file name"
                              :version/name "version name"
                              :artifact/id  artifact-id}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system
                                         :routes.api/projects:id.files
                                         {:params {:project/id project-id}})
                             (ihttp/as-async system handler))]
            (testing "returns an error"
              (is (http/client-error? response)))))

        (testing "when not authenticated"
          (let [project-id (:project/id (int/lookup-project system "Project Seed"))
                artifact-id (:artifact/id (int/lookup-artifact system "example.mp3"))
                response (-> {:file/name    "file name"
                              :version/name "version name"
                              :artifact/id  artifact-id}
                             ihttp/body-data
                             (ihttp/post system
                                         :routes.api/projects:id.files
                                         {:params {:project/id project-id}})
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest create-file-versions-test
  (testing "POST /api/files/:file-id"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated as a user with a project"
          (let [user (int/lookup-user system "joe@example.com")
                project-id (:project/id (int/lookup-project system "Project Seed"))
                artifact-id (:artifact/id (int/lookup-artifact system "example.mp3"))
                file-id (:file/id (int/lookup-file system "File Seed"))
                response (-> {:version/name "version name"
                              :artifact/id  artifact-id}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system
                                         :routes.api/files:id
                                         {:params {:file/id file-id}})
                             (ihttp/as-async system handler))]
            (testing "creates the file version"
              (is (http/success? response))
              (assert/is? {:file-version/id uuid?}
                          (get-in response [:body :data])))

            (testing "and when querying for the file"
              (let [response (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system
                                            :routes.api/files:id
                                            {:params {:file/id file-id}})
                                 handler)
                    {versions :file/versions :as data} (get-in response [:body :data])]
                (testing "includes the new file"
                  (assert/is? {:file/name       "File Seed"
                               :file/project-id project-id}
                              data)
                  (assert/has? {:file-version/name "version name"
                                :file-version/id   uuid?}
                               versions)
                  (assert/has? {:file-version/name "File Version Seed"
                                :file-version/id   uuid?}
                               versions))))

            (testing "and when querying for project files"
              (let [response (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system
                                            :routes.api/projects:id.files
                                            {:params {:project/id project-id}})
                                 handler)]
                (testing "includes the new file"
                  (assert/has? {:file/name       "File Seed"
                                :file/project-id project-id
                                :version/name    "version name"}
                               (get-in response [:body :data])))))))

        (testing "when authenticated as a user with no projects"
          (let [user {:user/id (uuids/random)}
                file-id (:file/id (int/lookup-file system "File Seed"))
                artifact-id (:artifact/id (int/lookup-artifact system "empty.mp3"))
                response (-> {:version/name "version name"
                              :artifact/id  artifact-id}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system
                                         :routes.api/files:id
                                         {:params {:file/id file-id}})
                             (ihttp/as-async system handler))]
            (testing "returns an error"
              (is (http/client-error? response)))))

        (testing "when not authenticated"
          (let [file-id (:file/id (int/lookup-file system "File Seed"))
                artifact-id (:artifact/id (int/lookup-artifact system "example.mp3"))
                response (-> {:version/name "version name"
                              :artifact/id  artifact-id}
                             ihttp/body-data
                             (ihttp/post system
                                         :routes.api/files:id
                                         {:params {:file/id file-id}})
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest artifact-test
  (int/with-config [system [:api/handler]]
    (let [serde (reify
                  pserdes/ISerde
                  (serialize [_ val opts]
                    (serdes/serialize serde/transit val opts))
                  (deserialize [_ val _]
                    val)

                  pserdes/IMime
                  (mime-type [_]
                    (serdes/mime-type serde/transit)))
          handler (-> system
                      (int/component :api/handler)
                      (ihttp/with-serde serde/transit))
          artifact-handler (-> system
                               (int/component :api/handler)
                               (ihttp/with-serde serde))]
      (testing "when authenticated"
        (let [user (int/lookup-user system "joe@example.com")]
          (testing "and when uploading the artifact"
            (let [response (-> "empty.mp3"
                               ihttp/file-upload
                               (ihttp/login system user)
                               (ihttp/post system :routes.api/artifact)
                               (ihttp/as-async system handler))]
              (testing "succeeds"
                (is (http/success? response))
                (let [{artifact-id :artifact/id filename :artifact/filename} (get-in response [:body :data])]
                  (is (uuid? artifact-id))
                  (is (= "empty.mp3" filename))

                  (testing "cannot access the artifact"
                    (let [response (-> {}
                                       (ihttp/login system user)
                                       (ihttp/get system
                                                  :routes.api/artifacts:id
                                                  {:params {:artifact/id artifact-id}})
                                       handler)]
                      (is (http/client-error? response))))

                  (testing "and when creating a file"
                    (let [project-id (:project/id (int/lookup-project system "Project Seed"))
                          response (-> {:file/name    "file name"
                                        :version/name "version name"
                                        :artifact/id  artifact-id}
                                       ihttp/body-data
                                       (ihttp/login system user)
                                       (ihttp/post system
                                                   :routes.api/projects:id.files
                                                   {:params {:project/id project-id}})
                                       (ihttp/as-async system handler))]
                      (is (http/success? response))

                      (testing "can access the artifact"
                        (let [response (-> {}
                                           (ihttp/login system user)
                                           (ihttp/get system
                                                      :routes.api/artifacts:id
                                                      {:params {:artifact/id artifact-id}})
                                           artifact-handler)]
                          (is (http/success? response))
                          (is (= (slurp (io/resource "empty.mp3"))
                                 (slurp (:body response)))))))

                    (testing "and when authenticated as a user with no artifacts"
                      (let [user {:user/id (uuids/random)}
                            response (-> {}
                                         (ihttp/login system user)
                                         (ihttp/get system
                                                    :routes.api/artifacts:id
                                                    {:params {:artifact/id artifact-id}})
                                         artifact-handler)]
                        (testing "returns an error"
                          (is (http/client-error? response)))))

                    (testing "and when not authenticated"
                      (let [response (-> {}
                                         (ihttp/get system
                                                    :routes.api/artifacts:id
                                                    {:params {:artifact/id artifact-id}})
                                         artifact-handler)]
                        (testing "returns an error"
                          (is (http/client-error? response)))))))))))))))
