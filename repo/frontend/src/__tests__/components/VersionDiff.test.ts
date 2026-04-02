import { describe, it, expect, vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import VersionDiff from "@/components/VersionDiff/VersionDiff.vue";

const versions = [
  { id: 1, label: "v1" },
  { id: 2, label: "v2" },
  { id: 3, label: "v3" },
];

const mockDiffs = [
  { fieldName: "title", oldValue: "Old Title", newValue: "New Title" },
  { fieldName: "date", oldValue: "2026-01-01", newValue: "2026-06-15" },
];

describe("VersionDiff.vue", () => {
  it("renders version selectors", () => {
    const wrapper = mount(VersionDiff, {
      props: {
        versions,
        fetchDiff: vi.fn().mockResolvedValue([]),
        allowRestore: false,
      },
    });
    const selects = wrapper.findAll("select");
    expect(selects.length).toBe(2);
  });

  it("auto-selects last two versions", () => {
    const wrapper = mount(VersionDiff, {
      props: {
        versions,
        fetchDiff: vi.fn().mockResolvedValue([]),
        allowRestore: false,
      },
    });
    const selects = wrapper.findAll("select");
    expect((selects[0].element as HTMLSelectElement).value).toBe("2");
    expect((selects[1].element as HTMLSelectElement).value).toBe("3");
  });

  it("displays diff fields after fetch", async () => {
    const fetchDiff = vi.fn().mockResolvedValue(mockDiffs);
    const wrapper = mount(VersionDiff, {
      props: { versions, fetchDiff, allowRestore: false },
    });
    await flushPromises();
    expect(wrapper.text()).toContain("title");
    expect(wrapper.text()).toContain("Old Title");
    expect(wrapper.text()).toContain("New Title");
  });

  it("shows 'No differences' when diff is empty", async () => {
    const fetchDiff = vi.fn().mockResolvedValue([]);
    const wrapper = mount(VersionDiff, {
      props: { versions, fetchDiff, allowRestore: false },
    });
    await flushPromises();
    expect(wrapper.text()).toContain("无差异");
  });

  it("hides restore button when allowRestore is false", async () => {
    const wrapper = mount(VersionDiff, {
      props: {
        versions,
        fetchDiff: vi.fn().mockResolvedValue([]),
        allowRestore: false,
      },
    });
    await flushPromises();
    expect(wrapper.find(".restore-btn").exists()).toBe(false);
  });

  it("shows restore button when allowRestore is true", async () => {
    const wrapper = mount(VersionDiff, {
      props: {
        versions,
        fetchDiff: vi.fn().mockResolvedValue([]),
        allowRestore: true,
      },
    });
    await flushPromises();
    expect(wrapper.find(".restore-btn").exists()).toBe(true);
  });

  it("emits restore event with version id on click", async () => {
    const wrapper = mount(VersionDiff, {
      props: {
        versions,
        fetchDiff: vi.fn().mockResolvedValue([]),
        allowRestore: true,
      },
    });
    await flushPromises();
    await wrapper.find(".restore-btn").trigger("click");
    expect(wrapper.emitted("restore")).toBeTruthy();
    expect(wrapper.emitted("restore")![0][0]).toBe(2); // left version id
  });
});
