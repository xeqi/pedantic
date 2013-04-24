(ns pedantic.core
  (:import (org.sonatype.aether.collection DependencyGraphTransformer)
           (org.sonatype.aether.util.graph.transformer
            ConflictIdSorter
            TransformationContextKeys)))

(defn range? [{:keys [node]}]
  (if-let [vc (.getVersionConstraint node)]
    (not (empty? (.getRanges vc)))))

(defn node->artifact-map [node]
  (if-let [d (.getDependency node)]
    (if-let [a (.getArtifact d)]
      (let [b (bean a)]
        (-> b
            (select-keys [:artifactId :groupId :exclusions :version :extension :properties])
            (update-in [:exclusions] vec))))))

(defn node= [n1 n2]
  (= (node->artifact-map n1)
     (node->artifact-map n2)))

(defn get-sorted-conflict-ids [node context]
  (if-let [ids (.get context
                     TransformationContextKeys/SORTED_CONFLICT_IDS)]
    ids
    (do (-> (ConflictIdSorter.)
            (.transformGraph node context))
        (.get context
              TransformationContextKeys/SORTED_CONFLICT_IDS))))

(defn get-conflict-id-map [context]
  (.get context TransformationContextKeys/CONFLICT_IDS))

(defn all-nodes [node]
  (loop [nodes [{:node node :parents []}]
         res []]
    (if (not (empty? nodes))
      (recur (mapcat (fn [{:keys [node parents]}]
                       ;;check for recursive dependencies
                       (if-not (some #{node} parents)
                         (for [c (.getChildren node)]
                           {:node c
                            :parents (conj parents node)}))) nodes)
             (concat res nodes))
      res)))

(defn id-to-nodes [node context]
  (let [sorted-ids (get-sorted-conflict-ids node context)
        conflict-ids (get-conflict-id-map context)
        nodes (all-nodes node)]
    (apply merge-with concat
           (map (fn [{:keys [node parents] :as x}]
                  {(.get conflict-ids node)
                   [x]})
                nodes))))

(defn lt [node1 node2]
  (< (compare (.getVersion node1) (.getVersion node2)) 0))

(defn mention [{:keys [node parents] :as n} conflicting-nodes
               overrides ranges]
  (let [nodes (remove range? (conflicting-nodes node))
        rest (remove #(= (:node %) node) nodes)
        m (filter (fn [x]
                    (or (lt node (:node x))
                        (= 1 (count (:parents x))))) rest)]
    (if (not (empty? m))
      (swap! overrides conj {:accepted n
                             :ignoreds m
                             :ranges
                             (filter #(node= (:node %)
                                             (:node n)) ranges)})
      (doseq [c (.getChildren node)]
        (mention {:node c
                  :parents (conj parents node)}
                 conflicting-nodes
                 overrides
                 ranges)))))

(defn use-transformer
  "Use the pedantic transformer.  This will wrap the current one in the session.

ranges and overrides are expect to be (atom []).  This allows setting the values there since the return value can't be used here.

After resolution:
  ranges will be a vector of maps with keys [:node :parents]
  overrides will be a vector of maps with keys [:accepted :ignoreds :ranges].  :accepted is the map that was resolved. :ignored is a list of maps that were not used. :ranges is a list of maps containing version ranges that might have affected the resolution."
  [session ranges overrides]
  (let [transformer (.getDependencyGraphTransformer session)]
    (.setDependencyGraphTransformer
     session
     (reify DependencyGraphTransformer
       (transformGraph [self node context]
         (reset! ranges (filter range? (all-nodes node)))
         (let [conflictmap (id-to-nodes node context)
               conflict-id-map (get-conflict-id-map context)]
           (.transformGraph transformer node context)
           (mention {:node node
                     :parents []}
                    #(conflictmap
                      (.get conflict-id-map %))
                    overrides
                    @ranges))
         node)))))