import axios, {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { createNonce, hashBody, signRequest } from "@/utils/crypto";
import { showError } from "@/utils/toast";
import { logger } from "@/utils/logger";
import type { AuthStore } from "@/stores/auth";

export const api = axios.create({
  baseURL: "/api/v1",
  timeout: 20000,
});

export function setupApiInterceptors(authStore: AuthStore): void {
  api.interceptors.request.use((config) => {
    const method = (config.method || "GET").toUpperCase();
    const requestPath = normalizePath(config);

    if (!(method === "POST" && requestPath.endsWith("/auth/login"))) {
      const timestamp = String(Math.floor(Date.now() / 1000));
      const nonce = createNonce();
      const bodyHash = hashBody(config.data);
      const secret = authStore.sessionSecret || authStore.token;

      if (secret) {
        const signature = signRequest({
          method,
          path: requestPath,
          timestamp,
          nonce,
          bodyHash,
          secret,
        });

        config.headers.set("X-Timestamp", timestamp);
        config.headers.set("X-Nonce", nonce);
        config.headers.set("X-Signature", signature);
      }
    }

    if (authStore.token) {
      config.headers.set("Authorization", `Bearer ${authStore.token}`);
    }

    logger.debug("API", `${method} ${requestPath}`);
    return config;
  });

  api.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
      const status = error.response?.status;

      if (status === 401) {
        logger.warn("API", "401 received — triggering logout");
        authStore.logout("SESSION_EXPIRED");
      }

      if (status === 429) {
        const retryAfter = error.response?.headers?.["retry-after"];
        const seconds = retryAfter ? parseInt(retryAfter, 10) : 60;
        const validSeconds = isNaN(seconds) || seconds <= 0 ? 60 : seconds;

        logger.warn("API", `Rate limited (429), retry after ${validSeconds}s`);

        let remaining = validSeconds;
        showError(`请求过于频繁，请 ${remaining}s 后重试 / Rate limited, retry in ${remaining}s`);

        const countdown = setInterval(() => {
          remaining--;
          if (remaining <= 0) {
            clearInterval(countdown);
          }
        }, 1000);
      }

      return Promise.reject(error);
    },
  );
}

export async function unwrap(
  promise: Promise<AxiosResponse<any>>,
): Promise<any> {
  const response = await promise;
  if (
    response.data &&
    typeof response.data === "object" &&
    "data" in response.data
  ) {
    return response.data.data;
  }
  return response.data;
}

function normalizePath(config: InternalAxiosRequestConfig): string {
  const raw = config.url || "";

  try {
    if (raw.startsWith("http://") || raw.startsWith("https://")) {
      const parsed = new URL(raw);
      return `${parsed.pathname}${parsed.search}`;
    }

    const basePath = new URL(
      config.baseURL || "",
      window.location.origin,
    ).pathname.replace(/\/$/, "");
    const normalizedRaw = raw.startsWith("/") ? raw : `/${raw}`;
    const merged = new URL(
      `${basePath}${normalizedRaw}`,
      window.location.origin,
    );
    return `${merged.pathname}${merged.search}`;
  } catch {
    return raw.startsWith("/") ? raw : `/${raw}`;
  }
}
