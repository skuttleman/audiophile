(ns com.ben-allred.audiophile.ui.infrastructure.store.mutations)

(def ^:private banner-err-codes
  {:login-failed "Authentication failed. Please try again."})

(defn user-profile [profile]
  [:user/profile profile])

(defn add-banner! [level code]
  (let [id (.getTime (js/Date.))]
    [:banners/add! {:id    id
                    :level level
                    :body  (banner-err-codes code)}]))

(defn remove-banner! [id]
  [:banners/remove! {:id id}])
