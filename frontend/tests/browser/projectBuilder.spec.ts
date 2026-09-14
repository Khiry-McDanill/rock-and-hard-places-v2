import { test, expect, type Page } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  page.on("pageerror", (error) =>
    console.error("BROWSER ERROR:", error.message),
  );
});

const owner = {
  id: 1,
  role: "HOMEOWNER",
  displayName: "Jordan Ellis",
  accountStatus: "ACTIVE",
  profileImageReference: null,
};
const idea =
  "I want to build a tree house in Hockessin. More than a kids’ playhouse: adults can use it too, with a small deck, lighting, maybe power, and weather protection.";
const plan = {
  plan: {
    summary:
      "An adult-friendly Hockessin Tree House with a deck, lighting and weather protection.",
    followUpQuestions: [
      "Freestanding or tree-supported?",
      "What size do you have in mind?",
      "Enclosed or open?",
      "How should access and deck railings work?",
      "Do you want permanent electrical power?",
      "How will you use it in wet weather?",
    ],
    suggestedTrades: [
      {
        trade: "Carpentry",
        reason: "The structure and deck need framing.",
        confidence: "HIGH",
        needsConfirmation: false,
      },
      {
        trade: "Electrical",
        reason: "Lighting and power were mentioned.",
        confidence: "MEDIUM",
        needsConfirmation: true,
      },
    ],
    tasks: [
      {
        title: "Plan access and railing",
        description: "Clarify safe access to the elevated deck.",
        trade: "Carpentry",
        reason: "Elevated access needs review.",
        confidence: "HIGH",
        needsConfirmation: true,
      },
      {
        title: "Weather protection",
        description: "Consider roof and exterior protection.",
        trade: "Exterior work",
        reason: "The structure is exposed.",
        confidence: "LOW",
        needsConfirmation: true,
      },
    ],
    assumptions: ["Adult use is intended."],
    warnings: ["Support and access need professional confirmation."],
  },
  recognizedTrades: [
    { suggestion: "Carpentry", tradeId: 1, tradeName: "Carpentry" },
  ],
  unresolvedTrades: ["Exterior work"],
};
async function setup(page: Page, status = 200) {
  const writes: { url: string; body: any }[] = [];
  const plans: { idea: string }[] = [];
  await page.route(
    (url) => url.pathname.startsWith("/api/"),
    async (route) => {
      const req = route.request();
      const path = new URL(req.url()).pathname;
      if (path === "/api/project-builder/plan") {
        plans.push(req.postDataJSON());
        await new Promise((resolve) => setTimeout(resolve, 150));
        return route.fulfill({
          status,
          json:
            status === 200
              ? plan
              : {
                  error:
                    status === 429 ? "PLANNING_QUOTA" : "PLANNING_UNAVAILABLE",
                  message: "Private provider diagnostic should not be shown",
                },
        });
      }
      if (req.method() !== "GET")
        writes.push({ url: path, body: req.postDataJSON() });
      if (path === "/api/account")
        return route.fulfill({
          json: {
            userId: 1,
            email: "test@example.com",
            activeRole: "HOMEOWNER",
            profile: owner,
            profiles: [owner],
          },
        });
      if (path === "/api/projects" && req.method() === "POST")
        return route.fulfill({
          json: {
            ...req.postDataJSON(),
            id: 321,
            status: "PLANNING",
            progressPercentage: 0,
          },
        });
      return route.fulfill({ json: [] });
    },
  );
  await page.goto("/projects/new");
  return { writes, plans };
}
async function enterManual(page: Page, review = true) {
  await page.getByRole("button", { name: "Enter project details" }).click();
  await page
    .getByLabel("Project name", { exact: true })
    .fill("Hockessin Tree House");
  await page
    .getByRole("textbox", { name: "Your vision", exact: true })
    .fill("Tree House with Deck and Lighting");
  await page.getByLabel("Project ZIP code").fill("19707");
  await page
    .getByRole("button", {
      name: review ? "Review my plan with RH&P" : "Continue to project review",
      exact: true,
    })
    .click();
}
async function startAI(page: Page) {
  await page.getByRole("button", { name: "Build my project plan" }).click();
  await page.getByLabel("Project idea", { exact: true }).fill(idea);
  await page.getByRole("button", { name: "Shape my project" }).click();
}

