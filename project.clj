(defproject dvlopt/clotty
            "0.0.0-alpha0"

  :description  "https://github.com/dvlopt/clotty"
  :url          "Clojure lib for serial port IO"
  :license      {:name "Eclipse Public License"
                 :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins      [[lein-codox "0.10.3"]]
  :codox        {:output-path  "doc/auto"
                 :source-paths ["src"]}
  :dependencies [[org.openmuc/jrxtx "1.0.0"]]
  :profiles     {:dev     {:source-paths ["dev"]
                           :main         user
                           :plugins      [[venantius/ultra "0.5.1"]
                                          [lein-midje      "3.0.0"]]
                           :dependencies [[org.clojure/clojure    "1.9.0-alpha19"]
                                          [org.clojure/spec.alpha "0.1.123"]
                                          [org.clojure/test.check "0.9.0"]
                                          [criterium              "0.4.4"]]
                           :global-vars  {*warn-on-reflection* true}}})
