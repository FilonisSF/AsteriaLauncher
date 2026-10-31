import { useEffect, useMemo, useRef, useState } from 'react'
import schema from '../mods/common-visuals/resources/asteria/schema.json'
import { Icon } from './Icons'
import { Check, ColorPick, Dropdown, Switch, Tilt } from './ui'
declare global { interface Window { api: any } }

// The same schema.json the mod reads: the launcher and the in-game menu edit one file (config/asteria-visuals.json in every build).
type S = { id: string; name: string; type: 'bool' | 'num' | 'mode' | 'color' | 'str'; def: any; min?: number; max?: number; step?: number; options?: string[] }
type M = { id: string; cat: string; name: string; desc: string; on: boolean; hud?: [number, number]; s: S[] }
type ModCfg = { on: boolean; bind: number; hud?: [number, number]; hs?: number; s: Record<string, any> }
type Theme = { accent: string; accent2: string; chroma: boolean; shadow: boolean; blur: boolean; bg: number; scale: number; hud?: number; glare?: boolean; font?: number | boolean; menu?: boolean; menuBg?: number; allFont?: boolean; lite?: boolean; bgBlur?: boolean }
export type VisCfg = { theme: Theme; modules: Record<string, ModCfg> }
const MODS = (schema as any).modules as M[], CATS = (schema as any).categories as { id: string; name: string }[]
const CAT_ICON: Record<string, string> = { hud: 'grid', world: 'cube', combat: 'sparkles', camera: 'eye' }
// card pictures: src/assets/art/vis-<id>.jpg (or .png / .webp), prompts in src/assets/art/PROMPTS_RU.md
const PIC: Record<string, string> = Object.fromEntries(Object.entries(import.meta.glob('./assets/art/vis-*.{jpg,png,webp}', { eager: true, query: '?url', import: 'default' }) as Record<string, string>).map(([k, v]) => [k.split('/').pop()!.replace(/^vis-|\.(jpg|png|webp)$/g, ''), v]))
const THEME0: Theme = { accent: '#22d3ee', accent2: '#3b82f6', chroma: false, shadow: true, blur: true, bg: 0.55, scale: 1.25, hud: 0, font: 8 } as Theme
export function visDefaults(): VisCfg {
  const modules: Record<string, ModCfg> = {}
  for (const m of MODS) modules[m.id] = { on: m.on, bind: 0, ...(m.hud ? { hud: [...m.hud] as [number, number] } : {}), s: Object.fromEntries(m.s.map((x) => [x.id, x.def])) }
  return { theme: { ...THEME0 }, modules }
}
type Preset = { id: string; name: string; cfg: VisCfg }
type Presets = { active: string; list: Preset[] }
const uid = () => Math.random().toString(36).slice(2, 10)
/** ready themes: colours + HUD style + font + transparency in one click (fonts: 0 Minecraft ... 8 Russo One, see FONTS) */
const THEMES: { name: string; t: Partial<Theme> }[] = [
  { name: 'Asteria', t: { accent: '#22d3ee', accent2: '#3b82f6', hud: 0, font: 8, bg: 0.55, chroma: false, glare: true } },
  { name: 'Неон', t: { accent: '#f0abfc', accent2: '#22d3ee', hud: 1, font: 8, bg: 0.7, chroma: false, glare: true } },
  { name: 'Сакура', t: { accent: '#fda4af', accent2: '#f0abfc', hud: 0, font: 4, bg: 0.45, chroma: false, glare: true } },
  { name: 'Океан', t: { accent: '#38bdf8', accent2: '#14b8a6', hud: 2, font: 8, bg: 0.5, chroma: false, glare: false } },
  { name: 'Закат', t: { accent: '#fb923c', accent2: '#f43f5e', hud: 1, font: 8, bg: 0.6, chroma: false, glare: true } },
  { name: 'Лес', t: { accent: '#4ade80', accent2: '#a3e635', hud: 2, font: 5, bg: 0.55, chroma: false, glare: false } },
  { name: 'Лёд', t: { accent: '#e0f2fe', accent2: '#7dd3fc', hud: 0, font: 8, bg: 0.4, chroma: false, glare: true } },
  { name: 'Золото', t: { accent: '#facc15', accent2: '#f59e0b', hud: 1, font: 9, bg: 0.65, chroma: false, glare: true } },
  { name: 'Вампир', t: { accent: '#ef4444', accent2: '#7f1d1d', hud: 2, font: 8, bg: 0.75, chroma: false, glare: false } },
  { name: 'Космос', t: { accent: '#a78bfa', accent2: '#6366f1', hud: 1, font: 10, bg: 0.6, chroma: false, glare: true } },
  { name: 'Мята', t: { accent: '#6ee7b7', accent2: '#5eead4', hud: 0, font: 3, bg: 0.45, chroma: false, glare: false } },
  { name: 'Монохром', t: { accent: '#f1f5f9', accent2: '#64748b', hud: 2, font: 8, bg: 0.7, chroma: false, glare: false } },
  { name: 'Конфета', t: { accent: '#f472b6', accent2: '#facc15', hud: 0, font: 4, bg: 0.5, chroma: false, glare: true } },
  { name: 'Радуга', t: { accent: '#22d3ee', accent2: '#a855f7', hud: 1, font: 8, bg: 0.6, chroma: true, glare: true } },
  { name: 'Ретро', t: { accent: '#fde047', accent2: '#22c55e', hud: 2, font: 0, bg: 0.6, chroma: false, glare: false } },
  { name: 'Лава', t: { accent: '#fb7185', accent2: '#f97316', hud: 0, font: 8, bg: 0.6, chroma: false, glare: true } },
]
/** ready-made starting points for a new config */
const TEMPLATES: { name: string; desc: string; make: () => VisCfg }[] = [
  { name: 'Стандарт', desc: 'Как после установки', make: visDefaults },
  { name: 'Стример', desc: 'Скрытый ник, музыка и аккуратный HUD', make: () => only(['island', 'music', 'nameprotect', 'info', 'hotbar', 'targethud', 'crosshair', 'notifications', 'zoom'], { hud: 2, bg: 0.5 }) },
  { name: 'Минимал', desc: 'Остров, хотбар и прицел — чистый экран', make: () => only(['island', 'hotbar', 'crosshair', 'blockhighlight', 'zoom'], { hud: 2, bg: 0.55 }) },
  { name: 'PvP', desc: 'Всё для боя: цель, броня, клавиши, зум', make: () => only(['island', 'info', 'keystrokes', 'armor', 'effects', 'targethud', 'notifications', 'crosshair', 'targetesp', 'hitparticles', 'damagenumbers', 'killeffect', 'sounds', 'hotbar', 'inventory', 'totems', 'cooldowns', 'blockhighlight', 'fullbright', 'zoom', 'totem'], { hud: 2, bg: 0.6 }) },
  { name: 'Красота', desc: 'Шлейфы, частицы, шляпа и радужная тема', make: () => only(['island', 'music', 'notifications', 'trails', 'jumpcircles', 'jumpparticles', 'ambient', 'halo', 'pet', 'targetesp', 'hitparticles', 'killeffect', 'swing', 'dash', 'totem', 'skycolor', 'zoom'], { hud: 1, chroma: true, glare: true }) },
]
const FONTS = ['Minecraft (пиксельный)', 'Ubuntu', 'Montserrat Alt', 'Balsamiq (мило)', 'Pangolin (мило)', 'Neucha (от руки)', 'Marmelad', 'Pacifico', 'Russo One', 'Philosopher', 'Unbounded']
// closest font the launcher itself has, so the preview looks like the game
const PREVIEW_FONT = ['ui-monospace,Consolas,monospace', '"Manrope Variable",sans-serif', '"Manrope Variable",sans-serif', '"Comic Sans MS","Manrope Variable",sans-serif', '"Comic Sans MS","Manrope Variable",sans-serif', '"Segoe Print","Manrope Variable",sans-serif', '"Manrope Variable",sans-serif', '"Segoe Script","Manrope Variable",cursive', '"Unbounded Variable",sans-serif', 'Georgia,serif', '"Unbounded Variable",sans-serif']
const fontIdx = (f: any) => typeof f === 'number' ? Math.min(f, FONTS.length - 1) : f === false ? 0 : 8 // old configs stored a boolean; Russo One by default
function only(ids: string[], theme: Partial<Theme> = {}): VisCfg { const d = visDefaults(); for (const m of MODS) d.modules[m.id].on = ids.includes(m.id); d.theme = { ...d.theme, ...theme }; return d }
function merge(saved: any): VisCfg {
  const d = visDefaults(); if (!saved || !saved.modules) return d
  for (const m of MODS) { const o = saved.modules[m.id]; if (!o) continue; const t = d.modules[m.id]
    t.on = o.on ?? t.on; t.bind = o.bind ?? 0; if (o.hud && t.hud) t.hud = [o.hud[0], o.hud[1]]; if (typeof o.hs === 'number') t.hs = o.hs; for (const x of m.s) if (o.s && o.s[x.id] !== undefined) t.s[x.id] = o.s[x.id] }
  return { theme: { ...d.theme, ...(saved.theme || {}) }, modules: d.modules }
}

