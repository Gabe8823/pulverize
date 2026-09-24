/** 配速: 秒/公里 → 6'10" */
export function formatPace(secKm: number | null | undefined): string {
  if (!secKm || secKm <= 0) return '--'
  const total = Math.round(secKm)
  const min = Math.floor(total / 60)
  const sec = total % 60
  return `${min}'${sec.toString().padStart(2, '0')}"`
}

/** 距离: 米 → km */
export function formatDistance(meters: number | null | undefined): string {
  if (!meters || meters <= 0) return '0.00'
  return (meters / 1000).toFixed(2)
}

/** 时长: 秒 → 1:02:15 或 02:15 */
export function formatDuration(seconds: number | null | undefined): string {
  if (!seconds || seconds <= 0) return '00:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) {
    return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

/** 日期时间格式化 */
export function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', weekday: 'short' })
}

export function formatDateTime(dateStr: string): string {
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}
