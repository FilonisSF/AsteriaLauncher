import { type ReactNode, useEffect, useMemo, useState } from 'react'
import { Icon } from './Icons'
import { Modal, Tilt } from './ui'

const api = (window as any).api

// ------------------------------------------------------------------ Minecraft text: &a §l &#RRGGBB §x§R§R§G§G§B§B
const PAL: Record<string, string> = { '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA', '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA', '8': '#555555', '9': '#5555FF', a: '#55FF55', b: '#55FFFF', c: '#FF5555', d: '#FF55FF', e: '#FFFF55', f: '#FFFFFF' }
type Seg = { t: string; c?: string; b?: boolean; i?: boolean; u?: boolean; s?: boolean; k?: boolean }
export function parseMc(src: string): Seg[] {
  const out: Seg[] = []; let cur: Seg = { t: '' }, i = 0
  const push = () => { if (cur.t) out.push(cur); cur = { ...cur, t: '' } }
  const s = String(src ?? '')
  while (i < s.length) {
    const ch = s[i]
    if ((ch === '§' || ch === '&') && i + 1 < s.length) {
      const n = s[i + 1].toLowerCase()
      if (n === '#' && /^[0-9a-f]{6}$/i.test(s.slice(i + 2, i + 8))) { push(); cur = { t: '', c: '#' + s.slice(i + 2, i + 8) }; i += 8; continue }
      if (n === 'x') { const m = s.slice(i, i + 14).match(/^[§&]x([§&][0-9a-f]){6}/i); if (m) { push(); cur = { t: '', c: '#' + m[0].replace(/[§&]/g, '').slice(1) }; i += 14; continue } }
      if (PAL[n]) { push(); cur = { t: '', c: PAL[n] }; i += 2; continue }
      if ('klmnor'.includes(n)) { push(); if (n === 'r') cur = { t: '' }; else cur = { ...cur, ...{ k: { k: true }, l: { b: true }, m: { s: true }, n: { u: true }, o: { i: true } }[n as 'k'] }; i += 2; continue }
    }
    cur.t += ch; i++
  }
  push(); return out
}
export const stripMc = (s: string) => parseMc(s).map((x) => x.t).join('')
/** in-game coloured text. plate = dark tag background, so white/yellow names stay readable on the light theme */
export function McText({ s, plate, className = '' }: { s?: string; plate?: boolean; className?: string }) {
  if (!s) return null
  return <span className={(plate ? 'mcplate ' : '') + 'mctext ' + className}>{parseMc(s).map((g, i) => <span key={i} className={g.k ? 'mck' : ''} style={{ color: g.c, fontWeight: g.b ? 800 : undefined, fontStyle: g.i ? 'italic' : undefined, textDecoration: [g.u && 'underline', g.s && 'line-through'].filter(Boolean).join(' ') || undefined }}>{g.t}</span>)}</span>
}
const gradText = (t: string, g: string[]) => <span style={{ background: `linear-gradient(90deg,${g.join(',')})`, WebkitBackgroundClip: 'text', color: 'transparent', fontWeight: 900 }}>{t}</span>

// ------------------------------------------------------------------ heads (menu heads by texture hash, players by nick)
const headCache = new Map<string, string>()
export function Head({ tex, name, s = 40, glow }: { tex?: string; name?: string; s?: number; glow?: string }) {
  const key = tex || name || '', [src, setSrc] = useState(headCache.get(key) || '')
  useEffect(() => {
    if (!key || headCache.get(key)) return; let off = false
    ;(async () => {
      const urls = tex ? [`https://textures.minecraft.net/texture/${tex}`] : [`https://mc-heads.net/skin/${name}`, `https://minotar.net/skin/${name}`]
      for (const u of urls) {
        const d = await api.img(u).catch(() => null); if (!d) continue
        const im = new Image(); im.src = d; await im.decode().catch(() => {})
        const k = im.width / 64, c = document.createElement('canvas'); c.width = c.height = 64
        const g = c.getContext('2d')!; g.imageSmoothingEnabled = false
        g.drawImage(im, 8 * k, 8 * k, 8 * k, 8 * k, 0, 0, 64, 64); g.drawImage(im, 40 * k, 8 * k, 8 * k, 8 * k, 0, 0, 64, 64) // face + hat layer
        const url = c.toDataURL(); headCache.set(key, url); if (!off) setSrc(url); return
      }
    })()
    return () => { off = true }
  }, [key])
  return <span className="head" style={{ width: s, height: s, ...(glow ? { boxShadow: `0 8px 24px -6px ${glow}, 0 0 0 1px ${glow}55` } : {}) }}>{src ? <img src={src} /> : <span className="w-full h-full block" style={{ background: glow ? `linear-gradient(135deg,${glow},#0b1220)` : 'var(--chip)' }} />}</span>
}

