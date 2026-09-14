import type { Recommendation } from "../api/projectBuilder";
import type { Trade } from "../api/types";

export type ReviewItem = Omit<
  Recommendation,
  "reason" | "confidence" | "needsConfirmation"
> & {
  title?: string;
  description?: string;
  reason?: string;
  confidence?: Recommendation["confidence"];
  needsConfirmation?: boolean;
  choice: "pending" | "kept" | "removed";
  editing?: boolean;
  origin: "suggestion" | "edited" | "added";
  sourceKey?: string;
};

// Match Phase 1's exact normalized catalog lookup. Never choose an ambiguous match.
export const normalizeTrade = (name: string) =>
  name.trim().replace(/\s+/g, " ").toLowerCase();
export function catalogTrade(name: string, catalog: Trade[] = []) {
  const matches = catalog.filter(
    (t) => normalizeTrade(t.name) === normalizeTrade(name),
  );
  return matches.length === 1 ? matches[0] : undefined;
}
const sourceKey = (item: { title?: string; trade: string }) =>
  JSON.stringify([
    item.title === undefined ? null : normalizeTrade(item.title),
    normalizeTrade(item.trade),
  ]);

export function reviseItem(
  item: ReviewItem,
  changes: Partial<ReviewItem>,
): ReviewItem {
  // Even a small title/scope change invalidates the old assessment; do not guess semantics.
  // Catalog-equivalent spelling changes alone do not change the assessed trade.
  const edited =
    (changes.title !== undefined && changes.title !== item.title) ||
    (changes.description !== undefined &&
      changes.description !== item.description) ||
    (changes.trade !== undefined &&
      normalizeTrade(changes.trade) !== normalizeTrade(item.trade));
  return {
    ...item,
    ...changes,
    ...(edited
      ? ({
          origin: item.origin === "added" ? "added" : "edited",
          reason: undefined,
          confidence: undefined,
          needsConfirmation: undefined,
        } as const)
      : {}),
  };
}

export function reviewSuggestions(
  previous: ReviewItem[],
  incoming: (Recommendation & { title?: string; description?: string })[],
): ReviewItem[] {
  // Retain decisions, edited scope, and authored additions across retries. Original
  // source keys also suppress a reissued suggestion whose visible title/trade was edited.
  const retained = previous.filter(
    (item) => item.choice !== "pending" || item.origin !== "suggestion",
  );
  const protectedKeys = new Set(
    retained.flatMap((item) => [item.sourceKey, sourceKey(item)]),
  );
  return [
    ...retained,
    ...incoming
      .filter((item) => !protectedKeys.has(sourceKey(item)))
      .map((item) => ({
        ...item,
        choice: "pending" as const,
        origin: "suggestion" as const,
        sourceKey: sourceKey(item),
      })),
  ];
}
