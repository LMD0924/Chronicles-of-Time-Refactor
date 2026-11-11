import { ref, watch } from 'vue'

const STORAGE_KEY = 'cot-theme'

function readStored() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved === 'light' || saved === 'dark' ? saved : null
  } catch { return null }
}

const theme = ref(readStored() || 'light')

function apply(value) {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('data-theme', value)
  document.documentElement.style.colorScheme = value
}

watch(theme, (value) => {
  apply(value)
  try { localStorage.setItem(STORAGE_KEY, value) } catch { /* private mode */ }
})

export function useTheme() {
  return {
    theme,
    initTheme: () => apply(theme.value),
    setTheme: (v) => { if (v === 'light' || v === 'dark') theme.value = v },
    toggleTheme: () => { theme.value = theme.value === 'dark' ? 'light' : 'dark' }
  }
}
