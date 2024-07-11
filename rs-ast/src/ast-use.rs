use react::{createContext, useContext, useEffect};
use react::useRef;
use ramda::*;

fn foo() {
  println!("foo");
}

fn bar() {
  println!("bar");
}

let add = |a: i32, b: i32| -> i32 {
  a + b
};

fn baz(arg: i32) {
  let result = arg * 2;
  const result2: i32 = add(result, 10);
  useRef.test("hello");
  return result2;
}