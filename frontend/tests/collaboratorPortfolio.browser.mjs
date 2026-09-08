/** Live integration audit: the actual selector, backend portfolio and browser gallery. */
import assert from 'node:assert/strict';
import { build } from 'esbuild';
import { mkdir, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
const output = join(tmpdir(), 'rhp-029-portfolio-review');
await mkdir(output, { recursive: true });
const bundle = join(output, 'selection.mjs');
await build({ stdin: { contents: `export { inspirationCategories } from './src/features/public/inspirationData'; export { selectCollaborators } from './src/features/public/seeItBuiltPeople'; export { resolvePortfolioMedia } from './src/components/seedMedia';`, resolveDir: process.cwd() }, bundle: true, platform: 'node', format: 'esm', outfile: bundle });
const { inspirationCategories, selectCollaborators, resolvePortfolioMedia } = await import(bundle);
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright');
const browser = await chromium.launch({ headless: true });
const base = process.env.INSPIRATION_BASE_URL || 'http://localhost:5173';
const relevant = {
  Carpentry: /cabinet|timber|storage|shelving|wood|framing/i,
  Electrical: /lighting|circuit|electrical|outlet/i,
  Plumbing: /sink|supply|drain|shower|plumbing/i,
  Drywall: /plaster|drywall|wall finish/i,
  Flooring: /floor|oak|boards/i,
  'Exterior Restoration': /weatherproof|masonry|brick|exterior|water repairs/i,
};
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1100 } });
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  const get = async path => { const r = await page.request.get(base + path); assert.ok(r.ok(), path); return r.json(); };
  const people = await get('/api/discovery/tradespeople');
  const selected = new Map(), matrix = [];
  const stories = inspirationCategories.flatMap(c => c.possibilities.filter(p => p.seeItBuilt));
  for (const concept of stories) {
    await page.goto(`${base}/ideas/${concept.category}`, { waitUntil: 'networkidle' });
    for (const {person, suggestion} of selectCollaborators(people, concept.seeItBuilt.teamSuggestions, concept.key)) {
      const id = person.profile.id;
      const link = page.getByRole('link', {name: `View profile: ${person.profile.displayName}`, exact: true});
      assert.equal(await link.getAttribute('href'), `/people/${id}`);
      const items = await get(`/api/tradespeople/${id}/portfolio`);
      assert.ok(items.some(i => relevant[suggestion.trade].test(`${i.title} ${i.description}`)), `${person.profile.displayName}: ${suggestion.contribution}`);
      matrix.push({ story: concept.category, person: person.profile.displayName, contribution: suggestion.contribution, portfolio: items.map(i=>i.title) });
      selected.set(id, {person, items});
    }
  }
  for (const [id, {person, items}] of selected) {
    await page.goto(`${base}/people/${id}`, {waitUntil:'networkidle'});
    await page.getByRole('heading', {name:person.profile.displayName, exact:true, level:1}).waitFor();
    assert.match(await page.locator('body').innerText(), /Qualified trades[\s\S]*Specialties[\s\S]*Availability/i);
    for (const item of items) {
      assert.ok(!stories.some(c => [c.title, c.seeItBuilt.title].includes(item.title)), 'No inspiration attribution');
      if (item.mediaReference?.includes('/rhp-029/')) {
        assert.equal(item.provenance,'SELF_REPORTED'); assert.equal(item.projectId,null); assert.equal(item.taskId,null);
      }
      if (item.provenance === 'RHP_VERIFIED') { assert.ok(item.projectId); assert.ok(item.taskId); assert.ok(item.approvedAttachmentIds.length); }
      const media = resolvePortfolioMedia(person.profile.displayName,item);
      assert.ok(media.frames.length, `${item.title}: imagery required`);
      for(const frame of media.frames) {
        assert.doesNotMatch(frame.url,/construction-detail|see-it-built/);
        const response = await page.request.get(base+frame.url); assert.ok(response.ok()); assert.match(response.headers()['content-type'],/^image\//);
      }
      await page.getByRole('button',{name:`View portfolio: ${item.title}`,exact:true}).click();
      const dialog=page.getByRole('dialog'); await dialog.waitFor();
      assert.match(await dialog.innerText(), item.provenance==='SELF_REPORTED' ? /Self-reported/ : item.provenance==='RHP_VERIFIED' ? /RH&P verified/ : /Externally verified/);
      await page.waitForFunction(()=>[...document.querySelectorAll('dialog img')].every(i=>i.complete&&i.naturalWidth>0));
      await dialog.screenshot({path:join(output,`${id}-${item.id}-detail.png`)});
      await page.getByRole('button',{name:'Close portfolio detail'}).click();
    }
    await page.waitForFunction(()=>[...document.images].every(i=>i.complete&&i.naturalWidth>0));
    await page.screenshot({path:join(output,`${id}-profile.png`),fullPage:true});
    console.log(`${person.profile.displayName}: relevant evidence, provenance, profile, galleries and images passed`);
  }
  const [failureId, failureProfile] = [...selected].find(([,value])=>value.items.some(i=>i.mediaReference));
  const failureItem = failureProfile.items.find(i=>i.mediaReference);
  await page.route(`**${failureItem.mediaReference}`, route=>route.abort());
  await page.goto(`${base}/people/${failureId}`, {waitUntil:'networkidle'});
  const failedCard = page.getByRole('button',{name:`View portfolio: ${failureItem.title}`,exact:true});
  await failedCard.getByText('No work photos yet').waitFor();
  assert.equal(await failedCard.locator('img').count(),0, 'Broken work image must not acquire unrelated fallback');
  assert.deepEqual(errors,[]);
  await writeFile(join(output,'matrix.json'),JSON.stringify(matrix,null,2));
  console.log(`All ${stories.length} stories audited. Screenshots: ${output}`);
} finally { await browser.close(); await rm(bundle,{force:true}); }
