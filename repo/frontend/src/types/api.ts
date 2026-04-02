export interface ErrorDetail {
  field?: string;
  message: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
  errors?: ErrorDetail[];
}

export interface PaginationMeta {
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface PageData<T> {
  items: T[];
  pagination: PaginationMeta;
}
