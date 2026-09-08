# RH&P-025 portfolio media handoff

RH&P Projects uses portfolio provenance and retains the API's projectId/taskId. Only a supplied projectId creates a project link. External Portfolio retains Externally verified or Self-reported and never links to an inferred project. Account verification is not a source of portfolio provenance.

The portfolio API currently delivers approvedAttachmentIds, not image URLs. No attachment URL is fabricated and no private project gallery is fetched for a public portfolio. PortfolioCard accepts an approved delivered-image array for a future delivery adapter; these images take precedence over presentation media. Multiple external delivered images use the same gallery without project-stage labels.

The three published RH&P project stories and two external works now have local presentation assets registered in `availableStoryAssets`. These are removable demo media, not delivered customer attachments. Missing or failed images are omitted without requesting nonexistent attachment URLs. No media was generated or replaced during the final release-readiness pass.

The following paths are present beneath frontend/public/seed-media/:

- `projects/cedar-park-bathroom-renovation/before.jpg`
- `projects/cedar-park-bathroom-renovation/early-work.jpg`
- `projects/cedar-park-bathroom-renovation/progress.jpg`
- `projects/cedar-park-bathroom-renovation/completed.jpg`
- `projects/cedar-park-oak-floor-restoration/before.jpg`
- `projects/cedar-park-oak-floor-restoration/early-work.jpg`
- `projects/cedar-park-oak-floor-restoration/progress.jpg`
- `projects/cedar-park-oak-floor-restoration/completed.jpg`
- `projects/fishtown-exterior-water-repairs/before.jpg`
- `projects/fishtown-exterior-water-repairs/early-work.jpg`
- `projects/fishtown-exterior-water-repairs/progress.jpg`
- `projects/fishtown-exterior-water-repairs/completed.jpg`

- `portfolios/leah-bennett/germantown-garden-wall-restoration.jpg`
- `portfolios/jordan-ellis/walnut-reading-nook.jpg`

Their paths relative to seed-media are registered in availableStoryAssets in frontend/src/components/seedMedia.ts. Register only assets that actually exist. Partial sets are supported; only available stages appear. The final available stage is the cover, with Completed preferred by the ordered fallback set. No dates or milestones are synthesized.

Bathroom: retain one consistent upstairs room across existing bath condition, shower valve/drain rough-in, shower/wall installation, and completed walk-in shower. Oak floor: retain the same room and boards across damage, board repairs, sanding/sealing, and completed low-sheen floor. Exterior repairs: retain the same brick facade and window trim across water damage, preparation, repointing/trim repair, and completed repairs. External photos must depict the garden wall and walnut shelving/window seat respectively.

Project covers, overview images, progress galleries, and RH&P portfolios share the same project registry. Presentation-media provenance remains identifiable internally; rendered copy follows the approved product-copy audit and does not expose implementation terminology. Portfolio verification labels still come from API provenance, independently of image selection. The native dialog supports Close and Escape, returns focus to its trigger, and exposes selectable thumbnails with aria-pressed. Missing/failed images are omitted; no stage is shown without media.

General image provenance and homepage asset notes: [RH&P-025 media assets](RHP-025-MEDIA-ASSETS.md).
