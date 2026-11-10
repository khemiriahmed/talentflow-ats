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

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  role: Role;
  enabled: boolean;
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
  summary: DashboardSummary;
}
