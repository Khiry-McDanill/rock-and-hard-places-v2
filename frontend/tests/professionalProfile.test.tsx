import { TrustBadge, verifiedTrades, hasCurrentInsurance } from '../src/components/TrustBadge';
import { MemoryRouter, Routes, Route } from 'react-router';
import { PersonProfile } from '../src/features/people';
import test from 'node:test';
import assert from 'node:assert/strict';
import { renderToStaticMarkup } from 'react-dom/server';
import { QueryClientProvider } from '@tanstack/react-query';
import { ProfessionalProfile, ExternalPortfolio, IdentityEditor, ProfileSaveNotice, TradesEditor } from '../src/features/professionalProfile';
import { queryClient, profileKey } from '../src/app/query';
import { ApiError } from '../src/api/client';
import { PortfolioCard } from '../src/components/PortfolioCard';
import type { Profile } from '../src/api/types';
const profile: Profile = { id: 55,role:'TRADESPERSON',displayName:'Responsible Human',profileImageReference:'/api/tradespeople/55/media/photo',accountStatus:'ACTIVE',verificationStatus:'VERIFIED',baseZip:'19801',serviceRadius:25,availabilityStatus:'AVAILABLE_NOW' };
for(const own of [true,false]) for(const presentation of ['BUSINESS_FIRST','PERSON_FIRST']) {
  test(`professional profile ${presentation}, owner=${own}, preserves human and credential scope`,() => {
    queryClient.clear();
    queryClient.setQueryData(profileKey(profile,'professional-profile-55'), { profile,identity:{headline:'Carpenter',bio:'Restoring homes',presentation,primaryTradeId:1},business:{name:'Good Culture',description:'Fine work',website:'',yearsInBusiness:4,role:'Owner',logoReference:null},qualifications:[{id:1,name:'Carpentry'}],specialties:[{id:3,name:'Remodeling'}],credentials:[{id:2,name:'Contractor registration',scope:'BUSINESS',type:'LICENSE',issuer:'Provider',jurisdiction:'DE',verificationStatus:'PROVIDED'}] });
    const html=renderToStaticMarkup(<QueryClientProvider client={queryClient}><ProfessionalProfile profile={profile} id={55} own={own} /></QueryClientProvider>);
    assert.match(html,/Responsible Human/);assert.match(html,/Good Culture/);assert.match(html,/Carpentry/);assert.match(html,/Remodeling/);assert.match(html,/Business · License/);assert.match(html,/Credential provided/);assert.doesNotMatch(html,/does not automatically establish a trade qualification/);
    if(presentation === 'BUSINESS_FIRST') assert.match(html,/<h2>Good Culture<\/h2>/);else assert.match(html,/<h2>Responsible Human<\/h2>/);
    assert.equal(html.includes('Edit Profile'),own);assert.equal(html.includes('Add credential'),own);
  });
}
const work = {id:2,title:'Walnut reading nook',description:'Own work',provenance:'SELF_REPORTED' as const,completionDate:null,projectId:null,taskId:null,approvedAttachmentIds:[]};
for (const own of [true, false]) test(`external portfolio owns its card actions without a duplicate list, owner=${own}`,() => {
  const html=renderToStaticMarkup(<QueryClientProvider client={queryClient}><ExternalPortfolio id={55} items={[work]} name={profile.displayName} own={own} /></QueryClientProvider>);
  assert.doesNotMatch(html,/Manage External Portfolio/);
  assert.equal((html.match(/class="portfolio-card-title">Walnut reading nook</g) || []).length,1);
  assert.equal(html.includes('+ Add work'),own);
  assert.equal(html.includes('aria-label="Edit Walnut reading nook"'),own);
  assert.equal(html.includes('>Delete</button>'),own);
  assert.equal(html.includes('portfolio-owner-actions'),own);
  if(own) assert.match(html,/<article class="portfolio-item">[\s\S]*portfolio-owner-actions[\s\S]*>Edit<\/button>[\s\S]*>Delete<\/button>[\s\S]*<\/article>/);
});
test('RH&P cards reject owner actions even if supplied',() => {
  const item={...work,provenance:'RHP_VERIFIED' as const};
  const html=renderToStaticMarkup(<PortfolioCard item={item} name={profile.displayName} ownerActions={<button>Edit</button>} />);
  assert.doesNotMatch(html,/portfolio-owner-actions|>Edit<|>Delete</);
});
for (const missing of [false, true]) test(`owner availability loads with editable service fields, missing=${missing}`,() => {
  queryClient.clear();
  const p=missing ? {...profile,baseZip:null,serviceRadius:null,availabilityStatus:null} : profile;
  const details={profile:p,identity:{headline:'',bio:'',presentation:'PERSON_FIRST' as const},business:null,qualifications:[],specialties:[],credentials:[]};
  queryClient.setQueryData(profileKey(p,'professional-profile-55'),details);
  queryClient.setQueryData(profileKey(p,'catalog'),[]);
  const html=renderToStaticMarkup(<QueryClientProvider client={queryClient}><ProfessionalProfile profile={p} id={55} own /><IdentityEditor details={details} /></QueryClientProvider>);
  assert.doesNotMatch(html,/couldn’t be loaded/);
  assert.match(html,/Edit Profile/);assert.match(html,/Save profile/);
  if(missing) {
    assert.match(html,/Availability not provided/);assert.match(html,/Base ZIP not provided/);assert.match(html,/Service radius not provided/);
    assert.match(html,/<option value="" selected="">Select availability/);
    assert.match(html,/<label>Service radius<input[^>]*value=""/);
  } else {
    assert.match(html,/Available now/);assert.match(html,/Base ZIP 19801/);assert.match(html,/Service radius 25/);
  }
});
test('service-area save failures identify the action and recovery',() => {
  const error=new ApiError(400,'Validation failed',{status:400,error:'Bad Request',message:'Validation failed',path:'/api/tradespeople/55/professional-profile',fieldErrors:{baseZip:'invalid'}});
  const html=renderToStaticMarkup(<ProfileSaveNotice mutation={{isError:true,error,isSuccess:false}} />);
  assert.match(html,/role="alert"/);assert.match(html,/availability and service area were not saved/);assert.match(html,/five-digit ZIP/);assert.doesNotMatch(html,/couldn’t be loaded/);
});

