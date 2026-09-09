# Future business and crew architecture

This document proposes future work; RH&P-030 does not implement it.

Person → Business Membership → Business → Project Team → Bid → Proposed Workers → Credential / Scope Coverage

The current Tradesperson remains the responsible human, backed by User. One optional business presentation is keyed to that profile. Personal and business credentials have explicit scopes; they are owner-provided assertions and do not confer verified qualifications. Future migration can give businesses independent IDs and move the existing representation into a membership without copying credentials.

A future business may have multiple people, administrators, member roles and invitations, and a business-level portfolio. An authorized bidder might submit a bid for Good Culture LLC Construction, submitted by Khiry McDanill · Owner, proposing Khiry McDanill · Carpentry, Nina Alvarez · Plumbing, and Marcus Reed · Electrical. Each proposed worker would be associated with a specific scope. Project-specific teams could later become longer-term crews through an explicit workflow.

Future eligibility must distinguish the responsible party, authorized bidder, proposed worker, credential coverage, and scope eligibility. Business membership never copies a business credential onto a person's personal profile. Business eligibility or responsibility for a scope must account for applicable credential, jurisdiction, supervision, employment/subcontracting and licensing requirements. Legal and licensing rules vary by trade and jurisdiction; future RH&P eligibility must account for those differences. This is an architectural constraint, not an implemented coverage determination.

No business invitations, crews, business bidding, delegated bids, credential inheritance, or coverage engine are part of RH&P-030.

## Owner-performed work

A future explicit owner-performed work model could represent legitimate DIY or self-performed scopes. It must distinguish this from a marketplace hire, exclude self reviews/reputation and define its own provenance. RH&P-030 does not implement it: public self-view grants no permission to bid on, award, review or manufacture RH&P-verified portfolio history from one's own project.
