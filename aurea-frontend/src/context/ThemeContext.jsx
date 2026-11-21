import { createContext, useContext, useEffect, useState } from "react";

const ThemeContext = createContext(null);
export const THEMES = [
  { id: "aurea", label: "Aurea clasica" },
  { id: "ciruela", label: "Tema oscuro" },
  { id: "noche", label: "Azul profundo" },
];

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => {
    try {
      const saved = localStorage.getItem("aurea-theme");
      return THEMES.some((item) => item.id === saved) ? saved : "aurea";
    }
    catch { return "aurea"; }
  });

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try { localStorage.setItem("aurea-theme", theme); } catch { /* storage may be disabled */ }
  }, [theme]);

  return <ThemeContext.Provider value={{ theme, setTheme }}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const value = useContext(ThemeContext);
  if (!value) throw new Error("useTheme debe usarse dentro de ThemeProvider");
  return value;
}
