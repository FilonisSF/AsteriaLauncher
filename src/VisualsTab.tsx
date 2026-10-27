import { useEffect, useMemo, useRef, useState } from 'react'
import schema from '../mods/common-visuals/resources/asteria/schema.json'
import { Icon } from './Icons'
import { Check, ColorPick, Dropdown, Switch, Tilt } from './ui'
declare global { interface Window { api: any } }

// The same schema.json the mod reads: the launcher and the in-game menu edit one file (config/asteria-visuals.json in every build).
type S = { id: string; name: string; type: 'bool' | 'num' | 'mode' | 'color'; def: any; min?: number; max?: number; step?: number; options?: string[] }
type M = { id: string; cat: string; name: string; desc: string; on: boolean; hud?: [number, number]; s: S[] }
type ModCfg = { on: boolean; bind: number; hud?: [number, number]; s: Record<string, any> }
type Theme = { accent: string; accent2: string; chroma: boolean; shadow: boolean; blur: boolean; bg: number; scale: number; hud?: number; glare?: boolean; font?: number | boolean; menu?: boolean }
export type VisCfg = { theme: Theme; modules: Record<string, ModCfg> }
const MODS = (schema as any).modules as M[], CATS = (schema as any).categories as { id: string; name: string }[]
const CAT_ICON: Record<string, string> = { hud: 'grid', world: 'cube', combat: 'sparkles', camera: 'eye' }
// card pictures: src/assets/art/vis-<id>.jpg (or .png / .webp), prompts in src/assets/art/PROMPTS_RU.md
const PIC: Record<string, string> = Object.fromEntries(Object.entries(import.meta.glob('./assets/art/vis-*.{jpg,png,webp}', { eager: true, query: '?url', import: 'default' }) as Record<string, string>).map(([k, v]) => [k.split('/').pop()!.replace(/^vis-|\.(jpg|png|webp)$/g, ''), v]))
const THEME0: Theme = { accent: '#22d3ee', accent2: '#3b82f6', chroma: false, shadow: true, blur: true, bg: 0.55, scale: 1, hud: 0 }
export function visDefaults(): VisCfg {
  const modules: Record<string, ModCfg> = {}
  for (const m of MODS) modules[m.id] = { on: m.on, bind: 0, ...(m.hud ? { hud: [...m.hud] as [number, number] } : {}), s: Object.fromEntries(m.s.map((x) => [x.id, x.def])) }
  return { theme: { ...THEME0 }, modules }
}
type Preset = { id: string; name: string; cfg: VisCfg }
type Presets = { active: string; list: Preset[] }
const uid = () => Math.random().toString(36).slice(2, 10)
/** ready-made starting points for a new config */
const TEMPLATES: { name: string; desc: string; make: () => VisCfg }[] = [
  { name: 'Стандарт', desc: 'Как после установки', make: visDefaults },
  { name: 'Минимал', desc: 'Только логотип, инфо и прицел, тонкие линии', make: () => only(['watermark', 'info', 'crosshair'], { hud: 1 }) },
  { name: 'PvP', desc: 'Всё для боя: цель, броня, клавиши, зум', make: () => only(['arraylist', 'keystrokes', 'info', 'armor', 'effects', 'targethud', 'notifications', 'crosshair', 'targetesp', 'hitparticles', 'hitcolor', 'killeffect', 'fullbright', 'zoom', 'island', 'hotbar', 'inventory', 'totems', 'cooldowns', 'damagenumbers', 'sounds', 'blockhighlight']) },
  { name: 'Красота', desc: 'Шлейфы, частицы, шляпа и радужная тема', make: () => only(['watermark', 'arraylist', 'notifications', 'frame', 'trails', 'jumpcircles', 'ambient', 'chinahat', 'targetesp', 'hitparticles', 'killeffect', 'swing', 'zoom', 'island', 'music', 'halo', 'pet', 'jumpparticles', 'particletrail', 'sonar', 'dash'], { chroma: true }) },
]
const FONTS = ['Minecraft (пиксельный)', 'Inter', 'Montserrat Alternates', 'Unbounded', 'Comfortaa', 'Glitch — «дед инсайд»', 'Ruslan Display', 'Russo One']
// closest font the launcher itself has, so the preview looks like the game
const PREVIEW_FONT = ['ui-monospace,Consolas,monospace', '"Manrope Variable",sans-serif', '"Manrope Variable",sans-serif', '"Unbounded Variable",sans-serif', '"Manrope Variable",sans-serif', '"Unbounded Variable",sans-serif', 'Georgia,serif', '"Unbounded Variable",sans-serif']
const fontIdx = (f: any) => typeof f === 'number' ? f : f === false ? 0 : 2 // old configs stored a boolean
function only(ids: string[], theme: Partial<Theme> = {}): VisCfg { const d = visDefaults(); for (const m of MODS) d.modules[m.id].on = ids.includes(m.id); d.theme = { ...d.theme, ...theme }; return d }
function merge(saved: any): VisCfg {
  const d = visDefaults(); if (!saved || !saved.modules) return d
  for (const m of MODS) { const o = saved.modules[m.id]; if (!o) continue; const t = d.modules[m.id]
    t.on = o.on ?? t.on; t.bind = o.bind ?? 0; if (o.hud && t.hud) t.hud = [o.hud[0], o.hud[1]]; for (const x of m.s) if (o.s && o.s[x.id] !== undefined) t.s[x.id] = o.s[x.id] }
  return { theme: { ...d.theme, ...(saved.theme || {}) }, modules: d.modules }
}

