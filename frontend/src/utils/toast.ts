/**
 * Apple 风格轻提示（替代 ElMessage）
 * 顶部居中胶囊，自动消失；容器挂在 body 上。
 */

type ToastType = 'success' | 'error' | 'info'

let container: HTMLElement | null = null

function ensureContainer(): HTMLElement {
  if (!container || !document.body.contains(container)) {
    container = document.createElement('div')
    container.className = 'a-toast-container'
    document.body.appendChild(container)
  }
  return container
}

export function toast(message: string, type: ToastType = 'info', duration = 2600): void {
  const parent = ensureContainer()
  const el = document.createElement('div')
  el.className = `a-toast a-toast--${type}`
  el.innerHTML = `<span class="a-toast__icon">${iconFor(type)}</span><span class="a-toast__text"></span>`
  ;el.querySelector('.a-toast__text')!.textContent = message
  parent.appendChild(el)
  requestAnimationFrame(() => el.classList.add('is-visible'))
  window.setTimeout(() => {
    el.classList.remove('is-visible')
    window.setTimeout(() => el.remove(), 260)
  }, duration)
}

toast.success = (m: string) => toast(m, 'success')
toast.error = (m: string) => toast(m, 'error', 3600)
toast.info = (m: string) => toast(m, 'info')

function iconFor(type: ToastType): string {
  if (type === 'success') {
    return '<svg viewBox="0 0 20 20" width="16" height="16" fill="none"><circle cx="10" cy="10" r="9" fill="#34c759"/><path d="M6 10.4l2.6 2.6L14.2 7.4" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  }
  if (type === 'error') {
    return '<svg viewBox="0 0 20 20" width="16" height="16" fill="none"><circle cx="10" cy="10" r="9" fill="#ff3b30"/><path d="M7 7l6 6M13 7l-6 6" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/></svg>'
  }
  return '<svg viewBox="0 0 20 20" width="16" height="16" fill="none"><circle cx="10" cy="10" r="9" fill="#e30613"/><path d="M10 6v5M10 13.6v.4" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/></svg>'
}

export { toast as default }
