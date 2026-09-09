import { Link } from "react-router";
import { api } from "../../../api/client";
import type { Profile, Task, Team } from "../../../api/types";
import { Empty, Portrait, State, useData, words } from "../../../components/ui";
import { useProjectAssignments } from "./useProjectAssignments";
export function ProjectTeam({
  profile,
  team,
  tasks,
  projectId,
  preview = false,
}: {
  profile: Profile;
  team: Team[];
  tasks: Task[];
  projectId: number;
  preview?: boolean;
}) {
  const people = useData(profile, "people", (signal) => api.people({}, signal));
  const assignments = useProjectAssignments(profile, tasks);
  const shown = preview
    ? team.filter((member) => member.status === "ACTIVE").slice(0, 3)
    : team;
  return (
    <section className={preview ? "build-team-preview" : "build-team-page"}>
      <div className="build-section-heading">
        <h2>{preview ? "Your team" : "The people behind your build"}</h2>
        {preview && <Link to={`/projects/${projectId}/team`}>View team →</Link>}
      </div>
      <div className="build-team-grid">
        {shown.map((member) => {
          const person = people.data?.find(
            (person) => person.profile.id === member.tradespersonId,
          );
          const assigned = assignments.data.filter(
            (row) => row.assignment.tradespersonId === member.tradespersonId,
          );
          return (
            <article className="build-team-member" key={member.id}>
              <div className="build-person-heading">
                <Portrait name={member.displayName} reference={member.profileImageReference} />
                <div>
                  <h3>
                    <Link to={`/people/${member.tradespersonId}`}>
                      {member.displayName}
                    </Link>
                  </h3>
                  <p>
                    Membership · <strong>{words(member.status)}</strong>
                  </p>
                </div>
              </div>
              <dl>
                <div>
                  <dt>Project roles</dt>
                  <dd>
                    {member.trades.join(" · ") || "No project roles recorded"}
                  </dd>
                </div>
                {!preview && (
                  <div>
                    <dt>Qualified trades</dt>
                    <dd>
                      <State query={people}>
                        {person
                          ? person.qualifications
                              .map((trade) => trade.name)
                              .join(" · ") || "None listed"
                          : "No qualification details to show."}
                      </State>
                    </dd>
                  </div>
                )}
              </dl>
              {!preview && (
                <div className="member-assignments">
                  <h4>Task assignments</h4>
                  <State query={assignments}>
                    {assigned.length ? (
                      assigned.map((row) => (
                        <Link
                          key={row.assignment.id}
                          to={`/projects/${projectId}/tasks#task-${row.task.id}`}
                        >
                          {row.task.title}
                          <small>{words(row.task.status)}</small>
                        </Link>
                      ))
                    ) : (
                      <p>No tasks assigned.</p>
                    )}
                  </State>
                </div>
              )}
            </article>
          );
        })}
      </div>
      {!shown.length && (
        <Empty title="Your team starts here.">
          Accept a task-trade bid to bring a skilled person into the project.
        </Empty>
      )}
    </section>
  );
}
