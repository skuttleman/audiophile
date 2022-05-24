(ns audiophile.test.web.common.page
  (:require
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.core :as u]
    [audiophile.test.web.common.selenium :as selenium]
    [integrant.core :as ig])
  (:import
    (org.apache.commons.io.output NullWriter)))

(def ^:dynamic *system*)

(defmacro with-web [test-id run! & body]
  `(if (= ~test-id :web)
     (binding [*system* (binding [*out* NullWriter/NULL_WRITER]
                          (~run!))]
       (try ~@body
            (finally
              (binding [*out* NullWriter/NULL_WRITER]
                (u/silent!
                  (ig/halt! *system*))))))
     (do ~@body)))

(defmacro with-driver [[sym] & body]
  `(let [~sym (binding [*out* NullWriter/NULL_WRITER]
                (selenium/create-driver nil))]
     (try ~@body
          (finally
            (binding [*out* NullWriter/NULL_WRITER]
              (u/silent!
                (selenium/close! ~sym)))))))

(defn visit! [driver path]
  (selenium/visit! driver (str (get *system* [:duct/const :env/base-url#ui]) path)))

(defn wait-by-css! [driver selector]
  (selenium/wait-for! driver (fn [ctx]
                               (first (selenium/find-by ctx (selenium/by-css selector))))))

(defn fill-out-form!
  ([driver m]
   (fill-out-form! driver "body" m))
  ([driver selector m]
   (let [form (wait-by-css! driver selector)]
     (doseq [[selector value] m]
       (-> form
           (selenium/find-by (selenium/by-css selector))
           colls/only!
           (selenium/input! value))))))
