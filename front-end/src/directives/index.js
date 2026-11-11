/** 与参考项目一致的滚动入场 / 鼠标跟随光 / 数字滚动指令，只动画 transform / opacity。 */
const reduce = () => typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches

let observer = null
function getObserver() {
  if (observer) return observer
  observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' })
  return observer
}

export const reveal = {
  mounted(el, binding) {
    if (reduce()) { el.classList.add('is-visible'); return }
    el.classList.add('reveal')
    el.style.transitionDelay = `${Math.min(Number(binding.value) || 0, 8) * 70}ms`
    getObserver().observe(el)
  },
  unmounted(el) { observer?.unobserve(el) }
}

export const spotlight = {
  mounted(el) {
    if (reduce()) return
    el.classList.add('spotlight')
    el._spot = (e) => {
      const r = el.getBoundingClientRect()
      el.style.setProperty('--mx', `${e.clientX - r.left}px`)
      el.style.setProperty('--my', `${e.clientY - r.top}px`)
    }
    el.addEventListener('pointermove', el._spot, { passive: true })
  },
  unmounted(el) { el.removeEventListener('pointermove', el._spot) }
}

function animateCount(el, target) {
  const num = Number(target)
  if (!Number.isFinite(num)) { el.textContent = target; return }
  if (reduce()) { el.textContent = String(num); return }
  const from = Number(String(el.textContent).replace(/[^\d.-]/g, '')) || 0
  if (from === num) { el.textContent = String(num); return }
  const start = performance.now()
  const dur = 900
  const step = (now) => {
    const t = Math.min(1, (now - start) / dur)
    const eased = 1 - Math.pow(1 - t, 3)
    el.textContent = String(Math.round(from + (num - from) * eased))
    if (t < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

export const count = {
  mounted(el, binding) { animateCount(el, binding.value) },
  updated(el, binding) { if (binding.value !== binding.oldValue) animateCount(el, binding.value) }
}
