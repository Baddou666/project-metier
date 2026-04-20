/* ─── DOM References ─────────────────────────────────────────── */
const manualText       = document.getElementById("manualText");
const checkPageButton  = document.getElementById("checkPageButton");
const checkTextButton  = document.getElementById("checkTextButton");
const copyResultButton = document.getElementById("copyResultButton");
const backButton       = document.getElementById("backButton");
const mainView         = document.getElementById("mainView");
const resultView       = document.getElementById("resultView");

// Status
const statusBanner  = document.getElementById("statusBanner");
const statusSpinner = document.getElementById("statusSpinner");
const statusEl      = document.getElementById("status");

// Error
const errorCard    = document.getElementById("errorCard");
const errorTitle   = document.getElementById("errorTitle");
const errorMessage = document.getElementById("errorMessage");

// Verdict
const verdictCard       = document.getElementById("verdictCard");
const verdictBadge      = document.getElementById("verdictBadge");
const verdictLabel      = document.getElementById("verdictLabel");
const verdictConfidence = document.getElementById("verdictConfidence");
const verdictMeta       = document.getElementById("verdictMeta");

// Stats
const statsRow  = document.getElementById("statsRow");
const statHuman = document.getElementById("statHuman");
const statAi    = document.getElementById("statAi");
const statTotal = document.getElementById("statTotal");

// Raw
const toggleRaw = document.getElementById("toggleRaw");
const rawWrap   = document.getElementById("rawWrap");
const resultPre = document.getElementById("result");

// Char counter
const charCount = document.getElementById("charCount");

// Auth Error Banner
const authErrorBanner   = document.getElementById("authErrorBanner");
const authErrorTitle    = document.getElementById("authErrorTitle");
const authErrorMessage  = document.getElementById("authErrorMessage");
const authRetryButton   = document.getElementById("authRetryButton");

/* ─── Config ─────────────────────────────────────────────────── */
const API_DOMAINE = "https://localhost"
const API_ENDPOINT = `${API_DOMAINE}/api/detect`;
const API_TOKEN_ENDPOINT = `${API_DOMAINE}/api/token/get`;
const TOKEN_RETRY_INTERVAL_MS = 2000;
const TOKEN_REQUEST_THROTTLE_MS = 2000;
const TOKEN_MAX_RETRIES = 5;

// Authentication state
let authToken = null;
let lastTokenRequestTime = 0;
let tokenRetryIntervalId = null;
let tokenRetryAttempts = 0;
let isManualTokenAttemptInProgress = false;

// Token storage constants
const TOKEN_STORAGE_KEY = "ai_detector_auth_token";
const TOKEN_EXPIRY_KEY = "ai_detector_token_expiry";
const TOKEN_CACHE_DURATION_MS =  5 * 60 * 1000; // 5 minutes

let activeHighlightedTabId = null;
let heartbeatIntervalId    = null;
const HEARTBEAT_MS = 1000;

const popupSessionPort = chrome.runtime.connect({ name: "popup-session" });

/* ─── Token Storage ──────────────────────────────────────────── */

/**
 * Décode un JWT et extrait la date d'expiration (exp)
 * @param {string} token - Le token JWT
 * @returns {number|null} La date d'expiration en millisecondes ou null si invalide
 */
function getJwtExpiry(token) {
  try {
    // Un JWT a la structure: header.payload.signature
    const parts = token.split('.');
    if (parts.length !== 3) {
      console.warn("[Auth] Token JWT invalide (structure incorrecte)");
      return null;
    }

    // Décoder le payload (2ème partie)
    let payload = parts[1];
    
    // Convertir base64url en base64 (remplacer - par + et _ par /)
    payload = payload.replace(/-/g, '+').replace(/_/g, '/');
    
    // Ajouter le padding si nécessaire
    const padding = 4 - (payload.length % 4);
    if (padding !== 4) {
      payload += '='.repeat(padding);
    }

    // Décoder le base64
    const decodedStr = atob(payload);
    const payloadObj = JSON.parse(decodedStr);

    // Extraire exp (expiration en secondes)
    if (payloadObj.exp && typeof payloadObj.exp === 'number') {
      const expiryMs = payloadObj.exp * 1000; // Convertir en millisecondes
      console.log("[Auth] Expiration du JWT extraite:", new Date(expiryMs).toLocaleString());
      return expiryMs;
    } else {
      console.warn("[Auth] Champ exp non trouvé dans le JWT");
      return null;
    }
  } catch (err) {
    console.warn("[Auth] Erreur lors du décodage du JWT:", err);
    return null;
  }
}

