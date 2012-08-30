(ns pedantic.core)

(defn- group
  [group-artifact]
    (or (namespace group-artifact) (name group-artifact)))

(defn similiar [[dep version & opts] [sdep sversion & sopts]]
  (let [om (apply hash-map opts)
        som (apply hash-map sopts)]
    (and (= (group dep)
            (group sdep))
         (= (name dep)
            (name sdep))
         (= (:extension om)
            (:extension som))
         (= (:classifier om)
            (:classifier som)))))

(defn lower? [v1 v2]
  (< (.compareToIgnoreCase v1 v2) 0))

(defn higher? [v1 v2]
  (> (.compareToIgnoreCase v1 v2) 0))

(defn breaks-expectation? [[_ real-v] [_ expected-v] top-level]
  (or (lower? real-v expected-v)
      (and (higher? real-v expected-v) top-level)))


(defn overrulled? [real-dep [parent coords :as individual]]
  (some #(and (not= real-dep %)
              (similiar real-dep %)
              (breaks-expectation? real-dep % (= parent %))
              %) coords))

(defn overrulled-versions? [collective-deps individual-deps]
  (filter identity
          (for [real-dep collective-deps
                individual individual-deps]
            (if-let [unexpected (overrulled? real-dep individual)]
              (vector real-dep (let [parent (first individual)]
                                 (if-not (= parent unexpected)
                                   parent)) unexpected)))))

(defn find-parent [node collective-deps]
  (first (filter identity (for [[dep transative-deps] collective-deps]
                            (if (contains? transative-deps node)
                              dep)))))

(defn determine-overrulled [collective-deps individual-deps]
  (for [[v a b]  (overrulled-versions? (keys collective-deps) individual-deps)]
    (vector (vector (find-parent v collective-deps) v) (vector a b))))