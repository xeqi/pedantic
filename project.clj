(defproject pedantic "0.2.1-SNAPSHOT"
  :description "A Clojure library designed to be used with pomegrante to check for common unexpected dependency problems."
  :url "https://github.com/xeqi/pedantic"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.cemerick/pomegranate "0.4.0-SNAPSHOT"]]
  :profiles {:1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}}
  :aliases {"all" ["with-profile" "test:test,1.5:test,1.6"]})
