function cleanupHighlightedTab(tabId) {
  if (!tabId) {
    return Promise.resolve();
  }

  return chrome.scripting.executeScript({
    target: { tabId },
    func: () => {
      const ELEMENT_ID_ATTR = "data-ai-detector-id";
      const RESULT_ATTR = "data-ai-detector-state";
      const STYLE_ID = "ai-detector-highlight-style";

      document.querySelectorAll(`[${ELEMENT_ID_ATTR}]`).forEach((node) => {
        node.removeAttribute(ELEMENT_ID_ATTR);
        node.removeAttribute(RESULT_ATTR);
        node.removeAttribute("data-ai-detector-score");
        node.removeAttribute("data-ai-detector-result");
      });

      document.getElementById(STYLE_ID)?.remove();
    }
  }).catch(() => {
    // Ignore cleanup errors when the target tab is gone or inaccessible.
  });
}

chrome.runtime.onConnect.addListener((port) => {
  if (port.name !== "popup-session") {
    return;
  }

  let highlightedTabId = null;

  port.onMessage.addListener((message) => {
    if (message?.type === "set-highlighted-tab") {
      highlightedTabId = Number.isInteger(message.tabId) ? message.tabId : null;
    }
  });

  port.onDisconnect.addListener(() => {
    void cleanupHighlightedTab(highlightedTabId);
  });
});
