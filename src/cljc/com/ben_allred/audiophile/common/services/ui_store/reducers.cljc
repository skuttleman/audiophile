(ns com.ben-allred.audiophile.common.services.ui-store.reducers
  (:require
    [com.ben-allred.collaj.reducers :as rcollaj]))

(defn ^:private resource-state
  ([]
   nil)
  ([state [action k data]]
   (case action
     :resource/register
     (update state k assoc :status :init ::sub data)

     :resource/request
     (update state k (comp (get-in state [k ::sub] identity) assoc)
             :status :requesting)

     :resource/success
     (update state k (comp (get-in state [k ::sub] identity) assoc)
             :status :success
             :value data)

     :resource/failure
     (update state k (comp (get-in state [k ::sub] identity) assoc)
             :status :error
             :errors data)

     :resource/remove
     (dissoc state k)

     state)))

(defn ^:private page
  ([]
   nil)
  ([state [type route]]
   (case type
     :router/updated route
     state)))

(def reducer
  (rcollaj/combine {:internal/resource-state resource-state
                    :page                    page}))
