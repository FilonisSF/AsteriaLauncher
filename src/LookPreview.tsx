import { useEffect, useRef, useState } from 'react'

/**
 * "How I look": your skin with the chosen 3D cosmetics and pet, drawn on a canvas (painter's order, flat shading) and turned with the mouse.
 * Cosmetics and pets are the very models of the mod, exported to assets/cosm3d (13 numbers per quad: 4 corners x y z in model pixels, colour).
 * Coordinates: model pixels, feet at y = 0, the player looks at +z, his right hand is at -x.
 */
const MESH = import.meta.glob('./assets/cosm3d/*.json', { query: '?url', import: 'default' }) as Record<string, () => Promise<string>>
const meshCache: Record<string, Promise<number[]>> = {}
const mesh = (name: string) => { const k = './assets/cosm3d/' + name + '.json'; if (!MESH[k]) return Promise.resolve([] as number[])
  return (meshCache[name] ||= MESH[k]().then((u) => fetch(u)).then((r) => r.json()).catch(() => [])) }

type Q = { p: number[]; c: number[] } // 4 corners (x y z x 4), rgba 0..255
/** the skin as one small quad per texture pixel (base layer + the outer layer a bit bigger) */
function skinQuads(img: HTMLImageElement, slim: boolean): Q[] {
  const cv = document.createElement('canvas'); cv.width = 64; cv.height = 64; const x = cv.getContext('2d')!; x.drawImage(img, 0, 0, 64, img.height >= 64 ? 64 : 32)
  const px = x.getImageData(0, 0, 64, 64).data, out: Q[] = []
  const at = (u: number, v: number) => { const i = (v * 64 + u) * 4; return [px[i], px[i + 1], px[i + 2], px[i + 3]] }
  const legacy = img.height < 64, aw = slim ? 3 : 4
  // u, v, w, h, d, min corner x y z, inflate, outer layer
  const box = (u: number, v: number, w: number, h: number, d: number, x0: number, y0: number, z0: number, e: number, over: boolean) => {
    const X0 = x0 - e, Y0 = y0 - e, Z0 = z0 - e, sx = (w + 2 * e) / w, sy = (h + 2 * e) / h, sz = (d + 2 * e) / d, sh = [1, 0.86, 0.8, 0.8, 1.08, 0.62]
    const put = (c: number[], s: number, p: number[]) => { if (c[3] < (over ? 30 : 1)) return; out.push({ p, c: [Math.min(255, c[0] * s), Math.min(255, c[1] * s), Math.min(255, c[2] * s), over ? c[3] : 255] }) }
    for (let i = 0; i < w; i++) for (let j = 0; j < h; j++) { const yt = Y0 + (h - j) * sy, yb = yt - sy
      put(at(u + d + i, v + d + j), sh[0], [X0 + i * sx, yt, Z0 + d * sz + 0.001, X0 + (i + 1) * sx, yt, Z0 + d * sz, X0 + (i + 1) * sx, yb, Z0 + d * sz, X0 + i * sx, yb, Z0 + d * sz])          // front (+z)
      put(at(u + 2 * d + w + i, v + d + j), sh[1], [X0 + (w - i) * sx, yt, Z0, X0 + (w - i - 1) * sx, yt, Z0, X0 + (w - i - 1) * sx, yb, Z0, X0 + (w - i) * sx, yb, Z0]) }                     // back
    for (let i = 0; i < d; i++) for (let j = 0; j < h; j++) { const yt = Y0 + (h - j) * sy, yb = yt - sy
      put(at(u + i, v + d + j), sh[2], [X0, yt, Z0 + i * sz, X0, yt, Z0 + (i + 1) * sz, X0, yb, Z0 + (i + 1) * sz, X0, yb, Z0 + i * sz])                                                    // right side (-x)
      put(at(u + d + w + i, v + d + j), sh[3], [X0 + w * sx, yt, Z0 + (d - i) * sz, X0 + w * sx, yt, Z0 + (d - i - 1) * sz, X0 + w * sx, yb, Z0 + (d - i - 1) * sz, X0 + w * sx, yb, Z0 + (d - i) * sz]) } // left side (+x)
    for (let i = 0; i < w; i++) for (let j = 0; j < d; j++) {
      put(at(u + d + i, v + j), sh[4], [X0 + i * sx, Y0 + h * sy, Z0 + j * sz, X0 + (i + 1) * sx, Y0 + h * sy, Z0 + j * sz, X0 + (i + 1) * sx, Y0 + h * sy, Z0 + (j + 1) * sz, X0 + i * sx, Y0 + h * sy, Z0 + (j + 1) * sz]) // top
      put(at(u + d + w + i, v + j), sh[5], [X0 + i * sx, Y0, Z0 + (d - j) * sz, X0 + (i + 1) * sx, Y0, Z0 + (d - j) * sz, X0 + (i + 1) * sx, Y0, Z0 + (d - j - 1) * sz, X0 + i * sx, Y0, Z0 + (d - j - 1) * sz]) } // bottom
  }
  for (const over of [false, true]) { const e = over ? 0.25 : 0
    box(over ? 32 : 0, 0, 8, 8, 8, -4, 24, -4, over ? 0.5 : 0, over)                                   // head / hat
    box(16, over ? 32 : 16, 8, 12, 4, -4, 12, -2, e, over)                                                 // body / jacket
    box(40, over ? 32 : 16, aw, 12, 4, -4 - aw, 12, -2, e, over)                                           // right arm
    box(legacy ? 40 : over ? 48 : 32, legacy ? 16 : 48, aw, 12, 4, 4, 12, -2, e, over)                     // left arm
    box(0, over ? 32 : 16, 4, 12, 4, -4, 0, -2, e, over)                                                   // right leg
    box(legacy ? 0 : over ? 0 : 16, legacy ? 16 : 48, 4, 12, 4, 0, 0, -2, e, over)                         // left leg
    if (legacy) break }
  return out
}
const fromMesh = (a: number[], dx = 0, dz = 0): Q[] => { const out: Q[] = []
  for (let i = 0; i + 12 < a.length; i += 13) { const c = a[i + 12] >>> 0; out.push({ p: [a[i] + dx, a[i + 1], a[i + 2] + dz, a[i + 3] + dx, a[i + 4], a[i + 5] + dz, a[i + 6] + dx, a[i + 7], a[i + 8] + dz, a[i + 9] + dx, a[i + 10], a[i + 11] + dz], c: [(c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255] }) }
  return out }

/** the cape: 10 x 16 behind the shoulders, a little away from the back at the bottom; HD capes keep up to 4 samples per pixel */
function capeQuads(img: HTMLImageElement): Q[] {
  const s = Math.max(1, img.width / 64), n = Math.min(4, Math.round(s)), cv = document.createElement('canvas'); cv.width = 22 * n; cv.height = 17 * n
  const x = cv.getContext('2d')!; x.imageSmoothingEnabled = true; x.drawImage(img, 0, 0, 22 * s, 17 * s, 0, 0, 22 * n, 17 * n); const px = x.getImageData(0, 0, 22 * n, 17 * n).data, out: Q[] = []
  const at = (u: number, v: number) => { const i = (v * 22 * n + u) * 4; return [px[i], px[i + 1], px[i + 2], px[i + 3]] }, d = 1 / n, tilt = Math.tan(10 * Math.PI / 180)
  const zAt = (y: number) => -2.7 - (24 - y) * tilt
  for (let i = 0; i < 10 * n; i++) for (let j = 0; j < 16 * n; j++) { const yt = 24 - j * d, yb = yt - d, x0 = 5 - i * d, x1 = x0 - d
    const o = at(n + i, n + j); if (o[3] > 8) out.push({ p: [x0, yt, zAt(yt) - 0.06, x1, yt, zAt(yt) - 0.06, x1, yb, zAt(yb) - 0.06, x0, yb, zAt(yb) - 0.06], c: [o[0] * 0.82, o[1] * 0.82, o[2] * 0.82, 255] }) // outside
    const inn = at(12 * n + i, n + j); if (inn[3] > 8) out.push({ p: [-x0, yt, zAt(yt), -x1, yt, zAt(yt), -x1, yb, zAt(yb), -x0, yb, zAt(yb)], c: [inn[0] * 0.7, inn[1] * 0.7, inn[2] * 0.7, 255] }) } // towards the back
  return out }
const loadImg = (src: string) => new Promise<HTMLImageElement | null>((res) => { const im = new Image(); im.crossOrigin = 'anonymous'; im.onload = () => res(im); im.onerror = () => res(null); im.src = src })

export function LookPreview({ skin, slim, height = 470, cape, hint = true }: { skin: string; slim: boolean; height?: number; cape?: string; hint?: boolean }) {
  const cv = useRef<HTMLCanvasElement>(null), [vis, setVis] = useState<any>(null), quads = useRef<Q[]>([]), view = useRef({ yaw: 200, pitch: 12, drag: false, lx: 0, ly: 0, idle: 0 })
  const load = () => window.api.visGet().then(setVis)
  useEffect(() => { load(); return window.api.onFiles?.((f: any) => { if (f.kind === 'vis') load() }) }, [])
  useEffect(() => { let alive = true; const img = new Image(); img.crossOrigin = 'anonymous'
    img.onload = async () => { const m = vis?.modules || {}, c = m.cosmetics, p = m.pet, list: Q[] = skinQuads(img, slim)
      const wingsOn = c?.on && (+c.s?.wings || 0) > 0
      if (cape && !wingsOn) { const ci = await loadImg(cape); if (ci) list.push(...capeQuads(ci)) } // wings take the cape's place, as in the game
      if (c?.on) for (const [k, pre] of [['wings', 'w'], ['hat', 'h'], ['back', 'b']] as const) { const i = +c.s?.[k] || 0; if (i > 0) list.push(...fromMesh(await mesh(pre + '-' + i))) }
      const pk = +p?.s?.kind; if (p?.on && (pk >= 10 && pk !== 16)) { const sc = Math.max(0.5, Math.min(2, +p.s?.size || 1)); list.push(...fromMesh((await mesh('pet-' + pk)).map((v, i) => (i % 13 === 12 ? v : v * sc)), 15, 6)) }
      if (alive) quads.current = list }
    img.src = skin; return () => { alive = false } }, [skin, slim, vis, cape])
  useEffect(() => { let raf = 0
    const draw = () => { raf = requestAnimationFrame(draw); const c = cv.current; if (!c) return; const W = c.clientWidth, H = c.clientHeight, dpr = devicePixelRatio || 1
      if (c.width !== W * dpr || c.height !== H * dpr) { c.width = W * dpr; c.height = H * dpr }
      const g = c.getContext('2d')!; g.setTransform(dpr, 0, 0, dpr, 0, 0); g.clearRect(0, 0, W, H)
      const v = view.current; if (!v.drag && performance.now() - v.idle > 1500) v.yaw += 0.35
      const a = v.yaw * Math.PI / 180, b = v.pitch * Math.PI / 180, ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b), k = H / 60, ox = W / 2, oy = H * 0.84
      const pr = quads.current.map((q) => { const pts: number[] = []; let z = 0
        for (let i = 0; i < 12; i += 3) { const x = q.p[i], y = q.p[i + 1], zz = q.p[i + 2], xr = x * ca + zz * sa, zr = -x * sa + zz * ca, yr = y * cb - zr * sb, zd = y * sb + zr * cb
          pts.push(ox + xr * k, oy - yr * k); z += zd } return { pts, z: z / 4, c: q.c } })
      pr.sort((p, q) => p.z - q.z) // far first: the camera looks along -z after turning
      for (const q of pr) { const [r, gg, bb, al] = q.c; g.fillStyle = g.strokeStyle = `rgba(${r | 0},${gg | 0},${bb | 0},${(al / 255).toFixed(3)})`
        g.beginPath(); g.moveTo(q.pts[0], q.pts[1]); for (let i = 2; i < 8; i += 2) g.lineTo(q.pts[i], q.pts[i + 1]); g.closePath(); g.fill(); if (al > 250) { g.lineWidth = 0.6; g.stroke() } } }
    draw(); return () => cancelAnimationFrame(raf) }, [])
  const down = (e: React.PointerEvent) => { const v = view.current; v.drag = true; v.lx = e.clientX; v.ly = e.clientY; (e.target as HTMLElement).setPointerCapture(e.pointerId) }
  const move = (e: React.PointerEvent) => { const v = view.current; if (!v.drag) return; v.yaw += (e.clientX - v.lx) * 0.6; v.pitch = Math.max(-30, Math.min(60, v.pitch + (e.clientY - v.ly) * 0.4)); v.lx = e.clientX; v.ly = e.clientY }
  const up = () => { const v = view.current; v.drag = false; v.idle = performance.now() }
  return <div className="relative" style={{ height }}>
    <div className="absolute inset-x-[18%] bottom-[9%] h-6 rounded-[50%]" style={{ background: 'radial-gradient(closest-side, rgba(0,0,0,.35), transparent)' }} />
    <canvas ref={cv} className="w-full h-full cursor-grab active:cursor-grabbing relative" onPointerDown={down} onPointerMove={move} onPointerUp={up} onPointerCancel={up} />
    {hint && <div className="absolute left-3 bottom-2 muted text-[11px]">Крутите мышью · так вас видят в игре</div>}</div>
}
