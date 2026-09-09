import { TrustBadge, verifiedTrades, hasCurrentInsurance } from '../components/TrustBadge';
import { useState, useId, type ReactNode } from 'react';
import { useMutation } from '@tanstack/react-query';
import { ApiError, request } from '../api/client';
import type { Profile, Specialty, Trade, AvailabilityStatus } from '../api/types';
import { PortfolioCard } from '../components/PortfolioCard';
import type { Portfolio } from '../api/workspace';
import { api } from '../api/client';
import { queryClient } from '../app/query';
import { Action, MutationNotice, Portrait, State, useData, words } from '../components/ui';

type Business = { name: string; description: string; website: string; phone?: string; address?: string; yearsInBusiness: number | null; role: string; logoReference: string | null };
type Credential = { id?: number; scope: 'PERSONAL' | 'BUSINESS'; type: 'LICENSE' | 'INSURANCE' | 'CERTIFICATION'; name: string; issuer: string; number?: string; jurisdiction: string; issuedDate: string | null; expirationDate: string | null; notes?: string; verificationStatus?: 'PROVIDED' | 'PENDING_VERIFICATION' | 'VERIFIED'; evidenceKind?: string; evidenceReference?: string | null; supportsQualification?: boolean; confirmedTradeName?: string | null; verifiedAt?: string | null };
type Details = { profile: Profile; identity: { headline: string; bio: string; presentation: 'PERSON_FIRST' | 'BUSINESS_FIRST'; primaryTradeId?: number | null }; business: Business | null; qualifications: Specialty[]; specialties: Specialty[]; credentials: Credential[] };
const save = (path: string, body: unknown, method = 'PUT') => request(path, { method, body: JSON.stringify(body) });
const refresh = () => queryClient.invalidateQueries();
const blankBusiness: Business = { name: '', description: '', website: '', phone: '', address: '', yearsInBusiness: null, role: '', logoReference: null };
function Field({ label, value, change, type = 'text', required = false, maxLength = 200 }: { label: string; value: string; change: (value: string) => void; type?: string; required?: boolean; maxLength?: number }) {
  return <label>{label}{type === 'textarea' ? <textarea value={value} onChange={e => change(e.target.value)} maxLength={maxLength} required={required} /> : <input type={type} value={value} onChange={e => change(e.target.value)} required={required} maxLength={maxLength} />}</label>;
}
export function ImageUpload({ id, label, reference, onChange }: { id: number; label: string; reference: string | null; onChange: (reference: string | null) => void }) {
  const mutation = useMutation({ mutationFn: async (file: File) => {
    if(file.size > 3000000) throw new Error('Image too large');
    const base64 = await new Promise<string>((resolve, reject) => { const reader = new FileReader(); reader.onload = () => resolve(String(reader.result).split(',')[1]); reader.onerror = reject; reader.readAsDataURL(file); });
    return request<{ reference: string }>(`/tradespeople/${id}/media`, { method: 'POST', body: JSON.stringify({ base64 }) });
  }, onSuccess: result => onChange(result.reference) });
  return <div><label>{label}<input type="file" accept="image/jpeg,image/png" disabled={mutation.isPending} onChange={e => { const file=e.target.files?.[0]; if(file) mutation.mutate(file); e.target.value=''; }} /></label><small>JPEG or PNG, up to 3 MB and 16 megapixels.</small>{reference?.startsWith('/') && <img src={reference} alt={`${label} preview`} width="120" />}{reference && <button type="button" className="secondary" onClick={() => onChange(null)}>Remove {label.toLowerCase()}</button>}<MutationNotice mutation={mutation} /></div>;
}
export function ProfessionalProfile({ profile, id, own, children }: { profile: Profile; id: number; own: boolean; children?: ReactNode }) {
  const query = useData(profile, `professional-profile-${id}`, signal => request<Details>(`/tradespeople/${id}/professional-profile`, { signal }));
  const [editing, setEditing] = useState(false);
  return <State query={query}>{query.data && <>
    <PublicDetails details={query.data}>
    {own && <><button onClick={() => setEditing(!editing)}>{editing ? 'Close editor' : 'Edit Profile'}</button>{editing && <IdentityEditor key={id} details={query.data} />}
    </>}
    </PublicDetails>
    {children}
    <CredentialEditor id={id} credentials={query.data.credentials} business={!!query.data.business} own={own} />
  </>}</State>;
}
function PublicDetails({ details: d, children }: { details: Details; children: ReactNode }) {
  const b = d.business;
  const trustedTrades = verifiedTrades(d.qualifications, d.credentials);
  const insured = hasCurrentInsurance(d.credentials);
  const businessFirst = d.identity.presentation === 'BUSINESS_FIRST' && b;
  const primary = d.qualifications.find(t => t.id === d.identity.primaryTradeId) ?? (d.qualifications.length === 1 ? d.qualifications[0] : undefined);
  return <section className="panel professional-profile">
    <Portrait name={d.profile.displayName} reference={d.profile.profileImageReference} />
    {businessFirst ? <><h2>{b.name}</h2><p>{d.profile.displayName}{b.role && ` · ${b.role}`}</p></> : <><h2>{d.profile.displayName}</h2><p>{d.identity.headline}</p>{b && <p>{b.name}{b.role && ` · ${b.role}`}</p>}</>}
    {businessFirst && <p>{d.identity.headline}</p>}
    <p>{d.identity.bio}</p>
    {b && <section aria-label="Business information">{b.logoReference && <img src={b.logoReference} alt={`${b.name} logo`} width="100" />}<p>{b.description}</p>{b.yearsInBusiness != null && <p>{b.yearsInBusiness} years in business</p>}{b.website && <a href={b.website} target="_blank" rel="noreferrer">Business website ↗</a>}</section>}
    {(d.qualifications.length > 0 || insured) && <div className="profile-trust-summary" aria-label="Trust summary">{d.qualifications.map(t => <TrustBadge key={t.id} kind={trustedTrades.some(v => v.id === t.id) ? "trade" : "qualification"} detail={t.name} />)}{insured && <TrustBadge kind="insured" />}</div>}
    <h3>Trades &amp; specialties</h3>
    <dl><dt>Primary trade</dt><dd>{primary?.name || 'Not selected'}</dd><dt>Additional qualified trades</dt><dd>{d.qualifications.filter(t => t.id !== primary?.id).map(t => t.name).join(', ') || 'None recorded'}</dd><dt>Specialties</dt><dd>{d.specialties.map(s => s.name).join(', ') || 'None recorded'}</dd><dt>Availability & service area</dt><dd>{d.profile.availabilityStatus ? words(d.profile.availabilityStatus) : 'Availability not provided'} · Base ZIP {d.profile.baseZip || 'not provided'} · Service radius {d.profile.serviceRadius ?? 'not provided'}</dd></dl>
    {children}

  </section>;
}
export function IdentityEditor({ details: d }: { details: Details }) {
  const id=d.profile.id;
  const [form, setForm] = useState({ ...d.identity, displayName: d.profile.displayName, baseZip: d.profile.baseZip || '', serviceRadius: d.profile.serviceRadius ?? '' as number | '', availabilityStatus: d.profile.availabilityStatus ?? '' as AvailabilityStatus | '', primaryTradeId: d.identity.primaryTradeId ?? (d.qualifications.length === 1 ? d.qualifications[0].id : null), specialtyIds: d.specialties.map(s => s.id), business: d.business });
  const catalog=useData(d.profile, 'catalog', api.trades);
  const mutation=useMutation({ mutationFn: () => save(`/tradespeople/${id}/professional-profile`,form), onSuccess: refresh });
  const photo=useMutation({ mutationFn: (reference: string | null) => save(`/tradespeople/${id}/photo`, { reference }), onSuccess: refresh });
  const business=(key: keyof Business, value: string | number | null) => setForm({ ...form, business: { ...form.business!, [key]: value } });
  return <form className="profile-editor" onSubmit={e => { e.preventDefault(); mutation.mutate(); }}>
    <fieldset><legend>Professional identity</legend>
      <Field label="Display name" value={form.displayName} change={displayName => setForm({ ...form, displayName })} required />
      <Field label="Professional headline / title" value={form.headline} change={headline => setForm({ ...form, headline })} />
      <Field label="About / professional bio" type="textarea" maxLength={5000} value={form.bio} change={bio => setForm({ ...form, bio })} />
      <label>Preferred public presentation<select value={form.presentation} onChange={e => setForm({ ...form, presentation: e.target.value as typeof form.presentation })}><option value="PERSON_FIRST">Person-first</option><option value="BUSINESS_FIRST" disabled={!form.business}>Business-first</option></select></label>
      <Portrait name={d.profile.displayName} reference={d.profile.profileImageReference} />
      <ImageUpload id={id} label="Profile photo" reference={d.profile.profileImageReference} onChange={ref => photo.mutate(ref)} /><MutationNotice mutation={photo} />
    </fieldset>
    <fieldset><legend>Optional business</legend><label><input type="checkbox" checked={!!form.business} onChange={e => setForm({ ...form, business: e.target.checked ? { ...blankBusiness } : null, presentation: 'PERSON_FIRST' })} />I represent a business</label>
      {form.business && <>{(['name','description','website','phone','address','role'] as const).map(key => <Field key={key} label={`Business ${key}${key === 'phone' || key === 'address' ? ' (private)' : ''}`} value={form.business![key] || ''} change={value => business(key,value)} type={key === 'description' ? 'textarea' : key === 'website' ? 'url' : 'text'} maxLength={key === 'description' ? 5000 : key === 'website' ? 1000 : key === 'address' ? 500 : key === 'phone' ? 100 : 200} required={key === 'name'} />)}
      <label>Years in business<input type="number" min="0" max="300" value={form.business.yearsInBusiness ?? ''} onChange={e => business('yearsInBusiness',e.target.value ? Number(e.target.value) : null)} /></label>
      <ImageUpload id={id} label="Business logo" reference={form.business.logoReference} onChange={ref => business('logoReference',ref)} /></>}
      <small>Remove business credentials before removing a business. Phone and address stay private.</small>
    </fieldset>
    <TradesEditor qualifications={d.qualifications} catalog={catalog} primaryTradeId={form.primaryTradeId}
      specialtyIds={form.specialtyIds} onPrimary={primaryTradeId => setForm({ ...form, primaryTradeId })}
      onSpecialties={specialtyIds => setForm({ ...form, specialtyIds })} saving={mutation.isPending || photo.isPending} />
    <fieldset><legend>Availability & service area</legend><label>Availability<select required value={form.availabilityStatus} onChange={e => setForm({ ...form, availabilityStatus: e.target.value as AvailabilityStatus | '' })}><option value="">Select availability</option>{['AVAILABLE_NOW','AVAILABLE_SOON','BUSY','NOT_ACCEPTING_WORK'].map(value => <option key={value} value={value}>{words(value)}</option>)}</select></label>
      <label>Base ZIP<input value={form.baseZip} pattern="[0-9]{5}" required onChange={e => setForm({ ...form, baseZip: e.target.value })} /></label>
      <label>Service radius<input type="number" min="0" max="1000" required value={form.serviceRadius} onChange={e => setForm({ ...form, serviceRadius: e.target.value === '' ? '' : Number(e.target.value) })} /></label>
    </fieldset><button disabled={mutation.isPending || photo.isPending}>Save profile</button><ProfileSaveNotice mutation={mutation} />
  </form>;
}
export function TradesEditor({ qualifications, catalog, primaryTradeId, specialtyIds, onPrimary, onSpecialties, saving }: {
  qualifications: Specialty[];
  catalog: { data?: Trade[]; isPending: boolean; isError: boolean; error: Error | null; refetch: () => unknown };
  primaryTradeId: number | null;
  specialtyIds: number[];
  onPrimary: (id: number) => void;
  onSpecialties: (ids: number[]) => void;
  saving: boolean;
}) {
  const primaryName = useId();
  return <fieldset className="trades-editor"><legend>Trades &amp; specialties</legend>
    <div><h3>Qualified trades</h3><div className="qualified-trades">
      {qualifications.map(trade => <span className="qualified-trade" key={trade.id}><strong>{trade.name}</strong><span>Qualified</span></span>)}
      {!qualifications.length && <p>No qualified trades recorded.</p>}
    </div></div>
    <div><h3>Primary trade</h3>
      {qualifications.length === 1 ? <p className="primary-trade-name">{qualifications[0].name}</p>
        : qualifications.length > 1 ? <div className="trade-options" role="group" aria-label="Primary trade">
          {qualifications.map(trade => <label className="trade-choice" key={trade.id}>
            <input type="radio" name={primaryName} value={trade.id} required checked={primaryTradeId === trade.id} onChange={() => onPrimary(trade.id)} />
            <span>{trade.name}<span className="choice-check" aria-hidden="true">✓</span></span>
          </label>)}
        </div> : <p>A primary trade can be selected once a qualification is recorded.</p>}
      {qualifications.length > 1 && <small>Choose which qualified trade appears first on your profile.</small>}
    </div>
    <div><h3>Specialties</h3><State query={catalog}>
      <div className="trade-options" role="group" aria-label="Specialties">
        {catalog.data?.flatMap(trade => trade.specialties).map(specialty => <label className="trade-choice" key={specialty.id}>
          <input type="checkbox" checked={specialtyIds.includes(specialty.id)} onChange={event => onSpecialties(event.target.checked ? [...specialtyIds, specialty.id] : specialtyIds.filter(id => id !== specialty.id))} />
          <span>{specialty.name}<span className="choice-check" aria-hidden="true">✓</span></span>
        </label>)}
      </div>
    </State><small>Specialties describe your work; selecting one does not verify a qualification.</small></div>
    <p className="trade-qualification-note">Additional trade qualifications cannot be requested through profile editing yet.</p>
    <div><button type="submit" disabled={saving}>Save changes</button></div>
  </fieldset>;
}
export function ProfileSaveNotice({ mutation }: { mutation: { isError: boolean; error: Error | null; isSuccess: boolean } }) {
  if (!mutation.isError) return mutation.isSuccess ? <p role="status">Profile saved, including availability and service area.</p> : null;
  const error = mutation.error;
  const fields = error instanceof ApiError ? error.details?.fieldErrors : undefined;
  const serviceError = fields && ['baseZip', 'serviceRadius', 'availabilityStatus'].some(key => key in fields);
  return <p role="alert">{serviceError
    ? 'Your availability and service area were not saved. Choose an availability status, enter a five-digit ZIP, and a service radius from 0 to 1000.'
    : error instanceof ApiError && error.status === 403
      ? 'Your profile changes were not saved. An active owner profile is required to edit availability and service area.'
      : 'Your profile changes, including availability and service area, were not saved. Check your entries and try Save profile again. Your entries are still here.'}</p>;
}
const blankCredential: Credential = { scope:'PERSONAL',type:'LICENSE',name:'',issuer:'',number:'',jurisdiction:'',issuedDate:null,expirationDate:null,notes:'',evidenceKind:'OTHER',evidenceReference:'' };
function CredentialEditor({ id, credentials, business, own }: { id: number; credentials: Credential[]; business: boolean; own: boolean }) {
  const [form,setForm]=useState<Credential | null>(null);
  const mutation=useMutation({ mutationFn: () => save(`/tradespeople/${id}/credentials${form?.id ? `/${form.id}` : ''}`,form,form?.id ? 'PUT' : 'POST'), onSuccess: async () => { setForm(null); await refresh(); } });
  return <section className="profile-credentials"><h2>Credentials &amp; qualifications</h2>
    {own && <button onClick={() => setForm({ ...blankCredential })}>Add credential</button>}
    {credentials.map(c => <article key={c.id}><h3>{c.name}</h3><p>{c.issuer}</p><p>{words(c.scope)} · {words(c.type)}</p>
      <TrustBadge kind={c.verificationStatus === 'VERIFIED' ? 'verified' : c.verificationStatus === 'PENDING_VERIFICATION' ? 'pending' : 'provided'} />
      {(c.jurisdiction || c.issuedDate || c.expirationDate || c.verifiedAt) && <details><summary>View credential details</summary>
        {c.jurisdiction && <p>{c.jurisdiction}</p>}{c.issuedDate && <p>Issued <time dateTime={c.issuedDate}>{c.issuedDate}</time></p>}
        {c.expirationDate && <p>Expires <time dateTime={c.expirationDate}>{c.expirationDate}</time></p>}
        {c.verifiedAt && <p>Verified <time dateTime={c.verifiedAt}>{c.verifiedAt}</time></p>}
      </details>}
      {own && c.evidenceReference && <p>Evidence reference: {c.evidenceReference}</p>}
      {own && <div className="profile-item-actions"><button className="secondary" aria-label={`Edit ${c.name}`} onClick={() => setForm(c)}>Edit</button><Action label="Delete" confirm={`Delete ${c.name}?`} run={async () => { await request(`/tradespeople/${id}/credentials/${c.id}`,{ method:'DELETE' }); await refresh(); }} /></div>}
    </article>)}
    {!credentials.length && <p>No credentials provided.</p>}
    {form && <form className="profile-editor" onSubmit={e => {e.preventDefault();mutation.mutate();}}><label>Credential belongs to<select value={form.scope} onChange={e => setForm({ ...form,scope:e.target.value as Credential['scope'] })}><option value="PERSONAL">Personal</option>{business && <option value="BUSINESS">Business</option>}</select></label><label>Credential type<select value={form.type} onChange={e => setForm({ ...form,type:e.target.value as Credential['type'],evidenceKind:'OTHER' })}>{['LICENSE','INSURANCE','CERTIFICATION'].map(t => <option key={t} value={t}>{words(t)}</option>)}</select></label>
      <label>Credential category<select value={form.evidenceKind || 'OTHER'} onChange={e => setForm({ ...form,evidenceKind:e.target.value })}>
        {(form.type === 'LICENSE' ? ['OTHER','TRADE_LICENSE'] : form.type === 'INSURANCE' ? ['OTHER','INSURANCE'] : ['OTHER','UNION_TRADE_CREDENTIAL','APPRENTICESHIP_COMPLETION','TRADE_SCHOOL_COMPLETION','INDUSTRY_CERTIFICATION','SAFETY_CERTIFICATION','MANUFACTURER_CERTIFICATION','EQUIPMENT_CERTIFICATION','CONTINUING_EDUCATION']).map(kind => <option key={kind} value={kind}>{words(kind)}</option>)}
      </select></label>
      <Field label="Evidence reference (owner view)" value={form.evidenceReference || ''} maxLength={500} change={evidenceReference => setForm({ ...form,evidenceReference })} />
      <div><button type="button" disabled>Upload evidence</button><p>Private document upload is not available yet. You can save or replace a document reference; no file is uploaded.</p></div>
      {(['name','issuer','number','jurisdiction','notes'] as const).map(key => <Field key={key} label={key === 'number' ? 'Credential number (private)' : key === 'notes' ? 'Notes (private)' : words(key)} value={form[key] || ''} change={v => setForm({ ...form,[key]:v })} required={key === 'name'} type={key === 'notes' ? 'textarea' : 'text'} maxLength={key === 'notes' ? 2000 : 200} />)}
      {(['issuedDate','expirationDate'] as const).map(key => <Field key={key} label={key === 'issuedDate' ? 'Issued date' : 'Expiration date'} type="date" value={form[key] || ''} change={v => setForm({ ...form,[key]:v || null })} />)}
      <p>Saving changes sets this credential to Provided. Verification and trade relevance require a separate authorized review.</p><button disabled={mutation.isPending}>Save credential</button><button type="button" className="secondary" onClick={() => setForm(null)}>Cancel</button>
    </form>}<MutationNotice mutation={mutation} /></section>;
}
export function ExternalPortfolio({ id, items, name, own }: { id: number; items: Portfolio[]; name: string; own: boolean }) {
  const [form,setForm]=useState<{ id?:number; title:string;description:string;completionDate:string|null;mediaReference:string|null } | null>(null);
  const mutation=useMutation({ mutationFn: () => save(`/tradespeople/${id}/portfolio${form?.id ? `/${form.id}` : ''}`,form,form?.id ? 'PUT' : 'POST'), onSuccess: async () => {setForm(null);await refresh();} });
  return <section className="portfolio-section" aria-label="External Portfolio">
    <div className="section-heading"><div><h3>External Portfolio</h3><p>Work performed outside Rock &amp; Hard Places.</p></div>
      {own && <button onClick={() => setForm({ title:'',description:'',completionDate:null,mediaReference:null })}>+ Add work</button>}
    </div>
    <div className="portfolio-grid">
      {items.filter(i => i.provenance !== 'RHP_VERIFIED').map(item => <PortfolioCard key={item.id} item={item} name={name} ownerActions={own ? <>
        <button className="secondary" aria-label={`Edit ${item.title}`} onClick={() => setForm({ ...item,mediaReference:item.mediaReference || null })}>Edit</button>
        <Action label="Delete" confirm={`Delete ${item.title}?`} run={async () => {await request(`/tradespeople/${id}/portfolio/${item.id}`,{method:'DELETE'});await refresh();}} />
      </> : undefined} />)}
    </div>
    {!items.some(i => i.provenance !== 'RHP_VERIFIED') && <p>No external work added yet.</p>}
    {form && <form className="profile-editor" onSubmit={e => {e.preventDefault();mutation.mutate();}}><Field label="Work title" required value={form.title} change={title => setForm({ ...form,title })} /><Field label="Work description" type="textarea" maxLength={5000} value={form.description} change={description => setForm({ ...form,description })} /><Field label="Completion date" type="date" value={form.completionDate || ''} change={completionDate => setForm({ ...form,completionDate:completionDate || null })} /><ImageUpload id={id} label="Cover image" reference={form.mediaReference} onChange={mediaReference => setForm({ ...form,mediaReference })} /><p>Self-reported external work. Editing previously checked external work returns it to self-reported status.</p><button disabled={mutation.isPending}>Save external work</button><button type="button" className="secondary" onClick={() => setForm(null)}>Cancel</button></form>}<MutationNotice mutation={mutation} />
  </section>;
}
