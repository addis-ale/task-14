export interface ScopeDto {
  campusIds?: number[];
  gradeIds?: number[];
  classIds?: number[];
  subjectIds?: number[];
}

/** Fine-grained action permissions returned by the server per-role */
export type ActionPermission =
  | "view"
  | "enter"
  | "import"
  | "review"
  | "publish"
  | "create"
  | "update"
  | "delete"
  | "export"
  | "assign";

export interface UserProfile {
  id: number;
  username: string;
  roles: string[];
  activeRole: string;
  scopes?: ScopeDto;
  permissions?: ActionPermission[];
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
