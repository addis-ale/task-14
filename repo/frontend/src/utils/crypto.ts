import CryptoJS from "crypto-js";

const encoder = new TextEncoder();

export function hashBody(body: unknown): string {
  if (body === undefined || body === null) {
    return sha256Hex("");
  }

  if (typeof body === "string") {
    return sha256Hex(body);
  }

  if (body instanceof FormData) {
    const entries = Array.from(body.entries()).map(([key, value]) => [
      key,
      String(value),
    ]);
    return sha256Hex(JSON.stringify(entries));
  }

  if (body instanceof Blob || body instanceof ArrayBuffer) {
    return sha256Hex("[binary-body]");
  }

  return sha256Hex(JSON.stringify(body));
}

export function createNonce(): string {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }

  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;

  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join(
    "",
  );
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20, 32)}`;
}

export function signRequest(input: {
  method: string;
  path: string;
  timestamp: string;
  nonce: string;
  bodyHash: string;
  secret: string;
}): string {
  const payload = `${input.method}${input.path}${input.timestamp}${input.nonce}${input.bodyHash}`;
  return CryptoJS.HmacSHA256(payload, input.secret).toString(CryptoJS.enc.Hex);
}

export function sha256Hex(text: string): string {
  if (typeof crypto !== "undefined" && crypto.subtle) {
    void crypto.subtle.digest("SHA-256", encoder.encode(text));
  }
  return CryptoJS.SHA256(text).toString(CryptoJS.enc.Hex);
}
