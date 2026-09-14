import { test, expect, type Page } from "@playwright/test";
import type { PlanResponse } from "../../src/api/projectBuilder";

const owner = {
  id: 1,
  role: "HOMEOWNER",
  displayName: "Jordan Ellis",
  accountStatus: "ACTIVE",
  profileImageReference: null,
};
const idea =
  "A separate Hockessin tree house for adult use, with a deck and lighting.";
const response = (summary: string): PlanResponse => ({
  plan: {
    summary,
    followUpQuestions: ["Freestanding or tree-supported?"],
    suggestedTrades: [],
    tasks: [
      {
        title: "Install exterior lighting",
        description: "Install exterior deck lights.",
        trade: "Electrical",
        reason: "Homeowner requested exterior lighting.",
        confidence: "HIGH",
        needsConfirmation: false,
      },
    ],
    assumptions: ["Adult use"],
    warnings: ["Confirm support."],
  },
  recognizedTrades: [],
  unresolvedTrades: [],
});

async function fixture(page: Page) {
  let failure: number | "timeout" | undefined;
  let custom: PlanResponse | undefined;
  const plans: { idea: string }[] = [];
  const creates: { body: any; key?: string }[] = [];
  await page.route(
    (url) => url.pathname.startsWith("/api/"),
    async (route) => {
      const req = route.request(),
        path = new URL(req.url()).pathname;
      if (path === "/api/account")
        return route.fulfill({
          json: {
            userId: 1,
            profile: owner,
            profiles: [owner],
            activeRole: "HOMEOWNER",
          },
        });
      if (path === "/api/catalog/trades")
        return route.fulfill({
          json: [
            { id: 1, name: "Carpentry", specialties: [] },
            { id: 2, name: "Electrical", specialties: [] },
            { id: 3, name: "Exterior Work", specialties: [] },
          ],
        });
      if (path === "/api/project-builder/plan") {
        plans.push(req.postDataJSON());
        if (failure === "timeout") return;
        return route.fulfill({
          status: failure ?? 200,
          json: failure
            ? {}
            : (custom ??
              response(
                plans.length === 1
                  ? "Tree-supported tree house."
                  : "Freestanding tree house.",
              )),
        });
      }
      if (path === "/api/project-builder/create") {
        creates.push({
          body: req.postDataJSON(),
          key: req.headers()["idempotency-key"],
        });
        return route.fulfill({
          status: 201,
          json: {
            ...req.postDataJSON(),
            id: 321,
            status: "PLANNING",
            progressPercentage: 0,
          },
        });
      }
      if (path === "/api/projects/321")
        return route.fulfill({
          json: {
            ...creates.at(-1)?.body,
            id: 321,
            status: "PLANNING",
            progressPercentage: 0,
          },
        });
      if (path === "/api/dashboard/homeowner")
        return route.fulfill({ json: { projects: [] } });
      return route.fulfill({ json: [] });
    },
  );
  await page.goto("/projects/new");
  return {
    plans,
    creates,
    fail: (value?: number | "timeout") => {
      failure = value;
    },
    plan: (value: PlanResponse) => {
      custom = value;
    },
  };
}
async function guided(page: Page) {
  await page.getByRole("button", { name: "Build my project plan" }).click();
  await page.getByRole("textbox", { name: "Project idea", exact: true }).fill(idea);
  await page.getByRole("button", { name: "Shape my project" }).click();
  await page.getByRole("button", { name: "Review proposed plan" }).click();
}
async function manual(page: Page, withReview = true) {
  await page.getByRole("button", { name: "Enter project details" }).click();
  await details(page, "Manual garage draft", "Garage only, no tree house.");
  await page
    .getByRole("button", {
      name: withReview
        ? "Review my plan with RH&P"
        : "Continue to project review",
    })
    .click();
  if (withReview)
    await page.getByRole("button", { name: "Review proposed plan" }).click();
}
async function details(page: Page, title: string, description?: string) {
  await page
    .getByRole("textbox", { name: "Project name", exact: true })
    .fill(title);
  if (description !== undefined)
    await page
      .getByRole("textbox", { name: "Your vision", exact: true })
      .fill(description);
  await page.getByLabel("Project ZIP code").fill("19707");
}
async function create(page: Page) {
  await page
    .getByRole("button", { name: "Create project", exact: true })
    .click();
  await expect(page).toHaveURL(/\/projects\/321$/);
}
const question = (page: Page) =>
  page.getByLabel("Freestanding or tree-supported?", { exact: false });
