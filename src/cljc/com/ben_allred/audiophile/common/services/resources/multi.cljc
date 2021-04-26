(ns com.ben-allred.audiophile.common.services.resources.multi
  (:require
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]
    [integrant.core :as ig])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(deftype MultiResource [resources]
  pres/IResource
  (request! [_ opts]
    (v/all (maps/map-vals #(pres/request! % opts)
                          resources)))
  (status [_]
    (let [statuses (into #{} (map (comp pres/status val)) resources)]
      (cond
        (contains? statuses :init) :init
        (contains? statuses :error) :error
        (contains? statuses :requesting) :requesting
        :else :success)))

  pv/IPromise
  (then [_ on-success on-error]
    (-> resources
        v/all
        (v/then on-success on-error)))

  IDeref
  (#?(:cljs -deref :default deref) [_]
    (loop [vals {} [[k res] :as resources] (seq resources)]
      (if (empty? resources)
        vals
        (let [status (pres/status res)
              value @res]
          (case status
            :error value
            :success (recur (assoc vals k value) (rest resources))
            nil))))))

(defn ->multi-resource [resources]
  (->MultiResource resources))

(defmethod ig/init-key ::resource [_ {:keys [resources]}]
  (->multi-resource resources))
