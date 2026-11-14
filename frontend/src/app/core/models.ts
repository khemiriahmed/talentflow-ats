export type Role = 'CANDIDATE' | 'HR' | 'MANAGER' | 'ADMIN';

export type Stage =
  | 'RECEIVED'
  | 'SCREENING'
  | 'HR_INTERVIEW'
  | 'TECH_INTERVIEW'
  | 'OFFER'
  | 'HIRED'
  | 'REJECTED';

export type ContractType = 'CDI' | 'CDD' | 'FREELANCE' | 'INTERNSHIP' | 'APPRENTICESHIP';

export type JobStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED';

export type InterviewMode = 'ONLINE' | 'ON_SITE';

export type InterviewStatus = 'SCHEDULED' | 'DONE' | 'CANCELLED';

export type Recommendation = 'STRONG_YES' | 'YES' | 'NO' | 'STRONG_NO';

export type Granularity = 'DAY' | 'WEEK' | 'MONTH';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  linkedinUrl: string | null;
  photoUrl: string | null;
  role: Role;
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export interface RegisterPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface UpdateProfilePayload {
  firstName: string;
  lastName: string;
  phone: string | null;
  linkedinUrl: string | null;
  photoUrl: string | null;
}

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

/** Offre telle que la voit le public (aucune information interne). */
export interface PublicJob {
  id: number;
  title: string;
  description: string;
  department: string | null;
  location: string | null;
  contractType: ContractType;
  salaryMin: number | null;
  salaryMax: number | null;
  deadline: string | null;
  createdAt: string;
}

/** Vue interne RH / Manager / Admin. */
export interface JobOffer extends PublicJob {
  status: JobStatus;
  createdById: number;
  managerId: number | null;
}

export interface JobOfferPayload {
  title: string;
  description: string;
  department: string | null;
  location: string | null;
  contractType: ContractType;
  salaryMin: number | null;
  salaryMax: number | null;
  deadline: string | null;
  managerId: number | null;
}

export interface Application {
  id: number;
  jobOfferId: number;
  jobTitle: string;
  candidateId: number;
  candidateName: string;
  candidateEmail: string;
  coverLetter: string | null;
  currentStage: Stage;
  source: string | null;
  createdAt: string;
  tags?: string[];
}

export interface StageHistoryEntry {
  id: number;
  fromStage: Stage | null;
  toStage: Stage;
  reason: string | null;
  changedById: number;
  changedByName: string;
  changedAt: string;
}

export interface Note {
  id: number;
  content: string;
  authorId: number;
  authorName: string;
  createdAt: string;
}

export interface Participant {
  id: number;
  name: string;
  role: Role;
}

export interface Interview {
  id: number;
  applicationId: number;
  jobTitle: string;
  candidateId: number;
  candidateName: string;
  startAt: string;
  durationMin: number;
  mode: InterviewMode;
  locationOrLink: string | null;
  status: InterviewStatus;
  participants: Participant[];
  createdAt: string;
}

export interface InterviewPayload {
  startAt: string;
  durationMin: number;
  mode: InterviewMode;
  locationOrLink: string | null;
  participantIds: number[];
}

export interface Feedback {
  id: number;
  interviewId: number;
  authorId: number;
  authorName: string;
  rating: number;
  strengths: string | null;
  weaknesses: string | null;
  recommendation: Recommendation;
  createdAt: string;
}

export interface FeedbackPayload {
  rating: number;
  strengths: string | null;
  weaknesses: string | null;
  recommendation: Recommendation;
}

export interface Notification {
  id: number;
  type: string;
  message: string;
  referenceType: string | null;
  referenceId: number | null;
  read: boolean;
  createdAt: string;
}

export interface StaffMember {
  id: number;
  firstName: string;
  lastName: string;
  role: Role;
}

export interface AuditLog {
  id: number;
  action: string;
  entityType: string | null;
  entityId: number | null;
  actorId: number | null;
  actorName: string | null;
  createdAt: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}

export interface DashboardSummary {
  totalApplications: number;
  openJobs: number;
  hired: number;
  rejected: number;
  inProgress: number;
}

export interface DashboardStats {
  from: string;
  to: string;
  jobOfferId: number | null;
  granularity: Granularity;
  summary: DashboardSummary;
  currentStages: { stage: Stage; count: number }[];
  funnel: { stage: Stage; count: number; percentOfReceived: number; percentOfPrevious: number | null }[];
  applicationsOverTime: { periodStart: string; count: number }[];
  topJobs: { jobOfferId: number; title: string; count: number }[];
  bySource: { label: string; count: number }[];
  timeToHire: { hiredCount: number; averageDays: number | null; medianDays: number | null };
}
