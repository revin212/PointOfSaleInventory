import type { ReactNode } from "react";

import { X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

type ModalProps = {
  open: boolean;
  title: string;
  children: ReactNode;
  onClose: () => void;
};

export function Modal({ open, title, children, onClose }: ModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-on-surface/25 p-4 backdrop-blur-sm">
      <div className="flex min-h-full items-start justify-center py-6">
        <Card className="glass-panel flex w-full max-w-2xl flex-col shadow-popover">
          <div className="flex items-start justify-between gap-3 border-b border-outline-variant/20 pb-3">
            <div className="space-y-1">
              <h3 className="text-lg font-bold text-on-surface">{title}</h3>
            </div>
            <Button type="button" variant="ghost" size="icon" onClick={onClose} aria-label="Close modal">
              <X className="h-4 w-4" />
            </Button>
          </div>

          <div className="min-h-0 flex-1 overflow-auto pt-4 pr-1">
            {children}
          </div>
        </Card>
      </div>
    </div>
  );
}

