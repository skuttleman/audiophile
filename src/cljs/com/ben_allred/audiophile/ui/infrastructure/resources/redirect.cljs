(ns com.ben-allred.audiophile.ui.infrastructure.resources.redirect
  (:require
    [com.ben-allred.audiophile.common.app.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]))

(defn ^:private ->redirect [nav handle params]
  (fn [_]
    (when handle
      (nav/navigate! nav handle params))))

(deftype RedirectResource [*resource nav routes]
  pres/IResource
  (request! [_ opts]
    (-> (pres/request! *resource opts)
        (v/peek (->redirect nav
                            (:success/handle routes)
                            (:success/params routes))
                (->redirect nav
                            (:error/handle routes)
                            (:error/params routes)))))
  (status [_]
    (pres/status *resource))

  pv/IPromise
  (then [_ on-success on-error]
    (v/then *resource on-success on-error))

  IDeref
  (-deref [_]
    @*resource))

(defn resource [{:keys [nav resource routes]}]
  (->RedirectResource resource nav routes))