// browser key -> GLFW key code (what the mod checks)
const GLFW: Record<string, number> = { Space: 32, Quote: 39, Comma: 44, Minus: 45, Period: 46, Slash: 47, Semicolon: 59, Equal: 61, BracketLeft: 91, Backslash: 92, BracketRight: 93, Backquote: 96, Enter: 257, Tab: 258, Backspace: 259, Insert: 260, Delete: 261,
  ArrowRight: 262, ArrowLeft: 263, ArrowDown: 264, ArrowUp: 265, PageUp: 266, PageDown: 267, Home: 268, End: 269, CapsLock: 280, ShiftLeft: 340, ControlLeft: 341, AltLeft: 342, ShiftRight: 344, ControlRight: 345, AltRight: 346 }
const glfw = (code: string) => code.startsWith('Key') ? code.charCodeAt(3) : code.startsWith('Digit') ? 48 + +code[5] : /^F\d+$/.test(code) ? 289 + +code.slice(1) : /^Numpad\d$/.test(code) ? 320 + +code[6] : GLFW[code] ?? 0
const NAMES: Record<number, string> = Object.fromEntries(Object.entries(GLFW).map(([k, v]) => [v, k.replace('Left', ' L').replace('Right', ' R').replace('Arrow', '')]))
export const keyName = (k: number) => !k ? '—' : k >= 65 && k <= 90 ? String.fromCharCode(k) : k >= 48 && k <= 57 ? String(k - 48) : k >= 290 && k <= 314 ? 'F' + (k - 289) : k >= 320 && k <= 329 ? 'Num ' + (k - 320) : NAMES[k] ?? '#' + k

// how big each element is in the 640x360 GUI preview (like the mod draws it)
const BOX: Record<string, [number, number]> = { watermark: [92, 16], arraylist: [78, 74], keystrokes: [70, 84], info: [96, 70], armor: [86, 22], effects: [104, 46], targethud: [124, 38], notifications: [132, 28], island: [170, 18], hotbar: [204, 26], inventory: [178, 76], totems: [48, 22], cooldowns: [104, 44], music: [156, 36] }
const GW = 640, GH = 360

