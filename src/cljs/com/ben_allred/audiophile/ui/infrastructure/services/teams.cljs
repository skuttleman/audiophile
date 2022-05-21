(ns com.ben-allred.audiophile.ui.infrastructure.services.teams
  (:require
    [com.ben-allred.audiophile.ui.infrastructure.forms.standard :as form.std]
    [com.ben-allred.audiophile.ui.infrastructure.services.pages :as pages]))

(defn res:fetch-all [sys]
  (pages/res:fetch-all sys :api/teams))

(defn form:new [sys attrs]
  (let [*form (form.std/create {:team/type :COLLABORATIVE})]
    (pages/form:new sys attrs *form :api/teams)))
