(ns pedantic.path
  "A path is {:node DependencyNode :parents [DependencyNode]}.  This represents
a point in the dependency tree.")

(defn all-paths
  "Breadth first traversal of the graph from DependencyNode node.
Short circuits a path when a cycle is detected."
  [node]
  (loop [paths [{:node node :parents []}]
         results []]
    (if (empty? paths)
      results
      (recur (for [{:keys [node parents]} paths
                   :when (not (some #{node} parents))
                   c (.getChildren node)]
               {:node c
                :parents (conj parents node)})
             (doall (concat results paths))))))

(defn range?
  "Does the path point to a DependencyNode asking for a version range?"
  [{:keys [node]}]
  (when-let [vc (.getVersionConstraint node)]
    (not (empty? (.getRanges vc)))))

(defn top-level?
  "Is the path a top level dependency in the project?"
  [{:keys [parents]}]
  ;; Parent is root node
  (= 1 (count parents)))
