(defproject ipfs-website-deployer "0.1.0-SNAPSHOT"
  :description "Deploy websites via IPFS"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ;; was 1.8.0 before!
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [cheshire "5.8.1"]
                 [ring "1.7.1"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-core "1.6.3"]
                 [hiccup "1.0.0"]
                 [clj-http "3.9.1" :exclusions [commons-io]]
                 [io.forward/yaml "1.0.9"]
                 [clj-jgit "0.8.10"]
                 [irresponsible/tentacles "0.6.3" :exclusions [commons-io]]
                 [iapetos "0.1.8"]
                 [environ "1.1.0"]
                 [clj-jwt "0.1.1"]
                 [ring-oauth2 "0.1.4" :exclusions [joda-time clj-time]]
                 [crypto-random "1.2.0"]
                 [ring/ring-defaults "0.3.1" :exclusions [hiccup]]
                 [org.clojure/core.incubator "0.1.4"]
                 [lispyclouds/clj-docker-client "0.1.10"]
                 [byte-streams "0.2.4"]
                 [org.apache.commons/commons-compress "1.18"]
                 [lynxeyes/dotenv "1.0.2"]]
  :plugins [[lein-ring "0.12.4"]]
  :min-lein-version "2.8.1"
  :global-vars {*warn-on-reflection* true}
  ;; :profiles {:uberjar {:aot :all}}
  :target-path "target/%s/"
  ;; :aot [ipfs-website-deployer.cli]
  :main ipfs-website-deployer.cli
  ;; :repl-options { :init-ns ipfs-website-deployer.docker }
  :ring {:handler ipfs-website-deployer.core/app})
