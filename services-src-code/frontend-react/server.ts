import express from "express";
import path from "path";

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(express.json());

  app.get("/health", (req, res) => {
    res.json({ status: "online", service: "ai-text-detector" });
  });

  app.get("/runtime-config.js", (req, res) => {
    res.type("application/javascript");
    res.send(`window.__APP_CONFIG__ = ${JSON.stringify({
      API_URL: process.env.API_URL || "",
    })};`);
  });

  // Serve static files in production
  if (process.env.NODE_ENV === "production") {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    // SPA fallback: handle all non-API routes by returning index.html
    app.get("*", (req, res) => {
       res.sendFile(path.join(distPath, "index.html"));
    });
  } else {
    // Vite middleware for development
    const { createServer: createViteServer } = await import("vite");
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`AI Text Detector running on http://$localhost:${PORT}`);
  });
}

startServer();
