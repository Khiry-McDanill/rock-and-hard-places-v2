import { Link, NavLink, useNavigate } from "react-router";
import type { Project } from "../../../api/types";
import { Progress, words } from "../../../components/ui";
import { ProjectImage } from "../../../components/ProjectImage";
export function ProjectHeader({ project }: { project: Project }) {
  return (
    <>
      <div className="project-breadcrumb">
        <Link to="/projects">← My projects</Link>
        <Link to={`/projects/${project.id}/edit`}>Edit project</Link>
      </div>
      <header className="build-header">
        <ProjectImage context={project} />
        <div className="build-identity">
          <p className="eyebrow">
            {words(project.status)} <span>· ZIP {project.jobZip}</span>
          </p>
          <h1>{project.title}</h1>
          <p>{project.description}</p>
        </div>
        <div className="build-header-progress">
          <Progress value={project.progressPercentage} />
        </div>
      </header>
    </>
  );
}
const sections = [
  "overview",
  "tasks",
  "team",
  "bids",
  "messages",
  "completion",
];
export function ProjectSectionNav({
  projectId,
  section,
}: {
  projectId: number;
  section: string;
}) {
  const navigate = useNavigate();
  const path = (value: string) =>
    `/projects/${projectId}${value === "overview" ? "" : `/${value}`}`;
  return (
    <div className="build-section-bar">
      <nav className="build-section-links" aria-label="Project sections">
        {sections.map((tab) => (
          <NavLink key={tab} end to={path(tab)}>
            {words(tab)}
          </NavLink>
        ))}
      </nav>
      <label className="build-section-select">
        Project section
        <select
          value={sections.includes(section) ? section : "overview"}
          onChange={(e) => navigate(path(e.target.value))}
        >
          {sections.map((tab) => (
            <option key={tab} value={tab}>
              {words(tab)}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}
