(ns ^:unit com.ben-allred.audiophile.common.services.navigation.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]))

(deftest serialize*-test
  (testing "serialize*"
    (let [routes ["" [[["/test/" [uuids/regex :project-id] "/route"] :test/route]]]
          id (uuids/random)]
      (testing "serializes params to url"
        (is (= (str "test://base/test/" id "/route?query&foo=bar")
               (nav/serialize* {:test "test://base"}
                               routes
                               :test/route
                               {:route-params {:project-id id}
                                :query-params {:query true
                                               :foo   :bar}})))))))

(deftest deserialize*-test
  (testing "deserialize*"
    (let [routes ["" [[["/test/" [uuids/regex :project-id] "/route"] :test/route]]]
          id (uuids/random)]
      (testing "deserializes url to params"
        (is (= {:handle       :test/route
                :route-params {:project-id id}
                :query-params {:query true
                               :foo   "bar"}
                :path         (str "/test/" id "/route")
                :query-string "query&foo=bar"}
               (nav/deserialize* routes (str "/test/" id "/route?query&foo=bar"))))))))
