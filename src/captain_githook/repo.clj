(ns captain-githook.repo
  (:require [schema.core :as s]
            [captain-githook.util :as util]
            [clojure.java.shell :as shell])
  (:import [java.net URI]
           [java.io File]))

;; Schema ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; A PayloadRepo is parsed from the string value of a :payload
;; param POSTed by an incoming hook.
(def PayloadRepo
  {:name String
   :owner String
   :provider String})

;; A ConfiguredRepo is parsed from the :repos key in config.edn.
(def ConfiguredRepo
  {:name String
   :owner String
   :provider String
   :url String
   :path String})

(def host->provider
  {"bitbucket.org" "bitbucket"
   "github.com" "github"})

(defn parse-configured-repo
  "Extracts metadata from the :url supplied in config.edn."
  [m]
  (let [uri (java.net.URI. (:url m))
        provider (host->provider (.getHost uri))
        [_ owner name] (re-find #"\/([^\/]+)\/([^\/]+).git"
                                (.getPath uri))
        path (util/captain-path provider name)]
    (merge m {:provider provider
              :owner owner
              :name name
              :path path})))


;; (def configured-repos
;;   (map parse-configured-repo
;;        (:repos (read-string
;;                 (slurp (captain-path "config.edn"))))))
(defn configured-repos []
  (map parse-configured-repo
       (:repos (read-string
                (slurp (util/captain-path "config.edn"))))))

(defn configured-repo?
  "An incoming PayloadRepo must be one of the ConfiguredRepos
   before we take action."
  [prep]
  (some (fn [crep]
          (and (= (:name prep) (:name crep))
               (= (:name prep) (:name crep))))
        (configured-repos)))


(defn parse-payload-repo
  "Parses POST hook payload param-string into a repository map
   similar to the repository map that represents repos configured
   in config.edn."
  [s]
  (let [json (util/parse-json s)
        name (-> json :repository :name)
        owner (-> json :repository :owner)
        provider (host->provider (.getHost
                                  (URI. (:canon_url json))))]
    {:name name
     :owner owner
     :provider provider}))

(defn payload->configured-repo
  "Returns either a configured-repo or nil."
  [payload-string]
  (let [payload-repo (parse-payload-repo payload-string)]
    (when (configured-repo? payload-repo)
      (let [path (util/captain-path (:provider payload-repo)
                                    (:name payload-repo))]
        (merge payload-repo
               {:path path})))))

;; Git ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn git-clone [repo]
  (shell/with-sh-dir (util/captain-path (:provider repo))
    (shell/sh "git" "clone" (:url repo))))

(defn git-pull [repo]
  (shell/with-sh-dir (:path repo)
    (shell/sh "git" "pull" "origin")))

(defn sync-repo
  "If dir exists at repo path, then just git-pull.
   If dir doesn't exist, then git-clone.
   Returns {:status _, :out _, :err _, ...}"
  [repo]
  (if (.exists (File. (:path repo)))
    (git-pull repo)
    (do
      ;; Make ~/captain-githook/{provider} dir
      (util/mkdir-p (util/captain-path (:provider repo)))
      ;; Clone repo into ~/c-g/{provider}/{name}
      (git-clone repo))))

;; (defn sync-repo
;;   "If dir exists at repo path, then just git-pull.
;;    If dir doesn't exist, then git-clone."
;;   [repo]
;;   (let [exists (.exists (File. (:path repo)))]
;;     (println (:path repo) exists)
;;     (println "MKDIR:" (util/mkdir-p (:path repo))))
;;   (if (util/mkdir-p (:path repo))
;;     (do (println "pull]]") (git-pull repo))
;;     (do (println "clone]]") (git-clone repo))))







;; ;; Schema ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ;; A PayloadRepo is parsed from the string value of a :payload
;; ;; param POSTed by an incoming hook.
;; (def PayloadRepo
;;   {:name String
;;    :owner String
;;    :provider String})

;; ;; A ConfiguredRepo is parsed from the :repos key in config.edn.
;; (def ConfiguredRepo
;;   {:name String
;;    :owner String
;;    :provider String
;;    :url String
;;    :path String})

;; (def host->provider
;;   {"bitbucket.org" "bitbucket"
;;    "github.com" "github"})

;; (s/defn parse-payload-repo :- {:name String
;;                                :owner String}
;;   "Parses POST hook payload param-string into a repository map
;;    similar to the repository map that represents repos configured
;;    in config.edn."
;;   [s :- s/String]
;;   (let [json (util/parse-json s)
;;         name (-> json :repository :name)
;;         owner (-> json :repository :owner)
;;         provider (host->provider (.getHost
;;                                   (URI. (:canon_url json))))]
;;     {:name name
;;      :owner owner
;;      :provider provider}))

;; (defn parse-configured-repo
;;   "Extracts metadata from the :url supplied in config.edn."
;;   [m]
;;   (let [uri (java.net.URI. (:url m))
;;         provider (host->provider (.getHost uri))
;;         [_ owner name] (re-find #"\/([^\/]+)\/([^\/]+).git"
;;                                 (.getPath uri))
;;         path (util/captain-path provider name)]
;;     (merge m {:provider provider
;;               :owner owner
;;               :name name
;;               :path path})))

;; ;; (def configured-repos
;; ;;   (map parse-configured-repo
;; ;;        (:repos (read-string
;; ;;                 (slurp (captain-path "config.edn"))))))

;; (s/defn configured-repo? :- Boolean
;;   "An incoming PayloadRepo must be one of the ConfiguredRepos
;;    before we take action."
;;   [prep :- PayloadRepo]
;;   (some (fn [crep]
;;           (and (= (:name prep) (:name crep))
;;                (= (:name prep) (:name crep))))
;;         configured-repos))

;; ;; Git ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (s/defn git-clone [repo :- ConfiguredRepo]
;;   (shell/with-sh-dir (util/captain-path (:provider repo))
;;     (shell/sh "git" "clone" (:url repo))))

;; (s/defn git-pull [repo :- ConfiguredRepo]
;;   (shell/with-sh-dir (:path repo)
;;     (shell/sh "git" "pull origin")))

;; (defn sync-repo
;;   "If dir exists at repo path, then just git-pull.
;;    If dir doesn't exist, then git-clone."
;;   [repo]
;;   (if (mkdir-p (:path repo))
;;     (do (print (str "---> Pulling " (:url repo) "... "))
;;         (git-pull repo)
;;         (println "Done."))
;;     ;; - If we had to create it, then we need to set the git
;;     ;;   origin before we pull.
;;     (do (print (str "---> Cloning " (:url repo) "... "))
;;         (git-clone repo)
;;         (println "Done."))))
