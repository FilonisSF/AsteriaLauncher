import { ReactNode, useEffect, useRef, useState } from 'react'
import Skin3D, { Hit } from './Skin3D'
import ColorWheel from './ColorWheel'
import { Check, ColorPick } from './ui'
import { Icon } from './Icons'

const SKIN_GUIDES: [number, number, number, number, string][] = [[0, 0, 32, 16, 'Голова'], [32, 0, 32, 16, 'Шапка'], [16, 16, 24, 16, 'Тело'], [16, 32, 24, 16, 'Куртка'], [40, 16, 16, 16, 'Пр. рука'], [40, 32, 16, 16, 'Рукав'],
  [0, 16, 16, 16, 'Пр. нога'], [0, 32, 16, 16, 'Штанина'], [16, 48, 16, 16, 'Лев. нога'], [0, 48, 16, 16, 'Штанина'], [32, 48, 16, 16, 'Лев. рука'], [48, 48, 16, 16, 'Рукав']]
const CAPE_GUIDES: [number, number, number, number, string][] = [[1, 1, 10, 16, 'Снаружи'], [12, 1, 10, 16, 'Изнутри'], [0, 1, 1, 16, ''], [11, 1, 1, 16, ''], [1, 0, 10, 1, ''], [11, 0, 10, 1, '']]
const PALETTE = ['#000000', '#3f3f46', '#a1a1aa', '#ffffff', '#7f1d1d', '#ef4444', '#f97316', '#facc15', '#65a30d', '#22c55e', '#0d9488', '#22d3ee', '#0ea5e9', '#1d4ed8', '#7c3aed', '#c026d3', '#ec4899', '#78350f', '#c68642', '#f1c27d']
const TOOLS: [string, string, string, string][] = [['brush', 'pencil', 'Кисть', 'B'], ['eraser', 'eraser', 'Ластик', 'E'], ['fill', 'bucket', 'Заливка', 'G'], ['picker', 'picker', 'Пипетка', 'I'],
  ['line', 'line', 'Линия', 'L'], ['rect', 'rect', 'Прямоугольник (Shift — залитый)', 'R'], ['noise', 'noise', 'Шум: оттенки текущего цвета', 'N'], ['lighten', 'sun', 'Осветлить', 'U'], ['darken', 'moon', 'Затемнить', 'D']]
/** the faces of every box on a 64x64 skin / a cape: painting stays inside them (nothing lands on unused parts of the texture) */
type Rect = [number, number, number, number]
const boxFaces = (u: number, v: number, w: number, h: number, d: number): Rect[] => [[u + d, v, w, d], [u + d + w, v, w, d], [u, v + d, d, h], [u + d, v + d, w, h], [u + d + w, v + d, d, h], [u + 2 * d + w, v + d, w, h]]
function skinFaces(slim: boolean): { r: Rect; over: boolean }[] {
  const a = slim ? 3 : 4, out: { r: Rect; over: boolean }[] = []
  const boxes: [number, number, number, number, number, boolean][] = [[0, 0, 8, 8, 8, false], [32, 0, 8, 8, 8, true], [16, 16, 8, 12, 4, false], [16, 32, 8, 12, 4, true], [40, 16, a, 12, 4, false], [40, 32, a, 12, 4, true],
    [32, 48, a, 12, 4, false], [48, 48, a, 12, 4, true], [0, 16, 4, 12, 4, false], [0, 32, 4, 12, 4, true], [16, 48, 4, 12, 4, false], [0, 48, 4, 12, 4, true]]
  for (const [u, v, w, h, d, over] of boxes) for (const r of boxFaces(u, v, w, h, d)) out.push({ r, over })
  return out
}
const CAPE_FACES: { r: Rect; over: boolean }[] = [[1, 1, 10, 16], [12, 1, 10, 16], [0, 1, 1, 16], [11, 1, 1, 16], [1, 0, 10, 1], [11, 0, 10, 1]].map((r) => ({ r: r as Rect, over: false }))
const RES: [number, string][] = [[1, '64×32 · классика'], [8, '512×256 · HD'], [16, '1024×512 · HD+']]

type Design = { bg: 'solid' | 'gradient'; c1: string; c2: string; img: string; scale: number; ox: number; oy: number; rot: number; fit: 'cover' | 'contain'; back: boolean; border: number; borderColor: string }
const DESIGN: Design = { bg: 'gradient', c1: '#0b1f3a', c2: '#0e7490', img: '', scale: 100, ox: 0, oy: 0, rot: 0, fit: 'cover', back: false, border: 1, borderColor: '#22d3ee' }