/**
 * Sauvegarde le token d'authentification dans le localStorage
 * Extrait automatiquement la date d'expiration depuis le JWT
 * @param {string} token - Le token JWT à sauvegarder
 */
function saveAuthToken(token) {
  try {
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
    
    // Extraire l'expiration du JWT au lieu d'assumer 24h
    const jwtExpiry = getJwtExpiry(token);
    const expiryTime = jwtExpiry || Date.now() + TOKEN_CACHE_DURATION_MS; // Fallback à 24h si erreur
    
    localStorage.setItem(TOKEN_EXPIRY_KEY, String(expiryTime));
    console.log("[Auth] Token sauvegardé dans le cache local avec expiration JWT.");
  } catch (err) {
    console.warn("[Auth] Impossible de sauvegarder le token:", err);
  }
}

/**
 * Récupère le token d'authentification depuis le localStorage s'il existe et n'est pas expiré
 * @returns {string|null} Le token en cache ou null si pas de cache valide
 */
function loadAuthToken() {
  try {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);
    
    if (!token || !expiry) {
      return null;
    }
    
    const expiryTime = Number(expiry);
    if (Date.now() > expiryTime) {
      console.log("[Auth] Token en cache expiré, suppression.");
      clearAuthToken();
      return null;
    }
    
    console.log("[Auth] Token chargé depuis le cache local.");
    return token;
  } catch (err) {
    console.warn("[Auth] Erreur lors de la lecture du token en cache:", err);
    return null;
  }
}

/**
 * Supprime le token d'authentification du localStorage
 */
function clearAuthToken() {
  try {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(TOKEN_EXPIRY_KEY);
    console.log("[Auth] Token supprimé du cache local.");
  } catch (err) {
    console.warn("[Auth] Impossible de supprimer le token:", err);
  }
}

/* ─── Authentication ──────────────────────────────────────────── */

/**
 * Récupère un token JWT depuis l'endpoint d'authentification
 * @returns {Promise<string|null>} Le token signé ou null en cas d'erreur
 * @throws {Error} Avec le message d'erreur détaillé
 */
async function fetchAuthToken() {
  console.log("[Auth] Tentative de récupération du token...");
  
  // Mettre à jour le timestamp de la dernière tentative
  lastTokenRequestTime = Date.now();

  try {
    const response = await fetch(API_TOKEN_ENDPOINT, {
      method: "POST",
      headers: { "Content-Type": "application/json" }
      // Body vide comme demandé
    });

    if (!response.ok) {
      let detail = "";
      try {
        const body = await response.json();
        detail = body.message || body.error || body.detail || "";
      } catch { /* ignore */ }

      const errorMap = {
        400: "Requête invalide (400).",
        401: "Non autorisé (401).",
        403: "Accès refusé (403).",
        404: "Endpoint introuvable (404). Vérifiez l'URL du backend.",
        422: "Données non traitables (422).",
        429: "Trop de requêtes (429). Réessayez dans quelques instants.",
        500: "Erreur interne du serveur (500).",
        502: "Gateway invalide (502). Le serveur backend est peut-être en cours de démarrage.",
        503: "Service temporairement indisponible (503).",
      };

      const msg = errorMap[response.status] || `Erreur HTTP ${response.status}.`;
      const fullMsg = detail ? `${msg} — ${detail}` : msg;
      console.error("[Auth] Erreur lors de la récupération du token:", fullMsg);
      throw new Error(fullMsg);
    }

    let payload;
    try {
      payload = await response.json();
    } catch {
      throw new Error("La réponse du serveur n'est pas au format JSON valide.");
    }

    // Extraire le token de la réponse (support de plusieurs formats possibles)
    const token = payload.token || payload.access_token || payload.data?.token;
    if (!token) {
      throw new Error("Token non trouvé dans la réponse du serveur.");
    }

    console.log("[Auth] Token reçu avec succès.");
    saveAuthToken(token);
    return token;
  } catch (err) {
    // Erreur réseau (DNS, connexion refusée, CORS, offline…)
    if (!navigator.onLine) {
      throw new Error("Aucune connexion réseau détectée. Vérifiez votre connexion Internet.");
    }
    
    // Si c'est déjà une erreur qu'on a lancée, la relancer
    if (err instanceof Error && err.message) {
      throw err;
    }

    throw new Error(`Impossible de joindre le serveur (${API_TOKEN_ENDPOINT}). Vérifiez que le backend est démarré.`);
  }
}

/**
 * Désactive l'application et affiche une bannière d'erreur d'authentification
 * @param {string} title - Titre de l'erreur
 * @param {string} message - Message d'erreur détaillé
 * @param {boolean} isRetrying - Si true, affiche qu'on essaie de reconnecter (orange)
 * @param {boolean} isError - Si true, affiche l'erreur finale (rouge)
 */
