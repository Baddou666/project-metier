const fs = require("fs");
const path = require("path");

const source = path.join(process.cwd(), "server.prod.cjs");
const target = path.join(process.cwd(), "dist", "server.cjs");

fs.copyFileSync(source, target);
