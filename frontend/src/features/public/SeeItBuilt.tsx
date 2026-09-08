import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../../api/client';
import type { Person } from '../../api/types';
import { Portrait } from '../../components/ui';
import type { InspirationConcept, SeeItBuiltStory } from './inspirationData';
import { selectCollaborators } from './seeItBuiltPeople';

export function CollaboratorCards({ people, story, direction }: { people: readonly Person[]; story: SeeItBuiltStory; direction: string }) {
  const collaborators = selectCollaborators(people, story.teamSuggestions, direction);
  return collaborators.length ? <div className="built-collaborators">{collaborators.map(({ person, suggestion }) =>
    <article className="built-person" key={person.profile.id}>
      <Portrait name={person.profile.displayName} reference={person.profile.profileImageReference} />
      <div><h4>{person.profile.displayName}</h4><p className="built-person-trade">{suggestion.trade}</p>
        <p>{suggestion.contribution}</p><Link to={`/people/${person.profile.id}`} aria-label={`View profile: ${person.profile.displayName}`}>View profile <span aria-hidden="true">→</span></Link>
      </div>
    </article>)}</div> : <p className="built-people-status">There aren’t matching people available to suggest right now. You can still start shaping your project.</p>;
}

function SuggestedPeople({ story, direction }: { story: SeeItBuiltStory; direction: string }) {
  const [people, setPeople] = useState<Person[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setPeople(null); setFailed(false);
    api.people({}, controller.signal).then(result => {
      if (!controller.signal.aborted) setPeople(result);
    }).catch(() => { if (!controller.signal.aborted) setFailed(true); });
    return () => controller.abort();
  }, [attempt]);
  // Discovery can be unavailable under the current account; inspiration itself never depends on it.
  return <div className="built-team">
    <h3>A build like this may bring together…</h3>
    <p>People with these skills could contribute. Explore their profiles to learn about their own work and specialties.</p>
    {people ? <CollaboratorCards people={people} story={story} direction={direction} />
      : failed ? <div className="built-people-status"><p>People suggestions aren’t available right now. Keep exploring this direction or try again.</p><button type="button" onClick={() => setAttempt(value => value + 1)}>Try people again</button></div>
      : <p className="built-people-status" role="status">Finding people with these skills…</p>}
  </div>;
}

export function SeeItBuilt({ concept }: { concept: InspirationConcept }) {
  const story = concept.seeItBuilt;
  const [selected, setSelected] = useState(2);
  const [imageFailed, setImageFailed] = useState(false);
  if (!story) return null;
  const stage = story.visualStages[selected];
  const visual = story.visuals[stage.visualIndex];
  return <section className="see-it-built" aria-labelledby="built-title"><div className="ideas-canvas">
    <div className="ideas-section-heading"><div><p className="public-eyebrow">04 / See it built</p><h2 id="built-title">See the idea take shape.</h2></div><p>The work takes shape when materials, systems and people start coming together.</p></div>
    <div className="built-provenance"><p>Inspired by <a href={`#${concept.key}`}>{concept.title}</a>.</p></div>
    <figure className="built-visual">
      <div className="built-stage-window" style={{ aspectRatio: `${visual.width} / ${visual.frameHeight}` }}>
        {!imageFailed ? <img src={visual.src} alt={stage.alt} loading="lazy" style={{ height: `${visual.height / visual.frameHeight * 100}%`, transform: `translateY(-${visual.frameOffsets[stage.frame] / visual.height * 100}%)` }} onError={() => setImageFailed(true)} />
          : <p className="built-media-unavailable">This visualization isn’t available right now.</p>}
      </div>
      <figcaption id="built-caption" aria-live="polite"><strong>{stage.label}</strong><span>{stage.caption}</span></figcaption>
    </figure>
    <div className="built-stage-controls" role="group" aria-label="Visualization stages">{story.visualStages.map((item, index) =>
      <button key={item.label} type="button" aria-pressed={selected === index} aria-describedby="built-caption" onClick={() => setSelected(index)}>{item.label}</button>)}</div>
    <div className="built-direction"><div><p className="public-eyebrow">Featured build direction</p><h3>{story.title}</h3><p>{story.summary}</p></div>
      <div><h3>Work that may be involved</h3><ul>{story.workAreas.map(area => <li key={area.trade}><strong>{area.trade}</strong><span>{area.description}</span></li>)}</ul></div>
    </div>
    <SuggestedPeople story={story} direction={concept.key} />
    <div className="built-cta"><p>Inspired by this direction?</p><Link className="public-button" to="/projects/new">{story.ctaLabel} ↗</Link></div>
  </div></section>;
}