function disableApp(title, message, isRetrying = false, isError = false) {
  console.log("[Auth] App désactivée:", title, message);
  
  // Désactiver tous les boutons
  checkPageButton.disabled = true;
  checkTextButton.disabled = true;
  
  // Afficher la bannière d'erreur
  authErrorBanner.classList.remove("hidden");
  authErrorTitle.textContent = title;
  authErrorMessage.textContent = message;
  
  // Gérer les classes pour les styles
  if (isRetrying) {
    authErrorBanner.classList.add("loading");
    authErrorBanner.classList.remove("error");
    authRetryButton.classList.add("animating");
  } else if (isError) {
    authErrorBanner.classList.add("error");
    authErrorBanner.classList.remove("loading");
    authRetryButton.classList.remove("animating");
  } else {
    authErrorBanner.classList.remove("loading");
    authErrorBanner.classList.remove("error");
    authRetryButton.classList.remove("animating");
  }
}

/**
 * Réactive l'application et masque la bannière d'erreur
 */
function enableApp() {
  console.log("[Auth] App réactivée.");
  
  // Réactiver tous les boutons
  checkPageButton.disabled = false;
  checkTextButton.disabled = false;
  
  // Masquer la bannière d'erreur
  authErrorBanner.classList.add("hidden");
  authErrorBanner.classList.remove("loading");
  authErrorBanner.classList.remove("error");
  
  // Arrêter l'animation du bouton de rafraîchissement
  authRetryButton.classList.remove("animating");
  
  // Arrêter le retry loop si actif
  stopTokenRetryLoop();
}

/**
 * Démarre une boucle de retry automatique du token toutes les 5 secondes
 * Maximum de 5 tentatives de récupération
 * Appelle automatiquement enableApp() si le token est récupéré avec succès
 */
function startTokenRetryLoop() {
  if (tokenRetryIntervalId) {
    return; // Déjà en cours
  }

  console.log("[Auth] Démarrage de la boucle de retry automatique...");
  tokenRetryAttempts = 0;
  
  tokenRetryIntervalId = setInterval(async () => {
    tokenRetryAttempts++;
    console.log(`[Auth] Tentative de récupération du token #${tokenRetryAttempts}/${TOKEN_MAX_RETRIES}`);
    
    try {
      const token = await fetchAuthToken();
      authToken = token;
      console.log("[Auth] Token récupéré avec succès lors du retry.");
      stopTokenRetryLoop();
      enableApp();
    } catch (err) {
      // Vérifier si on a atteint le nombre maximum de tentatives
      if (tokenRetryAttempts >= TOKEN_MAX_RETRIES) {
        console.error(`[Auth] Nombre maximum de tentatives (${TOKEN_MAX_RETRIES}) atteint.`);
        clearAuthToken();
        authToken = null;
        stopTokenRetryLoop();
        const msg = `Impossible de récupérer le token après ${TOKEN_MAX_RETRIES} tentatives. Veuillez vérifier la configuration du serveur.`;
        disableApp("Erreur d'authentification", msg, false, true);
      } else {
        const msg = `Tentative de reconnexion... (${tokenRetryAttempts}/${TOKEN_MAX_RETRIES})`;
        disableApp("Tentative de reconnexion", msg, true);
      }
    }
  }, TOKEN_RETRY_INTERVAL_MS);
}

/**
 * Arrête la boucle de retry automatique du token
 */
function stopTokenRetryLoop() {
  if (tokenRetryIntervalId) {
    clearInterval(tokenRetryIntervalId);
    tokenRetryIntervalId = null;
    authRetryButton.classList.remove("animating");
    console.log("[Auth] Arrêt de la boucle de retry.");
  }
}

/**
 * Effectue une seule tentative de récupération du token (pas de retry automatique)
 * Appelée par le bouton "Rafraîchir"
 * Ignorer les clics si une tentative est déjà en cours
 */
