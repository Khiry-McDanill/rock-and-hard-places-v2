import { Link } from "react-router";
import { api } from "../../api/client";
import type { Profile, ProjectSummary } from "../../api/types";
import {
  Empty,
  Header,
  Portrait,
  State,
  useData,
  words,
} from "../../components/ui";
import { ProjectFeature, nextStep } from "./ProjectFeature";

export function HomeOverview({ profile }: { profile: Profile }) {
  const query = useData(profile, "overview", api.homeownerDashboard);
  const projects = query.data?.projects ?? [];
  const primary =
    projects.find((s) => s.project.status === "IN_PROGRESS") ??
    projects.find((s) => s.project.status === "PLANNING") ??
    projects[0];
  const firstName = profile.displayName.trim().split(/\s+/)[0];
  return (
    <div className="home-overview">
      <Header
        eyebrow="Homeowner dashboard"
        title={firstName ? `Good morning, ${firstName}.` : "Good morning."}
        action={
          <Link className="button" to="/projects/new">
            <span aria-hidden="true">＋</span> New project
          </Link>
        }
      >
        Let’s keep your project moving forward.
      </Header>
      <State query={query}>
        {primary ? (
          <>
            <ProjectFeature summary={primary} />
            <div className="home-context">
              <NextStep summary={primary} />
              <Team summary={primary} />
              <TradesNeeded summary={primary} />
            </div>
            {projects.length > 1 && (
              <section className="other-projects">
                <div className="section-heading">
                  <h2>Also in your workspace</h2>
                  <Link to="/projects">View all →</Link>
                </div>
                {projects
                  .filter((s) => s !== primary)
                  .slice(0, 2)
                  .map((s) => (
                    <Link
                      className="other-project"
                      to={`/projects/${s.project.id}`}
                      key={s.project.id}
                    >
                      <span>
                        {s.project.title}
                        <small>{words(s.project.status)}</small>
                      </span>
                      <span>
                        {s.project.progressPercentage}%{" "}
                        <span aria-hidden="true">↗</span>
                      </span>
                    </Link>
                  ))}
              </section>
            )}
          </>
        ) : (
          query.data && (
            <Empty title="Every build starts with an idea.">
              A cabin, a rolling home, a workshop, or something entirely your
              own. Create your first project to start shaping the plan.
            </Empty>
          )
        )}
      </State>
    </div>
  );
}
function NextStep({ summary }: { summary: ProjectSummary }) {
  const next = nextStep(summary);
  const reviewTask =
    summary.nextAction === "REVIEW_WORK"
      ? summary.awaitingReview[0]
      : undefined;
  return (
    <section className="next-step">
      <h2>Your next step</h2>
      <p>{reviewTask ? <>Review “{reviewTask.title}”.</> : next.body}</p>
      <Link className="button" to={next.to}>
        {next.label}
      </Link>
    </section>
  );
}
function Team({ summary }: { summary: ProjectSummary }) {
  const team = summary.team.filter((member) => member.status === "ACTIVE");
  return (
    <section className="team-context">
      <h2>Your team</h2>
      {team.length ? (
        <>
          <p>
            <strong>
              {team.length}{" "}
              {team.length === 1 ? "tradesperson" : "tradespeople"}
            </strong>
            <br />
            working on your project
          </p>
          <div className="team-preview">
            {team.slice(0, 4).map((member) => (
              <Link
                className="team-person"
                to={`/people/${member.tradespersonId}`}
                key={member.id}
                aria-label={`${member.displayName}${member.trades.length ? ` — ${member.trades.join(", ")}` : ""}`}
                title={`${member.displayName} · ${member.trades.join(", ")}`}
              >
                <Portrait name={member.displayName} reference={member.profileImageReference} />
              </Link>
            ))}
          </div>
        </>
      ) : (
        <p>People will appear here as they join your project.</p>
      )}
      <Link
        className="context-link"
        to={`/projects/${summary.project.id}/team`}
      >
        View team →
      </Link>
    </section>
  );
}
function TradesNeeded({ summary }: { summary: ProjectSummary }) {
  return (
    <section className="trades-context">
      <h2>Trades you still need</h2>
      {summary.tradesNeeded.length ? (
        <>
          <ul>
            {summary.tradesNeeded.slice(0, 2).map((need) => (
              <li key={`${need.taskId}-${need.requiredTrade.id}`}>
                <span className="trade-symbol" aria-hidden="true">
                  ⌂
                </span>
                <span>
                  <strong>{need.requiredTrade.tradeName}</strong>
                  <small>{need.taskTitle}</small>
                </span>
              </li>
            ))}
          </ul>
          {summary.tradesNeeded.length > 2 && (
            <Link
              className="context-link"
              to={`/projects/${summary.project.id}/tasks`}
            >
              View all {summary.tradesNeeded.length} needs →
            </Link>
          )}
          <Link className="button" to="/people">
            Find tradespeople
          </Link>
        </>
      ) : (
        <>
          <p>No open trade needs in your current plan.</p>
          <Link
            className="context-link"
            to={`/projects/${summary.project.id}/tasks`}
          >
            View project plan →
          </Link>
        </>
      )}
    </section>
  );
}
