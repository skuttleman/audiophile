(ns com.ben-allred.audiophile.ui.infrastructure.resources.custom
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
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

(defn comment-poster [{:keys [*comments http-client]}]
  (fn [{data :form/value}]
    (v/and (res/request! http-client
                         {:body        {:data data}
                          :method      :post
                          :http/async? true
                          :nav/route   :api/comments})
           (res/request! *comments
                         {:nav/params {:route-params {:file-id (:file/id data)}}})
           {:data (select-keys data #{:comment/selection
                                      :comment/with-selection?
                                      :comment/file-version-id})})))