async function throttledRetryToken() {
  // Ignorer si une tentative manuelle est déjà en cours
  if (isManualTokenAttemptInProgress) {
    console.log("[Auth] Une tentative de reconnexion est déjà en cours, ignorée.");
    return;
  }

  const now = Date.now();
  const timeSinceLastRequest = now - lastTokenRequestTime;
  
  if (timeSinceLastRequest < TOKEN_REQUEST_THROTTLE_MS) {
    const remainingMs = TOKEN_REQUEST_THROTTLE_MS - timeSinceLastRequest;
    const remainingSecs = Math.ceil(remainingMs / 1000);
    
    const msg = `Veuillez attendre ${remainingSecs} sec avant de réessayer.`;
    console.log("[Auth] Throttle actif:", msg);
    authErrorMessage.textContent = msg;
    
    // Afficher le message temporairement puis revenir au message d'erreur
    setTimeout(() => {
      authErrorMessage.textContent = "Impossible de joindre le serveur.";
    }, 2000);
    
    return;
  }

  // Marquer que une tentative manuelle est en cours
  isManualTokenAttemptInProgress = true;
  
  // Afficher une tentative unique (pas la boucle de 5)
  console.log("[Auth] Tentative manuelle unique de récupération du token...");
  disableApp("Tentative de connexion", "Récupération du token en cours...", true);
  
  try {
    const token = await fetchAuthToken();
    authToken = token;
    console.log("[Auth] Token récupéré avec succès.");
    enableApp();
  } catch (err) {
    const msg = err?.message || String(err);
    
    // Supprimer le token en cache en cas d'erreur
    clearAuthToken();
    authToken = null;
    
    // Vérifier si c'est une erreur réseau (offline) ou une erreur HTTP
    const isNetworkError = msg.includes("connexion réseau") || 
                          msg.includes("joindre le serveur");
    
    if (isNetworkError) {
      // Erreur réseau : afficher l'erreur sans retry automatique
      disableApp("Serveur non joignable", msg, false, true);
    } else {
      // Erreur HTTP : afficher l'erreur sans retry automatique (l'utilisateur devra cliquer à nouveau)
      disableApp("Tentative échouée", `Erreur de connexion. ${msg}`, false, true);
    }
  } finally {
    // Marquer que la tentative manuelle est terminée
    isManualTokenAttemptInProgress = false;
  }
}

/**
 * Initialise l'application en récupérant le token au démarrage
 * Affiche une bannière d'erreur si l'authentification échoue
 */
async function initializeApp() {
  console.log("[Auth] Initialisation de l'application...");
  
  // Essayer de charger le token depuis le cache
  const cachedToken = loadAuthToken();
  if (cachedToken) {
    authToken = cachedToken;
    console.log("[Auth] Application initialisée avec le token en cache.");
    enableApp();
    return;
  }
  
  // Pas de cache valide, récupérer depuis le serveur
  disableApp("Initialisation", "Récupération du token en cours...", true);
  
  try {
    const token = await fetchAuthToken();
    authToken = token;
    console.log("[Auth] Application initialisée avec succès.");
    enableApp();
  } catch (err) {
    const msg = err?.message || String(err);
    console.error("[Auth] Erreur lors de l'initialisation:", msg);
    
    // Vérifier si c'est une erreur réseau (offline) ou une erreur HTTP
    const isNetworkError = msg.includes("connexion réseau") || 
                          msg.includes("joindre le serveur");
    
    if (isNetworkError) {
      // Erreur réseau : pas de retry automatique
      disableApp("Serveur non joignable", msg, false, true);
    } else {
      // Erreur HTTP : démarrer le retry automatique
      disableApp("Tentative de reconnexion", "Tentative de reconnexion... (0 tentative)", true);
      startTokenRetryLoop();
    }
  }
}

/* ─── Tabs ───────────────────────────────────────────────────── */
const tabSlider  = document.getElementById("tabSlider");
const tabBtns    = document.querySelectorAll(".tab");
const tabPanels  = document.querySelectorAll(".tab-panel");

function positionSlider(activeTab) {
  const tabsRect   = activeTab.closest(".tabs").getBoundingClientRect();
  const btnRect    = activeTab.getBoundingClientRect();
  tabSlider.style.width  = btnRect.width + "px";
  tabSlider.style.transform = `translateX(${btnRect.left - tabsRect.left - 4}px)`;
}

function switchTab(target) {
  tabBtns.forEach(btn => {
    const isActive = btn.dataset.tab === target;
    btn.classList.toggle("active", isActive);
    btn.setAttribute("aria-selected", String(isActive));
  });
  tabPanels.forEach(panel => {
    panel.classList.toggle("active", panel.id === `tab${capitalize(target)}`);
  });
  const activeBtn = document.querySelector(`.tab[data-tab="${target}"]`);
  if (activeBtn) positionSlider(activeBtn);
}

function capitalize(s) { return s.charAt(0).toUpperCase() + s.slice(1); }

// Init slider position
requestAnimationFrame(() => {
  const first = document.querySelector(".tab.active");
  if (first) positionSlider(first);
});

tabBtns.forEach(btn => btn.addEventListener("click", () => switchTab(btn.dataset.tab)));

/* ─── Character counter ──────────────────────────────────────── */
manualText.addEventListener("input", () => {
  const n = manualText.value.length;
  charCount.textContent = n.toLocaleString("fr-FR") + " car.";
});

