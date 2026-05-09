import { getApiBaseUrl } from "./runtimeConfig";

const TOKEN_STORAGE_KEY = "aiTextDetector.anonymousToken";
const EXPIRY_SAFETY_WINDOW_MS = 5_000;

type StoredAnonymousToken = {
  token: string;
  expiresAt: number;
  expiryDate: string;
};

type JwtPayload = {
  iat?: number;
  exp?: number;
};

function decodeBase64Url(value: string) {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, "=");
  return atob(padded);
}

function decodeTokenPayload(token: string): JwtPayload {
  const parts = token.split(".");
  const payload = parts.length >= 2 ? parts[1] : parts[0];
  return JSON.parse(decodeBase64Url(payload));
}

function extractToken(data: unknown): string {
  if (typeof data === "string") {
    return data;
  }

  if (!data || typeof data !== "object") {
    throw new Error("Response token invalide.");
  }

  const record = data as Record<string, any>;
  const token = record.token ?? record.accessToken ?? record.jwt ?? record.data?.token;
  if (typeof token !== "string" || !token.trim()) {
    throw new Error("Response token invalide: token manquant.");
  }

  return token;
}

function readStoredToken(): StoredAnonymousToken | null {
  const raw = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const stored = JSON.parse(raw) as StoredAnonymousToken;
    if (!stored.token || stored.expiresAt <= Date.now() + EXPIRY_SAFETY_WINDOW_MS) {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      return null;
    }
    return stored;
  } catch {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    return null;
  }
}

function storeToken(token: string): StoredAnonymousToken {
  const payload = decodeTokenPayload(token);
  if (typeof payload.iat !== "number" || typeof payload.exp !== "number" || payload.exp <= payload.iat) {
    throw new Error("Token anonyme invalide: iat/exp manquants.");
  }

  const lifetimeMs = (payload.exp - payload.iat) * 1000;
  const expiresAt = Date.now() + lifetimeMs;
  const stored = {
    token,
    expiresAt,
    expiryDate: new Date(expiresAt).toISOString(),
  };

  localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(stored));
  return stored;
}

export async function getAnonymousToken(): Promise<string> {
  const stored = readStoredToken();
  if (stored) {
    return stored.token;
  }

  const response = await fetch(`${getApiBaseUrl()}/api/anonym-token/get`, {
    method: "POST",
    headers: {
      "Accept": "application/json",
    },
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Erreur token anonyme (${response.status})`);
  }

  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  return storeToken(extractToken(data)).token;
}
