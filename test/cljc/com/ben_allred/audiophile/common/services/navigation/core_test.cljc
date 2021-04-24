(ns ^:unit com.ben-allred.audiophile.common.services.navigation.core-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]))

(defmethod nav/params->internal :test/route
  [params]
  (-> params
      (update-in [:route-params :param] uuids/->uuid)
      (update-in [:query-params :foo] keyword)))

(defmethod nav/internal->params :test/route
  [params]
  (update-in params [:route-params :param] str))

(deftest serialize*-test
  (testing "serialize*"
    (let [routes ["" [[["/test/" [uuids/regex :param] "/route"] :test/route]]]
          param (uuids/random)]
      (testing "serializes params to url"
        (is (= (str "/test/" param "/route?query&foo=bar")
               (nav/serialize* routes :test/route {:route-params {:param param}
                                                   :query-params {:query true
                                                                  :foo   :bar}})))))))

(deftest deserialize*-test
  (testing "deserialize*"
    (let [routes ["" [[["/test/" [uuids/regex :param] "/route"] :test/route]]]
          param (uuids/random)]
      (testing "deserializes url to params"
        (is (= {:handler      :test/route
                :route-params {:param param}
                :query-params {:query true
                               :foo   :bar}
                :path         (str "/test/" param "/route")
                :query-string "query&foo=bar"}
               (nav/deserialize* routes (str "/test/" param "/route?query&foo=bar"))))))))
