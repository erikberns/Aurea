import logoDark from "../assets/logo-dark.png";
import logoGold from "../assets/logo-gold.png";
import logoPrimary from "../assets/logo-primary.png";
import logoLight from "../assets/logo-light.png";

export default function Logo({
  variant = "dark",
  className = "h-8 md:h-9 w-auto",
  alt = "Áurea Joyas",
}) {
  const logos = {
    dark: logoDark,
    gold: logoGold,
    primary: logoPrimary,
    light: logoLight,
  };

  const selectedLogo = logos[variant] || logoDark;

  return (
    <img
      src={selectedLogo}
      alt={alt}
      className={`inline-block ${className} object-contain select-none transition-all duration-200 group-hover:opacity-85`}
      loading="eager"
      decoding="async"
    />
  );
}
