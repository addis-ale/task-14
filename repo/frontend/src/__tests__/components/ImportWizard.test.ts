import { describe, it, expect, vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import ImportWizard from "@/components/ImportWizard/ImportWizard.vue";
import * as XLSX from "xlsx";

function createExcelBuffer(rows: Record<string, string>[]): ArrayBuffer {
  const ws = XLSX.utils.json_to_sheet(rows);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Sheet1");
  return XLSX.write(wb, { type: "array", bookType: "xlsx" }) as ArrayBuffer;
}

function createFile(rows: Record<string, string>[]): File {
  const buffer = createExcelBuffer(rows);
  return new File([buffer], "test.xlsx", {
    type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  });
}

describe("ImportWizard.vue", () => {
  it("renders upload area", () => {
    const wrapper = mount(ImportWizard, {
      props: { requiredColumns: ["name", "id"] },
    });
    expect(wrapper.text()).toContain("批量导入向导");
    expect(wrapper.find("input[type='file']").exists()).toBe(true);
  });

  it("parses valid file and shows preview rows", async () => {
    const wrapper = mount(ImportWizard, {
      props: { requiredColumns: ["name"] },
    });

    const file = createFile([
      { name: "Alice", grade: "10" },
      { name: "Bob", grade: "11" },
    ]);

    // Simulate file input
    const input = wrapper.find("input[type='file']");
    Object.defineProperty(input.element, "files", {
      value: [file],
      writable: false,
    });
    await input.trigger("change");
    await flushPromises();

    expect(wrapper.text()).toContain("预览结果");
    expect(wrapper.text()).toContain("Alice");
    expect(wrapper.text()).toContain("Bob");
  });

  it("flags rows missing required columns as invalid", async () => {
    const wrapper = mount(ImportWizard, {
      props: { requiredColumns: ["name", "id"] },
    });

    const file = createFile([
      { name: "Alice", id: "1" },
      { name: "Bob", id: "" }, // missing id
    ]);

    const input = wrapper.find("input[type='file']");
    Object.defineProperty(input.element, "files", {
      value: [file],
      writable: false,
    });
    await input.trigger("change");
    await flushPromises();

    expect(wrapper.text()).toContain("invalid");
    expect(wrapper.text()).toContain("Missing required");
  });

  it("detects duplicate rows", async () => {
    const wrapper = mount(ImportWizard, {
      props: { requiredColumns: ["name"] },
    });

    const file = createFile([
      { name: "Alice" },
      { name: "Alice" }, // duplicate
    ]);

    const input = wrapper.find("input[type='file']");
    Object.defineProperty(input.element, "files", {
      value: [file],
      writable: false,
    });
    await input.trigger("change");
    await flushPromises();

    expect(wrapper.text()).toContain("duplicate");
    expect(wrapper.text()).toContain("Duplicate row");
  });

  it("only allows valid rows to be selected (checkbox disabled for invalid)", async () => {
    const wrapper = mount(ImportWizard, {
      props: { requiredColumns: ["name", "id"] },
    });

    const file = createFile([
      { name: "Alice", id: "1" },
      { name: "", id: "2" }, // invalid
    ]);

    const input = wrapper.find("input[type='file']");
    Object.defineProperty(input.element, "files", {
      value: [file],
      writable: false,
    });
    await input.trigger("change");
    await flushPromises();

    const checkboxes = wrapper.findAll('input[type="checkbox"]');
    // First row should be enabled, second disabled
    const disabledCheckboxes = checkboxes.filter(
      (cb) => (cb.element as HTMLInputElement).disabled,
    );
    expect(disabledCheckboxes.length).toBeGreaterThanOrEqual(1);
  });

  it("emits commit with only selected valid rows", async () => {
    const wrapper = mount(ImportWizard, {
      props: { requiredColumns: ["name"] },
    });

    const file = createFile([{ name: "Alice" }, { name: "Bob" }]);

    const input = wrapper.find("input[type='file']");
    Object.defineProperty(input.element, "files", {
      value: [file],
      writable: false,
    });
    await input.trigger("change");
    await flushPromises();

    const commitBtn = wrapper.findAll("button").find((b) => b.text().includes("Commit"));
    await commitBtn!.trigger("click");
    expect(wrapper.emitted("commit")).toBeTruthy();
    const emitted = wrapper.emitted("commit")![0][0] as any[];
    expect(emitted.length).toBe(2);
  });
});
