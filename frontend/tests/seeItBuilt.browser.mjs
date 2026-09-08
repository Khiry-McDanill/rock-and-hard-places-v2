/** Requires a running frontend/backend and Playwright; uses only read-only live profile requests.
 * PLAYWRIGHT_MODULE can point to an existing Playwright installation.
 */
import assert from 'node:assert/strict';
import { mkdir } from 'node:fs/promises';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright');
const baseURL = process.env.INSPIRATION_BASE_URL || 'http://127.0.0.1:5173';
const output = join(tmpdir(), 'rhp-029-review');
await mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true });
try {
  const cases = [['barns',1440,1000], ['rvs',1440,1000], ['beyond',1440,1000], ['rvs',390,844]];
  const selectedCases = process.env.SEE_IT_BUILT_CASES ? JSON.parse(process.env.SEE_IT_BUILT_CASES) : cases;
  for (const [slug, width, height] of selectedCases) {
    const page = await browser.newPage({ viewport: { width, height }, reducedMotion: 'reduce' });
    const errors = [], calls = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('request', request => { const path = new URL(request.url()).pathname; if (path.startsWith('/api/')) calls.push(path); });
    await page.goto(`${baseURL}/ideas/${slug}`, { waitUntil: 'networkidle' });
    const section = page.locator('.see-it-built');
    await section.scrollIntoViewIfNeeded();
    await page.waitForFunction(() => [...document.querySelectorAll('.see-it-built img')].every(image => image.complete && image.naturalWidth > 0));
    assert.equal(await page.locator('.built-person').count(), 3);
    const response = await page.request.get(`${baseURL}/api/discovery/tradespeople`);
    assert.ok(response.ok());
    const people = await response.json();
    for (const card of await page.locator('.built-person').all()) {
      const id = Number((await card.locator('a').getAttribute('href')).split('/').at(-1));
      const actual = people.find(person => person.profile.id === id);
      assert.ok(actual, 'profile ID must come from live people');
      assert.equal(await card.locator('h4').innerText(), actual.profile.displayName);
      const trade = await card.locator('.built-person-trade').innerText();
      assert.ok(actual.qualifications.some(qualification => qualification.name === trade));
      assert.ok(['AVAILABLE_NOW', 'AVAILABLE_SOON'].includes(actual.profile.availabilityStatus));
    }
    for (const label of ['Idea', 'Build', 'Result']) {
      const button = page.getByRole('button', { name: label, exact: true });
      await button.focus(); await page.keyboard.press('Enter');
      assert.equal(await button.getAttribute('aria-pressed'), 'true');
      assert.equal(await section.locator('figcaption strong').innerText(), label);
    }
    assert.doesNotMatch(await section.innerText(), /Built by|Completed by|RH&P verified|Project owner|Completed project|Actual RH&P project/i);
    const concept = page.locator('.idea-concept').first();
    await concept.locator('summary').focus(); await page.keyboard.press('Enter');
    assert.equal(await concept.evaluate(element => element.open), true);
    await concept.locator('.concept-close').click();
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false);
    assert.ok(calls.every(path => path === '/api/discovery/tradespeople'), 'inspiration may only read discovery, never account/project data');
    await section.screenshot({ path: join(output, `${slug}-${width}.png`) });
    assert.deepEqual(errors, []);
    if (slug === 'barns') {
      const card = page.locator('.built-person').first();
      const name = await card.locator('h4').innerText();
      const href = await card.locator('a').getAttribute('href');
      await card.locator('a').click();
      await page.waitForURL(`**${href}`);
      await page.getByRole('heading', { name, exact: true }).waitFor();
      await page.getByRole('heading', { name: 'RH&P Projects', exact: true }).waitFor();
      assert.match(await page.locator('body').innerText(), /Qualified trades/i);
      assert.match(await page.locator('body').innerText(), /Specialties/i);
      console.log(`Actual profile ${href}: name, qualifications, specialties and RH&P Projects displayed.`);
    }
    if (width === 390) {
      await section.locator('.built-cta a').click();
      await page.waitForURL('**/projects/new');
      // A discovery failure must never turn public inspiration into a role/authentication gate.
      await page.route('**/api/discovery/tradespeople', route => route.fulfill({ status: 403, contentType: 'application/json', body: '{"message":"Unavailable"}' }));
      await page.goto(`${baseURL}/ideas/${slug}`, { waitUntil: 'networkidle' });
      assert.equal(await page.locator('.built-person').count(), 0);
      assert.match(await page.locator('.built-people-status').innerText(), /aren’t available/);
      await page.getByRole('button', { name: 'Idea', exact: true }).click();
      assert.equal(await page.locator('.built-visual figcaption strong').innerText(), 'Idea');
      assert.equal(await page.locator('.built-cta a').getAttribute('href'), '/projects/new');
    }
    console.log(`${slug} ${width}: stages, real collaborator fit, profile links, concept disclosure, responsive layout and public browsing passed.`);
    await page.close();
  }
  console.log(`Review screenshots: ${output}`);
} finally { await browser.close(); }
