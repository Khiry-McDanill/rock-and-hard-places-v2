import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router';
import { App } from '../src/app/App';
import type { Person } from '../src/api/types';
import { inspirationCategories } from '../src/features/public/inspirationData';
import { selectCollaborators } from '../src/features/public/seeItBuiltPeople';
import { CollaboratorCards } from '../src/features/public/SeeItBuilt';

const person = (id: number, name: string, trade: string, specialty = ''): Person => ({
  profile: { id, displayName: name, role: 'TRADESPERSON', accountStatus: 'ACTIVE', availabilityStatus: 'AVAILABLE_SOON', profileImageReference: null, verificationStatus: null, baseZip: null, serviceRadius: null },
  qualifications: [{ id: 1, name: trade, specialties: [] }], specialties: specialty ? [{ id: 1, name: specialty }] : [],
});

for (const category of inspirationCategories) {
  test(`${category.name} has one concept-linked realistic story in the approved section order`, () => {
    const featured = category.possibilities.filter(concept => concept.seeItBuilt);
    assert.equal(featured.length, 1);
    const story = featured[0].seeItBuilt!;
    assert.equal(story.visuals.length, 1);
    assert.deepEqual(story.visualStages.map(stage => stage.label), ['Idea', 'Build', 'Result']);
    assert.deepEqual(story.visualStages.map(stage => stage.frame), [0, 1, 2]);
    assert.ok(story.visualStages.every(stage => stage.visualIndex === 0 && stage.alt.includes(featured[0].title)));
    for (const stage of story.visualStages) {
      assert.doesNotMatch(`${stage.label} ${stage.caption} ${stage.alt}`, /Generated|AI-generated|Imagined realization|Concept image|Illustrative|Placeholder/i);
    }
    assert.ok(story.teamSuggestions.length >= 3);
    assert.equal(story.ctaLabel, category.cta);
    assert.doesNotMatch(JSON.stringify(story), /built by|completed by|RH&P verified|project owner|completed project|actual RH&P project|\/seed-media\//i);
    // No query provider, account or project fixtures: the page remains public.
    const html = renderToStaticMarkup(<MemoryRouter initialEntries={[`/ideas/${category.slug}`]}><App /></MemoryRouter>);
    assert.match(html, /04 \/ See it built/);
    const builtSection = html.slice(html.indexOf('<section class="see-it-built"'), html.indexOf('</section>', html.indexOf('<section class="see-it-built"')));
    assert.ok(builtSection.includes(`Inspired by <a href="#${featured[0].key}">${featured[0].title}</a>.`));
    assert.doesNotMatch(builtSection, /Generated|AI-generated|Imagined realization|Concept image|Illustrative|Placeholder/i);
    assert.match(html, /05 \/ Start/);
    assert.ok(html.indexOf('ideas-planning') < html.indexOf('see-it-built'));
    assert.ok(html.indexOf('see-it-built') < html.indexOf('ideas-closing'));
    assert.match(html, /role="group" aria-label="Visualization stages"/);
    assert.equal((html.match(/aria-pressed="true"/g) ?? []).length, 1);
    assert.match(html, /href="\/projects\/new"/);
  });
}

test('realistic story assets exist as three-stage PNG sheets with valid crop bounds', () => {
  for (const category of inspirationCategories) {
    const story = category.possibilities.find(concept => concept.seeItBuilt)!.seeItBuilt!;
    const file = readFileSync(resolve('public', story.visuals[0].src.slice(1)));
    assert.equal(file.subarray(1, 4).toString(), 'PNG');
    const width = file.readUInt32BE(16), height = file.readUInt32BE(20);
    const visual = story.visuals[0];
    assert.equal(width, visual.width);
    assert.equal(height, visual.height);
    assert.equal(visual.frameOffsets.length, 3);
    for (const top of visual.frameOffsets) assert.ok(top >= 0 && top + visual.frameHeight <= height);
  }
});

test('collaborators come only from eligible supplied discovery records with actual trade fit', () => {
  const story = inspirationCategories[1].possibilities[0].seeItBuilt!;
  const carpenter = person(991, 'Available carpenter', 'Carpentry', 'Decks and structural framing');
  const electrician = person(992, 'Available electrician', 'Electrical');
  const unrelated = person(993, 'Different trade', 'Plumbing');
  const inactive = person(994, 'Inactive person', 'Exterior Restoration'); inactive.profile.accountStatus = 'SUSPENDED';
  const busy = person(995, 'Busy person', 'Exterior Restoration'); busy.profile.availabilityStatus = 'BUSY';
  const wrongRole = person(996, 'Homeowner', 'Exterior Restoration'); wrongRole.profile.role = 'HOMEOWNER';
  const records = [carpenter, electrician, unrelated, inactive, busy, wrongRole];
  const selected = selectCollaborators(records, story.teamSuggestions, 'barns-1');
  assert.deepEqual(selected.map(item => item.person), [carpenter, electrician]);
  assert.ok(selected.every(item => records.includes(item.person)));
  assert.deepEqual(selectCollaborators([], story.teamSuggestions, 'barns-1'), []);
  assert.deepEqual(selectCollaborators([...records].reverse(), story.teamSuggestions, 'barns-1'), selected);
  const html = renderToStaticMarkup(<MemoryRouter><CollaboratorCards people={records} story={story} direction="barns-1" /></MemoryRouter>);
  assert.match(html, /href="\/people\/991"/);
  assert.match(html, /href="\/people\/992"/);
  assert.doesNotMatch(html, /people\/99[3-6]|Built by|RH&amp;P verified/);
});

test('specialty fit uses the person’s specialties, not the trade catalog’s available specialties', () => {
  const finish = person(991, 'Finish carpenter', 'Carpentry', 'Finish carpentry');
  const framing = person(992, 'Framing carpenter', 'Carpentry', 'Decks and structural framing');
  framing.qualifications[0].specialties = [{ id: 1, name: 'Finish carpentry' }];
  const need = [{ trade: 'Carpentry', contribution: 'Cabinet fitting.', specialtyHints: ['Finish carpentry'] }];
  assert.equal(selectCollaborators([framing, finish], need, 'rvs-2')[0].person, finish);
  assert.equal(selectCollaborators([finish], [...need, ...need], 'rvs-2').length, 1);
});
