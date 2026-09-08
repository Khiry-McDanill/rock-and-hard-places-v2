import { Link } from 'react-router';
import type { BuildStage, InspirationCategory, InspirationConcept } from './inspirationData';
import { InspirationVisual } from './InspirationVisual';

/** A connected drawing grows from an outline into a frame, services and an inhabited space. */
function StageMark({ stage, category }: { stage: number; category?: string }) {
  const mobile = category === 'rvs' || category === 'buses';
  const industrial = category === 'containers';
  const outdoor = category === 'outdoor-spaces';
  const outline = mobile ? 'M12 52V26q0-8 8-8h54l14 16v18ZM21 52a6 6 0 0 0 12 0m34 0a6 6 0 0 0 12 0'
    : industrial ? 'M12 59V18h76v41Z'
    : outdoor ? 'M14 25 28 12h44l14 13ZM22 25v34m56-34v34'
    : 'M20 59V28L50 9l30 19v31';
  const frame = mobile ? 'M12 33h76M29 18v34m31-34v34'
    : industrial ? 'M18 23v31m7-31v31m50-31v31m7-31v31M35 59V28h30v31'
    : outdoor ? 'M26 25l11-13m6 13 7-13m11 13 2-13M14 25h72'
    : 'M20 28h60M35 19v40m30-40v40';
  return <svg viewBox="0 0 100 70" fill="none" aria-hidden="true">
    <g stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round">
      <path d={outline} strokeDasharray={stage === 0 ? '4 4' : undefined} />
      <path d="M8 64h84" opacity=".5" />
      {stage >= 1 && <path d={frame} />}
      {stage === 2 && <path d="M25 48h48V34H50v-8m-4 0h8m-25 22v5m-4 0h8" stroke="#dd9c70" />}
      {stage === 3 && <><path d="M25 50h50v5H25Z" fill="#C56A3D" /><path d="M29 55v4m42-4v4M42 45V34h16v11H42Z" /></>}
    </g>
  </svg>;
}

export function BuildSequence({ stages, illustrated = false, category }: { stages: BuildStage[]; illustrated?: boolean; category?: string }) {
  return <ol className={`build-sequence${illustrated ? ' illustrated' : ''}`}>
    {stages.map((stage, index) => <li key={stage.label}>
      <span className="build-step-number">0{index + 1}</span>
      {illustrated && <StageMark stage={index} category={category} />}
      <h3>{stage.label}</h3><p>{stage.description}</p>
    </li>)}
  </ol>;
}

export function ConceptPanel({ concept, category, featured = false }: { concept: InspirationConcept; category: InspirationCategory; featured?: boolean }) {
  return <details className={`idea-concept ${featured ? 'idea-featured' : 'idea-supporting'}`} name="inspiration-concept" id={concept.key}>
    <summary aria-label={`Explore ${concept.title}`}>
      <figure className="ideas-media"><InspirationVisual visual={concept.visual} category={category.name} /></figure>
      <div className="concept-preview">
        <p className="public-eyebrow">{featured ? 'Featured possibility' : 'Another direction'} / {concept.subtype}</p>
        <h3>{concept.title}</h3><p>{featured ? concept.narrative : concept.description}</p>
        {featured && <div className="concept-preview-goals"><p className="public-eyebrow">The design ambition</p><ul>{concept.conceptGoals.map(goal => <li key={goal}>{goal}</li>)}</ul></div>}
        <span className="concept-toggle"><span className="when-closed">Explore this direction <span aria-hidden="true">↗</span></span><span className="when-open">Close this direction <span aria-hidden="true">−</span></span></span>
      </div>
    </summary>
    <section className="concept-detail" aria-labelledby={`${concept.key}-title`}>
      <div className="concept-detail-intro"><p className="public-eyebrow">Inside the idea</p><h3 id={`${concept.key}-title`}>{concept.title}</h3><p>{concept.narrative}</p></div>
      <div className="concept-detail-columns">
        <div><h4>Design goals</h4><ul>{concept.conceptGoals.map(goal => <li key={goal}>{goal}</li>)}</ul></div>
        <div><h4>Design notes</h4>{concept.designNotes.map(note => <p key={note}>{note}</p>)}</div>
        <div><h4>Materials & systems</h4><p>{concept.materialDirection}</p><p>{concept.systemsConsiderations}</p></div>
      </div>
      <h4>Build direction</h4><BuildSequence stages={concept.buildStages} />
      <div className="concept-detail-bottom"><div><h4>Projects like this may involve</h4><p>{concept.possibleTrades.join(' · ')}</p></div>
        <Link className="public-button" to="/projects/new">{category.cta} ↗</Link>
      </div>
      <button className="concept-close" type="button" onClick={event => {
        const detail = event.currentTarget.closest('details');
        if (detail) { detail.open = false; detail.querySelector('summary')?.focus(); }
      }}>Close concept ↑</button>
    </section>
  </details>;
}