test("entry offers both paths and manual creation works without planning", async ({
  page,
}) => {
  const { writes, plans } = await setup(page);
  await expect(
    page.getByRole("heading", { name: "How would you like to start?" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Plan it with RH&P" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Start manually", exact: true }),
  ).toBeVisible();
  await enterManual(page, false);
  await expect(
    page.getByRole("heading", { name: "Project Review", exact: true }),
  ).toBeVisible();
  expect(writes).toEqual([]);
  expect(plans).toEqual([]);
  await page
    .getByRole("button", { name: "Create project", exact: true })
    .click();
  await expect.poll(() => writes.length).toBe(1);
  expect(writes[0]).toEqual({
    url: "/api/projects",
    body: {
      title: "Hockessin Tree House",
      description: "Tree House with Deck and Lighting",
      jobZip: "19707",
    },
  });
});

test("AI idea, loading, iterative answers, review and homeowner controls never create records", async ({
  page,
}) => {
  const { writes, plans } = await setup(page);
  await startAI(page);
  await expect(page.getByRole("status")).toContainText("RH&P is shaping");
  await expect(
    page.getByLabel("Freestanding or tree-supported?", { exact: false }),
  ).toBeVisible();
  await page
    .getByLabel("Freestanding or tree-supported?", { exact: false })
    .fill("Freestanding");
  await page
    .getByLabel("What size do you have in mind?", { exact: false })
    .fill("12 by 12 feet");
  await page
    .getByRole("button", { name: "Update plan with my answers" })
    .click();
  await expect.poll(() => plans.length).toBe(2);
  expect(plans[1].idea).toContain(idea);
  expect(plans[1].idea).toContain("Homeowner answer: Freestanding");
  await expect(
    page.getByRole("button", { name: "Review proposed plan" }),
  ).toBeEnabled();
  await page.getByRole("button", { name: "Review proposed plan" }).click();
  await expect(
    page.getByLabel("Freestanding or tree-supported?", { exact: false }),
  ).toHaveValue("Freestanding");
  await expect(page.getByText("Adult use is intended.")).toBeVisible();
  await expect(
    page.getByText("Support and access need professional confirmation."),
  ).toBeVisible();
  const electrical = page.getByRole("article", {
    name: "Electrical",
    exact: true,
  });
  await expect(
    electrical.getByText("Needs confirmation", { exact: true }),
  ).toBeVisible();
  await expect(electrical.getByText("Confidence: Likely fit")).toBeVisible();
  await electrical.getByRole("button", { name: "Keep", exact: true }).click();
  await expect(electrical.getByText("Kept", { exact: true })).toBeVisible();
  await electrical.getByRole("button", { name: "Edit", exact: true }).click();
  await electrical.getByLabel("Trade name").fill("Electrical scope");
  const edited = page.getByRole("article", {
    name: "Electrical scope",
    exact: true,
  });
  await edited.getByRole("button", { name: "Done editing" }).click();
  await edited.getByRole("button", { name: "Remove", exact: true }).click();
  await expect(edited.getByText("Removed", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Add task", exact: true }).click();
  await page
    .getByLabel("Task name", { exact: true })
    .fill("Confirm intended occupancy");
  await page.getByLabel("Task details").fill("Discuss adult use.");
  await page.getByLabel("Trade name").fill("Carpentry");
  const added = page.getByRole("article", {
    name: "Confirm intended occupancy",
  });
  await added.getByRole("button", { name: "Keep", exact: true }).click();
  await expect(added.getByText("Kept", { exact: true })).toBeVisible();
  expect(writes).toEqual([]);
  await expect(
    page.getByRole("button", { name: "Create project", exact: true }),
  ).toHaveCount(0);
});

test("manual recommendations are opt-in with Apply, Edit, Ignore and exact save preview", async ({
  page,
}) => {
  const { writes, plans } = await setup(page);
  await enterManual(page);
  await page.getByRole("button", { name: "Review proposed plan" }).click();
  expect(plans[0].idea).toContain("Tree House with Deck and Lighting");
  expect(plans[0].idea).toContain("19707");
  const preview = page.getByLabel("Description that will be saved");
  await expect(preview).toHaveValue("Tree House with Deck and Lighting");
  const electrical = page.getByRole("article", {
    name: "Electrical",
    exact: true,
  });
  await electrical.getByRole("button", { name: "Apply", exact: true }).click();
  await expect(preview).toHaveValue(/Trade: Electrical/);
  const task = page.getByRole("article", {
    name: "Weather protection",
    exact: true,
  });
  await task.getByRole("button", { name: "Edit", exact: true }).click();
  await task
    .getByLabel("Task details")
    .fill("Discuss exterior weather protection.");
  await expect(preview).not.toHaveValue(/Discuss exterior/);
  await task.getByRole("button", { name: "Apply", exact: true }).click();
  await expect(preview).toHaveValue(/Discuss exterior/);
  await task.getByRole("button", { name: "Ignore", exact: true }).click();
  await expect(preview).not.toHaveValue(/Discuss exterior/);
  expect(writes).toEqual([]);
  const description = await preview.inputValue();
  await page
    .getByRole("button", { name: "Create project", exact: true })
    .click();
  await expect.poll(() => writes.length).toBe(1);
  expect(writes[0].body.description).toBe(description);
});

for (const status of [429, 503, 502])
  test(`manual fallback for ${status} preserves the draft and allows creation`, async ({
    page,
  }) => {
    const { writes, plans } = await setup(page, status);
    await enterManual(page);
    await expect(page.getByRole("alert")).toContainText(
      "RH&P couldn't review this plan right now.",
    );
    await expect(
      page.getByText("Private provider diagnostic should not be shown"),
    ).toHaveCount(0);
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await expect.poll(() => plans.length).toBe(2);
    await page
      .getByRole("button", { name: "Continue manually", exact: true })
      .click();
    await expect(page.getByLabel("Project name", { exact: true })).toHaveValue(
      "Hockessin Tree House",
    );
    await page
      .getByRole("button", { name: "Continue to project review" })
      .click();
    expect(writes).toEqual([]);
    await page
      .getByRole("button", { name: "Create project", exact: true })
      .click();
    await expect.poll(() => writes.length).toBe(1);
  });

test("a stalled provider times out after 45 seconds and retains the idea", async ({
  page,
}) => {
  const { writes } = await setup(page);
  await page.clock.install();
  let requested = false;
  await page.route("**/api/project-builder/plan", () => {
    requested = true;
  });
  await startAI(page);
  await expect.poll(() => requested).toBe(true);
  await page.clock.fastForward(45001);
  await page.clock.resume();
  await expect(page.getByRole("alert")).toBeVisible();
  await page
    .getByRole("alert")
    .getByRole("button", { name: "Continue manually" })
    .click();
  await expect(
    page.getByRole("textbox", { name: "Your vision", exact: true }),
  ).toHaveValue(idea);
  expect(writes).toEqual([]);
});

for (const width of [1440, 943, 390])
  test(`Hockessin review and fallback fit ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 1000 });
    const { writes } = await setup(page);
    const noOverflow = async () =>
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      ).toBe(true);
    await noOverflow();
    await page.screenshot({
      path: `test-results/builder-${width}-entry.png`,
      fullPage: true,
    });
    await startAI(page);
    await page
      .getByLabel("Freestanding or tree-supported?", { exact: false })
      .fill("Freestanding, with an enclosed room and a small deck.");
    await noOverflow();
    await page.screenshot({
      path: `test-results/builder-${width}-questions.png`,
      fullPage: true,
    });
    await page.getByRole("button", { name: "Review proposed plan" }).click();
    await noOverflow();
    await page.screenshot({
      path: `test-results/builder-${width}-review.png`,
      fullPage: true,
    });
    expect(writes).toEqual([]);
    await page.goto("/projects/new");
    await page.route("**/api/project-builder/plan", (route) =>
      route.fulfill({ status: 503, json: {} }),
    );
    await enterManual(page);
    await expect(page.getByRole("alert")).toBeVisible();
    await noOverflow();
    await page.screenshot({
      path: `test-results/builder-${width}-fallback.png`,
      fullPage: true,
    });
  });

test("leaving a pending review ignores late results and preserves manual entries across options", async ({
  page,
}) => {
  const { writes } = await setup(page);
  let release: (() => Promise<void>) | undefined;
  await page.route("**/api/project-builder/plan", (route) => {
    release = () => route.fulfill({ json: plan });
  });
  await enterManual(page);
  await expect.poll(() => !!release).toBe(true);
  await page
    .getByRole("button", { name: "Continue manually", exact: true })
    .click();
  await page
    .getByRole("textbox", { name: "Your vision", exact: true })
    .fill("Keep these homeowner details");
  const received = page.waitForResponse("**/api/project-builder/plan");
  await release!();
  await received;
  await expect(
    page.getByRole("textbox", { name: "Your vision", exact: true }),
  ).toHaveValue("Keep these homeowner details");
  await expect(
    page.getByRole("heading", { name: "Questions that shape the work" }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "Back to start options" }).click();
  await page.getByRole("button", { name: "Enter project details" }).click();
  await expect(
    page.getByRole("textbox", { name: "Your vision", exact: true }),
  ).toHaveValue("Keep these homeowner details");
  expect(writes).toEqual([]);
});
