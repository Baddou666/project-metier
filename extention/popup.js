const manualText = document.getElementById("manualText");
const checkPageButton = document.getElementById("checkPageButton");
const checkTextButton = document.getElementById("checkTextButton");
const copyResultButton = document.getElementById("copyResultButton");
const backButton = document.getElementById("backButton");
const mainView = document.getElementById("mainView");
const resultView = document.getElementById("resultView");
const statusElement = document.getElementById("status");
const resultElement = document.getElementById("result");
const verdictCard = document.getElementById("verdictCard");
const verdictLabel = document.getElementById("verdictLabel");
const verdictMeta = document.getElementById("verdictMeta");

const API_ENDPOINT = "http://anonymous2:8080/api/detect";
const API_TOKEN = "";
let activeHighlightedTabId = null;
const popupSessionPort = chrome.runtime.connect({ name: "popup-session" });
let heartbeatIntervalId = null;
const HEARTBEAT_INTERVAL_MS = 1000;

function setBusy(isBusy) {
  checkPageButton.disabled = isBusy;
  checkTextButton.disabled = isBusy;
  copyResultButton.disabled = isBusy;
}

function updateResult(status, content) {
  statusElement.textContent = status;
  resultElement.textContent = content || "Aucun contenu disponible.";
}

function showResultView() {
  mainView.classList.add("hidden");
  resultView.classList.remove("hidden");
}

function showMainView() {
  resultView.classList.add("hidden");
  mainView.classList.remove("hidden");
}

function resetVerdict() {
  verdictCard.classList.add("hidden");
  verdictLabel.textContent = "En attente";
  verdictMeta.textContent = "";
  verdictLabel.style.color = "";
}

function showVerdict(label, meta, tone) {
  const toneMap = {
    success: "var(--success)",
    warning: "var(--warning)",
    danger: "var(--danger)"
  };

  verdictCard.classList.remove("hidden");
  verdictLabel.textContent = label;
  verdictMeta.textContent = meta || "";
  verdictLabel.style.color = toneMap[tone] || "var(--text)";
}

function normalizeWhitespace(text) {
  return text.replace(/\u0000/g, " ").replace(/[ \t]+\n/g, "\n").replace(/\n{3,}/g, "\n\n").trim();
}

async function getActiveTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab || !tab.id) {
    throw new Error("Impossible de trouver l'onglet actif.");
  }
  return tab;
}

async function extractHtmlSegmentsFromTab(tabId) {
  const [{ result }] = await chrome.scripting.executeScript({
    target: { tabId },
    func: () => {
      const ELEMENT_ID_ATTR = "data-ai-detector-id";
      const SKIPPED_TAGS = new Set(["SCRIPT", "STYLE", "NOSCRIPT", "IFRAME", "SVG"]);
      const isVisible = (element) => {
        if (!element || !element.isConnected) {
          return false;
        }

        const style = window.getComputedStyle(element);
        if (style.display === "none" || style.visibility === "hidden") {
          return false;
        }

        return true;
      };

      const nextId = (() => {
        let maxId = 0;
        document.querySelectorAll(`[${ELEMENT_ID_ATTR}]`).forEach((element) => {
          const value = Number(element.getAttribute(ELEMENT_ID_ATTR));
          if (Number.isFinite(value)) {
            maxId = Math.max(maxId, value);
          }
        });
        return () => {
          maxId += 1;
          return maxId;
        };
      })();

      const assignId = (element) => {
        const current = element.getAttribute(ELEMENT_ID_ATTR);
        if (current) {
          return Number(current);
        }

        const elemId = nextId();
        element.setAttribute(ELEMENT_ID_ATTR, String(elemId));
        return elemId;
      };

      const itemsByElement = new Map();
      const walker = document.createTreeWalker(document.body || document.documentElement, NodeFilter.SHOW_TEXT);

      while (walker.nextNode()) {
        const node = walker.currentNode;
        const parent = node.parentElement;
        const value = node.textContent?.replace(/\s+/g, " ").trim();

        if (!parent || !value || SKIPPED_TAGS.has(parent.tagName) || !isVisible(parent)) {
          continue;
        }

        const elemId = assignId(parent);
        const current = itemsByElement.get(elemId);

        if (current) {
          current.val = `${current.val} ${value}`.trim();
        } else {
          itemsByElement.set(elemId, {
            elemId,
            tagName: parent.tagName.toLowerCase(),
            val: value
          });
        }
      }

      return {
        pageTitle: document.title || "",
        items: Array.from(itemsByElement.values())
      };
    }
  });
  return {
    pageTitle: result?.pageTitle || "",
    items: (result?.items || []).map((item) => ({
      ...item,
      val: normalizeWhitespace(item.val || "")
    })).filter((item) => item.val)
  };
}

function summarizeItems(sourceType, items) {
  const preview = items
    .slice(0, 10)
    .map((item) => `#${item.elemId} <${item.tagName || "node"}> ${item.val}`)
    .join("\n");

  return [
    `sourceType: ${sourceType}`,
    `items: ${items.length}`,
    "",
    preview
  ].join("\n");
}