/* ─── Busy state ─────────────────────────────────────────────── */
function setBusy(isBusy) {
  checkPageButton.disabled  = isBusy;
  checkTextButton.disabled  = isBusy;
  copyResultButton.disabled = isBusy;
}

/* ─── View transitions ───────────────────────────────────────── */
function showResultView() {
  mainView.classList.add("hidden");
  resultView.classList.remove("hidden");
}

function showMainView() {
  resultView.classList.add("hidden");
  mainView.classList.remove("hidden");
}

/* ─── Status Banner ──────────────────────────────────────────── */
const STATUS_TYPES = {
  loading: { cls: "loading",  spin: true  },
  success: { cls: "success",  spin: false },
  error:   { cls: "error",    spin: false },
  done:    { cls: "done",     spin: false },
};

function setStatus(type, text) {
  const conf = STATUS_TYPES[type] || STATUS_TYPES.done;
  statusBanner.className = `status-banner ${conf.cls}`;
  statusEl.textContent   = text;
  statusSpinner.style.display = conf.spin ? "inline-flex" : "none";
}

/* ─── Error display ──────────────────────────────────────────── */
function showError(title, message) {
  errorCard.classList.remove("hidden");
  errorTitle.textContent   = title;
  errorMessage.textContent = message;
  setStatus("error", "Échec de l'analyse.");
}

function hideError() {
  errorCard.classList.add("hidden");
}

/* ─── Verdict ────────────────────────────────────────────────── */
function EMOJI(tone) {
  return tone === "success" ? "✅" : tone === "danger" ? "🔴" : "⚠️";
}

function resetVerdict() {
  verdictCard.classList.add("hidden");
  verdictCard.className = "verdict-card hidden";
  verdictBadge.className = "verdict-badge";
  verdictBadge.textContent = "";
  verdictLabel.textContent = "—";
  verdictConfidence.textContent = "";
  verdictMeta.innerHTML = "";
  statsRow.classList.add("hidden");
  resultPre.textContent = "Le contenu apparaîtra ici.";
  rawWrap.classList.remove("open");
  toggleRaw.classList.remove("open");
  hideError();
}

function showVerdict({ verdict, tone, confidence, chips, humanCount, aiCount, totalCount, resultText }) {
  // Card tone
  verdictCard.className = `verdict-card tone-${tone}`;

  // Badge
  verdictBadge.className = `verdict-badge badge-${tone}`;
  verdictBadge.textContent = EMOJI(tone);

  // Label
  verdictLabel.textContent = verdict || "—";

  // Confidence
  if (confidence != null) {
    const pct = (typeof confidence === "number")
      ? (confidence <= 1 ? confidence * 100 : confidence).toFixed(1) + "%"
      : confidence;
    verdictConfidence.textContent = pct;
  }

  // Chips
  verdictMeta.innerHTML = "";
  (chips || []).forEach(text => {
    const chip = document.createElement("span");
    chip.className = "chip";
    chip.textContent = text;
    verdictMeta.appendChild(chip);
  });

  verdictCard.classList.remove("hidden");

  // Stats
  if (totalCount > 0) {
    statHuman.textContent = humanCount;
    statAi.textContent    = aiCount;
    statTotal.textContent = totalCount;
    statsRow.classList.remove("hidden");
  }

  // Raw JSON
  if (resultText) {
    resultPre.textContent = resultText;
  }
}

/* ─── Raw toggle ─────────────────────────────────────────────── */
toggleRaw.addEventListener("click", () => {
  const open = rawWrap.classList.toggle("open");
  toggleRaw.classList.toggle("open", open);
});

/* ─── Utils ──────────────────────────────────────────────────── */
function normalizeWhitespace(text) {
  return text
    .replace(/\u0000/g, " ")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

async function getActiveTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab?.id) throw new Error("Impossible de trouver l'onglet actif.");
  return tab;
}

