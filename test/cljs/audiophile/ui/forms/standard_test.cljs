(ns audiophile.ui.forms.standard-test
  (:require
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.store.impl :as istore]
    [clojure.test :refer [are deftest is testing]]
    audiophile.ui.store.async
    audiophile.ui.store.mutations))

(deftest ^:unit standard-form-test
  (testing "StandardForm"
    (let [store (istore/create nil)]
      (testing "when using a form without a validator"
        (let [*form (form.std/create store {:bar {:quux "init"}})]
          (testing "has no error"
            (is (nil? (forms/errors *form))))

          (testing "tracks touch state"
            (is (not (forms/touched? *form)))
            (is (not (forms/touched? *form [:bar :quux]))))

          (testing "and when changing the form"
            (forms/change! *form [:foo] "foo")
            (forms/change! *form [:bar :baz] "bar/baz")
            (forms/touch! *form [:none])

            (testing "has data"
              (is (= {:foo "foo"
                      :bar {:baz  "bar/baz"
                            :quux "init"}}
                     @*form)))

            (testing "has no error"
              (is (nil? (forms/errors *form))))

            (testing "tracks touch state"
              (is (forms/touched? *form))
              (is (forms/touched? *form [:none]))
              (is (not (forms/touched? *form [:foo])))
              (is (not (forms/touched? *form [:bar :baz])))
              (is (not (forms/touched? *form [:bar :quux]))))

            (testing "and when initializing the form"
              (forms/init! *form {:new :value})

              (testing "has data"
                (is (= {:new :value}
                       @*form)))

              (testing "tracks touch state"
                (is (not (forms/touched? *form)))
                (is (not (forms/touched? *form [:new])))
                (is (not (forms/touched? *form [:none])))
                (is (not (forms/touched? *form [:bar :baz])))
                (is (not (forms/touched? *form [:foo])))
                (is (not (forms/touched? *form [:bar :quux]))))))))

      (testing "when using a form with a validator"
        (let [validator (fn [{:keys [a]}]
                          (when (not= :value a)
                            ["a must = value"]))
              form (form.std/create store {:a :value} validator)]
          (testing "and when the validator returns no errors"
            (testing "the form has no errors"
              (is (nil? (forms/errors form)))))

          (testing "and when the validator returns errors"
            (forms/change! form [:a] :not-value)

            (testing "the form has errors"
              (is (= ["a must = value"]
                     (forms/errors form))))))))))
