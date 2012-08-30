(ns pedantic.core-test
  (:use clojure.test
        pedantic.core
        pedantic.helper))

(def repo
  '{[a "1"] []
    [a "2"] []
    [b "1"] [[a "1"]]
    [c "1"] [[a "2"]]
    [d "1"] [[a "[2]"]]})

(use-fixtures :once (add-repo repo))
(use-fixtures :each clear-tmp)


(defn map-to-deps [coords]
  (into {} (map #(vector % (keys (resolve-deps [%]))) coords)))

(deftest top-level-soft-vs-transative-one-layer-soft
  (let [deps '[[a "1"] [c "1"]]]
    (is (= '[[[nil [a "1"]]
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
              [nil [a "1"]]]]
           (determine-overrulled (resolve-deps deps)
                                 (map-to-deps deps))))))