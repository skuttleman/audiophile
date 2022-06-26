(ns ^:integration audiophile.test.integration.projects-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.test.integration.common :as int]
    [audiophile.test.integration.common.http :as ihttp]
    [audiophile.test.utils.assertions :as assert]))

(deftest fetch-all-projects-test
  (testing "GET /api/projects"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated as a user with projects"
          (let [user (int/lookup-user system "joe@example.com")
                project-id (:project/id (int/lookup-project system "Project Seed"))
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :routes.api/projects)
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
                             (ihttp/get system :routes.api/projects)
                             handler)]
            (testing "returns no projects"
              (is (http/success? response))
              (is (empty? (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :routes.api/projects)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest fetch-project-test
  (testing "GET /api/projects/:project-id"
    (int/with-config [system [:api/handler]]
      (let [project-id (:project/id (int/lookup-project system "Project Seed"))
            handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated as a user with projects"
          (let [user (int/lookup-user system "joe@example.com")
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :routes.api/projects:id {:params {:project/id project-id}})
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
                             (ihttp/get system :routes.api/projects:id {:params {:project/id project-id}})
                             handler)]
            (testing "returns no projects"
              (is (http/client-error? response)))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :routes.api/projects:id {:params {:project/id project-id}})
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest create-projects-test
  (testing "POST /api/projects"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))
            team-id (:team/id (int/lookup-team system "Team Seed"))]
        (testing "when authenticated"
          (let [user (int/lookup-user system "joe@example.com")
                response (-> {:project/name    "project name"
                              :project/team-id team-id}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system :routes.api/projects)
                             (ihttp/as-async system handler))]
            (testing "creates the project"
              (is (http/success? response))
              (assert/is? {:project/id uuid?}
                          (get-in response [:body :data])))

            (testing "and when querying for projects"
              (let [response (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system :routes.api/projects)
                                 handler)]
                (testing "includes the new project"
                  (assert/has? {:project/team-id team-id
                                :project/name    "project name"
                                :project/id      uuid?}
                               (get-in response [:body :data])))))))

        (testing "when authenticated as a user without team access"
          (let [user {:user/id (uuids/random)}
                response (-> {:project/name    "project name"
                              :project/team-id team-id}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system :routes.api/projects)
                             (ihttp/as-async system handler))]
            (testing "fails"
              (is (http/client-error? response)))))

        (testing "when not authenticated"
          (let [response (-> {:project/name    "project name"
                              :project/team-id team-id}
                             ihttp/body-data
                             (ihttp/post system :routes.api/projects)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))
