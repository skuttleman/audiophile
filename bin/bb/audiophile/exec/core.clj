(ns audiophile.exec.core
  (:require
    [audiophile.exec.shared :as shared]
    audiophile.exec.run
    audiophile.exec.tasks))

(defn -main [& args]
  (shared/main* (first args) (rest args)))

(defmethod shared/main* :println
  [_ args]
  (println "println: " args))
