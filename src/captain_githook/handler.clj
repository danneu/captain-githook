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
  (POST "/" {{payload :payload} :params}
    (when-let [repo (repo/payload->configured-repo payload)]
      (println (str "---> Pulling " (:url repo) "... "))
      (let [{:keys [out err]} (repo/sync-repo repo)]
        (print out err))
      (println "Done.")))
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
            (println (str "     - Found "
                          (count (repo/configured-repos))
                          " repo(s)")))
        (do (println "Not found")
            (System/exit 0))))

    ;; Sync each repo
    (doseq [repo (repo/configured-repos)]
      (println (str "---> Syncing " (:url repo) "... "))
      (let [{:keys [out err]} (repo/sync-repo repo)]
        (print out err))
      (println "Done."))
    
    (start-server port)))

