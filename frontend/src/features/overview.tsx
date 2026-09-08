import { HomeOverview } from "./homeowner/HomeOverview";
import { Link } from "react-router";
import { api } from "../api/client";
import { workspaceApi } from "../api/workspace";
import type { Profile } from "../api/types";
import {
  Empty,
  Header,
  ProjectCard,
  State,
  useData,
  words,
} from "../components/ui";
export function Overview({ profile }: { profile: Profile }) {
  return profile.role === "HOMEOWNER" ? (
    <HomeOverview profile={profile} />
  ) : (
    <TradeOverview profile={profile} />
  );
}
function TradeOverview({ profile }: { profile: Profile }) {
  const query = useData(profile, "overview", api.tradespersonDashboard);
  return (
    <>
      <Header
        eyebrow="Skill with somewhere to go"
        title={`Let’s get to work, ${profile.displayName.split(" ")[0]}.`}
        action={
          <Link className="button" to="/opportunities">
            Find work
          </Link>
        }
      >
        Clear scope. Good people. Work worth putting your name on.
      </Header>
      <State query={query}>
        {query.data && (
          <>
            {!query.data.bidding.allowed && (
              <div className="attention">
                Bidding unavailable: {query.data.bidding.reason}. Account
                verification does not verify licenses or insurance.
              </div>
            )}
            <div className="section-heading">
              <h2>What matters today</h2>
              <Link to="/bids">
                My bids ({query.data.activeBids.length} submitted) →
              </Link>
            </div>
            <div className="work-list">
              {query.data.activeWork.map((work) => (
                <Link
                  className="work-row"
                  to={`/projects/${work.project.id}/tasks`}
                  key={work.assignment.id}
                >
                  <div>
                    <small>{work.project.title}</small>
                    <h3>{work.task.title}</h3>
                    <p>Assigned to you · {words(work.task.status)}</p>
                  </div>
                  <span>Open work →</span>
                </Link>
              ))}
            </div>
            {!query.data.activeWork.length && (
              <Empty title="No active assignments right now.">
                Explore available scopes or revisit your completed work.
              </Empty>
            )}
            <div className="section-heading">
              <h2>A place for your skills</h2>
              <Link to="/opportunities">Explore opportunities →</Link>
            </div>
            <p>
              {query.data.opportunities.length} matching task-trade
              opportunities · {query.data.completedWorkCount} completed assigned
              tasks in accessible memberships.
            </p>
            <Link className="button secondary" to="/work">
              View my work
            </Link>
          </>
        )}
      </State>
    </>
  );
}
export function Projects({ profile }: { profile: Profile }) {
  const query = useData(profile, "projects", workspaceApi.projects);
  return (
    <>
      <Header
        eyebrow={
          profile.role === "HOMEOWNER"
            ? "From idea to real"
            : "Your project memberships"
        }
        title={profile.role === "HOMEOWNER" ? "My projects" : "My work"}
        action={
          profile.role === "HOMEOWNER" ? (
            <Link className="button" to="/projects/new">
              Create a project
            </Link>
          ) : (
            <Link className="button" to="/bids">
              My bids
            </Link>
          )
        }
      />
      {profile.role === "TRADESPERSON" && <AssignedWork profile={profile} />}
      <State query={query}>
        <div className="project-grid">
          {query.data?.map((project) => (
            <ProjectCard key={project.id} project={project} />
          ))}
        </div>
        {query.data?.length === 0 && (
          <Empty title="No projects here yet.">
            Your accessible projects will appear here.
          </Empty>
        )}
      </State>
    </>
  );
}

function AssignedWork({ profile }: { profile: Profile }) {
  const query = useData(profile, "assigned-work", api.assignments);
  return (
    <section>
      <h2>Your assigned scopes</h2>
      <State query={query}>
        {query.data?.map((work) => (
          <Link
            className="work-row"
            key={work.assignment.id}
            to={`/projects/${work.project.id}/tasks`}
          >
            <div>
              <small>{work.project.title}</small>
              <h3>{work.task.title}</h3>
              <p>
                Assigned to you · {words(work.task.status)} ·{" "}
                {work.task.progressPercentage}% work completed
              </p>
            </div>
            <span>Open task workspace →</span>
          </Link>
        ))}
        {query.data?.length === 0 && (
          <Empty title="No assigned work yet.">
            Find an opportunity that fits your skills.
          </Empty>
        )}
      </State>
      <h2>Project memberships</h2>
    </section>
  );
}
