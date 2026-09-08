import test from 'node:test';
import assert from 'node:assert/strict';
import { resolvePortfolioMedia } from '../src/components/seedMedia';

test('external portfolio media uses backend references independently of person names and inspiration', () => {
  const item = {title:'Compact built-in storage', provenance:'SELF_REPORTED', mediaReference:'/seed-media/portfolios/rhp-029/compact-built-in-storage.png'};
  const media = resolvePortfolioMedia('Renamed profile', item);
  assert.equal(media.source, 'delivered');
  assert.deepEqual(media.frames.map(f=>f.url), [item.mediaReference]);
  assert.ok(media.frames.every(f=>!f.stage));
  assert.equal(resolvePortfolioMedia('Owen Price', {...item, mediaReference:null}).frames.length,0);
  assert.equal(resolvePortfolioMedia('Owen Price', {...item, mediaReference:'opaque/key'}).frames.length,0);
  assert.equal(resolvePortfolioMedia('Owen Price', {...item, provenance:'RHP_VERIFIED'}).frames.length,0);
});
