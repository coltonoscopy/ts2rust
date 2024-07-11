const ts = require('typescript');
const fs = require('fs');
const { inspect } = require('util');

const fileName = process.argv[2];
const sourceFile = ts.createSourceFile(
    fileName,
    fs.readFileSync(fileName).toString(),
    ts.ScriptTarget.Latest,
    true
);

const circularReference = sourceFile;
circularReference.myself = circularReference;

const getCircularReplacer = () => {
  const seen = new WeakSet();
  return (key, value) => {
    if (key === 'pos' || key === 'end' || key === 'id' || key === 'modifierFlagsCache' || key === 'transformFlags') {
      return;
    }
    if (typeof value === "object" && value !== null) {
      if (seen.has(value)) {
        return;
      }
      seen.add(value);
    }
    return value;
  };
};

const stringified = JSON.stringify(circularReference, getCircularReplacer(), 2);
console.log(stringified);