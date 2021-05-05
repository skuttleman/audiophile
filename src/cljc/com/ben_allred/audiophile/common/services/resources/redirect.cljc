(ns com.ben-allred.audiophile.common.services.resources.redirect
  (:require
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]
    [integrant.core :as ig])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defn ^:private ->redirect [nav page params]
  (fn [result]
    (when page
      (nav/navigate! nav page params))
    result))

(deftype RedirectResource [resource nav routes]
  pres/IResource
  (request! [_ opts]
    (-> (pres/request! resource opts)
        (v/then (->redirect nav
                            (:success/page routes)
                            (:success/params routes))
                (->redirect nav
                            (:error/page routes)
                            (:error/params routes)))))
  (status [_]
    (pres/status resource))

  pv/IPromise
  (then [_ on-success on-error]
    (v/then resource on-success on-error))

  IDeref
  (#?(:cljs -deref :default deref) [_]
    @resource))

(defmethod ig/init-key ::resource [_ {:keys [nav resource routes]}]
  (->RedirectResource resource nav routes))
