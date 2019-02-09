(ns ipfs-website-deployer.config
  (:refer-clojure :exclude [load])
  (:require [yaml.core :as yaml]
            [clojure.pprint :refer [pprint]]))

; config is yaml files that has the following look:
; website: peerpad.net
; build_directory: build/
; build_command: make build
; branches:
;   production: peerpad.net
;   master: dev.peerpad.net

(defn long-str [& strings] (clojure.string/join "\n" strings))

(def example-config
  (long-str "website: peerpad.net"
            "build_directory: build/"
            "build_command: make build"
            "# branches:"
            "#   production: peerpad.net"
            "#   master: dev.peerpad.net"
            ))

(defn load-config
  [directory]
  (yaml/from-file (str directory "/.github/website.yaml")))

(def build-dir "/home/user/projects/victorb/test-website")
