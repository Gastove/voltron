(ns voltron.server
  (:require [compojure.core :refer [defroutes context GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [org.httpkit.server :as server]
            [ring.middleware.basic-authentication :as basic]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.reload :as reload]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.session :as session]
            [cemerick.drawbridge :as drawbridge]
            [environ.core :refer [env]]))

(defroutes application-routes
  (GET "*" []
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (slurp (io/resource "public/index.html"))})
  (route/resources "/"))

(defn create-application
  [app-handlers]
  (-> (wrap-defaults app-handlers site-defaults)
      ;; (wrap-trim-trailing-slash)
      (wrap-json-body)
      ;; (wrap-route-not-found)
      (reload/wrap-reload)
      (trace/wrap-stacktrace)
      ;; ((if (= (:env (server-config)) :production)
      ;;    ;; wrap-error-page
      ;;    trace/wrap-stacktrace
      ;;    trace/wrap-stacktrace))
      ))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))
        ;; store (cookie/cookie-store {:key (env :session-secret)})
        application (create-application application-routes)]
    (server/run-server application {:port 5000})
    ))