const lighting = (page: Page) =>
  page.getByRole("article", { name: "Install exterior lighting", exact: true });

for (const width of [1440, 943, 390]) {
  test(`B01 paths isolate state and same-draft navigation preserves work at ${width}`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 1000 });
    const state = await fixture(page);
    await manual(page);
    await question(page).fill("Garage answer from old draft");
    await lighting(page)
      .getByRole("button", { name: "Apply", exact: true })
      .click();
    await page.getByRole("button", { name: "Back to start options" }).click();
    await guided(page);
    await expect(
      page.getByRole("textbox", { name: "Project name", exact: true }),
    ).toHaveValue("");
    await expect(
      page.getByRole("textbox", { name: "Your vision", exact: true }),
    ).toHaveValue("Freestanding tree house.");
    await expect(question(page)).toHaveValue("");
    await expect(
      lighting(page).getByRole("button", { name: "Keep", exact: true }),
    ).toHaveAttribute("aria-pressed", "false");
    expect(state.plans[1].idea).toBe(idea);
    await details(page, "Current guided project");
    await lighting(page)
      .getByRole("button", { name: "Keep", exact: true })
      .click();
    await page.getByRole("button", { name: "Back to start options" }).click();
    await page.getByRole("button", { name: "Enter project details" }).click();
    await expect(
      page.getByRole("textbox", { name: "Project name", exact: true }),
    ).toHaveValue("");
    await expect(
      page.getByRole("textbox", { name: "Your vision", exact: true }),
    ).toHaveValue("");
    await expect(page.getByLabel("Project ZIP code")).toHaveValue("");
    await details(page, "Manual garage draft", "Garage only, no tree house.");
    await page
      .getByRole("button", { name: "Continue to project review" })
      .click();
    await expect(page.getByRole("article")).toHaveCount(0);
    await expect(question(page)).toHaveCount(0);
    await page.getByRole("button", { name: "Back to start options" }).click();
    await page.getByRole("button", { name: "Enter project details" }).click();
    await expect(
      page.getByRole("textbox", { name: "Your vision", exact: true }),
    ).toHaveValue("Garage only, no tree house.");
    await page
      .getByRole("button", { name: "Continue to project review" })
      .click();
    await create(page);
    expect(state.creates[0].body).toEqual({
      title: "Manual garage draft",
      description: "Garage only, no tree house.",
      jobZip: "19707",
      tasks: [],
    });
  });

  test(`B01 generated summary updates until homeowner edits it at ${width}`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 1000 });
    const state = await fixture(page);
    await guided(page);
    await question(page).fill("Freestanding only");
    await page
      .getByRole("button", { name: "Update plan with my answers" })
      .click();
    await page.getByRole("button", { name: "Review proposed plan" }).click();
    await expect(
      page.getByRole("textbox", { name: "Your vision", exact: true }),
    ).toHaveValue("Freestanding tree house.");
    await details(
      page,
      "Current tree house",
      "My homeowner-authored final scope.",
    );
    await page
      .getByRole("button", { name: "Update plan with my answers" })
      .click();
    await page.getByRole("button", { name: "Review proposed plan" }).click();
    await expect(
      page.getByRole("textbox", { name: "Your vision", exact: true }),
    ).toHaveValue("My homeowner-authored final scope.");
    await lighting(page)
      .getByRole("button", { name: "Keep", exact: true })
      .click();
    const visible = await page
      .getByLabel("Description that will be saved")
      .inputValue();
    await create(page);
    expect(state.creates[0].body.description).toBe(visible);
    expect(state.creates[0].body.title).toBe("Current tree house");
    expect(state.creates[0].body.tasks).toEqual([
      {
        title: "Install exterior lighting",
        description: "Install exterior deck lights.",
        requiredTradeIds: [2],
      },
    ]);
  });

  for (const mode of ["guided", "manual"] as const)
    for (const failure of [429, 503, "timeout"] as const) {
      test(`B02 ${mode} ${failure} preserves answers, authored work and decisions at ${width}`, async ({
        page,
      }) => {
        await page.setViewportSize({ width, height: 1000 });
        const state = await fixture(page);
        if (mode === "manual") await manual(page);
        else await guided(page);
        await details(page, `Preserved ${mode}`, "My edited project scope.");
        await question(page).fill("Freestanding and adult use.");
        await lighting(page)
          .getByRole("button", { name: "Edit", exact: true })
          .click();
        await lighting(page)
          .getByLabel("Task details")
          .fill("Only interior low-voltage accent lights.");
        await lighting(page)
          .getByRole("button", {
            name: mode === "manual" ? "Apply" : "Keep",
            exact: true,
          })
          .click();
        await lighting(page)
          .getByRole("button", { name: "Done editing" })
          .click();
        await page
          .getByRole("button", { name: "Add task", exact: true })
          .click();
        await page
          .getByLabel("Task name", { exact: true })
          .fill("Homeowner-added access");
        const added = page.getByRole("article", {
          name: "Homeowner-added access",
        });
        await added.getByLabel("Task details").fill("My access requirements.");
        await added.getByRole("combobox").selectOption("1");
        await added
          .getByRole("button", {
            name: mode === "manual" ? "Apply" : "Keep",
            exact: true,
          })
          .click();
        state.fail(failure);
        if (failure === "timeout") await page.clock.install();
        const before = state.plans.length;
        await page
          .getByRole("button", { name: "Update plan with my answers" })
          .click();
        await expect.poll(() => state.plans.length).toBe(before + 1);
        if (failure === "timeout") {
          await page.clock.fastForward(45001);
          await page.clock.resume();
        }
        await expect(page.getByRole("alert")).toContainText("couldn't review");
        state.fail();
        await page
          .getByRole("button", { name: "Try again", exact: true })
          .click();
        await page
          .getByRole("button", { name: "Review proposed plan" })
          .click();
        await expect(page.getByRole("article")).toHaveCount(2);
        await expect(question(page)).toHaveValue("Freestanding and adult use.");
        state.fail(429);
        await page
          .getByRole("button", { name: "Update plan with my answers" })
          .click();
        await page
          .getByRole("alert")
          .getByRole("button", { name: "Continue manually" })
          .click();
        await expect(
          page.getByRole("textbox", { name: "Your vision", exact: true }),
        ).toHaveValue("My edited project scope.");
        await page
          .getByRole("button", { name: "Continue to project review" })
          .click();
        await expect(question(page)).toHaveValue("Freestanding and adult use.");
        await expect(page.getByRole("article")).toHaveCount(2);
        expect(
          await page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
          ),
        ).toBe(true);
        await create(page);
        expect(state.creates[0].body.tasks).toEqual([
          {
            title: "Install exterior lighting",
            description: "Only interior low-voltage accent lights.",
            requiredTradeIds: [2],
          },
          {
            title: "Homeowner-added access",
            description: "My access requirements.",
            requiredTradeIds: [1],
          },
        ]);
        expect(state.creates[0].body.description).toBe(
          "My edited project scope.",
        );
        expect(state.plans.at(-1)!.idea).toContain(
          "Freestanding and adult use.",
        );
      });
    }

  test(`B03 title, scope and trade edits invalidate evidence; authored task has none at ${width}`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 1000 });
    const state = await fixture(page);
    await guided(page);
    await details(page, "Edited scope");
    await expect(
      lighting(page).getByText("Confidence: Strong fit"),
    ).toBeVisible();
    await lighting(page)
      .getByRole("button", { name: "Edit", exact: true })
      .click();
    // Minor punctuation changes deliberately count as edits: no semantic guessing.
    await lighting(page)
      .getByLabel("Task name", { exact: true })
      .fill("Install exterior lighting.");
    const edited = page.getByRole("article", {
      name: "Install exterior lighting.",
      exact: true,
    });
    await expect(
      edited.getByText("Edited by you.", { exact: true }),
    ).toBeVisible();
    await expect(
      edited.getByText(/Confidence:|Homeowner requested exterior/),
    ).toHaveCount(0);
    await edited
      .getByLabel("Task details")
      .fill("Interior accent lights only.");
    await edited.getByRole("combobox").selectOption("1");
    await edited.getByRole("button", { name: "Keep", exact: true }).click();
    await edited.getByRole("button", { name: "Done editing" }).click();
    await page.getByRole("button", { name: "Add task", exact: true }).click();
    await page.getByLabel("Task name", { exact: true }).fill("My access scope");
    const added = page.getByRole("article", { name: "My access scope" });
    await added.getByLabel("Task details").fill("My own scope");
    await added.getByRole("combobox").selectOption("1");
    await expect(added.getByText(/Confidence:|Needs confirmation/)).toHaveCount(
      0,
    );
    await added.getByRole("button", { name: "Keep", exact: true }).click();
    await page.screenshot({
      path: `test-results/blockers-edited-${width}.png`,
      fullPage: true,
    });
    await create(page);
    expect(state.creates[0].body.tasks).toHaveLength(2);
    expect(JSON.stringify(state.creates[0].body)).not.toMatch(
      /confidence|reason|needsConfirmation|origin|sourceKey/,
    );
  });

  test(`B04 removal uses normalized catalog identity; unknown stays unresolved at ${width}`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 1000 });
    const state = await fixture(page);
    const result = response("Deck scope");
    result.plan.suggestedTrades = [
      {
        trade: "  CARPENTRY  ",
        reason: "Timber framing",
        confidence: "HIGH",
        needsConfirmation: true,
      },
    ];
    result.plan.tasks[0].trade = "carpentry";
    state.plan(result);
    await guided(page);
    await details(page, "Catalog identity");
    await page
      .getByRole("article")
      .filter({
        has: page.getByRole("heading", { name: "CARPENTRY", exact: true }),
      })
      .getByRole("button", { name: "Remove", exact: true })
      .click();
    await lighting(page)
      .getByRole("button", { name: "Keep", exact: true })
      .click();
    await expect(
      page.getByText(/A kept task uses a removed trade/),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Create project", exact: true }),
    ).toBeDisabled();
    await lighting(page)
      .getByRole("button", { name: "Edit", exact: true })
      .click();
    await lighting(page).getByLabel("Trade name").fill(" Exterior   Work ");
    await expect(
      page.getByRole("button", { name: "Create project", exact: true }),
    ).toBeEnabled();
    await lighting(page).getByLabel("Trade name").fill("Tree Wizard");
    await expect(
      page.getByRole("button", { name: "Create project", exact: true }),
    ).toBeDisabled();
    await lighting(page).getByRole("combobox").selectOption("3");
    await create(page);
    expect(state.creates[0].body.tasks[0].requiredTradeIds).toEqual([3]);
  });
}

