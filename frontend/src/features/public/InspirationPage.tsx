import { useEffect, useRef } from 'react';
import { Link, useParams } from 'react-router';
import { Brand } from '../../components/Brand';
import { inspirationCategories } from './inspirationData';
import { InspirationVisual } from './InspirationVisual';
import { BuildSequence, ConceptPanel } from './ConceptPanel';
import { SeeItBuilt } from './SeeItBuilt';

export function InspirationPage() {
  const { slug } = useParams();
  const category = inspirationCategories.find(item => item.slug === slug);
  const builtConcept = category?.possibilities.find(concept => concept.seeItBuilt);
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    const previousTitle = document.title;
    document.title = `${category?.name ?? 'Idea not found'} | Rock & Hard Places`;
    window.scrollTo(0, 0);
    heading.current?.focus({ preventScroll: true });
    return () => { document.title = previousTitle; };
  }, [category]);

  return <div className="public-home ideas-page">
    <a className="skip-link" href="#ideas-main">Skip to content</a>
    <header className="public-header">
      <Link to="/" className="public-brand" aria-label="Rock & Hard Places home"><Brand /></Link>
      <nav aria-label="Public navigation"><a href="/#inspiration">All ideas</a><Link to="/opportunities">Find Work ↗</Link></nav>
      <Link className="public-button" to="/projects/new">Start a Project ↗</Link>
    </header>
    {!category ? <main id="ideas-main" className="ideas-not-found ideas-canvas">
      <p className="public-eyebrow">Keep exploring</p><h1 ref={heading} tabIndex={-1}>Idea not found.</h1>
      <p>There isn’t an inspiration page at this address. Your idea still has a place here.</p>
      <a className="public-button" href="/#inspiration">Explore all ideas ↗</a>
    </main> : <main id="ideas-main" key={category.slug}>
      <nav className="ideas-category-nav ideas-canvas" aria-label="Inspiration categories">
        {inspirationCategories.map(item => <Link key={item.slug} to={`/ideas/${item.slug}`} aria-current={item.slug === slug ? 'page' : undefined}>{item.name}</Link>)}
      </nav>
      <section className="ideas-hero ideas-canvas">
        <div className="ideas-intro"><a className="ideas-back" href="/#inspiration">← All ideas</a>
          <p className="public-eyebrow">Inspiration / {category.name}</p>
          <h1 ref={heading} tabIndex={-1}>{category.headline}</h1><p>{category.statement}</p>
          <a className="ideas-text-link" href="#possibilities">Find your direction ↓</a>
        </div>
        <figure className="ideas-hero-art"><InspirationVisual visual={category.hero} category={category.name} eager /></figure>
      </section>
      <section id="possibilities" className="ideas-possibilities ideas-canvas" aria-labelledby="explore-title">
        <div className="ideas-section-heading"><div><p className="public-eyebrow">01 / Explore</p><h2 id="explore-title">{category.exploreTitle}</h2></div><p>{category.exploreIntroduction}</p></div>
        <div className="ideas-concepts">{category.possibilities.map((concept, index) => <ConceptPanel key={concept.key} concept={concept} category={category} featured={index === 0} />)}</div>
      </section>
      <section className="ideas-story" aria-labelledby="story-title"><div className="ideas-canvas">
        <div className="ideas-section-heading"><div><p className="public-eyebrow">02 / Understand</p><h2 id="story-title">{category.storyTitle}</h2></div><p>A possible path for {category.possibilities[0].title.toLowerCase()}. Let each decision inform the next.</p></div>
        <BuildSequence stages={category.possibilities[0].buildStages} illustrated category={category.slug} />
      </div></section>
      <section className="ideas-planning ideas-canvas" aria-labelledby="planning-title">
        <div className="ideas-section-heading"><div><p className="public-eyebrow">03 / Plan</p><h2 id="planning-title">What it may take.</h2></div><p>The scope starts with your idea and the conditions you’re working with. These are useful conversations to begin.</p></div>
        <div className="ideas-planning-columns">
          <div><h3>The people</h3><p>Projects like these may involve…</p><ul>{category.trades.map(trade => <li key={trade}>{trade}</li>)}</ul></div>
          <div><h3>Behind the surfaces</h3><p>{category.systems}</p><p>Bring these needs into the early layout discussion.</p></div>
          <div><h3>The material direction</h3><p>{category.materials}</p><p>Choose details that suit how you’ll use and care for the space.</p></div>
        </div>
      </section>
      {builtConcept && <SeeItBuilt key={builtConcept.key} concept={builtConcept} />}
      <section className="public-closing ideas-closing"><div className="ideas-canvas"><p className="public-eyebrow">05 / Start</p><h2>Your idea defines the project.</h2><p>You don’t need every answer to begin. Bring the direction that excites you and start shaping the work.</p><Link className="public-button" to="/projects/new">{category.cta} ↗</Link></div></section>
      <nav className="ideas-category-nav ideas-canvas" aria-label="Continue exploring">
        {inspirationCategories[inspirationCategories.indexOf(category) - 1] && <Link to={`/ideas/${inspirationCategories[inspirationCategories.indexOf(category) - 1].slug}`}>← {inspirationCategories[inspirationCategories.indexOf(category) - 1].name}</Link>}
        <a href="/#inspiration">Explore all ideas</a>
        {inspirationCategories[inspirationCategories.indexOf(category) + 1] && <Link to={`/ideas/${inspirationCategories[inspirationCategories.indexOf(category) + 1].slug}`}>{inspirationCategories[inspirationCategories.indexOf(category) + 1].name} →</Link>}
      </nav>
    </main>}
    <footer className="public-footer"><Link to="/" className="public-brand" aria-label="Rock & Hard Places home"><Brand /></Link><p>People. Projects. Possibilities.</p><a href="/#inspiration">Back to all ideas ↑</a></footer>
  </div>;
}
