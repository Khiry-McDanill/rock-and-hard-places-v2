import { test, expect } from "@playwright/test";

// Opt-in: an isolated backend database, with planning mocked in the browser.
const backend = process.env.RHP_INTEGRATION_URL;
test.describe("real approved project workflow", () => {
  test.skip(!backend, "Set RHP_INTEGRATION_URL to an isolated local backend");
  test.describe.configure({ mode: "serial" });
  for (const width of [1440, 943, 390])
    test(`guided and manual creation at ${width}px`, async ({
      page,
      request,
    }) => {
      const catalog = await (
        await request.get(`${backend}/api/catalog/trades`)
      ).json();
      const trade =
        catalog.find((t: any) => /carpent/i.test(t.name)) ?? catalog[0];
      const title = `AI-assisted Hockessin browser ${width} ${Date.now()}`;
      await page.setViewportSize({ width, height: 1000 });
      await page.route(
        (url) => url.pathname.startsWith("/api/"),
        async (route) => {
          if (
            new URL(route.request().url()).pathname ===
            "/api/project-builder/plan"
          )
            return route.fulfill({
              json: {
                plan: {
                  summary:
                    "Adult Hockessin tree house with deck, lighting and weather protection.",
                  followUpQuestions: ["Freestanding or tree-supported?"],
                  suggestedTrades: [],
                  tasks: [
                    {
                      title: "Approved deck and railing",
                      description: "Adult use with safe access.",
                      trade: trade.name,
                      confidence: "HIGH",
                      reason: "Elevated deck access.",
                      needsConfirmation: true,
                    },
                  ],
                  assumptions: ["Adult use"],
                  warnings: ["Confirm structural support."],
                },
                recognizedTrades: [],
                unresolvedTrades: [],
              },
            });
          const url = new URL(route.request().url());
          const response = await route.fetch({
            url: `${backend}${url.pathname}${url.search}`,
          });
          return route.fulfill({ response });
        },
      );
      await page.goto("/projects/new");
      await page.getByRole("button", { name: "Build my project plan" }).click();
      await page
        .getByLabel("Project idea", { exact: true })
        .fill(
          "I want to build a tree house in Hockessin for adults too, with a small deck, lighting, maybe power, and weather protection.",
        );
      await page.getByRole("button", { name: "Shape my project" }).click();
      await page
        .getByLabel("Freestanding or tree-supported?", { exact: false })
        .fill("Freestanding.");
      await page.getByRole("button", { name: "Review proposed plan" }).click();
      await page.getByLabel("Project name", { exact: true }).fill(title);
      await page.getByLabel("Project ZIP code").fill("19707");
      await page.getByRole("button", { name: "Keep", exact: true }).click();
      await page
        .getByRole("button", { name: "Create project", exact: true })
        .click();
      await expect(page).toHaveURL(/\/projects\/\d+$/);
      const id = page.url().split("/").pop();
      const project = await (
        await request.get(`${backend}/api/projects/${id}`)
      ).json();
      const tasks = await (
        await request.get(`${backend}/api/projects/${id}/tasks`)
      ).json();
      expect(project.title).toBe(title);
      expect(project.status).toBe("PLANNING");
      expect(tasks).toHaveLength(1);
      expect(tasks[0].requiredTrades[0].tradeId).toBe(trade.id);
      await expect(
        page.getByRole("heading", { name: title, exact: true }),
      ).toBeVisible();
      for (const section of [
        "tasks",
        "team",
        "bids",
        "messages",
        "completion",
      ]) {
        await page.goto(`/projects/${id}/${section}`);
        await expect(
          page.getByRole("heading", { name: title, exact: true }),
        ).toBeVisible();
        expect(
          await page.evaluate(
            () => document.documentElement.scrollWidth <= window.innerWidth,
          ),
        ).toBe(true);
      }
      await page.goto(`/projects/${id}/tasks`);
      await expect(
        page.getByText("Approved deck and railing", { exact: true }).first(),
      ).toBeVisible();
      await page.screenshot({
        path: `test-results/real-project-${width}.png`,
        fullPage: true,
      });
      await page.goto("/projects");
      await expect(
        page.getByText(title, { exact: true }).first(),
      ).toBeVisible();
      await page.goto("/projects/new");
      await page.getByRole("button", { name: "Enter project details" }).click();
      await page
        .getByLabel("Project name", { exact: true })
        .fill(`Manual browser ${width} ${Date.now()}`);
      await page
        .getByRole("textbox", { name: "Your vision", exact: true })
        .fill("Manual project without a planning request.");
      await page.getByLabel("Project ZIP code").fill("19707");
      await page
        .getByRole("button", { name: "Continue to project review" })
        .click();
      await page
        .getByRole("button", { name: "Create project", exact: true })
        .click();
      await expect(page).toHaveURL(/\/projects\/\d+$/);
      const manualId = page.url().split("/").pop();
      expect(
        await (
          await request.get(`${backend}/api/projects/${manualId}/tasks`)
        ).json(),
      ).toEqual([]);
    });
});
