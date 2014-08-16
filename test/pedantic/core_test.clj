(ns pedantic.core-test
  (:use clojure.test
        pedantic.helper))

(def repo
  '{[a "1"] []
    [a "2"] []
    [aa "2"] [[a "2"]]
    [range "1"] [[a "[1,)"]]
    [range "2"] [[a "[2,)"]]})

(use-fixtures :once (add-repo repo))
(use-fixtures :each clear-tmp)
(use-fixtures :each reset-state)


(deftest top-level-overrides-transative-later
  (resolve-deps '[[a "1"]
                  [aa "2"]])
  (is (= @ranges []))
  (is (= (translate @overrides)
         '[{:accepted {:node [a "1"]
                       :parents []}
            :ignoreds [{:node [a "2"]
                        :parents [[aa "2"]]}]
            :ranges []}])))

(deftest ranges-are-found
  (resolve-deps '[[range "1"]])
  (is (= (translate @ranges) '[{:node [a "1"]
                               :parents [[range "1"]]}
                               {:node [a "2"]
                               :parents [[range "1"]]}]))
  (is (= @overrides
         [])))

(deftest range-causes-other-transative-to-ignore-top-level
  (resolve-deps '[[a "1"]
                  [aa "2"]
                  [range "2"]])
  (is (= (translate @ranges) '[{:node [a "2"]
                                :parents [[range "2"]]}]))
  (is (= (translate @overrides)
         '[{:accepted {:node [a "2"]
                       :parents [[aa "2"]]}
            :ignoreds [{:node [a "1"]
                        :parents []}]
            :ranges [{:node [a "2"]
                      :parents [[range "2"]]}]}])))
