const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1") as string;
const rawUseMocks = (import.meta.env.VITE_USE_MOCKS ?? "false") as string;

export const API_BASE_URL = rawBaseUrl.replace(/\/$/, "");
export const USE_MOCKS = rawUseMocks === "true" || rawUseMocks === "1";
