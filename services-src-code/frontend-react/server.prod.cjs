const express = require("express");
const path = require("path");

const app = express();
const PORT = 3000;
const distPath = path.join(process.cwd(), "dist");

app.use(express.json());
app.use(express.static(distPath));

app.get("/health", (req, res) => {
  res.json({ status: "online", service: "ai-text-detector-frontend" });
});

app.get("/runtime-config.js", (req, res) => {
  res.type("application/javascript");
  res.send(`window.__APP_CONFIG__ = ${JSON.stringify({
    API_URL: process.env.API_URL || "",
  })};`);
});

app.get("*", (req, res) => {
    res.sendFile(path.join(distPath, "index.html"));
    return;
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`AI Text Detector running on http://localhost:${PORT}`);
});
