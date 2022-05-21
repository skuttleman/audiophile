(ns ^:web audiophile.test.web.signup-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.test.web.common.page :as pg]
    [audiophile.test.web.common.selenium :as selenium]))

(deftest signup-test
  (pg/with-driver [driver]
    (testing "when logging in as a new user"
      (pg/visit! driver "/")
      (-> driver
          (pg/wait-by-css! ".login-form .email")
          (selenium/input! "signup@user.com"))
      (-> driver
          (pg/wait-by-css! ".login-form .button.submit")
          selenium/click!)
      (selenium/wait-for! driver
                          (fns/=> (selenium/find-by (selenium/by-css "body"))
                                  selenium/text
                                  (->> (re-find #"Sign up"))))
      (pg/fill-out-form! driver
                         ".signup-form"
                         {".handle"        "signup-handle"
                          ".mobile-number" "9876543210"
                          ".first-name"    "Signup"
                          ".last-name"     "User"})
      (-> driver
          (selenium/wait-for! (fns/=> (selenium/find-by (selenium/by-css ".signup-form .button.submit"))
                                      (->> (filter selenium/enabled?))
                                      colls/only!))
          selenium/click!)
      (testing "views the home page"
        (is (selenium/wait-for! driver
                                (fns/=> (selenium/find-by (selenium/by-css ".team-list"))
                                        selenium/text
                                        (->> (re-find #"My Personal Projects")))))))))
