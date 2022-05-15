(ns com.ben-allred.audiophile.common.api.navigation.routes
  (:require
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(def auth-routes
  ["/auth"
   [["/callback" :auth/callback]
    ["/login" :auth/login]
    ["/logout" :auth/logout]
    ["/details" :auth/details]
    [true :api/not-found]]])

(def api-routes
  ["/api"
   [["/ws" :ws/connection]
    ["/artifacts"
     [["" :api/artifacts]
      [["/" [uuids/regex :artifact/id]] :api/artifact]]]
    ["/comments"
     [["" :api/comments]]]
    ["/events"
     [["" :api/events]]]
    ["/files"
     [[["/" [uuids/regex :file/id]]
       [["" :api/file]
        ["/comments" :api/file.comments]]]]]
    ["/projects"
     [["" :api/projects]
      [["/" [uuids/regex :project/id]]
       [["" :api/project]
        ["/files"
         [["" :api/project.files]]]]]]]
    ["/teams"
     [["" :api/teams]
      [["/" [uuids/regex :team/id]] :api/team]]]
    ["/search"
     [[["/" :field/entity "/" :field/name "/" :field/value] :api/search]]]
    ["/users"
     [["/profile" :api/profile]
      ["" :api/users]]]
    [true :api/not-found]]])

(def ui-routes
  [""
   [["/" :ui/home]
    ["/files"
     [[["/" [uuids/regex :file/id]] :ui/file]]]
    ["/login" :ui/login]
    ["/projects"
     [["" :ui/projects]
      [["/" [uuids/regex :project/id]] :ui/project]]]
    ["/teams"
     [["" :ui/teams]
      [["/" [uuids/regex :team/id]] :ui/team]]]]])

(def resource-routes
  [""
   [["/health" :resources/health]
    ["/js" [[true :resources/js]]]
    ["/css" [[true :resources/css]]]]])

(def all
  [""
   [auth-routes
    api-routes
    ui-routes
    resource-routes
    [true :route/not-found]]])
