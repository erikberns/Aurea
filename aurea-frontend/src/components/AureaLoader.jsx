import Logo from "./Logo";

export default function AureaLoader({ text = "Cargando...", fullScreen = false }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={`flex flex-col items-center justify-center gap-4 text-center ${
        fullScreen ? "min-h-[60vh]" : "py-12"
      }`}
    >
      <div className="relative flex h-20 w-20 items-center justify-center" aria-hidden="true">
        <span className="absolute inset-0 rounded-full border-2 border-primary-fixed-dim/50" />
        <span className="absolute inset-0 animate-spin rounded-full border-2 border-transparent border-r-secondary border-t-primary motion-reduce:animate-none" />
        <Logo variant="dark" className="h-5 w-12" />
      </div>
      <span className="font-label-md text-label-md text-on-surface-variant">{text}</span>
    </div>
  );
}