/** Skin / cape editor: paint right on the 3D model or on the 2D unwrap; cape designer (background, image, border, HD). */
/** pick the part of a picture that goes on the cape: a frame in the cape's proportions (10:16) you move and resize */
function CropBox({ src, aspect, onDone, onCancel, title = 'Выберите область для плаща', extra, okText = 'Использовать область', onRect }: { src: string; aspect: number; onDone: (png: string) => void; onCancel: () => void; title?: string; extra?: ReactNode; okText?: string; onRect?: (r: { x: number; y: number; w: number; h: number }) => void }) {
  const [im, setIm] = useState<HTMLImageElement | null>(null), [sel, setSel] = useState({ x: 0, y: 0, w: 0 }), drag = useRef<{ mode: 'move' | 'size'; px: number; py: number; s: typeof sel } | null>(null)
  useEffect(() => { const i = new Image(); i.onload = () => { setIm(i); const w = Math.min(i.width, i.height * aspect) * 0.8; setSel({ x: (i.width - w) / 2, y: (i.height - w / aspect) / 2, w }) }; i.src = src }, [src])
  const BOX = 520, k = im ? Math.min(BOX / im.width, BOX / im.height) : 1, h = sel.w / aspect
  const clamp = (s: typeof sel) => { if (!im) return s; let w = Math.max(16, Math.min(s.w, im.width, im.height * aspect)); const hh = w / aspect
    return { w, x: Math.max(0, Math.min(im.width - w, s.x)), y: Math.max(0, Math.min(im.height - hh, s.y)) } }
  const down = (mode: 'move' | 'size') => (e: React.PointerEvent) => { e.stopPropagation(); (e.target as Element).setPointerCapture(e.pointerId); drag.current = { mode, px: e.clientX, py: e.clientY, s: sel } }
  const move = (e: React.PointerEvent) => { const d = drag.current; if (!d) return; const dx = (e.clientX - d.px) / k, dy = (e.clientY - d.py) / k
    setSel(clamp(d.mode === 'move' ? { ...d.s, x: d.s.x + dx, y: d.s.y + dy } : { ...d.s, w: d.s.w + Math.max(dx, dy * aspect) })) }
  const wheel = (e: React.WheelEvent) => { const nw = sel.w * (e.deltaY > 0 ? 1.08 : 0.92), c = { x: sel.x + sel.w / 2, y: sel.y + h / 2 }; setSel(clamp({ w: nw, x: c.x - nw / 2, y: c.y - nw / aspect / 2 })) }
  const done = () => { if (!im) return; if (onRect) return onRect({ x: sel.x, y: sel.y, w: sel.w, h }); const H = Math.min(640, Math.round(h)), W = Math.round(H * aspect), c = document.createElement('canvas'); c.width = W; c.height = H
    const x = c.getContext('2d')!; x.imageSmoothingQuality = 'high'; x.drawImage(im, sel.x, sel.y, sel.w, h, 0, 0, W, H); onDone(c.toDataURL('image/png')) }
  return <div className="fixed inset-0 z-[90] flex items-center justify-center fade" style={{ background: 'rgba(2,8,14,.8)', backdropFilter: 'blur(6px)' }}>
    <div className="glass rounded-3xl p-5 flex flex-col gap-3" style={{ width: BOX + 40 }}>
      <div><div className="text-lg font-extrabold">{title}</div><div className="muted text-sm">Двигайте рамку мышью, тяните за угол или крутите колесо, чтобы изменить размер</div></div>
      <div className="relative mx-auto checker rounded-xl overflow-hidden select-none" style={{ width: im ? im.width * k : BOX, height: im ? im.height * k : BOX, touchAction: 'none' }} onPointerMove={move} onPointerUp={() => (drag.current = null)} onWheel={wheel}>
        {im && <><img src={src} draggable={false} style={{ width: im.width * k, height: im.height * k, display: 'block' }} />
          <div className="absolute cursor-move" onPointerDown={down('move')} style={{ left: sel.x * k, top: sel.y * k, width: sel.w * k, height: h * k, boxShadow: '0 0 0 9999px rgba(0,0,0,.55)', outline: '2px solid var(--a2)', borderRadius: 4 }}>
            <div className="absolute inset-0 pointer-events-none" style={{ backgroundImage: 'linear-gradient(rgba(255,255,255,.25) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.25) 1px,transparent 1px)', backgroundSize: '33.33% 33.33%' }} />
            <div className="absolute -right-2 -bottom-2 w-4 h-4 rounded-full cursor-nwse-resize" onPointerDown={down('size')} style={{ background: 'var(--a2)', boxShadow: '0 0 0 2px #fff' }} /></div></>}</div>
      <div className="flex gap-2 justify-end flex-wrap">{extra}<button className="btn" onClick={onCancel}>Отмена</button><button className="btn btn-primary" disabled={!im} onClick={done}>{okText}</button></div>
    </div></div>
}
const loadIm = (s: string) => new Promise<HTMLImageElement>((res, rej) => { const im = new Image(); im.onload = () => res(im); im.onerror = rej; im.src = s })
/** a 512x256 cape from two pictures: the outside and the inside ('mirror' = the outside mirrored and darker, 'dark' = one dark colour) */
export async function buildCape(front: string, back: string, K = 8): Promise<string> {
  const c = document.createElement('canvas'); c.width = 64 * K; c.height = 32 * K; const x = c.getContext('2d')!; x.imageSmoothingQuality = 'high'
  const f = await loadIm(front), W = 10 * K, H = 16 * K
  x.drawImage(f, K, K, W, H)
  if (back === 'mirror' || back === 'dark') { x.save(); x.translate(12 * K + W, K); x.scale(-1, 1); x.drawImage(f, 0, 0, W, H); x.restore(); x.fillStyle = back === 'dark' ? 'rgba(4,10,16,.82)' : 'rgba(0,0,0,.3)'; x.fillRect(12 * K, K, W, H) }
  else x.drawImage(await loadIm(back), 12 * K, K, W, H)
  // edges: the outer pixels of the outside picture stretched over the cape's thickness
  x.drawImage(c, K, K, 1, H, 0, K, K, H); x.drawImage(c, K + W - 1, K, 1, H, 11 * K, K, K, H)
  x.drawImage(c, K, K, W, 1, K, 0, W, K); x.drawImage(c, K, K + H - 1, W, 1, 11 * K, 0, W, K)
  x.drawImage(f, 22 * K, 0, 24 * K, 22 * K); x.drawImage(f, 24 * K, 2 * K, W, 20 * K); x.drawImage(f, 36 * K, 2 * K, W, 20 * K) // elytra take the cape picture
  return c.toDataURL('image/png')
}
/** GIF -> animated cape: frames (up to 24) stacked top to bottom, 256x128 each; the game and the launcher play them */
export function GifCapeMaker({ src, onDone, onCancel, onError }: { src: string; onDone: (png: string, frames: number) => void; onCancel: () => void; onError: (m: string) => void }) {
  const [first, setFirst] = useState(''), [busy, setBusy] = useState(false), frames = useRef<{ bmp: ImageBitmap | VideoFrame; w: number; h: number }[]>([])
  useEffect(() => { (async () => { try {
      const D = (window as any).ImageDecoder; if (!D) return onError('Эта версия лаунчера не умеет читать GIF')
      const blob = await (await fetch(src)).blob(), dec = new D({ data: blob.stream(), type: 'image/gif' }); await dec.tracks.ready
      const n = Math.min(24, dec.tracks.selectedTrack.frameCount), step = Math.max(1, Math.ceil(dec.tracks.selectedTrack.frameCount / 24))
      for (let i = 0; i < n * step && i < dec.tracks.selectedTrack.frameCount; i += step) { const r = await dec.decode({ frameIndex: i }); frames.current.push({ bmp: r.image, w: r.image.displayWidth, h: r.image.displayHeight }) }
      const f = frames.current[0], c = document.createElement('canvas'); c.width = f.w; c.height = f.h; c.getContext('2d')!.drawImage(f.bmp as any, 0, 0); setFirst(c.toDataURL('image/png'))
    } catch { onError('Не удалось прочитать GIF') } })() }, [src])
  if (!first) return <div className="fixed inset-0 z-[90] flex items-center justify-center" style={{ background: 'rgba(2,8,14,.8)' }}><div className="glass rounded-3xl p-6 flex items-center gap-3"><span className="dlspin" />Читаем GIF…</div></div>
  return <CropBox src={first} aspect={10 / 16} title={busy ? 'Собираем кадры…' : 'Область анимации на плаще'} okText="Сделать плащ" onCancel={onCancel} onDone={() => {}} onRect={async (r) => {
    setBusy(true); const W = 40, H = 64, out = document.createElement('canvas'), list = frames.current; out.width = 256; out.height = 128 * list.length; const ox = out.getContext('2d')!
    for (let i = 0; i < list.length; i++) { const c = document.createElement('canvas'); c.width = W; c.height = H; const x = c.getContext('2d')!; x.imageSmoothingQuality = 'high'; x.drawImage(list[i].bmp as any, r.x, r.y, r.w, r.h, 0, 0, W, H)
      const cape = await buildCape(c.toDataURL('image/png'), 'mirror', 4); ox.drawImage(await loadIm(cape), 0, 128 * i) }
    onDone(out.toDataURL('image/png'), list.length) }} />
}
/** any picture -> cape: pick the part for the outside, then the part for the inside */
export function CapeMaker({ src, onDone, onCancel }: { src: string; onDone: (png: string) => void; onCancel: () => void }) {
  const [front, setFront] = useState('')
  if (!front) return <CropBox src={src} aspect={10 / 16} title="Шаг 1 из 2 — внешняя сторона плаща" okText="Дальше" onCancel={onCancel} onDone={setFront} />
  return <CropBox src={src} aspect={10 / 16} title="Шаг 2 из 2 — внутренняя сторона" okText="Готово" onCancel={onCancel} onDone={async (b) => onDone(await buildCape(front, b))}
    extra={<><button className="btn" onClick={async () => onDone(await buildCape(front, 'mirror'))}>Как снаружи</button><button className="btn" onClick={async () => onDone(await buildCape(front, 'dark'))}>Однотонная</button></>} />
}
export default function SkinStudio({ mode, src, skin, cape, slim, onSave, onClose, pickImage }: {
  mode: 'skin' | 'cape'; src: string; skin: string; cape?: string; slim: boolean; onSave: (png: string) => void; onClose: () => void; pickImage: () => Promise<string | null> }) {
  const cv = useRef<HTMLCanvasElement>(null), undo = useRef<ImageData[]>([]), redo = useRef<ImageData[]>([]), raf = useRef(0)
  const [url, setUrl] = useState(''), [k, setK] = useState(1), [tool, setTool] = useState('brush'), [size, setSize] = useState(1), [color, setColor] = useState('#22d3ee')
  const [recent, setRecent] = useState<string[]>(() => { try { return JSON.parse(localStorage.getItem('recentColors') || '[]') } catch { return [] } })
  const [layer, setLayer] = useState<'over' | 'base'>('over'), [guides, setGuides] = useState(true), [d, setD] = useState<Design>(DESIGN), [, force] = useState(0)
  const isCape = mode === 'cape', TW = 64, TH = isCape ? 32 : 64
  const [view2, setView2] = useState<'3d' | 'flat'>('3d'), [zoom, setZoom] = useState(1), [grid, setGrid] = useState(true), [mirror, setMirror] = useState(false), [hover, setHover] = useState<[number, number] | null>(null)
  const faces = isCape ? CAPE_FACES : skinFaces(slim)
  const mask = useRef<Uint8Array>(new Uint8Array(0))
  if (mask.current.length !== TW * TH) { const m = new Uint8Array(TW * TH); for (const f of faces) for (let y = f.r[1]; y < f.r[1] + f.r[3]; y++) for (let x = f.r[0]; x < f.r[0] + f.r[2]; x++) m[y * TW + x] = 1; mask.current = m }
  const faceAt = (u: number, v: number) => faces.find((f) => u >= f.r[0] && u < f.r[0] + f.r[2] && v >= f.r[1] && v < f.r[1] + f.r[3])
  const valid = (px: number, py: number) => mask.current[Math.floor(py / k) * TW + Math.floor(px / k)] === 1
  /** clip drawing to the faces (fillRect / clearRect / strokes respect it) */
  const clipFaces = (x: CanvasRenderingContext2D) => { x.save(); x.beginPath(); for (const f of faces) x.rect(f.r[0] * k, f.r[1] * k, f.r[2] * k, f.r[3] * k); x.clip() }
  const last = useRef<[number, number] | null>(null), shape = useRef<{ base: ImageData; u: number; v: number } | null>(null), shift = useRef(false)
  const ctx = () => cv.current!.getContext('2d', { willReadFrequently: true })!
  const flush = () => { cancelAnimationFrame(raf.current); raf.current = requestAnimationFrame(() => setUrl(cv.current!.toDataURL('image/png'))) }

  useEffect(() => { // load the texture being edited
    const im = new Image()
    im.onload = () => { const kk = Math.max(1, Math.round(im.width / 64)); setK(kk); const c = cv.current!; c.width = TW * kk; c.height = TH * kk; const x = ctx(); x.imageSmoothingEnabled = false; x.clearRect(0, 0, c.width, c.height); x.drawImage(im, 0, 0, c.width, c.height); flush() }
    im.onerror = () => { const c = cv.current!; c.width = TW; c.height = TH; if (isCape) compose(DESIGN, 1, false); flush() }
    if (src) im.src = src; else im.onerror(new Event('x') as any)
  }, [])

  const snap = () => { undo.current.push(ctx().getImageData(0, 0, cv.current!.width, cv.current!.height)); if (undo.current.length > 60) undo.current.shift(); redo.current = []; force((x) => x + 1) }
  const history = (from: React.MutableRefObject<ImageData[]>, to: React.MutableRefObject<ImageData[]>) => { const p = from.current.pop(); if (!p) return; to.current.push(ctx().getImageData(0, 0, cv.current!.width, cv.current!.height)); if (p.width !== cv.current!.width) { cv.current!.width = p.width; cv.current!.height = p.height; setK(p.width / 64) } ctx().putImageData(p, 0, 0); flush(); force((x) => x + 1) }
  useEffect(() => { const f = (e: KeyboardEvent) => { if (e.ctrlKey && e.code === 'KeyZ') { e.preventDefault(); e.shiftKey ? history(redo, undo) : history(undo, redo) } else if (e.ctrlKey && e.code === 'KeyY') { e.preventDefault(); history(redo, undo) } else if (e.code === 'Escape') onClose()
      else if (!e.ctrlKey && !(e.target instanceof HTMLInputElement)) { const t = TOOLS.find((x) => 'Key' + x[3] === e.code); if (t) setTool(t[0])
        if (e.code === 'BracketLeft') setSize((s) => Math.max(1, s - 1)); if (e.code === 'BracketRight') setSize((s) => Math.min(4, s + 1)); if (e.code === 'KeyM') setMirror((m) => !m) } }
    const sh = (e: KeyboardEvent) => { shift.current = e.shiftKey }; addEventListener('keydown', sh); addEventListener('keyup', sh)
    addEventListener('keydown', f); return () => { removeEventListener('keydown', f); removeEventListener('keydown', sh); removeEventListener('keyup', sh) } }, [])
  const useColor = (c: string) => { setColor(c); if (tool === 'eraser' || tool === 'picker') setTool('brush') }
  const remember = (c: string) => setRecent((r) => { const n = [c, ...r.filter((x) => x !== c)].slice(0, 12); try { localStorage.setItem('recentColors', JSON.stringify(n)) } catch {} return n })

  /** mirror point inside its face (left <-> right), for symmetric faces and capes */
  const mirrored = (u: number, v: number): [number, number] | null => { const f = faceAt(u, v); if (!f) return null; return [f.r[0] + f.r[2] - 1 - (u - f.r[0]) + 0.0001, v] }
  const rgb = (c: string) => { const n = parseInt(c.slice(1), 16); return [(n >> 16) & 255, (n >> 8) & 255, n & 255] }
  /** one brush dab (size x size texture pixels) at (u, v) */
  const stamp = (x: CanvasRenderingContext2D, u: number, v: number) => {
    const px = Math.floor(u) * k, py = Math.floor(v) * k, s = size * k, x0 = px - Math.floor((size - 1) / 2) * k, y0 = py - Math.floor((size - 1) / 2) * k
    if (tool === 'eraser') { x.clearRect(x0, y0, s, s); return }
    if (tool === 'brush') { x.fillStyle = color; x.fillRect(x0, y0, s, s); return }
    const im = x.getImageData(x0, y0, s, s), a = im.data, [r, g, b] = rgb(color)
    for (let i = 0; i < a.length; i += 4) { const qx = x0 + (i / 4) % s, qy = y0 + Math.floor(i / 4 / s); if (!valid(qx, qy)) continue
      if (tool === 'noise') { const j = (Math.random() - 0.5) * 46; a[i] = Math.max(0, Math.min(255, r + j)); a[i + 1] = Math.max(0, Math.min(255, g + j)); a[i + 2] = Math.max(0, Math.min(255, b + j)); a[i + 3] = 255 }
      else if (a[i + 3]) { const f = tool === 'lighten' ? 1.12 : 0.88; for (const c of [0, 1, 2]) a[i + c] = Math.min(255, a[i + c] * f + (f > 1 ? 4 : 0)) } }
    x.putImageData(im, x0, y0)
  }
  /** paint at texture coordinates (u, v) in 64-pixel units */
  const paintAt = (u: number, v: number, first: boolean) => {
    if (u < 0 || v < 0 || u >= TW || v >= TH) return
    const x = ctx(), px = Math.floor(u * k), py = Math.floor(v * k)
    if (first && !faceAt(u, v)) return // started outside the unwrap: nothing to paint there
    if (first) snap()
    if (tool === 'picker') { const p = x.getImageData(px, py, 1, 1).data; if (p[3]) { const c = '#' + [p[0], p[1], p[2]].map((n) => n.toString(16).padStart(2, '0')).join(''); setColor(c); remember(c) } setTool('brush'); undo.current.pop(); return }
    if (tool === 'fill') { if (first) { flood(px, py); const m = mirror && mirrored(u, v); if (m) flood(Math.floor(m[0] * k), py) } flush(); return }
    if (tool === 'line' || tool === 'rect') { // shapes: drawn again from the start point on every move, kept on release
      if (first) shape.current = { base: x.getImageData(0, 0, cv.current!.width, cv.current!.height), u, v }
      const sh = shape.current; if (!sh) return; x.putImageData(sh.base, 0, 0); clipFaces(x); x.fillStyle = color
      const pts = (u0: number, v0: number, u1: number, v1: number) => { const n = Math.max(Math.abs(Math.floor(u1) - Math.floor(u0)), Math.abs(Math.floor(v1) - Math.floor(v0)), 1); const o: [number, number][] = []; for (let i = 0; i <= n; i++) o.push([Math.floor(u0 + (u1 - u0) * i / n), Math.floor(v0 + (v1 - v0) * i / n)]); return o }
      const draw = (u0: number, v0: number, u1: number, v1: number) => {
        if (tool === 'line') for (const [a, b] of pts(u0, v0, u1, v1)) x.fillRect(a * k, b * k, size * k, size * k)
        else { const l = Math.floor(Math.min(u0, u1)), t = Math.floor(Math.min(v0, v1)), r2 = Math.floor(Math.max(u0, u1)), b2 = Math.floor(Math.max(v0, v1))
          if (shift.current) x.fillRect(l * k, t * k, (r2 - l + 1) * k, (b2 - t + 1) * k)
          else { x.fillRect(l * k, t * k, (r2 - l + 1) * k, k); x.fillRect(l * k, b2 * k, (r2 - l + 1) * k, k); x.fillRect(l * k, t * k, k, (b2 - t + 1) * k); x.fillRect(r2 * k, t * k, k, (b2 - t + 1) * k) } } }
      draw(sh.u, sh.v, u, v); const m0 = mirror && mirrored(sh.u, sh.v), m1 = mirror && mirrored(u, v); if (m0 && m1) draw(m0[0], m0[1], m1[0], m1[1])
      x.restore(); if (first) remember(color); flush(); return }
    clipFaces(x)
    const from = first || !last.current ? [u, v] : last.current, n = Math.max(1, Math.ceil(Math.hypot(u - from[0], v - from[1]) * 2)) // no gaps when the mouse moves fast
    for (let i = 1; i <= n || i === 1; i++) { const t = n ? i / n : 1, uu = from[0] + (u - from[0]) * t, vv = from[1] + (v - from[1]) * t
      stamp(x, uu, vv); const m = mirror && mirrored(uu, vv); if (m) stamp(x, m[0], m[1]); if (i >= n) break }
    x.restore(); last.current = [u, v]; if (first && tool === 'brush') remember(color)
    flush()
  }
  const flood = (sx: number, sy: number) => {
    const c = cv.current!, x = ctx(), im = x.getImageData(0, 0, c.width, c.height), a = im.data, W = c.width, H = c.height, i0 = (sy * W + sx) * 4, t = [a[i0], a[i0 + 1], a[i0 + 2], a[i0 + 3]]
    const n = parseInt(color.slice(1), 16), rgb = [(n >> 16) & 255, (n >> 8) & 255, n & 255]
    if (t[0] === rgb[0] && t[1] === rgb[1] && t[2] === rgb[2] && t[3] === 255) return
    const st = [sx, sy]
    while (st.length) { const py = st.pop()!, px = st.pop()!; if (px < 0 || py < 0 || px >= W || py >= H || !valid(px, py)) continue; const i = (py * W + px) * 4
      if (a[i] !== t[0] || a[i + 1] !== t[1] || a[i + 2] !== t[2] || a[i + 3] !== t[3]) continue
      a[i] = rgb[0]; a[i + 1] = rgb[1]; a[i + 2] = rgb[2]; a[i + 3] = 255; st.push(px + 1, py, px - 1, py, px, py + 1, px, py - 1) }
    x.putImageData(im, 0, 0); remember(color)
  }
  const onSkinHit = (h: Hit) => { if (layer === 'base' && h.layer === 'over') return; paintAt(h.u, h.v, h.first) }
  const map = (e: React.PointerEvent, first: boolean) => { const r = cv.current!.getBoundingClientRect(); paintAt(Math.floor(((e.clientX - r.left) / r.width) * TW * 1000) / 1000, Math.floor(((e.clientY - r.top) / r.height) * TH * 1000) / 1000, first) }
  /** the big flat unwrap: same as the small one, plus the hovered pixel */
  const flat = (e: React.PointerEvent, first: boolean, hoverOnly = false) => { const r = (e.currentTarget as Element).getBoundingClientRect(), u = ((e.clientX - r.left) / r.width) * TW, v = ((e.clientY - r.top) / r.height) * TH
    setHover(faceAt(u, v) ? [Math.floor(u), Math.floor(v)] : null); if (!hoverOnly) paintAt(u, v, first) }

  // ---------------------------------------------------------------- cape designer
  const loadImg = (s: string) => new Promise<HTMLImageElement>((res, rej) => { const im = new Image(); im.onload = () => res(im); im.onerror = rej; im.src = s })
  async function compose(des: Design, kk: number, withSnap = true) {
    if (withSnap) snap()
    const c = cv.current!; c.width = 64 * kk; c.height = 32 * kk; const x = ctx(); x.imageSmoothingEnabled = kk > 1; x.clearRect(0, 0, c.width, c.height)
    const R = (u: number, v: number, w: number, h: number) => [u * kk, v * kk, w * kk, h * kk] as const
    const paintBg = (u: number, v: number, w: number, h: number, dark = 1) => { const [X, Y, W, H] = R(u, v, w, h)
      if (des.bg === 'gradient') { const g = x.createLinearGradient(0, Y, 0, Y + H); g.addColorStop(0, des.c1); g.addColorStop(1, des.c2); x.fillStyle = g } else x.fillStyle = des.c1
      x.fillRect(X, Y, W, H); if (dark < 1) { x.fillStyle = `rgba(0,0,0,${1 - dark})`; x.fillRect(X, Y, W, H) } }
    paintBg(1, 1, 10, 16); paintBg(12, 1, 10, 16, 0.78); paintBg(0, 1, 1, 16, 0.7); paintBg(11, 1, 1, 16, 0.7); paintBg(1, 0, 10, 1, 0.85); paintBg(11, 0, 10, 1, 0.6)
    if (des.img) { const im = await loadImg(des.img).catch(() => null)
      if (im) for (const [u, mirror] of des.back ? [[1, false], [12, true]] as [number, boolean][] : [[1, false]] as [number, boolean][]) {
        const [X, Y, W, H] = R(u, 1, 10, 16); x.save(); x.beginPath(); x.rect(X, Y, W, H); x.clip()
        const base = des.fit === 'cover' ? Math.max(W / im.width, H / im.height) : Math.min(W / im.width, H / im.height), sc = base * des.scale / 100
        x.translate(X + W / 2 + des.ox / 100 * W, Y + H / 2 + des.oy / 100 * H); if (mirror) x.scale(-1, 1); x.rotate(des.rot * Math.PI / 180)
        x.imageSmoothingEnabled = true; x.imageSmoothingQuality = 'high'; x.drawImage(im, -im.width * sc / 2, -im.height * sc / 2, im.width * sc, im.height * sc); x.restore() } }
    if (des.border > 0) { const b = Math.max(1, Math.round(des.border * kk * (kk > 1 ? 0.5 : 1))); x.fillStyle = des.borderColor
      for (const u of [1, 12]) { const [X, Y, W, H] = R(u, 1, 10, 16); x.fillRect(X, Y, W, b); x.fillRect(X, Y + H - b, W, b); x.fillRect(X, Y, b, H); x.fillRect(X + W - b, Y, b, H) } }
    setK(kk); flush()
  }
  const setRes = (kk: number) => { snap(); const c = cv.current!, old = document.createElement('canvas'); old.width = c.width; old.height = c.height; old.getContext('2d')!.drawImage(c, 0, 0)
    c.width = 64 * kk; c.height = 32 * kk; const x = ctx(); x.imageSmoothingEnabled = false; x.drawImage(old, 0, 0, c.width, c.height); setK(kk); flush() }
  const upd = (p: Partial<Design>) => setD((o) => ({ ...o, ...p }))
  const [cropSrc, setCropSrc] = useState(''), [makeSrc, setMakeSrc] = useState('')

  const view = { showOver: !isCape, dimOver: layer === 'base', showCape: isCape, animate: false }
  const guideList = isCape ? CAPE_GUIDES : SKIN_GUIDES, mapW = isCape ? 296 : 256, mapH = mapW * TH / TW
  return <div className="fixed inset-0 z-[80] flex items-center justify-center fade" style={{ background: 'rgba(2,8,14,.72)', backdropFilter: 'blur(8px)' }}>
    <div className="glass rounded-3xl p-4 flex flex-col gap-3" style={{ width: 1240, maxWidth: '97vw', height: 760, maxHeight: '96vh' }}>
      <div className="flex items-center gap-3"><div className="text-xl font-extrabold">{isCape ? 'Редактор плаща' : 'Редактор скина'}</div><span className="muted text-sm">Рисуйте прямо на модели или на развёртке справа</span>
        <div className="ml-auto flex gap-2"><button className="btn !p-2" title="Отменить (Ctrl+Z)" disabled={!undo.current.length} onClick={() => history(undo, redo)}><Icon n="undo" s={18} /></button>
          <button className="btn !p-2" title="Повторить (Ctrl+Y)" disabled={!redo.current.length} onClick={() => history(redo, undo)} style={{ transform: 'scaleX(-1)' }}><Icon n="undo" s={18} /></button>
          <button className="btn !p-2" title="Закрыть (Esc)" onClick={onClose}><Icon n="close" s={18} /></button></div></div>
      <div className="flex gap-3 flex-1 min-h-0">
        <div className="flex flex-col gap-1.5 overflow-auto">{TOOLS.map(([id, ic, label, key]) => <button key={id} title={label + ' (' + key + ')'} onClick={() => setTool(id)} className={'btn !p-2.5 ' + (tool === id ? 'btn-on' : '')}><Icon n={ic} s={20} /></button>)}
          <button title="Зеркально: рисует сразу на левой и правой половине (M)" onClick={() => setMirror(!mirror)} className={'btn !p-2.5 ' + (mirror ? 'btn-on' : '')}><Icon n="mirror" s={20} /></button>
          <div className="label mt-1 text-center">Кисть</div>{[1, 2, 3, 4].map((n) => <button key={n} title={'Размер ' + n + ' ([ и ])'} onClick={() => setSize(n)} className={'btn !p-0 h-8 flex items-center justify-center ' + (size === n ? 'btn-on' : '')}><span className="rounded-sm" style={{ width: 3 + n * 3.5, height: 3 + n * 3.5, background: 'currentColor' }} /></button>)}</div>
        <div className="flex-1 min-w-0 flex flex-col gap-2">
          <div className="flex items-center gap-2"><div className="seg"><button className={view2 === '3d' ? 'on' : ''} onClick={() => setView2('3d')}>3D-модель</button><button className={view2 === 'flat' ? 'on' : ''} onClick={() => setView2('flat')}>Развёртка крупно</button></div>
            {view2 === 'flat' && <><button className={'pill ' + (grid ? 'on' : '')} onClick={() => setGrid(!grid)}><Icon n="grid" s={13} />Сетка</button>
              <span className="muted text-xs flex items-center gap-1 ml-1"><Icon n="zoom" s={14} />{Math.round(zoom * 100)}%</span><input type="range" min={1} max={4} step={0.25} value={zoom} onChange={(e) => setZoom(+e.target.value)} style={{ width: 120 }} /></>}
            <span className="muted text-xs ml-auto">{hover ? `пиксель ${hover[0]}, ${hover[1]}` : view2 === '3d' ? 'Левая кнопка — рисовать, правая — вращать' : 'Колесо — масштаб'}</span></div>
          {view2 === '3d' ? <Skin3D skin={isCape ? skin : url || src} cape={isCape ? url : undefined} slim={slim} paint={isCape ? 'cape' : 'skin'} onSkinHit={onSkinHit} onCapeHit={(h) => paintAt(h.u, h.v, h.first)} view={view} height={600} scale={isCape ? 9 : 8} />
          : <div className="flex-1 min-h-0 overflow-auto rounded-2xl" style={{ background: 'rgba(0,0,0,.25)' }} onWheel={(e) => setZoom((z) => Math.max(1, Math.min(4, +(z * (e.deltaY < 0 ? 1.15 : 0.87)).toFixed(2))))}>
            <div className="relative checker m-auto" style={{ width: (isCape ? 820 : 560) * zoom, height: (isCape ? 410 : 560) * zoom, cursor: tool === 'picker' ? 'copy' : 'crosshair', touchAction: 'none' }}
              onPointerDown={(e) => { (e.currentTarget as Element).setPointerCapture(e.pointerId); flat(e, true) }} onPointerMove={(e) => { flat(e, false, !(e.buttons & 1) || tool === 'fill' || tool === 'picker') }} onPointerUp={() => { last.current = null; shape.current = null }} onPointerLeave={() => setHover(null)}>
              <img src={url} draggable={false} className="absolute inset-0 w-full h-full" style={{ imageRendering: k > 1 ? 'auto' : 'pixelated' }} />
              <svg className="absolute inset-0 w-full h-full pointer-events-none" viewBox={`0 0 ${TW} ${TH}`} preserveAspectRatio="none">
                <path fillRule="evenodd" fill="rgba(4,10,16,.72)" d={`M0 0H${TW}V${TH}H0Z ` + faces.map((f) => `M${f.r[0]} ${f.r[1]}h${f.r[2]}v${f.r[3]}h${-f.r[2]}Z`).join(' ')} />
                {faces.map((f, i) => <rect key={i} x={f.r[0]} y={f.r[1]} width={f.r[2]} height={f.r[3]} fill="none" stroke={f.over ? 'rgba(167,139,250,.7)' : 'rgba(34,211,238,.55)'} strokeWidth={0.12} />)}
                {hover && <rect x={hover[0]} y={hover[1]} width={size} height={size} fill="none" stroke="#fff" strokeWidth={0.15} />}</svg>
              {grid && <div className="absolute inset-0 pointer-events-none" style={{ backgroundImage: 'linear-gradient(rgba(255,255,255,.09) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.09) 1px,transparent 1px)', backgroundSize: `${100 / TW}% ${100 / TH}%` }} />}
            </div></div>}</div>
        <div className="flex flex-col gap-3 overflow-auto pr-1" style={{ width: 312 }}>
          <div className="card rounded-2xl p-3 flex flex-col items-center gap-2"><ColorWheel value={color} onChange={useColor} size={200} />
            <div className="flex gap-2 items-center w-full"><span className="w-9 h-9 rounded-xl shrink-0" style={{ background: color, boxShadow: 'inset 0 0 0 1px var(--line)' }} />
              <input className="flex-1 min-w-0 font-mono text-sm" value={color} onChange={(e) => /^#[0-9a-f]{6}$/i.test(e.target.value) ? useColor(e.target.value.toLowerCase()) : setColor(e.target.value)} /></div>
            <div className="grid grid-cols-10 gap-1 w-full">{PALETTE.map((c) => <button key={c} onClick={() => useColor(c)} className="aspect-square rounded-md" style={{ background: c, outline: color === c ? '2px solid var(--a1)' : '1px solid var(--line)', outlineOffset: color === c ? 1 : 0 }} />)}</div>
            {recent.length > 0 && <><div className="label self-start">Недавние</div><div className="grid grid-cols-12 gap-1 w-full">{recent.map((c) => <button key={c} onClick={() => useColor(c)} className="aspect-square rounded" style={{ background: c, outline: '1px solid var(--line)' }} />)}</div></>}</div>
          {!isCape && <div className="card rounded-2xl p-3"><div className="label mb-2">Слой</div><div className="seg w-full"><button className={'flex-1 ' + (layer === 'over' ? 'on' : '')} onClick={() => setLayer('over')}>Внешний</button><button className={'flex-1 ' + (layer === 'base' ? 'on' : '')} onClick={() => setLayer('base')}>Основной</button></div>
            <p className="muted text-xs mt-2">«Основной» — кожа и одежда под вторым слоем. «Внешний» — шапка, куртка, рукава (может быть прозрачным).</p></div>}
          <div className="card rounded-2xl p-3"><div className="flex items-center justify-between mb-2"><span className="label">Развёртка</span><button className={'pill !py-0.5 ' + (guides ? 'on' : '')} onClick={() => setGuides(!guides)}>Подписи</button></div>
            <div className="relative checker rounded-lg overflow-hidden" style={{ width: mapW, height: mapH }}>
              <canvas ref={cv} width={TW} height={TH} style={{ width: mapW, height: mapH, imageRendering: k > 1 ? 'auto' : 'pixelated', cursor: 'crosshair', touchAction: 'none' }}
                onPointerDown={(e) => { (e.target as Element).setPointerCapture(e.pointerId); map(e, true) }} onPointerUp={() => { last.current = null; shape.current = null }} onPointerMove={(e) => (e.buttons & 1) && tool !== 'fill' && tool !== 'picker' && map(e, false)} />
              {guides && guideList.map(([x, y, w, h, l], i) => <div key={i} className="absolute pointer-events-none text-[9px] text-white/90" style={{ left: x / TW * mapW, top: y / TH * mapH, width: w / TW * mapW, height: h / TH * mapH, border: '1px dashed rgba(255,255,255,.45)' }}>{l && <span style={{ background: 'rgba(0,0,0,.5)', padding: '0 3px' }}>{l}</span>}</div>)}</div></div>
          {isCape && <div className="card rounded-2xl p-3 flex flex-col gap-2.5">
            <div className="font-bold">Конструктор плаща</div>
            <div className="label">Разрешение</div><div className="flex flex-col gap-1">{RES.map(([kk, l]) => <button key={kk} className={'row !py-1.5 ' + (k === kk ? '!bg-[var(--chip)] font-semibold' : '')} onClick={() => setRes(kk)}>{k === kk ? <Icon n="check" s={14} /> : <span className="w-[14px]" />}{l}</button>)}</div>
            <div className="label">Фон</div><div className="seg"><button className={d.bg === 'solid' ? 'on' : ''} onClick={() => upd({ bg: 'solid' })}>Цвет</button><button className={d.bg === 'gradient' ? 'on' : ''} onClick={() => upd({ bg: 'gradient' })}>Градиент</button></div>
            <div className="flex gap-2 items-center"><ColorPick value={d.c1} set={(v) => upd({ c1: v })} />{d.bg === 'gradient' && <ColorPick value={d.c2} set={(v) => upd({ c2: v })} />}
              <button className="pill" onClick={() => upd({ c1: color })}>Текущий цвет</button></div>
            <div className="label">Картинка на плаще</div>
            <div className="flex gap-2"><button className="btn flex-1 flex items-center justify-center gap-2" onClick={async () => { const im = await pickImage(); if (im) setCropSrc(im) }}><Icon n="image" s={16} />{d.img ? 'Другая' : 'Выбрать'}</button>{d.img && <button className="btn" onClick={() => upd({ img: '' })}><Icon n="trash" s={16} /></button>}</div>
            {d.img && <><div className="seg"><button className={d.fit === 'cover' ? 'on' : ''} onClick={() => upd({ fit: 'cover' })}>Заполнить</button><button className={d.fit === 'contain' ? 'on' : ''} onClick={() => upd({ fit: 'contain' })}>Вписать</button></div>
              {([['scale', 'Масштаб', 20, 300, '%'], ['ox', 'Сдвиг X', -100, 100, ''], ['oy', 'Сдвиг Y', -100, 100, ''], ['rot', 'Поворот', -180, 180, '°']] as [keyof Design, string, number, number, string][]).map(([key, l, mn, mx, u]) =>
                <label key={key} className="text-sm"><div className="flex justify-between"><span>{l}</span><span className="muted">{d[key] as number}{u}</span></div><input type="range" className="w-full" min={mn} max={mx} value={d[key] as number} onChange={(e) => upd({ [key]: +e.target.value } as any)} /></label>)}
              <Check on={d.back} set={(v) => upd({ back: v })} label="И на внутренней стороне" /></>}
            <div className="label">Рамка</div><div className="flex gap-2 items-center"><input type="range" className="flex-1" min={0} max={3} value={d.border} onChange={(e) => upd({ border: +e.target.value })} /><ColorPick value={d.borderColor} set={(v) => upd({ borderColor: v })} size="sm" /></div>
            <button className="btn btn-primary" onClick={() => compose(d, k === 1 && d.img ? 8 : k)}>Применить дизайн</button>
            <p className="muted text-xs">Картинка лучше всего смотрится в HD: при наложении на классический плащ разрешение само переключится на 512×256. После применения можно дорисовать детали кистью.</p></div>}
        </div>
      </div>
      {makeSrc && <CapeMaker src={makeSrc} onCancel={() => setMakeSrc('')} onDone={(png) => { setMakeSrc(''); const im = new Image(); im.onload = () => { snap(); const c = cv.current!; c.width = 512; c.height = 256; const x = ctx(); x.clearRect(0, 0, 512, 256); x.drawImage(im, 0, 0); setK(8); flush() }; im.src = png }} />}
      {cropSrc && <CropBox src={cropSrc} aspect={10 / 16} onCancel={() => setCropSrc('')} onDone={(png) => { setCropSrc(''); const nd = { ...d, img: png, fit: 'cover' as const, scale: 100, ox: 0, oy: 0, rot: 0 }; setD(nd); compose(nd, Math.max(k, 8)) }} />}
      <div className="flex gap-2 justify-end items-center"><span className="muted text-sm mr-auto">{isCape ? 'Плащ увидят все игроки AsteriaLauncher.' : 'Скин увидят все игроки AsteriaLauncher; для лицензии — ещё и на всех серверах (кнопка «Применить»).'}</span>
        <button className="btn flex items-center gap-2" title={isCape ? 'Готовая развёртка плаща (64×32 или HD) или любая картинка — выберете область' : 'Готовый скин 64×64 или HD'} onClick={async () => { const p = await pickImage(); if (!p) return
          const im = new Image(); im.onload = () => { if (Math.abs(im.width / im.height - TW / TH) < 0.02) { snap(); const kk = Math.max(1, Math.round(im.width / 64)), c = cv.current!; c.width = TW * kk; c.height = TH * kk; const x = ctx(); x.imageSmoothingEnabled = false; x.clearRect(0, 0, c.width, c.height); x.drawImage(im, 0, 0, c.width, c.height); setK(kk); flush() } else if (isCape) setMakeSrc(p) }; im.src = p }}><Icon n="upload" s={16} />Загрузить PNG</button>
        <a className="btn flex items-center gap-2" href={url} download={mode + '.png'} title="Сохранить файл на компьютер"><Icon n="download" s={16} />Скачать</a>
        <button className="btn" onClick={onClose}>Отмена</button><button className="btn btn-primary" onClick={() => onSave(cv.current!.toDataURL('image/png'))}>Сохранить</button></div>
    </div>
  </div>
}
