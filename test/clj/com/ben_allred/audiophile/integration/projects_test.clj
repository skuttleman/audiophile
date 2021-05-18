(ns ^:integration com.ben-allred.audiophile.integration.projects-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [com.ben-allred.audiophile.integration.common :as int]
    [com.ben-allred.audiophile.integration.common.http :as ihttp]
    [test.utils.assertions :as assert]))

(deftest fetch-all-projects-test
  (testing "GET /api/projects"
    (int/with-config [system [:api/handler#api]] {:db/enabled? true}
      (let [handler (-> system
                        (int/component :api/handler#api)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when authenticated as a user with projects"
          (let [user (int/lookup-user system "joe@example.com")
                project-id (:project/id (int/lookup-project system "Project Seed"))
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/projects)
                             handler)]
            (testing "returns projects"
              (is (http/success? response))
              (assert/is? {:project/id   project-id
                           :project/name "Project Seed"}
                          (-> response
                              (get-in [:body :data])
                              colls/only!)))))

        (testing "when authenticated as a user with no projects"
          (let [user {:user/id (uuids/random)}
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/projects)
                             handler)]
            (testing "returns no projects"
              (is (http/success? response))
              (is (empty? (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :api/projects)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest fetch-project-test
  (testing "GET /api/projects/:project-id"
    (int/with-config [system [:api/handler#api]] {:db/enabled? true}
      (let [project-id (:project/id (int/lookup-project system "Project Seed"))
            handler (-> system
                        (int/component :api/handler#api)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when authenticated as a user with projects"
          (let [user (int/lookup-user system "joe@example.com")
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/project {:route-params {:project-id project-id}})
                             handler)]
            (testing "returns projects"
              (is (http/success? response))
              (assert/is? {:project/id   project-id
                           :project/name "Project Seed"}
                          (get-in response [:body :data])))))

        (testing "when authenticated as a user with no projects"
          (let [user {:user/id (uuids/random)}
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/project {:route-params {:project-id project-id}})
                             handler)]
            (testing "returns no projects"
              (is (http/client-error? response)))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :api/project {:route-params {:project-id project-id}})
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest create-projects-test
  (testing "POST /api/projects"
    (int/with-config [system [:api/handler#api]] {:db/enabled? true}
      (let [handler (-> system
                        (int/component :api/handler#api)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when authenticated"
          (let [user (int/lookup-user system "joe@example.com")
                team-id (:team/id (int/lookup-team system "Team Seed"))
                response (-> {:project/name    "project name"
                              :project/team-id team-id}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system :api/projects)
                             handler)]
            (testing "creates the project"
              (is (http/success? response))
              (assert/is? {:project/team-id team-id
                           :project/name    "project name"
                           :project/id      uuid?}
                          (get-in response [:body :data])))

            (testing "and when querying for projects"
              (let [response (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system :api/projects)
                                 handler)]
                (testing "includes the new project"
                  (assert/has? {:project/team-id team-id
                                :project/name    "project name"
                                :project/id      uuid?}
                               (get-in response [:body :data])))))))

        (testing "when authenticated as a user with no projects"
          (let [user {:user/id (uuids/random)}
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/projects)
                             handler)]
            (testing "returns no projects"
              (is (http/success? response))
              (is (empty? (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :api/projects)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))
