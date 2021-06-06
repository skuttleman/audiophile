(ns ^:unit com.ben-allred.audiophile.ui.core.forms.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.core.forms.protocols :as pforms]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]))

(deftest with-attrs-test
  (testing "with-attrs"
    (let [touch! (atom nil)
          *form (reify
                  pforms/ITrack
                  (touched? [_ path]
                    [::touched path])
                  (touch! [_ path]
                    (reset! touch! [::touch! path]))

                  pforms/IValidate
                  (errors [_]
                    {:some {:path ::errors}})

                  IDeref
                  (-deref [_]
                    {:some {:path ::value}})

                  pforms/IChange
                  (change! [_ _ value]
                    [::changed value])

                  pforms/IAttempt
                  (attempted? [_]
                    ::attempted)
                  (attempting? [_]
                    ::attempting))]
      (testing "updates form attrs"
        (let [[{:keys [on-blur on-change]} attrs] (-> {:some     :attrs
                                                       :disabled ::disabled}
                                                      (forms/with-attrs *form [:some :path])
                                                      (maps/extract-keys #{:on-blur :on-change}))]
          (is (= {:some       :attrs
                  :visited?   [::touched [:some :path]]
                  :disabled   ::disabled
                  :value      ::value
                  :errors     ::errors
                  :attempted? ::attempted}
                 attrs))
          (on-blur :value)
          (is (= [::touch! [:some :path]] @touch!))
          (is (= [::changed :value] (on-change :value))))))))
