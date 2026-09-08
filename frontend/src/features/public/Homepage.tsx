import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { Brand } from "../../components/Brand";
import { BuildSketch } from "./BuildSketch";

const inspiration = [
  { slug: "homes", title: "Homes", detail: "Restore. Renovate. Reimagine." },
  { slug: "barns", title: "Barns", detail: "Old character. New purpose." },
  { slug: "rvs", title: "RVs", detail: "Make room for the open road." },
  { slug: "buses", title: "Buses / Skoolies", detail: "A different way to feel at home." },
  { slug: "tiny-homes", title: "Tiny Homes", detail: "Small footprint. Big possibility." },
  { slug: "containers", title: "Containers", detail: "Think outside the original box." },
  { slug: "outdoor-spaces", title: "Outdoor Spaces", detail: "Build a little closer to nature." },
  { slug: "beyond", title: "And Beyond", detail: "Your idea defines the project." },
];

function BarnComparison() {
  const [selection, setSelection] = useState({ state: "after", manual: false });
  const [hovered, setHovered] = useState(false);
  const [controlsFocused, setControlsFocused] = useState(false);
  const [reducedMotion, setReducedMotion] = useState(true);
  const touchStart = useRef<{ x: number; y: number } | null>(null);
  const navigate = () => setSelection(current => ({
    state: current.state === "after" ? "before" : "after", manual: true,
  }));

  useEffect(() => {
    const preference = window.matchMedia("(prefers-reduced-motion: reduce)");
    const update = () => setReducedMotion(preference.matches);
    update();
    preference.addEventListener("change", update);
    return () => preference.removeEventListener("change", update);
  }, []);

  useEffect(() => {
    if (hovered || controlsFocused || reducedMotion) return;
    const timer = window.setTimeout(() => {
      setSelection(current => ({ state: current.state === "after" ? "before" : "after", manual: false }));
    }, 7000);
    return () => window.clearTimeout(timer);
  }, [selection, hovered, controlsFocused, reducedMotion]);

  return <div className="hero-visual" data-immediate={selection.manual}
    role="region" aria-roledescription="carousel" aria-label="Barn restoration inspiration" tabIndex={0}
    onPointerEnter={event => { if (event.pointerType !== "touch") setHovered(true); }}
    onPointerLeave={() => setHovered(false)}
    onFocus={() => setControlsFocused(true)}
    onBlur={event => {
      if (!event.currentTarget.contains(event.relatedTarget)) setControlsFocused(false);
    }}
    onKeyDown={event => {
      if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
        event.preventDefault();
        navigate();
      }
    }}
    onTouchStart={event => {
      const touch = event.touches[0];
      touchStart.current = event.touches.length === 1 ? { x: touch.clientX, y: touch.clientY } : null;
    }}
    onTouchCancel={() => { touchStart.current = null; }}
    onTouchEnd={event => {
      const start = touchStart.current;
      touchStart.current = null;
      const touch = event.changedTouches[0];
      if (!start || !touch) return;
      const distance = touch.clientX - start.x;
      if (Math.abs(distance) >= 40 && Math.abs(distance) > Math.abs(touch.clientY - start.y)) navigate();
    }}>
    <img className="barn-comparison-image" data-visible={selection.state === "before"}
      src="/images/barn-before.jpg" alt="Barn before restoration, with worn siding and an unfinished entrance"
      aria-hidden={selection.state !== "before"} />
    <img className="barn-comparison-image" data-visible={selection.state === "after"}
      src="/images/barn-after.jpg" alt="Timber workshop with an open entrance, crafted wood siding and a standing-seam roof"
      aria-hidden={selection.state !== "after"} fetchPriority="high" />
    <span className="barn-state-label">{selection.state === "after" ? "AFTER" : "BEFORE"}</span>
    <div className="barn-comparison-controls" role="group" aria-label="Barn image navigation">
      <button className="barn-previous" type="button" aria-label="Previous image" onClick={navigate}>
        <span aria-hidden="true">‹</span>
      </button>
      <button className="barn-next" type="button" aria-label="Next image" onClick={navigate}>
        <span aria-hidden="true">›</span>
      </button>
    </div>
    <div className="hero-caption"><span>From ideas</span><em>to reality.</em><small>Restore something old. Build something you.</small></div>
  </div>;
}

