(ns ^:unit com.ben-allred.audiophile.common.services.forms.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.common.services.forms.protocols :as pforms]
    [com.ben-allred.audiophile.common.utils.maps :as maps])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(deftest with-attrs-test
  (testing "with-attrs"
    (let [touch! (atom nil)
          form (reify
                 pforms/ITrack
                 (touched? [_ path]
                   [::touched path])
                 (touch! [_ path]
                   (reset! touch! [::touch! path]))

                 pforms/IValidate
                 (errors [_]
                   {:some {:path ::errors}})

                 IDeref
                 (#?(:cljs -deref :default deref) [_]
                   {:some {:path ::value}})

                 pforms/IChange
                 (change! [_ _ value]
                   [::changed value]))]
      (testing "updates form attrs"
        (let [[{:keys [on-blur on-change]} attrs] (-> {:some     :attrs
                                                       :disabled ::disabled}
                                                      (forms/with-attrs form [:some :path])
                                                      (maps/extract-keys #{:on-blur :on-change}))]
          (is (= {:some     :attrs
                  :visited? [::touched [:some :path]]
                  :disabled ::disabled
                  :value    ::value
                  :errors   ::errors}
                 attrs))
          (on-blur :value)
          (is (= [::touch! [:some :path]] @touch!))
          (is (= [::changed :value] (on-change :value))))))))
