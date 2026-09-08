/** Removable RH&P-023 presentation registry. No IDs, business state or demographic fields.
 * Delete the registry entries and seed-media directory when real media is delivered.
 * Name matching is a temporary fallback for API projections without storage references.
 */
export const seedPeople = [
  'Jordan Ellis', 'Maya Patel', 'Andre Brooks', 'Elena Rivera', 'Ruth Chen',
  'Caleb Morgan', 'Nina Alvarez', 'Marcus Reed', 'Sofia Nguyen', 'Darius Cole',
  'Leah Bennett', 'Owen Price',
] as const;
const slug = (value: string) => value.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-');
export interface PersonMediaContext { name: string; reference?: string | null; url?: string | null }
/** Only explicit delivered URLs are usable; opaque storage keys are never requested. */
export function deliveredMediaUrl(value?: string | null) {
  return value && (/^https?:\/\//.test(value) || /^\/(?!\/)/.test(value)) ? value : undefined;
}
export function seedPortrait({ name, reference }: PersonMediaContext) {
  const person = seedPeople.find((person) => reference
    ? reference === `demo/rhp-023/profiles/${slug(person)}.jpg`
    : person.toLowerCase() === name.trim().toLowerCase());
  return person ? `/seed-media/people/${slug(person)}.jpg` : undefined;
}
export function resolvePortrait(context: PersonMediaContext) {
  return deliveredMediaUrl(context.url) ?? deliveredMediaUrl(context.reference) ?? seedPortrait(context);
}
export interface ProjectDisplayContext { title?: string; description?: string }
export type WorkStage = 'Before' | 'Early work' | 'Progress' | 'Completed';
export interface ProjectMediaFrame { url: string; alt: string; caption?: string; stage?: WorkStage }
export interface WorkMedia {
  source: 'delivered' | 'presentation';
  frames: readonly ProjectMediaFrame[];
  cover?: ProjectMediaFrame;
}
// Register files here only after they have been delivered to public/seed-media.
export const availableStoryAssets: readonly string[] = [
  'projects/cedar-park-bathroom-renovation/before.jpg',
  'projects/cedar-park-bathroom-renovation/early-work.jpg',
  'projects/cedar-park-bathroom-renovation/progress.jpg',
  'projects/cedar-park-bathroom-renovation/completed.jpg',
  'projects/cedar-park-oak-floor-restoration/before.jpg',
  'projects/cedar-park-oak-floor-restoration/early-work.jpg',
  'projects/cedar-park-oak-floor-restoration/progress.jpg',
  'projects/cedar-park-oak-floor-restoration/completed.jpg',
  'projects/fishtown-exterior-water-repairs/before.jpg',
  'projects/fishtown-exterior-water-repairs/early-work.jpg',
  'projects/fishtown-exterior-water-repairs/progress.jpg',
  'projects/fishtown-exterior-water-repairs/completed.jpg',
  'portfolios/leah-bennett/germantown-garden-wall-restoration.jpg',
  'portfolios/jordan-ellis/walnut-reading-nook.jpg',
];
export const storyStages = [
  ['before.jpg', 'Before'], ['early-work.jpg', 'Early work'],
  ['progress.jpg', 'Progress'], ['completed.jpg', 'Completed'],
] as const;
export const seedProjectStories = [
  'Cedar Park bathroom renovation',
  'Cedar Park oak floor restoration',
  'Fishtown exterior water repairs',
] as const;
function projectStory(title: string): WorkMedia {
  const frames = storyStages.flatMap(([filename, stage]) => {
    const file = `projects/${slug(title)}/${filename}`;
    return availableStoryAssets.includes(file)
      ? [{ ...frame(file, `${title}: ${stage}`, stage), stage }] : [];
  });
  return { source: 'presentation', frames, cover: frames.at(-1) };
}
const frame = (file: string, subject: string, caption: string): ProjectMediaFrame => ({
  url: `/seed-media/${file}`, alt: subject, caption,
});
export const seedProjects = [
  'Passyunk kitchen remodel', 'Cedar Park bathroom renovation', 'Fishtown cedar deck',
  'Mount Airy attic framing', 'Fairmount plaster and drywall repairs',
  'Cedar Park oak floor restoration', 'Fairmount utility room coordination',
  'Fishtown exterior water repairs', 'Mount Airy side porch replacement',
] as const;
export function seedProjectMedia(context: ProjectDisplayContext = {}): WorkMedia {
  // Title takes precedence so an RV/outdoor kitchen never looks residential.
  const subject = context.title?.trim() || context.description?.trim() || "";
  const title = subject || "construction tools and materials";
  const story = seedProjectStories.find((item) => item.toLowerCase() === subject.toLowerCase());
  if (story) return projectStory(story);
  // No unrelated materials image for bathroom work when its photos are missing.
  if (/\bbathroom\b/i.test(subject)) return { source: 'presentation', frames: [] };
  const nonResidential = /\b(outdoor|patio|bathroom|rv|bus|skoolie|tiny|container)\b/i.test(subject);
  if (!nonResidential && /\bkitchen\b/i.test(subject)) {
    const cover = frame('kitchen-current.png', title, 'Progress');
    const early = frame('kitchen-preparation.png', title, 'Early work');
    const detail = frame('kitchen-finish.png', title, 'Detail');
    return { source: 'presentation' as const, frames: [early, cover, detail], cover };
  }
  const generic = !nonResidential && /\b(barn|workshop)\b/i.test(subject)
    ? frame('barn-current.png', title, 'Progress')
    : frame('construction-detail.png', 'construction tools and materials', 'Materials & planning');
  return { source: 'presentation' as const, frames: [generic], cover: generic };
}
export function resolveProjectMedia(context: ProjectDisplayContext = {}, delivered: readonly ProjectMediaFrame[] = []) {
  return delivered.length ? { source: 'delivered' as const, frames: delivered, cover: delivered[0] } : seedProjectMedia(context);
}
export const seedPortfolios = [
  ['Nina Alvarez', 'Cedar Park bathroom renovation'],
  ['Darius Cole', 'Cedar Park oak floor restoration'],
  ['Leah Bennett', 'Fishtown exterior water repairs'],
  ['Leah Bennett', 'Germantown garden wall restoration'],
  ['Jordan Ellis', 'Walnut reading nook'],
] as const;
/** RH&P work shares the project registry; external work never acquires project media by title. */
export function resolvePortfolioMedia(
  name: string,
  item: { title: string; provenance: string; mediaReference?: string | null },
  delivered: readonly ProjectMediaFrame[] = [],
): WorkMedia {
  const actual = delivered.filter((image) => deliveredMediaUrl(image.url));
  if (actual.length) return {
    source: 'delivered', frames: actual,
    cover: actual.find((image) => image.stage === 'Completed') ?? actual.at(-1),
  };
  const reference = deliveredMediaUrl(item.mediaReference);
  if (reference && item.provenance !== 'RHP_VERIFIED') {
    const image = { url: reference, alt: item.title, caption: 'Work photo' };
    return { source: 'delivered', frames: [image], cover: image };
  }
  if (item.provenance === 'RHP_VERIFIED') {
    // Only a coherent, registered project story can represent platform work.
    const known = seedProjectStories.find((title) => title === item.title);
    return known ? seedProjectMedia({ title: known }) : { source: 'presentation', frames: [] };
  }
  const known = seedPortfolios.find(([person, title]) => person === name && title === item.title);
  const file = known ? `portfolios/${slug(name)}/${slug(item.title)}.jpg` : '';
  const frames = availableStoryAssets.includes(file) ? [frame(file, item.title, 'Work photo')] : [];
  return { source: 'presentation', frames, cover: frames[0] };
}
