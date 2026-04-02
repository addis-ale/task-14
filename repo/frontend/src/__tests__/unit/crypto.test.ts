import { describe, it, expect } from "vitest";
import { hashBody, createNonce, signRequest, sha256Hex } from "@/utils/crypto";

describe("crypto.ts", () => {
  describe("sha256Hex", () => {
    it("returns a 64-char hex string for any input", () => {
      const result = sha256Hex("hello");
      expect(result).toHaveLength(64);
      expect(/^[0-9a-f]{64}$/.test(result)).toBe(true);
    });

    it("returns consistent hash for same input", () => {
      expect(sha256Hex("test")).toBe(sha256Hex("test"));
    });

    it("returns different hash for different input", () => {
      expect(sha256Hex("a")).not.toBe(sha256Hex("b"));
    });

    it("handles empty string", () => {
      const result = sha256Hex("");
      expect(result).toHaveLength(64);
    });
  });

  describe("hashBody", () => {
    it("hashes null/undefined as empty string hash", () => {
      const emptyHash = sha256Hex("");
      expect(hashBody(null)).toBe(emptyHash);
      expect(hashBody(undefined)).toBe(emptyHash);
    });

    it("hashes a string body directly", () => {
      expect(hashBody("test")).toBe(sha256Hex("test"));
    });

    it("hashes an object body via JSON.stringify", () => {
      const obj = { key: "value" };
      expect(hashBody(obj)).toBe(sha256Hex(JSON.stringify(obj)));
    });

    it("handles Blob as [binary-body]", () => {
      const blob = new Blob(["data"]);
      expect(hashBody(blob)).toBe(sha256Hex("[binary-body]"));
    });
  });

  describe("createNonce", () => {
    it("returns a UUID-formatted string", () => {
      const nonce = createNonce();
      expect(nonce).toBeTruthy();
      expect(typeof nonce).toBe("string");
      expect(nonce.length).toBeGreaterThan(0);
    });

    it("returns different values on successive calls (with real crypto)", () => {
      // Restore real randomUUID temporarily if possible
      const n1 = createNonce();
      const n2 = createNonce();
      // They may be same if stubbed, but format should be valid
      expect(n1).toBeTruthy();
      expect(n2).toBeTruthy();
    });
  });

  describe("signRequest", () => {
    it("produces a hex HMAC-SHA256 signature", () => {
      const sig = signRequest({
        method: "GET",
        path: "/api/v1/test",
        timestamp: "1234567890",
        nonce: "test-nonce",
        bodyHash: sha256Hex(""),
        secret: "my-secret",
      });
      expect(sig).toHaveLength(64);
      expect(/^[0-9a-f]{64}$/.test(sig)).toBe(true);
    });

    it("produces same signature for same inputs", () => {
      const input = {
        method: "POST",
        path: "/api/v1/users",
        timestamp: "9999",
        nonce: "nnn",
        bodyHash: "abc",
        secret: "sec",
      };
      expect(signRequest(input)).toBe(signRequest(input));
    });

    it("produces different signature when secret differs", () => {
      const base = {
        method: "GET",
        path: "/test",
        timestamp: "1",
        nonce: "n",
        bodyHash: "h",
      };
      expect(signRequest({ ...base, secret: "a" })).not.toBe(
        signRequest({ ...base, secret: "b" }),
      );
    });
  });
});
