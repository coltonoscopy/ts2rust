use std;
use std::*;
use std::fs;
use syn::{File, parse_file};
use fs::func as fs_func;
use fs::{func as fs_func, func2 as fs_func2};

use syn_serde::json;

fn main() {
  let filename = std::env::args().nth(1).expect("no filename given");
  let content = fs::read_to_string(&filename).expect("cannot read file");

  let ast: File = parse_file(&content).expect("cannot parse file");
  let buf = json::to_string_pretty(&ast);

  fs::write("ast.json", buf).expect("cannot write file");
}