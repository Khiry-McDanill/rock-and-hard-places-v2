import test from 'node:test';import assert from 'node:assert/strict';
import {renderToStaticMarkup} from 'react-dom/server';import {QueryClientProvider} from '@tanstack/react-query';import {MemoryRouter} from 'react-router';
import {ProposalCard,Opportunities} from '../src/features/work';import {TrustBadge,tradeLinework} from '../src/components/TrustBadge';import {queryClient,profileKey} from '../src/app/query';import type {Profile} from '../src/api/types';
const profile:Profile={id:77,role:'TRADESPERSON',displayName:'Builder',accountStatus:'ACTIVE',verificationStatus:'VERIFIED',profileImageReference:null,availabilityStatus:'AVAILABLE_NOW',baseZip:'19807',serviceRadius:20};
for(const status of ['SUBMITTED','ACCEPTED','REJECTED','WITHDRAWN']) test(`proposal ${status} preserves view and permits only submitted actions`,()=>{
 const html=renderToStaticMarkup(<QueryClientProvider client={queryClient}><ProposalCard item={{bid:{id:1,amount:1200,message:'Approach',status,tradespersonId:77},task:null,project:null}}/></QueryClientProvider>);
 assert.match(html,/View proposal/);assert.equal(html.includes('Edit proposal'),status==='SUBMITTED');assert.equal(html.includes('Withdraw proposal'),status==='SUBMITTED');assert.match(html,new RegExp(status[0]+status.slice(1).toLowerCase()));assert.match(html,/Scope details are no longer available/);
});
test('Find Work explains qualified open scopes and labels ZIP without internal policy copy',()=>{
 queryClient.clear();queryClient.setQueryData(profileKey(profile,'catalog'),[]);queryClient.setQueryData(profileKey(profile,'opportunities--'),[]);
 const html=renderToStaticMarkup(<QueryClientProvider client={queryClient}><MemoryRouter><Opportunities profile={profile}/></MemoryRouter></QueryClientProvider>);
 assert.match(html,/Work ZIP/);assert.match(html,/open project scopes matching your qualified trades/);assert.match(html,/No matching open scopes right now/);assert.match(html,/without an open trade requirement/);assert.doesNotMatch(html,/underlying user|self-dealing|Project ZIP/);
});
test('six catalog trades have distinct accessible marks without implying verification',()=>{
 assert.equal(Object.keys(tradeLinework).length,6);assert.equal(new Set(Object.values(tradeLinework)).size,6);
 for(const trade of Object.keys(tradeLinework)) {
  const plain=renderToStaticMarkup(<TrustBadge kind="qualification" detail={trade}/>);const verified=renderToStaticMarkup(<TrustBadge kind="trade" detail={trade}/>);
  assert.ok(plain.includes(trade));assert.ok(plain.includes(tradeLinework[trade]));assert.match(plain,/aria-hidden="true"/);assert.doesNotMatch(plain,/Verified|Uninsured/);assert.match(verified,/Verified/);assert.ok(verified.indexOf(trade+'</span>') < verified.indexOf('class="trust-verification"'));assert.ok(verified.includes(tradeLinework[trade]));
 }
});
