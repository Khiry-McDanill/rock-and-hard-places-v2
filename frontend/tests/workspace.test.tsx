import { resolvePortfolioMedia, storyStages } from "../src/components/seedMedia";
import { PortfolioCard } from "../src/components/PortfolioCard";
import { PersonProfile } from "../src/features/people";
import {
  resolveProjectMedia,
  seedProjectMedia,
} from "../src/components/seedProjectMedia";
import { taskDisplayOrder } from "../src/features/homeowner/project/ProjectTasks";
import { ProjectMediaGallery } from "../src/features/homeowner/project/ProjectMediaGallery";
import { projectPresentation } from "../src/components/projectPresentation";
import { ProjectImage } from "../src/components/ProjectImage";
import { ProjectFeature } from "../src/features/homeowner/ProjectFeature";
import type { ProjectSummary } from "../src/api/types";
import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { MemoryRouter } from "react-router";
import { QueryClientProvider } from "@tanstack/react-query";
import { App } from "../src/app/App";
import { queryClient, accountKey, profileKey } from "../src/app/query";
import { Progress, words, State, MutationNotice } from "../src/components/ui";
import { api, ApiError } from "../src/api/client";
import { workspaceApi } from "../src/api/workspace";
import type { Account, Profile } from "../src/api/types";
const homeowner: Profile = {
  id: 901,
  role: "HOMEOWNER",
  displayName: "Test Homeowner",
  profileImageReference: null,
  accountStatus: "ACTIVE",
  verificationStatus: null,
  baseZip: null,
  serviceRadius: null,
  availabilityStatus: null,
};
const tradesperson: Profile = {
  ...homeowner,
  id: 902,
  role: "TRADESPERSON",
  displayName: "Test Tradesperson",
  verificationStatus: "VERIFIED",
};
function shell(profile: Profile) {
  queryClient.clear();
  const account: Account = {
    userId: 900,
    email: "test@example.invalid",
    activeRole: profile.role,
    profile,
    profiles: [homeowner, tradesperson],
  };
  queryClient.setQueryData(accountKey, account);
  return renderToStaticMarkup(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/profile"]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
test("role-specific shell uses server identity and exposes mobile navigation", () => {
  const home = shell(homeowner);
  assert.match(home, /Homeowner/);
  assert.match(home, /Find tradespeople/);
  assert.doesNotMatch(home, /Find work/);
  const trade = shell(tradesperson);
  assert.match(trade, /Tradesperson/);
  assert.match(trade, /My bids/);
  assert.match(trade, /Mobile navigation/);
  assert.doesNotMatch(trade, /Find tradespeople/);
});
test("inactive accounts have an explicit restricted workspace", () => {
  assert.match(
    shell({ ...homeowner, accountStatus: "SUSPENDED" }),
    /Account suspended/,
  );
});
test("review state is not completed and progress preserves the server value", () => {
  assert.equal(words("READY_FOR_REVIEW"), "Awaiting your review");
  assert.match(renderToStaticMarkup(<Progress value={33} />), /value="33"/);
  assert.match(renderToStaticMarkup(<Progress value={0} />), /value="0"/);
});
test("API writes preserve task-trade scope, server role switching and message body", async () => {
  const original = globalThis.fetch;
  const calls: { url: string; options?: RequestInit }[] = [];
  globalThis.fetch = async (url, options) => {
    calls.push({ url: String(url), options });
    return new Response("{}", { status: 200 });
  };
  try {
    await api.submitBid(741, {
      taskTradeId: 852,
      amount: 350,
      message: "Defined scope",
    });
    assert.equal(calls[0].url, "/api/tasks/741/bids");
    assert.deepEqual(JSON.parse(String(calls[0].options?.body)), {
      taskTradeId: 852,
      amount: 350,
      message: "Defined scope",
    });
    await api.switchProfile("TRADESPERSON");
    assert.deepEqual(JSON.parse(String(calls[1].options?.body)), {
      role: "TRADESPERSON",
    });
    await workspaceApi.send(963, "Line one\nLine two");
    assert.deepEqual(JSON.parse(String(calls[2].options?.body)), {
      body: "Line one\nLine two",
    });
    globalThis.fetch = async () =>
      new Response(JSON.stringify({ message: "Permission denied" }), {
        status: 403,
      });
    await assert.rejects(
      api.account(),
      (error: unknown) =>
        error instanceof ApiError &&
        error.status === 403 &&
        error.message === "Permission denied",
    );
  } finally {
    globalThis.fetch = original;
  }
});
test("profile cache keys isolate overlapping IDs and cancelled reads cannot restore old data", async () => {
  queryClient.clear();
  assert.notDeepEqual(
    profileKey(homeowner, "projects"),
    profileKey({ ...tradesperson, id: homeowner.id }, "projects"),
  );
  const key = profileKey(homeowner, "slow");
  let aborted = false;
  const request = queryClient
    .fetchQuery({
      queryKey: key,
      queryFn: ({ signal }) =>
        new Promise((_resolve, reject) =>
          signal.addEventListener("abort", () => {
            aborted = true;
            reject(new Error("Aborted"));
          }),
        ),
    })
    .catch(() => undefined);
  await queryClient.cancelQueries();
  queryClient.removeQueries({ queryKey: ["profile"] });
  await request;
  assert.equal(aborted, true);
  assert.equal(queryClient.getQueryData(key), undefined);
  queryClient.clear();
});

test("project imagery keeps local inspiration separate from delivered media", () => {
  const fallback = renderToStaticMarkup(<ProjectImage />);
  assert.match(fallback, /src="\/seed-media\/construction-detail.png"/);
  assert.match(fallback, /alt="construction tools and materials"/);
  assert.doesNotMatch(fallback, /figcaption|concept|illustrative|not an uploaded/i);
  const actual = renderToStaticMarkup(
    <ProjectImage
      media={{
        url: "/api/media/uploaded-photo",
        alt: "Uploaded project photo",
      }}
    />,
  );
  assert.match(actual, /src="\/api\/media\/uploaded-photo"/);
  assert.doesNotMatch(actual, /figcaption|build-inspiration/);
});
test("compact project summary preserves API progress independently of task counts", () => {
  const summary: ProjectSummary = {
    project: {
      id: 741,
      title: "Demolition and rebuild",
      description: "Project scope",
      status: "IN_PROGRESS",
      jobZip: "19147",
      progressPercentage: 37,
    },
    totalTasks: 8,
    completedTasks: 2,
    totalSubtasks: 5,
    completedSubtasks: 1,
    team: [
      {
        id: 601,
        tradespersonId: 852,
        displayName: "Sam",
        status: "ACTIVE",
        trades: ["Carpentry"],
      },
      {
        id: 602,
        tradespersonId: 853,
        displayName: "Lee",
        status: "INVITED",
        trades: [],
      },
    ],
    awaitingReview: [],
    tradesNeeded: [],
    nextAction: "MONITOR_WORK",
  };
  const html = renderToStaticMarkup(
    <MemoryRouter>
      <ProjectFeature summary={summary} />
    </MemoryRouter>,
  );
  assert.match(html, /value="37"/);
  assert.match(html, /Tasks complete<\/dt><dd>2\/8/);
  assert.match(html, /Tradespeople<\/dt><dd>1/);
  assert.match(html, /Awaiting your approval<\/dt><dd>0/);
  assert.match(html, /Demolition and rebuild/);
  assert.match(html, /href="\/projects\/741"/);
});

test("presentation categories use display context, never project identity", () => {
  assert.equal(
    projectPresentation({ title: "Passyunk kitchen remodel" }).url,
    "/seed-media/kitchen-current.png",
  );
  assert.equal(
    projectPresentation({ description: "Update the kitchen cabinets" }).url,
    "/seed-media/kitchen-current.png",
  );
  assert.equal(
    projectPresentation({ title: "Barn conversion" }).url,
    "/seed-media/barn-current.png",
  );
  for (const title of [
    "Outdoor kitchen",
    "Patio",
    "RV kitchen",
    "Bus conversion",
    "Tiny home",
    "Container build",
    "Unspecified project",
  ]) {
    assert.equal(
      projectPresentation({ title }).url,
      "/seed-media/construction-detail.png",
    );
  }
  const image = renderToStaticMarkup(
    <ProjectImage
      context={{ title: "Kitchen remodel" }}
      media={{ url: "/api/media/actual", alt: "Uploaded kitchen" }}
    />,
  );
  assert.match(image, /src="\/api\/media\/actual"/);
  assert.doesNotMatch(image, /kitchen-remodel.png|figcaption/);
});

test("project media is coherent across cover and illustrative gallery, with delivered media priority", () => {
  const context = { title: "Passyunk kitchen remodel" };
  const media = seedProjectMedia(context);
  assert.equal(media.frames.length, 3);
  assert.equal(media.cover.url, projectPresentation(context).url);
  assert.ok(
    media.frames.every((frame) => frame.url.startsWith("/seed-media/kitchen-")),
  );
  const uploaded = [
    {
      url: "/api/media/project-photo",
      alt: "Owner photo",
      caption: "Cabinet detail",
    },
  ];
  assert.deepEqual(resolveProjectMedia(context, uploaded).frames, uploaded);
  assert.equal(resolveProjectMedia(context, uploaded).source, "delivered");
  const html = renderToStaticMarkup(<ProjectMediaGallery project={context} />);
  assert.match(html, /Progress photos/);
  assert.doesNotMatch(html, /2026|uploaded by|concept|illustrative|not an uploaded/i);
  const real = renderToStaticMarkup(
    <ProjectMediaGallery project={context} delivered={uploaded} />,
  );
  assert.doesNotMatch(real, /Illustrative views|seed-media/);
});
test("bathroom portfolio never substitutes unrelated media and stays coherent with project views", () => {
  const item = { id: 1, title: "Cedar Park bathroom renovation", description: "Shower valve and drain",
    provenance: "RHP_VERIFIED" as const, projectId: 2, taskId: 4,
    completionDate: null, approvedAttachmentIds: [12] };
  const registered = resolvePortfolioMedia("Nina Alvarez", item);
  assert.deepEqual(registered, seedProjectMedia(item));
  assert.deepEqual(registered.frames.map((frame) => frame.stage), ["Before", "Early work", "Progress", "Completed"]);
  assert.deepEqual(registered.frames.map((frame) => frame.url), [
    "/seed-media/projects/cedar-park-bathroom-renovation/before.jpg",
    "/seed-media/projects/cedar-park-bathroom-renovation/early-work.jpg",
    "/seed-media/projects/cedar-park-bathroom-renovation/progress.jpg",
    "/seed-media/projects/cedar-park-bathroom-renovation/completed.jpg",
  ]);
  assert.equal(registered.cover?.url, "/seed-media/projects/cedar-park-bathroom-renovation/completed.jpg");
  assert.deepEqual(projectPresentation(item), registered.cover);
  const html = renderToStaticMarkup(<PortfolioCard item={item} name="Nina Alvarez" />);
  assert.match(html, /RH&amp;P verified/);
  assert.match(html, /View project story/);
  assert.match(html, /4 stages/);
  assert.match(html, /src="\/seed-media\/projects\/cedar-park-bathroom-renovation\/completed.jpg"/);
  assert.doesNotMatch(html, /Project photos not available yet|construction-detail|woodwork|demo|concept|illustrative|not an uploaded|<time/i);
  const uploaded = storyStages.map(([, stage], index) => ({ url: `/api/media/approved-${index}`, alt: stage, stage }));
  const media = resolvePortfolioMedia("Nina Alvarez", item, uploaded);
  assert.deepEqual(media.frames, uploaded);
  assert.equal(media.cover?.stage, "Completed");
  const actual = renderToStaticMarkup(<PortfolioCard item={item} name="Nina Alvarez" delivered={uploaded} />);
  assert.match(actual, /4 stages/);
  assert.match(actual, /src="\/api\/media\/approved-3"/);
  assert.doesNotMatch(actual, /Concept image|seed-media/);
  assert.deepEqual(resolvePortfolioMedia("Nina Alvarez", item, [{ url: "private/storage-key", alt: "Private" }]), registered);
});

test("profile sections retain portfolio provenance independently of account verification", () => {
  queryClient.clear();
  const profile = { ...tradesperson, displayName: "Leah Bennett" };
  const base = { description: "Published work", completionDate: null, approvedAttachmentIds: [], projectId: null, taskId: null };
  queryClient.setQueryData(profileKey(profile, `person-${profile.id}`), profile);
  queryClient.setQueryData(profileKey(profile, "people"), []);
  queryClient.setQueryData(profileKey(profile, `portfolio-${profile.id}`), [
    { ...base, id: 1, title: "Fishtown exterior water repairs", provenance: "RHP_VERIFIED", projectId: 8 },
    { ...base, id: 2, title: "Germantown garden wall restoration", provenance: "EXTERNALLY_VERIFIED" },
    { ...base, id: 3, title: "Other commission", provenance: "SELF_REPORTED" },
  ]);
  const html = renderToStaticMarkup(<QueryClientProvider client={queryClient}><MemoryRouter>
    <PersonProfile profile={profile} own />
  </MemoryRouter></QueryClientProvider>);
  assert.match(html, /Work with a story/);
  const platform = html.slice(html.indexOf('aria-label="RH&amp;P Projects"'), html.indexOf('aria-label="External Portfolio"'));
  const external = html.slice(html.indexOf('aria-label="External Portfolio"'));
  assert.match(platform, /RH&amp;P verified/);
  assert.doesNotMatch(platform, /Germantown|Self-reported/);
  assert.match(external, /Externally verified/);
  assert.match(external, /Self-reported/);
  assert.doesNotMatch(external, /RH&amp;P verified|View project story|demo/i);
  assert.deepEqual(resolvePortfolioMedia("Leah Bennett", { title: "Cedar Park bathroom renovation", provenance: "SELF_REPORTED" }).frames, []);
  queryClient.clear();
});

test("task display ordering preserves hierarchy, statuses and server progress without dropping orphans", () => {
  const parent = {
    id: 751,
    projectId: 741,
    parentTaskId: null,
    title: "Build plan",
    description: "Scope",
    status: "IN_PROGRESS" as const,
    requiredTrades: [],
    progressPercentage: 37,
  };
  const child = {
    ...parent,
    id: 752,
    parentTaskId: 751,
    status: "READY_FOR_REVIEW" as const,
  };
  const orphan = { ...parent, id: 753, parentTaskId: 999 };
  const result = taskDisplayOrder([child, parent, orphan]);
  assert.deepEqual(
    result.map((task) => task.id),
    [751, 752, 753],
  );
  assert.equal(result[1], child);
  assert.equal(result[1].status, "READY_FOR_REVIEW");
  assert.equal(result[1].progressPercentage, 37);
});

test("read and form errors show product guidance without exposing service details", () => {
  for (const [status, copy] of [
    [400, "Check your entries"], [401, "Your session could not be confirmed"],
    [403, "You don’t have access"], [404, "We couldn’t find"],
    [409, "Something changed"], [422, "Check your entries"],
    [429, "Please wait a moment"], [500, "Something went wrong"],
  ] as const) {
    const error = new ApiError(status, "Java Spring endpoint /api/private unavailable", {
      status, error: "Internal error", message: "Backend unavailable", path: "/api/private",
      fieldErrors: { taskTradeId: "Internal constraint stack trace" },
    });
    const notice = renderToStaticMarkup(<MutationNotice mutation={{ isError: true, isSuccess: false, error }} />);
    const view = renderToStaticMarkup(<State query={{ isPending: false, isError: true, error, refetch: () => undefined }}>Content</State>);
    for (const html of [notice, view]) {
      assert.ok(html.includes(copy));
      assert.doesNotMatch(html, /Java|Spring|endpoint|\/api|backend|stack trace|taskTradeId/i);
    }
  }
});

function projectRoute(profile: Profile, projectId: number, path: string, taskId = projectId + 1000) {
  queryClient.clear();
  const project = {
    id: projectId, title: `Project ${projectId}`, description: `Scope for ${projectId}`,
    status: "IN_PROGRESS" as const, jobZip: "19147", progressPercentage: 37,
  };
  const task = {
    id: taskId, projectId, parentTaskId: null, title: `Task for ${projectId}`,
    description: "Assigned scope", status: "IN_PROGRESS" as const,
    requiredTrades: [], progressPercentage: 37,
  };
  queryClient.setQueryData(accountKey, {
    userId: 900, email: "test@example.invalid", activeRole: profile.role, profile,
    profiles: [homeowner, tradesperson],
  });
  const cache = (key: string, data: unknown) => queryClient.setQueryData(profileKey(profile, key), data);
  cache(`project-${projectId}`, project);
  cache("projects", [project]);
  cache(`tasks-${projectId}`, [task]);
  cache(`team-${projectId}`, []);
  cache(`assignments-${task.id}`, []);
  cache(`bids-${task.id}`, []);
  cache(`conversations-${projectId}`, []);
  cache("people", []);
  cache("catalog", []);
  cache("overview", { projects: [{ project, totalTasks: 1, completedTasks: 0,
    totalSubtasks: 0, completedSubtasks: 0, team: [], awaitingReview: [], tradesNeeded: [],
    nextAction: "MONITOR_WORK" }], nextAction: "VIEW_PROJECTS" });
  return renderToStaticMarkup(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}><App /></MemoryRouter>
    </QueryClientProvider>,
  );
}

for (const profile of [homeowner, tradesperson]) {
  test(`${profile.role} project sections resolve generated links and direct entries with the same project context`, () => {
    for (const id of [1, 741, 852]) {
      const overview = projectRoute(profile, id, `/projects/${id}`);
      assert.doesNotMatch(overview, /Page not found/);
      const nav = overview.match(/<nav[^>]*aria-label="Project sections"[^>]*>([\s\S]*?)<\/nav>/)?.[1];
      assert.ok(nav, "Project section navigation renders");
      const links = [...nav.matchAll(/href="([^"]+)"/g)].map((match) => match[1]);
      const sections = ["", "tasks", "team", "bids", "messages", "completion"];
      assert.deepEqual(links, sections.map((section) => `/projects/${id}${section ? `/${section}` : ""}`));
      for (const [index, path] of links.entries()) {
        // Each render starts a fresh router and independently loaded account/project data.
        const html = projectRoute(profile, id, path);
        assert.doesNotMatch(html, /Page not found|Project section not found/);
        assert.match(html, new RegExp(`Project ${id}`));
        assert.match(html, new RegExp(`Scope for ${id}`));
        assert.match(html, /value="37"/);
        const section = sections[index];
        if (section === "tasks" || section === "bids") assert.match(html, new RegExp(`Task for ${id}`));
        if (section === "team") assert.match(html, /Your team starts here/);
        if (section === "messages") assert.match(html, /No accessible conversations/);
        if (section === "completion") assert.match(html, /A thoughtful finish/);
      }
    }
    queryClient.clear();
  });
  test(`${profile.role} invalid project URLs still reach Page not found`, () => {
    for (const path of ["/not-a-page", "/projects/741/tasks/extra", "/projects/741/invalid-section"])
      assert.match(projectRoute(profile, 741, path), /Page not found/);
    queryClient.clear();
  });
}

for (const profile of [homeowner, tradesperson]) {
  test(`${profile.role} exact Tasks hash URLs preserve the section and project`, () => {
    for (const taskId of [2, 3]) {
      const html = projectRoute(profile, 1, `/projects/1/tasks#task-${taskId}`, taskId);
      assert.doesNotMatch(html, /Page not found|Project section not found/);
      assert.match(html, /Project 1/);
      assert.match(html, new RegExp(`id="task-${taskId}"`));
      if (profile.role === "HOMEOWNER") {
        assert.match(html, /The work, step by step/);
        assert.match(html, /task-detail-open/);
        assert.match(html, new RegExp(`href="/projects/1/tasks#task-${taskId}"`));
      }
    }
    const missing = projectRoute(profile, 1, "/projects/1/tasks#task-999", 2);
    assert.doesNotMatch(missing, /Page not found|Project section not found/);
    assert.match(missing, /id="task-2"/);
    if (profile.role === "HOMEOWNER") assert.match(missing, /task-list-open/);
    queryClient.clear();
  });
}

test("all shared and public RH&P wordmarks link to the public homepage accessibly", () => {
  const screens = [
    ...[homeowner, tradesperson].flatMap((profile) => [
      shell(profile),
      projectRoute(profile, 1, "/projects/1/tasks#task-2", 2),
    ]),
    projectRoute(homeowner, 1, "/people"),
    projectRoute(homeowner, 1, "/"),
  ];
  for (const html of screens) {
    const logos = [...html.matchAll(/<a\b[^>]*>[\s\S]*?<\/a>/g)]
      .map((match) => match[0]).filter((link) => link.includes('class="brand-lockup"'));
    assert.equal(logos.length, 2, "Both desktop/mobile or header/footer logos are present");
    for (const logo of logos) {
      assert.match(logo, /href="\/"/);
      assert.match(logo, /aria-label="Rock &amp; Hard Places home"/);
      assert.doesNotMatch(logo, /tabindex="-1"/);
    }
  }
  queryClient.clear();
});
