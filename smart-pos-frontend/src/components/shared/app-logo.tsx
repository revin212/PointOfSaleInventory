export function AppLogo() {
  return (
    <div className="flex items-center gap-3">
      <div className="flex h-10 w-10 items-center justify-center rounded-xl cta-gradient text-lg font-black text-white">PL</div>
      <div>
        <p className="text-sm font-black tracking-tight text-primary">Precision Ledger</p>
        <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-on-surface-variant">Smart POS</p>
      </div>
    </div>
  );
}
