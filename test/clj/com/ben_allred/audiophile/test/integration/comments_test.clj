(ns ^:integration com.ben-allred.audiophile.test.integration.comments-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.test.integration.common :as int]
    [com.ben-allred.audiophile.test.integration.common.http :as ihttp]
    [com.ben-allred.audiophile.test.utils.assertions :as assert]))

(deftest fetch-all-comments-test
  (testing "GET /api/files/:file-id/comments"
    (int/with-config [system [:api/handler]] {:db/enabled? true}
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde system :serdes/edn))]
        (testing "when authenticated as a user with comments"
          (let [user (int/lookup-user system "joe@example.com")
                file-id (:file/id (int/lookup-file system "File Seed"))
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/file.comments {:params {:file/id file-id}})
                             handler)
                comments (get-in response [:body :data])]
            (testing "returns comments"
              (is (http/success? response))
              (assert/has? {:comment/body      "Comment Seed 1"
                            :comment/selection [0.123 3.21]}
                           comments)
              (assert/has? {:comment/body "Comment Seed 2"}
                           comments))))

        (testing "when authenticated as a user with no comments"
          (let [user {:user/id (uuids/random)}
                file-id (:file/id (int/lookup-file system "File Seed"))
                response (-> {}
                             (ihttp/login system user)
                             (ihttp/get system :api/file.comments {:params {:file/id file-id}})
                             handler)]
            (testing "returns no comments"
              (is (http/success? response))
              (is (empty? (get-in response [:body :data]))))))

        (testing "when not authenticated"
          (let [response (-> {}
                             (ihttp/get system :api/file.comments {:params {:file/id (uuids/random)}})
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))

(deftest create-comments-test
  (testing "POST /api/comments"
    (int/with-config [system [:api/handler]] {:db/enabled? true}
      (let [handler (-> system
                        (int/component :api/handler)
                        (ihttp/with-serde system :serdes/edn))
            file-id (:file/id (int/lookup-file system "File Seed"))
            file-version-id (:file-version/id (int/lookup-file-version system "File Version Seed"))]
        (testing "when authenticated"
          (let [user (int/lookup-user system "joe@example.com")
                response (-> {:comment/selection       [0.123 0.456]
                              :comment/body            "comment body"
                              :comment/file-version-id file-version-id}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system :api/comments)
                             (ihttp/as-async system handler))]
            (testing "creates the comment"
              (is (http/success? response))
              (assert/is? {:comment/file-version-id file-version-id
                           :comment/body            "comment body"
                           :comment/id              uuid?}
                          (get-in response [:body :data])))

            (testing "and when querying for comments"
              (let [response (-> {}
                                 (ihttp/login system user)
                                 (ihttp/get system :api/file.comments {:params {:file/id file-id}})
                                 handler)]
                (testing "includes the new comment"
                  (assert/has? {:comment/file-version-id file-version-id
                                :comment/body            "comment body"
                                :comment/id              uuid?}
                               (get-in response [:body :data])))))))

        (testing "when authenticated as a user without file access"
          (let [user {:user/id (uuids/random)}
                response (-> {:comment/selection       [0.123 0.456]
                              :comment/body            "comment body"
                              :comment/file-version-id file-version-id}
                             ihttp/body-data
                             (ihttp/login system user)
                             (ihttp/post system :api/comments)
                             (ihttp/as-async system handler))]
            (testing "fails"
              (is (http/client-error? response)))))

        (testing "when not authenticated"
          (let [response (-> {:comment/name    "comment name"
                              :comment/team-id file-version-id}
                             ihttp/body-data
                             (ihttp/post system :api/comments)
                             handler)]
            (testing "returns an error"
              (is (http/client-error? response)))))))))
