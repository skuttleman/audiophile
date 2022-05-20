(ns com.ben-allred.audiophile.ui.infrastructure.store.actions)

(def ^:private banner-err-codes
  {:login-failed "Authentication failed. Please try again."})

(defn banner:add! [level code]
  (if-let [body (banner-err-codes code)]
    [:banners/add! {:id    (.getTime (js/Date.))
                    :level level
                    :body  body}]
    (throw (ex-info "banners must have a body" {:level level :cod code}))))

(defn banner:remove! [id]
  [:banners/remove! {:id id}])

(def profile:load!
  [:user.profile/load!])

(defn profile:set! [profile]
  [:user.profile/set! profile])

(defn router:update [route]
  [:router/update route])
