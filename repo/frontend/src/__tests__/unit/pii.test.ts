import { describe, it, expect } from "vitest";
import { maskStudentId } from "@/utils/pii";

describe("pii.ts — maskStudentId", () => {
  it("returns dash for null/undefined/empty", () => {
    expect(maskStudentId(null)).toBe("-");
    expect(maskStudentId(undefined)).toBe("-");
    expect(maskStudentId("")).toBe("-");
  });

  it("masks all but last 4 chars for long strings", () => {
    expect(maskStudentId("20260001234")).toBe("*******1234");
  });

  it("returns full value when length <= 4", () => {
    expect(maskStudentId("1234")).toBe("1234");
    expect(maskStudentId("AB")).toBe("AB");
  });

  it("handles numeric input", () => {
    expect(maskStudentId(20261234)).toBe("****1234");
  });

  it("shows raw value when showRaw is true", () => {
    expect(maskStudentId("20260001234", true)).toBe("20260001234");
  });

  it("still returns dash for null even with showRaw", () => {
    expect(maskStudentId(null, true)).toBe("-");
  });

  it("masks exactly 5-char string correctly", () => {
    expect(maskStudentId("ABCDE")).toBe("*BCDE");
  });

  it("always masks IDs longer than 4 chars by default (security regression)", () => {
    // Ensures all student IDs in operational views are masked
    const testIds = ["20261001", "S12345678", "STU-2026-001"];
    for (const id of testIds) {
      const masked = maskStudentId(id);
      expect(masked).not.toBe(id);
      expect(masked).toContain("*");
    }
  });
});
