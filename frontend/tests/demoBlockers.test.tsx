import test from 'node:test';
import assert from 'node:assert/strict';
import {renderToStaticMarkup} from 'react-dom/server';
import {QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter, Routes, Route} from 'react-router';
import {queryClient, profileKey} from '../src/app/query';
import {OpportunityDetail} from '../src/features/work';
import {TaskReview} from '../src/features/reviews';
import {resolvePortfolioMedia, seedPortrait} from '../src/components/seedMedia';
import type {Profile, Task} from '../src/api/types';
const profile:Profile={id:77,role:'TRADESPERSON',displayName:'Builder',accountStatus:'ACTIVE',verificationStatus:'VERIFIED',profileImageReference:null,availabilityStatus:'AVAILABLE_NOW',baseZip:'19130',serviceRadius:20};
const task:Task={id:1,projectId:2,title:'Cabinet scope',description:'Fit cabinets',status:'COMPLETED',parentTaskId:null,requiredTrades:[],progressPercentage:100};
for(const status of [null,'SUBMITTED','ACCEPTED','REJECTED','WITHDRAWN']) test(`opportunity existing ${status} controls submission and owner actions`,()=>{
 queryClient.clear();queryClient.setQueryData(profileKey(profile,'opportunity-5'),{task,project:{title:'Kitchen',jobZip:'19130'},requiredTrade:{id:5,tradeName:'Carpentry'},bidding:{allowed:true},ownBids:status?[{id:3,tradespersonId:77,status,amount:100,message:'Approach'}]:[]});
 const html=renderToStaticMarkup(<QueryClientProvider client={queryClient}><MemoryRouter initialEntries={['/opportunities/5']}><Routes><Route path="/opportunities/:requirementId" element={<OpportunityDetail profile={profile}/>}/></Routes></MemoryRouter></QueryClientProvider>);
 assert.equal(html.includes('Submit task-trade bid'),status===null);
 assert.equal(html.includes('Edit proposal'),status==='SUBMITTED');assert.equal(html.includes('Withdraw proposal'),status==='SUBMITTED');
 if(status) assert.match(html,/View proposal/);
});
test('retrieved review renders author subject rating body and date without duplicate publish form',()=>{
 queryClient.clear();const owner={...profile,role:'HOMEOWNER' as const};
 queryClient.setQueryData(profileKey(owner,'assignments-1'),[{id:1,tradespersonId:77,displayName:'Builder'}]);
 queryClient.setQueryData(profileKey(owner,'reviews-1'),[{id:8,tradespersonId:77,authorDisplayName:'Jordan',tradespersonDisplayName:'Builder',overallRating:5,body:'Persisted feedback',createdAt:'2026-09-09T12:00:00Z',withdrawn:false}]);
 const html=renderToStaticMarkup(<QueryClientProvider client={queryClient}><TaskReview profile={owner} task={task}/></QueryClientProvider>);
 assert.match(html,/Published review/);assert.match(html,/Jordan reviewed Builder/);assert.match(html,/Persisted feedback/);assert.match(html,/2026-09-09T12:00:00Z/);assert.doesNotMatch(html,/Publish task review/);
});
test('stable external cover survives text changes and explicit removal stays removed',()=>{
 const item={title:'Walnut reading nook',provenance:'SELF_REPORTED',mediaReference:'/seed-media/portfolios/jordan-ellis/walnut-reading-nook.jpg'};
 assert.equal(resolvePortfolioMedia('Renamed Jordan',{...item,title:'Renamed work'}).cover?.url,item.mediaReference);
 assert.deepEqual(resolvePortfolioMedia('Jordan Ellis',{...item,mediaReference:null}).frames,[]);
 assert.equal(seedPortrait({name:'Jordan Ellis',reference:null}),undefined);
});
