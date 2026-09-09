import type { Profile, Task } from "../../../api/types";
import { Empty } from "../../../components/ui";
import { TaskBids } from "../../projectWork";
export function ProjectBids({
  tasks,
  profile,
}: {
  tasks: Task[];
  profile: Profile;
}) {
  return (
    <section className="build-proposals">
      <div className="build-section-heading">
        <div>
          <h2>People & proposals</h2>
          <p>
            Compare proposals within each task and required trade. Acceptance
            covers that scope, not the whole project.
          </p>
        </div>
      </div>
      {tasks.map((task) => (
        <TaskBids key={task.id} task={task} profile={profile} hasSubtasks={tasks.some(child => child.parentTaskId === task.id)} />
      ))}
      {!tasks.length && (
        <Empty title="Start with a scope of work.">
          Add tasks and required trades to begin planning proposals.
        </Empty>
      )}
    </section>
  );
}
