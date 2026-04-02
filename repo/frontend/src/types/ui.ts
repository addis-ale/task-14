export interface TableColumn {
  key: string;
  label: string;
  sortable?: boolean;
  maskPii?: boolean;
  width?: string;
}

export interface SelectOption {
  label: string;
  value: string | number;
}

export type RoleName =
  | "ADMIN"
  | "ACADEMIC_AFFAIRS"
  | "HOMEROOM_TEACHER"
  | "SUBJECT_TEACHER"
  | "STUDENT";
