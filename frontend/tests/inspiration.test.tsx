import test from 'node:test';
import assert from 'node:assert/strict';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router';
import { App } from '../src/app/App';
import { inspirationCategories, buildStageLabels } from '../src/features/public/inspirationData';
import { ConceptPanel } from '../src/features/public/ConceptPanel';

const categories = [
  ['homes', 'Homes', 'Homes, reimagined.', 'Start a Home Project'],
  ['barns', 'Barns', 'Old bones. New possibilities.', 'Start a Barn Project'],
  ['rvs', 'RVs', 'Make the road feel like home.', 'Start an RV Project'],
  ['buses', 'Buses / Skoolies', 'Build the journey.', 'Start a Bus Conversion'],
  ['tiny-homes', 'Tiny Homes', 'Small footprint. Big possibility.', 'Start a Tiny Home Project'],
  ['containers', 'Containers', 'Structure, transformed.', 'Start a Container Project'],
  ['outdoor-spaces', 'Outdoor Spaces', 'Build beyond the walls.', 'Start an Outdoor Project'],
  ['beyond', 'And Beyond', 'If you can imagine it, start here.', 'Start Something Different'],
];
// Deliberately no QueryClientProvider or account/project fixtures: public browsing must be independent.
const publicRoute = (path: string) => renderToStaticMarkup(<MemoryRouter initialEntries={[path]}><App /></MemoryRouter>);

test('homepage keeps its hero and all eight inspiration cards open their matching public gallery', () => {
  const html = publicRoute('/');
  assert.match(html, /Any Project\./);
  assert.match(html, /Barn restoration inspiration/);
  const links = [...html.matchAll(/<a\b[^>]*class="inspiration-card"[^>]*>/g)].map(match => match[0]);
  assert.equal(links.length, 8);
  categories.forEach(([slug, name], index) => {
    assert.ok(links[index].includes(`href="/ideas/${slug}"`));
    assert.ok(links[index].includes(`aria-label="Explore ideas: ${name}"`));
  });
});

for (const [slug, , headline, cta] of categories) {
  test(`/ideas/${slug} renders publicly with its gallery, story, trades and project CTA`, () => {
    const html = publicRoute(`/ideas/${slug}`);
    assert.ok(html.includes(headline));
    assert.equal((html.match(/<details\b/g) ?? []).length, 3);
    assert.match(html, /aria-labelledby="story-title"/);
    assert.match(html, /Projects like these may involve/);
    const links = [...html.matchAll(/<a\b[^>]*href="\/projects\/new"[^>]*>(.*?)<\/a>/g)];
    assert.ok(links.some(match => match[1] === `${cta} ↗`));
    assert.match(html, /href="\/#inspiration"/);
    assert.match(html, /href="\/opportunities"/);
    assert.doesNotMatch(html, /\/seed-media\/|RH&amp;P verified|data-role=|Loading your workspace/);
  });
}

test('invalid inspiration paths show a public not-found page with a return path', () => {
  for (const path of ['/ideas/unknown', '/ideas/homes/extra', '/ideas']) {
    const html = publicRoute(path);
    assert.match(html, /Idea not found\./);
    assert.match(html, /href="\/#inspiration"/);
    assert.doesNotMatch(html, /ideas-concepts/);
  }
});


test('all 24 concepts contain complete editorial direction without business provenance', () => {
  assert.equal(inspirationCategories.length, 8);
  const keys = new Set<string>();
  for (const category of inspirationCategories) {
    assert.equal(category.possibilities.length, 3);
    const visuals = category.possibilities.map(concept => JSON.stringify(concept.visual));
    assert.equal(new Set(visuals).size, 3, `${category.name} concepts need distinct artwork`);
    assert.ok(!visuals.includes(JSON.stringify(category.hero)), 'hero artwork must not repeat as a concept');
    for (const concept of category.possibilities) {
      assert.ok(!keys.has(concept.key));
      keys.add(concept.key);
      assert.equal(concept.category, category.slug);
      for (const value of [concept.title, concept.description, concept.narrative, concept.materialDirection, concept.systemsConsiderations]) assert.ok(value.length > 10);
      assert.ok(concept.conceptGoals.length >= 3);
      assert.ok(concept.possibleTrades.length > 0);
      assert.ok(concept.designNotes.length >= 2);
      assert.deepEqual(concept.buildStages.map(stage => stage.label), [...buildStageLabels]);
      assert.ok(concept.buildStages.every(stage => stage.description.length > 20));
      assert.deepEqual(Object.keys(concept).filter(key => key !== "seeItBuilt").sort(), ['key', 'category', 'title', 'subtype', 'description', 'narrative', 'conceptGoals', 'possibleTrades', 'buildStages', 'designNotes', 'materialDirection', 'systemsConsiderations', 'visual'].sort());
      assert.doesNotMatch(JSON.stringify(concept), /seed-media|verified|completed project|completion date/i);
    }
  }
  assert.equal(keys.size, 24);
});

test('every concept uses a native keyboard-operable disclosure with a complete detail and project CTA', () => {
  for (const category of inspirationCategories) for (const concept of category.possibilities) {
    const html = renderToStaticMarkup(<MemoryRouter><ConceptPanel concept={concept} category={category} /></MemoryRouter>);
    assert.match(html, /<details[^>]*name="inspiration-concept"/);
    assert.match(html, /<summary aria-label="Explore /);
    assert.doesNotMatch(html, /<details[^>]*\bopen(?:=|>)/);
    assert.ok(html.includes(`aria-labelledby="${concept.key}-title"`));
    for (const label of ['Design goals', 'Build direction', 'Projects like this may involve', 'Design notes']) assert.ok(html.includes(label));
    assert.ok(html.includes(`${category.cta} ↗</a>`));
    assert.match(html, /href="\/projects\/new"/);
  }
});

test('category rhythm ends with the project CTA and avoids repeated gallery labels', () => {
  for (const category of inspirationCategories) {
    const html = publicRoute(`/ideas/${category.slug}`);
    assert.match(html, /build-sequence illustrated/);
    assert.match(html, /What it may take/);
    assert.doesNotMatch(html, /Concept sketch|Public inspiration and concept studies|See what it could become|ideas-more/);
    assert.ok(html.indexOf('ideas-concepts') < html.indexOf('ideas-story'));
    assert.ok(html.indexOf('ideas-story') < html.indexOf('ideas-planning'));
    assert.ok(html.indexOf('ideas-planning') < html.indexOf('ideas-closing'));
  }
});

for (const [slug] of categories) {
  test(`${slug} has all category links, an accessible current category, and one final conversion CTA`, () => {
    const html = publicRoute(`/ideas/${slug}`);
    const nav = html.match(/<nav[^>]*aria-label="Inspiration categories"[^>]*>([\s\S]*?)<\/nav>/)?.[1] || '';
    for (const [other] of categories) assert.ok(nav.includes(`href="/ideas/${other}"`));
    assert.equal((nav.match(/aria-current="page"/g) || []).length, 1);
    assert.match(nav, new RegExp(`<a[^>]*aria-current="page"[^>]*href="/ideas/${slug}"`));
    assert.doesNotMatch(html, /Inspired by this direction|built-cta/);
    assert.match(html, /05 \/ Start/);
    assert.equal((html.match(/<h2>Your idea defines the project\.<\/h2>/g) || []).length, 1);
    assert.match(html, /aria-label="Continue exploring"/);
  });
}
