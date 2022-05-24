(ns audiophile.test.integration.common.http
  (:refer-clojure :exclude [get])
  (:require
    [clojure.core.async :as async]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.utils.core :as u]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uri :as uri]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.test.integration.common :as int]
    [audiophile.test.utils :as tu])
  (:import
    (org.apache.commons.io FileUtils)))

(defn login
  ([request system]
   (login request system nil))
  ([request system user]
   (login request system user nil))
  ([request system user opts]
   (let [serde (int/component system :serdes/jwt)
         opts (update-in opts [:jwt/claims :aud] #(or % #{:token/auth}))
         token (serdes/serialize serde user opts)]
     (assoc-in request [:headers "cookie"] (str "auth-token=" token)))))

(defmacro with-ws [[sym response] & body]
  `(let [~sym (:body ~response)]
     (try ~@body
          (finally
            (u/silent! (async/close! ~sym))))))

(defn as-ws [request]
  (assoc request :websocket? true))

(defn ^:private go [request system method handle params]
  (let [nav (int/component system :services/nav)
        [uri query-string] (string/split (nav/path-for nav handle params) #"\?")]
    (maps/assoc-maybe request
                      :request-method method
                      :uri uri
                      :query-string query-string)))

(defn get
  ([request system handle]
   (get request system handle nil))
  ([request system handle params]
   (go request system :get handle params)))

(defn post
  ([request system handle]
   (post request system handle nil))
  ([request system handle params]
   (go request system :post handle params)))

(defn as-async [request system handler]
  (with-ws [ch (-> request
                   (get system :ws/connection)
                   as-ws
                   handler)]
    (let [ws (async/pipe ch (async/chan 10 (remove #{[:conn/ping] [:conn/pong]})))
          result (-> request
                     (assoc-in [:headers :x-request-id] (uuids/random))
                     handler)
          {:event/keys [type data]} (if (http/success? result)
                                      (second (rest (tu/<!!ms ws)))
                                      (get-in result [:body :data]))]
      (-> result
          (assoc-in [:body :data] data)
          (cond->
            (= :command/failed type) (assoc :status 400))))))

(defn body-data [payload]
  {:body {:data payload}})

(defn file-upload
  ([filename]
   (file-upload filename "audio/mp3"))
  ([filename content-type]
   (let [file (io/file (io/resource filename))]
     {:params {"files[]" {:tempfile     file
                          :filename     filename
                          :content-type content-type
                          :size         (FileUtils/sizeOf file)}}})))

(defn upload
  ([request res]
   (upload request res "content/type"))
  ([request res content-type]
   (let [file (io/as-file res)]
     (assoc-in request
               [:params "files[]"]
               {:filename     (.getName file)
                :content-type content-type
                :tempfile     file
                :size         (.length file)}))))

(defn with-serde [handler serde]
  (let [mime-type (serdes/mime-type serde)]
    (fn [{:keys [websocket?] :as request}]
      (-> request
          (cond->
            (not websocket?)
            (-> (update :headers assoc :content-type mime-type :accept mime-type)
                (maps/update-maybe :body (partial serdes/serialize serde)))

            websocket?
            (assoc :query-string (uri/join-query {:content-type mime-type
                                                  :accept       mime-type})))
          handler
          (cond->
            (not (:websocket? request))
            (maps/update-maybe :body (partial serdes/deserialize serde)))))))
