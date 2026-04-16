import type { ReactNode } from "react";

import { AlertTriangle } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

type ConfirmDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  footerNote?: ReactNode;
};

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  loading,
  onConfirm,
  onCancel,
  footerNote,
}: ConfirmDialogProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-on-surface/20 p-4">
      <Card className="glass-panel w-full max-w-md space-y-4 shadow-ambient">
        <div className="flex items-start gap-3">
          <div className="rounded-full bg-error/10 p-2 text-error">
            <AlertTriangle className="h-4 w-4" />
          </div>
          <div className="space-y-1">
            <h3 className="text-lg font-bold text-on-surface">{title}</h3>
            <p className="text-sm text-on-surface-variant">{description}</p>
          </div>
        </div>
        {footerNote}
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </Button>
          <Button variant="destructive" onClick={onConfirm} disabled={loading}>
            {loading ? "Processing..." : confirmLabel}
          </Button>
        </div>
      </Card>
    </div>
  );
}
