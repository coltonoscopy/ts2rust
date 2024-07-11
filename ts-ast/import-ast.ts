import { createContext, useContext, useEffect } from 'react';
import fs from 'fs';
import * as R from 'ramda';

function foo() {
  console.log('foo');
}

const bar = () => {
  console.log('bar');
};

function baz(arg: number) {
  const doc = { name: 'John' };
  let result = arg * 2;
  const result2 = R.add(result, 10);
  return result2; 
}