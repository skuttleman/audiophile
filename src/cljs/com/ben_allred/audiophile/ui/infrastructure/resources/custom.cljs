(ns com.ben-allred.audiophile.ui.infrastructure.resources.custom
  (:require
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.vow.core :as v :include-macros true]))

(defn comments-fetcher [{:keys [http-client]}]
  (fn [opts]
    (-> http-client
        (res/request! (maps/assoc-defaults opts
                                           :method :get
                                           :nav/route :api/file.comments))
        (v/then-> (update :data (fns/=>> (sort-by :comment/created-at >)
                                         (colls/nest-children :comment/id
                                                              :comment/comment-id
                                                              :comment/comments)))))))

(defn ^:private update-toast [result msg]
  (vary-meta (or result {}) maps/assoc-defaults :toast/msg msg))

(defn comment-poster [{:keys [*comments http-client]}]
  (fn [{data :form/value}]
    (-> http-client
        (res/request! {:body        {:data data}
                       :method      :post
                       :http/async? true
                       :nav/route   :api/comments})
        (v/then (fn [result]
                  (res/request! *comments
                                {:nav/params {:params {:file/id (:file/id data)}}})
                  (update result :data update-toast "Success - comment created"))
                (fn [result]
                  (-> result
                      (update :error update-toast "Error - comment creation failed")
                      v/reject))))))

(deftype ArtifactResource [http-client nav]
  pres/IResource
  (request! [_ opts]
    (http/get http-client
              (nav/path-for nav :api/artifact {:params opts})
              {:response-type :blob})))

(defn res-artifact [{:keys [http-client nav]}]
  (->ArtifactResource http-client nav))
