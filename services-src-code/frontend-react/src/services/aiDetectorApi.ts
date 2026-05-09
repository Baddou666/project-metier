import { getAnonymousToken } from "./anonymousToken";
import { getApiBaseUrl } from "./runtimeConfig";

export interface Classification {
  label: 'AI' | 'HUMAN';
  confidence: number;
  reasoning: string;
}

type DetectorApiItem = {
  isAi?: boolean;
  label?: string;
  classification?: string;
  result?: string;
  confidence?: number;
  probability?: number;
  score?: number;
  reasoning?: string;
  explanation?: string;
};

function normalizeLabel(value: unknown): 'AI' | 'HUMAN' {
  const label = String(value || "").toUpperCase();
  if (label.includes("HUMAN") || label.includes("HUMAIN")) {
    return "HUMAN";
  }
  if (label === "AI" || label.includes("GENERATED") || label.includes("IA")) {
    return "AI";
  }
  throw new Error("Response API invalide: classification manquante.");
}

function normalizeConfidence(value: unknown): number {
  const confidence = typeof value === "string" ? Number(value) : value;
  if (typeof confidence !== "number" || Number.isNaN(confidence)) {
    return 0;
  }
  return confidence > 1 ? confidence / 100 : confidence;
}

function normalizeResponse(data: any): Classification {
  const item: DetectorApiItem = Array.isArray(data)
    ? data[0]
    : Array.isArray(data?.items)
    ? data.items[0]
    : Array.isArray(data?.results)
      ? data.results[0]
      : data;
  const rawLabel = item?.label ?? item?.classification ?? item?.result;
  const label = typeof item?.isAi === "boolean"
    ? item.isAi ? "AI" : "HUMAN"
    : normalizeLabel(rawLabel);

  return {
    label,
    confidence: normalizeConfidence(item?.confidence ?? item?.probability ?? item?.score),
    reasoning: item?.reasoning ?? item?.explanation ?? `Score API: ${normalizeConfidence(item?.score).toFixed(4)}`,
  };
}

export async function classifyText(text: string): Promise<Classification & { latency: number }> {
  const startTime = Date.now();
  const endpoint = `${getApiBaseUrl()}/api/ai-detector/detect`;
  const token = await getAnonymousToken();

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Anonym ${token}`,
    },
    body: JSON.stringify({
      sourceType: "text",
      link: null,
      items: [
        {
          id: 1,
          text,
        },
      ],
    }),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Erreur API (${response.status})`);
  }

  const result = normalizeResponse(await response.json());
  return {
    ...result,
    latency: Date.now() - startTime,
  };
}
