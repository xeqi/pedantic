(ns pedantic.node
  "Helper functions for DependencyNode")

(defn node->artifact-map
  [node]
  (if-let [d (.getDependency node)]
    (if-let [a (.getArtifact d)]
      (let [b (bean a)]
        (-> b
            (select-keys [:artifactId :groupId :exclusions :version :extension :properties])
            (update-in [:exclusions] vec))))))

(defn node=
  "Check value equality instead of reference equality."
  [n1 n2]
  (= (node->artifact-map n1)
     (node->artifact-map n2)))

(defn lt
  "Is the version of node1 < version of node2."
  [node1 node2]
  (< (compare (.getVersion node1) (.getVersion node2)) 0))
