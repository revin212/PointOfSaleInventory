export function AppLogo() {
  return (
    <div className="flex items-center gap-3">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-highest">
        <img
          src="/pos-icon.png"
          alt=""
          className="h-full w-full object-contain p-0.5"
          width={40}
          height={40}
        />
      </div>
      <div className="min-w-0">
        <p className="text-xs font-black leading-snug tracking-tight text-primary sm:text-sm">
          Point of Sales & Inventory Management
        </p>
      </div>
    </div>
  );
}