export default function VisualsTab({ plan, bg, onBuy, say, ver }: { plan: string; bg: string; onBuy: () => void; say: (m: string) => void; ver: string }) {
  const premium = plan === 'PREMIUM'
  const [c, setC] = useState<VisCfg | null>(null), [cat, setCat] = useState('hud'), [open, setOpen] = useState(''), [bindFor, setBindFor] = useState(''), [view, setView] = useState<'mods' | 'theme' | 'layout' | 'configs'>('mods'), [q, setQ] = useState('')
  const [pr, setPr] = useState<Presets | null>(null), prRef = useRef<Presets | null>(null), [enabled, setEnabled] = useState(true)
  useEffect(() => { window.api.visEnabled?.().then((v: boolean) => setEnabled(v !== false)) }, [])
  const toggleAll = async (v: boolean) => { setEnabled(v); await window.api.visEnabled?.(v); say(v ? 'Визуалы включены — со следующего запуска игры' : 'Визуалы выключены — игра запустится без них') }
  const [keys, setKeys] = useState<Record<string, string>>({}), [listenKey, setListenKey] = useState(''), last = useRef(0), tm = useRef<any>()
  const loadPresets = async (cur: VisCfg) => { let p: Presets | null = await window.api.visPresets?.()
    if (!p || !p.list?.length) { const id = uid(); p = { active: id, list: [{ id, name: 'Мой конфиг', cfg: cur }] }; window.api.visPresetsSet?.(p) }
    p = { ...p, list: p.list.map((x) => x.id === p!.active ? { ...x, cfg: cur } : { ...x, cfg: merge(x.cfg) }) }; prRef.current = p; setPr(p) }
  useEffect(() => { window.api.visGet().then((v: any) => { const m = merge(v); setC(m); loadPresets(m) }) }, [])
  useEffect(() => window.api.onFiles?.((d: any) => { if (Date.now() - last.current < 1500) return; if (d.kind === 'vis') window.api.visGet().then((v: any) => { if (v) { const m = merge(v); setC(m); loadPresets(m) } }); if (d.kind === 'opts' && d.ver === ver) window.api.optsGet(ver).then((o: any) => setKeys({ menu: o['key_key.asteria.menu'] || 'key.keyboard.right.shift', zoom: o['key_key.asteria.zoom'] || 'key.keyboard.c' })) }), [ver])
  useEffect(() => { const id = setInterval(async () => { if (Date.now() - last.current < 4000) return; const s = await window.api.gameState(); if (s.running) { const v = await window.api.visGet(); if (v) setC(merge(v)) } }, 3000); return () => clearInterval(id) }, []) // edits made in the game menu come back here
  useEffect(() => { window.api.optsGet(ver).then((o: any) => setKeys({ menu: o['key_key.asteria.menu'] || 'key.keyboard.right.shift', zoom: o['key_key.asteria.zoom'] || 'key.keyboard.c' })) }, [ver])
  // every edit also lands in the active preset
  const save = (n: VisCfg) => { setC(n); last.current = Date.now(); const p = prRef.current; if (p) { const q = { ...p, list: p.list.map((x) => x.id === p.active ? { ...x, cfg: n } : x) }; prRef.current = q; setPr(q) }
    clearTimeout(tm.current); tm.current = setTimeout(async () => { const r = await window.api.visSet(n); if (prRef.current) window.api.visPresetsSet?.(prRef.current); if (r?.live) say('Применено в игре') }, 300) }
  const setPresets = (p: Presets, apply?: VisCfg) => { prRef.current = p; setPr(p); window.api.visPresetsSet?.(p); if (apply) save(apply) }
  const mod = (id: string, p: Partial<ModCfg>) => c && save({ ...c, modules: { ...c.modules, [id]: { ...c.modules[id], ...p } } })
  const setS = (id: string, k: string, v: any) => c && mod(id, { s: { ...c.modules[id].s, [k]: v } })
  const theme = (p: Partial<Theme>) => c && save({ ...c, theme: { ...c.theme, ...p } })
  useEffect(() => { if (!bindFor) return; const f = (e: KeyboardEvent) => { e.preventDefault(); e.stopPropagation(); mod(bindFor, { bind: e.code === 'Escape' || e.code === 'Backspace' ? 0 : glfw(e.code) }); setBindFor('') }
    addEventListener('keydown', f, true); return () => removeEventListener('keydown', f, true) }, [bindFor, c])
  useEffect(() => { if (!listenKey) return; const f = async (e: KeyboardEvent) => { e.preventDefault(); e.stopPropagation()
      const name = 'key.keyboard.' + (e.code.startsWith('Key') ? e.code.slice(3).toLowerCase() : e.code.startsWith('Digit') ? e.code.slice(5) : /^F\d+$/.test(e.code) ? e.code.toLowerCase() : ({ ShiftRight: 'right.shift', ShiftLeft: 'left.shift', ControlLeft: 'left.control', ControlRight: 'right.control', AltLeft: 'left.alt', AltRight: 'right.alt', Space: 'space', Tab: 'tab', Insert: 'insert', Delete: 'delete', Home: 'home', End: 'end', Backquote: 'grave.accent' } as any)[e.code] || e.code.toLowerCase())
      await window.api.optsSet(ver, { ['key_key.asteria.' + listenKey]: name }); setKeys((k) => ({ ...k, [listenKey]: name })); setListenKey(''); say('Клавиша сохранена') }
    addEventListener('keydown', f, true); return () => removeEventListener('keydown', f, true) }, [listenKey])
  const mods = useMemo(() => MODS.filter((m) => q ? (m.name + m.desc).toLowerCase().includes(q.toLowerCase()) : m.cat === cat), [cat, q])
  if (!c) return <div className="muted p-6">…</div>
  const onCount = MODS.filter((m) => c.modules[m.id].on).length
  const pretty = (k?: string) => (k || '').replace('key.keyboard.', '').replace('right.', 'R ').replace('left.', 'L ').toUpperCase()

  return <div className="fade">
    <div className="hero !min-h-[190px] mb-5"><img className="bg" src={bg} />
      <div className="relative p-7 text-white flex items-end justify-between gap-6 h-full min-h-[190px]">
        <div><div className="badge ast mb-2"><Icon n="sparkles" s={12} />Бесплатно для всех</div><h1 className="text-4xl font-black tracking-tight" style={{ textShadow: '0 0 30px var(--a2)' }}>Asteria Visuals</h1>
          <p className="text-sm opacity-85 mt-1 max-w-xl">Свой HUD, шлейфы, круги прыжка, метка цели, зум и многое другое. Работает на 1.16.5, 1.20.1 и 1.21.4. Только визуальные эффекты — без нечестных преимуществ.</p>
          <div className="flex gap-2 mt-3 text-xs"><span className="badge soft !bg-white/15 !text-white"><Icon n="keyboard" s={12} />Меню в игре: <b>{pretty(keys.menu)}</b></span><span className="badge soft !bg-white/15 !text-white"><Icon n="search" s={12} />Зум: <b>{pretty(keys.zoom)}</b></span><span className="badge soft !bg-white/15 !text-white">Включено: {onCount}/{MODS.length}</span></div></div>
        <button className={'vis-master ' + (enabled ? 'on' : '')} onClick={() => toggleAll(!enabled)} title="Мод визуалов Asteria целиком: выключите, чтобы играть без него">
          <span className={'switch ' + (enabled ? 'on' : '')} /><span className="text-left"><b className="block text-sm">{enabled ? 'Визуалы включены' : 'Визуалы выключены'}</b><span className="text-[11px] opacity-80">{enabled ? 'Нажмите, чтобы играть без них' : 'Игра запускается без мода визуалов'}</span></span></button>
      </div></div>
    {!premium && <div className="card rounded-2xl p-4 mb-4 flex items-center gap-3"><span className="cmd-ic"><Icon n="lock" s={16} /></span><div className="text-sm flex-1"><b>Можно всё настроить заранее.</b> <span className="muted">В игре визуалы включатся, как только на аккаунте появится PREMIUM — лаунчер проверяет подписку при каждом запуске.</span></div></div>}

    <div className="flex items-center gap-3 mb-4 flex-wrap">
      <div className="seg">{([['mods', 'Функции', 'sparkles'], ['theme', 'Тема', 'palette'], ['layout', 'Расположение HUD', 'move'], ['configs', 'Конфиги', 'folder']] as const).map(([k, l, i]) => <button key={k} className={'flex items-center gap-1.5 ' + (view === k ? 'on' : '')} onClick={() => setView(k)}><Icon n={i} s={14} />{l}</button>)}</div>
      <div className="flex-1" />
      <div className="flex items-center gap-2 text-sm"><span className="muted">Клавиши в игре:</span>
        {(['menu', 'zoom'] as const).map((k) => <button key={k} className={'btn !py-1.5 ' + (listenKey === k ? 'btn-on' : '')} onClick={() => setListenKey(k)}>{k === 'menu' ? 'Меню' : 'Зум'} <kbd className="ml-1">{listenKey === k ? '…' : pretty(keys[k])}</kbd></button>)}</div>
      <button className="btn !py-1.5 flex items-center gap-1.5" onClick={() => { save(visDefaults()); say('Сброшено к стандартным') }}><Icon n="reset" s={14} />Сбросить</button>
    </div>

    {view === 'mods' && <div className="grid gap-5" style={{ gridTemplateColumns: '210px minmax(0,1fr)' }}>
      <div className="flex flex-col gap-1.5">
        <div className="relative mb-2"><input className="w-full !pl-9" placeholder="Поиск функции" value={q} onChange={(e) => setQ(e.target.value)} /><span className="absolute left-3 top-2.5 muted"><Icon n="search" s={16} /></span></div>
        {CATS.map((k) => { const n = MODS.filter((m) => m.cat === k.id && c.modules[m.id].on).length
          return <button key={k.id} className={'navw ' + (cat === k.id && !q ? 'on' : '')} onClick={() => { setCat(k.id); setQ('') }}><Icon n={CAT_ICON[k.id] || 'sparkles'} s={18} /><span className="flex-1 text-left">{k.name}</span><span className="text-xs opacity-80">{n}</span></button> })}
      </div>
      <div className="grid gap-3 content-start stagger" style={{ gridTemplateColumns: 'repeat(auto-fill,minmax(300px,1fr))' }}>
        {mods.map((m) => { const mc = c.modules[m.id], ex = open === m.id
          return <div key={m.id} className={'card rounded-2xl overflow-hidden transition ' + (mc.on ? '' : 'opacity-[.86]')} style={mc.on ? { borderColor: 'color-mix(in srgb,var(--a2) 45%,transparent)' } : {}}>
            <div className="relative h-[120px] overflow-hidden" style={{ background: 'linear-gradient(135deg,color-mix(in srgb,var(--a1) 38%,#06121c),color-mix(in srgb,var(--a2) 30%,#050b16))' }}>
              {PIC[m.id] ? <img src={PIC[m.id]} loading="lazy" decoding="async" draggable={false} className={'absolute inset-0 w-full h-full object-cover transition duration-300 ' + (mc.on ? '' : 'grayscale-[.55]')} />
                : <span className="absolute inset-0 flex items-center justify-center text-white/35"><Icon n={CAT_ICON[m.cat] || 'sparkles'} s={46} /></span>}
              <div className="absolute inset-x-0 bottom-0 h-12" style={{ background: 'linear-gradient(transparent,color-mix(in srgb,var(--card) 85%,transparent))' }} />
            </div>
            <div className="p-4 pt-3">
            <div className="flex items-start gap-3">
              <span className="cmd-ic !w-10 !h-10" style={mc.on ? { background: 'linear-gradient(135deg,var(--a1),var(--a2))', color: '#fff' } : {}}><Icon n={CAT_ICON[m.cat] || 'sparkles'} s={18} /></span>
              <div className="flex-1 min-w-0"><div className="font-bold">{m.name}</div><div className="muted text-xs">{m.desc}</div></div>
              <Switch on={mc.on} set={(v) => mod(m.id, { on: v })} /></div>
            <div className="flex items-center gap-2 mt-3">
              <button className={'pill !py-1 ' + (bindFor === m.id ? 'on' : '')} title="Клавиша включения в игре" onClick={() => setBindFor(bindFor === m.id ? '' : m.id)}><Icon n="keyboard" s={13} />{bindFor === m.id ? 'Нажмите клавишу…' : keyName(mc.bind)}</button>
              {m.s.length > 0 && <button className="pill !py-1 ml-auto" onClick={() => setOpen(ex ? '' : m.id)}><Icon n="sliders" s={13} />Настройки<span className={'transition ' + (ex ? 'rotate-180' : '')}><Icon n="chevron" s={12} /></span></button>}</div>
            {ex && <div className="mt-3 pt-3 flex flex-col gap-3 fade" style={{ borderTop: '1px solid var(--line)' }}>{m.s.map((s) => <Setting key={s.id} s={s} v={mc.s[s.id]} set={(v) => setS(m.id, s.id, v)} />)}</div>}
          </div></div> })}
      </div></div>}

    {view === 'theme' && <div className="grid gap-5" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.2fr)' }}>
      <div className="card rounded-2xl p-5 flex flex-col gap-4">
        <div className="font-bold">Стиль HUD</div>
        <div className="grid grid-cols-3 gap-2">{[[0, 'Кристалл', 'Стеклянные панели со светящейся рамкой'], [1, 'Созвездие', 'Тёмные карточки, линия-созвездие и бегущая комета'], [2, 'Минимал', 'Спокойные тёмные карточки, как в современных клиентах']].map(([k, n, d]) =>
          <button key={k as number} onClick={() => theme({ hud: k as number })} className={'text-left rounded-2xl p-3 transition ' + ((c.theme.hud ?? 0) === k ? 'ring-2 ring-[var(--a2)]' : '')} style={{ background: 'var(--chip)' }}><b className="block text-sm">{n}</b><span className="muted text-xs">{d}</span></button>)}</div>
        <div className="font-bold">Цвета интерфейса</div>
        <div className="flex gap-6 flex-wrap"><ColorPick value={c.theme.accent} set={(v) => theme({ accent: v, chroma: false })} label="Основной" /><ColorPick value={c.theme.accent2} set={(v) => theme({ accent2: v, chroma: false })} label="Второй" /></div>
        <div className="flex gap-2 flex-wrap">{[['#22d3ee', '#3b82f6'], ['#22d3ee', '#a855f7'], ['#f43f5e', '#f59e0b'], ['#22c55e', '#06b6d4'], ['#e879f9', '#6366f1'], ['#facc15', '#fb7185'], ['#e2e8f0', '#64748b']].map(([a, b]) =>
          <button key={a} className="w-12 h-8 rounded-xl" style={{ background: `linear-gradient(135deg,${a},${b})`, boxShadow: c.theme.accent === a && c.theme.accent2 === b ? '0 0 0 2px var(--card),0 0 0 4px var(--a2)' : 'none' }} onClick={() => theme({ accent: a, accent2: b, chroma: false })} />)}</div>
        <Switch on={c.theme.chroma} set={(v) => theme({ chroma: v })} label="Радужный перелив" hint="Цвета HUD плавно меняются по кругу" />
        <Switch on={c.theme.shadow} set={(v) => theme({ shadow: v })} label="Тени" />
        <Switch on={c.theme.blur} set={(v) => theme({ blur: v })} label="Мягкое свечение панелей" />
        <div className="flex items-center justify-between gap-3"><div><div className="text-sm">Шрифт в игре</div><div className="muted text-xs">Весь текст HUD и меню Asteria</div></div>
          <Dropdown value={String(fontIdx(c.theme.font))} w={210} set={(v) => theme({ font: +v })} options={FONTS.map((n, i) => ({ value: String(i), label: n }))} /></div>
        <Switch on={c.theme.menu !== false} set={(v) => theme({ menu: v })} label="Свой стиль меню Minecraft" hint="Неоновые кнопки, логотип AsteriaLauncher вместо Minecraft, фон Asteria и без кнопки Realms" />
        <Switch on={!!c.theme.glare} set={(v) => theme({ glare: v })} label="Блик по панелям" hint="Иногда по панелям HUD пробегает светлый блик" />
        <Range label="Прозрачность подложки" min={0} max={1} step={0.05} v={c.theme.bg} show={(x) => Math.round(x * 100) + '%'} set={(v) => theme({ bg: v })} />
        <Range label="Размер HUD" min={0.6} max={1.6} step={0.05} v={c.theme.scale} show={(x) => Math.round(x * 100) + '%'} set={(v) => theme({ scale: v })} />
      </div>
      <Tilt className="rounded-2xl overflow-hidden relative h-fit" max={4}><img src={bg} className="w-full aspect-video object-cover" /><div className="absolute inset-0"><Preview c={c} /></div></Tilt>
    </div>}

    {view === 'configs' && pr && <Configs c={c} pr={pr} set={setPresets} say={say} />}

    {view === 'layout' && <Layout c={c} bg={bg} move={(id, x, y) => mod(id, { hud: [x, y] })} />}
  </div>
}

