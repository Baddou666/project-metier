window.AiDetectorHighlighter = (() => {
  async function applyDetectionResultsToTab(tabId, itemResults) {
    if (!itemResults.length) return;

    await chrome.scripting.executeScript({
      target: { tabId },
      args: [itemResults],
      func: (results) => {
        const ID_ATTR = "data-ai-detector-id";
        const STATE_ATTR = "data-ai-detector-state";
        const STYLE_ID = "ai-detector-highlight-style";
        const BEAT_KEY = "__aiDetectorHeartbeat";
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
          if (window[WATCH_KEY]) {
            clearInterval(window[WATCH_KEY]);
            window[WATCH_KEY] = null;
          }
        };

        ensureStyle();
        window[BEAT_KEY] = Date.now();
        window[CLEAN_KEY] = cleanup;

        if (!window[WATCH_KEY]) {
          window[WATCH_KEY] = setInterval(() => {
            if (Date.now() - Number(window[BEAT_KEY] || 0) > 2500) cleanup();
          }, 1200);
        }

        results.forEach(r => {
          const node = document.querySelector(`[${ID_ATTR}="${r.id}"]`);
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

  return {
    applyDetectionResultsToTab,
    cleanupTabMarkers,
    sendHeartbeat
  };
})();
