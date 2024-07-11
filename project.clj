(defproject ts2rust "0.1.0-SNAPSHOT"
  :description "TypeScript to Rust transpiler"
  :dependencies [
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.5.0"]]
  :main ^:skip-aot ts2rust.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
