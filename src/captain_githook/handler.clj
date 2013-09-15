(ns captain-githook.handler
  (:require [captain-githook.util :as util]
            [captain-githook.repo :as repo]
            [compojure.handler :as handler]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.route :as route]
            [clojure.string :as str]
            [ring.util.response :as response]
            [clojure.data.json :as json]
            [clojure.java.shell :as shell])
  (:use compojure.core)
  (:import [java.io File]
           [java.net URI])
  (:gen-class))

;; Routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defroutes app-routes
  (POST "/" {{payload-string :payload} :params}
    (let [payload (util/parse-json payload-string)]
      (if-let [repo (repo/payload->repo payload)]
        ;; Payload could be parsed
        (if-let [config-repo (repo/repo->config-repo repo)]
          ;; Repo listed in config.edn
          (do
            (println (str "---> Pulling " (:url config-repo) "... "))
            (let [{out :out err :err} (repo/sync-repo config-repo)]
              (print out err))
            (println "Done."))
          ;; Repo NOT listed in config.edn
          (do
            (println "Repo not listed in config.edn:")
            (clojure.pprint/pprint repo)))
        ;; Payload couldn't be parsed.
        (do
          (println "Could not parse this payload:")
          (clojure.pprint/pprint payload)))))
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

;; Server ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start-server [port]
  (run-jetty app {:port port}))

(defn -main [& args]
  (let [port (Integer. (or (first args) "5009"))]

    (println)
    (println "Captain Githook is preparing to set sail.")

    ;; Create ~/captain-githook
    ;; - Exit if mkdir fails
    (let [path (util/captain-path)]
      (print (str "---> Checking " path "... "))
      (if (.exists (File. path))
        (println "Exists")
        (if (.mkdir (File. path))
          (println "Created")
          (do (println "Failed to create it.")
              (System/exit 0)))))

    ;; Read ~/captain-githook/config.edn
    (let [path (util/captain-path "config.edn")]
      (print (str "---> Checking " path "... "))
      (if (.exists (File. path))
        (do (println "Exists")
            (println (str "     - Repos found: "
                          (count (repo/config-repos))))
            (doseq [repo (repo/config-repos)]
              (println (str "       " (:url repo)))))
        (do (println "Not found")
            (System/exit 0))))

    ;; Sync each repo
    (doseq [repo (repo/config-repos)]
      (println (str "---> Syncing " (:url repo) "... "))
      (let [{:keys [out err]} (repo/sync-repo repo)]
        (print out err))
      (println "Done."))
    
    (start-server port)))
