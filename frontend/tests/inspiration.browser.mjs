/** Focused visual/interaction check. Requires Playwright and a running frontend.
 * Optional: PLAYWRIGHT_MODULE=/absolute/path/to/playwright/index.mjs
 * Run: node tests/inspiration.browser.mjs (from frontend).
 */
import assert from 'node:assert/strict';
import { mkdir } from 'node:fs/promises';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright');
const baseURL = process.env.INSPIRATION_BASE_URL || 'http://127.0.0.1:5173';
const output = join(tmpdir(), 'rhp-028-depth-review');
await mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true });
try {
  for (const [slug, width, height] of [['barns',1440,1000], ['rvs',1440,1000], ['beyond',1440,1000], ['rvs',390,844]]) {
    const page = await browser.newPage({ viewport: { width, height }, reducedMotion: 'reduce' });
    const errors = [], apiRequests = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('request', request => { if (new URL(request.url()).pathname.startsWith('/api/')) apiRequests.push(request.url()); });
    await page.goto(`${baseURL}/ideas/${slug}`, { waitUntil: 'networkidle' });
    await page.evaluate(() => document.fonts.ready);
    const concepts = page.locator('.idea-concept');
    assert.equal(await concepts.count(), 3);
    await page.locator('.ideas-closing').scrollIntoViewIfNeeded();
    await page.waitForFunction(() => [...document.images].every(image => image.complete && image.naturalWidth > 0));
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({ path: join(output, `${slug}-${width}.png`), fullPage: true });
    for (let index = 0; index < 3; index++) {
      const concept = concepts.nth(index);
      const summary = concept.locator('summary');
      await summary.focus();
      await page.keyboard.press(index === 1 ? 'Space' : 'Enter');
      await page.waitForFunction(() => document.querySelectorAll('.idea-concept[open]').length === 1);
      assert.equal(await concept.evaluate(element => element.open), true);
      assert.equal(await concept.locator('.concept-detail').isVisible(), true);
      assert.equal(await concept.locator('.concept-detail h4').filter({ hasText: 'Design goals' }).count(), 1);
      assert.equal(await concept.locator('.concept-detail .build-sequence li').count(), 4);
      assert.equal(await concept.locator('.concept-detail .public-button').getAttribute('href'), '/projects/new');
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false);
      if (index === 1) await concept.screenshot({ path: join(output, `${slug}-${width}-detail.png`) });
    }
    // Opening the next native disclosure closes the previous one.
    assert.equal(await page.locator('.idea-concept[open]').count(), 1);
    await concepts.nth(2).locator('.concept-close').click();
    assert.equal(await page.locator('.idea-concept[open]').count(), 0);
    assert.equal(await concepts.nth(2).locator('summary').evaluate(element => element === document.activeElement), true);
    assert.deepEqual(errors, []);
    assert.ok(apiRequests.every(url => new URL(url).pathname === '/api/discovery/tradespeople'));
    console.log(`${slug} ${width}x${height}: visuals, all three keyboard disclosures, exclusive expansion, close/focus, CTA and overflow checks passed; only optional people discovery calls.`);
    if (width === 390) {
      await concepts.first().locator('summary').click();
      await concepts.first().locator('.concept-detail .public-button').click();
      await page.waitForURL('**/projects/new');
      console.log('Concept CTA navigated to /projects/new.');
    }
    await page.close();
  }
  console.log(`Screenshots: ${output}`);
} finally { await browser.close(); }
