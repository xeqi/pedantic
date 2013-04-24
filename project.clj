(defproject pedantic "0.0.3-SNAPSHOT"
  :description "Checkout out dependency graphs for common unexpected cases"
  :url "https://github.com/xeqi/pedantic"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.cemerick/pomegranate "0.2.0"]]
  :profiles {:1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}}
  :aliases {"all" ["with-profile" "test:test,1.5"]})