function normalizeApiResponse(payload, fallbackSourceType) {
  const results = Array.isArray(payload)
    ? payload
    : Array.isArray(payload.results)
      ? payload.results
      : Array.isArray(payload.items)
        ? payload.items
        : [];
  const aiCount = results.filter((item) => item.isAi === true).length;
  const humanCount = results.filter((item) => item.isAi === false).length;
  const verdict = Array.isArray(payload)
    ? `Segments analyses: ${results.length}`
    : payload.verdict || payload.label || `Segments analyses: ${results.length}`;
  const probability = Array.isArray(payload)
    ? null
    : payload.probability ?? payload.score ?? payload.confidence;
  const sourceType = Array.isArray(payload)
    ? fallbackSourceType
    : payload.sourceType || fallbackSourceType;
  const details = Array.isArray(payload)
    ? ""
    : payload.details || payload.message || payload.reason || "";

  let tone = "warning";
  const lowerVerdict = String(verdict).toLowerCase();

  if (lowerVerdict.includes("humain") || lowerVerdict.includes("human")) {
    tone = "success";
  } else if (lowerVerdict.includes("ia") || lowerVerdict.includes("ai")) {
    tone = "danger";
  }

  const metaParts = [];
  if (sourceType) {
    metaParts.push(`Source: ${sourceType}`);
  }
  if (typeof probability === "number") {
    metaParts.push(`Confiance: ${(probability * 100).toFixed(1)}%`);
  } else if (probability) {
    metaParts.push(`Confiance: ${probability}`);
  }
  if (details) {
    metaParts.push(details);
  }
  if (results.length) {
    metaParts.push(`Humain: ${humanCount}`);
    metaParts.push(`IA: ${aiCount}`);
  }

  return {
    verdict,
    tone,
    meta: metaParts.join(" | "),
    resultText: results.length ? JSON.stringify(results, null, 2) : JSON.stringify(payload, null, 2),
    itemResults: results
  };
}

async function detectWithBackend(sourceType, items, pageUrl) {
  const headers = {
    "Content-Type": "application/json"
  };

  if (API_TOKEN) {
    headers.Authorization = `Bearer ${API_TOKEN}`;
  }

  if (!API_ENDPOINT || API_ENDPOINT.includes("your-backend.example.com")) {
    throw new Error("Le backend REST n'est pas encore configure dans le code de l'extension.");
  }

  const response = await fetch(API_ENDPOINT, {
    method: "POST",
    headers,
    body: JSON.stringify({
      sourceType,
      pageUrl: pageUrl || null,
      items
    })
  });

  if (!response.ok) {
    throw new Error(`Le backend a repondu avec le statut ${response.status}.`);
  }

  const payload = await response.json();
  return normalizeApiResponse(payload, sourceType);
}

