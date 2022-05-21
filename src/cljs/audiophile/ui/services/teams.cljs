(ns audiophile.ui.services.teams
  (:require
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.services.pages :as pages]))

(defn res:fetch-all [sys]
  (pages/res:fetch-all sys :api/teams))

(defn form:new [sys attrs]
  (let [*form (form.std/create {:team/type :COLLABORATIVE})]
    (pages/form:new sys attrs *form :api/teams)))
