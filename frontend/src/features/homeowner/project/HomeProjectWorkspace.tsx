import { Link, useParams } from "react-router";
import { api } from "../../../api/client";
import { workspaceApi } from "../../../api/workspace";
import type { Profile } from "../../../api/types";
import { Empty, State, useData, words } from "../../../components/ui";
import { ProjectForm } from "../../projectForm";
import { Messages } from "../../messages";
import { TaskReview } from "../../reviews";
import { ProjectHeader, ProjectSectionNav } from "./ProjectHeader";
import { ProjectMediaGallery } from "./ProjectMediaGallery";
import {
  NextAction,
  ProjectMetricRow,
  ProjectTradesNeeded,
} from "./ProjectSummary";
import { CurrentWork } from "./CurrentWork";
import { ProjectTeam } from "./ProjectTeam";
import { ProjectTasks } from "./ProjectTasks";
import { ProjectBids } from "./ProjectBids";
export function HomeProjectWorkspace({
  profile,
  userId,
}: {
  profile: Profile;
  userId: number;
}) {
  const { projectId, section = "overview" } = useParams();
  const id = Number(projectId);
  const project = useData(profile, `project-${id}`, (signal) =>
    workspaceApi.project(id, signal),
  );
  const tasks = useData(profile, `tasks-${id}`, (signal) =>
    workspaceApi.tasks(id, signal),
  );
  const team = useData(profile, `team-${id}`, (signal) =>
    workspaceApi.team(id, signal),
  );
  const dashboard = useData(profile, "overview", api.homeownerDashboard);
  const summary = dashboard.data?.projects.find(
    (summary) => summary.project.id === id,
  );
  return (
    <div className="home-project">
      <State query={project}>
        {project.data &&
          (section === "edit" ? (
            <ProjectForm profile={profile} project={project.data} />
          ) : (
            <>
              <ProjectHeader project={project.data} />
              <ProjectSectionNav projectId={id} section={section} />
              {section === "overview" ? (
                <>
                  <State query={dashboard}>
                    {summary && <ProjectMetricRow summary={summary} />}
                  </State>
                  <div className="build-overview-grid">
                    <div>
                      <ProjectMediaGallery key={id} project={project.data} />
                      <State query={team}>
                        <State query={tasks}>
                          <ProjectTeam
                            profile={profile}
                            team={team.data ?? []}
                            tasks={tasks.data ?? []}
                            projectId={id}
                            preview
                          />
                        </State>
                      </State>
                    </div>
                    <div>
                      <State query={dashboard}>
                        {summary && <NextAction summary={summary} />}
                      </State>
                      <State query={tasks}>
                        <CurrentWork
                          profile={profile}
                          tasks={tasks.data ?? []}
                          projectId={id}
                        />
                      </State>
                      <State query={dashboard}>
                        {summary && <ProjectTradesNeeded summary={summary} />}
                      </State>
                    </div>
                  </div>
                </>
              ) : section === "tasks" ? (
                <State query={tasks}>
                  <State query={team}>
                    <ProjectTasks
                      profile={profile}
                      tasks={tasks.data ?? []}
                      team={team.data ?? []}
                      projectId={id}
                    />
                  </State>
                </State>
              ) : section === "team" ? (
                <State query={team}>
                  <State query={tasks}>
                    <ProjectTeam
                      profile={profile}
                      team={team.data ?? []}
                      tasks={tasks.data ?? []}
                      projectId={id}
                    />
                  </State>
                </State>
              ) : section === "bids" ? (
                <State query={tasks}>
                  <ProjectBids tasks={tasks.data ?? []} profile={profile} />
                </State>
              ) : section === "messages" ? (
                <Messages profile={profile} userId={userId} projectId={id} />
              ) : section === "completion" ? (
                <section className="build-completion">
                  <h2>A thoughtful finish</h2>
                  <p>
                    Project lifecycle:{" "}
                    <strong>{words(project.data.status)}</strong>. Work progress
                    and project completion are separate.
                  </p>
                  <State query={tasks}>
                    {tasks.data
                      ?.filter((task) => task.status === "COMPLETED")
                      .map((task) => (
                        <section className="panel" key={task.id}>
                          <h3>{task.title}</h3>
                          <p>Completed · {task.progressPercentage}%</p>
                          <TaskReview profile={profile} task={task} />
                        </section>
                      ))}
                  </State>
                  <Empty title="Completion records & feedback">
                    Follow task progress and review completed work in Tasks.
                  </Empty>
                  <Link to={`/projects/${id}/tasks`}>Review tasks →</Link>
                </section>
              ) : (
                <Empty title="Page not found">
                  <Link to={`/projects/${id}`}>Return to project overview</Link>
                </Empty>
              )}
            </>
          ))}
      </State>
    </div>
  );
}
