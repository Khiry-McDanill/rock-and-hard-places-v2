import { useMutation } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router";
import { workspaceApi } from "../api/workspace";
import type { Profile, Project } from "../api/types";
import { refreshProfileData } from "../app/query";
import { Empty, Header, MutationNotice } from "../components/ui";
export interface ProjectDraft {
  title: string;
  description: string;
  jobZip: string;
}
export function ManualProjectForm({
  profile,
  project,
  draft,
  onReview,
  onDraftChange,
}: {
  profile: Profile;
  project?: Project;
  draft?: ProjectDraft;
  onReview?: (draft: ProjectDraft, withReview: boolean) => void;
  onDraftChange?: (draft: ProjectDraft) => void;
}) {
  const navigate = useNavigate();
  const mutation = useMutation({
    mutationFn: (form: FormData) =>
      workspaceApi.saveProject(
        {
          title: String(form.get("title")),
          description: String(form.get("description")),
          jobZip: String(form.get("jobZip")),
        },
        project?.id,
      ),
    onSuccess: async (result) => {
      await refreshProfileData();
      navigate(`/projects/${result.id}`);
    },
  });
  if (profile.role !== "HOMEOWNER")
    return (
      <Empty title="Homeowner workspace required">
        Switch to your homeowner profile to plan a project.
      </Empty>
    );
  return (
    <>
      {!onReview && (
        <Header
          eyebrow="Start with possibility"
          title={
            project ? "Refine your vision" : "What would you like to build?"
          }
        >
          A renovation, a conversion, or an idea all your own. Give it a name
          and a place to begin.
        </Header>
      )}
      <form
        className="form-panel"
        onChange={(e) => {
          if (!onDraftChange) return;
          const form = new FormData(e.currentTarget);
          onDraftChange({
            title: String(form.get("title")),
            description: String(form.get("description")),
            jobZip: String(form.get("jobZip")),
          });
        }}
        onSubmit={(e) => {
          e.preventDefault();
          const form = new FormData(e.currentTarget);
          if (onReview) {
            const submitter = (e.nativeEvent as SubmitEvent)
              .submitter as HTMLButtonElement | null;
            onReview(
              {
                title: String(form.get("title")),
                description: String(form.get("description")),
                jobZip: String(form.get("jobZip")),
              },
              submitter?.value === "review",
            );
          } else mutation.mutate(form);
        }}
      >
        <label>
          Project name
          <input
            name="title"
            required
            defaultValue={project?.title ?? draft?.title}
            placeholder="A name for your vision"
          />
        </label>
        <label>
          Your vision
          <textarea
            name="description"
            required
            rows={5}
            defaultValue={project?.description ?? draft?.description}
            placeholder="Describe the space, the change, and what matters to you."
          />
        </label>
        <label>
          Project ZIP code
          <input
            name="jobZip"
            required
            defaultValue={project?.jobZip ?? draft?.jobZip}
            inputMode="numeric"
          />
        </label>
        <MutationNotice mutation={mutation} />
        <div className="actions">
          <button disabled={mutation.isPending}>
            {mutation.isPending
              ? "Saving…"
              : project
                ? "Save project"
                : "Continue to project review"}
          </button>
          {onReview && (
            <button type="submit" value="review" className="secondary">
              Review my plan with RH&P
            </button>
          )}
          {!onReview && (
            <Link to={project ? `/projects/${project.id}` : "/projects"}>
              Cancel
            </Link>
          )}
        </div>
      </form>
    </>
  );
}
