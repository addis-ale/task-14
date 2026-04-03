import { describe, it, expect } from "vitest";
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

async function mountAndUpload(
  rows: Record<string, string>[],
  props: { requiredColumns: string[]; columnValidators?: any[] } = { requiredColumns: ["studentNo", "studentName", "seatNo"] },
) {
  const wrapper = mount(ImportWizard, { props });
  const file = createFile(rows);
  const input = wrapper.find("input[type='file']");
  Object.defineProperty(input.element, "files", { value: [file], writable: false });
  await input.trigger("change");
  await flushPromises();
  return wrapper;
}

describe("ImportWizard Format Validation", () => {
  it("accepts valid rows with proper format", async () => {
    const wrapper = await mountAndUpload([
      { studentNo: "STU001", studentName: "Alice", seatNo: "A1" },
      { studentNo: "STU002", studentName: "Bob", seatNo: "B2" },
    ]);

    expect(wrapper.text()).toContain("有效 2");
    expect(wrapper.text()).not.toContain("invalid");
  });

  it("flags studentNo with invalid format (special characters)", async () => {
    const wrapper = await mountAndUpload([
      { studentNo: "STU@#$!", studentName: "Alice", seatNo: "A1" },
    ]);

    expect(wrapper.text()).toContain("invalid");
    expect(wrapper.text()).toContain("Invalid student ID format");
  });

  it("flags seatNo with invalid format (too long)", async () => {
    const wrapper = await mountAndUpload([
      { studentNo: "STU001", studentName: "Alice", seatNo: "THIS-IS-WAY-TOO-LONG-SEAT" },
    ]);

    expect(wrapper.text()).toContain("invalid");
    expect(wrapper.text()).toContain("Invalid seat number");
  });

  it("flags examDate with wrong format", async () => {
    const wrapper = await mountAndUpload([
      { studentNo: "STU001", studentName: "Alice", seatNo: "A1", examDate: "13/01/2026" },
    ]);

    expect(wrapper.text()).toContain("invalid");
    expect(wrapper.text()).toContain("Invalid date format");
  });

  it("accepts examDate with YYYY-MM-DD format", async () => {
    const wrapper = await mountAndUpload([
      { studentNo: "STU001", studentName: "Alice", seatNo: "A1", examDate: "2026-06-15" },
    ]);

    expect(wrapper.text()).not.toContain("Invalid date format");
  });

  it("flags invalid time format in startTime column", async () => {
    const wrapper = await mountAndUpload([
      { studentNo: "STU001", studentName: "Alice", seatNo: "A1", startTime: "9am" },
    ]);

    expect(wrapper.text()).toContain("invalid");
    expect(wrapper.text()).toContain("Invalid time format");
  });

  it("accepts valid time format HH:MM", async () => {
    const wrapper = await mountAndUpload([
      { studentNo: "STU001", studentName: "Alice", seatNo: "A1", startTime: "09:00" },
    ]);

    expect(wrapper.text()).not.toContain("Invalid time format");
  });

  it("supports custom column validators", async () => {
    const wrapper = await mountAndUpload(
      [{ studentNo: "STU001", studentName: "Alice", seatNo: "A1", grade: "invalid" }],
      {
        requiredColumns: ["studentNo"],
        columnValidators: [
          { column: "grade", label: "Grade", pattern: /^[0-9]{1,2}$/, message: "Grade must be 1-2 digits" },
        ],
      },
    );

    expect(wrapper.text()).toContain("invalid");
    expect(wrapper.text()).toContain("Grade must be 1-2 digits");
  });

  it("does not flag columns that are not present in data", async () => {
    // email validator exists by default but email column not in data
    const wrapper = await mountAndUpload([
      { studentNo: "STU001", studentName: "Alice", seatNo: "A1" },
    ]);

    expect(wrapper.text()).toContain("有效 1");
    expect(wrapper.text()).not.toContain("invalid");
  });

  it("combines missing-field and format checks (missing takes priority)", async () => {
    const wrapper = await mountAndUpload([
      { studentNo: "", studentName: "Alice", seatNo: "A1" }, // missing required
      { studentNo: "STU@@@", studentName: "Bob", seatNo: "A2" }, // bad format
    ]);

    const text = wrapper.text();
    // First row: missing required
    expect(text).toContain("Missing required");
    // Second row: format issue
    expect(text).toContain("Invalid student ID format");
    // 0 valid
    expect(text).toContain("有效 0");
  });
});
