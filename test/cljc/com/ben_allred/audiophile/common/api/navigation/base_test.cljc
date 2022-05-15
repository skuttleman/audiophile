(ns ^:unit com.ben-allred.audiophile.common.api.navigation.base-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.api.navigation.base :as bnav]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(def ^:private routes
  [""
   [[["/test/" [uuids/regex :project/id] "/route"] :test/route]]])

(deftest router-test
  (let [router (bnav/->Router {:test "test://base"} routes)
        id (uuids/random)]
    (testing "#path-for"
      (testing "serializes params to url"
        (is (= (str "test://base/test/" id "/route?query&foo=bar")
               (nav/path-for router
                             :test/route
                             {:params {:project/id id
                                       :query      true
                                       :foo        :bar}})))))

    (testing "#match-route"
      (testing "deserializes url to params"
        (is (= {:handle       :test/route
                :params       {:project/id id
                               :query      true
                               :foo        "bar"}
                :path         (str "/test/" id "/route")
                :query-string "query&foo=bar"}
               (nav/match-route router
                                (str "/test/" id "/route?query&foo=bar"))))))))
