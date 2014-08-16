(ns pedantic.core
  "The end goal for pedantic is to hook into Aether's dependency
resolution and provide feedback about the dependency tree. Using a
`DependencyGraphTransformer` allows us to look at the tree both before
and after conflict resolution so that downloading all of the
dependencies only occurs once.

Aether uses a `NearestVersionConflictResolver` to resolve which
versions to use in case of a conflict. The
`NearestVersionConflictResolver` uses a `ConflictIdSorter` to
determine those, and it will save the information in
`SORTED_CONFLICT_IDS` and `CONFLICT_IDS`. We can similarly use the
conflict information to determine which version is choosen in a
conflict.

Additional important classes from Aether:

* `DependencyGraphTransformationContext`
* `DependencyNode`
* `Dependency`
* `Artifact`
* `Version`
* `VersionConstraint`"
  (:import (org.sonatype.aether.collection DependencyGraphTransformer)
           (org.sonatype.aether.util.graph.transformer
            ConflictIdSorter
            TransformationContextKeys))
  (:require [pedantic.node :as node]
            [pedantic.path :as path]))

(defn initialize-conflict-ids!
  "Make sure that `SORTED_CONFLICT_IDS` and `CONFLICT_IDS` have been
initialized. Similar to what a NearestVersionConflictResolver will do."
  [node context]
  (when-not (.get context TransformationContextKeys/SORTED_CONFLICT_IDS)
    (-> (ConflictIdSorter.)
        (.transformGraph node context))))

(defn set-ranges!
  "Set ranges to contain all paths that asks for a version range"
  [ranges paths]
  (reset! ranges (doall (filter path/range? paths))))

(defn set-overrides!
  "Check each `accepted-path` against its conflicting paths. If a
conflicting path fails the pedantic criteria then add information
representing this possibly confusing situation to `overrides`."
[overrides conflicts accepted-paths ranges]
  (doseq [{:keys [node parents] :as path} accepted-paths]
    (let [ignoreds (for [conflict-path (conflicts node)
                         :when (and (not= path conflict-path)
                                    ;; This is the pedantic criteria
                                    (or (node/lt node (:node conflict-path))
                                        (path/top-level? conflict-path)))]
                     conflict-path)]
      (when (not (empty? ignoreds))
        (swap! overrides conj {:accepted path
                               :ignoreds ignoreds
                               :ranges
                               (filter #(node/node= (:node %) node) ranges)})))))

(defn transform-graph
  "Examine the tree with root `node` for version ranges, then
allow the original `transformer` to perform resolution, then check for
overriden dependencies."
  [ranges overrides node context transformer]
  ;; Force initialization of the context like NearestVersionConflictResolver
  (initialize-conflict-ids! node context)
  ;; Get all the paths of the graph before dependency resolution
  (let [potential-paths (path/all-paths node)]
    (set-ranges! ranges potential-paths)
    (.transformGraph transformer node context)
    ;; The original transformer should have done dependency resolution,
    ;; so now we can gather just the accepted paths and use the ConflictId
    ;; to match against the potential paths
    (let [node->id (.get context TransformationContextKeys/CONFLICT_IDS)
          id->paths (reduce (fn [acc {:keys [node] :as path}]
                              (update-in acc [(.get node->id node)] conj path))
                            {}
                            ;; Remove ranges as they cause problems and were
                            ;; warned above
                            (remove path/range? potential-paths))]
      (set-overrides! overrides
                      #(->> % (.get node->id) id->paths)
                      (path/all-paths node)
                      @ranges))))

;; API
;;------------------------------------------------------------------------------

(defn use-transformer
  "Wrap the session's current `DependencyGraphTransformer` with one that checks
for version ranges and overriden dependencies.

`ranges` and `overrides` are expect to be (atom []).  This provides a way to
send back information since the return value can't be used here.

After resolution:
  `ranges` will be a vector of paths (see pedantic.path)
  `overrides` will be a vector of maps with keys [:accepted :ignoreds :ranges].
    `:accepted` is the path that was resolved. :ignored is a list of
    paths that were not used.
    `:ranges` is a list of paths containing version ranges that might
    have affected the resolution."
  [session ranges overrides]
  (let [transformer (.getDependencyGraphTransformer session)]
    (.setDependencyGraphTransformer
     session
     (reify DependencyGraphTransformer
       (transformGraph [_ node context]
         (transform-graph ranges overrides node context transformer)
         ;;Return the DependencyNode in order to meet
         ;;transformGraph's contract
         node)))))
