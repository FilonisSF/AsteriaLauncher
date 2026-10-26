import { useEffect, useRef, useState } from 'react'
import Skin3D, { Hit } from './Skin3D'
import ColorWheel from './ColorWheel'
import { Check, ColorPick } from './ui'
import { Icon } from './Icons'

const SKIN_GUIDES: [number, number, number, number, string][] = [[0, 0, 32, 16, 'Голова'], [32, 0, 32, 16, 'Шапка'], [16, 16, 24, 16, 'Тело'], [16, 32, 24, 16, 'Куртка'], [40, 16, 16, 16, 'Пр. рука'], [40, 32, 16, 16, 'Рукав'],
  [0, 16, 16, 16, 'Пр. нога'], [0, 32, 16, 16, 'Штанина'], [16, 48, 16, 16, 'Лев. нога'], [0, 48, 16, 16, 'Штанина'], [32, 48, 16, 16, 'Лев. рука'], [48, 48, 16, 16, 'Рукав']]
const CAPE_GUIDES: [number, number, number, number, string][] = [[1, 1, 10, 16, 'Снаружи'], [12, 1, 10, 16, 'Изнутри'], [0, 1, 1, 16, ''], [11, 1, 1, 16, ''], [1, 0, 10, 1, ''], [11, 0, 10, 1, '']]
const PALETTE = ['#000000', '#3f3f46', '#a1a1aa', '#ffffff', '#7f1d1d', '#ef4444', '#f97316', '#facc15', '#65a30d', '#22c55e', '#0d9488', '#22d3ee', '#0ea5e9', '#1d4ed8', '#7c3aed', '#c026d3', '#ec4899', '#78350f', '#c68642', '#f1c27d']
const TOOLS: [string, string, string][] = [['brush', 'pencil', 'Кисть'], ['eraser', 'eraser', 'Ластик'], ['fill', 'bucket', 'Заливка'], ['picker', 'picker', 'Пипетка'], ['lighten', 'sun', 'Осветлить'], ['darken', 'moon', 'Затемнить']]
const RES: [number, string][] = [[1, '64×32 · классика'], [8, '512×256 · HD'], [16, '1024×512 · HD+']]

type Design = { bg: 'solid' | 'gradient'; c1: string; c2: string; img: string; scale: number; ox: number; oy: number; rot: number; fit: 'cover' | 'contain'; back: boolean; border: number; borderColor: string }
const DESIGN: Design = { bg: 'gradient', c1: '#0b1f3a', c2: '#0e7490', img: '', scale: 100, ox: 0, oy: 0, rot: 0, fit: 'cover', back: false, border: 1, borderColor: '#22d3ee' }

