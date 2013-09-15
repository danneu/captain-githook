(ns captain-githook.handler
  (:require [captain-githook.util :as util]
            [captain-githook.repo :as repo]
            [captain-githook.clansi :as clansi]
            [compojure.handler :as handler]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.route :as route]
            [clojure.string :as str]
            [ring.util.response :as response]
            [clojure.data.json :as json]
            [clojure.java.shell :as shell]
            [clojure.pprint :refer [pprint]]
            [ring.util.response :refer [response]])
  (:use compojure.core)
  (:import [java.io File]
           [java.net URI])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Trying to win an award for the worst abstraction in the world.
;;

(defmacro test-each [all-tests]
  `(loop [tests# ~all-tests
          latest-msg# ""]
     (if-let [testfn# (first tests#)]
       (let [[result# msg#] ((eval testfn#))]
         (if result#
           (recur (rest tests#) msg#)
           [false msg#]))
       [true latest-msg#])))

;; incoming lolz

(defn step [msg & tests]
  (print (str "---> " msg "... ")) (flush)
  (let [[result reportmsg] (test-each tests)]
     (if result
       (println (clansi/style
                 (str "Success"
                      (when reportmsg
                        (str \newline reportmsg)))
                 :green))
       (println (clansi/style
                 (str "Fail"
                      (when reportmsg
                        (str \newline reportmsg)))
                 :red)))
     result))

;; Steps ;;;;;;;;;;;;;L;;O::L;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step-check-captain-path []
  (step (str "Checking " (util/captain-path))
        #(if (.exists (File. (util/captain-path)))
           [true nil]
           [false "No directory found"])))

(defn step-check-config []
  (step (str "Checking " (util/captain-path "config.edn"))
        #(if (.exists (File. (util/captain-path "config.edn")))
           [true nil]
           [false "config.edn not found."])
        #(if (pos? (count (repo/config-repos)))
           [true nil]
           [false "No repos listed in config.edn"])))

(defn step-sync-repo [repo]
  (step (str "Syncing " (:url repo))
        (fn []
          (let [{:keys [exit out err]} (repo/sync-repo repo)]
            (if (= exit 0)
              [true (str err out)]
              [false (str err out)])))))

(defn step-autodeploy [repo]
  (step (str "Autodeploying " (:url repo))
        (fn []
          (let [{:keys [exit out err]} (repo/autodeploy repo)]
            (if (= exit 0)
              [true (str err out)]
              [false (str err out)])))))

;; Routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defroutes app-routes
  (POST "/" {{payload-string :payload} :params}
    (let [payload (util/parse-json payload-string)]
      (if-let [repo (repo/payload->repo payload)]
        (if-let [config-repo (repo/repo->config-repo repo)]
          (do (step-sync-repo config-repo)
              (step-autodeploy config-repo))
          (do (println "Repo not listed in config.edn:")
              (pprint repo)))
        (do (println "Could not parse this payload:")
            (pprint payload))))
    (response "Thanks"))
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

;; Server ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exit []
  (println "---> Exiting")
  (System/exit 0))

(defn start-server [port]
  (run-jetty app {:port port}))

(defn -main [& args]
  (let [port (Integer. (or (first args) "5009"))]
    (println "Captain Githook is preparing to set sail.")
    (when-not (step-check-captain-path) (exit))
    (when-not (step-check-config) (exit))
    (doseq [repo (repo/config-repos)]
      (step-sync-repo repo)
      (step-autodeploy repo))
    (start-server port)))
