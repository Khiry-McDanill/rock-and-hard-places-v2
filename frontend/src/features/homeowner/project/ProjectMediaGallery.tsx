import { useState } from "react";
import {
  resolveProjectMedia,
  type ProjectDisplayContext,
  type ProjectMediaFrame,
} from "../../../components/seedProjectMedia";
export function ProjectMediaGallery({
  project,
  delivered = [],
}: {
  project: ProjectDisplayContext;
  delivered?: readonly ProjectMediaFrame[];
}) {
  const [failed, setFailed] = useState<string[]>([]);
  const media = resolveProjectMedia(
    project,
    delivered.filter((frame) => !failed.includes(frame.url)),
  );
  const [selected, setSelected] = useState<string>();
  const frames = media.frames.filter((frame) => !failed.includes(frame.url));
  const current =
    frames.find((frame) => frame.url === selected) ??
    frames.find((frame) => frame.url === media.cover?.url) ?? frames[0];
  return (
    <section className="build-gallery" aria-label="Project progress imagery">
      <div className="build-section-heading">
        <h2>Project progress</h2>
        <span>Progress photos</span>
      </div>
      <figure className="gallery-main">
        {current ? <img
          src={current.url}
          alt={current.alt}
          onError={() => setFailed((urls) => [...urls, current.url])}
        /> : null}
        <figcaption>{current?.caption ?? "No project photos yet"}</figcaption>
      </figure>
      {frames.length > 1 && (
        <div className="gallery-thumbnails" aria-label="Choose project view">
          {frames.map((frame) => (
            <button
              key={frame.url}
              aria-label={`Show ${frame.caption ?? frame.alt}`}
              aria-pressed={frame.url === current.url}
              onClick={() => setSelected(frame.url)}
            >
              <img src={frame.url} alt="" loading="lazy" onError={() => setFailed((urls) => [...urls, frame.url])} />
              <span>{frame.caption ?? "Project photo"}</span>
            </button>
          ))}
        </div>
      )}
    </section>
  );
}