// browser key -> GLFW key code (what the mod checks)
const GLFW: Record<string, number> = { Space: 32, Quote: 39, Comma: 44, Minus: 45, Period: 46, Slash: 47, Semicolon: 59, Equal: 61, BracketLeft: 91, Backslash: 92, BracketRight: 93, Backquote: 96, Enter: 257, Tab: 258, Backspace: 259, Insert: 260, Delete: 261,
  ArrowRight: 262, ArrowLeft: 263, ArrowDown: 264, ArrowUp: 265, PageUp: 266, PageDown: 267, Home: 268, End: 269, CapsLock: 280, ShiftLeft: 340, ControlLeft: 341, AltLeft: 342, ShiftRight: 344, ControlRight: 345, AltRight: 346 }
const glfw = (code: string) => code.startsWith('Key') ? code.charCodeAt(3) : code.startsWith('Digit') ? 48 + +code[5] : /^F\d+$/.test(code) ? 289 + +code.slice(1) : /^Numpad\d$/.test(code) ? 320 + +code[6] : GLFW[code] ?? 0
const NAMES: Record<number, string> = Object.fromEntries(Object.entries(GLFW).map(([k, v]) => [v, k.replace('Left', ' L').replace('Right', ' R').replace('Arrow', '')]))
export const keyName = (k: number) => !k ? '—' : k < 0 ? ({ [-3]: 'Колесо', [-4]: 'Мышь 4', [-5]: 'Мышь 5' } as any)[k] ?? 'Мышь ' + -k : k >= 65 && k <= 90 ? String.fromCharCode(k) : k >= 48 && k <= 57 ? String(k - 48) : k >= 290 && k <= 314 ? 'F' + (k - 289) : k >= 320 && k <= 329 ? 'Num ' + (k - 320) : NAMES[k] ?? '#' + k

// how big each element is in the 640x360 GUI preview (like the mod draws it)
const BOX: Record<string, [number, number]> = { watermark: [92, 16], arraylist: [78, 74], keystrokes: [70, 84], info: [96, 70], armor: [86, 22], effects: [104, 46], targethud: [124, 38], notifications: [132, 28], island: [170, 18], hotbar: [204, 26], inventory: [178, 76], totems: [48, 22], cooldowns: [104, 44], music: [156, 36] }
const GW = 640, GH = 360

