(ns audiophile.test.integration.team-invitations-test
  (:require
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.test.integration.common :as int]
    [audiophile.test.integration.common.http :as ihttp]
    [audiophile.test.utils.assertions :as assert]
    [clojure.test :refer [are deftest is testing]]))

(deftest fetch-all-invitations-test
  (testing "GET /api/team-invitations"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))]
        (testing "when authenticated as a user with invitations"
          (let [user (int/lookup-user system "collin@blueprint.com")
                team-id (:team/id (int/lookup-team system "Collaborative Team Seed"))
                invited-by (:user/id (int/lookup-user system "joe@example.com"))
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :routes.api/team-invitations)
                             handler)]
            (testing "returns invitations"
              (is (http/success? response))
              (assert/has? {:team/id    team-id
                            :team/name  "Collaborative Team Seed"
                            :inviter/id invited-by}
                           (get-in response [:body :data])))))

        (testing "when authenticated as a user with no invitations"
          (let [user {:user/id (uuids/random)}
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :routes.api/team-invitations)
                             handler)]
            (testing "returns no invitations"
              (is (http/success? response))
              (is (empty? (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :routes.api/team-invitations)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest invitation-flow-test
  (testing "/api/team-invitations"
    (int/with-config [system [:api/handler]]
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde serde/transit))
            team-id (:team/id (int/lookup-team system "Collaborative Team Seed"))]
        (testing "when authenticated"
          (let [user (int/lookup-user system "joe@example.com")]
            (testing "and when creating an invitation for a known email"
              (let [response (-> {:team/id    team-id
                                  :user/email "another@user.com"}
                                 ihttp/body-data
                                 (ihttp/login system user)
                                 (ihttp/post system :routes.api/team-invitations)
                                 (ihttp/as-async system handler))]
                (testing "stores the invitation"
                  (is (http/success? response))
                  (let [result (-> {}
                                   (ihttp/login system (int/lookup-user system "another@user.com"))
                                   (ihttp/get system :routes.api/team-invitations)
                                   handler)]
                    (assert/has? {:team/id    team-id
                                  :inviter/id (:user/id user)}
                                 (get-in result [:body :data]))))))

            (testing "and when responding to an invitation"
              (let [status (rand-nth [:ACCEPTED :REJECTED])
                    response (-> {:team-invitation/team-id team-id
                                  :team-invitation/status  status}
                                 ihttp/body-data
                                 (ihttp/login system (int/lookup-user system "another@user.com"))
                                 (ihttp/patch system :routes.api/team-invitations)
                                 (ihttp/as-async system handler))]
                (testing "stores the response"
                  (is (http/success? response)))))

            (testing "and when creating an invitation for an unknown email"
              (let [response (-> {:team/id    team-id
                                  :user/email "junkville@jones.net"}
                                 ihttp/body-data
                                 (ihttp/login system user)
                                 (ihttp/post system :routes.api/team-invitations)
                                 (ihttp/as-async system handler))]
                (testing "stores the invitation"
                  (is (http/success? response)))))))

        (testing "when user doesn't have access to team"
          (let [user (int/lookup-user system "collin@blueprint.com")]
            (testing "and when creating an invitation"
              (let [response (-> {:team/id    team-id
                                  :user/email "some@bloke.org"}
                                 ihttp/body-data
                                 (ihttp/login system user)
                                 (ihttp/post system :routes.api/team-invitations)
                                 (ihttp/as-async system handler))]
                (testing "returns an error"
                  (is (http/client-error? response)))))))

        (testing "when not authenticated"
          (testing "and when creating an invitation"
            (let [response (-> {:team/id    team-id
                                :user/email "some@bloke.org"}
                               ihttp/body-data
                               (ihttp/post system :routes.api/team-invitations)
                               (ihttp/as-async system handler))]
              (testing "returns an error"
                (is (http/client-error? response))))))))))