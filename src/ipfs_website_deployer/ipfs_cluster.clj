(ns ipfs-website-deployer.ipfs-cluster
  (:require [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [ipfs-website-deployer.env :refer [env]]
            [ipfs-website-deployer.logger :refer [log]]))

(def ipfs-cluster-api (env "IPFS_CLUSTER_API"))

(defn ipfs-cluster-api-path [path]
  (format "%s/%s" ipfs-cluster-api path))

;; TODO needs to support basic-auth
(defn read-pin
  "Makes HTTP GET request to cluster for getting pinning status"
  [cid]
  (client/get
    (ipfs-cluster-api-path (str "/pins/" cid))
    {:as :json}
    ; {:basic-auth "user:pass"}
    ))

(defn fully-pinned?
  "Checks if a hash-map has the reply from all peer that its been pinned"
  [res]
  (= (:status (first (vals res))) "pinned"))

(defn is-pinned?
  "Checks if a CID is pinned in all cluster peers"
  [cid]
  (fully-pinned? (get-in (read-pin cid) [:body :peer_map])))

(defn wait-for-pinned
  "Blocks until the CID has been pinned"
  [cid]
  (loop []
    (when-not (is-pinned? cid)
      (do
        (log (str cid " not yet pinned"))
        (Thread/sleep 1000)
        (recur)))))

(defn pin
  "Makes HTTP POST request to cluster for pinning"
  [cid]
  (do
    (client/post
      (ipfs-cluster-api-path (str "/pins/" cid))
      ; {:basic-auth "user:pass"}
      )
    (wait-for-pinned cid)
    cid))