/** named configs: switch, create from templates, rename, duplicate, delete, share by code / short code / file */
function Configs({ c, pr, set, say }: { c: VisCfg; pr: Presets; set: (p: Presets, apply?: VisCfg) => void; say: (m: string) => void }) {
  const [code, setCode] = useState(''), [binds, setBinds] = useState(false), [ren, setRen] = useState(''), [busy, setBusy] = useState(''), [shared, setShared] = useState<{ name: string; id: string | null; code: string } | null>(null)
  const uniq = (n: string) => { let k = n, i = 2; while (pr.list.some((x) => x.name === k)) k = `${n} ${i++}`; return k }
  const add = (name: string, cfg: VisCfg) => { const id = uid(), n = { id, name: uniq(name), cfg }; set({ active: id, list: [...pr.list, n] }, cfg); say('Конфиг «' + n.name + '» создан') }
  const keepBinds = (cfg: VisCfg): VisCfg => binds ? cfg : { ...cfg, modules: Object.fromEntries(Object.entries(cfg.modules).map(([id, m]) => [id, { ...m, bind: c.modules[id]?.bind ?? 0 }])) }
  const imp = (o: any, from: string) => { if (!o || o.error) return say('Не получилось прочитать ' + from); add(String(o.name || 'Импорт').slice(0, 40), keepBinds(merge(o))); setCode('') }
  const payload = (p: Preset) => ({ name: p.name, theme: p.cfg.theme, modules: p.cfg.modules })
  const share = async (p: Preset) => { setBusy(p.id); try { const r = await window.api.visShare(payload(p)); setShared({ name: p.name, id: r.id, code: r.code }); say('Код скопирован') } finally { setBusy('') } }
  const count = (cfg: VisCfg) => MODS.filter((m) => cfg.modules[m.id]?.on).length
  return <div className="grid gap-5" style={{ gridTemplateColumns: 'minmax(0,1.3fr) minmax(0,1fr)' }}>
    <div className="flex flex-col gap-2.5">
      {pr.list.map((p) => { const act = p.id === pr.active
        return <div key={p.id} className="card rounded-2xl p-3.5 flex items-center gap-2" style={act ? { borderColor: 'color-mix(in srgb,var(--a2) 55%,transparent)' } : {}}>
          <span className="w-10 h-10 rounded-xl shrink-0 mr-1" style={{ background: `linear-gradient(135deg,${p.cfg.theme.accent},${p.cfg.theme.accent2})` }} />
          <div className="flex-1 min-w-0">
            {ren === p.id ? <input autoFocus className="w-full !py-1" defaultValue={p.name} onBlur={(e) => { const v = e.target.value.trim().slice(0, 40); setRen(''); if (v && v !== p.name) set({ ...pr, list: pr.list.map((x) => x.id === p.id ? { ...x, name: v } : x) }) }} onKeyDown={(e) => { if (e.key === 'Enter') (e.target as HTMLInputElement).blur(); if (e.key === 'Escape') setRen('') }} />
              : <div className="font-bold truncate">{p.name}{act && <span className="badge soft ml-2 !text-[10px]">Активный</span>}</div>}
            <div className="muted text-xs">Включено функций: {count(p.cfg)}/{MODS.length}</div></div>
          {!act && <button className="btn !py-1.5" onClick={() => set({ ...pr, active: p.id }, p.cfg)}>Включить</button>}
          <button className="pill !py-1" title="Переименовать" onClick={() => setRen(p.id)}><Icon n="pencil" s={13} /></button>
          <button className="pill !py-1" title="Дублировать" onClick={() => add(p.name + ' (копия)', JSON.parse(JSON.stringify(p.cfg)))}><Icon n="copy" s={13} /></button>
          <button className="pill !py-1" title="Поделиться: скопировать код" disabled={busy === p.id} onClick={() => share(p)}><Icon n="share" s={13} />{busy === p.id ? '…' : 'Поделиться'}</button>
          <button className="pill !py-1" title="Сохранить в файл .asteria" onClick={async () => { if (await window.api.visExport(payload(p), p.name)) say('Файл сохранён') }}><Icon n="download" s={13} /></button>
          {pr.list.length > 1 && <button className="pill !py-1" title="Удалить" onClick={() => { if (!confirm('Удалить конфиг «' + p.name + '»?')) return; const list = pr.list.filter((x) => x.id !== p.id); if (act) set({ active: list[0].id, list }, list[0].cfg); else set({ ...pr, list }) }}><Icon n="trash" s={13} /></button>}
        </div> })}
      <p className="muted text-xs mt-1">Все изменения сохраняются в активный конфиг. В игре конфиги переключаются в меню Asteria → «Конфиги».</p>
    </div>
    <div className="flex flex-col gap-4">
      {shared && <div className="card rounded-2xl p-5 flex flex-col gap-3 sharecard">
        <div className="flex items-center gap-2"><span className="cmd-ic !w-9 !h-9"><Icon n="share" s={16} /></span><div className="flex-1 min-w-0"><b className="block text-sm">Код конфига «{shared.name}»</b><span className="muted text-xs">Уже в буфере обмена — отправьте другу</span></div>
          <button className="iconbtn" title="Закрыть" onClick={() => setShared(null)}><Icon n="close" s={14} /></button></div>
        {shared.id ? <div className="sharecode">{shared.id.split('').map((ch, i) => <span key={i}>{ch}</span>)}</div>
          : <div className="muted text-xs break-all font-mono p-2 rounded-xl" style={{ background: 'var(--chip)', maxHeight: 72, overflow: 'hidden' }}>{shared.code}</div>}
        <div className="flex gap-2"><button className="btn btn-primary flex-1 flex items-center justify-center gap-2" onClick={() => { navigator.clipboard?.writeText(shared.id || shared.code); say('Скопировано') }}><Icon n="copy" s={15} />Скопировать ещё раз</button></div>
        {!shared.id && <p className="muted text-[11px]">Короткий код не получился (сервер Asteria недоступен) — скопирован полный код, он тоже работает.</p>}
      </div>}
      <div className="card rounded-2xl p-5 flex flex-col gap-3">
        <div className="font-bold">Импорт</div>
        <div className="flex gap-2"><input className="flex-1 min-w-0" placeholder="Код друга (K7Q2XD или AST1:…)" value={code} onChange={(e) => setCode(e.target.value)} onKeyDown={async (e) => { if (e.key === 'Enter' && code.trim()) imp(await window.api.visDecode(code), 'код') }} />
          <button className="btn" disabled={!code.trim()} onClick={async () => imp(await window.api.visDecode(code), 'код')}>Добавить</button></div>
        <div className="flex gap-2 flex-wrap"><button className="btn !py-1.5 flex items-center gap-1.5" onClick={async () => imp(await window.api.visDecode(null), 'буфер обмена')}><Icon n="copy" s={14} />Из буфера обмена</button>
          <button className="btn !py-1.5 flex items-center gap-1.5" onClick={async () => { const o = await window.api.visImport(); if (o) imp(o, 'файл') }}><Icon n="folder" s={14} />Из файла</button></div>
        <Check on={binds} set={setBinds} label="Взять и клавиши функций из чужого конфига" />
      </div>
      <div className="card rounded-2xl p-5 flex flex-col gap-2">
        <div className="font-bold mb-1">Новый конфиг</div>
        {TEMPLATES.map((t) => <button key={t.name} className="text-left rounded-xl p-3 flex items-center gap-3 transition hover:brightness-110" style={{ background: 'var(--chip)' }} onClick={() => add(t.name, t.make())}>
          <Icon n="plus" s={16} /><span className="flex-1"><b className="block text-sm">{t.name}</b><span className="muted text-xs">{t.desc}</span></span></button>)}
      </div>
    </div>
  </div>
}

