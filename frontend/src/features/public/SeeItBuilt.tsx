import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import type { Person } from '../../api/types';
import { Portrait } from '../../components/ui';
import type { InspirationConcept, SeeItBuiltStory } from './inspirationData';
import { selectCollaborators, loadSuggestedPeople, type PeopleSuggestionState } from './seeItBuiltPeople';

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

export function PeopleSuggestions({ state, story, direction, retry }: { state: PeopleSuggestionState; story: SeeItBuiltStory; direction: string; retry: () => void }) {
  if (state.status === 'ready') return <CollaboratorCards people={state.people} story={story} direction={direction} />;
  if (state.status === 'loading') return <p className="built-people-status" role="status">Finding people with these skills…</p>;
  if (state.status === 'homeowner-required') return <div className="built-people-status"><p>Switch to your Homeowner profile to explore matching tradespeople. You can keep exploring ideas with either profile.</p><Link to="/overview">Open your workspace →</Link></div>;
  if (state.status === 'sign-in-required') return <div className="built-people-status"><p>Sign in with a Homeowner profile to explore matching tradespeople. These ideas are open for everyone to explore.</p><Link to="/overview">Open your workspace →</Link></div>;
  if (state.status === 'restricted') return <p className="built-people-status">People suggestions aren’t available for your current profile. You can still explore this build direction.</p>;
  return <div className="built-people-status" role="alert"><p>People suggestions couldn’t be loaded. Keep exploring this direction or try again.</p><button type="button" onClick={retry}>Try people again</button></div>;
}

function SuggestedPeople({ story, direction }: { story: SeeItBuiltStory; direction: string }) {
  const [state, setState] = useState<PeopleSuggestionState>({ status: 'loading' });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setState({ status: 'loading' });
    loadSuggestedPeople(controller.signal).then(result => {
      if (!controller.signal.aborted) setState(result);
    }).catch(() => { if (!controller.signal.aborted) setState({ status: 'failed' }); });
    return () => controller.abort();
  }, [attempt]);
  return <div className="built-team">
    <h3>A build like this may bring together…</h3>
    <p>People with these skills could contribute. Explore their profiles to learn about their own work and specialties.</p>
    <PeopleSuggestions state={state} story={story} direction={direction} retry={() => setAttempt(value => value + 1)} />
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
  </div></section>;
}
