// Controle de syntaxe : parse CST de chaque fichier .java avec java-parser.
// Usage : node parse.js <rootDir>
const fs = require("fs");
const path = require("path");
const { parse } = require("java-parser");

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else if (entry.name.endsWith(".java")) out.push(full);
  }
  return out;
}

const roots = process.argv.slice(2);
const files = roots.flatMap((root) => walk(root));
let failures = 0;
for (const file of files) {
  const source = fs.readFileSync(file, "utf8");
  try {
    parse(source);
  } catch (error) {
    failures += 1;
    const message = String(error.message || error).split("\n").slice(0, 6).join(" | ");
    console.log(`SYNTAX FAIL ${file}\n   ${message}`);
  }
}
console.log(`parsed ${files.length} java files, ${failures} syntax failures`);
process.exit(failures === 0 ? 0 : 1);
