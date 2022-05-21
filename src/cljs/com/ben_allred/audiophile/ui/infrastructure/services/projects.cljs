(ns com.ben-allred.audiophile.ui.infrastructure.services.projects
  (:require
    [com.ben-allred.audiophile.ui.infrastructure.forms.standard :as form.std]
    [com.ben-allred.audiophile.ui.infrastructure.services.pages :as pages]))

(defn res:fetch-all [sys]
  (pages/res:fetch-all sys :api/projects))

(defn form:new [sys attrs team-id]
  (let [*form (form.std/create {:project/team-id team-id})]
    (pages/form:new sys attrs *form :api/projects)))
