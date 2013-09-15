(ns captain-githook.repo
  (:require [captain-githook.util :as util]
            [clojure.java.shell :as shell])
  (:import [java.net URI]
           [java.io File]))

(def Repo
  {:name String
   :owner String
   :provider String})

(def ConfigRepo
  (merge Repo {:url String}))

(defn config
  "Returns config.edn map."
  []
  (read-string (slurp (util/captain-path "config.edn"))))

(def host->provider
  {"bitbucket.org" "bitbucket"
   "github.com" "github"})

(defn url->repo
  "Extracts Repo metadata from a git provider url."
  [url]
  (let [uri (java.net.URI. url)
        provider (host->provider (.getHost uri))
        [_ owner name] (re-find #"\/([^\/]+)\/([^\.]+)"
                                (.getPath uri))]
    {:name name
     :owner owner
     :provider provider}))

(defn url->config-repo [url]
  (assoc (url->repo url) :url url))

(defn config-repos
  "Returns all the ConfigRepos listed in config.edn."
  []
  (map (comp url->config-repo :url) (:repos (config))))

(defn config-repo->repo
  "Degrades a ConfigRepo into a Repo so a Repo can easily be
   compared to all ConfigRepos with equality comparison."
  [config-repo]
  (select-keys config-repo [:name :owner :provider]))

(defn repo->config-repo
  "Returns a ConfigRepo if this Repo is listed in config.edn.
   Else it returns nil."
  [repo]
  (first (filter (fn [crepo] (= repo (config-repo->repo crepo)))
                 (config-repos))))

;; Payload ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Github and Bitbucket send different JSON.

(defn bitbucket-payload? [payload]
  (when-let [url (:canon_url payload)]
    (= (host->provider (.getHost (URI. url)))
       "bitbucket")))

(defn github-payload? [payload]
  (when-let [url (:url (:repository payload))]
    (= (host->provider (.getHost (URI. url)))
       "github")))

(defn bitbucket-payload->repo
  "Parses Payload JSON into a Repo."
  [payload]
  (let [name (:name (:repository payload))
        owner (:owner (:repository payload))
        provider (let [host (.getHost (URI. (:canon_url payload)))]
                   (host->provider host))]
    {:name name 
     :owner owner 
     :provider provider}))

(defn github-payload->repo [payload]
  (let [url (:url (:repository payload))]
    (url->repo url)))

(defn payload->repo [payload]
  (cond
   (bitbucket-payload? payload) (bitbucket-payload->repo payload)
   (github-payload? payload) (github-payload->repo payload)))

;; Repo paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn provider-path
  "~/captain-githook/{{provider}}"
  [repo]
  (util/captain-path (:provider repo)))

(defn owner-path
  "~/captain-githook/{{provider}}/{{owner}}"
  [repo]
  (util/captain-path (:provider repo) (:owner repo)))

(defn repo-path
  "~/captain-githook/{{provider}}/{{owner}}/{{name}}"
  [repo]
  (util/captain-path (:provider repo) (:owner repo) (:name repo)))

;; Shell ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; These return {:exit _, :out _, :in _}

(defn git-clone [repo]
  (shell/with-sh-dir (owner-path repo)
    (shell/sh "git" "clone" (:url repo))))

(defn git-pull [repo]
  (shell/with-sh-dir (repo-path repo)
    (shell/sh "git" "pull" "origin")))

(defn sync-repo
  "If dir exists at repo path, then just git-pull.
   If dir doesn't exist, then git-clone."
  [repo]
  (if (.exists (File. (repo-path repo)))
    (git-pull repo)
    (do (util/mkdir-p (owner-path repo))
        (git-clone repo))))

(defn autodeploy [repo]
  (shell/with-sh-dir (repo-path repo)
    (shell/sh "make" "githook-autodeploy")))
