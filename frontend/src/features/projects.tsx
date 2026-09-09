import { ProjectForm } from "./projectForm";
export { ProjectForm } from "./projectForm";
import { HomeProjectWorkspace } from "./homeowner/project/HomeProjectWorkspace";
import { TaskForm, TaskRow, TaskBids } from "./projectWork";
export { BidCard } from "./projectWork";
import { Link, NavLink, useParams } from "react-router";
import { workspaceApi } from "../api/workspace";
import type { Profile, Team } from "../api/types";
import {
  Empty,
  Header,
  Portrait,
  Progress,
  State,
  useData,
  words,
} from "../components/ui";
import { Messages } from "./messages";
import { TaskReview } from "./reviews";
function LegacyProjectWorkspace({
  profile,
  userId,
}: {
  profile: Profile;
  userId: number;
}) {
  const { projectId, section = "overview" } = useParams();
  const id = Number(projectId);
  const query = useData(profile, `project-${id}`, (signal) =>
    workspaceApi.project(id, signal),
  );
  const tasks = useData(profile, `tasks-${id}`, (signal) =>
    workspaceApi.tasks(id, signal),
  );
  const team = useData(profile, `team-${id}`, (signal) =>
    workspaceApi.team(id, signal),
  );
  return (
    <State query={query}>
      {query.data && (
        <>
          <Link to={profile.role === "HOMEOWNER" ? "/projects" : "/work"}>
            ← Back to {profile.role === "HOMEOWNER" ? "projects" : "my work"}
          </Link>
          {section === "edit" ? (
            <ProjectForm profile={profile} project={query.data} />
          ) : (
            <>
              <Header
                eyebrow={`${words(query.data.status)} · ZIP ${query.data.jobZip}`}
                title={query.data.title}
              />
              <div className="project-summary">
                <p>{query.data.description}</p>
                <Progress value={query.data.progressPercentage} />
              </div>
              <nav className="project-tabs" aria-label="Project sections">
                {[
                  "overview",
                  "tasks",
                  "team",
                  "bids",
                  "messages",
                  "completion",
                ].map((tab) => (
                  <NavLink
                    key={tab}
                    end
                    to={`/projects/${id}${tab === "overview" ? "" : `/${tab}`}`}
                  >
                    {words(tab)}
                  </NavLink>
                ))}
              </nav>
              {section === "messages" ? (
                <Messages profile={profile} userId={userId} projectId={id} />
              ) : section === "team" ? (
                <State query={team}>
                  <TeamList team={team.data ?? []} />
                </State>
              ) : section === "tasks" || section === "bids" ? (
                <State query={tasks}>
                  {section === "tasks" && profile.role === "HOMEOWNER" && (
                    <TaskForm
                      profile={profile}
                      projectId={id}
                      tasks={tasks.data ?? []}
                    />
                  )}
                  {tasks.data?.map((task) =>
                    section === "bids" ? (
                      <TaskBids key={task.id} task={task} profile={profile} />
                    ) : (
                      <TaskRow
                        key={task.id}
                        task={task}
                        tasks={tasks.data ?? []}
                        profile={profile}
                        team={team.data ?? []}
                      />
                    ),
                  )}
                  {tasks.data?.length === 0 && (
                    <Empty title="The plan is ready for its first task.">
                      Break your vision into clear pieces of work.
                    </Empty>
                  )}
                </State>
              ) : section === "completion" ? (
                <>
                  <h2>A thoughtful finish</h2>
                  <p>
                    Project lifecycle:{" "}
                    <strong>{words(query.data.status)}</strong>. Work progress
                    and project completion are separate.
                  </p>
                  <State query={tasks}>
                    {tasks.data
                      ?.filter((t) => t.status === "COMPLETED")
                      .map((task) => (
                        <section className="panel" key={task.id}>
                          <h3>{task.title}</h3>
                          <p>Completed · {task.progressPercentage}%</p>
                          {profile.role === "HOMEOWNER" && (
                            <TaskReview profile={profile} task={task} />
                          )}
                        </section>
                      ))}
                  </State>
                  <Empty title="Completion records & feedback">
                    Follow task progress and review completed work in Tasks.
                  </Empty>
                  <Link to={`/projects/${id}/tasks`}>Review tasks →</Link>
                </>
              ) : section === "overview" ? (
                <div className="split">
                  <section className="panel">
                    <p className="eyebrow">The plan</p>
                    <h2>Know what happens next.</h2>
                    <p>
                      Follow individual scopes, see who is assigned, and keep
                      decisions in the project conversation.
                    </p>
                    <div className="actions">
                      <Link className="button" to={`/projects/${id}/tasks`}>
                        Open tasks
                      </Link>
                      {profile.role === "HOMEOWNER" && (
                        <Link to={`/projects/${id}/edit`}>Edit project</Link>
                      )}
                    </div>
                  </section>
                  <section className="panel">
                    <h2>The people behind it</h2>
                    <State query={team}>
                      <TeamList team={team.data ?? []} />
                    </State>
                  </section>
                </div>
              ) : (
                <Empty title="Page not found">
                  <Link to={`/projects/${id}`}>Return to project overview</Link>
                </Empty>
              )}
            </>
          )}
        </>
      )}
    </State>
  );
}
function TeamList({ team }: { team: Team[] }) {
  return (
    <>
      {team.map((member) => (
        <article className="person-row" key={member.id}>
          <Portrait name={member.displayName} reference={member.profileImageReference} />
          <div>
            <h3>{member.displayName}</h3>
            <p>Membership: {words(member.status)}</p>
            <small>
              Roles on this project:{" "}
              {member.trades.join(", ") || "None recorded"}
            </small>
          </div>
        </article>
      ))}
      {!team.length && (
        <Empty title="Your team starts here.">
          Accept a task-trade bid to bring a skilled person into the project.
        </Empty>
      )}
    </>
  );
}

export function ProjectWorkspace(props: { profile: Profile; userId: number }) {
  return props.profile.role === "HOMEOWNER" ? (
    <HomeProjectWorkspace {...props} />
  ) : (
    <LegacyProjectWorkspace {...props} />
  );
}
