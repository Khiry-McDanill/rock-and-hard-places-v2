import { Link } from "react-router";
import type { ProjectSummary } from "../../api/types";
import { ProjectImage } from "../../components/ProjectImage";
import { Progress, words } from "../../components/ui";

export function nextStep(summary: ProjectSummary) {
  const base = `/projects/${summary.project.id}`;
  switch (summary.nextAction) {
    case "REVIEW_WORK":
      return {
        title: "Your work is ready for a look.",
        body: `${summary.awaitingReview.length} task${summary.awaitingReview.length === 1 ? " is" : "s are"} awaiting your approval.`,
        label: "Review work",
        to: `${base}/tasks`,
      };
    case "PLAN_TASKS":
      return {
        title: "Give your idea a plan.",
        body: "Outline the tasks that will bring this project to life.",
        label: "Plan your tasks",
        to: `${base}/tasks`,
      };
    case "FIND_TRADESPEOPLE":
      return {
        title: "Bring the right skills on board.",
        body: "Your project has trade needs ready to connect with people.",
        label: "Find tradespeople",
        to: "/people",
      };
    case "PROJECT_CLOSED":
      return {
        title: "Your project record, all together.",
        body: "Revisit the work and the people behind it.",
        label: "View project",
        to: base,
      };
    case "MONITOR_WORK":
      return {
        title: "Keep an eye on what’s taking shape.",
        body: "Follow your tasks and stay connected with your team.",
        label: "Follow the work",
        to: `${base}/tasks`,
      };
  }
}
export function ProjectFeature({ summary }: { summary: ProjectSummary }) {
  const { project } = summary;
  const activeTeam = summary.team.filter(
    (member) => member.status === "ACTIVE",
  );
  return (
    <article className="project-feature" aria-label="Current project">
      <ProjectImage context={project} />
      <div className="feature-content">
        <div className="feature-title">
          <div>
            <span
              className={`project-lifecycle ${project.status.toLowerCase()}`}
            >
              {words(project.status)}
            </span>
            <h2>{project.title}</h2>
          </div>
          <Link className="button" to={`/projects/${project.id}`}>
            View project
          </Link>
        </div>
        <Progress value={project.progressPercentage} />
        <dl className="project-metrics">
          <div>
            <dt>Tasks complete</dt>
            <dd>
              {summary.completedTasks}/{summary.totalTasks}
            </dd>
          </div>
          <div>
            <dt>Tradespeople</dt>
            <dd>{activeTeam.length}</dd>
          </div>
          <div>
            <dt>Awaiting your approval</dt>
            <dd>{summary.awaitingReview.length}</dd>
          </div>
        </dl>
        {summary.totalSubtasks > 0 && (
          <p className="task-context">
            {summary.completedSubtasks} of {summary.totalSubtasks} subtasks
            complete
          </p>
        )}
      </div>
    </article>
  );
}
