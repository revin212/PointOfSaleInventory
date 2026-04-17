const REFRESH_KEY = "smart-pos.refreshToken";

type Listener = () => void;

let accessToken: string | null = null;
const listeners = new Set<Listener>();

function readRefresh(): string | null {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem(REFRESH_KEY);
  } catch {
    return null;
  }
}

function writeRefresh(token: string | null): void {
  if (typeof window === "undefined") return;
  try {
    if (token === null) {
      window.localStorage.removeItem(REFRESH_KEY);
    } else {
      window.localStorage.setItem(REFRESH_KEY, token);
    }
  } catch {
    /* no-op */
  }
}

export const tokenStore = {
  getAccessToken(): string | null {
    return accessToken;
  },
  setAccessToken(token: string | null): void {
    accessToken = token;
    listeners.forEach((listener) => listener());
  },
  getRefreshToken(): string | null {
    return readRefresh();
  },
  setRefreshToken(token: string | null): void {
    writeRefresh(token);
    listeners.forEach((listener) => listener());
  },
  setTokens(access: string | null, refresh: string | null): void {
    accessToken = access;
    writeRefresh(refresh);
    listeners.forEach((listener) => listener());
  },
  clear(): void {
    accessToken = null;
    writeRefresh(null);
    listeners.forEach((listener) => listener());
  },
  subscribe(listener: Listener): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};

type AuthEvent = "unauthenticated";
const authListeners = new Map<AuthEvent, Set<() => void>>();

export function onAuthEvent(event: AuthEvent, handler: () => void): () => void {
  let set = authListeners.get(event);
  if (!set) {
    set = new Set();
    authListeners.set(event, set);
  }
  set.add(handler);
  return () => set?.delete(handler);
}

export function emitAuthEvent(event: AuthEvent): void {
  authListeners.get(event)?.forEach((handler) => handler());
}