function Range({ label, min, max, step, v, set, show }: { label: string; min: number; max: number; step: number; v: number; set: (n: number) => void; show?: (n: number) => string }) {
  return <label className="block"><div className="flex justify-between text-sm mb-1"><span>{label}</span><b>{show ? show(v) : v}</b></div><input type="range" className="w-full" min={min} max={max} step={step} value={v} onChange={(e) => set(+e.target.value)} /></label>
}
function Setting({ s, v, set }: { s: S; v: any; set: (v: any) => void }) {
  if (s.type === 'bool') return <Switch on={!!v} set={set} label={s.name} />
  if (s.type === 'num') return <Range label={s.name} min={s.min!} max={s.max!} step={s.step!} v={+v} set={set} show={(x) => (s.step! < 1 ? x.toFixed(2).replace(/0$/, '') : String(x))} />
  if (s.type === 'color') return <div className="flex items-center justify-between"><span className="text-sm">{s.name}</span><ColorPick value={String(v)} set={set} size="sm" /></div>
  if (s.options!.length > 5) return <div className="flex items-center justify-between gap-3"><span className="text-sm">{s.name}</span><Dropdown value={String(v)} w={170} set={(x) => set(+x)} options={s.options!.map((o, i) => ({ value: String(i), label: o }))} /></div>
  return <div><div className="text-sm mb-1">{s.name}</div><div className="seg flex w-full">{s.options!.map((o, i) => <button key={o} className={'flex-1 ' + (+v === i ? 'on' : '')} onClick={() => set(i)}>{o}</button>)}</div></div>
}

