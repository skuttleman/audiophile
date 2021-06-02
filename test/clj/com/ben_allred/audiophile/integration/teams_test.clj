(ns ^:integration com.ben-allred.audiophile.integration.teams-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.resources.http :as http]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.integration.common :as int]
    [com.ben-allred.audiophile.integration.common.http :as ihttp]
    [test.utils.assertions :as assert]))

(deftest fetch-all-teams-test
  (testing "GET /api/teams"
    (int/with-config [system [:api/handler#api]] {:db/enabled? true}
      (let [handler (-> system
                        (int/component :api/handler#api)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when authenticated as a user with teams"
          (let [user (int/lookup-user system "joe@example.com")
                team-id (:team/id (int/lookup-team system "Team Seed"))
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/teams)
                             handler)]
            (testing "returns teams"
              (is (http/success? response))
              (assert/is? {:team/id   team-id
                           :team/type :PERSONAL
                           :team/name "Team Seed"}
                          (-> response
                              (get-in [:body :data])
                              colls/only!)))))

        (testing "when authenticated as a user with no teams"
          (let [user {:user/id (uuids/random)}
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/teams)
                             handler)]
            (testing "returns no teams"
              (is (http/success? response))
              (is (empty? (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :api/teams)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest fetch-team-test
  (testing "GET /api/teams/:team-id"
    (int/with-config [system [:api/handler#api]] {:db/enabled? true}
      (let [team-id (:team/id (int/lookup-team system "Team Seed"))
            handler (-> system
                        (int/component :api/handler#api)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when authenticated as a user with teams"
          (let [user (int/lookup-user system "joe@example.com")
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/team {:route-params {:team-id team-id}})
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
                             (ihttp/get system :api/team {:route-params {:team-id team-id}})
                             handler)]
            (testing "returns no teams"
              (is (http/client-error? response)))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :api/team {:route-params {:team-id team-id}})
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest create-teams-test
  (testing "POST /api/teams"
    (int/with-config [system [:api/handler#api]] {:db/enabled? true}
      (let [handler (-> system
                        (int/component :api/handler#api)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when authenticated"
          (let [user (int/lookup-user system "joe@example.com")
                response (-> {:team/name "team name"
                              :team/type :COLLABORATIVE}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system :api/teams)
                             handler)]
            (testing "creates the team"
              (is (http/success? response))
              (assert/is? {:team/name "team name"
                           :team/id   uuid?}
                          (get-in response [:body :data])))

            (testing "and when querying for teams"
              (let [response (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system :api/teams)
                                 handler)]
                (testing "includes the new team"
                  (assert/has? {:team/name "team name"
                                :team/id   uuid?}
                               (get-in response [:body :data])))))))

        (testing "when authenticated as a user with no teams"
          (let [user {:user/id (uuids/random)}
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/teams)
                             handler)]
            (testing "returns no teams"
              (is (http/success? response))
              (is (empty? (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :api/teams)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))
