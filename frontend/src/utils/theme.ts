export type ThemeMode = 'light' | 'dark'

const THEME_KEY = 'runai-theme'

export function getTheme(): ThemeMode {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved === 'dark' || saved === 'light') return saved
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function applyTheme(mode: ThemeMode): void {
  document.documentElement.setAttribute('data-theme', mode)
}

export function setTheme(mode: ThemeMode): void {
  localStorage.setItem(THEME_KEY, mode)
  applyTheme(mode)
}

export function toggleTheme(): ThemeMode {
  const next: ThemeMode = getTheme() === 'dark' ? 'light' : 'dark'
  setTheme(next)
  return next
}