// ------------------------------------------------------------------ durations: "1 ч. 5 мин. 3 сек." -> seconds, so timers keep ticking between server updates
export function parseDur(raw: string): number | null {
  const t = stripMc(raw).toLowerCase(); let sec = 0, hit = false
  const re = /(\d+)\s*(дн|д|d|ч|h|мин|м|m|сек|с|s)/g; let m
  while ((m = re.exec(t))) { hit = true; const v = +m[1], u = m[2]; sec += v * (/^(дн|д|d)$/.test(u) ? 86400 : /^(ч|h)$/.test(u) ? 3600 : /^(мин|м|m)$/.test(u) ? 60 : 1) }
  if (!hit) { const c = t.match(/^\s*(?:(\d+):)?(\d{1,2}):(\d{2})\s*$/); if (c) return (+(c[1] || 0)) * 3600 + +c[2] * 60 + +c[3] }
  return hit ? sec : null
}
export const fmtDur = (s: number) => { s = Math.max(0, Math.round(s)); const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), x = s % 60
  return h ? `${h} ч ${String(m).padStart(2, '0')} мин` : m ? `${m} мин ${String(x).padStart(2, '0')} сек` : `${x} сек` }
const fmtShort = (s: number) => { s = Math.max(0, Math.round(s)); const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), x = s % 60; return h ? `${h}ч ${String(m).padStart(2, '0')}м` : `${m}м ${String(x).padStart(2, '0')}с` }
const useNow = (ms = 1000) => { const [n, setN] = useState(Date.now()); useEffect(() => { const id = setInterval(() => setN(Date.now()), ms); return () => clearInterval(id) }, [ms]); return n }
const ago = (t: number, now: number) => { const s = Math.round((now - t) / 1000); return s < 60 ? 'только что' : s < 3600 ? `${Math.floor(s / 60)} мин назад` : s < 86400 ? `${Math.floor(s / 3600)} ч назад` : new Date(t).toLocaleDateString('ru-RU') }

