(ns ^:web com.ben-allred.audiophile.test.web.signup-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.test.utils.selenium :as selenium]
    [com.ben-allred.audiophile.test.web :as web]))

(deftest signup-test
  (web/with-driver [driver]
    (testing "when logging in as a new user"
      (web/visit! driver "/")
      (-> driver
          (web/wait-by-css! ".login-form .email")
          (selenium/input! "signup@user.com"))
      (-> driver
          (web/wait-by-css! ".login-form .button.submit")
          selenium/click!)
      (selenium/wait-for! driver
                          (fns/=> (selenium/find-by (selenium/by-css "body"))
                                  selenium/text
                                  (->> (re-find #"Sign up"))))
      (web/fill-out-form! driver
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
