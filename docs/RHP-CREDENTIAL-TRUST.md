# Credential trust and evidence boundary

PROVIDED means owner-supplied information. PENDING_VERIFICATION means review is incomplete. VERIFIED means an authorized RH&P process confirmed the credential. No rejection state is needed by the current domain. Owner creation defaults to PROVIDED; owner edits reset verification and the separate trade assessment. Owners cannot set review status, review date, review basis or confirmed trade through the API. No automatic verification service or reviewer workflow has been invented.

Credential verification is distinct from account verification and PersonTrade qualification. A verified personal credential supports a trade only when an authorized assessment also links it to that particular trade and its category is relevant: trade license, union trade credential, apprenticeship completion, trade-school completion or appropriate industry certification. Business credentials never independently establish a person's qualification. Safety/OSHA, manufacturer, equipment and continuing-education credentials never independently qualify a trade, even if the credential itself is verified. Editing credentials does not add or remove existing PersonTrade records; reassessing qualifications after evidence changes remains a future reviewer workflow.

The additive, repeat-safe migration preserves existing facts and initializes legacy credentials as PROVIDED / OTHER. New fields store review status, evidence category, owner-view evidence reference, assessed trade, review date and internal review basis.

## Evidence delivery limitation

The existing profile media endpoint publishes photos without private delivery controls. The application also uses a shared demo account context, not authenticated per-user sessions. Reusing either as secure credential-document delivery would falsely promise privacy. This implementation therefore uses the explicitly permitted metadata/reference fallback: owners can save, replace and delete a reference with the credential; public projections omit it, credential number, notes and review basis. References are plain text, never fetched or linked as documents. No credential bytes are uploaded, stored in public assets or delivered. The editor explains this and disables Upload evidence. Production private upload requires authenticated ownership, private storage and authorized delivery; reference visibility here is a demo-role projection, not production security.

## Small fictional demo history

The one-time `rhp-030-credential-history-v1` seed explicitly represents historical RH&P review, not real credentials or live verification:

- Jordan: verified union journeyman credential; historical issuer/scope review supports his existing Carpentry qualification.
- Marcus: verified electrical trade license; historical issuer/scope review supports his existing Electrical qualification. No jurisdictional license requirement is asserted.
- Nina: pending manufacturer training; does not establish Plumbing qualification.
- Sofia: provided safety training; does not establish Drywall qualification.
- Darius, Leah and Owen receive no invented credentials.

Reviewed examples record 2026-01-07 and an internal review basis. All four are marked fictional in owner notes. The version marker prevents restoring owner edits/deletions on restart. The seed creates no qualifications or private documents.

## Public self-view and marketplace boundary

Homeowner discovery includes the same user's Tradesperson profile. Management requires the active TRADESPERSON role and matching profile ID, including when opened at `/people/{id}`. Public projections omit owner data. Discovery grants no authority to mutate profiles. Bid submission, bid acceptance, reviews and RH&P portfolio creation retain their same-underlying-user guards. Public discovery and marketplace eligibility deliberately use different rules.

## RH&P-030 factual trust marks

Six presentation marks use existing data only:

| Mark | Required data |
|---|---|
| Verified Trade · trade name | Listed qualification plus a VERIFIED credential whose server assessment supportsQualification is true and confirmedTradeName matches that exact qualification. |
| Credential Verified | That credential's status is VERIFIED. |
| Credential Provided | PROVIDED (legacy missing status defaults to Provided). Appears in credential details, not the summary. |
| Pending Verification | PENDING_VERIFICATION. |
| Insured | INSURANCE type, VERIFIED status, known expiration on/after today's UTC date, and no future issued date. Missing expiration suppresses the mark. |
| RH&P Project | RHP_VERIFIED portfolio provenance and a non-null projectId. This is work provenance, not trade verification. |

The UI consumes the existing server assessment; it does not infer trade relevance from a name, issuer or evidence category, and it does not change PersonTrade or opportunity eligibility. Insurance currentness is a presentation check against existing dates, not a new domain state. No new badges are seeded.

Public order is identity (saved person/business preference), factual trust summary, trades/specialties, service area, work, detailed credentials. Removed the public credential-mechanics paragraphs and the account-verification disclaimer block. Non-sensitive dates/jurisdiction are available through native disclosure controls. Owner credential forms retain the review-reset explanation. Private evidence, number, notes and business contact fields remain absent from public presentation.

## V3 possibilities — documentation only

Top Rated, Repeat Hire, Quick Responder, Project Milestones, Years on RH&P and Highly Recommended require real platform activity, documented thresholds and fair measurement. None is implemented or seeded. Owner-performed work remains the future truthful model for self-performed scopes; it does not authorize marketplace self-bidding, self-reviews or fabricated verified work.
