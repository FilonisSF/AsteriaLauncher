import { useEffect, useRef } from 'react'
export default function Fx({ kind, dark }: { kind: string; dark: boolean }) {
  const ref = useRef<HTMLCanvasElement>(null)
  useEffect(() => {
    const c = ref.current!, x = c.getContext('2d')!; let raf = 0
    const fit = () => { c.width = innerWidth; c.height = innerHeight }; fit(); addEventListener('resize', fit)
    if (kind === 'none') return () => removeEventListener('resize', fit)
    const N = kind === 'rain' ? 160 : kind === 'stars' ? 90 : 70
    const P = Array.from({ length: N }, () => ({ x: Math.random() * c.width, y: Math.random() * c.height, s: Math.random() * 2 + 1, v: Math.random() * 1.2 + .4, p: Math.random() * 6 }))
    const loop = (t: number) => {
      x.clearRect(0, 0, c.width, c.height)
      for (const p of P) {
        if (kind === 'snow') { p.y += p.v; p.x += Math.sin(t / 900 + p.p) * .5; x.fillStyle = dark ? 'rgba(255,255,255,.8)' : 'rgba(56,189,248,.55)'; x.beginPath(); x.arc(p.x, p.y, p.s, 0, 7); x.fill() }
        else if (kind === 'rain') { p.y += p.v * 9; p.x -= 1.5; x.strokeStyle = dark ? 'rgba(125,211,252,.5)' : 'rgba(14,165,233,.4)'; x.beginPath(); x.moveTo(p.x, p.y); x.lineTo(p.x + 3, p.y - 14); x.stroke() }
        else if (kind === 'leaves') { p.y += p.v; p.x += Math.sin(t / 700 + p.p) * 1.1; x.save(); x.translate(p.x, p.y); x.rotate(t / 500 + p.p); x.fillStyle = ['#f59e0b', '#ea580c', '#84cc16'][Math.floor(p.p) % 3]; x.globalAlpha = .75; x.beginPath(); x.ellipse(0, 0, p.s * 4, p.s * 2, 0, 0, 7); x.fill(); x.restore() }
        else if (kind === 'stars') { x.fillStyle = `rgba(${dark ? '255,255,255' : '14,165,233'},${.3 + .6 * Math.abs(Math.sin(t / 800 + p.p))})`; x.fillRect(p.x, p.y, p.s, p.s) }
        else { p.y -= p.v; p.x += Math.sin(t / 600 + p.p) * .4; x.fillStyle = 'rgba(34,211,238,.6)'; x.shadowColor = '#22d3ee'; x.shadowBlur = 8; x.fillRect(p.x, p.y, p.s, p.s); x.shadowBlur = 0 }
        if (p.y > c.height + 20) { p.y = -10; p.x = Math.random() * c.width } if (p.y < -20) p.y = c.height + 10
      }
      raf = requestAnimationFrame(loop)
    }
    raf = requestAnimationFrame(loop)
    return () => { cancelAnimationFrame(raf); removeEventListener('resize', fit) }
  }, [kind, dark])
  return <canvas ref={ref} className="fixed inset-0 pointer-events-none" style={{ zIndex: 0 }} />
}