export function Homepage() {
  useEffect(() => {
    if (window.location.hash === "#inspiration") {
      document.getElementById("inspiration")?.scrollIntoView();
    }
  }, []);

  return <div className="public-home">
    <a className="skip-link" href="#public-main">Skip to content</a>
    <header className="public-header">
      <Link to="/" className="public-brand" aria-label="Rock & Hard Places home"><Brand /></Link>
      <nav aria-label="Public navigation"><a href="#inspiration">Inspiration</a><a href="#homeowners">For Homeowners</a><a href="#tradespeople">For Tradespeople</a><Link to="/overview">Enter App ↗</Link></nav>
      <Link className="public-button" to="/projects/new">Start a Project <span aria-hidden="true">↗</span></Link>
    </header>
    <main id="public-main">
      <section className="public-hero">
        <div className="hero-story"><p className="public-eyebrow">People. Projects. Possibilities.</p><h1>Any Project.<br />Real People.<br /><em>A Brighter<br className="hero-break" /> Tomorrow.</em></h1><p className="hero-intro">You bring the idea. We help you find the people and organize the work to make it real.</p><div className="public-actions"><Link className="public-button" to="/projects/new">Start a Project ↗</Link><Link className="public-button secondary" to="/opportunities">Find Work ↗</Link></div><p className="hero-trust">Built on trust. Backed by skill.</p></div>
        <BarnComparison />
      </section>
      <div className="public-principles"><p><strong>01 / Plan the build</strong><span>Turn the idea into clear work.</span></p><p><strong>02 / Build your team</strong><span>Find the right people for each part.</span></p><p><strong>03 / See it take shape</strong><span>Follow the work from first task to final review.</span></p></div>
      <section className="inspiration-section public-section" id="inspiration"><div className="public-section-heading"><div><p className="public-eyebrow">Same foundation. A wider world.</p><h2>What do you have in mind?</h2></div><p>A place to live. A space to create. Something no one’s built before. <strong>Your idea defines the project.</strong></p></div><div className="inspiration-grid">{inspiration.map(item => <Link key={item.title} className="inspiration-card" to={`/ideas/${item.slug}`} aria-label={`Explore ideas: ${item.title}`}>
        <BuildSketch kind={item.title} /><div><h3>{item.title}<span aria-hidden="true">↗</span></h3><p>{item.detail}</p></div>
      </Link>)}</div></section>
      <section className="public-process public-section"><p className="public-eyebrow">A little structure. A lot of possibility.</p><h2>From “what if” to well built.</h2><ol>{[["Vision", "Bring the idea."], ["Plan", "Break it into manageable work."], ["People", "Find the right tradespeople."], ["Work", "Coordinate tasks, bids, teams and progress."], ["Completion", "Review the work and finish strong."]].map(([title, copy], i) => <li key={title}><span className="process-number">0{i + 1}<span aria-hidden="true"> →</span></span><h3>{title}</h3><p>{copy}</p></li>)}</ol></section>
      <section className="public-roles public-section" aria-label="A place for both sides of the build"><article id="homeowners"><p className="public-eyebrow">For Homeowners</p><h2>Your vision.<br />The right people.</h2><p>Turn your idea into a project with a clear scope. Find tradespeople, compare bids and build a team you can work with.</p><p>Keep the tasks and progress together, from the first plan to the finishing touches.</p><Link className="public-button" to="/projects/new">Start a Project ↗</Link></article><article id="tradespeople"><p className="public-eyebrow">For Tradespeople</p><h2>Your craft.<br />New possibilities.</h2><p>Find worthwhile opportunities with scope you can understand. Submit bids, join project teams and manage your assigned work.</p><p>Let your work speak for itself. Build your reputation through your portfolio and reviews.</p><Link className="public-button" to="/opportunities">Find Work ↗</Link></article></section>
      <section className="public-trust public-section"><div className="craft-photo"><img src="/images/construction-detail.png" alt="Carpenter’s tools, timber and plans on a workbench" loading="lazy" /></div><div><p className="public-eyebrow">Good work starts with common ground.</p><h2>Built on trust.<br />Backed by skill.</h2><p>Know the scope. Know the people. See how the work is coming together.</p><ul><li>Clear project scope & transparent bids</li><li>People-centered teams & visible qualifications</li><li>Task progress & reviews</li><li>Portfolios with context behind the work</li></ul></div></section>
      <section className="public-closing"><p className="public-eyebrow">Build. Restore. Convert. Create. Together.</p><h2>There’s more than one<br />way to build a brighter tomorrow.</h2><p>From a careful restoration to a home on wheels, if you can imagine the build, RH&amp;P helps you organize the people and work.</p><Link className="public-button" to="/projects/new">Bring your idea ↗</Link></section>
    </main><footer className="public-footer"><Link to="/" className="public-brand" aria-label="Rock & Hard Places home"><Brand /></Link><p>People. Projects. Possibilities.</p><Link to="/overview">Enter your workspace ↗</Link></footer>
  </div>;
}
