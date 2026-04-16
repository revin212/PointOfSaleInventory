import type { ReactNode } from "react";

import { AlertCircle, Inbox, LoaderCircle } from "lucide-react";

import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

type StateBlockProps = {
  title: string;
  description: string;
  action?: ReactNode;
};

export function LoadingBlock({ title, description }: Omit<StateBlockProps, "action">) {
  return (
    <Card className="flex flex-col items-center justify-center gap-3 py-12 text-center">
      <LoaderCircle className="h-8 w-8 animate-spin text-primary" />
      <h3 className="text-base font-bold text-on-surface">{title}</h3>
      <p className="max-w-sm text-sm text-on-surface-variant">{description}</p>
    </Card>
  );
}

export function EmptyBlock({ title, description, action }: StateBlockProps) {
  return (
    <Card className="flex flex-col items-center justify-center gap-3 py-12 text-center">
      <Inbox className="h-8 w-8 text-on-surface-variant" />
      <h3 className="text-base font-bold text-on-surface">{title}</h3>
      <p className="max-w-sm text-sm text-on-surface-variant">{description}</p>
      {action}
    </Card>
  );
}

type ErrorBlockProps = StateBlockProps & {
  retryLabel?: string;
  onRetry?: () => void;
};

export function ErrorBlock({ title, description, action, retryLabel = "Retry", onRetry }: ErrorBlockProps) {
  return (
    <Card className="space-y-4 py-8">
      <Alert tone="error" className="flex items-start gap-2">
        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
        <div className="space-y-1">
          <p className="font-semibold">{title}</p>
          <p>{description}</p>
        </div>
      </Alert>
      <div className="flex flex-wrap items-center gap-2">
        {onRetry ? (
          <Button variant="secondary" onClick={onRetry}>
            {retryLabel}
          </Button>
        ) : null}
        {action}
      </div>
    </Card>
  );
}

type SuccessBlockProps = {
  title: string;
  description: string;
};

export function SuccessBlock({ title, description }: SuccessBlockProps) {
  return (
    <Alert className="border-primary/30 bg-primary/10 text-primary">
      <p className="font-semibold">{title}</p>
      <p>{description}</p>
    </Alert>
  );
}
