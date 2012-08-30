(ns pedantic.core-test
  (:use clojure.test
        pedantic.core
        pedantic.helper))

(def repo
  '{[a "1"] []
    [a "2"] []
    [a "3"] []
    [b "1"] [[a "1"]]
    [c "1"] [[a "2"]]
    [c "2"] [[a "3"]]
    [d "1"] [[a "[2]"]]
    [e "1"] [[c "1"]]
    [f "1"] [[c "2"]]})

(use-fixtures :once (add-repo repo))
(use-fixtures :each clear-tmp)


(defn map-to-deps [coords]
  (into {} (map #(vector % (resolve-deps [%])) coords)))

(deftest top-level-soft-vs-transative-one-layer-soft
  (let [deps '[[a "1"] [c "1"]]]
    (is (= '[[[[a "1"]]
              [[c "1"] [a "2"]]]]
           (determine-overrulled (resolve-deps deps)
                                 (map-to-deps deps))))))

(deftest transative-one-layer-soft-vs-transative-one-layer-soft
  (let [deps '[[b "1"] [c "1"]]]
    (is (= '[[[[b "1"] [a "1"]]
              [[c "1"] [a "2"]]]]
           (determine-overrulled (resolve-deps deps)
                                 (map-to-deps deps))))))

(deftest transative-one-layer-soft-vs-transative-one-layer-soft-ignored
  (let [deps '[[c "1"] [b "1"]]]
    (is (= '[]
           (determine-overrulled (resolve-deps deps)
                                 (map-to-deps deps))))))

(deftest top-level-soft-vs-transative-hard
  (let [deps '[[a "1"] [d "1"]]]
    (is (= '[[[[d "1"] [a "2"]]
              [[a "1"]]]]
           (determine-overrulled (resolve-deps deps)
                                 (map-to-deps deps))))))

(deftest show-whole-parent-heirarchy
  (let [deps '[[e "1"] [a "1"]]]
    (is (= '[[[[a "1"]]
              [[e "1"] [c "1"] [a "2"]]]]
           (determine-overrulled (resolve-deps deps)
                                 (map-to-deps deps))))))

(deftest clip-subtrees
  (let [deps '[[e "1"] [f "1"]]]
    (is (= '[[[[e "1"] [c "1"]] [[f "1"] [c "2"]]]]
           (determine-overrulled (resolve-deps deps)
                                 (map-to-deps deps))))))

(comment
  (let [deps '[[org.clojure/clojure "1.4.0"]
               [com.cemerick/friend "0.0.9"]
               [noir "1.3.0-beta9"]]
        resolve-deps (fn [x] (cemerick.pomegranate.aether/resolve-dependencies
                             :coordinates x
                             :repositories (merge cemerick.pomegranate.aether/maven-central
                                                  {"clojars" "http://clojars.org/repo"})))
        map-to-deps (fn [coords] (into {} (map #(vector % (resolve-deps [%])) coords)))]
    (determine-overrulled (resolve-deps deps)
                          (map-to-deps deps))))