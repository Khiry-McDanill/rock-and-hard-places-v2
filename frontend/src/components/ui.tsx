import { ApiError } from "../api/client";
import { useState, type ReactNode } from "react";
import { deliveredMediaUrl, seedPortrait, type PersonMediaContext } from "./seedMedia";
import { ProjectImage } from "./ProjectImage";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Link } from "react-router";
import type { Profile, Project } from "../api/types";
import { profileKey, refreshProfileData } from "../app/query";
/** Product copy only; raw service errors and field details stay internal. */
export function productError(error: Error | null) {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
      case 422: return "Check your entries and try again.";
      case 401: return "Your session could not be confirmed. Refresh and try again.";
      case 403: return "You don’t have access to this information or action.";
      case 404: return "We couldn’t find what you’re looking for.";
      case 409: return "Something changed. Refresh and try again.";
      case 429: return "Please wait a moment and try again.";
    }
  }
  return "Something went wrong. Please try again.";
}
export const words = (value: string) =>
  value === "READY_FOR_REVIEW"
    ? "Awaiting your review"
    : value
        .toLowerCase()
        .replaceAll("_", " ")
        .replace(/^./, (c) => c.toUpperCase());
export function useData<T>(
  profile: Profile,
  key: string,
  fn: (signal: AbortSignal) => Promise<T>,
) {
  return useQuery({
    queryKey: profileKey(profile, key),
    queryFn: ({ signal }) => fn(signal),
  });
}
export function State({
  query,
  children,
}: {
  query: {
    isPending: boolean;
    isError: boolean;
    error: Error | null;
    refetch: () => unknown;
  };
  children: ReactNode;
}) {
  if (query.isPending)
    return (
      <div className="state" role="status">
        Bringing your workspace together…
      </div>
    );
  if (query.isError)
    return (
      <div className="state" role="alert">
        <h2>This view couldn’t be loaded.</h2>
        <p>{productError(query.error)}</p>
        <button onClick={() => query.refetch()}>Try again</button>
      </div>
    );
  return <>{children}</>;
}
export function Empty({
  title,
  children,
}: {
  title: string;
  children?: ReactNode;
}) {
  return (
    <div className="empty">
      <h3>{title}</h3>
      <p>{children}</p>
    </div>
  );
}
export function Header({
  eyebrow,
  title,
  children,
  action,
}: {
  eyebrow: string;
  title: string;
  children?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <header className="page-header">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        {children && <p className="lede">{children}</p>}
      </div>
      {action}
    </header>
  );
}
export function Portrait({ name, reference, url }: PersonMediaContext) {
  const [failed, setFailed] = useState<string[]>([]);
  const actual = deliveredMediaUrl(url) ?? deliveredMediaUrl(reference);
  const seed = seedPortrait({ name, reference });
  const src = [actual, seed].find((candidate) => candidate && !failed.includes(candidate));
  return <span className="portrait" role="img" aria-label={name}>
    {src ? <img src={src} alt="" width="480" height="480" loading="lazy" onError={() => setFailed((urls) => [...urls, src])} />
      : name.split(" ").map((n) => n[0]).slice(0, 2).join("")}
  </span>;
}
export function Progress({ value }: { value: number }) {
  return (
    <div className="progress">
      <div>
        <span>Work completed</span>
        <strong>{value}%</strong>
      </div>
      <progress value={value} max="100" aria-label="Work completed" />
    </div>
  );
}
export function ProjectCard({
  project,
  children,
}: {
  project: Project;
  children?: ReactNode;
}) {
  return (
    <article className="project-card">
      <ProjectImage context={project} loading="lazy" />
      <div className="card-body">
        <p className="eyebrow">
          {words(project.status)} · ZIP {project.jobZip}
        </p>
        <h2>
          <Link to={`/projects/${project.id}`}>{project.title}</Link>
        </h2>
        <p className="clamp">{project.description}</p>
        <Progress value={project.progressPercentage} />
        {children}
        <Link className="text-link" to={`/projects/${project.id}`}>
          Open project →
        </Link>
      </div>
    </article>
  );
}
export function Action({
  label,
  run,
  confirm,
}: {
  label: string;
  run: () => Promise<unknown>;
  confirm?: string;
}) {
  const mutation = useMutation({
    mutationFn: run,
    onSuccess: refreshProfileData,
  });
  return (
    <div>
      <button
        disabled={mutation.isPending}
        onClick={() => {
          if (!confirm || window.confirm(confirm)) mutation.mutate();
        }}
      >
        {mutation.isPending ? "Saving…" : label}
      </button>
      {mutation.isError && <p role="alert">{productError(mutation.error)}</p>}
      {mutation.isSuccess && (
        <p role="status">Saved. Your workspace is up to date.</p>
      )}
    </div>
  );
}
export function MutationNotice({
  mutation,
}: {
  mutation: { isError: boolean; error: Error | null; isSuccess: boolean };
}) {
  return (
    <>
      {mutation.isError && (
        <div role="alert">
          <p>{productError(mutation.error)}</p>
        </div>
      )}
      {mutation.isSuccess && <p role="status">Saved successfully.</p>}
    </>
  );
}
