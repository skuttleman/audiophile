(ns ^:integration audiophile.test.integration.teams-test
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

(deftest fetch-all-teams-test
  (testing "GET /api/teams"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated as a user with teams"
          (let [user (int/lookup-user system "joe@example.com")
                team-id (:team/id (int/lookup-team system "Team Seed"))
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :routes.api/teams)
                             handler)]
            (testing "returns teams"
              (is (http/success? response))
              (assert/has? {:team/id   team-id
                            :team/type :PERSONAL
                            :team/name "Team Seed"}
                           (get-in response [:body :data])))))

        (testing "when authenticated as a user with no teams"
          (let [user {:user/id (uuids/random)}
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :routes.api/teams)
                             handler)]
            (testing "returns no teams"
              (is (http/success? response))
              (is (empty? (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :routes.api/teams)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest fetch-team-test
  (testing "GET /api/teams/:team-id"
    (int/with-config [system [:api/handler]]
      (let [team-id (:team/id (int/lookup-team system "Team Seed"))
            handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated as a user with teams"
          (let [user (int/lookup-user system "joe@example.com")
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :routes.api/teams:id {:params {:team/id team-id}})
                             handler)]
            (testing "returns teams"
              (is (http/success? response))
              (assert/is? {:team/id   team-id
                           :team/name "Team Seed"}
                          (get-in response [:body :data])))))

        (testing "when authenticated as a user with no teams"
          (let [user {:user/id (uuids/random)}
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :routes.api/teams:id {:params {:team/id team-id}})
                             handler)]
            (testing "returns no teams"
              (is (http/client-error? response)))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :routes.api/teams:id {:params {:team/id team-id}})
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest create-teams-test
  (testing "POST /api/teams"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated"
          (let [user (int/lookup-user system "joe@example.com")
                response (-> {:team/name "team name"
                              :team/type :COLLABORATIVE}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system :routes.api/teams)
                             (ihttp/as-async system handler))]
            (testing "creates the team"
              (is (http/success? response))
              (assert/is? {:team/id uuid?}
                          (get-in response [:body :data])))

            (testing "and when querying for teams"
              (let [response (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system :routes.api/teams)
                                 handler)]
                (testing "includes the new team"
                  (assert/has? {:team/name "team name"
                                :team/id   uuid?}
                               (get-in response [:body :data])))))))

        (testing "when not authenticated"
          (let [response (-> {:team/name "team name"
                              :team/type :COLLABORATIVE}
                             ihttp/body-data
                             (ihttp/post system :routes.api/teams)
                             (ihttp/as-async system handler))]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest update-teams-test
  (testing "PATCH /api/teams/:team-id"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))
            team-id (:team/id (int/lookup-team system "Team Seed"))]
        (testing "when authenticated"
          (let [user (int/lookup-user system "joe@example.com")
                response (-> {:team/name "new team name"}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/patch system
                                          :routes.api/teams:id
                                          {:params {:team/id team-id}})
                             (ihttp/as-async system handler))]
            (testing "updates the team"
              (is (http/success? response)))

            (testing "and when querying for team"
              (let [response (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system
                                            :routes.api/teams:id
                                            {:params {:team/id team-id}})
                                 handler)]
                (testing "includes the new team"
                  (assert/is? {:team/id   team-id
                               :team/name "new team name"}
                              (get-in response [:body :data])))))))

        (testing "when authenticated as a user with no team access"
          (let [user {:user/id (uuids/random)}
                response (-> {:team/name "team name"}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/patch system
                                          :routes.api/teams:id
                                          {:params {:team/id team-id}})
                             (ihttp/as-async system handler))]
            (testing "returns an error"
              (is (http/client-error? response)))))

        (testing "when not authenticated"
          (let [response (-> {:team/name "team name"}
                             ihttp/body-data
                             (ihttp/patch system
                                          :routes.api/teams:id
                                          {:params {:team/id team-id}})
                             (ihttp/as-async system handler))]
            (testing "returns an error"
              (is (http/client-error? response)))))))))
