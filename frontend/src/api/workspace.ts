import { request, personDisplayName } from "./client";
import type { Project, Task, Team, Bid, Profile } from "./types";
export interface ConversationParticipant {
  userId: number;
  displayName: string | null;
  trades: string[];
}
export interface Conversation {
  participants: ConversationParticipant[];
  id: number;
  projectId: number;
  type: string;
  createdAt: string;
}
export interface Message {
  id: number;
  senderId: number;
  senderDisplayName: string | null;
  senderTrades: string[];
  body: string | null;
  createdAt: string;
  removed: boolean;
}
export interface Portfolio {
  id: number;
  title: string;
  description: string;
  provenance: "RHP_VERIFIED" | "EXTERNALLY_VERIFIED" | "SELF_REPORTED";
  completionDate: string | null;
  projectId: number | null;
  taskId: number | null;
  approvedAttachmentIds: number[];
  mediaReference?: string | null;
}
export type ProjectInput = Pick<Project, "title" | "description" | "jobZip">;
const post = <T>(path: string, body?: unknown) =>
  request<T>(path, {
    method: "POST",
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
export const workspaceApi = {
  createReview: (body: {
    tradespersonId: number;
    projectId: number;
    taskId: number;
    level: "TASK";
    overallRating: number;
    body: string;
  }) =>
    post<{ id: number; overallRating: number; body: string }>("/reviews", body),
  projects: (signal?: AbortSignal) =>
    request<Project[]>("/projects", { signal }),
  project: (id: number, signal?: AbortSignal) =>
    request<Project>(`/projects/${id}`, { signal }),
  saveProject: (body: ProjectInput, id?: number) =>
    request<Project>(id ? `/projects/${id}` : "/projects", {
      method: id ? "PATCH" : "POST",
      body: JSON.stringify(body),
    }),
  tasks: (id: number, signal?: AbortSignal) =>
    request<Task[]>(`/projects/${id}/tasks`, { signal }),
  createTask: (
    id: number,
    body: {
      title: string;
      description: string;
      parentTaskId: number | null;
      requiredTradeIds: number[];
    },
  ) => post<Task>(`/projects/${id}/tasks`, body),
  team: (id: number, signal?: AbortSignal) =>
    request<Team[]>(`/projects/${id}/team`, { signal }).then(team => team.map(personDisplayName)),
  bids: (id: number, signal?: AbortSignal) =>
    request<Bid[]>(`/tasks/${id}/bids`, { signal }),
  accept: (id: number) => post<Bid>(`/bids/${id}/accept`),
  assign: (id: number, tradespersonId: number) =>
    post(`/tasks/${id}/assignments`, { tradespersonId }),
  person: (id: number, signal?: AbortSignal) =>
    request<Profile>(`/tradespeople/${id}`, { signal }).then(personDisplayName),
  portfolio: (id: number, signal?: AbortSignal) =>
    request<Portfolio[]>(`/tradespeople/${id}/portfolio`, { signal }),
  conversations: (id: number, signal?: AbortSignal) =>
    request<Conversation[]>(`/projects/${id}/conversations`, { signal }),
  messages: (id: number, signal?: AbortSignal) =>
    request<Message[]>(`/conversations/${id}/messages`, { signal }),
  send: (id: number, body: string) =>
    post<Message>(`/conversations/${id}/messages`, { body }),
};
