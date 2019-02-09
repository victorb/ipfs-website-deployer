(ns ipfs-website-deployer.tmp
  (:require [clj-docker-client.core :as docker]))

(def conn (docker/connect))

(comment
  (let [id (docker/create
             conn
             "ubuntu:bionic"
             "bash -c 'whoami > /tmp/user'"
             {} {})]
    (docker/cp conn id "./src" "/tmp")
    ;; copy build directory to container
    ;; (docker/start conn "13cac051f628")
    ;; (copy-to-host conn id "/tmp/user" "./tmp")
    (println id)))
