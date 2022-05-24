(ns audiophile.ui.services.pages
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.infrastructure.store.core :as store]
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
                                        (with-toast sys))))
                       (or (:local->remote attrs) identity)
                       (or (:remote->local attrs) identity))))
