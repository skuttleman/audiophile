(ns com.ben-allred.audiophile.common.services.navigation.parsers
  (:require
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]))

(defmethod nav/params->internal :api/project
  [params]
  (update params :route-params maps/update-maybe :project-id uuids/->uuid))

(defmethod nav/params->internal :ui/project
  [params]
  (update params :route-params maps/update-maybe :project-id uuids/->uuid))

(defmethod nav/internal->params :api/project
  [params]
  (update params :route-params maps/update-maybe :project-id str))

(defmethod nav/internal->params :ui/project
  [params]
  (update params :route-params maps/update-maybe :project-id str))
