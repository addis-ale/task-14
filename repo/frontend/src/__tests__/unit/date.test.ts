import { describe, it, expect } from "vitest";
import { formatDateTime, formatDate, fromNow } from "@/utils/date";

describe("date.ts", () => {
  describe("formatDateTime", () => {
    it("formats an ISO date string", () => {
      expect(formatDateTime("2026-06-15T09:30:00")).toBe("2026-06-15 09:30");
    });

    it("returns dash for null/undefined", () => {
      expect(formatDateTime(null)).toBe("-");
      expect(formatDateTime(undefined)).toBe("-");
    });

    it("returns dash for empty string", () => {
      expect(formatDateTime("")).toBe("-");
    });
  });

  describe("formatDate", () => {
    it("formats to YYYY-MM-DD", () => {
      expect(formatDate("2026-12-25T10:00:00")).toBe("2026-12-25");
    });

    it("returns dash for null", () => {
      expect(formatDate(null)).toBe("-");
    });
  });

  describe("fromNow", () => {
    it("returns 'just now' for very recent time", () => {
      const now = new Date().toISOString();
      expect(fromNow(now)).toBe("just now");
    });

    it("returns dash for null", () => {
      expect(fromNow(null)).toBe("-");
    });

    it("returns minutes ago for older times", () => {
      const tenMinAgo = new Date(Date.now() - 10 * 60 * 1000).toISOString();
      const result = fromNow(tenMinAgo);
      expect(result).toMatch(/\d+ min ago/);
    });
  });
});
