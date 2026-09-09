export type TrustBadgeKind = 'trade' | 'qualification' | 'verified' | 'provided' | 'pending' | 'insured' | 'project';
const labels: Record<TrustBadgeKind, string> = {
  qualification: 'Trade', trade: 'Verified', verified: 'Credential verified', provided: 'Credential provided',
  pending: 'Pending verification', insured: 'Insured', project: 'RH&P project',
};
// Original schematic geometry, drawn on a shared 40-unit architectural grid.
export const tradeLinework: Record<string,string> = {
  'Carpentry': 'M5 19 20 6 35 19 M9 16v18h22V16 M15 34V22h10v12 M9 28h6 M25 28h6 M20 6v10',
  'Electrical': 'M7 7v25h12 M7 18h10V9h12v7 M25 20h8 M26 24h6 M28 24v7h6 M25 16a6 6 0 1 1 8 0l-1 4h-6Z M17 30h4v4h-4Z',
  'Plumbing': 'M8 5v17a10 10 0 0 0 10 10h17 M14 5v17a4 4 0 0 0 4 4h17 M5 9h12 M30 23v12 M5 5h12',
  'Drywall': 'M7 6h26v28H7Z M20 6v28 M11 11h1 M28 11h1 M11 20h1 M28 20h1 M11 29h1 M28 29h1',
  'Flooring': 'M5 14 25 6 35 27 15 35Z M8 21 28 13 M12 28 32 20 M15 10l3 7 M22 15l4 8 M19 25l3 7',
  'Exterior Restoration': 'M6 12 20 6 34 12 M9 13v21h23V13 M9 21h23 M9 28h23 M16 13v8 M25 21v7 M17 28v6 M6 34h29',
};
const tradeAccents: Record<string,string> = {
  'Carpentry': 'M14 17 20 12 26 17 M17 22h6',
  'Electrical': 'M5 7h4 M17 7v4 M34 29v4',
  'Plumbing': 'M27 6c0 0-4 5-4 7a4 4 0 0 0 8 0c0-2-4-7-4-7Z',
  'Drywall': 'M18 9v22 M22 9v22',
  'Flooring': 'M5 14 15 35 M15 35 35 27',
  'Exterior Restoration': 'M5 12h4v3 M32 12h4v3 M35 18l-2 3 M35 25l-2 3',
};
function EvidenceMark({kind}: {kind: TrustBadgeKind}) {
  return <svg className="trust-evidence-icon" aria-hidden="true" viewBox="0 0 20 20" fill="none">
    {kind === 'verified' || kind === 'trade' || kind === 'insured'
      ? <path d="m4 10 4 4 8-9" />
      : kind === 'pending'
        ? <><circle cx="10" cy="10" r="7" /><path d="M10 6v4l3 2" /></>
        : kind === 'project'
          ? <><path d="M4 3h9l3 3v11H4Z M12 3v4h4 M7 13V9h6v4 M7 13h6" /></>
          : <><circle cx="10" cy="10" r="6" /><path d="M8 10h4" /></>}
  </svg>;
}
export function TrustBadge({kind, detail}: {kind: TrustBadgeKind; detail?: string}) {
  const isTrade = kind === 'trade' || kind === 'qualification';
  if (isTrade || kind === 'insured') {
    const name = isTrade ? detail || 'Trade' : 'Insured';
    const verified = kind === 'trade' || kind === 'insured';
    return <span className={`trust-mark trust-mark--identity trust-mark--${kind}`}>
      <svg className="trust-trade-icon" aria-hidden="true" viewBox="0 0 40 40" fill="none">
        <path className="trust-construction-line" d="M3 37h34 M3 3v34" />
        <path d={isTrade ? tradeLinework[name] || 'M8 8h24v26H8Z M8 20h24' : 'M7 17a13 13 0 0 1 26 0 M7 17h26 M20 17v14a4 4 0 0 0 8 0 M11 14l9-7 9 7'} />
        <path className="trust-trade-accent" d={isTrade ? tradeAccents[name] || 'M14 8v26' : 'M4 25l3-4 M9 30l3-4'} />
      </svg>
      <span className="trust-identity-copy"><span className="trust-mark-detail">{name}</span>
        {verified && <span className="trust-verification"><EvidenceMark kind={kind} />Verified</span>}
      </span>
    </span>;
  }
  return <span className={`trust-mark trust-mark--evidence trust-mark--${kind}`}>
    <EvidenceMark kind={kind} /><span className="trust-mark-label">{labels[kind]}</span>
  </span>;
}
export type TrustCredential = {type: string; verificationStatus?: string; supportsQualification?: boolean; confirmedTradeName?: string | null; issuedDate?: string | null; expirationDate?: string | null};
export function verifiedTrades(trades: {id:number;name:string}[], credentials: TrustCredential[]) {
  return trades.filter(trade => credentials.some(c => c.verificationStatus === 'VERIFIED' && c.supportsQualification === true && c.confirmedTradeName === trade.name));
}
export function hasCurrentInsurance(credentials: TrustCredential[], today = new Date().toISOString().slice(0,10)) {
  return credentials.some(c => c.type === 'INSURANCE' && c.verificationStatus === 'VERIFIED'
    && !!c.expirationDate && /^\d{4}-\d{2}-\d{2}$/.test(c.expirationDate) && c.expirationDate >= today
    && (!c.issuedDate || c.issuedDate <= today));
}
