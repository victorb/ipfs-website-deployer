(ns ipfs-website-deployer.build
  (:use [clojure.java.shell :only [sh with-sh-dir]])
  (:require [clojure.pprint :refer [pprint]]
            [ipfs-website-deployer.env :refer [env]]
            ;; [ipfs-website-deployer.logger :refer [log]]
            ))

(def build-dir (env "BUILD_DIR"))

(defn cmd-split
  [str]
  (clojure.string/split str #" "))

(defn sh-str
  [str dir]
  (with-sh-dir dir
    (apply sh (cmd-split str))))

(defn mktemp [] (sh-str "mktemp -d" build-dir))

(defn mkdirp [dir] (sh-str (str "mkdir -p " dir) build-dir))

(defn build
  ""
  [directory config]
  (do
    ; (log (str "Building " directory))
    ; (log (:build_command config))
    ; TODO actually produce some build artifact
    (let [res (sh-str (:build_command config) directory)]
      ; (log res)
      ; (log (:out res))
      ; (shutdown-agents)
      (let [artifact (str directory
                          (str "/" (:build_directory config)))]
        ; (log (str "Artifact: " artifact))
      artifact))))