/* ─── Page extraction ────────────────────────────────────────── */
async function extractHtmlSegmentsFromTab(tabId) {
  const [{ result }] = await chrome.scripting.executeScript({
    target: { tabId },
    func: () => {
      const ATTR = "data-ai-detector-id";
      const SKIP = new Set(["SCRIPT","STYLE","NOSCRIPT","IFRAME","SVG"]);
      const isVisible = el => {
        if (!el?.isConnected) return false;
        const s = window.getComputedStyle(el);
        return s.display !== "none" && s.visibility !== "hidden";
      };

      let maxId = 0;
      document.querySelectorAll(`[${ATTR}]`).forEach(el => {
        const v = Number(el.getAttribute(ATTR));
        if (Number.isFinite(v)) maxId = Math.max(maxId, v);
      });
      const nextId = () => ++maxId;
      const assignId = el => {
        const cur = el.getAttribute(ATTR);
        if (cur) return Number(cur);
        const id = nextId();
        el.setAttribute(ATTR, String(id));
        return id;
      };

      const map = new Map();
      const walker = document.createTreeWalker(
        document.body || document.documentElement, NodeFilter.SHOW_TEXT
      );

      while (walker.nextNode()) {
        const node   = walker.currentNode;
        const parent = node.parentElement;
        const val    = node.textContent?.replace(/\s+/g, " ").trim();
        if (!parent || !val || SKIP.has(parent.tagName) || !isVisible(parent)) continue;
        const id = assignId(parent);
        const cur = map.get(id);
        if (cur) cur.val = `${cur.val} ${val}`.trim();
        else map.set(id, { elemId: id, tagName: parent.tagName.toLowerCase(), val });
      }

      return { pageTitle: document.title || "", items: Array.from(map.values()) };
    }
  });

  return {
    pageTitle: result?.pageTitle || "",
    items: (result?.items || [])
      .map(i => ({ ...i, val: normalizeWhitespace(i.val || "") }))
      .filter(i => i.val)
  };
}

function summarizeItems(sourceType, items) {
  const preview = items.slice(0, 10)
    .map(i => `#${i.elemId} <${i.tagName || "node"}> ${i.val}`)
    .join("\n");
  return [`sourceType: ${sourceType}`, `items: ${items.length}`, "", preview].join("\n");
}

