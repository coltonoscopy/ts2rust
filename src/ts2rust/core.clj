(ns ts2rust.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.walk :as walk] 
            [clojure.string :refer [join split lower-case]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :refer [reader]]))

(defn read-json [filename]
  (with-open [r (reader filename)]
    (json/read r :key-fn keyword)))

(def ts-ast (read-json "ts-ast/import-ast.json"))

;; convert camel-case variable name to snake case
(defn camel->snake [s]
  (->> (split s #"(?<=[a-z])(?=[A-Z])")
       (join "_")
       (lower-case)))

;; typescript ASTs spit out numbers that map to an enum in the typescript compiler
;; it's raw in the JSON, so we need to map it back to a keyword for readability
(def syntax-kind-map
  {0   :Unknown
   1   :EndOfFileToken
   2   :SingleLineCommentTrivia
   3   :MultiLineCommentTrivia
   4   :NewLineTrivia
   5   :WhitespaceTrivia
   6   :ShebangTrivia
   7   :ConflictMarkerTrivia
   9   :NumericLiteral
   11  :StringLiteral
   12  :JsxText
   14  :RegularExpressionLiteral
   16  :TemplateHead
   18  :TemplateTail
   28  :CommaToken
   29  :QuestionDotToken
   37  :EqualsEqualsEqualsToken
   38  :ExclamationEqualsEqualsToken
   39  :EqualsGreaterThanToken
   40  :PlusToken
   42  :AsteriskToken
   56  :AmpersandAmpersandToken
   57  :BarBarToken
   58  :QuestionToken
   59  :ColonToken
   64  :EqualsToken
   80  :Identifier
   97  :FalseKeyword
   103 :InKeyword
   106 :NullKeyword
   110 :ThisKeyword
   112 :TrueKeyword
   150 :NumberKeyword
   166 :QualifiedName
   169 :Parameter
   171 :PropertySignature
   192 :UnionType
   201 :LiteralType
   209 :ArrayLiteralExpression
   210 :ObjectLiteralExpression
   211 :PropertyAccessExpression
   212 :ElementAccessExpression
   213 :CallExpression
   214 :NewExpression
   217 :ParenthesizedExpression
   218 :FunctionExpression
   219 :ArrowFunction
   221 :TypeOfExpression
   224 :PrefixUnaryExpression
   226 :BinaryExpression
   227 :ConditionalExpression
   228 :TemplateExpression
   239 :TemplateSpan
   241 :Block
   243 :VariableStatement
   244 :ExpressionStatement
   245 :IfStatement
   253 :ReturnStatement
   257 :ThrowStatement
   258 :TryStatement
   260 :VariableDeclaration
   261 :VariableDeclarationList 
   262 :FunctionDeclaration
   264 :InterfaceDeclaration
   272 :ImportDeclaration
   273 :ImportClause
   274 :NamespaceImport
   275 :NamedImports
   276 :ImportSpecifier
   277 :ExportAssignment
   284 :JsxElement
   286 :JsxOpeningElement
   287 :JsxClosingElement
   292 :JsxAttributes
   294 :JsxExpression
   299 :CatchClause
   303 :PropertyAssignment
   307 :SourceFile
   })

(defn unnest-vector [coll]
  (mapcat #(if (vector? %) % [%]) coll))
(defn rs-ast-root [node] {:items (unnest-vector (:statements node))})
(defn cl-print [x] (doto x (println)))

;; funcs that take in an ast node for TS/JS and return the
;; equivalent Rust AST node
(defn node->unknown [node] node)
(defn node->end-of-file-token [node] node)
(defn node->single-line-comment-trivia [node] node)
(defn node->multi-line-comment-trivia [node] node)
(defn node->new-line-trivia [node] node)
(defn node->whitespace-trivia [node] node)
(defn node->shebang-trivia [node] node)
(defn node->conflict-marker-trivia [node] node)
(defn node->numeric-literal [node]
  {:lit {:int (:text node)}})
(defn node->string-literal [node]
  {:lit {:str (:text node)}})
(defn node->import-declaration [node]
  {:use
   {:tree
    {:path
     {:ident (get-in node [:moduleSpecifier :lit :str]) :tree (:importClause node)}}}})

;; if namedBindings, check for `elements` (NamedImports) or `name` (NamespaceImport)
;; if no namedBindings, check just for `name` (Identifier)
(defn node->import-clause [node]
  (if-let [namedBindings (:namedBindings node)]
    (if-let [group (:group namedBindings)]
      {:group group}
      {:name (:name namedBindings)})
    {:name (:name node)}))

(defn node->call-expression [node]
  (let [expr (:expression node)
        receiver (get-in expr [:expr :ident])
        method (get-in expr [:path :ident])
        args (:arguments node)]
  {:method_call {:receiver receiver :method method :args args}}))

(defn node->parenthesized-expression [node] node)
(defn node->function-expression [node] node)
(defn node->block [node] (unnest-vector (:statements node)))

;; return `:declarationList` if it exists, else the node itself
(defn node->variable-statement [node]
  (if-let [declarationList (:declarationList node)]
    declarationList
    node))
(defn node->expression-statement [node] (:expression node))
(defn node->import-specifier [node] (:name node))
(defn node->identifier [node]
  {:ident (camel->snake (:escapedText node))})
(defn node->named-imports [node]
  {:group (:elements node)})
(defn node->namespace-import [_]
  {:ident "*"})
(defn node->function-declaration [node]
  (let [body (:body node)]
    {:fn
     {:ident (:name node) :inputs (:parameters node)
      :stmts (vec body)}}))
(defn node->return-statement [node]
  {:return {:expr {:path {:segments (:expression node)}}}})
(defn node->parameter [node]
  {:typed {:pat (:name node) :ty (:type node)}})
(defn node->number-keyword [_] {:ident "i32"})
(defn node->property-access-expression [node]
  {:expr (:expression node) :path (:name node)})
(defn node->arrow-function [node] (node->function-declaration node))
(defn node->binary-expression [node]
  (let [left (get-in node [:left :ident])
        op (get-in node [:operatorToken :ident])
        right (get-in node [:right])]
    {:expr {:binary {:left left :op op :right right}}}))
(defn node->asterisk-token [_] {:ident "*"})

(defmacro if-let*
  "Multiple binding version of if-let"
  ([bindings then] `(if-let* ~bindings ~then nil))
  ([bindings then else] (if (seq bindings)
                          `(if-let [~(first bindings) ~(second bindings)]
                             (if-let* ~(vec (drop 2 bindings)) ~then ~else)
                           ~else)
                          then)))

;; if `initializer` is present, it's a variable declaration with an assignment
;; and so we need to return `initializer` and insert :name.:ident into it
(defn node->variable-declaration [node]
  (if-let* [initializer (:initializer node)
           func (:fn initializer)]
    (let [ident (get-in node [:name :ident])]
      (assoc-in initializer [:fn :ident] ident))
    (merge (:initializer node) {:ident (get-in node [:name :ident])})))

(defn node->variable-declaration-list [node] (:declarations node))

;; { kind: SyntaxKind.SourceFile, tree: [...] } -> { items: [...] }
(defn node->source-file [node] (rs-ast-root node))

;; a map of ast node kinds to the functions that convert them to Rust AST nodes
(def kind-fn-map
  {:Unknown                   node->unknown
   :EndOfFileToken            node->end-of-file-token
   :SingleLineCommentTrivia   node->single-line-comment-trivia
   :MultiLineCommentTrivia    node->multi-line-comment-trivia
   :NewLineTrivia             node->new-line-trivia
   :WhitespaceTrivia          node->whitespace-trivia
   :ShebangTrivia             node->shebang-trivia
   :ConflictMarkerTrivia      node->conflict-marker-trivia
   :NumericLiteral            node->numeric-literal
   :StringLiteral             node->string-literal
   :ImportDeclaration         node->import-declaration
   :ImportClause              node->import-clause
   :CallExpression            node->call-expression
   :ParenthesizedExpression   node->parenthesized-expression
   :FunctionExpression        node->function-expression
   :Block                     node->block
   :VariableStatement         node->variable-statement
   :ExpressionStatement       node->expression-statement
   :SourceFile                node->source-file
   :NamedImports              node->named-imports
   :Identifier                node->identifier
   :ImportSpecifier           node->import-specifier
   :NamespaceImport           node->namespace-import
   :FunctionDeclaration       node->function-declaration
   :ReturnStatement           node->return-statement
   :Parameter                 node->parameter
   :NumberKeyword             node->number-keyword
   :PropertyAccessExpression  node->property-access-expression
   :ArrowFunction             node->arrow-function 
   :VariableDeclaration       node->variable-declaration
   :VariableDeclarationList   node->variable-declaration-list
   :BinaryExpression          node->binary-expression 
   :AsteriskToken             node->asterisk-token
   })

(defn print-use [node] (str "use " (:use node) ";"))
(defn print-use-group [node]
  (let [joined (join ", " (:group node))]
    (str "{" joined "}")))
(defn print-items [node]
  (let [joined (join "\n" (:items node))]
    (str joined "\n")))
(defn print-ident-tree [node]
  (str (:ident node)
       (if (not= (:tree node) (:ident node))
         (str "::" (or (:tree node) "*"))
         "")))
(defn print-return [node] (str "return " (apply str (:return node)) ";"))
(defn print-statements [stmts] (join "\n" stmts))
(defn print-block [stmts] (str "{\n  " (print-statements stmts) "\n}"))

(defn print-fn [node]
  (let [fn (:fn node)
        name (:ident fn)
        inputs (join ", " (:inputs fn))
        stmts (print-block (:stmts fn))]
    (str "\nfn " name "(" inputs ") " stmts)))

(defn print-method-call [node]
  (let [receiver (:receiver node)
        method (:method node)
        args (join ", " (:args node))]
    (str receiver "." method "(" args ");")))

(defn print-assignment-expression [node]
  (let [expr (:expr node)
        ident (:ident node)]
    (str ident " = " expr ";")))

(defn print-binary-expression [node]
  (let [binary (:binary node)]
    (str (:left binary) " " (:op binary) " " (:right binary))))

(def kind-print-map
  {#{:block}                   print-block
   #{:use}                     print-use
   #{:group}                   print-use-group
   #{:items}                   print-items
   #{:ident}                   #(:ident %)
   #{:ident :tree}             print-ident-tree
   #{:path}                    #(:path %)
   #{:tree}                    #(:tree %)
   #{:name}                    #(:name %)
   #{:fn}                      print-fn
   #{:return}                  print-return
   #{:segments}                #(apply str (:segments %))
   #{:expr}                    #(apply str (:expr %))
   #{:stmts}                   print-statements
   #{:pat :ty}                 #(str (:pat %) ": " (:ty %))
   #{:typed}                   #(:typed %)
   #{:method_call}             #(:method_call %)
   #{:text}                    #(:text %)
   #{:str}                     #(str "\"" (:str %) "\"")
   #{:lit}                     #(:lit %)
   #{:int}                     #(:int %)
   #{:receiver :method :args}  print-method-call
   #{:expr :ident}             print-assignment-expression
   #{:binary}                  print-binary-expression
   #{:method_call :ident}      #(str (:ident %) " = " (:method_call %))
   })

;; walk the typescript AST and convert it to a Rust AST, depth-first
(defn ast-ts->rs [ts-ast]
  (walk/postwalk
   (fn [node]
     (let [kind (syntax-kind-map (:kind node))
           fn (kind-fn-map kind)]
       (if fn
         (fn node)
         node)))
   ts-ast))

;; walk the Rust AST and output it as source code
(defn ast-rs->source [rs-ast]
  (walk/postwalk
   (fn [node]
     (if-let [fn (kind-print-map (if (map? node) (set (keys node)) :Invalid))]
       (fn node)
       node))
   rs-ast))

(def rs-ast (ast-ts->rs ts-ast))
(pprint rs-ast)
(println (ast-rs->source rs-ast))
