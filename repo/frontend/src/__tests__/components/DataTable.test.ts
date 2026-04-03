import { describe, it, expect, vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import DataTable from "@/components/DataTable/DataTable.vue";
import type { PageData } from "@/types/api";
import type { TableColumn } from "@/types/ui";

const columns: TableColumn[] = [
  { key: "name", label: "Name", sortable: true },
  { key: "sid", label: "Student ID", sortable: false, maskPii: true },
];

function makeFetcher(items: Record<string, unknown>[] = []) {
  return vi.fn().mockResolvedValue({
    items,
    pagination: { page: 1, size: 20, totalItems: items.length, totalPages: 1 },
  } as PageData<Record<string, unknown>>);
}

describe("DataTable.vue", () => {
  it("renders column headers", async () => {
    const wrapper = mount(DataTable, {
      props: { columns, fetcher: makeFetcher() },
    });
    await flushPromises();
    const headers = wrapper.findAll("th");
    expect(headers.length).toBeGreaterThanOrEqual(2);
    expect(headers[0].text()).toContain("Name");
  });

  it("shows skeleton loader while loading", async () => {
    // fetcher that never resolves — keeps loading=true
    const fetcher = vi.fn().mockReturnValue(new Promise(() => {}));
    const wrapper = mount(DataTable, {
      props: { columns, fetcher },
    });
    // Wait a tick for onMounted to fire and set loading=true
    await new Promise((r) => setTimeout(r, 10));
    // Skeleton rows have class skeleton-row containing span.skeleton
    const skeletonRows = wrapper.findAll(".skeleton-row");
    expect(skeletonRows.length).toBeGreaterThan(0);
  });

  it("shows empty state when no data", async () => {
    const wrapper = mount(DataTable, {
      props: { columns, fetcher: makeFetcher([]) },
    });
    await flushPromises();
    expect(wrapper.text()).toContain("暂无数据");
  });

  it("renders data rows", async () => {
    const fetcher = makeFetcher([
      { name: "Alice", sid: "20261001" },
      { name: "Bob", sid: "20261002" },
    ]);
    const wrapper = mount(DataTable, {
      props: { columns, fetcher },
    });
    await flushPromises();
    const rows = wrapper.findAll("tbody tr");
    expect(rows.length).toBe(2);
  });

  it("emits rowClick on row click", async () => {
    const fetcher = makeFetcher([{ name: "Alice", sid: "123" }]);
    const wrapper = mount(DataTable, {
      props: { columns, fetcher },
    });
    await flushPromises();
    await wrapper.find("tbody tr").trigger("click");
    expect(wrapper.emitted("rowClick")).toBeTruthy();
  });

  it("renders pagination controls", async () => {
    const wrapper = mount(DataTable, {
      props: { columns, fetcher: makeFetcher([{ name: "X", sid: "1" }]) },
    });
    await flushPromises();
    expect(wrapper.find(".table-pagination").exists()).toBe(true);
  });

  it("masks PII by default and reveals on toggle", async () => {
    const fetcher = makeFetcher([{ name: "Alice", sid: "20261234" }]);
    const wrapper = mount(DataTable, {
      props: { columns, fetcher },
    });
    await flushPromises();
    // By default PII is masked
    const cells = wrapper.findAll("tbody td");
    const sidCell = cells[1]; // second column
    expect(sidCell.text()).toContain("****1234");

    // Toggle PII
    const checkbox = wrapper.find('.mask-toggle input[type="checkbox"]');
    await checkbox.setValue(true);
    await flushPromises();
    expect(wrapper.findAll("tbody td")[1].text()).toContain("20261234");
  });

  it("shows error state with retry button on fetch failure", async () => {
    const fetcher = vi.fn().mockRejectedValue(new Error("Network error"));
    const wrapper = mount(DataTable, {
      props: { columns, fetcher },
    });
    await flushPromises();
    expect(wrapper.text()).toContain("加载失败");
    expect(wrapper.text()).toContain("Network error");
    expect(wrapper.find(".retry-btn").exists()).toBe(true);
  });

  it("retries fetch when retry button clicked", async () => {
    let callCount = 0;
    const fetcher = vi.fn().mockImplementation(() => {
      callCount++;
      if (callCount === 1) return Promise.reject(new Error("fail"));
      return Promise.resolve({
        items: [{ name: "Alice", sid: "1" }],
        pagination: { page: 1, size: 20, totalItems: 1, totalPages: 1 },
      });
    });
    const wrapper = mount(DataTable, {
      props: { columns, fetcher },
    });
    await flushPromises();
    expect(wrapper.text()).toContain("加载失败");

    await wrapper.find(".retry-btn").trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("Alice");
    expect(wrapper.find(".retry-btn").exists()).toBe(false);
  });
});
