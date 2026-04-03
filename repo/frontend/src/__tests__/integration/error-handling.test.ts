import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";

// Track mock behaviors
let mockGetBehavior: "success" | "error" = "success";

vi.mock("@/api", () => ({
  api: {
    get: vi.fn().mockImplementation(() => {
      if (mockGetBehavior === "error") {
        return Promise.reject({ response: { status: 500, data: { message: "Server Error" } } });
      }
      return Promise.resolve({
        data: {
          data: {
            preferences: [{ eventType: "EXAM_REMINDER", enabled: true }],
            dndStart: "22:00",
            dndEnd: "07:00",
          },
        },
      });
    }),
    put: vi.fn().mockImplementation(() => {
      if (mockGetBehavior === "error") {
        return Promise.reject({ response: { status: 500, data: { message: "Save Failed" } } });
      }
      return Promise.resolve({ data: { data: {} } });
    }),
  },
  unwrap: vi.fn().mockImplementation(async (promise: Promise<any>) => {
    const res = await promise;
    return res.data?.data ?? res.data;
  }),
}));

vi.mock("@/utils/toast", () => ({
  handleApiError: vi.fn(),
  showSuccess: vi.fn(),
  showError: vi.fn(),
}));

describe("Student Page Error Handling", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    mockGetBehavior = "success";
  });

  describe("MyExamsPage", () => {
    it("shows error state with retry on fetch failure", async () => {
      mockGetBehavior = "error";
      const MyExamsPage = (await import("@/views/student/MyExamsPage.vue")).default;
      const wrapper = mount(MyExamsPage);
      await flushPromises();

      expect(wrapper.text()).toContain("加载失败");
      expect(wrapper.text()).toContain("重试");
      const retryBtn = wrapper.find("button");
      expect(retryBtn.exists()).toBe(true);
    });

    it("loads successfully on happy path", async () => {
      mockGetBehavior = "success";
      // Override get for exams endpoint
      const { api } = await import("@/api");
      vi.mocked(api.get).mockResolvedValueOnce({
        data: {
          data: [
            { sessionId: 1, examDate: "2026-06-15", startTime: "09:00", endTime: "11:00", subjectName: "Math", roomName: "R101", seatNo: "A1" },
          ],
        },
      } as any);

      const MyExamsPage = (await import("@/views/student/MyExamsPage.vue")).default;
      const wrapper = mount(MyExamsPage);
      await flushPromises();

      expect(wrapper.text()).not.toContain("加载失败");
    });

    it("retry button triggers refetch", async () => {
      mockGetBehavior = "error";
      const MyExamsPage = (await import("@/views/student/MyExamsPage.vue")).default;
      const wrapper = mount(MyExamsPage);
      await flushPromises();

      expect(wrapper.text()).toContain("重试");

      // Now make the next call succeed
      mockGetBehavior = "success";
      const { api } = await import("@/api");
      vi.mocked(api.get).mockResolvedValueOnce({
        data: { data: [] },
      } as any);

      await wrapper.find("button").trigger("click");
      await flushPromises();

      // Error state should be cleared
      expect(wrapper.text()).not.toContain("加载失败");
    });
  });

  describe("NotificationPreferencesPage", () => {
    it("shows error state with retry on load failure", async () => {
      mockGetBehavior = "error";
      const PrefsPage = (await import("@/views/student/NotificationPreferencesPage.vue")).default;
      const wrapper = mount(PrefsPage);
      await flushPromises();

      expect(wrapper.text()).toContain("加载失败");
      expect(wrapper.text()).toContain("重试");
    });

    it("loads preferences successfully on happy path", async () => {
      mockGetBehavior = "success";
      const PrefsPage = (await import("@/views/student/NotificationPreferencesPage.vue")).default;
      const wrapper = mount(PrefsPage);
      await flushPromises();

      expect(wrapper.text()).not.toContain("加载失败");
      expect(wrapper.text()).toContain("EXAM_REMINDER");
    });
  });
});
