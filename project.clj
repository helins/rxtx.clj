(defproject dvlopt/rxtx
            "1.0.0"

  :description  "https://github.com/dvlopt/rxtx"
  :url          "Serial IO based on RXTX"
  :license      {:name "Eclipse Public License"
                 :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[dvlopt/void       "0.0.0"]
                 [org.openmuc/jrxtx "1.0.1"]]
  :profiles     {:dev     {:source-paths ["dev"]
                           :main         user
                           :dependencies [[criterium              "0.4.4"]
                                          [org.clojure/clojure    "1.9.0"]
                                          [org.clojure/test.check "0.10.0-alpha2"]]
                           :plugins      [[lein-codox      "0.10.3"]
                                          [venantius/ultra "0.5.1"]]
                           :codox        {:output-path  "doc/auto"
                                          :source-paths ["src"]}}})