// ------------------------------------------------------------------ server events (config: events[], live: AsteriaBridge -> /events)
export function useEvents() {
  const [d, setD] = useState<any>(null)
  useEffect(() => { const get = () => api.events?.().then((x: any) => x && setD(x)).catch(() => {}); get(); const id = setInterval(get, 15000); return () => clearInterval(id) }, [])
  return d
}
function EventTimer({ live, at, now, big, compact }: { live?: string; at: number; now: number; big?: boolean; compact?: boolean }) {
  if (!live) return <span className="muted">—</span>
  const s = parseDur(live)
  if (s == null) return <McText s={live} />
  const left = s - (now - at) / 1000
  return <span className={'tabular-nums ' + (big ? 'font-black' : 'font-bold')}>{left <= 0 ? 'скоро' : compact ? fmtShort(left) : fmtDur(left)}</span>
}
export function EventsBlock({ events, intro }: { events: any[]; intro?: string }) {
  const [open, setOpen] = useState<any>(null)
  if (!events?.length) return null
  const title = (e: any) => gradText(e.name, e.grad?.length ? e.grad : [e.color, e.color])
  return <>
    <div className="section">Ивенты сервера</div>
    {intro && <p className="muted text-sm -mt-1 mb-3 max-w-3xl">{intro}</p>}
    <div className="evgrid stagger">{events.map((e) => <Tilt key={e.id} max={6} className="evcard" onClick={() => setOpen(e)}>
        <div className="evglow" style={{ background: `radial-gradient(120% 90% at 0% 0%, ${e.color}55, transparent 60%)` }} />
        <div className="relative"><Head tex={e.head} s={44} glow={e.color} />
          <div className="min-w-0 mt-3"><div className="text-[15px] leading-tight truncate">{title(e)}</div>
            <div className="text-[11px] font-bold uppercase tracking-wider mt-0.5" style={{ color: e.color }}>{e.world}</div></div></div>
        <p className="relative text-xs mt-2 opacity-80 line-clamp-3">{e.short || (e.lines || [])[1]}</p>
        <div className="relative flex gap-1.5 mt-3 flex-wrap">{e.id !== 'blackauc' && <span className="chip-xs" style={{ color: '#fb7185' }}><Icon n="swords" s={11} />PvP</span>}<span className="chip-xs"><Icon n="arrow" s={11} />подробнее</span></div>
      </Tilt>)}</div>
    {open && <Modal open title="" onClose={() => setOpen(null)} w={520}>
      <div className="flex items-center gap-4 mb-4"><Head tex={open.head} s={64} glow={open.color} />
        <div><div className="text-2xl">{title(open)}</div><div className="text-xs font-bold uppercase tracking-wider" style={{ color: open.color }}>{open.world}</div></div></div>
      <div className="flex flex-col gap-2 text-sm">{(open.lines || []).map((x: string, i: number) => <div key={i} className="flex gap-2"><span className="w-1 rounded-full shrink-0" style={{ background: open.color }} />{x}</div>)}</div>
      <div className="flex flex-col gap-1.5 mt-4">{(open.tips || []).map((x: string, i: number) => <div key={i} className="flex gap-2 text-sm"><span style={{ color: open.color }}>☀</span>{x}</div>)}</div>
      {open.cmd && <button className="btn mt-4 flex items-center gap-2" onClick={() => { navigator.clipboard.writeText(open.cmd) }}><Icon n="copy" s={14} />{open.cmd}</button>}
    </Modal>}
  </>
}

// ------------------------------------------------------------------ clans (SafiClan launcher-sync -> /clans)
export function useClans() {
  const [d, setD] = useState<any>(null)
  useEffect(() => { const get = () => api.clans?.().then((x: any) => x && setD(x)).catch(() => {}); get(); const id = setInterval(get, 30000); return () => clearInterval(id) }, [])
  return d
}
const num = (n: number) => (n ?? 0).toLocaleString('ru-RU')
// ꒙ / ꒉ are resource-pack glyphs on the server; the launcher draws its own coin and sapphire
export const Coin = ({ s = 14 }: { s?: number }) => <svg width={s} height={s} viewBox="0 0 16 16" className="inline-block align-[-2px]"><circle cx="8" cy="8" r="7" fill="#f59e0b" /><circle cx="8" cy="8" r="5" fill="#fbbf24" stroke="#d97706" strokeWidth="1" /><path d="M6.5 5.5h3M6.5 10.5h3M8 5.5v5" stroke="#b45309" strokeWidth="1.3" strokeLinecap="round" /></svg>
export const Sapph = ({ s = 14 }: { s?: number }) => <svg width={s} height={s} viewBox="0 0 16 16" className="inline-block align-[-2px]"><path d="M8 1.5l5.5 4L8 14.5 2.5 5.5z" fill="#38bdf8" /><path d="M8 1.5l5.5 4H2.5z" fill="#7dd3fc" /><path d="M8 5.5v9" stroke="#0ea5e9" strokeWidth=".8" /></svg>
const money = (n: number, kind: 'coin' | 'sap') => <span className="inline-flex items-center gap-1">{num(n)}{kind === 'coin' ? <Coin /> : <Sapph />}</span>
const SORTS: [string, string, (c: any) => number, (c: any, ic: any) => ReactNode][] = [
  ['level', 'Уровень', (c) => c.level * 1e9 + c.xp, (c) => `${c.level} ур.`],
  ['vault', 'Казна', (c) => c.vault, (c) => money(c.vault, 'coin')],
  ['points', 'Сапфиры', (c) => c.points, (c) => money(c.points, 'sap')],
  ['kills', 'Убийства', (c) => c.kills, (c) => `${num(c.kills)} Θ`],
  ['rep', 'Честь', (c) => c.rep, (c) => (c.rep >= 0 ? '+' : '') + c.rep],
  ['rep_neg', 'Разбойники', (c) => -c.rep, (c) => String(c.rep)],
  ['members', 'Участники', (c) => c.members?.length || 0, (c) => String(c.members?.length || 0)],
]
const ROLE_ICON: Record<string, string> = { owner: 'crown', deputy_owner: 'shield', curator: 'star', regular: 'check', novice: 'user' }
const Bar = ({ v, max, color }: { v: number; max: number; color?: string }) => <div className="h-2 rounded-full overflow-hidden" style={{ background: 'var(--chip)' }}><div className="h-full rounded-full transition-all duration-700" style={{ width: Math.min(100, (v / Math.max(1, max)) * 100) + '%', background: color || 'linear-gradient(90deg,var(--a1),var(--a2))' }} /></div>
const Kpi = ({ l, v, icon }: { l: string; v: ReactNode; icon?: string }) => <div className="kpi !p-3"><div className="muted text-[11px] flex items-center gap-1">{icon && <Icon n={icon} s={12} />}{l}</div><div className="text-lg font-black mt-0.5 truncate">{v}</div></div>

