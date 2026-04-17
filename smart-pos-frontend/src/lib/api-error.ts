export type ApiErrorDetail = {
  field?: string;
  message?: string;
};

export type ApiErrorEnvelope = {
  timestamp?: string;
  status?: number;
  code?: string;
  message?: string;
  path?: string;
  details?: ApiErrorDetail[];
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: ApiErrorDetail[];

  constructor(status: number, code: string, message: string, details: ApiErrorDetail[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.details = details;
  }

  static fromEnvelope(status: number, envelope: ApiErrorEnvelope | null, fallbackMessage: string): ApiError {
    const code = envelope?.code ?? "UNKNOWN_ERROR";
    const message = envelope?.message ?? fallbackMessage;
    const details = envelope?.details ?? [];
    return new ApiError(envelope?.status ?? status, code, message, details);
  }
}
