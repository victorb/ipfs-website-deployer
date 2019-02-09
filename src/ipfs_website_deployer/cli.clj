(ns ipfs-website-deployer.cli
  (:use [ipfs-website-deployer.docker :only [build]]
        [ipfs-website-deployer.dnsimple :only [set-dnslink]]
        [ipfs-website-deployer.config :only [load-config]]
        [ipfs-website-deployer.ipfs :only [add]]
        [ipfs-website-deployer.ipfs-cluster :only [pin]])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [ipfs-website-deployer.env :refer [env]])
  (:gen-class))

(defn -main [& args]
  (let [directory (first args)]
    (let [config (load-config directory)]
      (try
        (println "running all things")
        (set-dnslink config (pin (add (build directory config))))
      (finally
        (println "error happened, shutting down")
        (shutdown-agents))))))
