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

const VS = `attribute vec3 p; attribute vec4 c; uniform vec4 rot; uniform vec3 view; varying vec4 vc;
void main() { float xr = p.x * rot.x + p.z * rot.y, zr = -p.x * rot.y + p.z * rot.x, yr = p.y * rot.z - zr * rot.w, zd = p.y * rot.w + zr * rot.z;
  gl_Position = vec4(xr * view.x, (yr - view.z) * view.y, -zd / 120.0, 1.0); vc = c; }`
const FS = `precision mediump float; varying vec4 vc; void main() { gl_FragColor = vc; }`
/** quads -> triangles: x y z r g b a per corner (colours 0..1) */
function tris(list: Q[]) { const out = new Float32Array(list.length * 6 * 7); let o = 0
  for (const q of list) { const c = [q.c[0] / 255, q.c[1] / 255, q.c[2] / 255, q.c[3] / 255]
    for (const i of [0, 1, 2, 0, 2, 3]) { out[o++] = q.p[i * 3]; out[o++] = q.p[i * 3 + 1]; out[o++] = q.p[i * 3 + 2]; out[o++] = c[0]; out[o++] = c[1]; out[o++] = c[2]; out[o++] = c[3] } }
  return out }

/** bare: only the skin and the cape (the "Skin" view) — the same picture, size and light as the view with cosmetics */
export function LookPreview({ skin, slim, height = 470, cape, hint = true, bare = false, spin = true }: { skin: string; slim: boolean; height?: number; cape?: string; hint?: boolean; bare?: boolean; spin?: boolean }) {
  const cv = useRef<HTMLCanvasElement>(null), [vis, setVis] = useState<any>(null), quads = useRef<{ solid: Q[]; glass: Q[]; v: number }>({ solid: [], glass: [], v: 0 }), view = useRef({ yaw: 200, pitch: 12, drag: false, lx: 0, ly: 0, idle: 0 })
  const spinRef = useRef(spin); spinRef.current = spin
  const load = () => window.api.visGet().then(setVis)
  useEffect(() => { load(); return window.api.onFiles?.((f: any) => { if (f.kind === 'vis') load() }) }, [])
  useEffect(() => { let alive = true; const img = new Image(); img.crossOrigin = 'anonymous'
    img.onload = async () => { const m = bare ? {} : vis?.modules || {}, c = (m as any).cosmetics, p = (m as any).pet, list: Q[] = skinQuads(img, slim)
      const wingsOn = c?.on && (+c.s?.wings || 0) > 0
      if (cape && !wingsOn) { const ci = await loadImg(cape); if (ci) list.push(...capeQuads(ci)) } // wings take the cape's place, as in the game
      if (c?.on) for (const [k, pre] of [['wings', 'w'], ['hat', 'h'], ['back', 'b']] as const) { const i = +c.s?.[k] || 0; if (i > 0) list.push(...fromMesh(await mesh(pre + '-' + i))) }
      const pk = +p?.s?.kind; if (p?.on && pk !== 16 && pk !== 9) { const sc = Math.max(0.5, Math.min(2, +p.s?.size || 1)), q = fromMesh((await mesh('pet-' + pk)).map((v, i) => (i % 13 === 12 ? v : v * sc)), 15, 6)
        if (pk <= 8) { // the white line pictures are tinted with the visuals theme, top colour to bottom colour, like in the game
          const th = vis?.theme || {}, hex = (h: string, d: number[]) => /^#[0-9a-f]{6}$/i.test(h || '') ? [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16)) : d
          const a1 = hex(th.accent, [34, 211, 238]), a2 = hex(th.accent2, [99, 102, 241]), ys = q.flatMap((x) => [x.p[1], x.p[4], x.p[7], x.p[10]]), y0 = Math.min(...ys), y1 = Math.max(...ys)
          for (const x of q) { const t = ((x.p[1] + x.p[7]) / 2 - y0) / Math.max(1e-6, y1 - y0), sh = x.c[0] / 255; x.c = [0, 1, 2].map((k) => Math.min(255, (a2[k] + (a1[k] - a2[k]) * t) * 0.82 * sh + 255 * 0.18 * sh)).concat(x.c[3]) } }
        list.push(...q) }
      if (alive) quads.current = { solid: list.filter((q) => q.c[3] > 250), glass: list.filter((q) => q.c[3] <= 250), v: quads.current.v + 1 } }
    img.src = skin; return () => { alive = false } }, [skin, slim, vis, cape, bare])
  // WebGL with a depth buffer: small details on a face (eyes, stripes) no longer flicker in and out while turning, and it is light for the computer
  useEffect(() => { const c = cv.current; if (!c) return; const gl = c.getContext('webgl', { antialias: true, premultipliedAlpha: false, alpha: true }); if (!gl) return
    const sh = (t: number, src: string) => { const s = gl.createShader(t)!; gl.shaderSource(s, src); gl.compileShader(s); return s }
    const pg = gl.createProgram()!; gl.attachShader(pg, sh(gl.VERTEX_SHADER, VS)); gl.attachShader(pg, sh(gl.FRAGMENT_SHADER, FS)); gl.linkProgram(pg); gl.useProgram(pg)
    const aP = gl.getAttribLocation(pg, 'p'), aC = gl.getAttribLocation(pg, 'c'), uRot = gl.getUniformLocation(pg, 'rot'), uView = gl.getUniformLocation(pg, 'view')
    const bufS = gl.createBuffer(), bufG = gl.createBuffer(); let have = -1, nS = 0, nG = 0, raf = 0
    const bind = (b: WebGLBuffer | null) => { gl.bindBuffer(gl.ARRAY_BUFFER, b); gl.enableVertexAttribArray(aP); gl.vertexAttribPointer(aP, 3, gl.FLOAT, false, 28, 0); gl.enableVertexAttribArray(aC); gl.vertexAttribPointer(aC, 4, gl.FLOAT, false, 28, 12) }
    const draw = () => { raf = requestAnimationFrame(draw); const W = c.clientWidth, H = c.clientHeight, dpr = devicePixelRatio || 1
      if (c.width !== W * dpr || c.height !== H * dpr) { c.width = W * dpr; c.height = H * dpr }
      const v = view.current; if (spinRef.current && !v.drag && performance.now() - v.idle > 1500) v.yaw += 0.35
      const a = v.yaw * Math.PI / 180, b = v.pitch * Math.PI / 180, ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b)
      const q = quads.current
      if (have !== q.v) { have = q.v; const s = tris(q.solid); gl.bindBuffer(gl.ARRAY_BUFFER, bufS); gl.bufferData(gl.ARRAY_BUFFER, s, gl.STATIC_DRAW); nS = s.length / 7 }
      // see-through parts (wings, jelly, glass) go back to front each frame, over the solid ones
      const g = q.glass.map((x) => { let z = 0; for (let i = 0; i < 12; i += 3) { const zr = -x.p[i] * sa + x.p[i + 2] * ca; z += x.p[i + 1] * sb + zr * cb } return { x, z } }).sort((m, n) => m.z - n.z).map((m) => m.x)
      const gd = tris(g); gl.bindBuffer(gl.ARRAY_BUFFER, bufG); gl.bufferData(gl.ARRAY_BUFFER, gd, gl.DYNAMIC_DRAW); nG = gd.length / 7
      gl.viewport(0, 0, c.width, c.height); gl.clearColor(0, 0, 0, 0); gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
      const k = H / 60; gl.uniform4f(uRot, ca, sa, cb, sb); gl.uniform3f(uView, 2 * k / W, 2 * k / H, (H * 0.34) / k) // the feet at 84 % of the height, like before
      gl.enable(gl.DEPTH_TEST); gl.depthFunc(gl.LEQUAL); gl.disable(gl.BLEND); gl.depthMask(true); bind(bufS); gl.drawArrays(gl.TRIANGLES, 0, nS)
      gl.enable(gl.BLEND); gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA); gl.depthMask(false); bind(bufG); gl.drawArrays(gl.TRIANGLES, 0, nG); gl.depthMask(true) }
    draw(); return () => cancelAnimationFrame(raf) }, [])
  const down = (e: React.PointerEvent) => { const v = view.current; v.drag = true; v.lx = e.clientX; v.ly = e.clientY; (e.target as HTMLElement).setPointerCapture(e.pointerId) }
  const move = (e: React.PointerEvent) => { const v = view.current; if (!v.drag) return; v.yaw += (e.clientX - v.lx) * 0.6; v.pitch = Math.max(-30, Math.min(60, v.pitch + (e.clientY - v.ly) * 0.4)); v.lx = e.clientX; v.ly = e.clientY }
  const up = () => { const v = view.current; v.drag = false; v.idle = performance.now() }
  return <div className="relative" style={{ height }}>
    <div className="absolute inset-0 pointer-events-none" style={{ background: 'radial-gradient(48% 42% at 50% 46%, color-mix(in srgb, var(--a2) 22%, transparent), transparent 70%)' }} />
    <div className="absolute inset-x-[18%] bottom-[9%] h-6 rounded-[50%]" style={{ background: 'radial-gradient(closest-side, rgba(0,0,0,.35), transparent)' }} />
    <canvas ref={cv} className="w-full h-full cursor-grab active:cursor-grabbing relative" onPointerDown={down} onPointerMove={move} onPointerUp={up} onPointerCancel={up} />
    {hint && <div className="absolute left-3 bottom-2 muted text-[11px]">Крутите мышью · так вас видят в игре</div>}</div>
}
