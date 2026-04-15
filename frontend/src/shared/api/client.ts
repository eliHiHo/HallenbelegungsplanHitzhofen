// Central API client
// Backend uses cookie-based sessions (HB_SESSION, HTTPOnly).
// All requests must include credentials so the browser sends the session cookie.

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly body: unknown = null
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function parseResponse<T>(res: Response): Promise<T> {
  if (res.status === 204 || res.headers.get("content-length") === "0") {
    return undefined as unknown as T;
  }
  const text = await res.text();
  if (!text) return undefined as unknown as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    return text as unknown as T;
  }
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown
): Promise<T> {
  const init: RequestInit = {
    method,
    credentials: "include", // required for HB_SESSION cookie
    headers: {
      "Content-Type": "application/json",
    },
  };
  if (body !== undefined) {
    init.body = JSON.stringify(body);
  }

  const res = await fetch(`${BASE_URL}${path}`, init);

  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    let errorBody: unknown = null;
    try {
      errorBody = await res.json();
      if (
        errorBody &&
        typeof errorBody === "object" &&
        "message" in errorBody
      ) {
        message = (errorBody as { message: string }).message;
      }
    } catch {
      // ignore parse error
    }
    throw new ApiError(res.status, message, errorBody);
  }

  return parseResponse<T>(res);
}

export const api = {
  get: <T>(path: string) => request<T>("GET", path),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, body),
  put: <T>(path: string, body?: unknown) => request<T>("PUT", path, body),
  delete: <T>(path: string) => request<T>("DELETE", path),
};
