import { useEffect, useRef, useState } from 'react'

const hex2rgb = (h: string) => { const n = parseInt(h.slice(1), 16); return [(n >> 16) & 255, (n >> 8) & 255, n & 255] }
const rgb2hex = (r: number, g: number, b: number) => '#' + [r, g, b].map((x) => Math.round(Math.max(0, Math.min(255, x))).toString(16).padStart(2, '0')).join('')
export function hsv2hex(h: number, s: number, v: number) {
  const f = (n: number) => { const k = (n + h / 60) % 6; return v - v * s * Math.max(0, Math.min(k, 4 - k, 1)) }
  return rgb2hex(f(5) * 255, f(3) * 255, f(1) * 255)
}
export function hex2hsv(hex: string): [number, number, number] {
  const [r, g, b] = hex2rgb(hex).map((x) => x / 255), mx = Math.max(r, g, b), mn = Math.min(r, g, b), d = mx - mn
  let h = 0
  if (d) h = mx === r ? ((g - b) / d) % 6 : mx === g ? (b - r) / d + 2 : (r - g) / d + 4
  return [(h * 60 + 360) % 360, mx ? d / mx : 0, mx]
}

/** Hue ring with a saturation/value square inside (the "square in a circle" picker). */
export default function ColorWheel({ value, onChange, size = 210 }: { value: string; onChange: (hex: string) => void; size?: number }) {
  const cv = useRef<HTMLCanvasElement>(null), [hsv, setHsv] = useState<[number, number, number]>(() => hex2hsv(value)), drag = useRef<'ring' | 'sq' | null>(null)
  const R = size / 2, ring = Math.max(14, size * 0.09), rin = R - ring - 4, sq = Math.floor(rin * Math.SQRT2) - 6, sq0 = R - sq / 2
  useEffect(() => { if (hsv2hex(hsv[0], hsv[1], hsv[2]) !== value.toLowerCase()) { const n = hex2hsv(value); setHsv((o) => [n[1] === 0 || n[2] === 0 ? o[0] : n[0], n[1], n[2]]) } }, [value])
  useEffect(() => { // draw: conic hue ring + square (hue -> white horizontally, -> black vertically)
    const c = cv.current!.getContext('2d')!, dpr = devicePixelRatio || 1
    cv.current!.width = size * dpr; cv.current!.height = size * dpr; c.scale(dpr, dpr); c.clearRect(0, 0, size, size)
    const g = c.createConicGradient(-Math.PI / 2, R, R)
    for (let i = 0; i <= 12; i++) g.addColorStop(i / 12, `hsl(${i * 30},100%,50%)`)
    c.beginPath(); c.arc(R, R, R - 1, 0, Math.PI * 2); c.arc(R, R, R - ring, 0, Math.PI * 2, true); c.fillStyle = g; c.fill()
    c.fillStyle = hsv2hex(hsv[0], 1, 1); c.beginPath(); c.roundRect(sq0, sq0, sq, sq, 6); c.fill()
    const w = c.createLinearGradient(sq0, 0, sq0 + sq, 0); w.addColorStop(0, '#fff'); w.addColorStop(1, 'rgba(255,255,255,0)'); c.fillStyle = w; c.beginPath(); c.roundRect(sq0, sq0, sq, sq, 6); c.fill()
    const b = c.createLinearGradient(0, sq0, 0, sq0 + sq); b.addColorStop(0, 'rgba(0,0,0,0)'); b.addColorStop(1, '#000'); c.fillStyle = b; c.beginPath(); c.roundRect(sq0, sq0, sq, sq, 6); c.fill()
    const ang = (hsv[0] - 90) * Math.PI / 180, rm = R - ring / 2 - 0.5
    const mark = (x: number, y: number, r: number, fill: string) => { c.beginPath(); c.arc(x, y, r, 0, Math.PI * 2); c.fillStyle = fill; c.fill(); c.lineWidth = 3; c.strokeStyle = '#fff'; c.stroke(); c.lineWidth = 1; c.strokeStyle = 'rgba(0,0,0,.35)'; c.beginPath(); c.arc(x, y, r + 2, 0, Math.PI * 2); c.stroke() }
    mark(R + rm * Math.cos(ang), R + rm * Math.sin(ang), ring / 2 - 2, hsv2hex(hsv[0], 1, 1))
    mark(sq0 + hsv[1] * sq, sq0 + (1 - hsv[2]) * sq, 7, hsv2hex(hsv[0], hsv[1], hsv[2]))
  }, [hsv, size])
  const pick = (e: React.PointerEvent, start: boolean) => {
    const r = cv.current!.getBoundingClientRect(), x = e.clientX - r.left, y = e.clientY - r.top, dist = Math.hypot(x - R, y - R)
    if (start) drag.current = dist > R - ring - 2 ? 'ring' : x >= sq0 - 6 && x <= sq0 + sq + 6 && y >= sq0 - 6 && y <= sq0 + sq + 6 ? 'sq' : null
    let n: [number, number, number] = hsv
    if (drag.current === 'ring') n = [(Math.atan2(y - R, x - R) * 180 / Math.PI + 90 + 360) % 360, hsv[1], hsv[2]]
    else if (drag.current === 'sq') n = [hsv[0], Math.max(0, Math.min(1, (x - sq0) / sq)), Math.max(0, Math.min(1, 1 - (y - sq0) / sq))]
    else return
    setHsv(n); onChange(hsv2hex(n[0], n[1], n[2]))
  }
  return <canvas ref={cv} style={{ width: size, height: size, touchAction: 'none', cursor: 'crosshair' }}
    onPointerDown={(e) => { (e.target as Element).setPointerCapture(e.pointerId); pick(e, true) }} onPointerMove={(e) => e.buttons & 1 && pick(e, false)} onPointerUp={() => { drag.current = null }} />
}
