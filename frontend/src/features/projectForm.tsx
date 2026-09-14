import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router";
import type { Profile, Project } from "../api/types";
import {
  planProject,
  createApprovedProject,
  type ApprovedProjectDraft,
  type Answer,
  type PlanResponse,
} from "../api/projectBuilder";
import { api } from "../api/client";
import { refreshProfileData } from "../app/query";
import { Empty } from "../components/ui";
import { ManualProjectForm, type ProjectDraft } from "./manualProjectForm";

import {
  catalogTrade,
  reviewSuggestions,
  reviseItem,
  type ReviewItem as Item,
} from "./projectBuilderDraft";

const blankDraft: ProjectDraft = { title: "", description: "", jobZip: "" };
const confidence = {
  HIGH: "Strong fit",
  MEDIUM: "Likely fit",
  LOW: "Worth exploring",
};

export function ProjectForm({
  profile,
  project,
}: {
  profile: Profile;
  project?: Project;
}) {
  if (profile.role !== "HOMEOWNER")
    return (
      <Empty title="Homeowner workspace required">
        Switch to your homeowner profile to plan a project.
      </Empty>
    );
  if (project) return <ManualProjectForm profile={profile} project={project} />;
  return <ProjectBuilder key={profile.id} profile={profile} />;
}

function ProjectBuilder({ profile }: { profile: Profile }) {
  const navigate = useNavigate();
  const [step, setStep] = useState<
    "entry" | "idea" | "manual" | "questions" | "review"
  >("entry");
  const [manual, setManual] = useState(false);
  const [idea, setIdea] = useState("");
  const [draft, setDraft] = useState<ProjectDraft>(blankDraft);
  const [response, setResponse] = useState<PlanResponse>();
  const [answers, setAnswers] = useState<Answer[]>([]);
  const [trades, setTrades] = useState<Item[]>([]);
  const [tasks, setTasks] = useState<Item[]>([]);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [hasEdits, setHasEdits] = useState(false);
  const generation = useRef(0);
  const descriptionEdited = useRef(false);
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    heading.current?.focus();
  }, [step]);
  useEffect(
    () => () => {
      generation.current += 1;
    },
    [],
  );
  const [submissionKey, setSubmissionKey] = useState(() => crypto.randomUUID());
  const catalog = useQuery({
    queryKey: ["catalog", "trades"],
    queryFn: ({ signal }) => api.trades(signal),
  });
  const create = useMutation({
    mutationFn: (value: ApprovedProjectDraft) =>
      createApprovedProject(value, submissionKey),
    onSuccess: async (result) => {
      await refreshProfileData();
      navigate(`/projects/${result.id}`, { state: { projectCreated: true } });
    },
  });
  function resetDraft(nextManual: boolean) {
    generation.current += 1;
    setManual(nextManual);
    setIdea("");
    setDraft(blankDraft);
    setResponse(undefined);
    setAnswers([]);
    setTrades([]);
    setTasks([]);
    setPending(false);
    setError("");
    setHasEdits(false);
    descriptionEdited.current = false;
    setSubmissionKey(crypto.randomUUID());
    create.reset();
  }
  function startPath(nextManual: boolean) {
    // Returning to the same path edits this draft; choosing the other entry path
    // starts a separate project. Continue manually below is explicitly a continuation.
    if (nextManual !== manual) resetDraft(nextManual);
    setStep(nextManual ? "manual" : "idea");
  }
  const continueManually = () => {
    generation.current += 1;
    setPending(false);
    setError("");
    setManual(true);
    if (!draft.description && !descriptionEdited.current)
      setDraft((previous) => ({ ...previous, description: idea }));
    setStep("manual");
  };
  async function plan(manualDraft?: ProjectDraft) {
    const current = ++generation.current;
    setPending(true);
    setError("");
    try {
      const result = await planProject(
        idea,
        answers,
        manualDraft ?? (manual ? draft : undefined),
      );
      if (generation.current !== current) return;
      setResponse(result);
      if (!manual && !descriptionEdited.current)
        setDraft((previous) => ({
          ...previous,
          description: result.plan.summary,
        }));
      setTrades((previous) =>
        reviewSuggestions(previous, result.plan.suggestedTrades),
      );
      setTasks((previous) => reviewSuggestions(previous, result.plan.tasks));
      setAnswers((previous) => {
        const next = [...previous];
        result.plan.followUpQuestions.forEach((question) => {
          if (!next.some((a) => a.question === question))
            next.push({ question, answer: "" });
        });
        return next;
      });
      setStep(result.plan.followUpQuestions.length ? "questions" : "review");
    } catch (failure) {
      if (generation.current === current)
        setError(
          failure instanceof Error && failure.message === "CONTEXT_TOO_LONG"
            ? "Please shorten your idea or answers before trying again. Your draft is still here."
            : "RH&P couldn't review this plan right now. You can continue manually or try again.",
        );
    } finally {
      if (generation.current === current) setPending(false);
    }
  }
  function update(
    kind: "trade" | "task",
    index: number,
    changes: Partial<Item>,
  ) {
    setHasEdits(true);
    (kind === "trade" ? setTrades : setTasks)((items) =>
      items.map((item, i) => (i === index ? reviseItem(item, changes) : item)),
    );
  }
  const matchTrade = (name: string) => catalogTrade(name, catalog.data);
  const keptTasks = tasks.filter((item) => item.choice === "kept");
  const keptTrades = trades.filter((item) => item.choice === "kept");
  const removedTrades = trades.filter((item) => item.choice === "removed");
  const unresolved = [...tasks, ...trades].some(
    (item) => item.choice !== "removed" && !matchTrade(item.trade),
  );
  const orphanTrade = keptTrades.some(
    (item) =>
      !keptTasks.some(
        (task) => matchTrade(task.trade)?.id === matchTrade(item.trade)?.id,
      ),
  );
  const conflictingTrade = keptTasks.some((task) =>
    removedTrades.some((trade) => {
      const id = matchTrade(trade.trade)?.id;
      return id !== undefined && id === matchTrade(task.trade)?.id;
    }),
  );
  const savedDraft: ApprovedProjectDraft = {
    ...draft,
    tasks: keptTasks.map((item) => ({
      title: item.title ?? "",
      description: item.description ?? "",
      requiredTradeIds: matchTrade(item.trade)
        ? [matchTrade(item.trade)!.id]
        : [],
    })),
  };
  const busy = pending || create.isPending || create.isSuccess;
  const invalidKept =
    unresolved ||
    orphanTrade ||
    conflictingTrade ||
    keptTasks.some((item) => !item.title?.trim() || !item.description?.trim());

  return (
    <div className="project-builder">
      <p className="builder-kicker">RH&P / PROJECT BUILDER</p>
      <h1 ref={heading} tabIndex={-1}>
        {step === "entry"
          ? "How would you like to start?"
          : step === "review"
            ? "Project Review"
            : step === "manual"
              ? "Start with your vision."
              : "Give your idea room to grow."}
      </h1>
      <ol className="builder-progress" aria-label="Project planning progress">
        {["Idea", "Questions", "Plan", "Review"].map((label, i) => (
          <li
            key={label}
            aria-current={
              (
                step === "review"
                  ? i === 3
                  : step === "questions"
                    ? i === 1
                    : i === 0
              )
                ? "step"
                : undefined
            }
          >
            <span>0{i + 1}</span>
            {label}
          </li>
        ))}
      </ol>
      {step === "entry" && (
        <div className="builder-paths">
          <section className="builder-path">
            <span className="builder-number" aria-hidden="true">
              01 / IDEA TO PLAN
            </span>
            <h2>Plan it with RH&P</h2>
            <p>
              Tell us what you want to build. RH&P will help shape the work,
              identify likely trades, and ask the questions that matter.
            </p>
            <button onClick={() => startPath(false)}>
              Build my project plan
            </button>
          </section>
          <section className="builder-path">
            <span className="builder-number" aria-hidden="true">
              02 / YOUR VISION
            </span>
            <h2>Start manually</h2>
            <p>
              Already know the project details? Enter them yourself and RH&P can
              review the plan before you create it.
            </p>
            <button className="secondary" onClick={() => startPath(true)}>
              Enter project details
            </button>
          </section>
        </div>
      )}
      {step === "idea" && (
        <form
          className="form-panel"
          onSubmit={(e) => {
            e.preventDefault();
            void plan();
          }}
        >
          <label>
            Project idea
            <textarea
              required
              rows={7}
              maxLength={6000}
              value={idea}
              disabled={pending}
              onChange={(e) => {
                if (
                  response ||
                  tasks.length ||
                  answers.length ||
                  draft.description
                )
                  resetDraft(false);
                setIdea(e.target.value);
              }}
              placeholder="A tree house in Hockessin, a more welcoming kitchen, a quiet place to work…"
            />
          </label>
          <p>
            Tell us about the place, how you’ll use it, and what matters most.
            You make the final decisions.
          </p>
          <button disabled={pending}>Shape my project</button>
        </form>
      )}
      {step === "manual" && (
        <ManualProjectForm
          profile={profile}
          draft={draft}
          onDraftChange={(value) => {
            if (value.description !== draft.description)
              descriptionEdited.current = true;
            setDraft(value);
          }}
          onReview={(value, withReview) => {
            setDraft(value);
            setError("");
            setStep("review");
            if (withReview) void plan(value);
          }}
        />
      )}
      {pending && (
        <div className="builder-notice" role="status">
          RH&P is shaping your plan… Your draft stays here while we consider the
          details.
          {manual && (
            <div className="actions">
              <button
                className="secondary"
                disabled={create.isPending || create.isSuccess}
                onClick={continueManually}
              >
                Continue manually
              </button>
            </div>
          )}
        </div>
      )}
      {error && (
        <div className="builder-notice" role="alert">
          <p>{error}</p>
          <div className="actions">
            <button onClick={() => void plan()} disabled={busy}>
              Try again
            </button>
            <button
              className="secondary"
              disabled={create.isPending || create.isSuccess}
              onClick={continueManually}
            >
              Continue manually
            </button>
          </div>
        </div>
      )}
      {(step === "questions" || step === "review") && (
        <>
          {response && (
            <section className="builder-understanding">
              <p className="builder-kicker">RH&P UNDERSTANDING</p>
              <h2>{manual ? draft.title : "The shape of your project"}</h2>
              <p>{response.plan.summary}</p>
            </section>
          )}
          {!!answers.length && (
            <section className="builder-questions">
              <h2>Questions that shape the work</h2>
              <p>
                Answer what you can. Open questions can stay in your review.
              </p>
              {answers.map((answer, i) => (
                <label key={answer.question}>
                  <span className="builder-number">
                    {String(i + 1).padStart(2, "0")}
                  </span>
                  {answer.question}
                  <textarea
                    rows={2}
                    maxLength={1500}
                    disabled={busy}
                    value={answer.answer}
                    onChange={(e) =>
                      setAnswers((items) =>
                        items.map((item, j) =>
                          i === j ? { ...item, answer: e.target.value } : item,
                        ),
                      )
                    }
                  />
                </label>
              ))}
              {hasEdits && (
                <p>
                  Updating the plan refreshes unreviewed recommendations. Your
                  edits, decisions, and answers remain.
                </p>
              )}
              <div className="actions">
                <button
                  disabled={busy || !answers.some((a) => a.answer.trim())}
                  onClick={() => void plan()}
                >
                  Update plan with my answers
                </button>
                {step === "questions" && (
                  <button
                    className="secondary"
                    disabled={busy}
                    onClick={() => setStep("review")}
                  >
                    Review proposed plan
                  </button>
                )}
              </div>
            </section>
          )}
          {step === "review" && (
            <>
              <section className="form-panel">
                <h2>Project summary</h2>
                <>
                  <label>
                    Project name
                    <input
                      required
                      value={draft.title}
                      disabled={busy}
                      onChange={(e) =>
                        setDraft({ ...draft, title: e.target.value })
                      }
                    />
                  </label>
                  <label>
                    Your vision
                    <textarea
                      required
                      rows={4}
                      value={draft.description}
                      disabled={busy}
                      onChange={(e) => {
                        descriptionEdited.current = true;
                        setDraft({ ...draft, description: e.target.value });
                      }}
                    />
                  </label>
                  <label>
                    Project ZIP code
                    <input
                      required
                      inputMode="numeric"
                      value={draft.jobZip}
                      disabled={busy}
                      onChange={(e) =>
                        setDraft({ ...draft, jobZip: e.target.value })
                      }
                    />
                  </label>
                </>
                {manual && !response && !error && (
                  <button disabled={busy} onClick={() => void plan()}>
                    Review my plan with RH&P
                  </button>
                )}
              </section>
              {(["trade", "task"] as const).map((kind) => (
                <section key={kind} className="builder-recommendations">
                  <h2>
                    {kind === "trade"
                      ? "Suggested / selected trades"
                      : "Proposed tasks"}
                  </h2>
                  <p>
                    {manual
                      ? "Apply only the recommendations you want to include."
                      : "Keep, edit, or remove each recommendation. You decide what belongs."}
                  </p>
                  {(kind === "trade" ? trades : tasks).map((item, i) => (
                    <article
                      className={`builder-card ${item.choice === "removed" ? "builder-removed" : ""}`}
                      key={i}
                      aria-label={item.title ?? item.trade}
                    >
                      <div className="builder-card-top">
                        <h3>{item.title ?? item.trade}</h3>
                        <span className="builder-tag">
                          {item.choice === "kept"
                            ? manual
                              ? "Applied"
                              : "Kept"
                            : item.choice === "removed"
                              ? manual
                                ? "Ignored"
                                : "Removed"
                              : "Recommended"}
                        </span>
                      </div>
                      {item.title && (
                        <p>
                          {item.description}
                          <br />
                          <strong>Trade: {item.trade}</strong>
                        </p>
                      )}
                      {item.origin === "suggestion" ? (
                        <p>{item.reason}</p>
                      ) : (
                        <p>
                          {item.origin === "added"
                            ? "Added by you."
                            : "Edited by you."}
                        </p>
                      )}
                      <p className="builder-meta">
                        {item.origin === "suggestion" && item.confidence && (
                          <>Confidence: {confidence[item.confidence]}</>
                        )}
                        {item.origin === "suggestion" &&
                          item.needsConfirmation && (
                            <span className="builder-tag">
                              Needs confirmation
                            </span>
                          )}
                        {!matchTrade(item.trade) && (
                          <span className="builder-tag">
                            Trade needs matching
                          </span>
                        )}
                      </p>
                      {item.choice !== "removed" && (
                        <label>
                          Required trade for {item.title ?? item.trade}
                          <select
                            disabled={busy}
                            value={matchTrade(item.trade)?.id ?? ""}
                            onChange={(e) => {
                              const selected = catalog.data?.find(
                                (t) => t.id === Number(e.target.value),
                              );
                              if (selected)
                                update(kind, i, { trade: selected.name });
                            }}
                          >
                            <option value="">Choose an existing trade</option>
                            {catalog.data?.map((t) => (
                              <option key={t.id} value={t.id}>
                                {t.name}
                              </option>
                            ))}
                          </select>
                        </label>
                      )}
                      {item.editing && (
                        <div className="builder-edit">
                          {item.title !== undefined && (
                            <>
                              <label>
                                Task name
                                <input
                                  value={item.title}
                                  disabled={busy}
                                  maxLength={200}
                                  onChange={(e) =>
                                    update(kind, i, { title: e.target.value })
                                  }
                                />
                              </label>
                              <label>
                                Task details
                                <textarea
                                  value={item.description}
                                  disabled={busy}
                                  maxLength={4000}
                                  onChange={(e) =>
                                    update(kind, i, {
                                      description: e.target.value,
                                    })
                                  }
                                />
                              </label>
                            </>
                          )}
                          <label>
                            Trade name
                            <input
                              value={item.trade}
                              disabled={busy}
                              maxLength={200}
                              onChange={(e) =>
                                update(kind, i, { trade: e.target.value })
                              }
                            />
                          </label>
                          <button
                            className="secondary"
                            onClick={() => update(kind, i, { editing: false })}
                          >
                            Done editing
                          </button>
                        </div>
                      )}
                      <div className="actions">
                        <button
                          disabled={
                            busy ||
                            !item.trade.trim() ||
                            (item.title !== undefined &&
                              (!item.title.trim() || !item.description?.trim()))
                          }
                          aria-pressed={item.choice === "kept"}
                          onClick={() => update(kind, i, { choice: "kept" })}
                        >
                          {manual ? "Apply" : "Keep"}
                        </button>
                        <button
                          disabled={busy}
                          className="secondary"
                          onClick={() =>
                            update(kind, i, { editing: !item.editing })
                          }
                        >
                          Edit
                        </button>
                        <button
                          disabled={busy}
                          className="secondary"
                          aria-pressed={item.choice === "removed"}
                          onClick={() => update(kind, i, { choice: "removed" })}
                        >
                          {manual ? "Ignore" : "Remove"}
                        </button>
                      </div>
                    </article>
                  ))}
                  {!(kind === "trade" ? trades : tasks).length && (
                    <p>No {kind === "trade" ? "trades" : "tasks"} added yet.</p>
                  )}
                  <button
                    className="secondary"
                    disabled={busy}
                    onClick={() => {
                      setHasEdits(true);
                      const item: Item = {
                        trade: "",
                        origin: "added",
                        choice: "pending",
                        editing: true,
                        ...(kind === "task"
                          ? { title: "", description: "" }
                          : {}),
                      };
                      (kind === "trade" ? setTrades : setTasks)((items) => [
                        ...items,
                        item,
                      ]);
                    }}
                  >
                    {kind === "task" ? "Add task" : "Add trade"}
                  </button>
                </section>
              ))}
              {response && (
                <div className="builder-notes">
                  {(["assumptions", "warnings"] as const).map((key) => (
                    <section key={key}>
                      <h2>
                        {key === "assumptions" ? "Assumptions" : "Warnings"}
                      </h2>
                      {response.plan[key].length ? (
                        <ul>
                          {response.plan[key].map((note, i) => (
                            <li key={i}>{note}</li>
                          ))}
                        </ul>
                      ) : (
                        <p>None raised in this review.</p>
                      )}
                    </section>
                  ))}
                </div>
              )}
              <section className="builder-notice">
                <h2>
                  {manual
                    ? "Ready when you are."
                    : "Your plan, your decisions."}
                </h2>
                <p>
                  Only kept or applied tasks and their selected catalog trades
                  will be created. Unselected recommendations are excluded. You
                  can create a project without tasks.
                </p>
                {unresolved && (
                  <p role="alert">
                    Choose an existing RH&P trade for each unresolved item, or
                    remove it.
                  </p>
                )}
                {orphanTrade && (
                  <p role="alert">
                    Each kept trade needs a kept task. Add or keep a task for
                    that trade, or remove the trade.
                  </p>
                )}
                {conflictingTrade && (
                  <p role="alert">
                    A kept task uses a removed trade. Change the task’s trade,
                    remove the task, or keep the trade.
                  </p>
                )}
                {catalog.isError && (
                  <p>
                    We couldn’t load the trade catalog.{" "}
                    <button
                      className="secondary"
                      onClick={() => void catalog.refetch()}
                    >
                      Retry trade catalog
                    </button>{" "}
                    You can still create a project without tasks.
                  </p>
                )}
                <label>
                  Description that will be saved
                  <textarea readOnly rows={4} value={savedDraft.description} />
                </label>
                <p>{savedDraft.tasks.length} approved tasks will be created.</p>
                {create.isError && (
                  <div role="alert">
                    <p>
                      We couldn’t create your project. Your review is still
                      here. Retry with the same approval, or check My Projects
                      if an earlier request may have succeeded.
                    </p>
                    <Link to="/projects">My Projects</Link>
                  </div>
                )}
                {create.isPending && (
                  <p role="status">Creating your project and approved tasks…</p>
                )}
                <button
                  disabled={
                    busy ||
                    invalidKept ||
                    !draft.title.trim() ||
                    !draft.description.trim() ||
                    !draft.jobZip.trim()
                  }
                  onClick={() => {
                    if (!create.isPending && !create.isSuccess)
                      create.mutate(savedDraft);
                  }}
                >
                  {create.isPending
                    ? "Creating…"
                    : create.isError
                      ? "Retry Create project"
                      : "Create project"}
                </button>
              </section>
            </>
          )}
        </>
      )}
      <div className="actions builder-footer">
        {step !== "entry" && (
          <button
            className="secondary"
            disabled={busy}
            onClick={() => setStep("entry")}
          >
            Back to start options
          </button>
        )}
        {!manual && step !== "entry" && (
          <button
            className="secondary"
            disabled={create.isPending || create.isSuccess}
            onClick={continueManually}
          >
            Continue manually
          </button>
        )}
        <Link to="/projects">Cancel</Link>
      </div>
    </div>
  );
}
