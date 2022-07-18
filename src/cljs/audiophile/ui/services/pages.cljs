(ns audiophile.ui.services.pages
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.forms.submittable :as form.submit]
    [audiophile.ui.resources.impl :as ires]
    [audiophile.ui.store.actions :as act]
    [clojure.string :as string]
    [com.ben-allred.vow.core :as v]))

(defn ^:private err-msg [path]
  (str (string/join "." (map name path)) " is in use"))

(defn ^:private with-handlers [vow {:keys [on-success on-error]}]
  (v/peek vow on-success on-error))

(defn ^:private with-toast [vow {:keys [store]}]
  (cond-> vow
    store (v/peek (fn [_]
                    (let [action (act/toast#add! :success "Success")]
                      (store/dispatch! store action)))
                  (fn [_]
                    (let [action (act/toast#add! :error "Something went wrong")]
                      (store/dispatch! store action))))))

(defn conflict-validator [validator *conflicts]
  (fn [data]
    (reduce (fn [errors [path value]]
              (cond-> errors
                (= value (get-in data path)) (update-in path conj (err-msg path))))
            (validator data)
            @*conflicts)))

(defn res:fetch
  ([sys handle]
   (res:fetch sys handle nil))
  ([{:keys [http-client nav store]} handle params]
   (ires/http store
              http-client
              (fn [opts]
                {:method :get
                 :url    (nav/path-for nav handle (update params :params merge opts))}))))

(defn res:modify
  ([sys attrs handle]
   (res:modify sys attrs handle nil))
  ([{:keys [http-client nav store] :as sys} attrs handle params]
   (ires/http store
              http-client
              (fn [body]
                {:method      :patch
                 :url         (nav/path-for nav handle params)
                 :body        {:data body}
                 :http/async? true})
              (fn [vow]
                (-> vow
                    (with-handlers attrs)
                    (with-toast sys))))))

(defn ^:private form:* [{:keys [http-client nav store] :as sys} attrs *form method handle params]
  (let [id (forms/id *form)]
    (form.submit/create id
                        *form
                        (ires/http id
                                   store
                                   http-client
                                   (fn [body]
                                     {:method      method
                                      :url         (nav/path-for nav handle params)
                                      :body        {:data body}
                                      :http/async? true})
                                   (fn [vow]
                                     (-> vow
                                         (with-handlers attrs)
                                         (with-toast sys))))
                        (or (:local->remote attrs) identity)
                        (or (:remote->local attrs) identity))))

(defn form:new
  ([sys attrs *form handle]
   (form:new sys attrs *form handle nil))
  ([sys attrs *form handle params]
   (form:* sys attrs *form :post handle params)))

(defn form:modify
  ([sys attrs *form handle]
   (form:modify sys attrs *form handle nil))
  ([sys attrs *form handle params]
   (form:* sys attrs *form :patch handle params)))

(defn modal:open [{:keys [store]} header body]
  (fn [_]
    (store/dispatch! store (act/modal#add! header body))))