export default function VisualsTab({ plan, bg, onBuy, say, ver }: { plan: string; bg: string; onBuy: () => void; say: (m: string) => void; ver: string }) {
  const premium = plan === 'PREMIUM'
  const [c, setC] = useState<VisCfg | null>(null), [cat, setCat] = useState('hud'), [open, setOpen] = useState(''), [bindFor, setBindFor] = useState(''), [view, setView] = useState<'mods' | 'theme' | 'layout' | 'configs' | 'crosshair' | 'marks' | 'abbr' | 'binds' | 'voice'>('mods'), [q, setQ] = useState('')
  const [pr, setPr] = useState<Presets | null>(null), prRef = useRef<Presets | null>(null), [enabled, setEnabled] = useState(true)
  useEffect(() => { window.api.visEnabled?.().then((v: boolean) => setEnabled(v !== false)) }, [])
  const toggleAll = async (v: boolean) => { setEnabled(v); await window.api.visEnabled?.(v); say(v ? 'Визуалы включены — со следующего запуска игры' : 'Визуалы выключены — игра запустится без них') }
  const [keys, setKeys] = useState<Record<string, string>>({}), [listenKey, setListenKey] = useState(''), last = useRef(0), wrote = useRef(0), tm = useRef<any>()
  const loadPresets = async (cur: VisCfg) => { let p: Presets | null = await window.api.visPresets?.()
    if (!p || !p.list?.length) { const id = uid(); p = { active: id, list: [{ id, name: 'Мой конфиг', cfg: cur }] }; window.api.visPresetsSet?.(p) }
    p = { ...p, list: p.list.map((x) => x.id === p!.active ? { ...x, cfg: cur } : { ...x, cfg: merge(x.cfg) }) }; prRef.current = p; setPr(p) }
  useEffect(() => { window.api.visGet().then((v: any) => { const m = merge(v); setC(m); loadPresets(m) }) }, [])
  // our own write coming back (wrote) is skipped: reading it would put an older config into the active one
  useEffect(() => window.api.onFiles?.((d: any) => { if (Date.now() - last.current < 1500 || Date.now() - wrote.current < 2500) return; if (d.kind === 'vis') window.api.visGet().then((v: any) => { if (v) { const m = merge(v); setC(m); loadPresets(m) } }); if (d.kind === 'opts' && d.ver === ver) window.api.optsGet(ver).then((o: any) => setKeys({ menu: o['key_key.asteria.menu'] || 'key.keyboard.right.shift', zoom: o['key_key.asteria.zoom'] || 'key.keyboard.c' })) }), [ver])
  useEffect(() => { const id = setInterval(async () => { if (Date.now() - last.current < 4000) return; const s = await window.api.gameState(); if (s.running) { const v = await window.api.visGet(); if (v) setC(merge(v)) } }, 3000); return () => clearInterval(id) }, []) // edits made in the game menu come back here
  useEffect(() => { window.api.optsGet(ver).then((o: any) => setKeys({ menu: o['key_key.asteria.menu'] || 'key.keyboard.right.shift', zoom: o['key_key.asteria.zoom'] || 'key.keyboard.c' })) }, [ver])
  // every edit also lands in the active preset
  const save = (n: VisCfg) => { setC(n); last.current = Date.now(); const p = prRef.current; if (p) { const q = { ...p, list: p.list.map((x) => x.id === p.active ? { ...x, cfg: n } : x) }; prRef.current = q; setPr(q) }
    clearTimeout(tm.current); tm.current = setTimeout(() => write(n), 300) }
  const write = async (n: VisCfg) => { last.current = Date.now(); const r = await window.api.visSet(n); wrote.current = Date.now(); if (prRef.current) window.api.visPresetsSet?.(prRef.current); if (r?.live) say('Применено в игре') }
  // switching a config writes at once (no 300 ms wait): quick clicks always end on the last one picked
  const setPresets = (p: Presets, apply?: VisCfg) => { prRef.current = p; setPr(p); window.api.visPresetsSet?.(p); if (apply) { save(apply); clearTimeout(tm.current); write(apply) } }
  const mod = (id: string, p: Partial<ModCfg>) => c && save({ ...c, modules: { ...c.modules, [id]: { ...c.modules[id], ...p } } })
  const setS = (id: string, k: string, v: any) => c && mod(id, { s: { ...c.modules[id].s, [k]: v } })
  const theme = (p: Partial<Theme>) => c && save({ ...c, theme: { ...c.theme, ...p } })
  useEffect(() => { if (!bindFor) return; const f = (e: KeyboardEvent) => { e.preventDefault(); e.stopPropagation(); mod(bindFor, { bind: e.code === 'Escape' || e.code === 'Backspace' ? 0 : glfw(e.code) }); setBindFor('') }
    // mouse buttons too (side buttons 4 / 5, wheel click): stored as negative codes, like the game reads them
    const mf = (e: MouseEvent) => { if (e.button < 1) return; e.preventDefault(); e.stopPropagation(); mod(bindFor, { bind: -([0, 3, 2, 4, 5][e.button] ?? 0) }); setBindFor('') }
    addEventListener('keydown', f, true); const tm = setTimeout(() => addEventListener('mousedown', mf, true), 120)
    return () => { removeEventListener('keydown', f, true); removeEventListener('mousedown', mf, true); clearTimeout(tm) } }, [bindFor, c])
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
      <div className="seg">{([['mods', 'Функции', 'sparkles'], ['theme', 'Тема', 'palette'], ['crosshair', 'Прицел', 'plus'], ['marks', 'Метки', 'servers'], ['binds', 'Бинды команд', 'keyboard'], ['configs', 'Конфиги', 'folder'], ['abbr', 'Сокращения', 'rules']] as const).map(([k, l, i]) => <button key={k} className={'flex items-center gap-1.5 ' + (view === k ? 'on' : '')} onClick={() => setView(k)}><Icon n={i} s={14} />{l}</button>)}</div>
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
      <div className="vis-cols">
        {mods.map((m) => { const mc = c.modules[m.id], ex = open === m.id
          return <div key={m.id} className={'card rounded-2xl overflow-hidden transition ' + (mc.on ? '' : 'opacity-[.86]')} style={mc.on ? { borderColor: 'color-mix(in srgb,var(--a2) 45%,transparent)' } : {}}>
            {PIC[m.id] && <div className="relative h-[120px] overflow-hidden">
              <img src={PIC[m.id]} loading="lazy" decoding="async" draggable={false} className={'absolute inset-0 w-full h-full object-cover transition duration-300 ' + (mc.on ? '' : 'grayscale-[.55]')} />
              <div className="absolute inset-x-0 bottom-0 h-12" style={{ background: 'linear-gradient(transparent,color-mix(in srgb,var(--card) 85%,transparent))' }} />
            </div>}
            <div className="p-4 pt-3">
            <div className="flex items-start gap-3">
              <div className="flex-1 min-w-0"><div className="font-bold">{m.name}</div><div className="muted text-xs">{m.desc}</div></div>
              <button className={'vswitch ' + (mc.on ? 'on' : '')} title={mc.on ? 'Включено' : 'Выключено'} onClick={() => mod(m.id, { on: !mc.on })} /></div>
            <div className="flex items-center gap-2 mt-3">
              <button className={'pill !py-1 ' + (bindFor === m.id ? 'on' : '')} title="Клавиша включения в игре" onClick={() => setBindFor(bindFor === m.id ? '' : m.id)}><Icon n="keyboard" s={13} />{bindFor === m.id ? 'Нажмите клавишу…' : keyName(mc.bind)}</button>
              {m.s.length > 0 && <button className="pill !py-1 ml-auto" onClick={() => setOpen(ex ? '' : m.id)}><Icon n="sliders" s={13} />Настройки<span className={'transition ' + (ex ? 'rotate-180' : '')}><Icon n="chevron" s={12} /></span></button>}</div>
            {ex && <div className="mt-3 pt-3 flex flex-col gap-3 fade" style={{ borderTop: '1px solid var(--line)' }}>{m.s.map((s) => <Setting key={s.id} mod={m.id} s={s} v={mc.s[s.id]} set={(v) => setS(m.id, s.id, v)} />)}
              <div className="flex gap-2 justify-end flex-wrap">{m.id === 'viewmodel' && ([['Сброс правой руки', false], ['Сброс левой руки', true]] as const).map(([t, left]) => <button key={t} className="pill !text-xs" onClick={() => mod(m.id, { s: { ...mc.s, ...Object.fromEntries(m.s.filter((x) => x.id !== 'split' && (/^l/.test(x.id) === left) && !(left && x.id === 'scale')).map((x) => [x.id, x.def])) } })}><Icon n="reset" s={12} />{t}</button>)}
                {m.s.length > 0 && <button className="pill !text-xs" onClick={() => mod(m.id, { s: Object.fromEntries(m.s.map((x) => [x.id, x.def])) })}><Icon n="reset" s={12} />Сбросить всё</button>}</div></div>}
          </div></div> })}
      </div></div>}

    {view === 'theme' && <div className="grid gap-5" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.2fr)' }}>
      <div className="card rounded-2xl p-5 flex flex-col gap-4">
        <div className="font-bold">Готовые темы</div>
        <div className="grid grid-cols-4 gap-2">{THEMES.map(({ name, t }) => { const on = c.theme.accent === t.accent && c.theme.accent2 === t.accent2 && !!c.theme.chroma === !!t.chroma
          return <button key={name} onClick={() => theme(t)} className={'rounded-2xl p-2.5 text-left transition hover:-translate-y-0.5 ' + (on ? 'ring-2 ring-[var(--a2)]' : '')} style={{ background: `linear-gradient(135deg,${t.accent}33,${t.accent2}33)`, border: '1px solid var(--line)' }}>
            <span className="block h-6 rounded-lg mb-1.5" style={{ background: t.chroma ? 'linear-gradient(90deg,#f43f5e,#facc15,#22c55e,#22d3ee,#a855f7)' : `linear-gradient(135deg,${t.accent},${t.accent2})` }} />
            <b className="block text-[13px] leading-tight" style={{ fontFamily: PREVIEW_FONT[fontIdx(t.font)] }}>{name}</b><span className="muted text-[10.5px]">{FONTS[fontIdx(t.font)]}</span></button> })}</div>
        <div className="font-bold">Стиль HUD</div>
        <div className="grid grid-cols-2 gap-2">{[[0, 'Кристалл', 'Стеклянные панели со светящейся рамкой'], [1, 'Созвездие', 'Тёмные карточки, линия-созвездие и бегущая комета'], [2, 'Минимал', 'Спокойные тёмные карточки, как в современных клиентах'], [3, 'Стекло', 'Светлое матовое стекло с тонкой рамкой, как в iPhone']].map(([k, n, d]) =>
          <button key={k as number} onClick={() => theme({ hud: k as number })} className={'text-left rounded-2xl p-3 transition ' + ((c.theme.hud ?? 0) === k ? 'ring-2 ring-[var(--a2)]' : '')} style={{ background: 'var(--chip)' }}><b className="block text-sm">{n}</b><span className="muted text-xs">{d}</span></button>)}</div>
        <div className="font-bold">Цвета интерфейса</div>
        <div className="flex gap-6 flex-wrap"><ColorPick value={c.theme.accent} set={(v) => theme({ accent: v, chroma: false })} label="Основной" /><ColorPick value={c.theme.accent2} set={(v) => theme({ accent2: v, chroma: false })} label="Второй" /></div>
        <div className="flex gap-2 flex-wrap">{[['#22d3ee', '#3b82f6'], ['#22d3ee', '#a855f7'], ['#f43f5e', '#f59e0b'], ['#22c55e', '#06b6d4'], ['#e879f9', '#6366f1'], ['#facc15', '#fb7185'], ['#e2e8f0', '#64748b']].map(([a, b]) =>
          <button key={a + b} className="w-12 h-8 rounded-xl" style={{ background: `linear-gradient(135deg,${a},${b})`, boxShadow: c.theme.accent === a && c.theme.accent2 === b ? '0 0 0 2px var(--card),0 0 0 4px var(--a2)' : 'none' }} onClick={() => theme({ accent: a, accent2: b, chroma: false })} />)}</div>
        <Switch on={c.theme.chroma} set={(v) => theme({ chroma: v })} label="Радужный перелив" hint="Цвета HUD плавно меняются по кругу" />
        <Switch on={c.theme.shadow} set={(v) => theme({ shadow: v })} label="Тени" />
        <Switch on={c.theme.blur} set={(v) => theme({ blur: v })} label="Мягкое свечение панелей" />
        <div className="flex items-center justify-between gap-3"><div><div className="text-sm">Шрифт в игре</div><div className="muted text-xs">Весь текст HUD и меню Asteria</div></div>
          <Dropdown value={String(fontIdx(c.theme.font))} w={210} set={(v) => theme({ font: +v })} options={FONTS.map((n, i) => ({ value: String(i), label: n }))} /></div>
        {c.theme.menu !== false && <div className="flex items-center justify-between gap-3"><span className="text-sm">Фон меню Minecraft</span><div className="seg">{['Гладкий HD', 'Картинка'].map((l, i) => <button key={l} className={(c.theme.menuBg ?? 0) === i ? 'on' : ''} onClick={() => theme({ menuBg: i })}>{l}</button>)}</div></div>}
        <OwnBg blur={c.theme.bgBlur !== false} setBlur={(v) => theme({ bgBlur: v })} say={say} />
        <Switch on={!!c.theme.allFont} set={(v) => theme({ allFont: v })} label="Шрифт для всего Minecraft" hint="Чат, ники, меню и названия предметов — выбранным шрифтом (не «Minecraft»)" />
        <Switch on={!!c.theme.lite} set={(v) => theme({ lite: v })} label="Лёгкий режим (больше FPS)" hint="Без теней и бликов на панелях. Визуалы и эффекты в мире всегда немного снижают FPS — выключайте ненужные" />
        <Switch on={c.theme.menu !== false} set={(v) => theme({ menu: v })} label="Свой стиль меню Minecraft" hint="Неоновые кнопки, логотип AsteriaLauncher вместо Minecraft, фон Asteria и без кнопки Realms" />
        <Switch on={!!c.theme.glare} set={(v) => theme({ glare: v })} label="Блик по панелям" hint="Иногда по панелям HUD пробегает светлый блик" />
        <Range label="Прозрачность подложки" min={0} max={1} step={0.05} v={c.theme.bg} show={(x) => Math.round(x * 100) + '%'} set={(v) => theme({ bg: v })} />
        <Range label="Размер HUD (шаг 25% — текст всегда чёткий)" min={0.75} max={2} step={0.25} v={Math.round(c.theme.scale * 4) / 4} show={(x) => Math.round(x * 100) + '%'} set={(v) => theme({ scale: v })} />
      </div>
      <Tilt className="rounded-2xl overflow-hidden relative h-fit" max={4}><img src={bg} className="w-full aspect-video object-cover" /><div className="absolute inset-0"><Preview c={c} /></div></Tilt>
    </div>}

    {view === 'configs' && pr && <Configs c={c} pr={pr} set={setPresets} say={say} />}
    {view === 'marks' && <MarksTab ver={ver} say={say} />}
    {view === 'abbr' && <AbbrTab />}
    {view === 'binds' && <BindsTab say={say} />}
    {view === 'crosshair' && c.modules.crosshair && <CrosshairTab m={c.modules.crosshair} on={(v) => mod('crosshair', { on: v })} set={(k, v) => setS('crosshair', k, v)} say={say} />}

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
/** markers per world (overworld / nether / end), the same on every server: name, colour, coordinates; add by coordinates, edit, delete */
type MarkT = { x: number; y: number; z: number; name?: string; color?: string }
function MarksTab({ ver, say }: { ver: string; say: (s: string) => void }) {
  const [all, setAll] = useState<Record<string, any[]>>({}), [srv, setSrv] = useState(''), [f, setF] = useState({ x: '', y: '', z: '', name: '', color: '#22d3ee' })
  useEffect(() => { window.api.marksGet(ver).then((d: any) => { const o: Record<string, any[]> = {}; for (const [k, v] of Object.entries(d || {})) { const dim = k.includes('|') ? k.split('|')[1] || 'minecraft:overworld' : k; o[dim] = [...(o[dim] || []), ...(v as any[])] } setAll(o); setSrv((s) => s || 'minecraft:overworld') }) }, [ver])
  const norm = (m: any): MarkT => Array.isArray(m) ? { x: m[0], y: m[1], z: m[2] } : m
  const list: MarkT[] = (all[srv] || []).map(norm)
  const save = (l: MarkT[]) => { const n = { ...all, [srv]: l }; setAll(n); window.api.marksSet(ver, n) }
  const add = () => { const x = +f.x, y = +f.y, z = +f.z; if ([f.x, f.y, f.z].some((v) => v.trim() === '' || isNaN(+v))) return say('Введите X, Y и Z'); save([...list, { x: Math.floor(x) + 0.5, y: Math.floor(y), z: Math.floor(z) + 0.5, name: f.name || 'Метка ' + (list.length + 1), color: f.color }]); setF({ ...f, x: '', y: '', z: '', name: '' }); say('Метка добавлена — в игре появится через пару секунд') }
  const DIMS: Record<string, string> = { 'minecraft:overworld': 'Верхний мир', 'minecraft:the_nether': 'Ад', 'minecraft:the_end': 'Энд' }
  const label = (k: string) => DIMS[k] || k.replace(/^minecraft:/, '')
  const servers = Array.from(new Set(['minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end', ...Object.keys(all)]))
  return <div className="grid gap-5" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.3fr)' }}>
    <div className="card rounded-3xl p-5 flex flex-col gap-3"><b className="text-lg">Новая метка</b>
      <div className="flex items-center gap-2"><span className="text-sm w-20">Мир</span><select className="flex-1" value={srv} onChange={(e) => setSrv(e.target.value)}>{servers.map((s) => <option key={s} value={s}>{label(s)}</option>)}</select></div>
      <div className="grid grid-cols-3 gap-2">{(['x', 'y', 'z'] as const).map((k) => <input key={k} placeholder={k.toUpperCase()} value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} className="text-center font-mono" />)}</div>
      <input placeholder="Название (База, Шахта…)" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} />
      <div className="flex items-center justify-between"><span className="text-sm">Цвет</span><ColorPick value={f.color} set={(v) => setF({ ...f, color: v })} size="sm" /></div>
      <button className="btn-add self-start" onClick={add}><span className="ic"><Icon n="plus" s={14} /></span>Добавить метку</button>
      <p className="muted text-xs">В игре: B — метка туда, куда смотрите (ещё раз B рядом — убрать), Shift+B — убрать все. В чат: <b>.метка 100 64 -200 База</b> — координаты из чата тоже можно скопировать так.</p></div>
    <div className="card rounded-3xl p-5"><div className="flex items-center justify-between mb-3"><b className="text-lg">Метки · {label(srv)}</b>{list.length > 0 && <button className="pill" onClick={() => save([])}><Icon n="trash" s={13} />Убрать все</button>}</div>
      {!list.length && <div className="muted text-sm">Пока нет меток в этом мире</div>}
      <div className="flex flex-col gap-2">{list.map((m, i) => <div key={i} className="mcard !p-2.5 flex items-center gap-2">
        <ColorPick value={m.color || '#22d3ee'} set={(v) => save(list.map((x, j) => j === i ? { ...x, color: v } : x))} size="sm" />
        <input className="flex-1 min-w-0" value={m.name || ''} placeholder="Без названия" onChange={(e) => save(list.map((x, j) => j === i ? { ...x, name: e.target.value } : x))} />
        <span className="muted text-xs font-mono shrink-0">{Math.floor(m.x)} {Math.floor(m.y)} {Math.floor(m.z)}</span>
        <button className="iconbtn" title="Удалить" onClick={() => save(list.filter((_, j) => j !== i))}><Icon n="trash" s={14} /></button></div>)}</div></div></div>
}
/** own picture for the Minecraft menus: as it is on the title screen, blurred on the others */
function OwnBg({ blur, setBlur, say }: { blur: boolean; setBlur: (v: boolean) => void; say: (s: string) => void }) {
  const [img, setImg] = useState<string | null>(null); useEffect(() => { window.api.bgGet?.().then(setImg) }, [])
  const pick = async () => { const d = await window.api.pickImage(); if (!d) return; const im = new Image(); im.onload = () => {
      const k = Math.min(1, 2560 / im.width), c = document.createElement('canvas'); c.width = Math.round(im.width * k); c.height = Math.round(im.height * k); c.getContext('2d')!.drawImage(im, 0, 0, c.width, c.height)
      const orig = c.toDataURL('image/png'), b = document.createElement('canvas'); b.width = Math.round(c.width / 2); b.height = Math.round(c.height / 2); const x = b.getContext('2d')!; x.filter = 'blur(14px) brightness(0.85)'; x.drawImage(c, -20, -20, b.width + 40, b.height + 40)
      window.api.bgSet(orig, b.toDataURL('image/png')); setImg(orig); say('Фон установлен — в игре сразу') }; im.src = d }
  return <div className="rounded-2xl p-3 flex items-center gap-3" style={{ background: 'var(--chip)' }}>
    <span className="w-20 h-12 rounded-lg shrink-0 overflow-hidden flex items-center justify-center" style={{ background: 'rgba(0,0,0,.3)' }}>{img ? <img src={img} className="w-full h-full object-cover" /> : <Icon n="image" s={18} />}</span>
    <div className="flex-1 min-w-0"><b className="text-sm">Свой фон Minecraft</b><div className="muted text-[11px]">В главном меню — как есть, в остальных меню — размыто</div>
      <div className="mt-1"><Switch on={blur} set={setBlur} label="Размытие в меню" /></div></div>
    <div className="flex flex-col gap-1"><button className="btn !py-1 text-sm" onClick={pick}>Выбрать</button>{img && <button className="btn !py-1 text-sm" onClick={() => { window.api.bgSet(null, null); setImg(null) }}>Убрать</button>}</div></div>
}
/** the announcer: a Windows voice says "Функция «…» включена" and the alerts; phrases are made once here and played by the mod */
const ALERT_PHRASES: [string, string][] = [['generic', 'Внимание'], ['hp', 'Мало здоровья'], ['armor', 'Броня ломается'], ['food', 'Вы проголодались'], ['totem', 'Тотем использован'], ['effect', 'Эффект заканчивается'], ['enemy_totem', 'Противник потерял тотем'], ['enemy_armor', 'У противника ломается броня']]
const clean = (n: string) => n.replace(/\s*\([^)]*\)/g, '').trim()
function VoiceTab({ say }: { say: (s: string) => void }) {
  const [voices, setVoices] = useState<{ id: string; name: string; lang: string; gender: string }[]>([]), [voice, setVoice] = useState(''), [rate, setRate] = useState(1), [pitch, setPitch] = useState(1), [busy, setBusy] = useState(''), [err, setErr] = useState(''), [ready, setReady] = useState(false)
  useEffect(() => { window.api.voiceList?.().then((r: any) => { if (r?.error) setErr(r.error); const l = r?.voices || []; setVoices(l); const ch = r?.chosen; setReady(!!ch)
    setVoice(ch?.voice || (l.find((v: any) => /ru/i.test(v.lang)) || l[0])?.id || ''); if (ch) { setRate(+ch.rate || 1); setPitch(+ch.pitch || 1) } }) }, [])
  const items = () => [...MODS.flatMap((m) => [{ id: 'on_' + m.id, text: 'Функция «' + clean(m.name) + '» включена' }, { id: 'off_' + m.id, text: 'Функция «' + clean(m.name) + '» выключена' }]), ...ALERT_PHRASES.map(([id, text]) => ({ id: 'alert_' + id, text }))]
  const test = async () => { setBusy('test'); const u = await window.api.voiceTest({ voice, rate, pitch, text: 'Функция «Траектория» включена' }); setBusy(''); if (u) new Audio(u).play(); else say('Не удалось прослушать') }
  const make = async () => { setBusy('make'); const r = await window.api.voiceMake({ voice, rate, pitch, items: items() }); setBusy(''); if (r?.error) return say(r.error); setReady(true); say('Озвучка готова: ' + r.count + ' фраз — в игре сразу') }
  const vn = (v: any) => v.name.replace(/^Microsoft\s*/, '') + (v.gender === 'Male' ? ' · мужской' : v.gender === 'Female' ? ' · женский' : '') + (/ru/i.test(v.lang) ? '' : ' · ' + v.lang)
  return <div className="grid gap-5" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1fr)' }}>
    <div className="card rounded-3xl p-5 flex flex-col gap-4"><div><b className="text-lg">Диктор</b><div className="muted text-sm">Голос называет функцию, когда вы её включаете или выключаете, и читает предупреждения: мало здоровья, ломается броня, противник потерял тотем.</div></div>
      {err && <div className="text-sm" style={{ color: '#ff8a8a' }}>{err}</div>}
      <div className="flex flex-col gap-2">{voices.map((v) => <button key={v.id} onClick={() => setVoice(v.id)} className="mcard !p-3 flex items-center gap-3 text-left" style={voice === v.id ? { outline: '2px solid var(--a1)' } : undefined}>
        <span className="w-9 h-9 rounded-full flex items-center justify-center shrink-0" style={{ background: voice === v.id ? 'linear-gradient(135deg,var(--a1),var(--a2))' : 'var(--chip)', color: voice === v.id ? '#fff' : undefined }}><Icon n="music" s={15} /></span><b className="text-sm">{vn(v)}</b></button>)}</div>
      <label className="text-sm"><div className="flex justify-between"><span>Скорость</span><b>{rate.toFixed(2)}</b></div><input type="range" className="w-full" min={0.6} max={1.6} step={0.05} value={rate} onChange={(e) => setRate(+e.target.value)} /></label>
      <label className="text-sm"><div className="flex justify-between"><span>Высота голоса</span><b>{pitch.toFixed(2)}</b></div><input type="range" className="w-full" min={0.5} max={1.5} step={0.05} value={pitch} onChange={(e) => setPitch(+e.target.value)} /></label>
      <div className="flex gap-2 flex-wrap"><button className="btn" disabled={!voice || !!busy} onClick={test}><Icon n="play" s={14} /> {busy === 'test' ? 'Готовлю…' : 'Прослушать'}</button>
        <button className="btn-add" disabled={!voice || !!busy} onClick={make}><span className="ic"><Icon n="music" s={13} /></span>{busy === 'make' ? 'Озвучиваю…' : ready ? 'Пересоздать озвучку' : 'Создать озвучку'}</button>
        {ready && <button className="pill" onClick={async () => { await window.api.voiceClear(); setReady(false); say('Озвучка удалена') }}><Icon n="trash" s={13} />Убрать</button>}</div></div>
    <div className="card rounded-3xl p-5 flex flex-col gap-2"><b>Что будет звучать</b><div className="muted text-xs mb-1">Включите «Диктор» в функциях визуалов. Громкость и что озвучивать — там же.</div>
      {[...ALERT_PHRASES.map(([, t]) => t), 'Функция «Трейлы» включена', 'Функция «Прицел» выключена'].map((t) => <div key={t} className="mcard !p-2.5 text-sm flex items-center gap-2"><Icon n="music" s={12} />{t}</div>)}</div></div>
}
/** key -> command: pressed in the game, the command is sent (V -> /spawn) */
function BindsTab({ say }: { say: (s: string) => void }) {
  const [list, setList] = useState<{ key: number; cmd: string }[]>([]), [listen, setListen] = useState(-1)
  useEffect(() => { window.api.bindsGet?.().then((l: any) => setList(l || [])) }, [])
  const save = (l: { key: number; cmd: string }[]) => { setList(l); window.api.bindsSet(l) }
  useEffect(() => { if (listen < 0) return
    const kd = (e: KeyboardEvent) => { e.preventDefault(); e.stopPropagation(); save(list.map((b, i) => i === listen ? { ...b, key: e.code === 'Escape' ? 0 : glfw(e.code) } : b)); setListen(-1) }
    const md = (e: MouseEvent) => { if (e.button === 0) return; e.preventDefault(); save(list.map((b, i) => i === listen ? { ...b, key: -([0, 3, 2, 4, 5][e.button] || 0) } : b)); setListen(-1) }
    addEventListener('keydown', kd, true); addEventListener('mousedown', md, true); return () => { removeEventListener('keydown', kd, true); removeEventListener('mousedown', md, true) } }, [listen, list])
  return <div className="card rounded-3xl p-5 flex flex-col gap-3"><div className="flex items-center justify-between"><div><b className="text-lg">Бинды команд</b><div className="muted text-sm">Нажимаете клавишу в игре — отправляется команда или сообщение. Работает, когда функция «Бинды команд» включена</div></div>
    <button className="btn-add" onClick={() => save([...list, { key: 0, cmd: '/spawn' }])}><span className="ic"><Icon n="plus" s={14} /></span>Добавить бинд</button></div>
    {!list.length && <div className="muted text-sm">Пока нет биндов. Например: V → /spawn, H → /home, J → /warp casino</div>}
    {list.map((b, i) => <div key={i} className="mcard !p-2.5 flex items-center gap-2">
      <button className={'btn !py-1.5 min-w-[90px] ' + (listen === i ? 'btn-on' : '')} onClick={() => setListen(i)}>{listen === i ? 'Нажмите…' : b.key ? keyName(b.key) : 'Клавиша'}</button>
      <Icon n="right" s={14} /><input className="flex-1 min-w-0 font-mono" value={b.cmd} placeholder="/spawn" onChange={(e) => save(list.map((x, j) => j === i ? { ...x, cmd: e.target.value } : x))} />
      <button className="iconbtn" title="Удалить" onClick={() => save(list.filter((_, j) => j !== i))}><Icon n="trash" s={14} /></button></div>)}
  </div>
}
/** what the short enchantment names above players mean (same table as Hud.ABBR in the mod) */
const ABBR: [string, string][] = [['ЗСн', 'Защита от снарядов'], ['ЗОг', 'Защита от огня / Огнеупорность'], ['ЗВз', 'Защита от взрывов'], ['Защ', 'Защита'], ['Нев', 'Невесомость'], ['Дых', 'Подводное дыхание'], ['Вод', 'Родство с водой'],
  ['Шип', 'Шипы'], ['ПХ', 'Подводная ходьба'], ['Лёд', 'Ледоход'], ['Душ', 'Скорость душ'], ['Пров', 'Проворство'], ['Остр', 'Острота'], ['Кара', 'Небесная кара'], ['Бич', 'Бич членистоногих'], ['Отд', 'Отдача'],
  ['Огн', 'Заговор огня'], ['Доб', 'Добыча'], ['Клин', 'Разящий клинок'], ['Эфф', 'Эффективность'], ['Шёлк', 'Шёлковое касание'], ['Проч', 'Прочность'], ['Удч', 'Удача'], ['Сила', 'Сила'], ['Отк', 'Откидывание'], ['Пыл', 'Горящая стрела'],
  ['Беск', 'Бесконечность'], ['МУд', 'Морская удача'], ['Прим', 'Приманка'], ['Верн', 'Верность'], ['Пронз', 'Пронзатель'], ['Тяг', 'Тягун'], ['Гром', 'Громовержец'], ['Мульт', 'Тройной выстрел'], ['БП', 'Быстрая перезарядка'],
  ['Скв', 'Пронзающая стрела'], ['Почин', 'Починка'], ['ПрН', 'Проклятие несъёмности'], ['ПрУ', 'Проклятие утраты'], ['Плот', 'Плотность'], ['Проб', 'Пробитие'], ['Вет', 'Порыв ветра']]
