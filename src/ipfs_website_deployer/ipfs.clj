(ns ipfs-website-deployer.ipfs
  (:require [ipfs-website-deployer.env :refer [env]]
            [clj-http.client :as client]
            [ipfs-website-deployer.logger :refer [log]]
            [cheshire.core :refer [parse-string]]))

(defn starts-with-dot? "" [file] (= \. (first (.getName ^java.io.File file))))

(defn is-dir? "" [file] (.isDirectory ^java.io.File file))

(defn hide-dotfiles "" [files] (remove starts-with-dot? files))
(defn hide-dirs "" [files] (remove is-dir? files))

(defn hide-dotfiles-from-files-map [files]
  (remove #(= \. (first (:name %))) files))

(def ipfs-api (env "IPFS_API"))

(defn ipfs-api-path [path] (format "%s/%s" ipfs-api path))

(defn add-to-ipfs
  ""
  [multipart]
  (client/post
    (ipfs-api-path "/api/v0/add?wrap-with-directory")
    multipart))

(defn get-files "" [dir] (hide-dirs (file-seq (clojure.java.io/file dir))))

(defn to-local-filename
  ""
  [dir file]
  (let [filename (.getCanonicalPath ^java.io.File file)]
    ; TODO dir might have trailing / or not...
    (clojure.string/replace filename (str dir "/") "")))

(defn ndjson-str-to-map
  ""
  [string]
  (mapv parse-string (clojure.string/split-lines string)))

(defn files-to-multipart
  ""
  [dir files]
  {:multipart
   (let [files (mapv #(into {} {:name (to-local-filename dir %)
                               :content %}) files)]
                (hide-dotfiles-from-files-map files))})

(defn get-hash-of-last
  ""
  [m]
  (get-in (last m) ["Hash"]))

(defn add "" [directory]
  (log (str "adding website to ipfs " directory))
  (let [files (get-files directory)]
    (get-hash-of-last
      (ndjson-str-to-map
        (:body
          (add-to-ipfs
            (files-to-multipart directory files)))))))
