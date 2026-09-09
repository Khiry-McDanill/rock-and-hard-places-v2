export type Role = 'HOMEOWNER' | 'TRADESPERSON';
export type AccountStatus = 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED';
export type VerificationStatus = 'NOT_SUBMITTED' | 'PENDING' | 'VERIFIED' | 'REJECTED';
export type AvailabilityStatus = 'AVAILABLE_NOW' | 'AVAILABLE_SOON' | 'BUSY' | 'NOT_ACCEPTING_WORK';
export type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type TaskStatus = ProjectStatus | 'READY_FOR_REVIEW';
export interface Profile {
  id: number; role: Role; displayName: string; profileImageReference: string | null;
  accountStatus: AccountStatus; verificationStatus: VerificationStatus | null;
  baseZip: string | null; serviceRadius: number | null; availabilityStatus: AvailabilityStatus | null;
}
export interface Account { userId: number; email: string; activeRole: Role; profile: Profile; profiles: Profile[] }
export interface Project { id: number; title: string; description: string; status: ProjectStatus; jobZip: string; progressPercentage: number }
export interface RequiredTrade { id: number; tradeId: number; tradeName: string }
export interface Task { id: number; projectId: number; parentTaskId: number | null; title: string; description: string; status: TaskStatus; requiredTrades: RequiredTrade[]; progressPercentage: number }
export interface Assignment { profileImageReference?: string | null; id: number; taskId: number; tradespersonId: number; displayName: string }
export interface Team { profileImageReference?: string | null; id: number; tradespersonId: number; displayName: string; status: 'ACTIVE' | 'INVITED' | 'PENDING' | 'SUSPENDED'; trades: string[] }
export interface Bid { id: number; taskId: number; taskTradeId: number; tradespersonId: number; amount: number; message: string | null; status: 'SUBMITTED' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN'; createdAt: string; updatedAt: string }
export interface Specialty { id: number; name: string }
export interface Trade extends Specialty { specialties: Specialty[] }
export interface Person { profile: Profile; qualifications: Trade[]; specialties: Specialty[] }
export interface Eligibility { allowed: boolean; reason: string | null }
export interface Opportunity { project: Project; task: Task; requiredTrade: RequiredTrade; homeownerDisplayName: string; bidding: Eligibility; ownBids: Bid[] }
export interface NeededTrade { taskId: number; taskTitle: string; requiredTrade: RequiredTrade }
export interface ProjectSummary {
  project: Project; totalTasks: number; completedTasks: number; totalSubtasks: number; completedSubtasks: number;
  team: Team[]; awaitingReview: Task[]; tradesNeeded: NeededTrade[];
  nextAction: 'PROJECT_CLOSED' | 'REVIEW_WORK' | 'PLAN_TASKS' | 'FIND_TRADESPEOPLE' | 'MONITOR_WORK';
}
export interface HomeownerDashboard { projects: ProjectSummary[]; nextAction: 'CREATE_PROJECT' | 'REVIEW_WORK' | 'VIEW_PROJECTS' }
export interface WorkSummary { assignment: Assignment; project: Project; task: Task }
export interface BidSummary { bid: Bid; project: Project | null; task: Task | null }
export interface TradespersonDashboard { profile: Profile; bidding: Eligibility; activeWork: WorkSummary[]; activeBids: BidSummary[]; completedWorkCount: number; completedProjectCount: number; opportunities: Opportunity[] }
export interface ApiErrorBody { status: number; error: string; message: string; path: string; fieldErrors: Record<string, string> }
