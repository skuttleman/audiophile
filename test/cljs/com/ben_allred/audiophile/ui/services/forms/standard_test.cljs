(ns com.ben-allred.audiophile.ui.services.forms.standard-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.ui.services.forms.standard :as forms.std]))

(deftest ^:unit standard-form-test
  (testing "StandardForm"
    (testing "when using a form without a validator"
      (let [form (forms.std/create {:bar {:quux "init"}})]
        (testing "has no error"
          (is (nil? (forms/errors form))))

        (testing "tracks visitation"
          (is (not (forms/visited? form)))
          (is (not (forms/visited? form [:bar :quux]))))

        (testing "and when updating the form"
          (forms/update! form [:foo] "foo")
          (forms/update! form [:bar :baz] "bar/baz")
          (forms/visit! form [:none])

          (testing "has data"
            (is (= {:foo "foo"
                    :bar {:baz  "bar/baz"
                          :quux "init"}}
                   @form)))

          (testing "has no error"
            (is (nil? (forms/errors form))))

          (testing "tracks visitation"
            (is (forms/visited? form))
            (is (forms/visited? form [:none]))
            (is (forms/visited? form [:bar :baz]))
            (is (forms/visited? form [:foo]))
            (is (not (forms/visited? form [:bar :quux]))))

          (testing "and when initializing the form"
            (forms/init! form {:new :value})

            (testing "has data"
              (is (= {:new :value}
                     @form)))

            (testing "tracks visitation"
              (is (not (forms/visited? form)))
              (is (not (forms/visited? form [:new])))
              (is (not (forms/visited? form [:none])))
              (is (not (forms/visited? form [:bar :baz])))
              (is (not (forms/visited? form [:foo])))
              (is (not (forms/visited? form [:bar :quux]))))))))


    (testing "when using a form with a validator"
      (let [validator (fn [{:keys [a]}]
                        (when (not= :value a)
                          ["a must = value"]))
            form (forms.std/create {:a :value} validator)]
        (testing "and when the validator returns no errors"
          (testing "the form has no errors"
            (is (nil? (forms/errors form)))))

        (testing "and when the validator returns errors"
          (forms/update! form [:a] :not-value)

          (testing "the form has errors"
            (is (= ["a must = value"]
                   (forms/errors form)))))))))
