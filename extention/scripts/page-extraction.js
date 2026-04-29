window.AiDetectorPageExtraction = (() => {
  const MIN_HTML_SEGMENT_LENGTH = 50;

  function normalizeWhitespace(text) {
    return text
      .replace(/\u0000/g, " ")
      .replace(/[ \t]+\n/g, "\n")
      .replace(/\n{3,}/g, "\n\n")
      .trim();
  }

  async function extractHtmlSegmentsFromTab(tabId) {
    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId },
      args: [MIN_HTML_SEGMENT_LENGTH],
      func: (minimumTextLength) => {
        const ATTR = "data-ai-detector-id";
        const SKIP = new Set(["SCRIPT", "STYLE", "NOSCRIPT", "IFRAME", "SVG"]);
        const normalize = text => (text || "").replace(/\s+/g, " ").trim();
        const isVisible = el => {
          if (!el?.isConnected) return false;
          const s = window.getComputedStyle(el);
          return s.display !== "none" && s.visibility !== "hidden";
        };

        document.querySelectorAll(`[${ATTR}]`).forEach(el => {
          el.removeAttribute(ATTR);
          el.removeAttribute("data-ai-detector-state");
          el.removeAttribute("data-ai-detector-score");
          el.removeAttribute("data-ai-detector-result");
        });

        let nextId = 1;
        const segmentByElement = new Map();
        const walker = document.createTreeWalker(
          document.body || document.documentElement,
          NodeFilter.SHOW_TEXT
        );

        while (walker.nextNode()) {
          const node = walker.currentNode;
          const parent = node.parentElement;
          const text = normalize(node.textContent);

          if (!parent || !text || SKIP.has(parent.tagName) || !isVisible(parent)) {
            continue;
          }

          const current = segmentByElement.get(parent);
          if (current) {
            current.text = normalize(`${current.text} ${text}`);
          } else {
            segmentByElement.set(parent, { element: parent, text });
          }
        }

        const items = [];
        segmentByElement.forEach(({ element, text }) => {
          if (text.length < minimumTextLength) {
            return;
          }

          const id = nextId++;
          element.setAttribute(ATTR, String(id));
          items.push({ id, text });
        });

        return { pageTitle: document.title || "", items };
      }
    });

    return {
      pageTitle: result?.pageTitle || "",
      items: (result?.items || [])
        .map(i => ({ ...i, text: normalizeWhitespace(i.text || "") }))
        .filter(i => i.text.length >= MIN_HTML_SEGMENT_LENGTH)
    };
  }

  return {
    MIN_HTML_SEGMENT_LENGTH,
    extractHtmlSegmentsFromTab
  };
})();
