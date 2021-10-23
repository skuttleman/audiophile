(ns ^:unit com.ben-allred.audiophile.ui.infrastructure.store.reducers-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.ui.infrastructure.store.reducers :as reducers]))

(deftest route-test
  (testing "initializes to nil"
    (is (nil? (reducers/route))))

  (testing "updates on :router/updated"
    (is (= ::route (reducers/route ::state [:router/updated ::route])))))

(deftest toasts-test
  (testing "initializes to an empty map"
    (is (= {} (reducers/toasts))))

  (testing "updates on :toasts/add!"
    (is (= {:a 1 ::id {:state :init :level ::level :body ::body}}
           (reducers/toasts {:a 1} [:toasts/add! {:id ::id :level ::level :body ::body}]))))

  (testing "updates on :toasts/display!"
    (is (= {::id {:state :showing :level ::level :body ::body}}
           (reducers/toasts {::id {:state :init :level ::level :body ::body}}
                            [:toasts/display! {:id ::id}]))))

  (testing "updates on :toasts/hide!"
    (is (= {::id {:state :removing :level ::level :body ::body}}
           (reducers/toasts {::id {:state :init :level ::level :body ::body}}
                            [:toasts/hide! {:id ::id}]))))

  (testing "updates on :toasts/remove!"
    (is (= {}
           (reducers/toasts {::id {:state :init :level ::level :body ::body}}
                            [:toasts/remove! {:id ::id}])))))

(deftest banners-test
  (testing "initializes to an empty map"
    (is (= {} (reducers/banners))))

  (testing "updates on :banners/add!"
    (is (= {:a 1 ::id {:level ::level :body ::body :key ::key}}
           (reducers/banners {:a 1} [:banners/add! {:id ::id :level ::level :body ::body :key ::key}]))))

  (testing "when the banner key exists"
    (is (= {:a 1 ::id-1 {:level ::level :body ::body :key ::key}}
           (-> {:a 1}
               (reducers/banners [:banners/add! {:id ::id-1 :level ::level :body ::body :key ::key}])
               (reducers/banners [:banners/add! {:id ::id-2 :level ::level :body ::body :key ::key}])))))

  (testing "updates on :banners/hide!"
    (is (= {}
           (reducers/banners {::id {:level ::level :body ::body}}
                             [:banners/remove! {:id ::id}])))))

(deftest modals-test
  (testing "initializes to an empty map"
    (is (= {} (reducers/modals))))

  (testing "updates on :modals/add!"
    (is (= {:a 1 ::id {:state :init :level ::level :body ::body}}
           (reducers/modals {:a 1} [:modals/add! ::id {:level ::level :body ::body}]))))

  (testing "updates on :modals/display!"
    (is (= {::id {:state :showing :level ::level :body ::body}}
           (reducers/modals {::id {:state :init :level ::level :body ::body}}
                            [:modals/display! ::id]))))

  (testing "updates on :modals/hide!"
    (is (= {::id {:state :removing :level ::level :body ::body}}
           (reducers/modals {::id {:state :init :level ::level :body ::body}}
                            [:modals/hide! ::id]))))

  (testing "updates on :modals/hide-all!"
    (is (= {::id1 {:state :removing :level ::level :body ::body}
            ::id2 {:state :removing :level ::level :body ::body}}
           (reducers/modals {::id1 {:state :init :level ::level :body ::body}
                             ::id2 {:state :showing :level ::level :body ::body}}
                            [:modals/hide-all!]))))

  (testing "updates on :modals/remove!"
    (is (= {::id2 {:state :removing :level ::level :body ::body}}
           (reducers/modals {::id1 {:state :init :level ::level :body ::body}
                             ::id2 {:state :removing :level ::level :body ::body}}
                            [:modals/remove! ::id1]))))

  (testing "updates on :modals/remove!"
    (is (= {}
           (reducers/modals {::id1 {:state :init :level ::level :body ::body}
                             ::id2 {:state :removing :level ::level :body ::body}}
                            [:modals/remove-all!])))))
