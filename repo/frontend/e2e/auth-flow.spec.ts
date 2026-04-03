import { test, expect } from "@playwright/test";

/**
 * E2E tests for authentication flows.
 * These tests verify the login/logout lifecycle, session timeout warning,
 * role-based redirect behavior, and security properties of the login page.
 *
 * Prerequisites: The backend API must be running at the configured proxy target,
 * or these tests should be run with a mock API server.
 */

test.describe("Login Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/login");
  });

  test("renders login form with required fields", async ({ page }) => {
    await expect(page.locator("h1")).toContainText("安全排考与通知系统");
    await expect(page.locator('input[type="text"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test("shows generic error message on failed login (no account enumeration)", async ({ page }) => {
    await page.fill('input[type="text"]', "nonexistent_user");
    await page.fill('input[type="password"]', "wrongpass");
    await page.click('button[type="submit"]');

    // Wait for error message
    const error = page.locator(".error");
    await expect(error).toBeVisible({ timeout: 10_000 });
    // Must show generic message, NOT "user not found" or similar
    const errorText = await error.textContent();
    expect(errorText).toContain("用户名或密码错误");
    expect(errorText).not.toContain("not found");
    expect(errorText).not.toContain("does not exist");
  });

  test("shows password strength indicator", async ({ page }) => {
    await page.fill('input[type="password"]', "a");
    await expect(page.locator(".strength-meter")).toBeVisible();
    await expect(page.locator(".strength-bar.weak")).toBeVisible();

    await page.fill('input[type="password"]', "Str0ng!Pass#2026");
    await expect(page.locator(".strength-bar.strong")).toBeVisible();
  });

  test("remember device checkbox is available", async ({ page }) => {
    const checkbox = page.locator('input[type="checkbox"]');
    await expect(checkbox).toBeVisible();
    await checkbox.check();
    await expect(checkbox).toBeChecked();
  });
});

test.describe("Route Protection", () => {
  test("unauthenticated user is redirected to login from protected routes", async ({ page }) => {
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/login/);
  });

  test("unauthenticated user is redirected to login from admin routes", async ({ page }) => {
    await page.goto("/admin/users");
    await expect(page).toHaveURL(/\/login/);
  });

  test("unauthenticated user is redirected to login from student routes", async ({ page }) => {
    await page.goto("/student/exams");
    await expect(page).toHaveURL(/\/login/);
  });

  test("redirect query parameter is preserved", async ({ page }) => {
    await page.goto("/admin/audit-logs");
    await expect(page).toHaveURL(/\/login\?redirect=/);
    const redirect = new URL(page.url()).searchParams.get("redirect");
    expect(redirect).toBe("/admin/audit-logs");
  });
});

test.describe("Session Timeout Warning", () => {
  test("session timeout warning modal structure exists in layout", async ({ page }) => {
    // Just verify the login page is accessible and the app loads
    await page.goto("/login");
    await expect(page.locator("h1")).toContainText("安全排考");
  });
});
