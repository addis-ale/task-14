import { ref } from 'vue';

export interface ToastItem {
  id: number;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  duration: number;
}

let nextId = 1;
export const toasts = ref<ToastItem[]>([]);

export function showToast(
  message: string,
  type: ToastItem['type'] = 'info',
  duration = 3000,
) {
  const id = nextId++;
  toasts.value.push({ id, message, type, duration });
  setTimeout(() => {
    toasts.value = toasts.value.filter((t) => t.id !== id);
  }, duration);
}

export function showSuccess(message: string) {
  showToast(message, 'success');
}

export function showError(message: string) {
  showToast(message, 'error', 5000);
}

const ERROR_MAP: Record<string, string> = {
  VALIDATION_ERROR: '输入数据验证失败 Validation failed',
  UNAUTHORIZED: '未授权 Unauthorized',
  FORBIDDEN: '权限不足 Forbidden',
  NOT_FOUND: '资源不存在 Not found',
  CONFLICT: '数据冲突 Conflict',
  RATE_LIMITED: '请求过频 Rate limited',
  SERVER_ERROR: '服务器错误 Server error',
};

export function handleApiError(err: unknown) {
  const error = err as {
    response?: { status?: number; data?: { code?: string; message?: string } };
    message?: string;
  };

  if (!error.response) {
    showError(error.message || '网络连接失败 Network error');
    return;
  }

  const code = error.response.data?.code || '';
  const mapped = ERROR_MAP[code];
  if (mapped) {
    showError(mapped);
    return;
  }

  const msg = error.response.data?.message || `Error ${error.response.status}`;
  showError(msg);
}