/** small look-alike of the HUD over a game picture */
function Preview({ c }: { c: VisCfg }) {
  const t = c.theme, grad = `linear-gradient(90deg,${t.accent},${t.accent2})`, lines = t.hud === 1, minimal = t.hud === 2
  const panel: React.CSSProperties = minimal ? { background: `rgba(15,13,18,${Math.max(0.4, t.bg) * 0.96})`, borderRadius: 7, border: '1px solid rgba(255,255,255,.08)', boxShadow: `inset 3px 0 0 -1.5px ${t.accent}`, color: '#fff', fontSize: 10 * t.scale, padding: '4px 8px', fontWeight: 600 }
    : lines ? { background: `rgba(7,6,12,${Math.max(0.35, t.bg) * 0.92})`, borderRadius: 5, borderTop: `1px solid ${t.accent2}`, boxShadow: `inset 2px 0 0 0 ${t.accent}`, color: '#fff', fontSize: 10 * t.scale, padding: '4px 7px', fontWeight: 600 }
    : { background: `rgba(5,11,22,${Math.max(0.35, t.bg)})`, borderRadius: 6, border: `1px solid ${t.accent}cc`, boxShadow: t.shadow ? `0 0 10px ${t.accent}55` : 'none', color: '#fff', fontSize: 10 * t.scale, padding: '4px 7px', fontWeight: 600 }
  const on = (id: string) => c.modules[id]?.on
  return <div className="absolute inset-0 p-3" style={{ fontFamily: PREVIEW_FONT[fontIdx(t.font)] }}>
    {on('watermark') && <div style={{ ...panel, position: 'absolute', left: 10, top: 10, display: 'flex', gap: 6, alignItems: 'center' }}><span style={{ background: grad, WebkitBackgroundClip: 'text', color: 'transparent', fontWeight: 900 }}>✦ Asteria</span><span style={{ opacity: .7 }}>| 144 fps</span></div>}
    {on('info') && <div style={{ ...panel, position: 'absolute', left: 10, top: 44, lineHeight: 1.5 }}><div>FPS <b style={{ color: t.accent }}>144</b></div><div>Ping <b style={{ color: t.accent }}>23</b></div><div>XYZ <b style={{ color: t.accent }}>12 64 -380</b></div></div>}
    {on('arraylist') && <div style={{ position: 'absolute', right: 10, top: 10, display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 2 }}>{MODS.filter((m) => c.modules[m.id].on && m.cat !== 'hud').slice(0, 6).map((m, i) =>
      <div key={m.id} style={{ ...panel, borderRadius: 4, padding: '2px 7px', borderRight: `2px solid ${i % 2 ? t.accent2 : t.accent}` }}>{m.name}</div>)}</div>}
    {on('keystrokes') && <div style={{ position: 'absolute', left: 10, bottom: 12, display: 'grid', gridTemplateColumns: 'repeat(3,22px)', gap: 3 }}>{['', 'W', '', 'A', 'S', 'D'].map((k, i) => <div key={i} style={k ? { ...panel, padding: 0, height: 22, display: 'flex', alignItems: 'center', justifyContent: 'center', background: k === 'W' ? grad : panel.background } : {}}>{k}</div>)}</div>}
    {on('targethud') && <div style={{ ...panel, position: 'absolute', left: '56%', top: '56%', width: 130 }}><div>Steve</div><div style={{ height: 4, borderRadius: 3, background: 'rgba(255,255,255,.15)', marginTop: 4 }}><div style={{ width: '64%', height: 4, borderRadius: 3, background: grad }} /></div></div>}
    {on('island') && <div style={{ ...panel, position: 'absolute', left: '50%', top: 8, transform: 'translateX(-50%)', borderRadius: 999, padding: '3px 12px', display: 'flex', gap: 8, alignItems: 'center', background: 'rgba(5,9,13,.92)', border: 'none', boxShadow: '0 6px 18px rgba(0,0,0,.35)', borderBottom: `1px solid ${t.accent2}` }}><span style={{ background: grad, WebkitBackgroundClip: 'text', color: 'transparent', fontWeight: 900 }}>Asteria</span><span style={{ opacity: .6 }}>12:40 · 144 fps · 23 ms</span></div>}
    {on('music') && <div style={{ ...panel, position: 'absolute', left: 10, top: '42%', width: 150, display: 'flex', gap: 7, alignItems: 'center', padding: 5 }}>
      <div style={{ width: 26, height: 26, borderRadius: 6, background: grad, flex: 'none', display: 'flex', alignItems: 'flex-end', justifyContent: 'center', gap: 2, padding: 5 }}>{[6, 11, 8, 13].map((h, k) => <i key={k} style={{ width: 2.5, height: h, background: '#fff', borderRadius: 1 }} />)}</div>
      <div style={{ minWidth: 0, flex: 1 }}><div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>Turbo (Majestic)</div><div style={{ opacity: .6, fontSize: 8 * t.scale }}>Big Baby Tape</div>
        <div style={{ height: 2, borderRadius: 2, background: 'rgba(255,255,255,.15)', marginTop: 3 }}><div style={{ width: '70%', height: 2, borderRadius: 2, background: grad }} /></div></div></div>}
    {on('cooldowns') && <div style={{ ...panel, position: 'absolute', right: 10, bottom: 44, width: 110, lineHeight: 1.6 }}>{[['Жемчуг', 70], ['Хорус', 35]].map(([n, w]) => <div key={n as string}><div style={{ display: 'flex', justifyContent: 'space-between' }}><span>{n}</span><span style={{ opacity: .6 }}>0:0{Math.round(+w / 10)}</span></div>
      <div style={{ height: 2, borderRadius: 2, background: 'rgba(255,255,255,.15)' }}><div style={{ width: w + '%', height: 2, borderRadius: 2, background: grad }} /></div></div>)}</div>}
    {on('totems') && <div style={{ ...panel, position: 'absolute', left: '40%', bottom: 34, display: 'flex', gap: 5, alignItems: 'center', padding: '3px 8px' }}><span style={{ width: 10, height: 12, borderRadius: 3, background: 'linear-gradient(#fde68a,#d97706)' }} /><b>x3</b></div>}
    {on('hotbar') && <div style={{ ...panel, position: 'absolute', left: '50%', bottom: 4, transform: 'translateX(-50%)', display: 'flex', gap: 2, padding: 2 }}>{Array.from({ length: 9 }, (_, k) => <div key={k} style={{ width: 16, height: 16, borderRadius: 4, background: k === 0 ? grad : 'rgba(255,255,255,.06)' }} />)}</div>}
    {on('crosshair') && <div style={{ position: 'absolute', left: '50%', top: '50%', width: 14, height: 14, transform: 'translate(-50%,-50%)', border: `2px solid ${c.modules.crosshair.s.color}`, borderRadius: '50%' }} />}
  </div>
}

