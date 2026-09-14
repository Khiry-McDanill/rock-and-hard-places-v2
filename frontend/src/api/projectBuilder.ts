import { request } from "./client";
import type { ProjectDraft } from "../features/manualProjectForm";

export interface Recommendation {
  trade: string;
  reason: string;
  confidence: "HIGH" | "MEDIUM" | "LOW";
  needsConfirmation: boolean;
}
export interface SuggestedTask extends Recommendation {
  title: string;
  description: string;
}
export interface ProjectPlan {
  summary: string;
  followUpQuestions: string[];
  suggestedTrades: Recommendation[];
  tasks: SuggestedTask[];
  assumptions: string[];
  warnings: string[];
}
export interface PlanResponse {
  plan: ProjectPlan;
  recognizedTrades: {
    suggestion: string;
    tradeId: number;
    tradeName: string;
  }[];
  unresolvedTrades: string[];
}
export interface Answer {
  question: string;
  answer: string;
}

// Phase 1 accepts plain text. Keep the original idea and every answer together
// within that existing contract; no new endpoint or persistent draft is needed.
export function planningContext(
  idea: string,
  answers: Answer[],
  draft?: ProjectDraft,
) {
  const context = [
    draft
      ? `Review this homeowner's manual draft for missing trades, tasks and useful questions. Suggestions are optional.\nProject: ${draft.title}\nZIP: ${draft.jobZip}\nDetails: ${draft.description}`
      : idea,
    ...answers
      .filter((a) => a.answer.trim())
      .map((a) => `Question: ${a.question}\nHomeowner answer: ${a.answer}`),
  ].join("\n\n");
  if (context.length > 8000) throw new Error("CONTEXT_TOO_LONG");
  return context;
}
export async function planProject(
  idea: string,
  answers: Answer[],
  draft?: ProjectDraft,
): Promise<PlanResponse> {
  const context = planningContext(idea, answers, draft);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 45000);
  try {
    return await request<PlanResponse>("/project-builder/plan", {
      method: "POST",
      body: JSON.stringify({ idea: context }),
      signal: controller.signal,
    });
  } finally {
    clearTimeout(timer);
  }
}
