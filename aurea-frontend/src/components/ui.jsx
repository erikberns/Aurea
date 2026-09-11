export function FormField({ label, error, children }) {
  return (
    <label className="block">
      <span className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wide">
        {label}
      </span>
      <div className="mt-1.5">{children}</div>
      {error && <span className="block mt-1 font-body-sm text-body-sm text-error">{error}</span>}
    </label>
  );
}

export function TextInput(props) {
  return (
    <input
      {...props}
      className={
        "w-full h-12 bg-surface-container-lowest border border-outline-variant/70 rounded px-3 font-body-md text-body-md text-on-surface placeholder:text-outline/70 focus:outline-none focus:border-primary focus:ring-2 focus:ring-tertiary/20 transition-colors " +
        (props.className || "")
      }
    />
  );
}

export function PrimaryButton({ children, className = "", ...props }) {
  return (
    <button
      {...props}
      className={
        "w-full h-12 bg-primary-container text-on-primary font-label-md text-label-md uppercase tracking-wider rounded transition-transform active:scale-[0.99] hover:bg-secondary disabled:opacity-50 disabled:cursor-not-allowed " +
        className
      }
    >
      {children}
    </button>
  );
}

export function SecondaryButton({ children, className = "", ...props }) {
  return (
    <button
      {...props}
      className={
        "w-full h-12 border border-primary-container text-secondary font-label-md text-label-md uppercase tracking-wider rounded bg-transparent hover:bg-primary-container/10 transition-colors " +
        className
      }
    >
      {children}
    </button>
  );
}

export function Banner({ tone = "error", children }) {
  const tones = {
    error: "bg-error-container text-on-error-container border-error/30",
    success: "bg-secondary-container/60 text-on-secondary-container border-secondary/30",
  };
  return (
    <div className={`border rounded px-4 py-3 font-body-sm text-body-sm ${tones[tone]}`}>
      {children}
    </div>
  );
}
