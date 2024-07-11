# ts2rust

TypeScript to Rust transpiler written in Clojure

## Installation

### Prerequisites

Technically you don't need TypeScript or Rust installed to run the transpiler itself, but you'll want to install `npm` and `cargo` to run AST generator scripts I've written to get data to actually work with. You can find the TypeScript stuff in `ts-ast` and the Rust stuff in `rs-ast`, which the Clojure code is referencing (I have a couple of very basic programs and ASTs pre-created in the repo, likely to have more in the future).

## Usage

Currently I just use Calva in VS Code to run the forms, since it's in pretty experimental development.