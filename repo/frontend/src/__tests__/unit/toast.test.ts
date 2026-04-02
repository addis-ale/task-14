import { describe, it, expect, vi, beforeEach } from "vitest";
import { toasts, showToast, showError, handleApiError } from "@/utils/toast";

describe("toast.ts", () => {
  beforeEach(() => {
    toasts.value = [];
  });

  describe("showToast", () => {
    it("adds a toast to the list", () => {
      showToast("hello", "info", 10000);
      expect(toasts.value).toHaveLength(1);
      expect(toasts.value[0].message).toBe("hello");
      expect(toasts.value[0].type).toBe("info");
    });

    it("auto-removes after duration", async () => {
      vi.useFakeTimers();
      showToast("temp", "success", 500);
      expect(toasts.value).toHaveLength(1);
      vi.advanceTimersByTime(600);
      expect(toasts.value).toHaveLength(0);
      vi.useRealTimers();
    });
  });

  describe("showError", () => {
    it("adds an error toast with longer duration", () => {
      showError("fail");
      expect(toasts.value).toHaveLength(1);
      expect(toasts.value[0].type).toBe("error");
    });
  });

  describe("handleApiError", () => {
    it("maps known error codes to messages", () => {
      handleApiError({
        response: { status: 400, data: { code: "VALIDATION_ERROR" } },
      });
      expect(toasts.value).toHaveLength(1);
      expect(toasts.value[0].message).toContain("Validation");
    });

    it("shows generic message for unknown codes", () => {
      handleApiError({
        response: { status: 500, data: { message: "Internal" } },
      });
      expect(toasts.value).toHaveLength(1);
      expect(toasts.value[0].message).toContain("Internal");
    });

    it("handles network error (no response)", () => {
      handleApiError({ message: "Network Error" });
      expect(toasts.value).toHaveLength(1);
      expect(toasts.value[0].message).toContain("Network");
    });

    it("handles error with status only", () => {
      handleApiError({ response: { status: 503, data: {} } });
      expect(toasts.value).toHaveLength(1);
      expect(toasts.value[0].message).toContain("503");
    });
  });
});