async function applyDetectionResultsToTab(tabId, itemResults) {
  if (!itemResults.length) {
    return;
  }

  await chrome.scripting.executeScript({
    target: { tabId },
    args: [itemResults],
    func: (results) => {
      const ELEMENT_ID_ATTR = "data-ai-detector-id";
      const RESULT_ATTR = "data-ai-detector-state";
      const STYLE_ID = "ai-detector-highlight-style";
      const HEARTBEAT_KEY = "__aiDetectorHeartbeat";
      const CLEANUP_KEY = "__aiDetectorCleanup";
      const WATCHER_KEY = "__aiDetectorWatcher";

      const ensureStyle = () => {
        if (document.getElementById(STYLE_ID)) {
          return;
        }

        const style = document.createElement("style");
        style.id = STYLE_ID;
        style.textContent = `
          [${RESULT_ATTR}] {
            position: relative;
            border-radius: 10px;
            transition: box-shadow 180ms ease, background 180ms ease, transform 180ms ease;
          }

          [${RESULT_ATTR}="ai"] {
            background: linear-gradient(120deg, rgba(255, 99, 99, 0.18), rgba(255, 184, 77, 0.22));
            box-shadow:
              0 0 0 1px rgba(214, 69, 69, 0.22),
              0 10px 30px rgba(214, 69, 69, 0.10),
              inset 0 0 0 1px rgba(255, 255, 255, 0.22);
          }

          [${RESULT_ATTR}="human"] {
            background: linear-gradient(120deg, rgba(48, 178, 106, 0.16), rgba(114, 213, 156, 0.22));
            box-shadow:
              0 0 0 1px rgba(31, 143, 87, 0.18),
              0 10px 30px rgba(31, 143, 87, 0.08),
              inset 0 0 0 1px rgba(255, 255, 255, 0.18);
          }

          [${RESULT_ATTR}]:hover {
            transform: translateY(-1px);
          }
        `;
        document.documentElement.appendChild(style);
      };

      const cleanup = () => {
        document.querySelectorAll(`[${ELEMENT_ID_ATTR}]`).forEach((node) => {
          node.removeAttribute(ELEMENT_ID_ATTR);
          node.removeAttribute(RESULT_ATTR);
          node.removeAttribute("data-ai-detector-score");
          node.removeAttribute("data-ai-detector-result");
        });

        document.getElementById(STYLE_ID)?.remove();
        if (window[WATCHER_KEY]) {
          window.clearInterval(window[WATCHER_KEY]);
          window[WATCHER_KEY] = null;
        }
      };

      ensureStyle();
      window[HEARTBEAT_KEY] = Date.now();
      window[CLEANUP_KEY] = cleanup;

      if (!window[WATCHER_KEY]) {
        window[WATCHER_KEY] = window.setInterval(() => {
          const lastHeartbeat = Number(window[HEARTBEAT_KEY] || 0);
          if (Date.now() - lastHeartbeat > 2500) {
            cleanup();
          }
        }, 1200);
      }

      results.forEach((result) => {
        const selector = `[${ELEMENT_ID_ATTR}="${result.elemId}"]`;
        const node = document.querySelector(selector);

        if (!node) {
          return;
        }

        node.setAttribute(RESULT_ATTR, result.isAi ? "ai" : "human");
        node.setAttribute("data-ai-detector-score", result.score != null ? String(result.score) : "");
        node.setAttribute("data-ai-detector-result", JSON.stringify(result));
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
    func: () => {
      window.__aiDetectorHeartbeat = Date.now();
    }
  });
}

function stopHeartbeat() {
  if (!heartbeatIntervalId) {
    return;
  }

  window.clearInterval(heartbeatIntervalId);
  heartbeatIntervalId = null;
}

function startHeartbeat(tabId) {
  stopHeartbeat();
  heartbeatIntervalId = window.setInterval(() => {
    void sendHeartbeat(tabId).catch(() => {
      stopHeartbeat();
    });
  }, HEARTBEAT_INTERVAL_MS);
}

function syncHighlightedTab(tabId) {
  try {
    popupSessionPort.postMessage({
      type: "set-highlighted-tab",
      tabId: tabId ?? null
    });
  } catch {
    // Ignore background sync errors in the popup UI.
  }
}

async function runDetection(sourceType, items, pageUrl, tabId) {
  if (!items.length) {
    throw new Error("Aucun contenu lisible n'a ete trouve.");
  }

  showResultView();
  resetVerdict();
  updateResult("Envoi des segments vers le backend d'analyse...", summarizeItems(sourceType, items));

  const detection = await detectWithBackend(sourceType, items, pageUrl);
  if (sourceType === "html" && tabId) {
    await applyDetectionResultsToTab(tabId, detection.itemResults || []);
    activeHighlightedTabId = tabId;
    syncHighlightedTab(tabId);
    startHeartbeat(tabId);
  }
  showVerdict(detection.verdict, detection.meta, detection.tone);
  updateResult("Analyse terminee.", detection.resultText);
}

async function handleCheckPage() {
  setBusy(true);
  showResultView();
  resetVerdict();
  updateResult("Extraction de la page en cours...", "");

  try {
    if (activeHighlightedTabId) {
      stopHeartbeat();
      await cleanupTabMarkers(activeHighlightedTabId);
      activeHighlightedTabId = null;
      syncHighlightedTab(null);
    }

    const tab = await getActiveTab();
    const extraction = await extractHtmlSegmentsFromTab(tab.id);

    await runDetection("html", extraction.items, tab.url || "", tab.id);
  } catch (error) {
    updateResult("Echec de l'analyse.", error.message || String(error));
  } finally {
    setBusy(false);
  }
}

async function handleCheckText() {
  setBusy(true);
  showResultView();
  resetVerdict();

  try {
    const content = normalizeWhitespace(manualText.value || "");

    if (!content) {
      updateResult("Zone de texte vide.", "Ajoutez un texte dans la zone avant verification.");
      return;
    }

    await runDetection("text", [{ elemId: 1, val: content, tagName: "manual" }], "", null);
  } catch (error) {
    updateResult("Echec de l'analyse.", error.message || String(error));
  } finally {
    setBusy(false);
  }
}

async function handleCopyResult() {
  const content = resultElement.textContent || "";

  if (!content || content === "Le contenu apparaitra ici.") {
    updateResult("Rien a copier.", "Lancez une verification avant de copier le resultat.");
    return;
  }

  try {
    await navigator.clipboard.writeText(content);
    statusElement.textContent = "Resultat copie dans le presse-papiers.";
  } catch {
    statusElement.textContent = "Copie impossible depuis cette popup.";
  }
}

async function handleBack() {
  if (activeHighlightedTabId) {
    try {
      stopHeartbeat();
      await cleanupTabMarkers(activeHighlightedTabId);
    } catch {
      // Ignore cleanup errors when returning to the main screen.
    } finally {
      activeHighlightedTabId = null;
      syncHighlightedTab(null);
    }
  }

  showMainView();
}

checkPageButton.addEventListener("click", handleCheckPage);
checkTextButton.addEventListener("click", handleCheckText);
copyResultButton.addEventListener("click", handleCopyResult);
backButton.addEventListener("click", handleBack);
window.addEventListener("pagehide", stopHeartbeat);
window.addEventListener("beforeunload", stopHeartbeat);
