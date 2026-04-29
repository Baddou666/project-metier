window.AiDetectorApi = (() => {
  function normalizeApiResponse(payload, fallbackSourceType) {
    const results = Array.isArray(payload)
      ? payload
      : Array.isArray(payload.results) ? payload.results
      : Array.isArray(payload.items)   ? payload.items
      : [];

    const aiCount = results.filter(r => r.isAi === true).length;
    const humanCount = results.filter(r => r.isAi === false).length;

    const verdict = Array.isArray(payload)
      ? `${results.length} segments analyses`
      : payload.verdict || payload.label || `${results.length} segments analyses`;

    const probability = Array.isArray(payload)
      ? null
      : payload.probability ?? payload.score ?? payload.confidence ?? null;

    const sourceType = Array.isArray(payload)
      ? fallbackSourceType
      : payload.sourceType || fallbackSourceType;

    const details = Array.isArray(payload)
      ? ""
      : payload.details || payload.message || payload.reason || "";

    const lv = String(verdict).toLowerCase();
    let tone = "warning";
    if (lv.includes("humain") || lv.includes("human")) tone = "success";
    else if (lv.includes("ia") || lv.includes("ai")) tone = "danger";

    const chips = [];
    if (sourceType) chips.push(`Source : ${sourceType}`);
    if (details) chips.push(details);

    return {
      verdict,
      tone,
      confidence: probability,
      chips,
      humanCount,
      aiCount,
      totalCount: results.length,
      resultText: results.length
        ? JSON.stringify(results, null, 2)
        : JSON.stringify(payload, null, 2),
      itemResults: results
    };
  }

  async function detectWithBackend({ endpoint, authToken, refreshAuthToken }, sourceType, items, link, isRetry = false) {
    if (!endpoint || endpoint.includes("your-backend")) {
      throw new Error("Le backend REST n'est pas encore configure dans le code de l'extension.");
    }

    if (!authToken()) {
      throw new Error("Token d'authentification non disponible. Veuillez rafraichir la connexion.");
    }

    const headers = {
      "Content-Type": "application/json",
      "Authorization": `Anonym ${authToken()}`
    };

    let response;
    try {
      response = await fetch(endpoint, {
        method: "POST",
        headers,
        body: JSON.stringify({ sourceType, link: link || null, items })
      });
    } catch {
      const isOffline = !navigator.onLine;
      if (isOffline) throw new Error("Aucune connexion reseau detectee. Verifiez votre connexion Internet.");
      throw new Error(`Impossible de joindre le serveur (${endpoint}). Verifiez que le backend est demarre.`);
    }

    if (!isRetry && (response.status === 400 || response.status === 401 || response.status === 403)) {
      console.log(`[API] Erreur ${response.status} - Tentative de rafraichissement du token et retry...`);
      try {
        await refreshAuthToken();
        return detectWithBackend({ endpoint, authToken, refreshAuthToken }, sourceType, items, link, true);
      } catch (tokenErr) {
        console.error("[API] Impossible de rafraichir le token:", tokenErr);
        throw new Error("Session expiree. Impossible de renouveler l'authentification.");
      }
    }

    if (!response.ok) {
      let detail = "";
      try {
        const body = await response.json();
        detail = body.message || body.error || body.detail || "";
      } catch { /* ignore */ }

      const statusMap = {
        400: "Requete invalide (400).",
        401: "Non autorise (401). Verifiez le token API.",
        403: "Acces refuse (403).",
        404: "Endpoint introuvable (404). Verifiez l'URL du backend.",
        422: "Donnees non traitables (422). Format inattendu.",
        429: "Trop de requetes (429). Reessayez dans quelques instants.",
        500: "Erreur interne du serveur (500).",
        502: "Gateway invalide (502). Le serveur backend est peut-etre en cours de demarrage.",
        503: "Service temporairement indisponible (503).",
      };

      const msg = statusMap[response.status] || `Erreur HTTP ${response.status}.`;
      const fullMsg = detail ? `${msg} - ${detail}` : msg;
      console.error(`[API] Erreur ${response.status}:`, fullMsg);
      throw new Error(fullMsg);
    }

    let payload;
    try {
      payload = await response.json();
    } catch {
      throw new Error("La reponse du serveur n'est pas au format JSON valide.");
    }

    return normalizeApiResponse(payload, sourceType);
  }

  return {
    detectWithBackend,
    normalizeApiResponse
  };
})();
