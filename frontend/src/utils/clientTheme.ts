/**
 * 桌面客户端专属：背景主题（网页端永不启用）
 * - isClient()：仅 Electron 客户端为 true（preload 注入 window.runai）
 * - 客户端与网页是不同 origin（127.0.0.1:5219 vs :2020），localStorage 天然隔离
 */

export type ClientBgId = 'default' | 'red-black' | 'graphite' | 'cloud'

declare global {
  interface Window {
    runai?: { isClient: boolean; client: string; electron?: string; chrome?: string }
  }
}

const KEY = 'runai-client-bg'

export function isClient(): boolean {
  return !!window.runai?.isClient
}

export function getBg(): string {
  try {
    return localStorage.getItem(KEY) || 'default'
  } catch {
    return 'default'
  }
}

export function applyBg(value: string): void {
  if (!isClient()) return
  const root = document.documentElement
  if (value.startsWith('image:')) {
    root.dataset.bg = 'image'
    root.style.setProperty('--client-bg-image', `url("${value.slice(6)}")`)
  } else {
    root.dataset.bg = value === 'image' ? 'default' : value
    root.style.removeProperty('--client-bg-image')
  }
  try {
    localStorage.setItem(KEY, value)
  } catch {
    /* 超配额时忽略持久化，仅本次生效 */
  }
}

/** 本地图片 → 压缩 dataURL（≤1600px / webp 0.82），超限抛错 */
export async function fileToBg(file: File): Promise<string> {
  if (!file.type.startsWith('image/')) throw new Error('not-image')
  const bitmap = await createImageBitmap(file)
  const scale = Math.min(1, 1600 / Math.max(bitmap.width, bitmap.height))
  const w = Math.max(1, Math.round(bitmap.width * scale))
  const h = Math.max(1, Math.round(bitmap.height * scale))
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('canvas-unavailable')
  ctx.drawImage(bitmap, 0, 0, w, h)
  const url = canvas.toDataURL('image/webp', 0.82)
  if (url.length > 3500000) throw new Error('too-large')
  return 'image:' + url
}
