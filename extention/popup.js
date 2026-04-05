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

/* ─── Config ─────────────────────────────────────────────────── */
const API_ENDPOINT = "http://anonymous2:8080/api/detect";
const API_TOKEN    = "";

let activeHighlightedTabId = null;
let heartbeatIntervalId    = null;
const HEARTBEAT_MS = 1000;

const popupSessionPort = chrome.runtime.connect({ name: "popup-session" });

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

async function detectWithBackend(sourceType, items, pageUrl) {
  if (!API_ENDPOINT || API_ENDPOINT.includes("your-backend")) {
    throw new Error("Le backend REST n'est pas encore configuré dans le code de l'extension.");
  }

  const headers = { "Content-Type": "application/json" };
  if (API_TOKEN) headers.Authorization = `Bearer ${API_TOKEN}`;

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
    throw new Error(detail ? `${msg} — ${detail}` : msg);
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
window.addEventListener("pagehide",        stopHeartbeat);
window.addEventListener("beforeunload",    stopHeartbeat);