const tradesCatalog = {data:[{id:1,name:'Carpentry',specialties:[{id:10,name:'Built-in cabinetry'},{id:11,name:'Finish carpentry'}]}],isPending:false,isError:false,error:null,refetch:()=>{}};
function tradesEditor(qualifications: {id:number;name:string}[], primaryTradeId: number|null) {
  return renderToStaticMarkup(<TradesEditor qualifications={qualifications} catalog={tradesCatalog} primaryTradeId={primaryTradeId} specialtyIds={[10]} onPrimary={()=>{}} onSpecialties={()=>{}} saving={false} />);
}
test('one qualified trade is read-only primary presentation without a selector',()=>{
  const html=tradesEditor([{id:1,name:'Carpentry'}],1);
  assert.match(html,/qualified-trade/);assert.match(html,/Qualified/);assert.match(html,/primary-trade-name">Carpentry/);
  assert.doesNotMatch(html,/<select|type="radio"|Not selected/);
  assert.match(html,/cannot be requested through profile editing yet/);
});
test('multiple qualified trades use accessible radio choices and preserve primary selection',()=>{
  const html=tradesEditor([{id:1,name:'Carpentry'},{id:2,name:'Plumbing'}],2);
  assert.equal((html.match(/type="radio"/g)||[]).length,2);
  assert.match(html,/<input type="radio"[^>]*checked=""[^>]*value="2"/);
  assert.doesNotMatch(html,/Not selected|Add another trade|Request another trade/);
});
test('specialty chips retain native checkbox semantics and saved selection',()=>{
  const html=tradesEditor([{id:1,name:'Carpentry'}],1);
  assert.equal((html.match(/type="checkbox"/g)||[]).length,2);
  assert.match(html,/aria-label="Specialties"/);
  assert.match(html,/<label class="trade-choice"><input type="checkbox" checked=""\/><span>Built-in cabinetry/);
  assert.match(html,/Save changes/);
});
for(const status of ['PROVIDED','PENDING_VERIFICATION','VERIFIED']) for(const own of [true,false]) test(`credential status ${status} and evidence privacy owner=${own}`,()=>{
 queryClient.clear();
 queryClient.setQueryData(profileKey(profile,'professional-profile-55'),{profile,identity:{presentation:'PERSON_FIRST'},business:null,qualifications:[],specialties:[],credentials:[{id:1,name:'Safety certificate',scope:'PERSONAL',type:'CERTIFICATION',verificationStatus:status,evidenceReference:'private-owner-reference',supportsQualification:false}]});
 const html=renderToStaticMarkup(<QueryClientProvider client={queryClient}><ProfessionalProfile profile={profile} id={55} own={own}/></QueryClientProvider>);
 assert.match(html,new RegExp(status==='PROVIDED'?'Credential provided':status==='VERIFIED'?'Credential verified':'Pending verification'));
 assert.equal(html.includes('private-owner-reference'),own);
 assert.doesNotMatch(html,/Verified evidence supporting/);
});

for(const role of ['HOMEOWNER','TRADESPERSON'] as const) test(`same profile public route respects active ${role} authority`,()=>{
 queryClient.clear();const viewer={...profile,role};
 queryClient.setQueryData(profileKey(viewer,'person-55'),profile);
 queryClient.setQueryData(profileKey(viewer,'professional-profile-55'),{profile,identity:{presentation:'PERSON_FIRST'},business:null,qualifications:[],specialties:[],credentials:[]});
 queryClient.setQueryData(profileKey(viewer,'portfolio-55'),[work]);
 const html=renderToStaticMarkup(<QueryClientProvider client={queryClient}><MemoryRouter initialEntries={['/people/55']}><Routes><Route path="/people/:personId" element={<PersonProfile profile={viewer}/>}/></Routes></MemoryRouter></QueryClientProvider>);
 assert.equal(html.includes('Edit Profile'),role==='TRADESPERSON');assert.equal(html.includes('+ Add work'),role==='TRADESPERSON');assert.equal(html.includes('portfolio-owner-actions'),role==='TRADESPERSON');
});

test('trade marks require a qualified trade and explicit matching verified assessment',()=>{
 const trades=[{id:1,name:'Carpentry'},{id:2,name:'Plumbing'}];
 const c={type:'CERTIFICATION',verificationStatus:'VERIFIED',supportsQualification:true,confirmedTradeName:'Carpentry'};
 assert.deepEqual(verifiedTrades(trades,[c]),[trades[0]]);
 for(const change of [{verificationStatus:'PROVIDED'},{verificationStatus:'PENDING_VERIFICATION'},{supportsQualification:false},{confirmedTradeName:'Electrical'}]) assert.deepEqual(verifiedTrades(trades,[{...c,...change}]),[]);
});
test('insured requires verified insurance and known current expiration',()=>{
 const c={type:'INSURANCE',verificationStatus:'VERIFIED',expirationDate:'2026-12-31',issuedDate:'2026-01-01'};
 assert.equal(hasCurrentInsurance([c],'2026-09-09'),true);
 for(const change of [{type:'LICENSE'},{verificationStatus:'PROVIDED'},{verificationStatus:'PENDING_VERIFICATION'},{expirationDate:null},{expirationDate:'2026-09-08'},{issuedDate:'2026-10-01'}]) assert.equal(hasCurrentInsurance([{...c,...change}],'2026-09-09'),false);
});
test('public work precedes credentials without lectures or private evidence',()=>{
 queryClient.clear();queryClient.setQueryData(profileKey(profile,'professional-profile-55'),{profile,identity:{presentation:'PERSON_FIRST'},business:null,qualifications:[],specialties:[],credentials:[{id:1,name:'Private metadata test',type:'CERTIFICATION',scope:'PERSONAL',evidenceReference:'SECRET',number:'SECRET',notes:'SECRET',verificationStatus:'PROVIDED'}]});
 const html=renderToStaticMarkup(<QueryClientProvider client={queryClient}><ProfessionalProfile profile={profile} id={55} own={false}><h2>Work with a story</h2></ProfessionalProfile></QueryClientProvider>);
 assert.ok(html.indexOf('Work with a story')<html.indexOf('Credentials &amp; qualifications'));
 assert.doesNotMatch(html,/SECRET|automatically establish|not verified by this|Each credential shows|trust-mark--trade|trust-mark--insured/);
});
test('project mark is provenance text, separate from skill verification',()=>{
 const html=renderToStaticMarkup(<TrustBadge kind="project"/>);assert.match(html,/RH&amp;P project/);assert.doesNotMatch(html,/Verified trade|Credential verified/);
});
