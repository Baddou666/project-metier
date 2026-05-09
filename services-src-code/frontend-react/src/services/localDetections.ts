export interface DetectionResult {
  id?: string;
  text?: string;
  textHash?: string;
  inputLength?: number;
  textPreview: string;
  label: 'AI' | 'HUMAN';
  confidence: number;
  latency: number;
  timestamp: Date;
  userId: string | null;
}

const STORAGE_KEY = "aiTextDetector.sessionDetections";
const CHANGE_EVENT = "ai-text-detector-detections-changed";

function readDetections(): DetectionResult[] {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return [];
  }

  try {
    const data = JSON.parse(raw) as Array<Omit<DetectionResult, "timestamp"> & { timestamp: string }>;
    return data.map((item) => ({
      ...item,
      timestamp: new Date(item.timestamp),
    }));
  } catch (error) {
    console.error("Local detection storage is invalid:", error);
    return [];
  }
}

function writeDetections(detections: DetectionResult[]) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(detections.slice(0, 50)));
  window.dispatchEvent(new Event(CHANGE_EVENT));
}

async function hashText(text: string): Promise<string> {
  const data = new TextEncoder().encode(text);
  const hash = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(hash))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

export async function findDetectionByText(text: string): Promise<DetectionResult | null> {
  const normalizedText = text.trim();
  if (!normalizedText) {
    return null;
  }

  const textHash = await hashText(normalizedText);
  return readDetections().find((detection) => detection.textHash === textHash) ?? null;
}

export async function logDetection(result: Omit<DetectionResult, 'timestamp' | 'userId' | 'id'>) {
  const detections = readDetections();
  const normalizedText = result.text?.trim();
  const textHash = normalizedText ? await hashText(normalizedText) : result.textHash;
  const nextDetection = {
    text: result.text,
    textHash,
    inputLength: result.inputLength ?? normalizedText?.length ?? result.textPreview.length,
    textPreview: result.textPreview,
    label: result.label,
    confidence: result.confidence,
    latency: result.latency,
    id: crypto.randomUUID(),
    timestamp: new Date(),
    userId: null,
  };

  const remainingDetections = textHash
    ? detections.filter((detection) => detection.textHash !== textHash)
    : detections;

  writeDetections([
    nextDetection,
    ...remainingDetections,
  ]);
}

export function subscribeToRecentDetections(callback: (detections: DetectionResult[]) => void) {
  const emit = () => callback(readDetections());
  window.addEventListener(CHANGE_EVENT, emit);
  window.addEventListener("storage", emit);
  emit();

  return () => {
    window.removeEventListener(CHANGE_EVENT, emit);
    window.removeEventListener("storage", emit);
  };
}
