import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { api } from "../api/client";
import { workspaceApi } from "../api/workspace";
import type { Profile, Task, Team, Bid } from "../api/types";
import { refreshProfileData } from "../app/query";
import {
  Action,
  MutationNotice,
  Portrait,
  Progress,
  State,
  useData,
  words,
} from "../components/ui";
export function TaskForm({
  profile,
  projectId,
  tasks,
}: {
  profile: Profile;
  projectId: number;
  tasks: Task[];
}) {
  const catalog = useData(profile, "catalog", api.trades);
  const [open, setOpen] = useState(false);
  const mutation = useMutation({
    mutationFn: (form: FormData) =>
      workspaceApi.createTask(projectId, {
        title: String(form.get("title")),
        description: String(form.get("description")),
        parentTaskId: form.get("parent") ? Number(form.get("parent")) : null,
        requiredTradeIds: form.getAll("trade").map(Number),
      }),
    onSuccess: async () => {
      await refreshProfileData();
      setOpen(false);
    },
  });
  return (
    <div className="task-create">
      <button
        className="secondary"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
      >
        {open ? "Close task form" : "Add a task or subtask"}
      </button>
      {open && (
        <form
          className="form-panel"
          onSubmit={(e) => {
            e.preventDefault();
            mutation.mutate(new FormData(e.currentTarget));
          }}
        >
          <label>
            Task title
            <input name="title" required />
          </label>
          <label>
            Scope of work
            <textarea name="description" required />
          </label>
          <label>
            Task relationship
            <select name="parent">
              <option value="">Top-level task</option>
              {tasks.map((t) => (
                <option key={t.id} value={t.id}>
                  Subtask of {t.title}
                </option>
              ))}
            </select>
          </label>
          <fieldset>
            <legend>Trades needed</legend>
            <State query={catalog}>
              {catalog.data?.map((trade) => (
                <label className="checkbox" key={trade.id}>
                  <input type="checkbox" name="trade" value={trade.id} />
                  {trade.name}
                </label>
              ))}
            </State>
          </fieldset>
          <MutationNotice mutation={mutation} />
          <button disabled={mutation.isPending}>Save task</button>
        </form>
      )}
    </div>
  );
}
export function TaskRow({
  task,
  tasks,
  profile,
  team,
}: {
  task: Task;
  tasks: Task[];
  profile: Profile;
  team: Team[];
}) {
  const assignments = useData(profile, `assignments-${task.id}`, (signal) =>
    api.taskAssignments(task.id, signal),
  );
  const [member, setMember] = useState("");
  const parent = tasks.find((t) => t.id === task.parentTaskId);
  const own = assignments.data?.some((a) => a.tradespersonId === profile.id);
  return (
    <article
      id={`task-${task.id}`}
      className={`task-row ${parent ? "subtask" : ""}`}
      data-task-status={task.status}
    >
      <div className="section-heading">
        <div>
          {parent && <small>Subtask of {parent.title}</small>}
          <h3>{task.title}</h3>
        </div>
        <span
          className={
            task.status === "READY_FOR_REVIEW" ? "attention" : "task-status"
          }
        >
          {words(task.status)}
        </span>
      </div>
      <p>{task.description}</p>
      <Progress value={task.progressPercentage} />
      <p>
        Trades needed:{" "}
        {task.requiredTrades.map((t) => t.tradeName).join(", ") ||
          "No trade specified"}
      </p>
      <State query={assignments}>
        {profile.role === "HOMEOWNER" &&
          assignments.data &&
          assignments.data.length > 0 && (
            <div className="task-assignees">
              {assignments.data.map((assignment) => (
                <span key={assignment.id} title={assignment.displayName}>
                  <Portrait name={assignment.displayName} reference={assignment.profileImageReference} />
                </span>
              ))}
            </div>
          )}
        <p className="assignment">
          {assignments.data?.length
            ? `Assigned to this task: ${assignments.data.map((a) => a.displayName).join(", ")}`
            : "Unassigned"}
        </p>
      </State>
      <div className="actions">
        {profile.role === "HOMEOWNER" && task.status === "READY_FOR_REVIEW" && (
          <>
            <Action
              label="Approve work"
              confirm="Approve this task’s work as completed?"
              run={() => api.taskAction(task.id, "approve")}
            />
            <Action
              label="Request changes"
              run={() => api.taskAction(task.id, "reject")}
            />
          </>
        )}
        {profile.role === "TRADESPERSON" &&
          own &&
          task.status === "PLANNING" && (
            <Action
              label="Start work"
              run={() => api.taskAction(task.id, "start")}
            />
          )}
        {profile.role === "TRADESPERSON" &&
          own &&
          task.status === "IN_PROGRESS" && (
            <Action
              label="Submit for homeowner review"
              run={() => api.taskAction(task.id, "ready-for-review")}
            />
          )}
      </div>
      {profile.role === "HOMEOWNER" &&
        task.status !== "CANCELLED" &&
        task.status !== "COMPLETED" && (
          <details>
            <summary>Assign a team member</summary>
            <p>
              Choose a qualified team member with the required project role.
            </p>
            <label>
              Active project member
              <select
                value={member}
                onChange={(e) => setMember(e.target.value)}
              >
                <option value="">Choose a person</option>
                {team
                  .filter((m) => m.status === "ACTIVE")
                  .map((m) => (
                    <option key={m.id} value={m.tradespersonId}>
                      {m.displayName} · {m.trades.join(", ")}
                    </option>
                  ))}
              </select>
            </label>
            {member && (
              <Action
                label="Assign to this task"
                run={() => workspaceApi.assign(task.id, Number(member))}
              />
            )}
          </details>
        )}
    </article>
  );
}
export function TaskBids({ task, profile, hasSubtasks = false }: { task: Task; profile: Profile; hasSubtasks?: boolean }) {
  const query = useData(profile, `bids-${task.id}`, (signal) =>
    workspaceApi.bids(task.id, signal),
  );
  return (
    <section className="bid-section">
      <h2>{task.title}</h2>
      {profile.role === "HOMEOWNER" && (
        <p className="bid-scope-trades">
          {task.requiredTrades.map((trade) => trade.tradeName).join(" · ") ||
            "No required trades recorded"}
        </p>
      )}
      <State query={query}>
        <div
          className={
            profile.role === "HOMEOWNER" ? "task-bid-comparison" : undefined
          }
        >
          {query.data?.map((bid) => (
            <BidCard key={bid.id} bid={bid} task={task} profile={profile} />
          ))}
        </div>
        {query.data?.length === 0 && (
          <p className="bid-empty">{hasSubtasks ? "Proposals are recorded under the individual scopes below." : profile.role === "TRADESPERSON" ? "You have no proposals for this task." : "No proposals for this task."}</p>
        )}
      </State>
    </section>
  );
}
export function BidCard({
  bid,
  task,
  profile,
}: {
  bid: Bid;
  task?: Task | null;
  profile: Profile;
}) {
  const person = useData(profile, `person-${bid.tradespersonId}`, (signal) =>
    workspaceApi.person(bid.tradespersonId, signal),
  );
  return (
    <article className="bid-card">
      <div className="section-heading">
        <div>
          {person.data && (
            <Portrait name={person.data.displayName} reference={person.data.profileImageReference} />
          )}
          <h3>{person.data ? <Link to={`/people/${person.data.id}`}>{person.data.displayName}</Link> : "Tradesperson"}</h3>
          <p>Proposal: {words(bid.status)}</p>
        </div>
        <strong className="amount">
          {/* Current U.S. seed bids have no currency field in the API contract. */}
          {bid.amount.toLocaleString("en-US", { style: "currency", currency: "USD" })}
          <small>USD</small>
        </strong>
      </div>
      <p>Task: {task?.title ?? "Scope no longer accessible"}</p>
      <p>
        Required trade:{" "}
        {task?.requiredTrades.find((t) => t.id === bid.taskTradeId)
          ?.tradeName ?? "Unavailable"}
      </p>
      <blockquote>{bid.message || "No proposal text provided."}</blockquote>
      {profile.role === "HOMEOWNER" && bid.status === "SUBMITTED" && (
        <Action
          label="Accept this task-trade bid"
          confirm="Accept this bid for this task and required trade? This adds project membership, the trade role, and a task assignment, and rejects competing submitted bids for this requirement. It does not award the whole project."
          run={() => workspaceApi.accept(bid.id)}
        />
      )}
    </article>
  );
}
import { Link } from "react-router";