/* ─── API call ───────────────────────────────────────────────── */
function normalizeApiResponse(payload, fallbackSourceType) {
  const results = Array.isArray(payload)
    ? payload
    : Array.isArray(payload.results) ? payload.results
    : Array.isArray(payload.items)   ? payload.items
    : [];

  const aiCount    = results.filter(r => r.isAi === true).length;
  const humanCount = results.filter(r => r.isAi === false).length;

  const verdict    = Array.isArray(payload)
    ? `${results.length} segments analysés`
    : payload.verdict || payload.label || `${results.length} segments analysés`;

  const probability = Array.isArray(payload)
    ? null
    : payload.probability ?? payload.score ?? payload.confidence ?? null;

  const sourceType = Array.isArray(payload)
    ? fallbackSourceType
    : payload.sourceType || fallbackSourceType;

  const details = Array.isArray(payload)
    ? ""
    : payload.details || payload.message || payload.reason || "";

  // Tone
  const lv = String(verdict).toLowerCase();
  let tone = "warning";
  if (lv.includes("humain") || lv.includes("human")) tone = "success";
  else if (lv.includes("ia") || lv.includes("ai"))   tone = "danger";

  // Chips
  const chips = [];
  if (sourceType) chips.push(`Source : ${sourceType}`);
  if (details)    chips.push(details);

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

/**
 * Réactualise le token d'authentification
 * @returns {Promise<string>} Le nouveau token
 */
async function refreshAuthToken() {
  console.log("[API] Rafraîchissement du token...");
  try {
    const token = await fetchAuthToken();
    authToken = token;
    console.log("[API] Token rafraîchi avec succès.");
    return token;
  } catch (err) {
    console.error("[API] Erreur lors du rafraîchissement du token:", err);
    throw err;
  }
}

/**
 * Effectue une requête API de détection avec gestion du token et retry automatique
 * En cas d'erreur 400 ou 401, tente de rafraîchir le token et retry une fois
 * @param {string} sourceType - Type de source (html ou text)
 * @param {Array} items - Éléments à analyser
 * @param {string} pageUrl - URL de la page (optionnel)
 * @param {boolean} isRetry - Indicateur de retry (interne)
 * @returns {Promise} Résultat de la détection
 */
async function detectWithBackend(sourceType, items, pageUrl, isRetry = false) {
  if (!API_ENDPOINT || API_ENDPOINT.includes("your-backend")) {
    throw new Error("Le backend REST n'est pas encore configuré dans le code de l'extension.");
  }

  if (!authToken) {
    throw new Error("Token d'authentification non disponible. Veuillez rafraîchir la connexion.");
  }

  // Construire les headers avec le Bearer token
  const headers = { 
    "Content-Type": "application/json",
    "Authorization": `Bearer ${authToken}`
  };

  let response;
  try {
    response = await fetch(API_ENDPOINT, {
      method: "POST",
      headers,
      body: JSON.stringify({ sourceType, pageUrl: pageUrl || null, items })
    });
  } catch (err) {
    // Network error (DNS, refused connection, CORS, offline…)
    const isOffline = !navigator.onLine;
    if (isOffline) throw new Error("Aucune connexion réseau détectée. Vérifiez votre connexion Internet.");
    throw new Error(`Impossible de joindre le serveur (${API_ENDPOINT}). Vérifiez que le backend est démarré.`);
  }

  // Gestion des erreurs d'authentification : 400 ou 401
  if (!isRetry && (response.status === 400 || response.status === 401 || response.status === 403)) {
    console.log(`[API] Erreur ${response.status} - Tentative de rafraîchissement du token et retry...`);
    try {
      await refreshAuthToken();
      // Retry la requête une fois avec le nouveau token
      return detectWithBackend(sourceType, items, pageUrl, true);
    } catch (tokenErr) {
      console.error("[API] Impossible de rafraîchir le token:", tokenErr);
      throw new Error("Session expirée. Impossible de renouveler l'authentification.");
    }
  }

  if (!response.ok) {
    let detail = "";
    try {
      const body = await response.json();
      detail = body.message || body.error || body.detail || "";
    } catch { /* ignore */ }

    const statusMap = {
      400: "Requête invalide (400).",
      401: "Non autorisé (401). Vérifiez le token API.",
      403: "Accès refusé (403).",
      404: "Endpoint introuvable (404). Vérifiez l'URL du backend.",
      422: "Données non traitables (422). Format inattendu.",
      429: "Trop de requêtes (429). Réessayez dans quelques instants.",
      500: "Erreur interne du serveur (500).",
      502: "Gateway invalide (502). Le serveur backend est peut-être en cours de démarrage.",
      503: "Service temporairement indisponible (503).",
    };

    const msg = statusMap[response.status] || `Erreur HTTP ${response.status}.`;
    const fullMsg = detail ? `${msg} — ${detail}` : msg;
    console.error(`[API] Erreur ${response.status}:`, fullMsg);
    throw new Error(fullMsg);
  }

  let payload;
  try {
    payload = await response.json();
  } catch {
    throw new Error("La réponse du serveur n'est pas au format JSON valide.");
  }

  return normalizeApiResponse(payload, sourceType);
}

/* ─── Highlight on page ──────────────────────────────────────── */
async function applyDetectionResultsToTab(tabId, itemResults) {
  if (!itemResults.length) return;

  await chrome.scripting.executeScript({
    target: { tabId },
    args: [itemResults],
    func: (results) => {
      const ID_ATTR   = "data-ai-detector-id";
      const STATE_ATTR = "data-ai-detector-state";
      const STYLE_ID  = "ai-detector-highlight-style";
      const BEAT_KEY  = "__aiDetectorHeartbeat";
      const CLEAN_KEY = "__aiDetectorCleanup";
      const WATCH_KEY = "__aiDetectorWatcher";

      const ensureStyle = () => {
        if (document.getElementById(STYLE_ID)) return;
        const s = document.createElement("style");
        s.id = STYLE_ID;
        s.textContent = `
          [${STATE_ATTR}] {
            position: relative;
            border-radius: 6px;
            transition: background 200ms ease, box-shadow 200ms ease;
          }
          [${STATE_ATTR}="ai"] {
            background: rgba(239,68,68,.14);
            box-shadow: 0 0 0 1px rgba(239,68,68,.2);
          }
          [${STATE_ATTR}="human"] {
            background: rgba(16,185,129,.12);
            box-shadow: 0 0 0 1px rgba(16,185,129,.18);
          }
        `;
        document.documentElement.appendChild(s);
      };

      const cleanup = () => {
        document.querySelectorAll(`[${ID_ATTR}]`).forEach(n => {
          n.removeAttribute(ID_ATTR);
          n.removeAttribute(STATE_ATTR);
          n.removeAttribute("data-ai-detector-score");
          n.removeAttribute("data-ai-detector-result");
        });
        document.getElementById(STYLE_ID)?.remove();
        if (window[WATCH_KEY]) { clearInterval(window[WATCH_KEY]); window[WATCH_KEY] = null; }
      };

      ensureStyle();
      window[BEAT_KEY]  = Date.now();
      window[CLEAN_KEY] = cleanup;

      if (!window[WATCH_KEY]) {
        window[WATCH_KEY] = setInterval(() => {
          if (Date.now() - Number(window[BEAT_KEY] || 0) > 2500) cleanup();
        }, 1200);
      }

      results.forEach(r => {
        const node = document.querySelector(`[${ID_ATTR}="${r.elemId}"]`);
        if (!node) return;
        node.setAttribute(STATE_ATTR, r.isAi ? "ai" : "human");
        if (r.score != null) node.setAttribute("data-ai-detector-score", String(r.score));
      });
    }
  });
}

async function cleanupTabMarkers(tabId) {
  await chrome.scripting.executeScript({
    target: { tabId },
    func: () => {
      window.__aiDetectorCleanup?.();
      window.__aiDetectorHeartbeat = 0;
    }
  });
}

async function sendHeartbeat(tabId) {
  await chrome.scripting.executeScript({
    target: { tabId },
    func: () => { window.__aiDetectorHeartbeat = Date.now(); }
  });
}

function stopHeartbeat() {
  if (!heartbeatIntervalId) return;
  clearInterval(heartbeatIntervalId);
  heartbeatIntervalId = null;
}

function startHeartbeat(tabId) {
  stopHeartbeat();
  heartbeatIntervalId = setInterval(() => {
    sendHeartbeat(tabId).catch(() => stopHeartbeat());
  }, HEARTBEAT_MS);
}

function syncHighlightedTab(tabId) {
  try {
    popupSessionPort.postMessage({ type: "set-highlighted-tab", tabId: tabId ?? null });
  } catch { /* ignore */ }
}

/* ─── Run detection ──────────────────────────────────────────── */
async function runDetection(sourceType, items, pageUrl, tabId) {
  if (!items.length) throw new Error("Aucun contenu lisible n'a été trouvé sur cette page.");

  showResultView();
  resetVerdict();
  setStatus("loading", "Envoi vers le backend d'analyse…");

  const detection = await detectWithBackend(sourceType, items, pageUrl);

  if (sourceType === "html" && tabId) {
    await applyDetectionResultsToTab(tabId, detection.itemResults || []);
    activeHighlightedTabId = tabId;
    syncHighlightedTab(tabId);
    startHeartbeat(tabId);
  }

  showVerdict(detection);
  setStatus("done", "Analyse terminée avec succès.");
}

/* ─── Handlers ───────────────────────────────────────────────── */
async function handleCheckPage() {
  setBusy(true);
  showResultView();
  resetVerdict();
  setStatus("loading", "Extraction du contenu de la page…");

  try {
    if (activeHighlightedTabId) {
      stopHeartbeat();
      await cleanupTabMarkers(activeHighlightedTabId).catch(() => {});
      activeHighlightedTabId = null;
      syncHighlightedTab(null);
    }

    const tab = await getActiveTab();
    const extraction = await extractHtmlSegmentsFromTab(tab.id);
    await runDetection("html", extraction.items, tab.url || "", tab.id);
  } catch (err) {
    const msg = err?.message || String(err);
    showError("Analyse échouée", msg);
  } finally {
    setBusy(false);
  }
}

async function handleCheckText() {
  setBusy(true);
  showResultView();
  resetVerdict();
  setStatus("loading", "Préparation du texte…");

  try {
    const content = normalizeWhitespace(manualText.value || "");
    if (!content) {
      showError("Zone de texte vide", "Ajoutez du texte dans la zone avant de lancer l'analyse.");
      return;
    }

    await runDetection("text", [{ elemId: 1, val: content, tagName: "manual" }], "", null);
  } catch (err) {
    const msg = err?.message || String(err);
    showError("Analyse échouée", msg);
  } finally {
    setBusy(false);
  }
}

async function handleCopyResult() {
  const content = resultPre.textContent || "";
  if (!content || content === "Le contenu apparaîtra ici.") {
    setStatus("done", "Rien à copier — lancez d'abord une analyse.");
    return;
  }

  try {
    await navigator.clipboard.writeText(content);
    const original = copyResultButton.innerHTML;
    copyResultButton.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>`;
    setTimeout(() => { copyResultButton.innerHTML = original; }, 1800);
  } catch {
    setStatus("done", "Copie impossible depuis cette popup.");
  }
}

async function handleBack() {
  if (activeHighlightedTabId) {
    try {
      stopHeartbeat();
      await cleanupTabMarkers(activeHighlightedTabId);
    } catch { /* ignore */ } finally {
      activeHighlightedTabId = null;
      syncHighlightedTab(null);
    }
  }
  showMainView();
}

/* ─── Event listeners ────────────────────────────────────────── */
checkPageButton.addEventListener("click",  handleCheckPage);
checkTextButton.addEventListener("click",  handleCheckText);
copyResultButton.addEventListener("click", handleCopyResult);
backButton.addEventListener("click",       handleBack);
authRetryButton.addEventListener("click",  throttledRetryToken);
window.addEventListener("pagehide",        stopHeartbeat);
window.addEventListener("beforeunload",    stopHeartbeat);

/* ─── Initialization ─────────────────────────────────────────── */
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initializeApp);
} else {
  // Document already loaded
  initializeApp();
}
