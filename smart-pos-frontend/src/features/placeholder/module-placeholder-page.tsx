import { Link } from "react-router-dom";

import { EmptyBlock, SuccessBlock } from "@/components/shared/state-blocks";

type ModulePlaceholderPageProps = {
  moduleName: string;
};

export function ModulePlaceholderPage({ moduleName }: ModulePlaceholderPageProps) {
  return (
    <div className="space-y-4">
      <SuccessBlock
        title={`${moduleName} foundation ready`}
        description="This module page will be implemented in planned order after login and dashboard."
      />
      <EmptyBlock
        title="Module not built yet"
        description="Only v1 planned modules will be implemented incrementally."
        action={
          <Link
            to="/dashboard"
            className="inline-flex h-10 items-center justify-center rounded-xl bg-surface-container-low px-4 text-sm font-semibold text-on-surface transition hover:bg-surface-container-highest"
          >
            Back to dashboard
          </Link>
        }
      />
    </div>
  );
}
