import type { Account, ApiErrorBody, Assignment, AvailabilityStatus, Bid, HomeownerDashboard, Opportunity, Person, Role, Task, Trade, TradespersonDashboard, WorkSummary } from './types';

export class ApiError extends Error {
  constructor(readonly status: number, message: string, readonly details?: ApiErrorBody) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...options, credentials: 'same-origin', cache: 'no-store',
    headers: { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
  });
  if (!response.ok) {
    const details = await response.json().catch(() => undefined) as ApiErrorBody | undefined;
    throw new ApiError(response.status, details?.message ?? `Request failed (${response.status})`, details);
  }
  return response.json() as Promise<T>;
}
function filters(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => { if (value !== undefined && value !== '') params.set(key, String(value)); });
  const query = params.toString();
  return query ? `?${query}` : '';
}
export const api = {
  account: (signal?: AbortSignal) => request<Account>('/account', { signal }),
  switchProfile: (role: Role) => request<Account>('/account/switch', { method: 'POST', body: JSON.stringify({ role }) }),
  homeownerDashboard: (signal?: AbortSignal) => request<HomeownerDashboard>('/dashboard/homeowner', { signal }),
  tradespersonDashboard: (signal?: AbortSignal) => request<TradespersonDashboard>('/dashboard/tradesperson', { signal }),
  trades: (signal?: AbortSignal) => request<Trade[]>('/catalog/trades', { signal }),
  people: (values: { tradeId?: number; q?: string; availability?: AvailabilityStatus } = {}, signal?: AbortSignal) => request<Person[]>(`/discovery/tradespeople${filters(values)}`, { signal }),
  opportunities: (values: { tradeId?: number; jobZip?: string } = {}, signal?: AbortSignal) => request<Opportunity[]>(`/opportunities${filters(values)}`, { signal }),
  opportunity: (taskTradeId: number, signal?: AbortSignal) => request<Opportunity>(`/opportunities/${taskTradeId}`, { signal }),
  assignments: (signal?: AbortSignal) => request<WorkSummary[]>('/assignments', { signal }),
  taskAssignments: (taskId: number, signal?: AbortSignal) => request<Assignment[]>(`/tasks/${taskId}/assignments`, { signal }),
  taskAction: (taskId: number, action: 'start' | 'ready-for-review' | 'approve' | 'reject') => request<Task>(`/tasks/${taskId}/${action}`, { method: 'POST' }),
  submitBid: (taskId: number, bid: { taskTradeId: number; amount: number; message?: string }) => request<Bid>(`/tasks/${taskId}/bids`, { method: 'POST', body: JSON.stringify(bid) }),
};
