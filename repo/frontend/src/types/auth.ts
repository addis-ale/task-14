export interface ScopeDto {
  campusIds?: number[];
  gradeIds?: number[];
  classIds?: number[];
  subjectIds?: number[];
}

export interface UserProfile {
  id: number;
  username: string;
  roles: string[];
  activeRole: string;
  scopes?: ScopeDto;
}

export interface LoginPayload {
  token: string;
  expiresIn: number;
  user: UserProfile;
  sessionSecret?: string;
  drafts?: Array<{ formKey: string; updatedAt: string }>;
}

export interface SwitchRolePayload {
  activeRole: string;
  scopes?: ScopeDto;
}
