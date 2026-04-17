import { API_BASE_URL } from "@/lib/env";
import { ApiError, type ApiErrorEnvelope } from "@/lib/api-error";
import { emitAuthEvent, tokenStore } from "@/lib/token-store";

export type QueryValue = string | number | boolean | null | undefined;
export type QueryParams = Record<string, QueryValue>;

export type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  query?: QueryParams;
  auth?: boolean;
  skipRefresh?: boolean;
};

function buildUrl(path: string, query?: QueryParams): string {
  const base = API_BASE_URL.replace(/\/$/, "");
  const cleanPath = path.startsWith("/") ? path : `/${path}`;
  const url = `${base}${cleanPath}`;
  if (!query) return url;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    params.append(key, String(value));
  });
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

async function parseEnvelope(response: Response): Promise<ApiErrorEnvelope | null> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as ApiErrorEnvelope;
  } catch {
    return { message: text };
  }
}

async function parseJson<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!text) return undefined as unknown as T;
  return JSON.parse(text) as T;
}

let refreshInFlight: Promise<boolean> | null = null;

async function refreshTokens(): Promise<boolean> {
  if (refreshInFlight) return refreshInFlight;
  const refreshToken = tokenStore.getRefreshToken();
  if (!refreshToken) return false;

  refreshInFlight = (async () => {
    try {
      const response = await fetch(buildUrl("/auth/refresh"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });
      if (!response.ok) {
        tokenStore.clear();
        return false;
      }
      const payload = (await response.json()) as {
        accessToken?: string;
        refreshToken?: string;
      };
      if (!payload.accessToken || !payload.refreshToken) {
        tokenStore.clear();
        return false;
      }
      tokenStore.setTokens(payload.accessToken, payload.refreshToken);
      return true;
    } catch {
      tokenStore.clear();
      return false;
    } finally {
      refreshInFlight = null;
    }
  })();

  return refreshInFlight;
}

async function sendRequest<T>(path: string, options: RequestOptions): Promise<T> {
  const method = options.method ?? "GET";
  const useAuth = options.auth ?? true;

  const headers: Record<string, string> = { Accept: "application/json" };
  let body: BodyInit | undefined;
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }
  if (useAuth) {
    const accessToken = tokenStore.getAccessToken();
    if (accessToken) headers["Authorization"] = `Bearer ${accessToken}`;
  }

  const response = await fetch(buildUrl(path, options.query), {
    method,
    headers,
    body,
  });

  if (response.ok) {
    if (response.status === 204) return undefined as unknown as T;
    return parseJson<T>(response);
  }

  if (response.status === 401 && useAuth && !options.skipRefresh) {
    const refreshed = await refreshTokens();
    if (refreshed) {
      return sendRequest<T>(path, { ...options, skipRefresh: true });
    }
    emitAuthEvent("unauthenticated");
  }

  const envelope = await parseEnvelope(response);
  throw ApiError.fromEnvelope(response.status, envelope, response.statusText || "Request failed");
}

export function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return sendRequest<T>(path, options);
}

export const api = {
  get<T>(path: string, options: Omit<RequestOptions, "method" | "body"> = {}): Promise<T> {
    return sendRequest<T>(path, { ...options, method: "GET" });
  },
  post<T>(path: string, body?: unknown, options: Omit<RequestOptions, "method" | "body"> = {}): Promise<T> {
    return sendRequest<T>(path, { ...options, method: "POST", body });
  },
  put<T>(path: string, body?: unknown, options: Omit<RequestOptions, "method" | "body"> = {}): Promise<T> {
    return sendRequest<T>(path, { ...options, method: "PUT", body });
  },
  patch<T>(path: string, body?: unknown, options: Omit<RequestOptions, "method" | "body"> = {}): Promise<T> {
    return sendRequest<T>(path, { ...options, method: "PATCH", body });
  },
  delete<T>(path: string, options: Omit<RequestOptions, "method" | "body"> = {}): Promise<T> {
    return sendRequest<T>(path, { ...options, method: "DELETE" });
  },
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
