(ns audiophile.common.infrastructure.navigation.routes
  (:require
    [audiophile.common.core.utils.uuids :as uuids]))

(def auth-routes
  ["/auth"
   [["/callback" :routes.auth/callback]
    ["/login" :routes.auth/login]
    ["/logout" :routes.auth/logout]
    [true :routes.auth/not-found]]])

(def api-routes
  ["/api"
   [["/ws" :routes.ws/connection]
    ["/artifacts"
     [["" :routes.api/artifact]
      [["/" [uuids/regex :artifact/id]] :routes.api/artifacts:id]]]
    ["/comments"
     [["" :routes.api/comments]]]
    ["/events"
     [["" :routes.api/events]]]
    ["/files"
     [[["/" [uuids/regex :file/id]]
       [["" :routes.api/files:id]
        ["/comments" :routes.api/files:id.comments]]]]]
    ["/projects"
     [["" :routes.api/projects]
      [["/" [uuids/regex :project/id]]
       [["" :routes.api/projects:id]
        ["/files"
         [["" :routes.api/projects:id.files]]]]]]]
    ["/team-invitations"
     [["" :routes.api/team-invitations]]]
    ["/teams"
     [["" :routes.api/teams]
      [["/" [uuids/regex :team/id]]
       [["" :routes.api/teams:id]]]]]
    ["/users"
     [["" :routes.api/users]
      ["/profile" :routes.api/users.profile]]]
    [true :routes.api/not-found]]])

(def ui-routes
  [""
   [["/" :routes.ui/home]
    ["/files"
     [[["/" [uuids/regex :file/id]] :routes.ui/files:id]]]
    ["/login" :routes.ui/login]
    ["/projects"
     [[["/" [uuids/regex :project/id]] :routes.ui/projects:id]]]
    ["/teams"
     [[["/" [uuids/regex :team/id]] :routes.ui/teams:id]]]]])

(def resource-routes
  [""
   [["/health" :routes.resources/health]
    ["/js" [[true :routes.resources/js]]]
    ["/css" [[true :routes.resources/css]]]]])

(def all
  [""
   [auth-routes
    api-routes
    ui-routes
    resource-routes
    [true :routes/not-found]]])
