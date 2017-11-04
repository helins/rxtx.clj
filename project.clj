(defproject dvlopt/clotty
            "0.0.0-alpha1"

  :description  "https://github.com/dvlopt/clotty"
  :url          "Clojure lib for serial port IO"
  :license      {:name "Eclipse Public License"
                 :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.openmuc/jrxtx "1.0.0"]]
  :profiles     {:dev     {:source-paths ["dev"]
                           :main         user
                           :dependencies [[org.clojure/clojure    "1.9.0-beta3"]
                                          [org.clojure/test.check "0.10.0-alpha2"]
                                          [criterium              "0.4.4"]]
                           :plugins      [[venantius/ultra "0.5.1"]
                                          [lein-codox      "0.10.3"]]
                           :codox        {:output-path  "doc/auto"
                                          :source-paths ["src"]}
                           :global-vars  {*warn-on-reflection* true}}})