function ClanModal({ c, icons, onClose, Avatar, me }: { c: any; icons: any; onClose: () => void; Avatar: any; me?: string }) {
  const now = useNow(30000), on = (c.members || []).filter((m: any) => m.online).length
  return <Modal open title="" onClose={onClose} w={760}>
    <div className="clanhero mb-4"><div className="flex items-center gap-4 flex-wrap"><div className="text-3xl"><McText s={c.display || c.name} plate /></div>
      <span className="badge rec"><Icon n="star" s={11} fill />{c.level} уровень</span>{c.pvp && <span className="badge" style={{ background: 'rgba(244,63,94,.18)', color: '#fb7185' }}><Icon n="swords" s={11} />PvP в клане</span>}</div>
      <div className="flex items-center gap-2 mt-2 text-sm"><Icon n="crown" s={14} />Владелец: <b>{c.owner}</b><span className="muted">·</span><McText s={c.repText} /></div>
      <div className="mt-3"><div className="flex justify-between text-xs muted mb-1"><span>Опыт клана</span><span>{c.maxLevel ? 'максимальный уровень' : `${num(c.xp)} / ${num(c.xpNeed)} XP`}</span></div><Bar v={c.maxLevel ? 1 : c.xp} max={c.maxLevel ? 1 : c.xpNeed} /></div></div>
    <div className="grid grid-cols-4 gap-2 mb-4"><Kpi l="Казна" v={money(c.vault, 'coin')} icon="shop" /><Kpi l="Сапфиры" v={money(c.points, 'sap')} icon="sparkles" /><Kpi l="Убийства / смерти" v={`${num(c.kills)} / ${num(c.deaths)}`} icon="swords" /><Kpi l="Репутация" v={(c.rep >= 0 ? '+' : '') + c.rep} icon="shield" /></div>
    <div className="grid gap-4" style={{ gridTemplateColumns: 'minmax(0,1.4fr) minmax(0,1fr)' }}>
      <div><div className="font-bold mb-2 flex items-center gap-2"><Icon n="user" s={15} />Участники <span className="muted text-sm">{c.members?.length} · в сети {on}</span></div>
        <div className="flex flex-col gap-1.5 max-h-[340px] overflow-auto pr-1">{(c.members || []).map((m: any) => <div key={m.name} className={'mrow ' + (me && m.name.toLowerCase() === me.toLowerCase() ? 'me' : '')}>
          <span className="relative"><Avatar name={m.name} s={30} />{m.online && <span className="glow-dot absolute -right-0.5 -bottom-0.5 !w-2.5 !h-2.5" />}</span>
          <div className="min-w-0 flex-1"><div className="font-semibold text-sm truncate">{m.name}</div><div className="text-[11px] flex items-center gap-1"><Icon n={ROLE_ICON[m.role] || 'user'} s={10} /><McText s={m.roleText} /></div></div>
          <div className="text-right text-[11px] muted tabular-nums">{m.kills} Θ · {m.deaths} Δ<div>{m.xp ? num(m.xp) + ' XP' : ''}</div></div></div>)}</div></div>
      <div className="flex flex-col gap-4">
        <div><div className="font-bold mb-2 flex items-center gap-2"><Icon n="swords" s={15} />Войны</div>{c.wars?.length ? <div className="flex flex-col gap-1.5">{c.wars.map((w: any) => <div key={w.clan} className="mrow"><McText s={w.display} plate className="text-sm" /><span className="ml-auto text-[11px] muted text-right">{w.kills} убийств<div>с {ago(w.since, now)}</div></span></div>)}</div> : <p className="muted text-sm">Мирное время</p>}</div>
        <div><div className="font-bold mb-2 flex items-center gap-2"><Icon n="shield" s={15} />Союзы</div>{c.allies?.length ? <div className="flex gap-1.5 flex-wrap">{c.allies.map((a: any) => <McText key={a.clan} s={a.display} plate className="text-sm" />)}</div> : <p className="muted text-sm">Пока нет союзников</p>}</div>
      </div></div>
  </Modal>
}

