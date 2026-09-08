import { Link } from "react-router";
import type { Profile, Task } from "../../../api/types";
import { State, words } from "../../../components/ui";
import { useProjectAssignments } from "./useProjectAssignments";
export function CurrentWork({
  profile,
  tasks,
  projectId,
}: {
  profile: Profile;
  tasks: Task[];
  projectId: number;
}) {
  const work = tasks.filter(
    (task) =>
      task.status === "IN_PROGRESS" || task.status === "READY_FOR_REVIEW",
  );
  const assignments = useProjectAssignments(profile, work);
  return (
    <section className="current-build-work">
      <div className="build-section-heading">
        <h2>What’s happening now</h2>
        <Link to={`/projects/${projectId}/tasks`}>All tasks →</Link>
      </div>
      {work.length ? (
        work.slice(0, 3).map((task) => (
          <article className="current-build-task" key={task.id}>
            <span
              className={`work-indicator ${task.status.toLowerCase()}`}
              aria-hidden="true"
            />
            <div>
              <small>{words(task.status)}</small>
              <h3>
                <Link to={`/projects/${projectId}/tasks#task-${task.id}`}>
                  {task.title}
                </Link>
              </h3>
              <p>
                {task.requiredTrades
                  .map((trade) => trade.tradeName)
                  .join(" · ") || "No trade specified"}
              </p>
              <State query={assignments}>
                <p>
                  {assignments.data
                    .filter((row) => row.task.id === task.id)
                    .map((row) => row.assignment.displayName)
                    .join(", ") || "Unassigned"}
                </p>
              </State>
            </div>
            <span>{task.progressPercentage}%</span>
          </article>
        ))
      ) : (
        <p>
          No work is currently in progress or awaiting review. Open the plan to
          see your tasks.
        </p>
      )}
    </section>
  );
}
