import type { Person } from '../../api/types';
import type { TeamSuggestion } from './inspirationData';

export interface SuggestedCollaborator { person: Person; suggestion: TeamSuggestion }
const normalized = (value: string) => value.trim().toLowerCase();
const rotationScore = (value: string) => [...value].reduce((score, char) => (score * 31 + char.charCodeAt(0)) >>> 0, 0);

/** Resolve editorial trade needs against live discovery records; never invent identities or qualifications. */
export function selectCollaborators(people: readonly Person[], suggestions: readonly TeamSuggestion[], direction: string): SuggestedCollaborator[] {
  const used = new Set<number>();
  const selected: SuggestedCollaborator[] = [];
  for (const suggestion of suggestions) {
    const fit = (person: Person) => person.specialties.filter(specialty =>
      suggestion.specialtyHints.some(hint => normalized(specialty.name).includes(normalized(hint)))).length;
    const candidates = people.filter(person => person.profile.role === 'TRADESPERSON'
      && person.profile.accountStatus === 'ACTIVE'
      && Number.isSafeInteger(person.profile.id) && person.profile.id > 0
      && !used.has(person.profile.id)
      && (person.profile.availabilityStatus === 'AVAILABLE_NOW' || person.profile.availabilityStatus === 'AVAILABLE_SOON')
      && person.qualifications.some(trade => normalized(trade.name) === normalized(suggestion.trade)));
    candidates.sort((a, b) => fit(b) - fit(a)
      || rotationScore(`${direction}:${a.profile.displayName}`) - rotationScore(`${direction}:${b.profile.displayName}`)
      || a.profile.id - b.profile.id);
    const person = candidates[0];
    if (person) { used.add(person.profile.id); selected.push({ person, suggestion }); }
  }
  return selected;
}
