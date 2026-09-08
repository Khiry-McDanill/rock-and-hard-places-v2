# Frontend presentation imagery

`build-inspiration.png` is an original AI-generated brand asset created with the built-in image generation tool. It is not an uploaded project photo or a seeded business record. `src/components/ProjectImage.tsx` prioritizes delivered media. `src/components/projectPresentation.ts` selects local presentation assets from broad title/description context, never IDs. Alt text describes the image subject; implementation provenance is documented here rather than in rendered product copy. A future media integration can supply a usable delivered URL and alt text; opaque storage references must not be passed as URLs.

Generation prompt:

> Use case: photorealistic-natural. Create one landscape 3:2 architectural photograph-style brand presentation asset for Rock & Hard Places, a craftsmanship and creative-building app. A modest beautifully crafted timber workshop/barn with charcoal standing-seam pitched roof, warm wood siding, a large glazed entrance, subtle warm interior light, gravel approach and sage green planting, surrounded by trees in soft late-afternoon natural daylight. Eye-level three-quarter view with the entire building centered and usable in a small square crop. Authentic grounded materials, quiet premium photography, crisp believable construction details. No people, no text, no logos, no watermarks, no collage, no UI. This is a generic inspirational brand image, not a depiction of an actual customer's project.

The reference board is design documentation only and is never loaded by the runtime UI. The roof/mountain logo is shared SVG geometry in `src/components/Brand.tsx`, refined against that approved reference.

The homepage barn carousel loads `barn-before.jpg` and `barn-after.jpg` as separate full images. The unused `barn-before-after.png` source composite was removed during final cleanup. These provenance notes live in docs so they are not published with frontend static assets.


## Contextual fallback assets

- `kitchen-remodel.png`: kitchen display context, unless the title identifies outdoor, vehicle, bathroom, or another explicitly different context.
- `build-inspiration.png`: barn/workshop context.
- `construction-detail.png`: neutral tools/materials for all other contexts, including categories without a dedicated asset. This avoids implying an unrelated property. Title context takes precedence over description context.

These categories are visual hints only, not business classifications. All three are local frontend presentation assets. No API records or IDs are created or modified by image selection.

The two additional images were created with the built-in image generation tool using these exact prompts:

### Kitchen

> Create a photorealistic landscape 3:2 presentation image for a craftsmanship renovation dashboard. A compact rowhouse kitchen during a careful remodel: warm oak lower cabinets, cream walls, simple white stone countertop, tiled backsplash, original wood flooring, daylight through a rear window, a neatly placed carpenter's tape and folded protective canvas on the counter, a small unfinished cabinet detail showing work in progress. Eye-level architectural interior photography, natural grounded premium quality, entire kitchen clearly recognizable in a square crop. No people, no lettering, no logos, no watermark, no UI. Generic kitchen renovation concept, not any actual customer's project.

### Neutral construction

> Create a photorealistic landscape 3:2 neutral construction presentation image for a premium grounded craftsmanship dashboard. Close view of a clean workbench with natural timber offcuts, carpenter's square, tape measure, pencil, and folded cream construction drawings with no readable writing. Soft daylight, warm oak, charcoal metal tools, subtle sage background, carefully used authentic materials. Tight still-life composition, no identifiable room, no building exterior, no people, no logos, no text, no watermark, no UI. Must be appropriate as a generic fallback for any kind of building or conversion project without implying a specific property. Photography, not an illustration.
