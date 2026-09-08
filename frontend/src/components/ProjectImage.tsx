import { useState } from "react";
import {
  projectPresentation,
  type ProjectDisplayContext,
} from "./projectPresentation";

export interface DeliveredProjectImage {
  url: string;
  alt: string;
}
/** Delivered media takes precedence; storage references are not deliverable URLs. */
export function ProjectImage({
  media,
  context,
  loading = "eager",
}: {
  media?: DeliveredProjectImage;
  context?: ProjectDisplayContext;
  loading?: "eager" | "lazy";
}) {
  const fallback = projectPresentation(context);
  const [failed, setFailed] = useState<string[]>([]);
  const actual = media && !failed.includes(media.url) ? media : undefined;
  const current = actual ?? (fallback && !failed.includes(fallback.url) ? fallback : undefined);
  return (
    <figure className="project-image">
      {current ? <img
        src={current.url}
        alt={current.alt}
        loading={loading}
        width="960"
        height="640"
        onError={() => setFailed((urls) => [...urls, current.url])}
      /> : <figcaption>No project photos yet</figcaption>}
    </figure>
  );
}
