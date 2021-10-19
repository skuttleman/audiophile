(ns com.ben-allred.audiophile.backend.dev.accessors
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.api.pubsub.core :as ps]
    [com.ben-allred.audiophile.backend.api.pubsub.protocols :as pps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [integrant.core :as ig]))

(defonce ^:private repl-mock-state (atom nil))

(comment
  (reset! repl-mock-state nil))

(defmulti ^:private return-mock (fn [async? _ val _ _]
                                  [(if async? ::async ::sync)
                                   (if (instance? Throwable val) ::ex ::val)]))

(defmethod return-mock [::async ::ex]
  [_ k v ch opts]
  (log/info "MOCKING :command/failed" k v)
  (ps/command-failed! ch (:request/id opts) opts)
  nil)

(defmethod return-mock [::async ::val]
  [_ k v ch opts]
  (let [event-type (:event-type v)]
    (log/info "MOCKING" event-type k v)
    (ps/emit-event! ch (:request/id opts) event-type (dissoc v :event-type) opts)
    nil))

(defmethod return-mock [::sync ::ex]
  [_ k v _ _]
  (log/info "THROWING MOCK EXCEPTION" k)
  (throw v))

(defmethod return-mock [::sync ::val]
  [_ k v _ _]
  (log/info "RETURNING MOCK DATA" k)
  v)

(defmacro ^:private mock! [[_ action :as key] ch opts impl]
  `(if-let [[key# mock#] (find @repl-mock-state ~key)]
     (if (fn? mock#)
       (mock# (fn [] ~impl))
       (let [async?# (= \! (let [s# (name ~action)]
                             (get s# (dec (count s#)))))]
         (return-mock async?# key# mock# ~ch ~opts)))
     ~impl))

(defn ^:private set-mock!
  ([key]
   (set-mock! key nil))
  ([key value]
   (swap! repl-mock-state assoc key value)))

(defn ^:private unset-mock! [key]
  (swap! repl-mock-state dissoc key))

(deftype MockChannel [handler]
  pps/IChannel
  (send! [_ msg]
    (int/handle! handler msg)))

(deftype MockCommentAccessor [accessor ch]
  pint/ICommentAccessor
  pint/IAccessor
  (query-many [_ opts]
    (mock! [::comment :query-many] ch opts
      (pint/query-many accessor opts)))
  (create! [_ data opts]
    (mock! [::comment :create!] ch opts
      (pint/create! accessor data opts))))

(comment)

(deftype MockFileAccessor [accessor ch]
  pint/IAccessor
  (query-many [_ opts]
    (mock! [::file :query-many] ch opts
      (pint/query-many accessor opts)))
  (query-one [_ opts]
    (mock! [::file :query-one] ch opts
      (pint/query-one accessor opts)))

  pint/IFileAccessor
  (create-artifact! [_ data opts]
    (mock! [::file :create-artifact!] ch opts
      (pint/create-artifact! accessor data opts)))
  (create-file! [_ data opts]
    (mock! [::file :create-file!] ch opts
      (pint/create-file! accessor data opts)))
  (create-file-version! [_ data opts]
    (mock! [::file :create-file-version!] ch opts
      (pint/create-file-version! accessor data opts)))
  (get-artifact [_ opts]
    (mock! [::file :get-artifact] ch opts
      (pint/get-artifact accessor opts))))

(comment
  (set-mock! [::file :create-artifact!] (fn [impl] (Thread/sleep 5000) (impl)))
  (unset-mock! [::file :create-artifact!]))

(deftype MockProjectAccessor [accessor ch]
  pint/IProjectAccessor
  pint/IAccessor
  (query-many [_ opts]
    (mock! [::project :query-many] ch opts
      (pint/query-many accessor opts)))
  (query-one [_ opts]
    (mock! [::project :query-one] ch opts
      (pint/query-one accessor opts)))
  (create! [_ data opts]
    (mock! [::project :create!] ch opts
      (pint/create! accessor data opts))))

(comment
  (set-mock! [::project :query-many])
  (unset-mock! [::project :query-many]))

(deftype MockTeamAccessor [accessor ch]
  pint/ITeamAccessor
  pint/IAccessor
  (query-many [_ opts]
    (mock! [::team :query-many] ch opts
      (pint/query-many accessor opts)))
  (query-one [_ opts]
    (mock! [::team :query-one] ch opts
      (pint/query-one accessor opts)))
  (create! [_ data opts]
    (mock! [::team :create!] ch opts
      (pint/create! accessor data opts))))

(comment
  (set-mock! [::team :query-many] (fn [] (Thread/sleep 5000) []))
  (unset-mock! [::team :query-many]))

(defmethod ig/init-key :audiophile.repositories.dev/mock-interactor#comment [_ {:keys [accessor handler]}]
  (->MockCommentAccessor accessor (->MockChannel handler)))

(defmethod ig/init-key :audiophile.repositories.dev/mock-interactor#file [_ {:keys [accessor handler]}]
  (->MockFileAccessor accessor (->MockChannel handler)))

(defmethod ig/init-key :audiophile.repositories.dev/mock-interactor#project [_ {:keys [accessor handler]}]
  (->MockProjectAccessor accessor (->MockChannel handler)))

(defmethod ig/init-key :audiophile.repositories.dev/mock-interactor#team [_ {:keys [accessor handler]}]
  (->MockTeamAccessor accessor (->MockChannel handler)))
