import { Link } from "react-router";
import type { ProjectSummary as Summary } from "../../../api/types";
import { nextStep } from "../ProjectFeature";
export function ProjectMetricRow({ summary }: { summary: Summary }) {
  return (
    <dl className="build-metrics">
      <div>
        <dt>Tasks complete</dt>
        <dd>
          {summary.completedTasks}/{summary.totalTasks}
        </dd>
      </div>
      <div>
        <dt>Active tradespeople</dt>
        <dd>
          {summary.team.filter((member) => member.status === "ACTIVE").length}
        </dd>
      </div>
      <div>
        <dt>Awaiting your review</dt>
        <dd>{summary.awaitingReview.length}</dd>
      </div>
      <div>
        <dt>Open trade needs</dt>
        <dd>{summary.tradesNeeded.length}</dd>
      </div>
    </dl>
  );
}
export function NextAction({ summary }: { summary: Summary }) {
  const next = nextStep(summary);
  return (
    <section className="build-next-action">
      <div>
        <p className="eyebrow">Your next step</p>
        <h2>{next.title}</h2>
        <p>{next.body}</p>
      </div>
      <Link className="button" to={next.to}>
        {next.label} →
      </Link>
    </section>
  );
}
export function ProjectTradesNeeded({ summary }: { summary: Summary }) {
  return (
    <section className="build-needs">
      <div className="build-section-heading">
        <h2>Trades still needed</h2>
        {summary.tradesNeeded.length > 0 && (
          <Link to="/people">Find tradespeople →</Link>
        )}
      </div>
      {summary.tradesNeeded.length ? (
        <ul>
          {summary.tradesNeeded.map((need) => (
            <li key={`${need.taskId}-${need.requiredTrade.id}`}>
              <strong>{need.requiredTrade.tradeName}</strong>
              <Link
                to={`/projects/${summary.project.id}/tasks#task-${need.taskId}`}
              >
                {need.taskTitle} →
              </Link>
            </li>
          ))}
        </ul>
      ) : (
        <p>No open trade needs in the current project plan.</p>
      )}
    </section>
  );
}