/** "Мой клан" card for the profile page */
export function MyClan({ data, me, onOpen }: { data: any; me?: string; onOpen: (c: any) => void }) {
  const c = me && data?.clans?.find((x: any) => x.members?.some((m: any) => m.name.toLowerCase() === me.toLowerCase()))
  if (!c) return <div className="card rounded-3xl p-5"><div className="font-bold mb-1 flex items-center gap-2"><Icon n="shield" s={16} />Клан</div><p className="muted text-sm">{data?.clans ? 'Вы пока не в клане. Создайте свой: /clan create <название> или вступите по приглашению.' : 'Данные о кланах появятся, когда сервер передаст их лаунчеру.'}</p></div>
  const role = c.members.find((m: any) => m.name.toLowerCase() === me!.toLowerCase())
  return <button className="card rounded-3xl p-5 text-left clanmini" onClick={() => onOpen(c)}>
    <div className="flex items-center gap-3"><div className="text-xl"><McText s={c.display} plate /></div><span className="badge rec !py-0.5">{c.level} ур.</span><span className="ml-auto muted"><Icon n="arrow" s={16} /></span></div>
    <div className="text-sm mt-2 flex items-center gap-1.5"><Icon n={ROLE_ICON[role?.role] || 'user'} s={13} /><McText s={role?.roleText} /><span className="muted">· {c.members.length} участников</span></div>
    <div className="mt-3"><Bar v={c.maxLevel ? 1 : c.xp} max={c.maxLevel ? 1 : c.xpNeed} /></div></button>
}

