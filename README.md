
# pedantic

A Clojure library designed to be used with pomegrante to check for common unexpected dependency problems.

## Usage

Use the transformer as part of the pomegranate dependency resolution.  Include two atoms of lists in order to pass information after resolution.

```
(require '[cemerick.pomegranate.aether :as aether])
(require '[pedantic.core :as pedantic])

(def ranges (atom []))
(def overrides (atom []))

(aether/resolve-dependencies
 :coordinates coords
 :repository-session-fn
 #(-> %
      aether/repository-session
      (pedantic/use-transformer ranges
                                overrides)))
```

After resolution:
  ```ranges``` will be a vector of paths (maps with keys ```[:node :parents]```)
  ```overrides``` will be a vector of maps with keys ```[:accepted :ignoreds :ranges]```.  ```:accepted``` is the path that was resolved. ```:ignored``` is a list of paths that were not used. ```:ranges``` is a list of paths containing version ranges that might have affected the resolution.

The nodes included will be ```org.eclipse.aether.graph.DependencyNode```s.

## License

Copyright © 2014 Nelson Morris

Distributed under the Eclipse Public License, the same as Clojure.