/** drag HUD elements where you want them (positions are fractions of the screen, like the mod uses) */
function Layout({ c, bg, move }: { c: VisCfg; bg: string; move: (id: string, x: number, y: number) => void }) {
  const box = useRef<HTMLDivElement>(null), [drag, setDrag] = useState<{ id: string; dx: number; dy: number } | null>(null), [tmp, setTmp] = useState<Record<string, [number, number]>>({})
  const els = MODS.filter((m) => m.hud && c.modules[m.id].on)
  const pos = (id: string, w: number, h: number) => { const [fx, fy] = tmp[id] || c.modules[id].hud!; return [fx > 0.5 ? fx * GW - w : fx * GW, fy > 0.5 ? fy * GH - h : fy * GH] }
  const onMove = (e: React.PointerEvent) => { if (!drag) return; const r = box.current!.getBoundingClientRect(), k = r.width / GW, [w, h] = BOX[drag.id] || [80, 20]
    let x = (e.clientX - r.left) / k - drag.dx, y = (e.clientY - r.top) / k - drag.dy; x = Math.max(0, Math.min(GW - w, x)); y = Math.max(0, Math.min(GH - h, y))
    const fx = x + w / 2 > GW / 2 ? (x + w) / GW : x / GW, fy = y + h / 2 > GH / 2 ? (y + h) / GH : y / GH; setTmp({ ...tmp, [drag.id]: [+fx.toFixed(4), +fy.toFixed(4)] }) }
  const end = () => { if (drag && tmp[drag.id]) move(drag.id, tmp[drag.id][0], tmp[drag.id][1]); setDrag(null); setTmp({}) }
  return <div><p className="muted text-sm mb-3">Перетаскивайте элементы мышью. Элемент у правого или нижнего края «прилипает» к нему и остаётся там при любом размере окна. В игре то же самое можно сделать в меню Asteria → «Расположение HUD».</p>
    <div ref={box} className="relative rounded-2xl overflow-hidden select-none" style={{ aspectRatio: '16/9', boxShadow: 'var(--shadow-lg)' }} onPointerMove={onMove} onPointerUp={end} onPointerLeave={end}>
      <img src={bg} className="absolute inset-0 w-full h-full object-cover" draggable={false} /><div className="absolute inset-0" style={{ background: 'rgba(0,0,0,.18)', backgroundImage: 'linear-gradient(rgba(255,255,255,.06) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.06) 1px,transparent 1px)', backgroundSize: '6.25% 11.1%' }} />
      <ScaleBox>{els.map((m) => { const [w, h] = BOX[m.id] || [80, 20], [x, y] = pos(m.id, w, h)
        return <div key={m.id} className="absolute flex items-center justify-center text-[9px] font-bold text-white rounded-md cursor-grab active:cursor-grabbing"
          style={{ left: x, top: y, width: w, height: h, background: drag?.id === m.id ? 'color-mix(in srgb,var(--a1) 55%,transparent)' : 'rgba(8,14,24,.62)', border: '1px solid color-mix(in srgb,var(--a2) 70%,transparent)', boxShadow: drag?.id === m.id ? '0 0 18px var(--a2)' : 'none' }}
          onPointerDown={(e) => { (e.target as Element).setPointerCapture(e.pointerId); const r = box.current!.getBoundingClientRect(), k = r.width / GW; setDrag({ id: m.id, dx: (e.clientX - r.left) / k - x, dy: (e.clientY - r.top) / k - y }) }}>{m.name}</div> })}</ScaleBox>
    </div></div>
}
/** a 640x360 coordinate space scaled to the preview */
function ScaleBox({ children }: { children: React.ReactNode }) {
  const el = useRef<HTMLDivElement>(null), [k, setK] = useState(1)
  useEffect(() => { const f = () => el.current && setK(el.current.parentElement!.clientWidth / GW); f(); const ro = new ResizeObserver(f); ro.observe(el.current!.parentElement!); return () => ro.disconnect() }, [])
  return <div ref={el} className="absolute left-0 top-0" style={{ width: GW, height: GH, transform: `scale(${k})`, transformOrigin: '0 0' }}>{children}</div>
}
