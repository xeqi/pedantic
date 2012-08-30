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


(defn overrulled? [real-dep [parent depmap :as individual]]
  (some #(and (not= real-dep %)
              (similiar real-dep %)
              (breaks-expectation? real-dep % (= parent %))
              %) (keys depmap)))

(defn find-parent [node collective-deps]
  (first (filter identity (for [[dep transative-deps] collective-deps]
                            (if (contains? transative-deps node)
                              dep)))))

(defn find-parents [node collective-deps]
  (loop [node node
         children '()]
    (if-let [parent (first (filter identity
                                   (for [[dep transative-deps] collective-deps]
                                     (if (contains? transative-deps node)
                                       dep))))]
      (recur parent (conj children node))
      (conj children node))))

(defn overrulled-versions? [collective-deps individual-deps]
  (filter identity
          (for [real-dep collective-deps
                individual individual-deps]
                (if-let [unexpected (overrulled? real-dep individual)]
                  (vector real-dep (find-parents unexpected (second individual)))))))

(defn sublist? [l1 l2]
  (= (take (count l1) l2) l1))

(defn is-higher-override? [[o e :as d1] [o2 e2 :as d2]]
  (and (not= d1 d2)
       (sublist? o o2)
       (sublist? e e2)))

(defn remove-subtrees [overrides]
  (remove #(some (fn [x] (is-higher-override? x %)) overrides) overrides))

(defn determine-overrulled [collective-deps individual-deps]
  (remove-subtrees
   (for [[real over]  (overrulled-versions? (keys collective-deps) individual-deps)]
     (vector (find-parents real collective-deps) over))))