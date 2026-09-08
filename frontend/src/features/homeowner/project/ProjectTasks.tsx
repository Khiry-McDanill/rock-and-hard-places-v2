import { Link, useLocation } from "react-router";
import type { Profile, Task, Team } from "../../../api/types";
import { Empty, words } from "../../../components/ui";
import { useProjectAssignments } from "./useProjectAssignments";
import { TaskForm, TaskRow } from "../../projectWork";
/** Display ordering only; hierarchy and progress remain server-owned. */
export function taskDisplayOrder(tasks: Task[]) {
  const ordered: Task[] = [];
  const visited = new Set<number>();
  const visit = (task: Task) => {
    if (visited.has(task.id)) return;
    visited.add(task.id);
    ordered.push(task);
    tasks.filter((child) => child.parentTaskId === task.id).forEach(visit);
  };
  tasks
    .filter(
      (task) =>
        task.parentTaskId === null ||
        !tasks.some((parent) => parent.id === task.parentTaskId),
    )
    .forEach(visit);
  tasks.forEach(visit);
  return ordered;
}
export function ProjectTasks({
  profile,
  tasks,
  team,
  projectId,
}: {
  profile: Profile;
  tasks: Task[];
  team: Team[];
  projectId: number;
}) {
  const location = useLocation();
  const assignments = useProjectAssignments(profile, tasks);
  const ordered = taskDisplayOrder(tasks);
  const requested = Number(location.hash.replace("#task-", ""));
  const requestedTask = tasks.find((task) => task.id === requested);
  const selected =
    requestedTask ??
    tasks.find((task) => task.status === "READY_FOR_REVIEW") ??
    ordered[0];
  const taskPath = (id: number) => `/projects/${projectId}/tasks#task-${id}`;
  return (
    <section className="build-task-plan">
      <div className="build-section-heading">
        <div>
          <h2>The work, step by step</h2>
          <p>
            Follow each scope, the skills it needs, and the people doing the
            work.
          </p>
        </div>
      </div>
      <TaskForm profile={profile} projectId={projectId} tasks={tasks} />
      {selected ? (
        <div
          className={`build-task-workspace ${requestedTask ? "task-detail-open" : "task-list-open"}`}
        >
          <div className="task-scope-navigation">
            {assignments.isError && (
              <p className="task-assignment-notice" role="alert">
                Some assignments could not be loaded.{" "}
                <button
                  className="secondary"
                  onClick={() => void assignments.refetch()}
                >
                  Retry assignments
                </button>
              </p>
            )}
            <nav className="task-scope-list" aria-label="Project task scopes">
              {ordered.map((task) => (
                <Link
                  className={`task-scope-link ${task.parentTaskId ? "child-scope" : ""}`}
                  key={task.id}
                  to={taskPath(task.id)}
                  aria-current={selected.id === task.id ? "true" : undefined}
                >
                  <small>
                    {task.parentTaskId ? "Subtask" : "Project scope"} ·{" "}
                    {words(task.status)}
                  </small>
                  <strong>{task.title}</strong>
                  <span>
                    {task.requiredTrades
                      .map((trade) => trade.tradeName)
                      .join(" · ") || "No trade specified"}
                  </span>
                  <span className="scope-assignee">
                    {assignments.data
                      .filter((row) => row.task.id === task.id)
                      .map((row) => row.assignment.displayName)
                      .join(", ") ||
                      (assignments.isPending
                        ? "Loading assignment…"
                        : assignments.isError
                          ? "Assignment unavailable"
                          : "Unassigned")}
                  </span>
                  <span className="scope-progress">
                    {task.progressPercentage}% work completed
                  </span>
                </Link>
              ))}
            </nav>
          </div>
          <div className="selected-build-task">
            <Link
              className="task-list-back"
              to={`/projects/${projectId}/tasks`}
            >
              ← Back to task list
            </Link>
            <TaskRow
              key={selected.id}
              task={selected}
              tasks={tasks}
              profile={profile}
              team={team}
            />
          </div>
        </div>
      ) : (
        <Empty title="The plan is ready for its first task.">
          Break your vision into clear pieces of work.
        </Empty>
      )}
    </section>
  );
}
