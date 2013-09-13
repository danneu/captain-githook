(ns captain-githook.util
  (:require [schema.core :as s]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:import [java.io File]))

;; Paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def home-path  ;=> "/Users/danneu
  (System/getProperty "user.home"))

(defn captain-path
  ([] (captain-path ""))
  ([& paths]
     (let [root (.getPath (File. home-path "captain-githook"))]
       (.getPath (File. root (str/join "/" paths))))))

;; (assert (= (captain-path)
;;            "/Users/danneu/captain-githook"))
;; (assert (= (captain-path "config.edn")
;;            "/Users/danneu/captain-githook/config.edn"))

;; Util ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-json
  "Reads a string into a map with keys -> keywords."
  [s]
  (json/read-str s :key-fn keyword))

(defmulti mkdir-p
  "Like `mkdir -p` in that it creates the necessary
   parent directories until it can create the given directory."
  class)

(defmethod mkdir-p String
  [path]
  (mkdir-p (File. path)))

(defmethod mkdir-p File
  [dir]
  (loop [dir dir
         pending-dirs []]
    (if (.exists dir)
      false
      (let [parent-dir (.getParentFile dir)]
        (if (and (.exists parent-dir)
                 (not (.exists dir)))
          (let [success (.mkdir dir)]
            (doseq [d pending-dirs] (mkdir-p d))
            success)
          (recur parent-dir (conj pending-dirs dir)))))))
