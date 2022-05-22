(ns audiophile.ui.services.pages
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.forms.submittable :as form.submit]
    [audiophile.ui.resources.impl :as ires]
    [audiophile.ui.store.actions :as act]
    [com.ben-allred.vow.core :as v]))

(defn ^:private with-handlers [vow {:keys [on-success on-error]}]
  (v/peek vow on-success on-error))

(defn ^:private with-toast [vow {:keys [store]}]
  (v/peek vow
          nil
          (fn [_]
            (store/dispatch! store (act/toast#add! :error "Something went wrong")))))

(defn ^:private with-resource [vow {:keys [*res]}]
  (v/peek vow
          (fn [_]
            (some-> *res res/request!))))

(defn res:fetch
  ([sys handle]
   (res:fetch sys handle nil))
  ([{:keys [http-client nav]} handle params]
   (ires/http http-client
              (fn [opts]
                {:method :get
                 :url    (nav/path-for nav handle (update params :params merge opts))}))))

(defn form:new
  ([sys attrs *form handle]
   (form:new sys attrs *form handle nil))
  ([{:keys [http-client nav] :as sys} attrs *form handle params]
   (form.submit/create *form
                       (ires/http http-client
                                  (fn [body]
                                    {:method      :post
                                     :url         (nav/path-for nav handle params)
                                     :body        {:data body}
                                     :http/async? true})
                                  (fn [vow]
                                    (-> vow
                                        (with-handlers attrs)
                                        (with-toast sys)
                                        (with-resource attrs))))
                       (or (:local->remote attrs) identity)
                       (or (:remote->local attrs) identity))))
