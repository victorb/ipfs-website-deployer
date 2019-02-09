(ns ipfs-website-deployer.docker
  (:use [clojure.java.shell :only [sh with-sh-dir]])
  (:require [clj-docker-client.core :as docker]
            [ipfs-website-deployer.env :refer [env]]
            [clojure.pprint :refer [pprint]])
            ; [ipfs-website-deployer.logger :refer [log]])
  (:import (org.apache.commons.compress.archivers.tar TarArchiveInputStream)
           (java.io FileOutputStream)
           (java.io FileInputStream)
           (java.io File)))

(def build-dir (env "BUILD_DIR"))

(defn cmd-split
  [str]
  (clojure.string/split str #" "))

(defn sh-str
  [str]
  (apply sh (cmd-split str)))

(defn mktemp []
  (clojure.string/trim (:out (sh-str "mktemp -d"))))

(defn inputstream-to-dir
  [inputstream outdirpath]
  (let [buffer (byte-array 1024)
        outdir (File. outdirpath)]
    (.mkdir outdir)
    (loop [e (.getNextEntry inputstream)]
      (if e
        (let [filename (.getName e)
              outfile (File. (str outdir (File/separator) filename))]
          (if (.isDirectory e)
            (.mkdirs outfile)
            (do
              (.mkdirs (File. (.getParent outfile)))
              (with-open [outstream (FileOutputStream. outfile)]
                (loop [len (.read inputstream buffer)]
                  (if (< 0 len)
                    (do
                      (.write outstream buffer 0 len)
                      (recur (.read inputstream buffer))))))))
          (recur (.getNextEntry inputstream)))))))

(defn copy-to-host
  [conn id container-path host-path]
  (inputstream-to-dir
    (new TarArchiveInputStream
         (.archiveContainer conn id container-path)) host-path))


(defn build
  [directory config]
  (let [cmd (:build_command config)
        build-dir (mktemp)
        conn (docker/connect)]
    (let [id (docker/create
               conn
               "ipfs/ci-websites"
               "make -C /site build"
               ;; cmd
               {} {})]
      (docker/cp conn id directory "/site")
      (docker/start conn id)
      (docker/wait-container conn id)
      (copy-to-host conn id "/site/" build-dir)
      (docker/disconnect conn)
      (str build-dir "/site/" (:build_directory config)))))