function AbbrTab() {
  return <div className="card rounded-3xl p-5"><b className="text-lg">Сокращения зачарований</b><p className="muted text-sm mb-3">Так они подписаны над предметами игроков («Инфо над игроками»). Цифра после — уровень: «Остр5» = Острота V.</p>
    <div className="grid gap-2" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>{ABBR.map(([a, n]) => <div key={a} className="flex items-center gap-3 rounded-xl p-2" style={{ background: 'var(--chip)' }}><b className="w-14 text-center rounded-lg py-1" style={{ background: 'linear-gradient(135deg,var(--a1),var(--a2))', color: '#fff' }}>{a}</b><span className="text-sm">{n}</span></div>)}</div></div>
}
/** the crosshair as the game draws it (Hud.crosshair), scaled up */
function CrossPreview({ s, px = 8 }: { s: any; px?: number }) {
  const st = +s.style, size = +s.size, gap = +s.gap, th = +s.thick, al = +s.alpha, ot = s.outline ? +s.othick : 0, col = s.color || '#fff', W = 30, c = W / 2, o = 'rgba(0,0,0,.85)'
  const els: JSX.Element[] = []; const R = (x0: number, y0: number, x1: number, y1: number, key: string) => { if (ot) els.push(<rect key={key + 'o'} x={x0 - ot} y={y0 - ot} width={x1 - x0 + 2 * ot} height={y1 - y0 + 2 * ot} fill={o} />); els.push(<rect key={key} x={x0} y={y0} width={x1 - x0} height={y1 - y0} fill={col} />) }
  if (st === 6) { const p = String(s.pattern || ''), n = Math.round(Math.sqrt(p.length)); if (n * n === p.length) for (let i = 0; i < p.length; i++) if (p[i] === '1') R(c - n * th / 2 + (i % n) * th, c - n * th / 2 + Math.floor(i / n) * th, c - n * th / 2 + (i % n + 1) * th, c - n * th / 2 + (Math.floor(i / n) + 1) * th, 'p' + i) }
  if (st === 0 || st === 1 || st === 5) for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) { if (st === 5 && dy < 0) continue
    const x1 = c + dx * gap, y1 = c + dy * gap, x2 = c + dx * (gap + size), y2 = c + dy * (gap + size), hx = dx === 0 ? th / 2 : 0, hy = dy === 0 ? th / 2 : 0
    R(Math.min(x1, x2) - hx, Math.min(y1, y2) - hy, Math.max(x1, x2) + hx, Math.max(y1, y2) + hy, 'a' + dx + dy) }
  if (st === 3 || st === 4) { if (ot) els.push(<circle key="ro" cx={c} cy={c} r={size + gap} fill="none" stroke={o} strokeWidth={th + 2 * ot} />); els.push(<circle key="r" cx={c} cy={c} r={size + gap} fill="none" stroke={col} strokeWidth={th} />) }
  if (st === 1 || st === 2 || st === 4) { const r = Math.max(0.6, th * 0.75) * (st === 2 ? 1.4 : 1); if (ot) els.push(<circle key="do" cx={c} cy={c} r={r + ot} fill={o} />); els.push(<circle key="d" cx={c} cy={c} r={r} fill={col} />) }
  if (st === 8) els.push(<text key="i" x={c} y={c + 1.5} fontSize={4} textAnchor="middle" fill="#fff">PNG</text>)
  if (st === 7) els.push(<g key="v" opacity={0.9}><rect x={c - 4.5} y={c - 0.5} width={9} height={1} fill="#fff" /><rect x={c - 0.5} y={c - 4.5} width={1} height={9} fill="#fff" /></g>)
  return <svg width={W * px} height={W * px} viewBox={`0 0 ${W} ${W}`} style={{ opacity: al, shapeRendering: 'crispEdges' }}>{els}</svg>
}
function CrosshairTab({ m, on, set, say }: { m: ModCfg; on: (v: boolean) => void; set: (k: string, v: any) => void; say: (s: string, k?: any) => void }) {
  const [xh, setXh] = useState<string | null>(null); useEffect(() => { window.api.xhGet?.().then(setXh) }, [])
  const sch = MODS.find((x) => x.id === 'crosshair')!, s = m.s, [bg, setBg] = useState(0), BGS = ['linear-gradient(160deg,#5b8c32,#2d4a1a)', 'linear-gradient(160deg,#87c6ff,#cfe9ff)', 'linear-gradient(160deg,#1b1b24,#050508)']
  return <div className="grid gap-5" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.1fr)' }}>
    <div className="card rounded-3xl p-5 flex flex-col gap-4">
      <div className="flex items-center justify-between"><b className="text-lg">Прицел</b><Switch on={m.on} set={on} label="Свой прицел" /></div>
      <div className="rounded-2xl flex items-center justify-center" style={{ height: 300, background: BGS[bg] }}><CrossPreview s={s} px={9} /></div>
      <div className="flex gap-1.5 items-center"><span className="muted text-xs mr-1">Фон:</span>{['Трава', 'Небо', 'Ночь'].map((l, i) => <button key={l} className={'pill !py-0.5 ' + (bg === i ? 'on' : '')} onClick={() => setBg(i)}>{l}</button>)}
        <div className="flex-1" /><button className="pill" title="Тонкий белый крест, как обычный прицел Minecraft, но с нашими настройками" onClick={() => { for (const [k, v] of [['style', 0], ['size', 4.5], ['gap', 0], ['thick', 1], ['outline', false], ['color', '#ffffff'], ['alpha', 0.95]] as [string, any][]) set(k, v) }}>Как в Minecraft</button>
        <button className="pill" onClick={async () => { const r = await window.api.shareMake('crosshair', s); r?.id ? say(r.long ? 'Код прицела скопирован — отправьте его другу целиком' : 'Код ' + r.id + ' скопирован — отправьте его другу') : say(r?.error || 'Не удалось', 'err') }}><Icon n="share" s={13} />Поделиться прицелом</button></div>
      <div><div className="text-sm mb-1.5">Форма</div><div className="grid grid-cols-4 gap-2">{sch.s[0].options!.map((o, i) => <button key={o} onClick={() => set('style', i)} className="rounded-xl flex flex-col items-center gap-1 py-2 transition" style={{ background: +s.style === i ? 'linear-gradient(135deg,var(--a1),var(--a2))' : 'var(--chip)', color: +s.style === i ? '#fff' : undefined }}>
        <span className="rounded-lg flex items-center justify-center" style={{ background: 'rgba(0,0,0,.35)', width: 56, height: 56 }}><CrossPreview s={{ ...s, style: i, alpha: 1 }} px={1.8} /></span><span className="text-[11px] font-semibold">{o}</span></button>)}</div></div>
    </div>
    <div className="card rounded-3xl p-5 flex flex-col gap-3">
      <div className="card rounded-2xl p-3 flex items-center gap-3"><span className="w-14 h-14 rounded-xl flex items-center justify-center shrink-0 checker">{xh ? <img src={xh} className="w-10 h-10 object-contain" /> : <Icon n="image" s={20} />}</span>
        <div className="flex-1 min-w-0"><b className="text-sm">Своя картинка прицела</b><div className="muted text-xs">PNG с прозрачным фоном — как ванильный, только ваш. Форма «Своя картинка», размер — ползунком «Длина»</div></div>
        <button className="btn !py-1.5 text-sm" onClick={async () => { const d = await window.api.pickPng(); if (d) { await window.api.xhSet(d); setXh(d); set('style', 8); say('Картинка прицела установлена') } }}>Выбрать</button>
        {xh && <button className="btn !py-1.5" onClick={async () => { await window.api.xhSet(null); setXh(null); if (+s.style === 8) set('style', 1) }}><Icon n="trash" s={14} /></button>}</div>
      {sch.s.filter((x) => x.id !== 'style').map((x) => <Setting key={x.id} mod="crosshair" s={x} v={s[x.id]} set={(v) => set(x.id, v)} />)}
    </div></div>
}
/** draw your own crosshair: 15 x 15 cells, saved as a string of 0/1 (the game draws each cell "thickness" pixels big) */
const PN = 15
function PatternEditor({ v, set }: { v: string; set: (v: string) => void }) {
  const cells = (v && v.length === PN * PN ? v : '0'.repeat(PN * PN)).split('')
  const paint = useRef<string | null>(null)
  const put = (i: number) => { if (paint.current == null || cells[i] === paint.current) return; cells[i] = paint.current; set(cells.join('')) }
  const preset = (fn: (x: number, y: number) => boolean) => set(Array.from({ length: PN * PN }, (_, i) => (fn(i % PN - 7, Math.floor(i / PN) - 7) ? '1' : '0')).join(''))
  return <div onMouseUp={() => (paint.current = null)} onMouseLeave={() => (paint.current = null)}>
    <div className="text-sm mb-1">Свой рисунок <span className="muted text-xs">— выберите форму «Свой рисунок», рисуйте мышью (правая кнопка стирает)</span></div>
    <div className="flex gap-3 items-start">
      <div className="grid select-none rounded-lg overflow-hidden" style={{ gridTemplateColumns: `repeat(${PN}, 14px)`, background: '#0b1220', border: '1px solid var(--line)' }} onContextMenu={(e) => e.preventDefault()}>
        {cells.map((c, i) => <div key={i} style={{ width: 14, height: 14, background: c === '1' ? 'var(--a1)' : i === 112 ? 'rgba(255,255,255,.12)' : 'transparent', outline: '1px solid rgba(255,255,255,.05)' }}
          onMouseDown={(e) => { paint.current = e.button === 2 ? '0' : cells[i] === '1' ? '0' : '1'; put(i) }} onMouseEnter={() => put(i)} />)}</div>
      <div className="flex flex-col gap-1.5">
        <button className="pill" onClick={() => preset((x, y) => (x === 0 || y === 0) && Math.abs(x) + Math.abs(y) >= 2 && Math.abs(x) + Math.abs(y) <= 5)}>Крест</button>
        <button className="pill" onClick={() => preset((x, y) => { const d = Math.hypot(x, y); return d > 3.5 && d < 4.6 || (x === 0 && y === 0) })}>Круг</button>
        <button className="pill" onClick={() => preset((x, y) => Math.abs(x) === Math.abs(y) && Math.abs(x) >= 1 && Math.abs(x) <= 3)}>Икс</button>
        <button className="pill" onClick={() => set('0'.repeat(PN * PN))}>Очистить</button>
      </div>
    </div>
  </div>
}
/** pet pictures: the same textures the game draws (mods/common-visuals/resources/assets/asteria_visuals/textures/fx/trinket) */
export const PET_PIC: Record<string, string> = Object.fromEntries(Object.entries(import.meta.glob('../mods/common-visuals/resources/assets/asteria_visuals/textures/fx/trinket/*.png', { eager: true, query: '?url', import: 'default' }) as Record<string, string>).map(([k, v]) => [k.split('/').pop()!.replace('.png', ''), v]))
/** pictures of the 3D pets (index 10+), rendered from the same boxes the game draws */
export const VOX_PIC: Record<number, string> = Object.fromEntries(Object.entries(import.meta.glob('./assets/pets/pet-*.png', { eager: true, query: '?url', import: 'default' }) as Record<string, string>).map(([k, v]) => [+k.replace(/.*pet-|.png/g, ''), v]))
export const petPic = (i: number) => i >= 10 ? VOX_PIC[i] : PET_PIC[PET_FILES[i]]
export const PET_FILES = ['cat', 'ghost', 'flame', 'gem', 'atom', 'flower', 'donut', 'ball', 'banana']
function Setting({ s, v, set, mod }: { s: S; v: any; set: (v: any) => void; mod?: string }) {
  if (mod === 'pet' && s.id === 'kind') return <div><div className="text-sm mb-1.5">{s.name}</div><div className="grid gap-1.5" style={{ gridTemplateColumns: 'repeat(5, minmax(0, 1fr))' }}>
    {s.options!.map((o, i) => { const pic = petPic(i); return <button key={o} title={o} onClick={() => set(i)} className="rounded-xl flex flex-col items-center gap-1 p-1.5 transition" style={{ background: +v === i ? 'linear-gradient(135deg,var(--a1),var(--a2))' : 'var(--chip)', color: +v === i ? '#fff' : undefined }}>
      {pic ? <img src={pic} className="w-9 h-9 object-contain" draggable={false} /> : <span className="w-9 h-9 flex items-center justify-center"><Icon n="user" s={22} /></span>}<span className="text-[10.5px] font-semibold leading-tight truncate w-full text-center">{o}</span></button> })}</div></div>
  if (s.type === 'str' && s.id === 'pattern') return <PatternEditor v={String(v ?? '')} set={set} />
  if (s.type === 'str') return <label className="block"><div className="text-sm mb-1">{s.name}</div><input className="w-full" placeholder="ezz" value={String(v ?? '')} maxLength={100} onChange={(e) => set(e.target.value)} /></label>
  if (s.type === 'bool') return <Switch on={!!v} set={set} label={s.name} />
  if (s.type === 'num') return <Range label={s.name} min={s.min!} max={s.max!} step={s.step!} v={+v} set={set} show={(x) => (s.step! < 1 ? x.toFixed(2).replace(/0$/, '') : String(x))} />
  if (s.type === 'color') return <div className="flex items-center justify-between"><span className="text-sm">{s.name}</span><ColorPick value={String(v)} set={set} size="sm" /></div>
  if (s.options!.length > 4) return <div><div className="text-sm mb-1.5">{s.name}</div><div className="flex gap-1.5 flex-wrap">{s.options!.map((o, i) => <button key={o} className={'pill !py-1 ' + (+v === i ? 'on' : '')} onClick={() => set(i)}>{o}</button>)}</div></div>
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
    {on('watermark') && <div style={{ ...panel, position: 'absolute', left: 10, top: 10, display: 'flex', gap: 6, alignItems: 'center' }}><span style={{ background: grad, WebkitBackgroundClip: 'text', color: 'transparent', fontWeight: 900 }}>Asteria</span><span style={{ opacity: .7 }}>| 144 fps</span></div>}
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