test("B01 changing the guided idea starts a fresh intent; unchanged back navigation keeps approvals", async ({
  page,
}) => {
  const state = await fixture(page);
  await guided(page);
  await details(page, "Old guided title");
  await question(page).fill("Old guided answer");
  await lighting(page)
    .getByRole("button", { name: "Keep", exact: true })
    .click();
  await page.getByRole("button", { name: "Back to start options" }).click();
  await page.getByRole("button", { name: "Build my project plan" }).click();
  await page.getByRole("button", { name: "Shape my project" }).click();
  await page.getByRole("button", { name: "Review proposed plan" }).click();
  await expect(question(page)).toHaveValue("Old guided answer");
  await expect(
    lighting(page).getByRole("button", { name: "Keep", exact: true }),
  ).toHaveAttribute("aria-pressed", "true");
  await page.getByRole("button", { name: "Back to start options" }).click();
  await page.getByRole("button", { name: "Build my project plan" }).click();
  await page
    .getByRole("textbox", { name: "Project idea", exact: true })
    .fill("New kitchen idea");
  await page.getByRole("button", { name: "Shape my project" }).click();
  await page.getByRole("button", { name: "Review proposed plan" }).click();
  await expect(question(page)).toHaveValue("");
  await expect(
    page.getByRole("textbox", { name: "Project name", exact: true }),
  ).toHaveValue("");
  await expect(
    lighting(page).getByRole("button", { name: "Keep", exact: true }),
  ).toHaveAttribute("aria-pressed", "false");
  expect(state.plans.at(-1)!.idea).toBe("New kitchen idea");
});

for (const change of ["description", "trade"] as const)
  test(`B03 direct ${change} edit clears the original assessment independently`, async ({
    page,
  }) => {
    await fixture(page);
    await guided(page);
    if (change === "description") {
      await lighting(page)
        .getByRole("button", { name: "Edit", exact: true })
        .click();
      await lighting(page)
        .getByLabel("Task details")
        .fill("Interior accent lights only.");
    } else await lighting(page).getByRole("combobox").selectOption("1");
    await expect(
      lighting(page).getByText("Edited by you.", { exact: true }),
    ).toBeVisible();
    await expect(
      lighting(page).getByText(
        /Confidence:|Homeowner requested exterior lighting|Needs confirmation/,
      ),
    ).toHaveCount(0);
  });
