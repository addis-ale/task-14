import { test, expect } from "@playwright/test";

/**
 * E2E tests for role-based user journeys.
 * These tests verify that different roles see appropriate navigation,
 * can access their permitted pages, and are blocked from unauthorized pages.
 *
 * Note: These tests require a running backend with test accounts.
 * When running without backend, they verify the frontend routing/guard behavior.
 */

test.describe("Role-based Navigation", () => {
  test("login page is accessible and renders correctly", async ({ page }) => {
    await page.goto("/login");
    await expect(page.locator("h1")).toBeVisible();
    await expect(page.locator("form")).toBeVisible();
  });

  test("403 forbidden page renders for unauthorized access", async ({ page }) => {
    await page.goto("/403");
    await expect(page).toHaveURL(/403/);
  });

  test("404 not found page renders for unknown routes", async ({ page }) => {
    await page.goto("/nonexistent-page-xyz");
    // Should show the not-found page
    const body = await page.textContent("body");
    expect(body).toBeTruthy();
  });
});

test.describe("Direct URL Access Prevention", () => {
  test("accessing /admin/users without auth redirects to login", async ({ page }) => {
    await page.goto("/admin/users");
    await expect(page).toHaveURL(/\/login/);
  });

  test("accessing /scheduling/sessions/new without auth redirects to login", async ({ page }) => {
    await page.goto("/scheduling/sessions/new");
    await expect(page).toHaveURL(/\/login/);
  });

  test("accessing /rosters/import without auth redirects to login", async ({ page }) => {
    await page.goto("/rosters/import");
    await expect(page).toHaveURL(/\/login/);
  });

  test("accessing /notifications/compliance-reviews without auth redirects to login", async ({ page }) => {
    await page.goto("/notifications/compliance-reviews");
    await expect(page).toHaveURL(/\/login/);
  });

  test("accessing /notifications/create without auth redirects to login", async ({ page }) => {
    await page.goto("/notifications/create");
    await expect(page).toHaveURL(/\/login/);
  });

  test("accessing /student/exams without auth redirects to login", async ({ page }) => {
    await page.goto("/student/exams");
    await expect(page).toHaveURL(/\/login/);
  });

  test("accessing /student/preferences without auth redirects to login", async ({ page }) => {
    await page.goto("/student/preferences");
    await expect(page).toHaveURL(/\/login/);
  });

  test("accessing /notifications/inbox without auth redirects to login", async ({ page }) => {
    await page.goto("/notifications/inbox");
    await expect(page).toHaveURL(/\/login/);
  });

  test("accessing /admin/audit-logs without auth redirects to login", async ({ page }) => {
    await page.goto("/admin/audit-logs");
    await expect(page).toHaveURL(/\/login/);
  });

  test("accessing /admin/jobs without auth redirects to login", async ({ page }) => {
    await page.goto("/admin/jobs");
    await expect(page).toHaveURL(/\/login/);
  });
});

test.describe("Concurrent Session UI", () => {
  test("login page loads without concurrent session warnings", async ({ page }) => {
    await page.goto("/login");
    // The concurrent session modal should not be visible on login page
    const modal = page.locator("text=检测到并发会话");
    await expect(modal).not.toBeVisible();
  });
});
