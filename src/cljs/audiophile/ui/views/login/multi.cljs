(ns audiophile.ui.views.login.multi)

(defmulti form (fn [type _ _]
                 type))