export default function ClansTab({ data, me, Avatar, initial }: { data: any; me?: string; Avatar: any; initial?: any }) {
  const [sort, setSort] = useState('level'), [q, setQ] = useState(''), [open, setOpen] = useState<any>(initial || null), now = useNow(10000)
  const icons = data?.icons || {}, S = SORTS.find((x) => x[0] === sort)!
  const list = useMemo(() => [...(data?.clans || [])].sort((a, b) => S[2](b) - S[2](a)), [data, sort])
  const shown = list.filter((c) => !q || (c.name + ' ' + c.owner + ' ' + (c.members || []).map((m: any) => m.name).join(' ')).toLowerCase().includes(q.toLowerCase()))
  const mine = me && list.find((c) => c.members?.some((m: any) => m.name.toLowerCase() === me.toLowerCase()))
  const total = list.reduce((a, c) => a + (c.members?.length || 0), 0), wars = list.reduce((a, c) => a + (c.wars?.length || 0), 0) / 2
  return <div className="fade">
    <div className="ph"><div><h1 className="grad-text">Кланы</h1><div className="sub">{data?.time ? `Живые данные с сервера · обновлено ${ago(data.time, now)}` : 'Топы и составы кланов SafiMine'}</div></div>
      <div className="relative w-[260px]"><input className="w-full !pl-10" placeholder="Клан, глава или игрок…" value={q} onChange={(e) => setQ(e.target.value)} /><span className="absolute left-3 top-2.5 muted"><Icon n="search" s={18} /></span></div></div>
    {!list.length ? <div className="card rounded-3xl p-10 text-center"><div className="w-16 h-16 rounded-2xl mx-auto mb-3 flex items-center justify-center" style={{ background: 'linear-gradient(135deg,var(--a1),var(--a2))', color: '#fff' }}><Icon n="shield" s={30} /></div>
      <div className="text-xl font-black">Кланы скоро появятся</div><p className="muted text-sm mt-1 max-w-md mx-auto">Здесь будут топы, составы, войны и союзы всех кланов сервера. Данные приходят прямо от плагина кланов, как только сервер их передаст.</p></div> : <>
      <div className="grid grid-cols-4 gap-3 mb-5"><Kpi l="Кланов" v={num(list.length)} icon="shield" /><Kpi l="Игроков в кланах" v={num(total)} icon="user" /><Kpi l="Идёт войн" v={num(wars)} icon="swords" /><Kpi l="Лучший клан" v={<McText s={list[0]?.display} plate />} icon="crown" /></div>
      {mine && <button className="clanhero w-full text-left mb-5" onClick={() => setOpen(mine)}><div className="text-xs uppercase tracking-wider muted mb-1">Ваш клан</div>
        <div className="flex items-center gap-3 flex-wrap"><span className="text-3xl"><McText s={mine.display} plate /></span><span className="badge rec"><Icon n="star" s={11} fill />{mine.level} уровень</span><span className="badge soft">#{[...(data.clans)].sort((a: any, b: any) => SORTS[0][2](b) - SORTS[0][2](a)).indexOf(mine) + 1} в топе</span></div>
        <div className="grid grid-cols-4 gap-2 mt-3"><Kpi l="Казна" v={money(mine.vault, 'coin')} /><Kpi l="Сапфиры" v={money(mine.points, 'sap')} /><Kpi l="Участники" v={`${mine.members.length} · в сети ${mine.members.filter((m: any) => m.online).length}`} /><Kpi l="Войны / союзы" v={`${mine.wars?.length || 0} / ${mine.allies?.length || 0}`} /></div></button>}
      <div className="seg mb-4 flex-wrap">{SORTS.map(([k, n]) => <button key={k} className={sort === k ? 'on' : ''} onClick={() => setSort(k)}>{n}</button>)}</div>
      {!q && <div className="podium mb-4">{[1, 0, 2].map((i) => list[i] && <Tilt key={list[i].name} max={6} className={'pod p' + (i + 1)} onClick={() => setOpen(list[i])}>
        <div className="place">{i + 1}</div><div className="text-xl truncate max-w-full"><McText s={list[i].display} plate /></div>
        <div className="muted text-xs mt-1 truncate">глава {list[i].owner} · {list[i].members?.length} уч.</div><div className="font-black text-lg mt-2">{S[3](list[i], icons)}</div></Tilt>)}</div>}
      <div className="flex flex-col gap-1.5 stagger">{shown.slice(q ? 0 : 3, 100).map((c) => <button key={c.name} className="crow" onClick={() => setOpen(c)}>
        <span className="w-8 text-center font-black muted tabular-nums">{list.indexOf(c) + 1}</span><span className="min-w-0 flex-1 text-left"><McText s={c.display} plate /></span>
        <span className="muted text-xs w-36 flex items-center gap-1 min-w-0"><Icon n="crown" s={12} /><span className="truncate">{c.owner}</span></span><span className="muted text-xs w-20 text-right">{c.members?.length} уч.</span>
        <span className="font-bold w-32 text-right tabular-nums">{S[3](c, icons)}</span></button>)}</div></>}
    {open && <ClanModal c={open} icons={icons} me={me} Avatar={Avatar} onClose={() => setOpen(null)} />}
  </div>
}
