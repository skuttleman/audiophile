(ns audiophile.ui.services.login)

(defmulti form (fn [type _ _]
                 type))
