import { useEffect, useId, useRef, useState } from "react";
import type { Portfolio } from "../api/workspace";
import { Link } from "react-router";
import { resolvePortfolioMedia, type ProjectMediaFrame } from "./seedMedia";

const provenance = {
  RHP_VERIFIED: "RH&P verified",
  EXTERNALLY_VERIFIED: "Externally verified",
  SELF_REPORTED: "Self-reported",
};

export function PortfolioCard({ item, name, delivered = [] }: {
  item: Portfolio;
  name: string;
  // Only portfolio-approved, delivered media may enter this gallery.
  // Attachment IDs from the current endpoint are not deliverable URLs.
  delivered?: readonly ProjectMediaFrame[];
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const titleId = useId();
  const [open, setOpen] = useState(false);
  const [failed, setFailed] = useState<string[]>([]);
  const [selected, setSelected] = useState<string>();
  const isProject = item.provenance === "RHP_VERIFIED";
  const resolved = resolvePortfolioMedia(name, item, delivered.filter((image) => !failed.includes(image.url)));
  const frames = resolved.frames.filter((image) => !failed.includes(image.url));
  const cover = frames.find((image) => image.url === resolved.cover?.url) ?? frames.at(-1);
  const current = frames.find((image) => image.url === selected) ?? cover;
  const stageCount = new Set(frames.flatMap((image) => image.stage ? [image.stage] : [])).size;
  const image = (media?: ProjectMediaFrame) => (
    <span className="portfolio-image">
      {media ? <img src={media.url} alt={media.alt}
        onError={() => setFailed((urls) => [...urls, media.url])} />
        : <span className="portfolio-image-placeholder">{isProject ? "No project photos yet" : "No work photos yet"}</span>}
    </span>
  );

  useEffect(() => {
    if (!open) return;
    const element = dialog.current!;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    element.showModal();
    return () => {
      element.close();
      document.body.style.overflow = previousOverflow;
      trigger.current?.focus();
    };
  }, [open]);

  return (
    <article className="portfolio-item">
      <button ref={trigger} className="portfolio-card" aria-haspopup="dialog"
        aria-label={`View portfolio: ${item.title}`} onClick={() => { setSelected(undefined); setOpen(true); }}>
        {image(cover)}
        <span className="portfolio-card-copy">
          <span className="provenance">{provenance[item.provenance]}</span>
          <span className="portfolio-card-title">{item.title}</span>
          <span className="portfolio-card-description">{item.description}</span>
          {item.completionDate && <span>Completed <time dateTime={item.completionDate}>{item.completionDate}</time></span>}
          {isProject && stageCount > 0 && <span>{stageCount} {stageCount === 1 ? "stage" : "stages"}</span>}
          <span className="portfolio-card-open">{isProject ? "View project story" : "View work"} <span aria-hidden="true">→</span></span>
        </span>
      </button>
      {open && <dialog ref={dialog} className="portfolio-dialog" aria-labelledby={titleId}
        onCancel={() => setOpen(false)} onClose={() => setOpen(false)}>
        <div className="portfolio-dialog-toolbar">
          <span>{isProject ? "Project story" : "Portfolio work"}</span>
          <button autoFocus className="secondary" onClick={() => setOpen(false)} aria-label="Close portfolio detail">Close ×</button>
        </div>
        <div className="portfolio-detail-copy">
          <p className="provenance">{provenance[item.provenance]}</p>
          <h2 id={titleId}>{item.title}</h2>
          {item.completionDate && <p>Completed <time dateTime={item.completionDate}>{item.completionDate}</time></p>}
          <p className="portfolio-full-description">{item.description}</p>
          {isProject && item.projectId != null && (
            <Link to={`/projects/${item.projectId}`}>View RH&amp;P project →</Link>
          )}
        </div>
        <section className="portfolio-gallery" aria-label={isProject ? "Project stage gallery" : "Work gallery"}>
          {image(current)}
          {current && <p className="portfolio-selected-caption">{isProject ? current.stage ?? current.caption : current.caption}</p>}
          {frames.length > 1 && (
            <div className="portfolio-stage-controls" aria-label={isProject ? "Choose project stage" : "Choose work photo"}>
              {frames.map((media, index) => (
                <button key={media.url} className="secondary" aria-pressed={media.url === current?.url}
                  onClick={() => setSelected(media.url)}>
                  <img src={media.url} alt="" loading="lazy"
                    onError={() => setFailed((urls) => [...urls, media.url])} />
                  <span>{isProject ? media.stage ?? media.caption ?? `Photo ${index + 1}` : media.caption ?? `Photo ${index + 1}`}</span>
                </button>
              ))}
            </div>
          )}
        </section>
      </dialog>}
    </article>
  );
}
