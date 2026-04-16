import { Layers, PackageSearch, Wallet } from "lucide-react";

import { FilterToolbar } from "@/components/shared/filter-toolbar";
import { KpiCard } from "@/components/shared/kpi-card";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { StatusBadge, StockBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { formatIDR } from "@/lib/format";

export function FoundationPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Foundation Layer"
        subtitle="Theme, shell, and shared interaction blocks are ready for module pages."
        actions={<Button>Primary Action</Button>}
      />

      <div className="grid gap-4 md:grid-cols-3">
        <KpiCard label="Total Sales" value={formatIDR(12500000)} trend="+12.5% today" icon={Wallet} />
        <KpiCard label="Low Stock Items" value="8 Items" icon={PackageSearch} />
        <KpiCard label="Shared Components" value="12 Blocks" icon={Layers} />
      </div>

      <FilterToolbar
        searchPlaceholder="Search foundation docs..."
        filters={
          <>
            <StatusBadge tone="success" label="Success" />
            <StatusBadge tone="warning" label="Warning" />
            <StatusBadge tone="error" label="Error" />
            <StockBadge stockTone="in-stock" />
            <StockBadge stockTone="low" />
            <StockBadge stockTone="out" />
          </>
        }
      />

      <Card className="space-y-4">
        <SuccessBlock title="Success state" description="Shared components can now be reused for all v1 modules." />
        <EmptyBlock
          title="Empty state sample"
          description="Use this when tables or lists return zero rows."
          action={<Button variant="secondary">Create Item</Button>}
        />
        <ErrorBlock
          title="Error state sample"
          description="Use this for API failures with retry action."
          onRetry={() => null}
        />
        <LoadingBlock title="Loading state sample" description="Use this while waiting for initial server response." />
      </Card>
    </div>
  );
}
