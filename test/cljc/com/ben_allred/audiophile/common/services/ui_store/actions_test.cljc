(ns ^:unit com.ben-allred.audiophile.common.services.ui-store.actions-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.services.ui-store.protocols :as pui-store]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [test.utils :refer [async] :as tu]
    [test.utils.stubs :as stubs])
  #?(:clj
     (:import
       (java.util Date))))

(defn ^:private stub-store []
  (stubs/create (reify
                  pui-store/IStore
                  (dispatch! [_ _])
                  (get-state [_]))))

(deftest remove-toast!-test
  (let [store (stub-store)]
    (async done
      (async/go
        (testing "removes the toast id"
          (let [before (.getTime #?(:cljs (js/Date.) :default (Date.)))
                _ (tu/<ch! (ui-store/dispatch! store (actions/remove-toast! ::id 100)))
                after (.getTime #?(:cljs (js/Date.) :default (Date.)))
                [hide remove] (colls/only! 2 (stubs/calls store :dispatch!))]
            (is (> (- after before) 100))
            (is (= [:toasts/hide! {:id ::id}] (colls/only! hide)))
            (is (= [:toasts/remove! {:id ::id}] (colls/only! remove)))))

        (done)))))

(deftest toast!-test
  (let [store (stub-store)]
    (async done
      (async/go
        (testing "creates a toast"
          (ui-store/dispatch! store (actions/toast! ::level ::body 100 50))

          (let [add (colls/only! (colls/only! (stubs/calls store :dispatch!)))
                {:keys [id body]} (get add 1)]
            (is (= [:toasts/add! {:level ::level}]
                   (update add 1 select-keys #{:level})))
            (stubs/init! store)
            @body
            (tu/<ch! (async/timeout 300))
            (let [[display hide remove] (colls/only! 3 (stubs/calls store :dispatch!))]
              (is (= [:toasts/display! {:id id}] (colls/only! display)))
              (is (= [:toasts/hide! {:id id}] (colls/only! hide)))
              (is (= [:toasts/remove! {:id id}] (colls/only! remove))))))

        (done)))))

(deftest banner!-test
  (let [store (stub-store)]
    (testing "creates a banner"
      (ui-store/dispatch! store (actions/banner! ::level ::body))

      (let [add (colls/only! (colls/only! (stubs/calls store :dispatch!)))
            {:keys [id]} (get add 1)]
        (is (= [:banners/add! {:id id :level ::level :body ::body}]
               add))))))

(deftest remove-modal!-test
  (let [store (stub-store)]
    (testing "removes a modal"
      (async done
        (async/go
          (tu/<ch! (ui-store/dispatch! store (actions/remove-modal! ::id)))
          (let [[hide remove] (colls/only! 2 (stubs/calls store :dispatch!))]
            (is (= [:modals/hide! ::id]
                   (colls/only! hide)))
            (is (= [:modals/remove! ::id]
                   (colls/only! remove))))

          (done))))))

(deftest remove-modal-all!-test
  (let [store (stub-store)]
    (testing "removes all modals"
      (async done
        (async/go
          (tu/<ch! (ui-store/dispatch! store actions/remove-modal-all!))
          (let [[hide remove] (colls/only! 2 (stubs/calls store :dispatch!))]
            (is (= [:modals/hide-all!]
                   (colls/only! hide)))
            (is (= [:modals/remove-all!]
                   (colls/only! remove))))

          (done))))))

(deftest modal!-test
  (let [store (stub-store)]
    (testing "creates a modal"
      (async done
        (async/go
          (ui-store/dispatch! store (actions/modal! ::header ::body ::buttons))
          (tu/<ch! (async/timeout 50))
          (let [[add display] (colls/only! 2 (stubs/calls store :dispatch!))
                id (second (first add))]
            (is (= [:modals/add! id {:header  ::header
                                     :body    [::body]
                                     :buttons ::buttons}]
                   (colls/only! add)))
            (is (= [:modals/display! id] (colls/only! display))))

          (done))))))