/** Skin / cape editor: paint right on the 3D model or on the 2D unwrap; cape designer (background, image, border, HD). */
export default function SkinStudio({ mode, src, skin, cape, slim, onSave, onClose, pickImage }: {
  mode: 'skin' | 'cape'; src: string; skin: string; cape?: string; slim: boolean; onSave: (png: string) => void; onClose: () => void; pickImage: () => Promise<string | null> }) {
  const cv = useRef<HTMLCanvasElement>(null), undo = useRef<ImageData[]>([]), redo = useRef<ImageData[]>([]), raf = useRef(0)
  const [url, setUrl] = useState(''), [k, setK] = useState(1), [tool, setTool] = useState('brush'), [size, setSize] = useState(1), [color, setColor] = useState('#22d3ee')
  const [recent, setRecent] = useState<string[]>(() => { try { return JSON.parse(localStorage.getItem('recentColors') || '[]') } catch { return [] } })
  const [layer, setLayer] = useState<'over' | 'base'>('over'), [guides, setGuides] = useState(true), [d, setD] = useState<Design>(DESIGN), [, force] = useState(0)
  const isCape = mode === 'cape', TW = 64, TH = isCape ? 32 : 64
  const ctx = () => cv.current!.getContext('2d', { willReadFrequently: true })!
  const flush = () => { cancelAnimationFrame(raf.current); raf.current = requestAnimationFrame(() => setUrl(cv.current!.toDataURL('image/png'))) }

  useEffect(() => { // load the texture being edited
    const im = new Image()
    im.onload = () => { const kk = isCape ? Math.max(1, Math.round(im.width / 64)) : 1; setK(kk); const c = cv.current!; c.width = TW * kk; c.height = TH * kk; const x = ctx(); x.imageSmoothingEnabled = false; x.clearRect(0, 0, c.width, c.height); x.drawImage(im, 0, 0, c.width, c.height); flush() }
    im.onerror = () => { const c = cv.current!; c.width = TW; c.height = TH; if (isCape) compose(DESIGN, 1, false); flush() }
    if (src) im.src = src; else im.onerror(new Event('x') as any)
  }, [])

  const snap = () => { undo.current.push(ctx().getImageData(0, 0, cv.current!.width, cv.current!.height)); if (undo.current.length > 60) undo.current.shift(); redo.current = []; force((x) => x + 1) }
  const history = (from: React.MutableRefObject<ImageData[]>, to: React.MutableRefObject<ImageData[]>) => { const p = from.current.pop(); if (!p) return; to.current.push(ctx().getImageData(0, 0, cv.current!.width, cv.current!.height)); if (p.width !== cv.current!.width) { cv.current!.width = p.width; cv.current!.height = p.height; setK(p.width / 64) } ctx().putImageData(p, 0, 0); flush(); force((x) => x + 1) }
  useEffect(() => { const f = (e: KeyboardEvent) => { if (e.ctrlKey && e.code === 'KeyZ') { e.preventDefault(); e.shiftKey ? history(redo, undo) : history(undo, redo) } else if (e.ctrlKey && e.code === 'KeyY') { e.preventDefault(); history(redo, undo) } else if (e.code === 'Escape') onClose() }
    addEventListener('keydown', f); return () => removeEventListener('keydown', f) }, [])
  const useColor = (c: string) => { setColor(c); if (tool === 'eraser' || tool === 'picker') setTool('brush') }
  const remember = (c: string) => setRecent((r) => { const n = [c, ...r.filter((x) => x !== c)].slice(0, 12); try { localStorage.setItem('recentColors', JSON.stringify(n)) } catch {} return n })

  /** paint at texture coordinates (u, v) in 64-pixel units */
  const paintAt = (u: number, v: number, first: boolean) => {
    const x = ctx(), px = Math.floor(u * k), py = Math.floor(v * k), s = size * k, x0 = px - Math.floor((size - 1) / 2) * k, y0 = py - Math.floor((size - 1) / 2) * k
    if (first) snap()
    if (tool === 'picker') { const p = x.getImageData(px, py, 1, 1).data; if (p[3]) { const c = '#' + [p[0], p[1], p[2]].map((n) => n.toString(16).padStart(2, '0')).join(''); setColor(c); remember(c) } setTool('brush'); undo.current.pop(); return }
    if (tool === 'fill') { if (first) flood(px, py); flush(); return }
    if (tool === 'eraser') x.clearRect(x0, y0, s, s)
    else if (tool === 'lighten' || tool === 'darken') {
      const im = x.getImageData(x0, y0, s, s), a = im.data, f = tool === 'lighten' ? 1.12 : 0.88
      for (let i = 0; i < a.length; i += 4) if (a[i + 3]) { a[i] = Math.min(255, a[i] * f + (f > 1 ? 4 : 0)); a[i + 1] = Math.min(255, a[i + 1] * f + (f > 1 ? 4 : 0)); a[i + 2] = Math.min(255, a[i + 2] * f + (f > 1 ? 4 : 0)) }
      x.putImageData(im, x0, y0)
    } else { x.fillStyle = color; x.fillRect(x0, y0, s, s); if (first) remember(color) }
    flush()
  }
  const flood = (sx: number, sy: number) => {
    const c = cv.current!, x = ctx(), im = x.getImageData(0, 0, c.width, c.height), a = im.data, W = c.width, H = c.height, i0 = (sy * W + sx) * 4, t = [a[i0], a[i0 + 1], a[i0 + 2], a[i0 + 3]]
    const n = parseInt(color.slice(1), 16), rgb = [(n >> 16) & 255, (n >> 8) & 255, n & 255]
    if (t[0] === rgb[0] && t[1] === rgb[1] && t[2] === rgb[2] && t[3] === 255) return
    const st = [sx, sy]
    while (st.length) { const py = st.pop()!, px = st.pop()!; if (px < 0 || py < 0 || px >= W || py >= H) continue; const i = (py * W + px) * 4
      if (a[i] !== t[0] || a[i + 1] !== t[1] || a[i + 2] !== t[2] || a[i + 3] !== t[3]) continue
      a[i] = rgb[0]; a[i + 1] = rgb[1]; a[i + 2] = rgb[2]; a[i + 3] = 255; st.push(px + 1, py, px - 1, py, px, py + 1, px, py - 1) }
    x.putImageData(im, 0, 0); remember(color)
  }
  const onSkinHit = (h: Hit) => { if (layer === 'base' && h.layer === 'over') return; paintAt(h.u, h.v, h.first) }
  const map = (e: React.PointerEvent, first: boolean) => { const r = cv.current!.getBoundingClientRect(); paintAt(Math.floor(((e.clientX - r.left) / r.width) * TW * 1000) / 1000, Math.floor(((e.clientY - r.top) / r.height) * TH * 1000) / 1000, first) }

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

  const view = { showOver: !isCape, dimOver: layer === 'base', showCape: isCape, animate: false }
  const guideList = isCape ? CAPE_GUIDES : SKIN_GUIDES, mapW = isCape ? 296 : 256, mapH = mapW * TH / TW
  return <div className="fixed inset-0 z-[80] flex items-center justify-center fade" style={{ background: 'rgba(2,8,14,.72)', backdropFilter: 'blur(8px)' }}>
    <div className="glass rounded-3xl p-4 flex flex-col gap-3" style={{ width: 1240, maxWidth: '97vw', height: 760, maxHeight: '96vh' }}>
      <div className="flex items-center gap-3"><div className="text-xl font-extrabold">{isCape ? 'Редактор плаща' : 'Редактор скина'}</div><span className="muted text-sm">Рисуйте прямо на модели или на развёртке справа</span>
        <div className="ml-auto flex gap-2"><button className="btn !p-2" title="Отменить (Ctrl+Z)" disabled={!undo.current.length} onClick={() => history(undo, redo)}><Icon n="undo" s={18} /></button>
          <button className="btn !p-2" title="Повторить (Ctrl+Y)" disabled={!redo.current.length} onClick={() => history(redo, undo)} style={{ transform: 'scaleX(-1)' }}><Icon n="undo" s={18} /></button>
          <button className="btn !p-2" title="Закрыть (Esc)" onClick={onClose}><Icon n="close" s={18} /></button></div></div>
      <div className="flex gap-3 flex-1 min-h-0">
        <div className="flex flex-col gap-1.5">{TOOLS.map(([id, ic, label]) => <button key={id} title={label} onClick={() => setTool(id)} className={'btn !p-2.5 ' + (tool === id ? 'btn-on' : '')}><Icon n={ic} s={20} /></button>)}
          <div className="label mt-2 text-center">Кисть</div>{[1, 2, 3].map((n) => <button key={n} onClick={() => setSize(n)} className={'btn !p-0 h-9 flex items-center justify-center ' + (size === n ? 'btn-on' : '')}><span className="rounded-sm" style={{ width: 4 + n * 4, height: 4 + n * 4, background: 'currentColor' }} /></button>)}</div>
        <div className="flex-1 min-w-0"><Skin3D skin={isCape ? skin : url || src} cape={isCape ? url : undefined} slim={slim} paint={isCape ? 'cape' : 'skin'} onSkinHit={onSkinHit} onCapeHit={(h) => paintAt(h.u, h.v, h.first)} view={view} height={640} scale={isCape ? 9 : 8} /></div>
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
                onPointerDown={(e) => { (e.target as Element).setPointerCapture(e.pointerId); map(e, true) }} onPointerMove={(e) => (e.buttons & 1) && tool !== 'fill' && tool !== 'picker' && map(e, false)} />
              {guides && guideList.map(([x, y, w, h, l], i) => <div key={i} className="absolute pointer-events-none text-[9px] text-white/90" style={{ left: x / TW * mapW, top: y / TH * mapH, width: w / TW * mapW, height: h / TH * mapH, border: '1px dashed rgba(255,255,255,.45)' }}>{l && <span style={{ background: 'rgba(0,0,0,.5)', padding: '0 3px' }}>{l}</span>}</div>)}</div></div>
          {isCape && <div className="card rounded-2xl p-3 flex flex-col gap-2.5">
            <div className="font-bold">Конструктор плаща</div>
            <div className="label">Разрешение</div><div className="flex flex-col gap-1">{RES.map(([kk, l]) => <button key={kk} className={'row !py-1.5 ' + (k === kk ? '!bg-[var(--chip)] font-semibold' : '')} onClick={() => setRes(kk)}>{k === kk ? <Icon n="check" s={14} /> : <span className="w-[14px]" />}{l}</button>)}</div>
            <div className="label">Фон</div><div className="seg"><button className={d.bg === 'solid' ? 'on' : ''} onClick={() => upd({ bg: 'solid' })}>Цвет</button><button className={d.bg === 'gradient' ? 'on' : ''} onClick={() => upd({ bg: 'gradient' })}>Градиент</button></div>
            <div className="flex gap-2 items-center"><ColorPick value={d.c1} set={(v) => upd({ c1: v })} />{d.bg === 'gradient' && <ColorPick value={d.c2} set={(v) => upd({ c2: v })} />}
              <button className="pill" onClick={() => upd({ c1: color })}>Текущий цвет</button></div>
            <div className="label">Картинка на плаще</div>
            <div className="flex gap-2"><button className="btn flex-1 flex items-center justify-center gap-2" onClick={async () => { const im = await pickImage(); if (im) upd({ img: im }) }}><Icon n="image" s={16} />{d.img ? 'Другая' : 'Выбрать'}</button>{d.img && <button className="btn" onClick={() => upd({ img: '' })}><Icon n="trash" s={16} /></button>}</div>
            {d.img && <><div className="seg"><button className={d.fit === 'cover' ? 'on' : ''} onClick={() => upd({ fit: 'cover' })}>Заполнить</button><button className={d.fit === 'contain' ? 'on' : ''} onClick={() => upd({ fit: 'contain' })}>Вписать</button></div>
              {([['scale', 'Масштаб', 20, 300, '%'], ['ox', 'Сдвиг X', -100, 100, ''], ['oy', 'Сдвиг Y', -100, 100, ''], ['rot', 'Поворот', -180, 180, '°']] as [keyof Design, string, number, number, string][]).map(([key, l, mn, mx, u]) =>
                <label key={key} className="text-sm"><div className="flex justify-between"><span>{l}</span><span className="muted">{d[key] as number}{u}</span></div><input type="range" className="w-full" min={mn} max={mx} value={d[key] as number} onChange={(e) => upd({ [key]: +e.target.value } as any)} /></label>)}
              <Check on={d.back} set={(v) => upd({ back: v })} label="И на внутренней стороне" /></>}
            <div className="label">Рамка</div><div className="flex gap-2 items-center"><input type="range" className="flex-1" min={0} max={3} value={d.border} onChange={(e) => upd({ border: +e.target.value })} /><ColorPick value={d.borderColor} set={(v) => upd({ borderColor: v })} size="sm" /></div>
            <button className="btn btn-primary" onClick={() => compose(d, k === 1 && d.img ? 8 : k)}>Применить дизайн</button>
            <p className="muted text-xs">Картинка лучше всего смотрится в HD: при наложении на классический плащ разрешение само переключится на 512×256. После применения можно дорисовать детали кистью.</p></div>}
        </div>
      </div>
      <div className="flex gap-2 justify-end items-center"><span className="muted text-sm mr-auto">{isCape ? 'Плащ увидят все игроки AsteriaLauncher.' : 'Скин увидят все игроки AsteriaLauncher; для лицензии — ещё и на всех серверах (кнопка «Применить»).'}</span>
        <a className="btn flex items-center gap-2" href={url} download={mode + '.png'}><Icon n="download" s={16} />PNG</a>
        <button className="btn" onClick={onClose}>Отмена</button><button className="btn btn-primary" onClick={() => onSave(cv.current!.toDataURL('image/png'))}>Сохранить</button></div>
    </div>
  </div>
}
