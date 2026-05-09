type RuntimeConfig = {
  API_URL?: string;
};

declare global {
  interface Window {
    __APP_CONFIG__?: RuntimeConfig;
  }
}

export function getApiBaseUrl() {
  return (window.__APP_CONFIG__?.API_URL || "").replace(/\/$/, "");
}
