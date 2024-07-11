use std::fs;
use syn::{File, parse_file};

use syn_serde::json;

fn main() {
  let filename = std::env::args().nth(1).expect("no filename given");
  let content = fs::read_to_string(&filename).expect("cannot read file");

  let ast: File = parse_file(&content).expect("cannot parse file");
  let buf = json::to_string_pretty(&ast);

  // output json in console
  println!("{}", buf);
}
