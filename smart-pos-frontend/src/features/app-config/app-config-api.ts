import { api } from "@/lib/api-client";

export type AppConfig = {
  taxEnabled: boolean;
  vatRate: number;
  taxMode: string;
};

export async function getAppConfig(): Promise<AppConfig> {
  const raw = await api.get<AppConfig>("/app-config");
  return {
    taxEnabled: Boolean(raw.taxEnabled),
    vatRate: Number(raw.vatRate ?? 0),
    taxMode: String(raw.taxMode ?? "EXCLUSIVE"),
  };
}

