import { describe, it, expect, vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import FormBuilder from "@/components/FormBuilder/FormBuilder.vue";

// Mock the auto-save composable
vi.mock("@/composables/useAutoSave", () => ({
  useAutoSave: () => ({
    saving: { value: false },
    lastSavedAt: { value: "" },
    hasDraft: { value: false },
    loadDraft: vi.fn().mockResolvedValue(null),
    saveDraft: vi.fn().mockResolvedValue(undefined),
    deleteDraft: vi.fn(),
  }),
}));

const steps = [
  {
    title: "Step 1",
    subtitle: "Basic info",
    fields: [
      { key: "name", label: "Name", type: "text", required: true },
    ],
  },
  {
    title: "Step 2",
    subtitle: "Details",
    fields: [
      { key: "desc", label: "Description", type: "textarea", required: false },
    ],
  },
];

describe("FormBuilder.vue", () => {
  it("renders the first step on mount", () => {
    const wrapper = mount(FormBuilder, {
      props: {
        steps,
        formKey: "test",
        modelValue: { name: "", desc: "" },
      },
    });
    expect(wrapper.text()).toContain("Step 1");
  });

  it("shows validation error when required field is empty and next clicked", async () => {
    const wrapper = mount(FormBuilder, {
      props: {
        steps,
        formKey: "test",
        modelValue: { name: "", desc: "" },
      },
    });
    // Click next without filling name
    const nextBtn = wrapper.findAll("button").find((b) => b.text().includes("下一步"));
    await nextBtn!.trigger("click");
    expect(wrapper.text()).toContain("必填项");
  });

  it("navigates to next step when valid", async () => {
    const wrapper = mount(FormBuilder, {
      props: {
        steps,
        formKey: "test",
        modelValue: { name: "Test", desc: "" },
      },
    });
    // Fill in the field via internal state
    const input = wrapper.find('input[type="text"]');
    await input.setValue("Alice");
    const nextBtn = wrapper.findAll("button").find((b) => b.text().includes("下一步"));
    await nextBtn!.trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("Step 2");
  });

  it("emits submit on final step", async () => {
    const wrapper = mount(FormBuilder, {
      props: {
        steps,
        formKey: "test",
        modelValue: { name: "Test", desc: "" },
      },
    });
    // Go to step 1, fill, next
    await wrapper.find('input[type="text"]').setValue("Alice");
    const nextBtn = wrapper.findAll("button").find((b) => b.text().includes("下一步"));
    await nextBtn!.trigger("click");
    await flushPromises();

    // Now on step 2, click submit
    const submitBtn = wrapper.findAll("button").find((b) => b.text().includes("提交") || b.text().includes("Submit"));
    await submitBtn!.trigger("click");
    await flushPromises();
    expect(wrapper.emitted("submit")).toBeTruthy();
  });

  it("navigates back to previous step", async () => {
    const wrapper = mount(FormBuilder, {
      props: {
        steps,
        formKey: "test",
        modelValue: { name: "Test", desc: "" },
      },
    });
    await wrapper.find('input[type="text"]').setValue("Alice");
    const nextBtn = wrapper.findAll("button").find((b) => b.text().includes("下一步"));
    await nextBtn!.trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("Step 2");

    const prevBtn = wrapper.findAll("button").find((b) => b.text().includes("上一步"));
    await prevBtn!.trigger("click");
    expect(wrapper.text()).toContain("Step 1");
  });
});
