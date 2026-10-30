import { useEffect, useMemo, useRef, useState } from 'react'
import hero from './assets/safimine.jpg'
import bgServer from './assets/bg-server.jpg'
import grumImg from './assets/servers/grumworld.jpg'
import grumIcon from './assets/servers/grumworld-icon.png'
import hudBg from './assets/hud-bg.jpg'
import defSkin from './assets/default-skin.png'
import defConf from './config.default.json'
import avatar from './assets/avatar.png'
import Fx from './Fx'
import Skin3D from './Skin3D'
import SkinStudio, { CapeMaker, GifCapeMaker } from './SkinStudio'
import GameSettings from './GameSettings'
import VisualsTab, { visDefaults, petPic } from './VisualsTab'
import { MapsCatalog } from './MapsCatalog'
import { Worlds } from './Worlds'
import { LookPreview } from './LookPreview'
import { BackupCard, UninstallCard } from './Backup'
import { LegalCard } from './Legal'
/** first start: the language of Windows if the launcher has it (Russian for ru/be/kk), otherwise English */
const deviceLang = () => { try { const l = (navigator.languages?.[0] || navigator.language || 'ru').slice(0, 2).toLowerCase(); if (['ru', 'be', 'kk'].includes(l)) return 'ru'; return LANGS.some(([k]) => k === l) ? l : 'en' } catch { return 'ru' } }
import { EventsBlock } from './Clans'
import Versions, { ALL_VERSIONS, ASTERIA, fabricFor } from './Versions'
import { VINFO } from './versionInfo'
import { Splash, LaunchOverlay, UpdateModal, LogModal } from './Overlays'
import { Icon, Gem, Wordmark, FLAGS } from './Icons'
import { Check, ColorPick, Dropdown, LoaderIcon, Modal, Palette, Switch, Tilt, type Cmd } from './ui'
import { LANGS, tr } from './i18n'
import { setUiLang, knows } from './autoTranslate'
declare global { interface Window { api: any } }
const api = window.api
const glob = (g: Record<string, string>, key: string) => Object.entries(g).filter(([k]) => k.includes(key)).map(([k, v]) => [k.split(key)[1].replace(/\.(png|jpg)$/, ''), v] as [string, string])
const cos = import.meta.glob('./assets/cosmetics/*.png', { eager: true, query: '?inline', import: 'default' }) as Record<string, string>
const pos = import.meta.glob('./assets/posters/*.jpg', { eager: true, query: '?url', import: 'default' }) as Record<string, string>
let CONF: any = defConf
// your own HD pictures: put them into src/assets/art/ (hero.jpg, v-1.16.5.jpg, news-wipe.jpg ...) — they win over everything else
const OVR: Record<string, string> = Object.fromEntries(Object.entries({ ...(import.meta.glob('./assets/art/*.jpg', { eager: true, query: '?url', import: 'default' }) as Record<string, string>), ...(import.meta.glob('./assets/art/*.png', { eager: true, query: '?url', import: 'default' }) as Record<string, string>) }).map(([k, v]) => [k.split('/').pop()!.replace(/\.(jpg|png)$/, ''), v]))
/** news without an uploaded picture get official Minecraft key art that fits the topic */
const NEWSART: Record<string, string[]> = { wipe: ['26.1', '1.21.11', '1.21.4'], news: ['1.20.1', '1.20.4'], ender: ['1.19.4', '1.19'], spawners: ['1.21.4', '1.21'], apiary: ['1.15.2', '1.15'], about: ['1.18.2', '1.18'] }
const CAPES = glob(cos, 'cape-'), SKINS = glob(cos, 'skin-'), POSTERS = Object.fromEntries(glob(pos, 'v-'))
const NAV: [string, string][] = [['home', 'home'], ['versions', 'versions'], ['mods', 'mods'], ['visuals', 'sparkles'], ['skins', 'skins'], ['worlds', 'cube'], ['profile', 'user'], ['servers', 'servers'], ['screenshots', 'image'], ['settings', 'settings']]
const SHORT: Record<string, string> = { home: 'Главная', versions: 'Версии', mods: 'Моды', visuals: 'Визуалы', skins: 'Скины', worlds: 'Миры', profile: 'Профиль', servers: 'Серверы', screenshots: 'Скрины', settings: 'Настройки' }
const ACCENTS: Record<string, [string, string]> = { aqua: ['#0ea5e9', '#22d3ee'], blue: ['#2563eb', '#60a5fa'], violet: ['#7c3aed', '#c084fc'], pink: ['#db2777', '#f9a8d4'], rose: ['#e11d48', '#fb7185'],
  red: ['#dc2626', '#f87171'], orange: ['#ea580c', '#fdba74'], amber: ['#d97706', '#fcd34d'], lime: ['#65a30d', '#bef264'], emerald: ['#059669', '#34d399'], teal: ['#0d9488', '#5eead4'], slate: ['#475569', '#cbd5e1'] }
const CATS = ['optimization', 'utility', 'decoration', 'adventure', 'worldgen', 'technology', 'library']
const usePref = (k: string, d: string): [string, (v: string) => void] => { const [v, s] = useState(() => { try { return localStorage.getItem(k) ?? d } catch { return d } }); return [v, (x) => { try { localStorage.setItem(k, x) } catch {} s(x) }] }
let ART: Record<string, string> = {} // official Mojang art by version, filled at start
const cache = new Map<string, string | null>()
function useImg(urls: string[]) { // remote images go through the main process (system proxy/VPN aware, disk-cached); failures are retried
  const key = urls.join('|'), [src, setSrc] = useState<string | null>(cache.get(key) ?? null), [n, setN] = useState(0)
  useEffect(() => { let off = false, tm: any; if (!key) return; if (cache.get(key)) { setSrc(cache.get(key)!); return }
    ;(async () => { for (const u of urls) { const d = await api.img(u).catch(() => null); if (d) { cache.set(key, d); if (!off) setSrc(d); return } }
      if (!off && n < 5) tm = setTimeout(() => setN((x) => x + 1), 4000 * (n + 1)) })()
    return () => { off = true; clearTimeout(tm) } }, [key, n])
  return src
}
/** a player's head: from the skin set in the launcher when there is one (any resolution), otherwise by nick */
const Avatar = ({ name, s = 32, skin }: { name: string; s?: number; skin?: string }) => { const src = useImg(skin ? [] : [`https://mc-heads.net/avatar/${name}/64`, `https://minotar.net/avatar/${name}/64`])
  if (skin) { const bg = (x: number) => ({ position: 'absolute' as const, inset: 0, backgroundImage: `url(${skin})`, backgroundSize: '800% 800%', backgroundPosition: `${x / 56 * 100}% ${8 / 56 * 100}%`, imageRendering: 'pixelated' as const })
    return <span style={{ position: 'relative', display: 'inline-block', flex: 'none', width: s, height: s, borderRadius: s / 4, overflow: 'hidden' }}><span style={bg(8)} /><span style={bg(40)} /></span> }
  return src ? <img src={src} style={{ width: s, height: s, borderRadius: s / 4 }} /> : <div style={{ width: s, height: s, borderRadius: s / 4, background: 'linear-gradient(135deg,var(--a1),var(--a2))' }} className="flex items-center justify-center text-white font-bold">{name[0]?.toUpperCase()}</div> }
const Pic = ({ src, cls, alt = '' }: { src?: string; cls: string; alt?: string }) => { const d = useImg(src ? [src] : []); return d ? <img src={d} className={cls} alt={alt} /> : <div className={cls + ' flex items-center justify-center card'}><Gem s={22} /></div> }
const Spark = ({ data }: { data: number[] }) => { if (data.length < 2) return null; const mx = Math.max(...data, 1), mn = Math.min(...data), pts = data.map((v, i) => `${(i / (data.length - 1)) * 100},${28 - ((v - mn) / Math.max(1, mx - mn)) * 24}`).join(' ')
  return <svg viewBox="0 0 100 30" preserveAspectRatio="none" className="w-full h-10 mt-2"><defs><linearGradient id="spk" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stopColor="var(--a2)" stopOpacity=".45" /><stop offset="1" stopColor="var(--a2)" stopOpacity="0" /></linearGradient></defs>
    <polygon points={`0,30 ${pts} 100,30`} fill="url(#spk)" /><polyline points={pts} fill="none" stroke="var(--a2)" strokeWidth="1.6" vectorEffect="non-scaling-stroke" /></svg> }
function ShotImg({ file, w, cls }: { file: string; w: number; cls: string }) { const [d, setD] = useState<string | null>(null)
  useEffect(() => { let off = false; api.shotThumb(file, w).then((x: string | null) => !off && setD(x)); return () => { off = true } }, [file])
  return d ? <img src={d} className={cls} /> : <div className={cls + ' card'} /> }
const Stat = ({ l, v, blur, icon }: { l: string; v: string; blur?: boolean; icon?: string }) => <div className="kpi"><div className="muted text-xs flex items-center gap-1.5">{icon && <Icon n={icon} s={13} />}{l}</div><div className={'text-2xl font-black mt-1 ' + (blur ? 'blur-md' : '')}>{v}</div></div>
const NEWSIMG: Record<string, string> = Object.fromEntries(Object.entries(import.meta.glob('./assets/news/*.jpg', { eager: true, query: '?url', import: 'default' }) as Record<string, string>).map(([k, v]) => [k.split('/').pop()!.replace('.jpg', ''), v]))
const NewsImg = ({ v, cls }: { v: string; cls: string }) => { if (/^https?:/.test(v)) return <Pic src={v} cls={cls} />; if (OVR['news-' + v]) return <img src={OVR['news-' + v]} className={cls} />
  const a = (NEWSART[v] || []).map((k) => ART[k]).find(Boolean); return a ? <Pic src={a} cls={cls + ' object-cover'} /> : <img src={NEWSIMG[v] || bgServer} className={cls} /> }
const VImg = ({ v, cls }: { v: string; cls: string }) => { if (OVR['v-' + v]) return <img src={OVR['v-' + v]} className={cls + ' object-cover'} />; const u = CONF.versionImages?.[v] || ART[v]; return u ? <Pic src={u} cls={cls + ' object-cover'} /> : <img src={POSTERS[v] || POSTERS['1.16.5']} className={cls + ' object-cover'} /> }
function NumInput({ v, on }: { v: number; on: (n: number) => void }) { // free typing, no spinner arrows
  const [txt, setTxt] = useState(String(v)); useEffect(() => { if (+txt !== v) setTxt(String(v)) }, [v])
  return <input inputMode="numeric" className="w-24 text-center" value={txt} onChange={(e) => { const d = e.target.value.replace(/\D/g, '').slice(0, 5); setTxt(d); if (d) on(+d) }} onBlur={() => { if (!txt || +txt < 320) setTxt(String(v)) }} />
}
/** your own 3D models (Blockbench / resource pack): pet, hat, item on the back, wings — everyone with AsteriaLauncher sees them */
function ModelsCard({ say }: { say: (m: string, k?: any) => void }) {
  const [list, setList] = useState<any>({}), [busy, setBusy] = useState('')
  useEffect(() => { window.api.modelList?.().then((l: any) => setList(l || {})) }, [])
  const slots: [string, string, string][] = [['pet', 'Питомец', 'Ходит за вами'], ['hat', 'На голову', 'Шляпа, корона, ушки'], ['back', 'За спину', 'Коса, меч, молот'], ['wings', 'Крылья', 'На спине']]
  const use = async (slot: string) => { const v = await window.api.visGet(); if (!v) return; v.modules = v.modules || {}
    if (slot === 'pet') await window.api.visPatch('pet', { on: true, s: { kind: 16 } })
    else await window.api.visPatch('cosmetics', { on: true, s: { [slot]: ({ wings: 5, hat: 6, back: 4 } as any)[slot] } }) }
  return <div className="card rounded-3xl p-5 flex flex-col gap-3">
    <div><b className="text-lg">Свои 3D-модели</b><div className="muted text-xs">Файл Blockbench (.bbmodel) или ресурспак с моделью (.zip). Видят все игроки Asteria. Загружайте только свои модели или те, на которые у вас есть права</div></div>
    <div className="grid grid-cols-2 gap-2">{slots.map(([id, title, hint]) => <div key={id} className="rounded-2xl p-3 flex flex-col gap-2" style={{ background: 'var(--chip)' }}>
      <div><b className="text-sm">{title}</b><div className="muted text-[11px]">{list[id] ? list[id].name : hint}</div></div>
      <div className="flex gap-1.5"><button className="btn !py-1.5 text-sm flex-1 flex items-center justify-center gap-1.5" disabled={!!busy} onClick={async () => { setBusy(id); const r = await window.api.modelImport(id); setBusy('')
          if (!r) return; if (r.error) return say(r.error, 'err'); setList({ ...list, [id]: { name: r.name } }); await use(id); say(r.uploaded ? 'Модель «' + r.name + '» надета — её видят игроки Asteria' : 'Модель надета (другие увидят её, когда сервер Asteria обновится)') }}>
        {busy === id ? <span className="dlspin" /> : <Icon n="upload" s={14} />}{list[id] ? 'Заменить' : 'Загрузить'}</button>
        {list[id] && <button className="btn !py-1.5" title="Убрать" onClick={async () => { await window.api.modelRemove(id); const n = { ...list }; delete n[id]; setList(n) }}><Icon n="trash" s={14} /></button>}</div></div>)}</div></div>
}
/** 3D cosmetics on the Skins page: wings, hat, item on the back (pictures are rendered from the same models the game draws) */
const COSM_PIC: Record<string, string> = Object.fromEntries(Object.entries(import.meta.glob('./assets/cosm/*.png', { eager: true, query: '?url', import: 'default' }) as Record<string, string>).map(([k, v]) => [k.split('/').pop()!.replace('.png', ''), v]))
/** a visuals module shown outside the Visuals page: kept in sync with the game and the other pages, saved as a merge of only this module */
/** a soft chime for invites (two notes up) and messages (one short note) */
let chimeCtx: AudioContext | null = null
function chime(short = false) { try { const c = chimeCtx ||= new AudioContext(); const t0 = c.currentTime
  for (const [i, f] of (short ? [880] : [660, 990]).entries()) { const o = c.createOscillator(), g = c.createGain(); o.type = 'sine'; o.frequency.value = f; o.connect(g); g.connect(c.destination)
    const t = t0 + i * 0.16; g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(0.18, t + 0.02); g.gain.exponentialRampToValueAtTime(0.001, t + 0.45); o.start(t); o.stop(t + 0.5) } } catch {} }
function useVisModule(id: string) {
  const [v, setV] = useState<any>(null), own = useRef(0)
  const load = () => window.api.visGet().then((x: any) => { const d = visDefaults() as any; const m = x || d; m.modules = m.modules || {}; m.modules[id] = { ...d.modules[id], ...(m.modules[id] || {}), s: { ...d.modules[id]?.s, ...(m.modules[id]?.s || {}) } }; setV(m) })
  useEffect(() => { load(); return window.api.onFiles?.((f: any) => { if (f.kind === 'vis' && Date.now() - own.current > 1200) load() }) }, [])
  const put = (patch: any) => { own.current = Date.now(); setV((cur: any) => { const o = cur.modules[id]; return { ...cur, modules: { ...cur.modules, [id]: { ...o, ...patch, s: { ...o.s, ...(patch.s || {}) } } } } }); window.api.visPatch(id, patch) }
  return [v, put] as const
}
const COS_ROWS: [string, string, string, [number, string][]][] = [['wings', 'w', 'Крылья', [[1, 'Ангел'], [2, 'Демон'], [3, 'Фея'], [4, 'Дракон'], [6, 'Кристальные'], [7, 'Бабочка'], [8, 'Летучая мышь']]],
    ['hat', 'h', 'Головной убор', [[1, 'Корона'], [2, 'Цилиндр'], [3, 'Кошачьи ушки'], [4, 'Шляпа ведьмы'], [5, 'Рога'], [7, 'Ледяная тиара'], [8, 'Цветочный венок'], [9, 'Тыква']]],
    ['back', 'b', 'За спиной', [[1, 'Коса'], [2, 'Молот'], [3, 'Катана'], [5, 'Небесный клинок'], [6, 'Посох звёзд'], [7, 'Лук ветра'], [8, 'Нефритовое копьё'], [9, 'Метла ведьмы']]]]
const PET_NAMES = ['Котик', 'Призрак', 'Огонёк', 'Кристалл', 'Атом', 'Цветок', 'Пончик', 'Мячик', 'Банан', 'Мини-я', 'Котёнок', 'Лисёнок', 'Зайчик', 'Слайм', 'Пчёлка', 'Аксолотль', 'Своя модель', 'Дракончик', 'Пингвин']
/** the profile showcase: you in 3D with everything you wear (skin, cape, 3D cosmetics, pet), the name plate as above your head in the game, and what is on */
function ProfileLook({ skin, slim, cape, name, sub, children }: { skin: string; slim: boolean; cape?: string; name: string; sub?: React.ReactNode; children?: React.ReactNode }) {
  const [v] = useVisModule('cosmetics'), c = v?.modules?.cosmetics, p = v?.modules?.pet
  const worn: [string, string][] = []
  if (c?.on) for (const [id, pre, title, names] of COS_ROWS) { const i = +c.s?.[id] || 0, n = names.find(([k]) => k === i)?.[1]; if (n) worn.push([title, n]); void pre }
  if (cape && !(c?.on && +c.s?.wings > 0)) worn.push(['Плащ', 'надет'])
  if (p?.on && PET_NAMES[+p.s?.kind]) worn.push(['Питомец', PET_NAMES[+p.s?.kind]])
  return <div className="card rounded-3xl overflow-hidden h-fit relative">
    <div className="absolute inset-x-0 top-0 h-40" style={{ background: 'linear-gradient(135deg,var(--a1),var(--a2))', opacity: .8 }} />
    <div className="absolute inset-x-0 top-24 h-40" style={{ background: 'linear-gradient(180deg,transparent,var(--card))' }} />
    <div className="relative pt-4 flex justify-center"><span className="px-3 py-1 rounded-lg text-sm font-bold text-white flex items-center gap-1.5" style={{ background: 'rgba(0,0,0,.45)' }}><Gem s={14} />{name}</span></div>
    <div className="relative -mt-2"><LookPreview skin={skin} slim={slim} cape={cape} height={380} hint={false} /></div>
    <div className="relative px-5 pb-5 -mt-3 flex flex-col items-center gap-2 text-center">
      <div className="text-2xl font-black">{name}</div>{sub}
      <div className="flex flex-wrap gap-1.5 justify-center">{worn.length ? worn.map(([k, n]) => <span key={k} className="pill !text-xs !py-1"><span className="muted">{k}:</span> {n}</span>) : <span className="muted text-xs">Косметики пока нет — выберите её в «Скинах»</span>}</div>
      <div className="muted text-[11px]">Крутите мышью · так вас видят игроки Asteria</div>
      {children}</div></div>
}
function CosmCard() {
  const [v, put] = useVisModule('cosmetics')
  if (!v || !v.modules.cosmetics) return null
  const c = v.modules.cosmetics
  // [index in the mod, name]: the own model keeps its old index (wings 5, hat 6, back 4), new pieces come after it
  const rows = COS_ROWS
  return <div className="card rounded-3xl p-5 flex flex-col gap-3">
    <div className="flex items-center justify-between"><div><b className="text-lg">Косметика 3D</b><div className="muted text-xs">Детализированные крылья, головные уборы и оружие за спиной. Видят все игроки Asteria</div></div><Switch on={!!c.on} set={(on) => put({ on })} /></div>
    {rows.map(([id, pre, title, names]) => <div key={id}><div className="text-sm mb-1.5 font-semibold">{title}</div>
      <div className="grid gap-1.5" style={{ gridTemplateColumns: 'repeat(8, minmax(0, 1fr))', opacity: c.on ? 1 : 0.55 }}>
        <button onClick={() => put({ s: { [id]: 0 } })} className="rounded-xl flex flex-col items-center justify-center gap-1 p-1.5 muted text-[10.5px]" style={{ background: +c.s[id] === 0 ? 'linear-gradient(135deg,var(--a1),var(--a2))' : 'var(--chip)', color: +c.s[id] === 0 ? '#fff' : undefined, minHeight: 66 }}><Icon n="close" s={16} />Нет</button>
        {names.map(([i, n]) => { const sel = +c.s[id] === i; return <button key={n} title={n} onClick={() => put({ on: true, s: { [id]: i } })} className="rounded-xl flex flex-col items-center gap-1 p-1.5 transition hover:-translate-y-0.5" style={{ background: sel ? 'linear-gradient(135deg,var(--a1),var(--a2))' : 'var(--chip)', color: sel ? '#fff' : undefined }}>
          <img src={COSM_PIC[pre + '-' + i]} className="w-11 h-11 object-contain" draggable={false} /><span className="text-[10.5px] font-semibold truncate w-full text-center">{n}</span></button> })}</div></div>)}
    <div className="flex items-center justify-between gap-3"><Switch on={c.s.tint !== false} set={(x) => put({ s: { tint: x } })} label="Цвет темы визуалов" />{c.s.tint === false && <ColorPick value={c.s.color || '#ffb36b'} set={(x) => put({ s: { color: x } })} size="sm" />}</div>
  </div>
}
/** the pet right on the Skins page: who, size, on / off (same settings as Visuals -> Pet) */
function PetCard({ face }: { face: string }) {
  const [v, put] = useVisModule('pet')
  if (!v) return null
  const p = v.modules.pet
  const names = PET_NAMES, order = [10, 11, 12, 17, 18, 13, 14, 15, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8]
  return <div className="card rounded-3xl p-5 flex flex-col gap-3">
    <div className="flex items-center justify-between"><div><b className="text-lg">Питомец</b><div className="muted text-xs">Бегает рядом с вами в игре. Его видят все игроки Asteria</div></div><Switch on={!!p.on} set={(on) => put({ on })} /></div>
    <div className="grid gap-1.5" style={{ gridTemplateColumns: 'repeat(5, minmax(0, 1fr))', opacity: p.on ? 1 : 0.5 }}>{order.map((i) => { const n = names[i], pic = petPic(i), sel = +p.s.kind === i
      return <button key={n} onClick={() => put({ on: true, s: { kind: i } })} className="rounded-xl flex flex-col items-center gap-1 p-1.5 transition hover:-translate-y-0.5" style={{ background: sel ? 'linear-gradient(135deg,var(--a1),var(--a2))' : 'var(--chip)', color: sel ? '#fff' : undefined }}>
        {pic ? <img src={pic} className="w-10 h-10 object-contain" draggable={false} /> : <span className="w-10 h-10 rounded-lg" style={{ backgroundImage: `url(${face})`, backgroundSize: '800% 800%', backgroundPosition: `${8 / 56 * 100}% ${8 / 56 * 100}%`, imageRendering: 'pixelated' }} />}
        <span className="text-[10.5px] font-semibold truncate w-full text-center">{n}</span></button> })}</div>
    <label className="text-sm"><div className="flex justify-between"><span>Размер</span><b>{(+p.s.size).toFixed(1)}</b></div><input type="range" className="w-full" min={0.5} max={2} step={0.1} value={+p.s.size} onChange={(e) => put({ s: { size: +e.target.value } })} /></label>
    <Switch on={p.s.others !== false} set={(x) => put({ s: { others: x } })} label="Показывать питомцев других игроков Asteria" />
  </div>
}
/** fills the elytra area (x 22..46, y 0..22 of 64x32) with the cape's outside picture when it is empty: elytra then look like the cape */
const withElytra = (url: string): Promise<string> => new Promise((res) => { const im = new Image(); im.onload = () => {
  const k = im.width / 64, c = document.createElement('canvas'); c.width = im.width; c.height = im.height; const x = c.getContext('2d')!; x.drawImage(im, 0, 0)
  const a = x.getImageData(Math.round(24 * k), Math.round(2 * k), Math.round(10 * k), Math.round(20 * k)).data; let filled = 0; for (let i = 3; i < a.length; i += 16) if (a[i] > 10) filled++
  if (filled > a.length / 64) return res(url) // the texture already has elytra
  x.imageSmoothingQuality = 'high'; const src = [1 * k, 1 * k, 10 * k, 16 * k] as const
  x.drawImage(im, ...src, 22 * k, 0, 24 * k, 22 * k); x.drawImage(im, ...src, 24 * k, 2 * k, 10 * k, 20 * k); x.drawImage(im, ...src, 36 * k, 2 * k, 10 * k, 20 * k)
  res(c.toDataURL('image/png')) }; im.onerror = () => res(url); im.src = url })
/** a picture as a data: URL (bigger assets come as file URLs; the launcher saves data: URLs) */
const toData = async (u: string): Promise<string> => u.startsWith('data:') ? u : await fetch(u).then((r) => r.blob()).then((b) => new Promise<string>((res) => { const fr = new FileReader(); fr.onload = () => res(String(fr.result)); fr.readAsDataURL(b) }))
type Saved = { id: string; data: string }
const readList = (k: string): Saved[] => { try { return JSON.parse(localStorage.getItem(k) || '[]') } catch { return [] } }
const writeList = (k: string, l: Saved[]) => { for (let n = Math.min(16, l.length); n >= 0; n--) try { localStorage.setItem(k, JSON.stringify(l.slice(0, n))); return } catch {} } // full: the oldest ones are dropped, never the new one
/** face of a skin (any resolution): base + hat layer */
function SkinTile({ url, on, pick, del, title }: { url: string; on: boolean; pick: () => void; del?: () => void; title?: string }) {
  const bg = (x: number) => ({ backgroundImage: `url(${url})`, backgroundSize: '800% 800%', backgroundPosition: `${x / 56 * 100}% ${8 / 56 * 100}%`, imageRendering: 'pixelated' as const })
  return <div className="relative group"><button onClick={pick} title={title} className={'w-full rounded-xl flex items-center justify-center transition hover:-translate-y-0.5 ' + (on ? 'ring-2 ring-[var(--a1)]' : '')} style={{ height: 84, background: 'var(--chip)' }}>
    <span className="relative rounded-lg overflow-hidden shadow-lg" style={{ width: 52, height: 52 }}><span className="absolute inset-0" style={bg(8)} /><span className="absolute inset-0" style={bg(40)} /></span></button>
    {del && <button className="absolute -top-1.5 -right-1.5 w-6 h-6 rounded-full hidden group-hover:flex items-center justify-center" style={{ background: '#ef4444', color: '#fff' }} title="Удалить" onClick={del}><Icon n="close" s={12} /></button>}</div>
}
function CapeTile({ name, url, on, pick, del }: { name: string; url: string; on: boolean; pick: () => void; del?: () => void }) {
  const [hd, setHd] = useState(false), [frames, setFrames] = useState(1), [fi, setFi] = useState(0)
  useEffect(() => { const im = new Image(); im.onload = () => { setHd(im.width > 64); setFrames(Math.max(1, Math.round(im.height / (im.width / 2)))) }; im.src = url }, [url])
  useEffect(() => { if (frames < 2) return; const id = setInterval(() => setFi((x) => (x + 1) % frames), 90); return () => clearInterval(id) }, [frames])
  const k = 6.4
  return <div className="relative group"><button onClick={pick} title={name} className={'w-full rounded-xl flex items-center justify-center transition hover:-translate-y-0.5 ' + (on ? 'ring-2 ring-[var(--a1)]' : '')} style={{ height: 112, background: 'var(--chip)' }}>
    <span className="rounded-[3px] shadow-lg" style={{ width: 10 * k, height: 16 * k, backgroundImage: `url(${url})`, backgroundSize: `${64 * k}px ${32 * k * frames}px`, backgroundPosition: `${-1 * k}px ${-1 * k - fi * 32 * k}px`, imageRendering: hd ? 'auto' : 'pixelated' }} /></button>
    {del && <button className="absolute -top-1.5 -right-1.5 w-6 h-6 rounded-full hidden group-hover:flex items-center justify-center" style={{ background: '#ef4444', color: '#fff' }} title="Удалить плащ" onClick={del}><Icon n="close" s={12} /></button>}</div>
}
const PageHead = ({ title, sub, right }: { title: string; sub?: string; right?: React.ReactNode }) => <div className="ph"><div><h1 className="grad-text">{title}</h1>{sub && <div className="sub">{sub}</div>}</div>{right}</div>

/** "Now playing" from Windows (Spotify, Yandex Music, browser): cover, track and controls; the same track shows in the game */
/** the search in the title bar: wide, glassy, with hints that change every few seconds */
const HINTS = ['Найти мод, например Sodium', 'Открыть версию 1.20.1', 'Скопировать IP сервера', 'Включить визуалы', 'Сменить скин или плащ', 'Открыть скриншоты']
function SearchBar({ open }: { open: () => void }) {
  const [i, setI] = useState(0)
  useEffect(() => { const id = setInterval(() => setI((x) => (x + 1) % HINTS.length), 2800); return () => clearInterval(id) }, [])
  return <button className="nodrag searchbar" onClick={open} title="Поиск по лаунчеру и быстрые команды">
    <span className="search-ic"><Icon n="search" s={15} /></span>
    <span className="flex-1 min-w-0 text-left relative h-5 overflow-hidden"><span key={i} className="search-hint">{HINTS[i]}</span></span>
    <kbd>Ctrl</kbd><kbd>F</kbd></button>
}

function MiniPlayer() {
  const [m, setM] = useState<any>(null)
  useEffect(() => { api.mediaGet?.().then(setM); return api.onMedia?.(setM) }, [])
  const [, tick] = useState(0); useEffect(() => { const id = setInterval(() => tick((x) => x + 1), 500); return () => clearInterval(id) }, []) // the bar moves between updates
  if (!m?.title) return <div className="mt-auto" /> // after every hook: React needs the same hooks on each render
  const cmd = (c: string) => { api.mediaCmd(c); if (c === 'toggle') setM({ ...m, playing: !m.playing, pos: pos, at: Date.now() }) }
  const pos = Math.min(m.dur || 0, (m.pos || 0) + (m.playing ? (Date.now() - m.at) / 1000 : 0)), f = m.dur > 0 ? pos / m.dur : 0
  const t = (x: number) => Math.floor(x / 60) + ':' + String(Math.floor(x % 60)).padStart(2, '0')
  return <div className="miniplayer mt-auto">
    <div className="flex gap-2.5 items-center">
      {m.cover ? <img src={m.cover} className="mp-cover" /> : <span className="mp-cover flex items-center justify-center" style={{ background: 'linear-gradient(135deg,var(--a1),var(--a2))', color: '#fff' }}><Icon n="music" s={18} /></span>}
      <div className="lbl min-w-0 flex-1"><b className="block truncate text-[12.5px] leading-tight">{m.title}</b><span className="block truncate text-[11px] muted">{m.artist || (m.playing ? 'Играет' : 'Пауза')}</span></div></div>
    <div className="lbl mt-2"><div className="mp-bar"><div style={{ width: f * 100 + '%' }} /></div>
      {m.dur > 0 && <div className="flex justify-between text-[10px] muted mt-1 tabular-nums"><span>{t(pos)} / {t(m.dur)}</span><span>-{t(Math.max(0, m.dur - pos))}</span></div>}
      <div className="flex items-center justify-center gap-1.5 mt-1"><button className="mp-btn" title="Назад" onClick={() => cmd('prev')}><Icon n="prev" s={13} /></button>
        <button className="mp-btn big" title={m.playing ? 'Пауза' : 'Играть'} onClick={() => cmd('toggle')}><Icon n={m.playing ? 'pause' : 'play'} s={14} fill={!m.playing} /></button>
        <button className="mp-btn" title="Дальше" onClick={() => cmd('next')}><Icon n="next" s={13} /></button></div>
      <div className="flex items-center justify-center gap-1 mt-1"><button className="mp-btn" title="Тише" onClick={() => cmd('voldown')}><b className="text-[12px]">−</b></button>
        <button className="mp-btn" title="Без звука / со звуком" onClick={() => cmd('mute')}><Icon n="volume" s={12} /></button>
        <button className="mp-btn" title="Громче" onClick={() => cmd('volup')}><b className="text-[12px]">+</b></button></div></div>
  </div>
}

export default function App() {
  const [tab, setTab] = useState('home'), [menu, setMenu] = useState(''), [addOpen, setAddOpen] = useState(false), [st, setSt] = useState({ online: false, ping: 0, players: 0, max: 500 })
  const [lang, setLang] = usePref('lang', deviceLang()); useEffect(() => { setUiLang(lang) }, [lang]) // every Russian text left on screen follows the language too
  const [theme, setTheme] = usePref('theme', 'dark'), [accent, setAccent] = usePref('accent', 'aqua'), [fx, setFx] = usePref('fx', 'stars'), [custom, setCustom] = usePref('customAccent', '#22d3ee')
  const [look, setLook] = usePref('lookPreview', '1'), [skinSpin, setSkinSpin] = usePref('skinSpin', '1'), [ver, setVer] = usePref('ver', '1.16.5'), [capeId, setCapeS] = useState(''), [badgeId] = usePref('badge', 'crystal')
  const [acc, setAcc] = useState<{ list: any[]; active?: string }>({ list: [] }), [cfg, setCfg] = useState<any>(null), [sys, setSys] = useState({ totalMb: 16384 })
  const [prog, setProg] = useState<any>({ stage: '', pct: 0, detail: '', step: 0 }), [toast, setToast] = useState(''), [skin, setSkin] = useState(''), [variant, setVariant] = usePref('variant', 'classic')
  const [q, setQ] = useState(''), [sort, setSort] = useState('downloads'), [cat, setCat] = useState(''), [hits, setHits] = useState<any[]>([]), [mods, setMods] = useState<any[]>([]), [busy, setBusy] = useState(''), [lq, setLq] = useState(''), [offNick, setOffNick] = useState('')
  const [boot, setBoot] = useState({ pct: 0, msg: '' }), [running, setRunning] = useState(false), [upd, setUpd] = useState<any>(null), [logOpen, setLogOpen] = useState(false), [stats, setStats] = useState<any>({ playMs: 0 })
  const [news, setNews] = useState(''), [kind, setKind] = useState('mod'), [prev, setPrev] = useState<any>(null), [edit, setEdit] = useState(''), [shots, setShots] = useState<any[]>([]), [shotQ, setShotQ] = useState(''), [shotView, setShotView] = useState<any>(null), [storage, setStorage] = useState<any>(null), [hist, setHist] = useState<number[]>([])
  const [customCape, setCustomCape] = usePref('ccape', ''), [conf, setConf] = useState<any>(defConf), [feed, setFeed] = useState<any[]>([]), [profiles, setProfiles] = useState<any>({ list: [], active: null }), [profModal, setProfModal] = useState(false), [pName, setPName] = useState(''), [pVer, setPVer] = useState('1.16.5')
  const [streamer, setStreamer] = usePref('streamer', '0'), [uiscale, setUiscale] = usePref('uiscale', '100'), [, setArtTick] = useState(0), cfgReady = useRef(false), [pal, setPal] = useState(false), [apiSt, setApiSt] = useState<any>(null)
  const [targets, setTargets] = useState<any[]>([]), [tsel, setTsel] = useState<string[]>([]), [tOpen, setTOpen] = useState(false), [srvSt, setSrvSt] = useState<Record<string, any>>({})
  useEffect(() => { (window as any).__setTab = setTab }, []) // lets the UI preview/tests switch tabs
  const t = (k: string) => tr(lang, k), me = acc.list.find((a) => a.id === acc.active), fabricOk = fabricFor(ver)
  CONF = conf
  const tx = (o: any) => o?.[lang] ?? (o?.ru && knows(lang, o.ru) ? o.ru : o?.en) ?? o?.ru ?? '', LINKS: [string, string, string][] = conf.links.map((l: any) => [l.title, l.url, l.icon]), CMDS: string[] = conf.commands.includes('/events') ? conf.commands : [...conf.commands.slice(0, 3), '/events', ...conf.commands.slice(3)], about = tx(conf.about) || t('aboutText')
  const sm = streamer === '1', ip = sm ? '••••••••' : conf.server.ip, nick = (n?: string) => (sm ? '••••••' : n ?? ''), rec = conf.server.recommended || '1.16.5'
  const [mySrv, setMySrv] = useState<any[]>([]), [srvForm, setSrvForm] = useState<any>(null); useEffect(() => { api.myServers?.().then((l: any) => setMySrv(l || [])) }, [])
  const saveMySrv = (l: any[]) => { setMySrv(l); api.myServers?.(l) }
  // partners are built in: they stay in the launcher and in the game list
  const [srvOpen, setSrvOpen] = useState('') // the server card whose description is unfolded
  const PARTNERS = [{ name: 'GrumWorld', ip: 'mc.GrumWorld.space', port: 25565, partner: true, imageLocal: grumImg, iconLocal: grumIcon, tag: 'Гриф · 1.16–26.2', verLabel: '1.16–26.2', // verLabel: what the server accepts; the game starts on the recommended version
    desc: 'Новый уровень грифа: честный геймплей, своя валюта Грумы, кланы, войны и альянсы. Умный игрок здесь сильнее богатого. Сайт: GrumWorld.space · бот: @grumworld_bot',
    links: [['Сайт GrumWorld.space', 'https://grumworld.space', 'globe'], ['Бот в Telegram — @grumworld_bot', 'https://t.me/grumworld_bot', 'telegram']] }]
  const servers: any[] = useMemo(() => [{ name: conf.server.name, ip: conf.server.ip, port: conf.server.port || 25565, version: rec, desc: about, image: '', main: true }, ...PARTNERS,
    ...(conf.servers || []).filter((s: any) => s.ip && s.ip !== conf.server.ip), ...mySrv.map((s) => ({ ...s, mine: true }))].filter((s, i, a) => a.findIndex((x) => x.ip.toLowerCase() === s.ip.toLowerCase()) === i), [conf, about, mySrv])
  useEffect(() => { const a = accent === 'custom' ? [custom, custom] : ACCENTS[accent] ?? ACCENTS.aqua; const r = document.documentElement; r.style.setProperty('--a1', a[0]); r.style.setProperty('--a2', a[1]); r.dataset.theme = theme }, [accent, theme, custom])
  const refresh = () => api.accounts().then(setAcc)
  const instKeyNow = profiles.active?.version === ver ? profiles.active.id : ver
  const [mine, setMine] = useState<any[]>([]), [dropOn, setDropOn] = useState(false)
  const loadMods = () => { if (kind === 'map') return Promise.resolve(setMods([])); if (kind === 'mod') api.modsMine?.(instKeyNow).then(setMine); return (kind === 'mod' ? (fabricOk ? api.modsInstalled(ver) : Promise.resolve([])) : api.pkList(ver, kind)).then(setMods) }
  /** own .jar mods: kept in "My mods" and installed into the chosen builds at once */
  const upload = async (paths: string[] | null) => { const keys = tsel.length ? tsel : [instKeyNow]; const r = await api.modsUpload(paths, keys)
    if (r.added.length) say(`Добавлено: ${r.added.length === 1 ? r.added[0] : r.added.length + ' мод.'}${keys.length > 1 ? ' · сборок: ' + keys.length : ''}`)
    if (r.bad.length) say('Это не мод (.jar): ' + r.bad.join(', '), 'err'); loadMods() }
  const onDrop = (e: React.DragEvent) => { e.preventDefault(); setDropOn(false); if (kind !== 'mod') return
    const paths = [...e.dataTransfer.files].map((f) => api.filePath(f)).filter(Boolean); if (paths.length) upload(paths) }
  const reqId = useRef(0), search = async () => { const id = ++reqId.current; setBusy('s'); const r = kind === 'mod' ? await api.modsSearch({ q, ver, sort, cat, pvp }) : await api.pkSearch({ q, ver, sort, kind, pvp }); if (id !== reqId.current) return; setHits(r); setBusy('') }
  useEffect(() => { (async () => { const st1 = (pct: number, k: string) => setBoot({ pct, msg: tr(localStorage.getItem('lang') ?? 'ru', k) })
    st1(10, 'boot1'); api.onStatus((d: any) => { setSt(d); setHist((h) => [...h.slice(-29), d.players]) }); api.onProgress(setProg); api.onGame(setRunning); api.onUpdate(setUpd); api.onFriendNotes?.((l: any[]) => { for (const x of l) { if (x.k === 'invite') { chime(); setInvite(x); continue } if (x.k === 'msg') { chime(true); if (!(window as any).__chatWith || (window as any).__chatWith.toLowerCase() !== String(x.who).toLowerCase()) say(x.who + ': ' + x.text, 'info'); continue }
      say(x.k === 'request' ? x.who + ' хочет добавить вас в друзья — «Миры» → «Друзья»' : x.k === 'accepted' ? x.who + ' принял(а) вашу заявку в друзья' : x.who + ' отклонил(а) заявку в друзья', x.k === 'declined' ? 'err' : 'info') } }); addEventListener('focus', () => api.checkUpdate?.()); api.onApi?.(setApiSt)
    setCfg(await api.getSettings()); api.config().then((c: any) => c && setConf({ ...defConf, ...c })); api.tgFeed('safimine_grief').then(setFeed); api.versionArt().then((a: any) => { ART = a || {}; setArtTick((x) => x + 1) }).catch(() => {}); api.profList().then(setProfiles); api.apiInfo?.().then(setApiSt)
    st1(40, 'accounts'); await refresh(); st1(65, 'boot2'); setSys(await api.sysInfo()); setStats(await api.stats())
    st1(85, 'boot3'); await new Promise((r) => setTimeout(r, 600)); st1(100, 'boot4') })() }, [])
  useEffect(() => { api.stats().then(setStats) }, [running])
  // "Download" installs the version completely (Java, libraries, mods, visuals), then "Play"; "Update" when newer mods came with the launcher
  const [vst, setVst] = useState<any>({ installed: true }), [dling, setDling] = useState(false)
  const [miniSkin, setMiniSkin] = useState<string | null>(null), [capeSrc, setCapeSrc] = useState(''), [gifSrc, setGifSrc] = useState('')
  // each account has its own skin, cape and mini-me skin
  const setCape = (v: string) => { setCapeS(v); try { localStorage.setItem('cape:' + (acc.active || ''), v) } catch {} }
  useEffect(() => { if (!acc.active) return; (async () => { await api.accTextures?.()
    try { setCapeS(localStorage.getItem('cape:' + acc.active) ?? '') } catch {}
    setSkin((await api.skinCurrent?.().catch(() => null)) || ''); setMiniSkin((await api.miniGet?.().catch(() => null)) || null) })() }, [acc.active])
  const refreshVst = () => api.verStatus?.(ver).then(setVst).catch(() => {})
  const [alts, setAlts] = useState<string[]>([]); useEffect(() => { api.alts?.().then((l: string[]) => setAlts(l || [])); return api.onAlts?.((l: string[]) => setAlts(l || [])) }, [])
  const [invite, setInvite] = useState<any>(null)
  const [shotAsk, setShotAsk] = useState(false), [worldsView, setWorldsView] = useState('mine')
  const [verAsk, setVerAsk] = useState<'' | 'reinstall' | 'remove'>('')
  const verDo = async (a: string, withData = false) => { setVerAsk(''); const r = await api.verRemove(ver, withData); if (r?.error) return say(r.error, 'err'); await refreshVst(); if (a === 'reinstall') download(); else say('Minecraft ' + ver + ' удалён' + (withData ? ' вместе с мирами и модами (в корзине)' : '')) }
  useEffect(() => { refreshVst() }, [ver, running])
  // newer mods / visuals for an installed version are fetched quietly — nothing to press
  useEffect(() => { if (vst.installed && vst.update && !running && !dling && !vst.busy) download(true) }, [vst.installed, vst.update, running])
  const download = async (quiet = false) => { if (dling) return; setDling(true); const r = await api.install(ver).catch((e: any) => ({ error: String(e?.message || e) })); setDling(false); await refreshVst()
    r?.error ? (!quiet && say(r.error, 'err')) : say(quiet ? 'Моды и визуалы для ' + ver + ' обновлены' : 'Minecraft ' + ver + ' скачан — можно играть') }
  // a Minecraft from this launcher may still be open (launcher restarted / updated): show it as running, and notice when it closes
  useEffect(() => { const f = () => api.gameState().then((s: any) => setRunning(!!s?.running)).catch(() => {}); f(); const id = setInterval(f, 10000); return () => clearInterval(id) }, [])
  useEffect(() => { loadMods() }, [ver, tab, kind, profiles.active?.id]); useEffect(() => { if (tab !== 'mods' || kind === 'map' || !(fabricOk || kind !== 'mod')) return; const id = setTimeout(search, 180); return () => clearTimeout(id) }, [tab, ver, sort, cat, kind, q])
  useEffect(() => { if (tab === 'mods') api.modsTargets().then(setTargets) }, [tab, profiles])
  useEffect(() => { setTsel([instKeyNow]) }, [instKeyNow])
  useEffect(() => { const f = () => api.zoom(Math.max(0.72, Math.min(1, outerWidth / 1440, outerHeight / 880)) * (+uiscale / 100)); f(); addEventListener('resize', f); return () => removeEventListener('resize', f) }, [uiscale])
  useEffect(() => { if (!cfg) return; if (!cfgReady.current) { cfgReady.current = true; return } const id = setTimeout(() => { api.saveSettings(cfg) }, 500); return () => clearTimeout(id) }, [cfg])
  useEffect(() => api.onFiles?.((d: any) => { if (d.kind === 'shots') api.shots().then(setShots) }), []) // F2 in the game: the picture shows up at once
  // what is on screen follows the folders: screenshots, mods, packs changed by the game or by hand show up at once
  useEffect(() => api.onFiles?.((d: any) => { if (d.kind === 'shots' && tab === 'screenshots') api.shots().then(setShots); if ((d.kind === 'mods' || d.kind === 'packs') && tab === 'mods') loadMods() }), [tab, kind, ver])
  useEffect(() => { if (tab !== 'worlds') setWorldsView('mine') }, [tab])
  useEffect(() => { if (tab === 'screenshots') api.shots().then(setShots); if (tab === 'settings') { api.storage().then(setStorage); api.apiInfo?.(true).then(setApiSt) } }, [tab])
  useEffect(() => { if (tab !== 'servers') return; for (const s of servers) api.srvStatus(s.ip, s.port).then((r: any) => setSrvSt((o) => ({ ...o, [s.ip]: r }))) }, [tab, servers])
  useEffect(() => { if (cfg && !cfg.ramSet && sys.totalMb) { const c = { ...cfg, ramMb: ramOpt }; setCfg(c); api.saveSettings(c) } }, [!!cfg, cfg?.ramSet, sys.totalMb])
  useEffect(() => { api.setCosmetics({ cape: capeId, badge: badgeId }) }, [capeId, badgeId])
  useEffect(() => { if (cfg && cfg.streamer !== sm) setCfg({ ...cfg, streamer: sm }) }, [sm, !!cfg])
  const [toastKind, setToastKind] = useState<'ok' | 'err' | 'info'>('ok')
  /** toast: 'ok' = done, 'err' = something went wrong (stays longer), 'info' = a neutral notice */
  const say = (m: string, kind: 'ok' | 'err' | 'info' = 'ok') => { setToast(m); setToastKind(kind); clearTimeout((say as any).t); (say as any).t = setTimeout(() => setToast(''), kind === 'err' ? 7000 : 4500) }
  // screenshots: arrows (and ← →) flip through them
  const shotList = shots.filter((x) => x.name.toLowerCase().includes(shotQ.toLowerCase()))
  const shotStep = (d: number) => { if (!shotView || !shotList.length) return; const i = shotList.findIndex((x) => x.file === shotView.file); setShotView(shotList[(i + d + shotList.length) % shotList.length]) }
  useEffect(() => { if (!shotView) return; const f = (e: KeyboardEvent) => { if (e.key === 'Escape') setShotView(null); if (e.key === 'ArrowRight') shotStep(1); if (e.key === 'ArrowLeft') shotStep(-1) }; addEventListener('keydown', f); return () => removeEventListener('keydown', f) }, [shotView, shots, shotQ])
  const copy = (x: string) => { navigator.clipboard.writeText(x); say(t('copied') + (sm ? '' : ': ' + x)) }
  // favourite mods / resource packs / shaders (the AL mark on a card)
  const [pvp, setPvp] = useState(false), [prevInfo, setPrevInfo] = useState<any>(null)
  useEffect(() => { search() }, [pvp])
  // preview of any mod / pack / shader: pictures + description translated into the launcher language
  const openPrev = async (h: any) => { setPrev(h); setPrevInfo(null); setPrevInfo(await api.mrProject?.(h.slug, lang).catch(() => null) || { desc: h.description, text: '' }) }
  const [favs, setFavsS] = useState<any[]>(() => { try { return JSON.parse(localStorage.getItem('favs') || '[]') } catch { return [] } }), [favOnly, setFavOnly] = useState(false)
  const setFavs = (l: any[]) => { setFavsS(l); try { localStorage.setItem('favs', JSON.stringify(l)) } catch {} }
  const isFav = (slug: string) => favs.some((f) => f.slug === slug && f.kind === kind)
  const toggleFav = (h: any) => isFav(h.slug) ? setFavs(favs.filter((f) => !(f.slug === h.slug && f.kind === kind))) : setFavs([{ kind, slug: h.slug, title: h.title, description: h.description, icon_url: h.icon_url, downloads: h.downloads, gallery: h.gallery, featured_gallery: h.featured_gallery }, ...favs])
  // sharing with a friend
  const [friend, setFriend] = useState<{ open: boolean; code: string; busy: boolean; got?: any }>({ open: false, code: '', busy: false })
  const share = async (k: string, data: any) => { const r = await api.shareMake(k, data); r?.id ? say(r.long ? 'Код скопирован (длинный: сервер Asteria сейчас недоступен) — отправьте его другу целиком' : 'Код ' + r.id + ' скопирован — отправьте его другу') : say(r?.error || 'Не удалось', 'err') }
  const sharePack = async (part: 'all' | 'mods' | 'packs' | 'shaders' = 'all') => { const p = await api.packExport(ver); const q = { ver: p.ver, mods: part === 'all' || part === 'mods' ? p.mods : [], packs: part === 'all' || part === 'packs' ? p.packs : [], shaders: part === 'all' || part === 'shaders' ? p.shaders : [] }
    if (!q.mods.length && !q.packs.length && !q.shaders.length) return say('Здесь пока нечего отправить: нет модов, ресурспаков или шейдеров из каталога', 'info'); share('modpack', q) }
  const applyFriend = async (got: any) => { const d = got.data
    if (got.kind === 'skin') { addSkin(d); await useSkin(d) }
    else if (got.kind === 'cape') await addCape(d)
    else if (got.kind === 'crosshair') { const v = await api.visGet(); if (v) { v.modules = v.modules || {}; v.modules.crosshair = { ...(v.modules.crosshair || {}), ...d, on: true }; await api.visSet(v); say('Прицел друга установлен') } }
    else if (got.kind === 'modpack') { let ok = 0, n = 0; say('Ставим сборку друга на ' + ver + '…', 'info')
      for (const s of d.mods || []) { n++; const r = await api.modsInstall(s, ver).catch(() => null); if (r && !r.error) ok++ }
      for (const [list, k] of [[d.packs, 'resourcepack'], [d.shaders, 'shader']] as [string[], string][]) for (const s of list || []) { n++; const r = await api.pkInstall(s, ver, k).catch(() => null); if (r?.ok) ok++ }
      say(`Сборка установлена: ${ok} из ${n}` + (d.ver !== ver ? ` (сделана для ${d.ver})` : '')); loadMods() }
    setFriend({ open: false, code: '', busy: false }) }
  const pickCape = async (n: string, u: string) => { setCape(n); api.saveTexture({ data: await withElytra(await toData(u)), kind: 'cape' }) }
  // your own capes and skins: kept in a list, any of them can be put on again or removed
  const [myCapes, setMyCapesS] = useState<Saved[]>(() => { const l = readList('myCapes'); try { const old = localStorage.getItem('ccape'); if (old && !l.length) { l.push({ id: 'custom', data: old }); writeList('myCapes', l) } } catch {} return l })
  const [mySkins, setMySkinsS] = useState<Saved[]>(() => readList('mySkins')), [presetSkin, setPresetSkin] = usePref('presetSkin', '')
  const myCapesUrl = (id: string) => myCapes.find((c) => c.id === id)?.data
  const setMyCapes = (l: Saved[]) => { setMyCapesS(l); writeList('myCapes', l) }, setMySkins = (l: Saved[]) => { setMySkinsS(l); writeList('mySkins', l) }
  const addCape = async (data: string) => { const it = { id: 'my' + Date.now().toString(36), data }; setMyCapes([it, ...myCapes]); setCape(it.id); const r = await api.saveTexture({ data: await withElytra(data), kind: 'cape' }); say(r?.uploaded ? t('uploaded') : t('savedLocal')) }
  const addSkin = (data: string) => { setPresetSkin(''); if (!mySkins.some((x) => x.data === data)) setMySkins([{ id: 'my' + Date.now().toString(36), data }, ...mySkins]) }
  const useSkin = async (data: string) => { setSkin(data); const r = await api.saveTexture({ data, kind: 'skin', slim: variant === 'slim' }); say(r?.uploaded ? t('uploaded') : t('savedLocal')) }
  const play = async (v: string, join = false, server?: any, extra?: { world?: string; together?: boolean }) => { if (!me) { setAddOpen(true); return say('Сначала добавьте аккаунт', 'info') } let r: any; try { r = await api.launch({ version: v, join, lang, server, ...(extra || {}) }) } catch (e: any) { r = { error: 'Не удалось запустить: ' + (e?.message || e) } } if (r?.warn) say(r.warn, 'info'); if (r?.error) say(r.error, 'err') }
  const install = async (slug: string) => { setBusy(slug)
    if (kind === 'mod') { const list = targets.filter((x) => tsel.includes(x.key)); const tg = list.length ? list : [{ key: instKeyNow, version: ver }]
      const r = await api.modsInstallTo(slug, tg); const ok = r.filter((x: any) => x.ok).length; say(ok === r.length ? `Установлено: ${slug}${r.length > 1 ? ' · сборок: ' + ok : ''}` : `Установлено в ${ok} из ${r.length} — для части версий мода нет`) }
    else { const r = await api.pkInstall(slug, ver, kind); r.ok ? say(slug) : say(r.error, 'err') }
    setBusy(''); loadMods() }
  const visArt = useImg(OVR.visuals ? [] : [ART['1.21.4'], ART['1.20.1'], ART['1.16.5']].filter(Boolean) as string[]), visBg = OVR.visuals || visArt || hudBg
  const skinImg = useImg(skin ? [] : [`https://mc-heads.net/skin/${me?.name ?? 'Steve'}`, `https://minotar.net/skin/${me?.name ?? 'Steve'}`])
  const skinUrl = skin || skinImg || defSkin, capeUrl = myCapesUrl(capeId) || (capeId === 'custom' && customCape ? customCape : CAPES.find(([n]) => n === capeId)?.[1])
  const fps = mods.some((m) => m.slug === 'sodium' && m.enabled) // the optimization mods really are in this version's folder
  const ramMax = Math.max(2048, Math.round(sys.totalMb / 512) * 512), ramOpt = sys.totalMb <= 4096 ? 1536 : sys.totalMb <= 8192 ? 3072 : sys.totalMb <= 16384 ? 4096 : 6144
  const needDl = !running && !vst.installed, mainBtn = () => (running ? api.killGame() : dling ? undefined : needDl ? download() : play(ver)), mainLabel = running ? t('closeGame') : dling ? (vst.installed ? 'Обновление…' : 'Скачивание…') : needDl ? 'Скачать' : t('play'), hrs = (stats.playMs / 3600000).toFixed(1)
  const verOpts = ALL_VERSIONS.map((v) => ({ value: v, label: 'Minecraft ' + v, hint: VINFO[v]?.name, icon: <LoaderIcon k={ASTERIA[v] ? 'asteria' : 'vanilla'} s={16} /> }))
  const shop = () => api.openLink(conf.links.find((l: any) => l.icon === 'shop')?.url)
  const cmds: Cmd[] = [...NAV.map(([k, i]) => ({ id: 'go-' + k, label: 'Открыть: ' + SHORT[k], icon: i, run: () => setTab(k) })),
    { id: 'play', label: running ? 'Закрыть игру' : needDl ? mainLabel + ' ' + ver : 'Играть ' + ver, icon: needDl ? 'download' : 'play', hint: 'Enter', run: mainBtn }, { id: 'join', label: 'Зайти на ' + conf.server.name, icon: 'servers', run: () => { setVer(rec); play(rec, true) } },
    ...ALL_VERSIONS.map((v) => ({ id: 'v-' + v, label: 'Версия ' + v, hint: VINFO[v]?.name, icon: 'versions', run: () => { setVer(v); setTab('versions') } })),
    { id: 'theme', label: theme === 'dark' ? 'Светлая тема' : 'Тёмная тема', icon: theme === 'dark' ? 'sun' : 'moon', run: () => setTheme(theme === 'dark' ? 'light' : 'dark') },
    { id: 'stream', label: sm ? 'Выключить режим стримера' : 'Режим стримера', icon: 'eye', run: () => setStreamer(sm ? '0' : '1') }, { id: 'log', label: t('journal'), icon: 'rules', run: () => setLogOpen(true) },
    { id: 'folder', label: t('folder'), icon: 'folder', run: () => api.openGameDir() }, { id: 'acc', label: t('addAccount'), icon: 'user', run: () => setAddOpen(true) }, { id: 'ip', label: t('copyIP'), icon: 'copy', run: () => copy(conf.server.ip) }]
  const profilesBar = <div className="flex gap-2 flex-wrap items-center justify-end"><span className="muted text-sm">{t('profilesT')}</span>
    <button className={'pill ' + (!profiles.active ? 'on' : '')} onClick={async () => { await api.profSet(null); setProfiles(await api.profList()) }}>Обычный</button>
    {profiles.list.map((p: any) => <span key={p.id} className="inline-flex items-center"><button className={'pill ' + (profiles.active?.id === p.id ? 'on' : '')} onClick={async () => { await api.profSet(p.id); setVer(p.version); setProfiles(await api.profList()) }}>{p.name} · {p.version}</button>
      <button className="muted px-1 hover:text-[var(--text)]" onClick={async () => { await api.profDel(p.id); setProfiles(await api.profList()) }}><Icon n="close" s={12} /></button></span>)}
    <button className="pill" onClick={() => { setPVer(ver); setProfModal(true) }}><Icon n="plus" s={14} />{t('newProfile')}</button></div>

  return (
    <div className="h-screen flex flex-col relative">
      <div className="aurora"><i /><i /><i /></div><div className="grain" />
      {invite && <div className="fixed right-5 bottom-5 z-[95] card rounded-3xl p-4 w-[340px] shadow-2xl" style={{ borderColor: 'var(--a1)' }}>
        <div className="flex items-center gap-3"><span className="cmd-ic !w-11 !h-11 shrink-0" style={{ color: '#4ade80' }}><Icon n="user" s={20} /></span>
          <div className="min-w-0"><b className="block">Приглашение от {invite.who}</b><div className="muted text-xs truncate">{invite.where?.kind === 'world' ? 'В свой мир' : 'На сервер ' + (invite.where?.label || invite.where?.host)}{invite.where?.ver ? ' · Minecraft ' + invite.where.ver : ''}</div></div></div>
        <div className="flex gap-2 mt-3"><button className="btn btn-primary flex-1 flex items-center justify-center gap-2" disabled={running} onClick={() => { const w = invite.where; setInvite(null); play(w.ver || ver, true, { host: w.host, port: w.port || 25565 }) }}><Icon n="play" s={15} />{running ? 'Игра уже запущена' : 'Зайти'}</button>
          <button className="btn" onClick={() => setInvite(null)}>Позже</button></div></div>}
      <Fx kind={fx} dark={theme === 'dark'} /> {/* stays on while the game runs: a hidden / minimised window pauses the animation by itself */}
      {menu && <div className="fixed inset-0 z-20" onClick={() => setMenu('')} />}
      {/* ---------- title bar */}
      <div className="drag h-12 flex items-center justify-between px-4 relative z-30 gap-3">
        <span className="w-[180px] muted text-xs font-semibold tracking-wide hidden xl:block">{conf.server.name} · {st.online ? st.players + ' онлайн' : t('maint')}</span>
        <SearchBar open={() => setPal(true)} />
        <div className="nodrag flex items-center gap-2">
          <div className="relative"><button className="topchip" onClick={() => setMenu(menu === 'lang' ? '' : 'lang')}><img src={FLAGS[lang]} className="w-5 h-3.5 rounded-sm" />{lang.toUpperCase()}</button>
            {menu === 'lang' && <div className="pop z-50" style={{ width: 260 }}><div className="relative mb-2"><input className="w-full !pl-9" autoFocus placeholder={t('searchLang')} value={lq} onChange={(e) => setLq(e.target.value)} /><span className="absolute left-3 top-2.5 muted"><Icon n="search" s={16} /></span></div>
              <div style={{ maxHeight: 280, overflow: 'auto' }}>{LANGS.filter(([k, n]) => (k + n).toLowerCase().includes(lq.toLowerCase())).map(([k, n]) => <button key={k} className={'row ' + (lang === k ? 'sel' : '')} onClick={() => { setLang(k); setMenu('') }}>
                <img src={FLAGS[k]} className="w-7 h-5 rounded-sm shadow" /><span className="flex-1">{n}</span>{lang === k && <Icon n="check" s={16} />}</button>)}</div></div>}</div>
          <div className="relative"><button className="topchip" title={t('color')} onClick={() => setMenu(menu === 'color' ? '' : 'color')}><span className="w-4 h-4 rounded-full" style={{ background: 'linear-gradient(135deg,var(--a1),var(--a2))' }} /></button>
            {menu === 'color' && <div className="pop z-50" style={{ width: 250 }}><div className="muted text-xs mb-2">{t('color')}</div><div className="grid grid-cols-6 gap-2 mb-3">{Object.entries(ACCENTS).map(([k, [a, b]]) => <button key={k} onClick={() => setAccent(k)} className="w-8 h-8 rounded-full transition hover:scale-110" style={{ background: `linear-gradient(135deg,${a},${b})`, boxShadow: accent === k ? '0 0 0 2px var(--pop-bg),0 0 0 4px var(--text)' : 'none' }} />)}</div>
              <div className="flex items-center justify-between pt-2" style={{ borderTop: '1px solid var(--line)' }}><span className="text-sm">Свой цвет</span><ColorPick value={custom} set={(v) => { setCustom(v); setAccent('custom') }} size="sm" /></div></div>}</div>
          <button className={'topchip ' + (sm ? '!bg-[var(--a1)] !text-white' : '')} title={t('streamer')} onClick={() => { setStreamer(sm ? '0' : '1'); say(sm ? 'Режим стримера выключен' : 'Режим стримера: ник и IP скрыты') }}><Icon n="eye" s={16} /></button>
          <button className="topchip" onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}><Icon n={theme === 'dark' ? 'sun' : 'moon'} s={16} /></button>
          <div className="relative"><button className="topchip !pl-1" onClick={() => setMenu(menu === 'acct' ? '' : 'acct')}>{me ? <Avatar name={sm ? 'Steve' : me.name} s={26} skin={sm ? undefined : skin || undefined} /> : <Icon n="user" s={18} />}{me ? nick(me.name) : t('noAcc')}<Icon n="chevron" s={14} /></button>
            {menu === 'acct' && <div className="pop z-50" style={{ width: 280 }}>{acc.list.map((a) => <div key={a.id} className={'row ' + (a.id === acc.active ? 'sel' : '')}><button className="flex items-center gap-2 flex-1 text-left" onClick={async () => { await api.selectAccount(a.id); refresh(); setMenu('') }}>
              <Avatar name={sm ? 'Steve' : a.name} s={30} skin={!sm && a.id === acc.active ? skin || undefined : undefined} /><span className="flex-1">{nick(a.name)}<span className="muted text-xs block">{a.type === 'microsoft' ? 'Microsoft' : 'Offline'}</span></span>{a.id === acc.active && <Icon n="check" s={16} />}</button>
              <button title={t('logout')} className="iconbtn" onClick={async () => { await api.removeAccount(a.id); refresh() }}><Icon n="trash" s={15} /></button></div>)}
              <button className="row" onClick={() => { setAddOpen(true); setMenu('') }}><Icon n="plus" s={18} />{t('addAccount')}</button>
              <button className="row" onClick={() => { setTab('skins'); setMenu('') }}><Icon n="skins" s={18} />{t('changeSkin')}</button></div>}</div>
          <div className="w-px h-6 mx-1" style={{ background: 'var(--line)' }} />
          {[['min', 'min'], ['max', 'max'], ['close', 'close']].map(([a, i]) => <button key={a} onClick={() => api.win(a)} className={'iconbtn ' + (a === 'close' ? 'hover:!bg-rose-500 hover:!text-white' : '')}><Icon n={i} s={15} /></button>)}
        </div>
      </div>
      <div className="flex flex-1 gap-4 p-4 pt-1 min-h-0 relative z-10 shell">
        {/* ---------- navigation rail */}
        <nav className="side glass">
          <div className="brand"><Wordmark tag big /></div>
          <div className="flex flex-col gap-1">{NAV.map(([k, i]) => <button key={k} onClick={() => setTab(k)} title={t(k)} className={'sitem ' + (tab === k ? 'on' : '')}>
            <Icon n={i} s={19} /><span className="lbl">{SHORT[k]}</span></button>)}</div>
          <MiniPlayer />
          <button className="usercard" onClick={() => me ? setTab('profile') : setAddOpen(true)}>
            {me ? <Avatar name={sm ? 'Steve' : me.name} s={36} skin={sm ? undefined : skin || undefined} /> : <span className="w-9 h-9 rounded-xl flex items-center justify-center" style={{ background: 'var(--chip)' }}><Icon n="user" s={18} /></span>}
            <span className="lbl min-w-0 flex-1 text-left"><b className="block truncate text-sm">{me ? nick(me.name) : t('noAcc')}</b>
              <span className="block text-[11px] leading-snug muted">{me ? (me.type === 'microsoft' ? 'Microsoft' : 'Offline') : 'Нажмите, чтобы добавить'}</span></span></button>
        </nav>
        <main key={tab} className="flex-1 min-w-0 glass rounded-[26px] p-7 overflow-auto fade">

          {/* =============================== HOME */}
          {tab === 'home' && <div className="grid gap-5" style={{ gridTemplateColumns: 'minmax(0,1fr) 300px' }}>
            <div className="min-w-0">
              <div className="hero mb-6">{OVR.hero ? <img src={OVR.hero} className="bg" /> : ART[rec] ? <Pic src={ART[rec]} cls="bg object-cover" /> : <img src={hero} className="bg" />}
                <div className="heroText relative h-full min-h-[330px] flex flex-col justify-end p-8 text-white">
                  <div className="flex gap-2 mb-3"><span className="badge ast"><span className={'glow-dot ' + (st.online ? '' : 'off')} />{st.online ? `${st.players} игроков онлайн` : t('maint')}</span>{ver === rec && <span className="badge rec"><Gem s={11} glow={false} />{t('recommended')}</span>}</div>
                  <div className="text-lg font-bold text-white/85">Добро пожаловать на</div>
                  <h1 className="text-6xl font-black tracking-tight leading-none" style={{ textShadow: '0 6px 40px rgba(0,0,0,.45)' }}>{conf.server.name.slice(0, Math.ceil(conf.server.name.length / 2))}<span style={{ color: 'var(--a2)' }}>{conf.server.name.slice(Math.ceil(conf.server.name.length / 2))}</span></h1>
                  <p className="max-w-xl text-white/85 text-sm mt-3 mb-6 leading-relaxed">{about}</p>
                  <div className="flex gap-3 items-center flex-wrap">
                    <button className={'playbtn ' + (running ? 'stop' : '')} disabled={dling} onClick={mainBtn}><Icon n={running ? 'close' : needDl ? 'download' : 'play'} s={20} fill={!running && !needDl} />{mainLabel}</button>
                    <Dropdown value={ver} set={setVer} options={verOpts} w={260} className="!bg-white/15 !border-white/20 !text-white !h-[50px] !rounded-2xl backdrop-blur" />
                    <button className="btn !bg-white/15 !text-white !border-white/20 !h-[50px] !rounded-2xl backdrop-blur flex items-center gap-2" onClick={() => { setVer(rec); play(rec, true) }}><Icon n="servers" s={16} />{t('join')}</button>
                  </div></div>
                <div className="herocards">{conf.news.slice(0, 2).map((n: any) => <button key={n.id} className="hcard" onClick={() => setNews(n.id)}><NewsImg v={n.image} cls="w-11 h-11 rounded-xl object-cover shrink-0" />
                  <span className="min-w-0 flex-1 text-left"><b className="block text-[13px] truncate" style={{ color: 'var(--a2)' }}>{tx(n.title)}</b><span className="block text-[11px] text-white/70 truncate">{tx(n.text)}</span></span><Icon n="right" s={14} /></button>)}</div></div>
              <EventsBlock events={conf.events || []} intro={tx(conf.eventsIntro)} />
              <div className="section">{t('news')}</div>
              <div className="grid grid-cols-3 gap-4 stagger">{conf.news.map((n: any) => <Tilt key={n.id} className="newscard cursor-pointer" onClick={() => setNews(n.id)}><NewsImg v={n.image} cls="" />
                <div className="t flex justify-between items-end gap-2"><span className="leading-tight">{tx(n.title)}</span><span className="w-8 h-8 shrink-0 rounded-full bg-white text-black flex items-center justify-center"><Icon n="arrow" s={14} /></span></div></Tilt>)}</div>
              {feed.length > 0 && <><div className="section">Telegram</div><div className="grid grid-cols-2 gap-3 items-start">{feed.slice(0, 4).map((f: any) => <button key={f.link} className="tgpost" onClick={() => api.openLink(f.link)}>
                {f.img && <Pic src={f.img} cls="tgpost-img" />}
                <div className="p-4"><div className="flex items-center gap-2 mb-2"><span className="tgpost-ava"><Icon n="telegram" s={14} /></span><b className="text-sm">SafiMine</b><span className="muted text-xs">{f.date ? new Date(f.date).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' }) : ''}</span><span className="ml-auto muted"><Icon n="link" s={14} /></span></div>
                  <div className="tgpost-text">{f.text}</div></div></button>)}</div></>}
              <div className="section">{t('links')}</div>
              <div className="linkgrid">{LINKS.map(([n, u, i]) => <button key={u} className="pill justify-center" title={n} onClick={() => api.openLink(u)}><Icon n={i} s={14} /><span className="lbl">{n}</span></button>)}</div>
            </div>
            <aside className="flex flex-col gap-4 stagger">
              <div className="card rounded-3xl p-5"><div className="flex items-center gap-3 mb-3"><img src={avatar} className="w-12 h-12 rounded-2xl shadow-lg" /><div className="min-w-0"><div className="font-black text-lg leading-tight">{conf.server.name}</div><div className="muted text-xs truncate">{ip}</div></div>
                <button className="iconbtn ml-auto" title={t('copyIP')} onClick={() => copy(conf.server.ip)}><Icon n="copy" s={15} /></button></div>
                <div className="flex items-baseline gap-2"><span className="text-3xl font-black">{st.online ? st.players : '—'}</span><span className="muted text-sm">/ {st.max} · {st.online ? st.ping + ' ms' : t('maint')}</span></div>
                <div className="h-2 rounded-full mt-3 overflow-hidden" style={{ background: 'var(--chip)' }}><div className="h-2 rounded-full transition-all duration-700" style={{ width: Math.min(100, st.players / st.max * 100) + '%', background: 'linear-gradient(90deg,var(--a1),var(--a2))' }} /></div>
                <Spark data={hist} />
                <button className="btn btn-primary w-full mt-3 flex items-center justify-center gap-2" onClick={() => { setVer(rec); play(rec, true) }}><Icon n="play" s={14} fill />{t('join')}</button></div>
              <div className="card rounded-3xl p-5"><div className="font-bold mb-3 flex items-center gap-2"><Icon n="keyboard" s={16} />{t('commands')}</div><div className="flex gap-1.5 flex-wrap">{CMDS.map((c) => <button key={c} className="pill" title={t('cmdHint')} onClick={() => copy(c)}>{c}</button>)}</div></div>
              <div className="grid grid-cols-2 gap-3"><Stat l={t('playtime')} v={hrs + ' ч'} icon="play" /><Stat l={t('launches')} v={String(stats.launches ?? 0)} icon="sparkles" /></div>
              <button className="btn flex items-center justify-center gap-2" onClick={() => setLogOpen(true)}><Icon n="rules" s={15} />{t('journal')}</button>
            </aside>
          </div>}

          {/* =============================== VERSIONS */}
          {tab === 'versions' && <Versions manage={(a) => setVerAsk(a)} ver={ver} setVer={setVer} cfg={cfg} setCfg={setCfg} running={running} play={mainBtn} label={needDl || dling ? mainLabel + ' ' + ver : undefined} VImg={VImg} profiles={profilesBar} recommended={rec} />}

          {/* =============================== MODS */}
          {tab === 'mods' && <div onDragOver={(e) => { if (kind === 'mod' && fabricOk && e.dataTransfer.types.includes('Files')) { e.preventDefault(); setDropOn(true) } }} onDragLeave={(e) => { if (e.currentTarget === e.target) setDropOn(false) }} onDrop={onDrop} className="relative">
            {dropOn && <div className="dropzone-full"><Icon n="upload" s={34} /><b className="text-lg mt-2">Отпустите, чтобы добавить моды</b><span className="text-sm opacity-80">Они попадут в «Мои моды» и в выбранные сборки</span></div>}
            <PageHead title={kind === 'map' ? 'Карты' : t(kind === 'mod' ? 'mods' : kind === 'shader' ? 'shaders' : 'packs')} sub={`Для Minecraft ${ver}${profiles.active?.version === ver ? ' · профиль «' + profiles.active.name + '»' : ''}`}
              right={<div className="flex gap-2 items-center"><Dropdown value={ver} set={setVer} options={verOpts} w={260} /><div className="seg">{[['mod', t('mods')], ['resourcepack', t('packs')], ['shader', t('shaders')]].map(([k, n]) => <button key={k} className={kind === k ? 'on' : ''} onClick={() => { setKind(k); setHits([]); setMods([]) }}>{n}</button>)}<button className={kind === 'map' ? 'on' : ''} onClick={() => { setKind('map'); setHits([]); setMods([]) }}>Карты</button></div></div>} />
            {kind === 'shader' && <p className="muted text-sm mb-3">{t('shadersHint')}</p>}
            {kind === 'map' ? <MapsCatalog ver={ver} say={say} openWorlds={() => { setWorldsView('mine'); setTab('worlds') }} /> : kind === 'mod' && !fabricOk ? <div className="hero !min-h-[260px]"><VImg v={ver} cls="bg" /><div className="relative p-8 text-white min-h-[260px] flex flex-col justify-end">
              <h2 className="text-3xl font-black">Моды для {ver} пока недоступны</h2><p className="text-white/80 text-sm mt-2 mb-5 max-w-xl">Для этой версии лаунчер запускает чистую игру. Моды из каталога, FPS-буст и Asteria работают на версиях с Fabric — переключитесь в один клик:</p>
              <div className="flex gap-2 flex-wrap">{['1.16.5', '1.20.1', '1.21.4'].map((v) => <button key={v} className="btn !bg-white/15 !text-white !border-white/25 flex items-center gap-2" onClick={() => setVer(v)}><LoaderIcon k="asteria" s={14} />Перейти на {v}{v === rec && <span className="badge rec !py-0.5">SafiMine</span>}</button>)}
                <button className="btn !bg-transparent !text-white !border-white/25" onClick={() => setKind('resourcepack')}>Ресурспаки для {ver}</button></div></div></div> :
            <div className="grid gap-5" style={{ gridTemplateColumns: 'minmax(0,1fr) 320px' }}><div className="min-w-0">
              <div className="flex gap-2 mb-3"><div className="relative flex-1"><input className="w-full !pl-10" placeholder={t(kind === 'mod' ? 'searchPh' : kind === 'shader' ? 'searchShader' : 'searchPack')} value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && search()} /><span className="absolute left-3 top-2.5 muted"><Icon n="search" s={18} /></span></div>
                <Dropdown value={sort} set={setSort} w={170} options={[{ value: 'downloads', label: t('popular') }, { value: 'newest', label: t('newest') }, { value: 'updated', label: t('updated') }]} />
                {kind === 'mod' && <div className="relative"><button className="dd !min-w-0" onClick={() => setTOpen(!tOpen)} title="В какие сборки ставить мод"><Icon n="folder" s={15} />{tsel.length <= 1 ? 'В текущую сборку' : 'Сборок: ' + tsel.length}<Icon n="chevron" s={14} /></button>
                  {tOpen && <><div className="fixed inset-0 z-40" onClick={() => setTOpen(false)} /><div className="pop z-50" style={{ width: 300 }}><div className="muted text-xs px-2 pb-2">Куда устанавливать моды</div>
                    {[{ key: instKeyNow, version: ver, name: profiles.active?.version === ver ? profiles.active.name : 'Minecraft ' + ver, profile: false }, ...targets.filter((x) => x.key !== instKeyNow)].map((x) =>
                      <div key={x.key} className="target"><Check on={tsel.includes(x.key)} set={(v) => setTsel(v ? [...tsel, x.key] : tsel.filter((k) => k !== x.key))} label={<span><b>{x.name}</b><span className="muted text-xs block">{x.profile ? 'Профиль · ' : ''}Minecraft {x.version}{x.key === instKeyNow ? ' · сейчас выбрана' : ''}</span></span>} /></div>)}
                    <p className="muted text-[11px] px-2 pt-1">Для каждой сборки скачается версия мода под её Minecraft.</p></div></>}</div>}</div>
              {kind === 'mod' && <div className="flex gap-1.5 flex-wrap mb-4"><button className={'pill ' + (!cat ? 'on' : '')} onClick={() => setCat('')}>{t('allCats')}</button>{CATS.map((c) => <button key={c} className={'pill ' + (cat === c ? 'on' : '')} onClick={() => setCat(c)}>{t('cat_' + c)}</button>)}</div>}
              <div className="flex gap-1.5 mb-3 flex-wrap"><button className={'pill ' + (pvp ? 'on' : '')} onClick={() => setPvp(!pvp)} title="Для PVP: удобные ресурспаки, моды и шейдеры"><Icon n="swords" s={13} />PVP</button><button className={'pill ' + (favOnly ? 'on' : '')} onClick={() => setFavOnly(!favOnly)}><Icon n="star" s={13} />Избранное {favs.filter((f) => f.kind === kind).length || ''}</button>
                <button className="pill" onClick={() => sharePack(kind === 'mod' ? 'mods' : kind === 'shader' ? 'shaders' : 'packs')}><Icon n="share" s={13} />{kind === 'mod' ? 'Поделиться модами' : kind === 'shader' ? 'Поделиться шейдерами' : 'Поделиться ресурспаками'}</button>
                <button className="pill" onClick={() => sharePack('all')}><Icon n="share" s={13} />Вся сборка</button><button className="pill" onClick={() => setFriend({ open: true, code: '', busy: false })}><Icon n="link" s={13} />Код от друга</button></div>
              <div className="flex flex-col gap-2">{(favOnly ? favs.filter((f) => f.kind === kind) : hits).map((h) => { const have = mods.some((m) => m.slug === h.slug); return <div key={h.slug} className="mcard"><button onClick={() => openPrev(h)} className="shrink-0" title="Подробнее"><Pic src={kind !== 'mod' && (h.featured_gallery || h.gallery?.[0]) ? (h.featured_gallery || h.gallery[0]) : h.icon_url} cls={kind === 'mod' ? 'w-14 h-14 rounded-xl object-cover' : 'w-28 h-16 rounded-xl object-cover'} /></button>
                <div className="min-w-0 flex-1"><div className="font-bold truncate">{h.title}</div><div className="muted text-xs line-clamp-2">{h.description}</div><div className="muted text-xs mt-0.5 flex items-center gap-1"><Icon n="download" s={11} />{h.downloads?.toLocaleString()}</div></div>
                <button className="iconbtn shrink-0" title={isFav(h.slug) ? 'Убрать из избранного' : 'В избранное'} onClick={() => toggleFav(h)} style={isFav(h.slug) ? { color: 'var(--a1)' } : { opacity: 0.45 }}><Icon n="star" s={18} /></button>
                <button disabled={have && tsel.length <= 1} className={'btn flex items-center gap-1.5 ' + (have && tsel.length <= 1 ? '' : 'btn-primary')} onClick={() => install(h.slug)}>{have && tsel.length <= 1 ? <><Icon n="check" s={16} />{t('installedBadge')}</> : busy === h.slug ? <span className="flex items-center gap-2"><span className="dlspin" />Скачиваем</span> : <><Icon n="download" s={16} />{t('install')}</>}</button></div> })}</div></div>
              <div><div className="flex items-center justify-between mb-2"><h3 className="text-lg font-bold">{t(kind === 'mod' ? 'installed' : kind === 'shader' ? 'installedShaders' : 'installedPacks')} <span className="muted text-sm">{mods.length}</span></h3>{kind === 'mod' && <button className="pill" title={t('verifyHint')} onClick={async () => { const r = await api.modsVerify(ver, true); say(`${t('verifyDone')}: ${r.total} · ${t('updated2')} ${r.updated} · ${t('fixed')} ${r.fixed}`); loadMods() }}><Icon n="shield" s={14} />{t('verify')}</button>}</div>
                {kind === 'mod' && (fps ? <div className="pill mb-3" style={{ background: 'rgba(16,185,129,.18)' }}><Icon n="check" s={14} />{t('fps')}</div>
                  : <div className="mcard mb-3 !items-start" style={{ borderColor: 'rgba(245,158,11,.45)', background: 'rgba(245,158,11,.10)' }}><span className="cmd-ic !w-9 !h-9 shrink-0" style={{ color: '#f59e0b' }}><Icon n="close" s={16} /></span><div className="min-w-0"><b className="text-sm">FPS-бусты неактивны</b><div className="muted text-xs">{cfg?.optimize === false ? 'Оптимизация выключена в настройках лаунчера — включите её, и моды добавятся при следующем запуске.' : 'Установите эту версию игры (кнопка «Скачать» или «Играть») — моды-оптимизаторы Sodium, Lithium и другие добавятся сами.'}</div></div></div>)}
                {ASTERIA[ver] && kind === 'mod' && <div className="mcard mb-2" style={{ borderColor: 'color-mix(in srgb,var(--a2) 45%,transparent)' }}><span className="cmd-ic !w-10 !h-10"><LoaderIcon k="asteria" s={20} /></span><div className="min-w-0 flex-1"><div className="font-semibold text-sm">Asteria</div><div className="muted text-[11px]">Знак AL в табе, скины и плащи{ASTERIA[ver] === 'full' ? ' и визуалы' : ''} · ставится сам, бесплатно</div></div></div>}
                {kind === 'mod' && <div className="card rounded-2xl p-3 mb-3">
                  <div className="flex items-center justify-between mb-2"><b className="text-sm">Мои моды <span className="muted">{mine.length}</span></b><button className="pill !py-1" title="Открыть папку «Мои моды»" onClick={() => api.modsOpenMine()}><Icon n="folder" s={13} /></button></div>
                  <button className="dropzone w-full" onClick={() => upload(null)}><Icon n="upload" s={18} /><span><b className="block text-sm">Загрузить свой мод</b><span className="muted text-[11px]">Нажмите или перетащите .jar в окно</span></span></button>
                  {mine.length > 0 && <div className="flex flex-col gap-1.5 mt-2">{mine.map((m) => <div key={m.file} className="flex items-center gap-2 rounded-xl p-1.5" style={{ background: 'var(--chip)' }}>
                    {m.icon ? <img src={m.icon} className="w-8 h-8 rounded-lg object-cover" style={{ imageRendering: 'pixelated' }} /> : <span className="cmd-ic !w-8 !h-8"><Icon n="cube" s={15} /></span>}
                    <div className="min-w-0 flex-1"><div className="text-[13px] font-semibold truncate">{m.title}</div><div className="muted text-[10px] truncate">{[m.loader, m.version, m.mc && 'MC ' + m.mc].filter(Boolean).join(' · ')}</div></div>
                    {m.installed ? <span className="muted text-[11px] flex items-center gap-1 pr-1"><Icon n="check" s={12} />стоит</span> : <button className="pill !py-1 on" onClick={async () => { await api.modsMineInstall(m.file, tsel.length ? tsel : [instKeyNow]); say('Установлено: ' + m.title); loadMods() }}><Icon n="download" s={12} />Поставить</button>}
                    <button className="iconbtn !w-7 !h-7" title="Убрать из «Моих модов»" onClick={async () => { await api.modsMineRemove(m.file); loadMods() }}><Icon n="trash" s={13} /></button></div>)}</div>}
                </div>}
                <div className="flex flex-col gap-2">{mods.filter((m) => !q || (m.title + ' ' + m.file).toLowerCase().includes(q.toLowerCase())).map((m) => <div key={m.file} className="mcard !p-2.5"><Pic src={m.icon} cls="w-10 h-10 rounded-lg object-cover" />
                  <div className="min-w-0 flex-1"><div className="font-semibold text-sm truncate">{m.title}{m.own && <span className="badge ml-1.5 !text-[9px] !py-0.5" style={{ background: "var(--chip)", color: "var(--a2)" }}>свой</span>}</div><div className="muted text-[11px] line-clamp-1">{m.desc}</div></div>
                  {kind === 'mod' && <Switch on={m.enabled} set={async () => { await api.modsToggle(m.file, ver); loadMods() }} />}
                  <button className="iconbtn" onClick={async () => { await (kind === 'mod' ? api.modsRemove(m.file, ver) : api.pkRemove(m.file, ver, kind)); loadMods() }}><Icon n="trash" s={15} /></button></div>)}</div></div></div>}
          </div>}

          {/* =============================== SKINS */}
          {tab === 'skins' && <div><PageHead title={t('skins')} sub="Ваш скин и плащ в игре через мод Asteria — на любом сервере и в одиночной игре" />
            <div className="card rounded-2xl p-3 mb-4 flex items-center gap-3 text-sm"><span className="cmd-ic !w-9 !h-9 shrink-0"><Icon n="eye" s={17} /></span>
              <div><b>Скин и плащ отсюда видят только игроки с AsteriaLauncher.</b> <span className="muted">Остальные видят ваш обычный скин на сервере.{me?.type === 'microsoft' ? ' Чтобы скин видели все — нажмите «' + t('apply') + '».' : ''}</span></div></div>
            <div className="grid gap-5" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1.15fr)' }}>
            <div className="card rounded-3xl p-4 relative"><div className="seg absolute right-4 top-4 z-10"><button className={look ? '' : 'on'} onClick={() => setLook('')}>Скин</button><button className={look ? 'on' : ''} onClick={() => setLook('1')}>С косметикой</button></div>
              {look ? <LookPreview skin={skinUrl} slim={variant === 'slim'} height={470} /> : <Skin3D skin={skinUrl} slim={variant === 'slim'} cape={capeUrl} height={470} spin={skinSpin === '1'} />}
              <div className="mt-2"><Check on={skinSpin === '1'} set={(v) => setSkinSpin(v ? '1' : '0')} label="Вращать модель" /></div>
              <div className="flex gap-2 items-center flex-wrap mt-3"><button className="btn btn-primary flex items-center gap-2" onClick={() => setEdit('skin')}><Icon n="pencil" s={16} />Редактировать на модели</button>
                <button className="btn flex items-center gap-2" onClick={async () => { const d = await api.pickSkin(); if (d) { setSkin(d); addSkin(d); const r = await api.saveTexture({ data: d, kind: 'skin', slim: variant === 'slim' }); say(r?.uploaded ? t('uploaded') : t('savedLocal')) } }}><Icon n="upload" s={16} />{t('pickPng')}</button>
                <div className="seg"><button className={variant === 'classic' ? 'on' : ''} onClick={() => setVariant('classic')}>Classic</button><button className={variant === 'slim' ? 'on' : ''} onClick={() => setVariant('slim')}>{t('slim')}</button></div>
                {me?.type === 'microsoft' && <button className="btn" title="Скин на всех серверах (Mojang)" onClick={async () => { const r = await api.applySkin(variant); r.error ? say(r.error, 'err') : say(t('applied')) }}>{t('apply')}</button>}</div>
              <div className="flex gap-2 mt-3 flex-wrap"><button className="pill" onClick={() => share('skin', skinUrl.startsWith('data:') ? skinUrl : null)} disabled={!skinUrl.startsWith('data:')}><Icon n="share" s={13} />Поделиться скином</button>
                <button className="pill" disabled={!capeUrl} onClick={async () => capeUrl && share('cape', await toData(capeUrl))}><Icon n="share" s={13} />Поделиться плащом</button>
                <button className="pill" onClick={() => setFriend({ open: true, code: '', busy: false })}><Icon n="link" s={13} />Код от друга</button></div>
              <p className="muted text-xs mt-2">{t('drag')}</p>
              <div className="mt-4"><div className="flex items-center justify-between mb-2"><b className="text-sm">Скины</b><span className="muted text-xs">Наведите на свой скин — появится кнопка удаления</span></div>
                <div className="grid grid-cols-6 gap-2">{mySkins.map((x) => <SkinTile key={x.id} url={x.data} on={skin === x.data} title="Мой скин" pick={() => useSkin(x.data)} del={() => { setMySkins(mySkins.filter((y) => y.id !== x.id)); say('Скин удалён из списка') }} />)}
                  {SKINS.map(([n, u]) => <SkinTile key={n} url={u} on={presetSkin === n && !mySkins.some((x) => x.data === skin)} title={'HD-скин ' + n} pick={async () => { setPresetSkin(n); useSkin(await toData(u)) }} />)}</div></div></div>
            <div className="flex flex-col gap-4">
              <PetCard face={miniSkin || skinUrl} />
              <CosmCard />
              <ModelsCard say={say} />
              <div className="card rounded-3xl p-5 flex gap-4 items-center"><div className="w-16 h-16 rounded-2xl shrink-0 overflow-hidden flex items-center justify-center" style={{ background: 'var(--chip)' }}>{miniSkin ? <div className="w-full h-full" style={{ backgroundImage: `url(${miniSkin})`, backgroundSize: '512px 512px', backgroundPosition: '-64px -64px', imageRendering: 'pixelated' }} /> : <Icon n="user" s={26} />}</div>
                <div className="flex-1 min-w-0"><b>Питомец «Мини-я»</b><div className="muted text-sm">Маленькая копия вас бегает следом. Включите в Визуалы → Питомец → «Мини-я»; скин — ваш или отдельный отсюда. Его видят все игроки Asteria</div>
                  <div className="flex gap-2 mt-2 flex-wrap"><button className="btn !py-1.5 text-sm flex items-center gap-2" onClick={async () => { const d = await api.pickSkin(); if (d) { setMiniSkin(d); const r = await api.saveTexture({ data: d, kind: 'mini' }); say(r?.uploaded ? 'Скин «Мини-я» сохранён' : 'Скин сохранён на этом компьютере', r?.uploaded ? 'ok' : 'info') } }}><Icon n="upload" s={15} />Свой скин для «Мини-я»</button>
                    {miniSkin && <button className="btn !py-1.5 text-sm" onClick={async () => { setMiniSkin(null); await api.saveTexture({ data: null, kind: 'mini' }); say('«Мини-я» снова в вашем скине') }}>Как мой скин</button>}</div></div></div>
              <div className="card rounded-3xl p-5"><div className="flex items-center justify-between gap-2 mb-1"><h3 className="text-lg font-bold">{t('capes')}</h3>
                <button className="btn !py-1.5 flex items-center gap-1.5 text-sm" onClick={() => setEdit('cape')}><Icon n="pencil" s={14} />Создать свой</button></div><p className="muted text-xs mb-3">{t('cosm')}</p>
                <div className="grid grid-cols-5 gap-2.5"><button className={'rounded-xl flex flex-col items-center justify-center gap-1 muted text-xs ' + (!capeId ? 'ring-2 ring-[var(--a1)]' : '')} style={{ height: 112, background: 'var(--chip)' }} onClick={() => { setCape(''); api.saveTexture({ data: null, kind: 'cape' }) }}><Icon n="close" s={18} />Без плаща</button>
                  {myCapes.map((c) => <CapeTile key={c.id} name="Мой плащ" url={c.data} on={capeId === c.id} pick={() => pickCape(c.id, c.data)} del={() => { setMyCapes(myCapes.filter((x) => x.id !== c.id)); if (capeId === c.id) { setCape(''); api.saveTexture({ data: null, kind: 'cape' }) } say('Плащ удалён') }} />)}
                  {CAPES.map(([n, u]) => <CapeTile key={n} name={n} url={u} on={capeId === n} pick={() => pickCape(n, u)} />)}</div>
                <div className="flex gap-2 mt-3 flex-wrap"><button className="btn flex items-center gap-2 text-sm" onClick={async () => { const d = await api.pickImage(); if (!d) return; if (d.startsWith('data:image/gif')) return setGifSrc(d); const im = new Image(); im.onload = () => Math.abs(im.width / im.height - 2) < 0.02 ? addCape(d) : setCapeSrc(d); im.src = d }}><Icon n="upload" s={15} />Картинка или GIF для плаща</button>
                  <button className="btn flex items-center gap-2 text-sm" onClick={() => setEdit('cape')}><Icon n="image" s={15} />Плащ из своей картинки</button></div></div>
              <div className="card rounded-3xl p-5 flex gap-4 items-center"><div className="w-14 h-14 rounded-2xl flex items-center justify-center shrink-0" style={{ background: 'linear-gradient(135deg,#0b2a3c,#0e7490)' }}><Gem s={30} /></div>
                <div><b>Значок Asteria</b><div className="muted text-sm">Знак AL перед ником в табе и над головой. Его видите вы и все игроки с AsteriaLauncher.</div></div></div>
            </div></div></div>}

          {/* =============================== PROFILE */}
          {tab === 'profile' && <div><PageHead title={t('profile')} sub="Аккаунты и ваша статистика в лаунчере" />
            <div className="grid gap-5" style={{ gridTemplateColumns: '360px minmax(0,1fr)' }}>
            <ProfileLook skin={skinUrl} slim={variant === 'slim'} cape={capeUrl} name={me ? nick(me.name) : t('noAcc')} sub={me && <span className="badge soft">{me.type === 'microsoft' ? 'Лицензия Microsoft' : 'Offline-аккаунт'}</span>}>
              <div className="flex gap-2 flex-wrap justify-center mt-1"><button className="btn btn-primary !py-2 flex items-center gap-2" onClick={() => setAddOpen(true)}><Icon n="plus" s={15} />{t('addAccount')}</button>
                <button className="btn !py-2 flex items-center gap-2" onClick={() => setTab('skins')}><Icon n="skins" s={15} />{t('changeSkin')}</button></div></ProfileLook>
            <div className="flex flex-col gap-4">
              <div className="card rounded-3xl p-5"><div className="font-bold mb-3">Аккаунты</div><div className="flex flex-col gap-2">{acc.list.map((a) => <div key={a.id} className={'mcard !p-2.5 ' + (a.id === acc.active ? 'ring-2 ring-[var(--a2)]' : '')}>
                <Avatar name={sm ? 'Steve' : a.name} s={36} skin={!sm && a.id === acc.active ? skin || undefined : undefined} /><div className="flex-1 min-w-0"><b className="block truncate">{nick(a.name)}</b><span className="muted text-xs">{a.type === 'microsoft' ? 'Microsoft' : 'Offline'}</span></div>
                {a.id !== acc.active && <button className="btn !py-1.5 text-sm" onClick={async () => { await api.selectAccount(a.id); refresh() }}>Выбрать</button>}
                {a.id !== acc.active && (alts.includes(a.id) ? <button className="pill !text-xs" title="Закрыть это окно игры" onClick={() => api.altStop(a.id)}><Icon n="close" s={11} />Окно</button>
                  : <button className="pill !text-xs" title={'Открыть ещё одно окно Minecraft ' + ver + ' под этим аккаунтом и зайти на SafiMine'} onClick={async () => { const r = await api.altLaunch({ accountId: a.id, version: ver, join: true }); if (r?.error) say(r.error, 'err'); else say(nick(a.name) + ': второе окно запускается') }}><Icon n="play" s={11} />Ещё окно</button>)}
                <button className="iconbtn" title={t('logout')} onClick={async () => { await api.removeAccount(a.id); refresh() }}><Icon n="trash" s={15} /></button></div>)}
                {!acc.list.length && <p className="muted text-sm">Аккаунтов пока нет — добавьте offline-ник или войдите через Microsoft.</p>}</div></div>
              <div className="card rounded-3xl p-5"><div className="font-bold mb-3">{t('localStats')}</div><div className="grid grid-cols-3 gap-3 mb-4"><Stat l={t('playtime')} v={hrs + ' ' + t('hours')} /><Stat l={t('launches')} v={String(stats.launches ?? 0)} /><Stat l={t('mods')} v={String(mods.length)} /></div>
                {Object.entries(stats.perVer ?? {}).map(([v, ms]: any) => <div key={v} className="flex items-center gap-3 mb-2 text-sm"><span className="w-16">{v}</span><div className="flex-1 h-2 rounded-full" style={{ background: 'var(--chip)' }}><div className="h-2 rounded-full" style={{ width: Math.min(100, ms / Math.max(1, stats.playMs) * 100) + '%', background: 'linear-gradient(90deg,var(--a1),var(--a2))' }} /></div><span className="muted w-14 text-right">{(ms / 3600000).toFixed(1)} {t('hours')}</span></div>)}</div></div></div></div>}

          {/* =============================== SCREENSHOTS */}
          {tab === 'screenshots' && <div><PageHead title={t('screenshots')} sub={shots.length + ' скриншотов из всех версий'} right={<div className="flex items-center gap-2">{shots.length > 0 && <button className="pill" onClick={() => setShotAsk(true)}><Icon n="trash" s={13} />Удалить все</button>}<div className="relative w-72"><input className="w-full !pl-10" placeholder={t('search')} value={shotQ} onChange={(e) => setShotQ(e.target.value)} /><span className="absolute left-3 top-2.5 muted"><Icon n="search" s={18} /></span></div></div>} />
            {shots.length === 0 ? <p className="muted">{t('noShots')}</p> : <div className="grid grid-cols-3 gap-4 stagger">{shots.filter((x) => x.name.toLowerCase().includes(shotQ.toLowerCase())).slice(0, 60).map((x) => <Tilt key={x.file} className="card rounded-2xl overflow-hidden cursor-pointer" onClick={() => setShotView(x)} max={4}><ShotImg file={x.file} w={420} cls="w-full h-36 object-cover" />
              <div className="p-2.5"><div className="text-sm font-semibold truncate">{x.name}</div><div className="muted text-xs">Minecraft {x.version} · {new Date(x.mtime).toLocaleString()}</div></div></Tilt>)}</div>}</div>}

          {/* =============================== SERVERS */}

          {tab === 'servers' && <div><PageHead title={t('servers')} sub="Статус, адрес и быстрый вход. Все серверы отсюда сами появляются в списке серверов Minecraft" right={<button className="btn-add" onClick={() => setSrvForm({ name: '', ip: '', port: '', desc: '' })}><span className="ic"><Icon n="plus" s={14} /></span>Добавить сервер</button>} />
            {srvForm && <div className="ask-bg" onClick={() => setSrvForm(null)}><div className="ask flex flex-col gap-2" onClick={(e) => e.stopPropagation()}><b className="text-lg">Свой сервер</b>
              <input placeholder="Название" value={srvForm.name} onChange={(e) => setSrvForm({ ...srvForm, name: e.target.value })} />
              <div className="flex gap-2"><input className="flex-1" placeholder="Адрес: play.server.ru" value={srvForm.ip} onChange={(e) => setSrvForm({ ...srvForm, ip: e.target.value.trim() })} /><input className="w-24" placeholder="Порт" value={srvForm.port} onChange={(e) => setSrvForm({ ...srvForm, port: e.target.value.replace(/\D/g, '') })} /></div>
              <textarea rows={3} placeholder="Описание (необязательно)" value={srvForm.desc} onChange={(e) => setSrvForm({ ...srvForm, desc: e.target.value })} />
              <div className="flex gap-2 justify-end mt-2"><button className="btn" onClick={() => setSrvForm(null)}>Отмена</button><button className="btn-add" disabled={!/^[a-z0-9.\-]{3,80}$/i.test(srvForm.ip)} onClick={() => { const ip = srvForm.ip.replace(/:\d+$/, ''), port = +srvForm.port || +(srvForm.ip.match(/:(\d+)$/) || [])[1] || 25565
                saveMySrv([...mySrv.filter((x) => x.ip !== ip), { name: srvForm.name || ip, ip, port, desc: srvForm.desc }]); setSrvForm(null); say('Сервер добавлен — он появится и в Minecraft') }}><span className="ic"><Icon n="plus" s={13} /></span>Добавить</button></div></div></div>}
            <div className="grid grid-cols-2 gap-5 stagger">{servers.map((s) => { const r = s.main ? { ...st, online: st.online } : srvSt[s.ip]; return <Tilt key={s.ip} className="card rounded-3xl overflow-hidden" max={3}>
              <div className="relative h-44">{s.image ? <Pic src={s.image} cls="w-full h-full object-cover" /> : <img src={s.imageLocal || (s.main ? hero : bgServer)} className="w-full h-full object-cover" />}<div className="absolute inset-0 bg-gradient-to-t from-black/80 to-transparent" />
                <div className="absolute bottom-3 left-5 right-5 text-white flex items-end justify-between gap-3"><div className="flex items-center gap-3">{r?.icon ? <img src={r.icon} className="w-12 h-12 rounded-xl" /> : s.iconLocal ? <img src={s.iconLocal} className="w-12 h-12 rounded-xl" /> : s.main ? <img src={avatar} className="w-12 h-12 rounded-xl" /> : null}<div>{s.partner && <span className="badge ast mb-1 inline-flex"><Icon n="star" s={11} />Сотрудничество</span>}<div className="text-2xl font-black">{s.name}</div><div className="text-sm opacity-85 flex items-center gap-1.5">{sm ? '••••••' : s.ip}{s.port && +s.port !== 25565 ? ':' + s.port : ''} · <Icon n="star" s={12} fill />{s.verLabel || s.version || rec}</div></div></div>
                  <span className="badge ast"><span className={'glow-dot ' + (r?.online ? '' : 'off')} />{r?.online ? `${r.players}/${r.max}` : r ? 'офлайн' : '…'}</span></div></div>
              <div className="p-5">{(() => { const open = srvOpen === s.ip, links: [string, string, string][] = s.links || (s.main ? LINKS : []); return <button className="block w-full text-left mb-4 group" title={open ? 'Свернуть' : 'Подробнее'} onClick={() => setSrvOpen(open ? '' : s.ip)}>
                <p className={'text-sm muted ' + (open ? '' : 'line-clamp-3')}>{tx(s.desc) || s.desc}</p>
                {open && links.length > 0 && <div className="flex flex-wrap gap-1.5 mt-3" onClick={(e) => e.stopPropagation()}>{links.map(([n, u, i]) => <span key={u} role="link" className="pill !text-xs cursor-pointer" onClick={() => api.openLink(u)}><Icon n={i || 'link'} s={12} />{n}</span>)}</div>}
                <span className="text-xs font-semibold mt-1.5 inline-flex items-center gap-1" style={{ color: 'var(--a1)' }}>{open ? 'Свернуть' : 'Подробнее'}<span style={{ display: 'inline-flex', transform: open ? 'rotate(180deg)' : 'none', transition: 'transform .2s' }}><Icon n="chevron" s={12} /></span></span></button> })()}<div className="flex gap-2"><button className="btn btn-primary flex items-center gap-2" onClick={() => { const v = s.version || rec; setVer(v); play(v, true, { host: s.ip, port: s.port || 25565 }) }}><Icon n="play" s={14} fill />{s.main ? t('join') : 'Зайти на ' + s.name}</button><button className="btn flex items-center gap-2" onClick={() => copy(s.ip + (s.port && +s.port !== 25565 ? ':' + s.port : ''))}><Icon n="copy" s={14} />{t('copyIP')}</button>
                {s.mine && <button className="btn ml-auto" title="Убрать из лаунчера" onClick={() => saveMySrv(mySrv.filter((x) => x.ip !== s.ip))}><Icon n="trash" s={14} /></button>}</div></div></Tilt> })}</div>
            <button className="card rounded-3xl p-4 mt-5 w-full flex items-center gap-3 text-left" onClick={() => api.openLink('https://t.me/filonis_music')}><Icon n="telegram" s={20} /><div className="flex-1"><b>Хотите добавить свой сервер в лаунчер?</b><div className="muted text-sm">Напишите в Telegram — @filonis_music</div></div><Icon n="right" s={16} /></button></div>}

          {/* =============================== SETTINGS */}
          {tab === 'settings' && cfg && <div><PageHead title={t('settings')} sub="Всё сохраняется автоматически" />
            <GameSettings ver={ver} versions={ALL_VERSIONS} setVer={setVer} say={say} />
            <div className="section">Лаунчер</div>
            <div className="grid grid-cols-2 gap-5">
            <BackupCard say={say} />
            <UninstallCard />
            <LegalCard />
            <div className="card rounded-3xl p-5 flex flex-col gap-3"><div className="font-bold flex items-center gap-2"><Icon n="cpu" s={18} />{t('ram')}</div><div className="muted text-sm">{t('ramHint')}</div>
              <div className="flex items-end justify-between"><div className="text-4xl font-black" style={{ color: 'var(--a2)' }}>{(cfg.ramMb / 1024).toFixed(1)} GB</div><button className="pill" onClick={() => setCfg({ ...cfg, ramMb: ramOpt, ramSet: true })}>{t('optimal')}: {(ramOpt / 1024).toFixed(1)} GB</button></div>
              <input type="range" min={1024} max={ramMax} step={512} value={Math.min(cfg.ramMb, ramMax)} onChange={(e) => setCfg({ ...cfg, ramMb: +e.target.value, ramSet: true })} className="w-full" /><div className="flex justify-between muted text-xs"><span>1 GB</span><span>{(ramMax / 1024).toFixed(0)} GB</span></div></div>
            <div className="card rounded-3xl p-5 flex flex-col gap-3"><div className="font-bold flex items-center gap-2"><Icon n="max" s={16} />Окно игры</div>
              <div className="seg flex"><button className={'flex-1 ' + (!cfg.fullscreen ? 'on' : '')} onClick={() => setCfg({ ...cfg, fullscreen: false })}>В окне</button><button className={'flex-1 ' + (cfg.fullscreen ? 'on' : '')} onClick={() => setCfg({ ...cfg, fullscreen: true })}>Полный экран</button></div>
              {!cfg.fullscreen && <><div className="flex gap-1.5 flex-wrap">{[[1280, 720], [1600, 900], [1920, 1080], [2560, 1440]].map(([a, b]) => <button key={a} className={'pill ' + (cfg.width === a && cfg.height === b ? 'on' : '')} onClick={() => setCfg({ ...cfg, width: a, height: b })}>{a}×{b}</button>)}
                <button className={'pill ' + (cfg.width === screen.width && cfg.height === screen.height ? 'on' : '')} onClick={() => setCfg({ ...cfg, width: screen.width, height: screen.height })}>{t('screenSize')}</button></div>
              <div className="flex items-center gap-2"><span className="muted text-sm">Свой размер:</span><NumInput v={cfg.width} on={(n) => setCfg({ ...cfg, width: n })} />×<NumInput v={cfg.height} on={(n) => setCfg({ ...cfg, height: n })} /></div></>}
              <Switch on={cfg.hideOnLaunch !== false} set={(v) => setCfg({ ...cfg, hideOnLaunch: v })} label={t('hideLaunch')} />
              <Switch on={cfg.optimize !== false} set={(v) => setCfg({ ...cfg, optimize: v })} label="Оптимизация" hint="Sodium, Lithium, FerriteCore и др. Включена — удалённые моды скачаются снова; выключена — игра без них" /></div>
            <div className="card rounded-3xl p-5 flex flex-col gap-3"><div className="font-bold flex items-center gap-2"><Icon n="palette" s={16} />Внешний вид</div>
              <label className="text-sm"><div className="flex justify-between"><span>{t('uiScale')}</span><b>{uiscale}%</b></div><input type="range" min={70} max={130} step={5} value={uiscale} onChange={(e) => setUiscale(e.target.value)} className="w-full" /></label>
              <div className="text-sm">{t('effects')}</div><div className="flex gap-1.5 flex-wrap">{['none', 'snow', 'rain', 'leaves', 'stars', 'sparks'].map((k) => <button key={k} className={'pill ' + (fx === k ? 'on' : '')} onClick={() => setFx(k)}>{t('fx_' + k)}</button>)}</div>
              <div className="flex items-center justify-between"><span className="text-sm">Акцентный цвет</span><ColorPick value={accent === 'custom' ? custom : (ACCENTS[accent] ?? ACCENTS.aqua)[1]} set={(v) => { setCustom(v); setAccent('custom') }} size="sm" /></div></div>
            <div className="card rounded-3xl p-5 flex flex-col gap-3"><div className="font-bold flex items-center gap-2"><Icon n="discord" s={16} />Discord</div>
              <Switch on={cfg.discord !== false} set={(v) => { setCfg({ ...cfg, discord: v }); setTimeout(() => api.rpcRefresh(), 800) }} label="Статус в Discord" hint="Сервер, его аватарка, IP и версия — пока вы играете" />
              </div>
            <div className="card rounded-3xl p-5 flex flex-col gap-3 col-span-2"><div className="font-bold flex items-center gap-2"><Icon n="folder" s={16} />Файлы и Java</div>
              <div className="grid grid-cols-2 gap-3"><input placeholder={t('jvm')} value={cfg.jvm} onChange={(e) => setCfg({ ...cfg, jvm: e.target.value })} />
              <div className="flex gap-2"><input className="flex-1" placeholder={t('java')} value={cfg.java} readOnly />{cfg.java && <button className="btn" title="Авто" onClick={() => setCfg({ ...cfg, java: '' })}><Icon n="close" s={16} /></button>}<button className="btn" onClick={async () => { const j = await api.pickJava(); if (j) setCfg({ ...cfg, java: j }) }}><Icon n="folder" s={16} /></button></div></div>
              <div className="flex gap-2 flex-wrap"><button className="btn flex items-center gap-2" onClick={() => api.openGameDir()}><Icon n="folder" s={16} />{t('folder')}</button></div></div>
            </div>
            <div className="card rounded-3xl p-5 mt-5"><div className="flex justify-between items-center mb-3"><div className="font-bold">{t('storage')}</div><button className="btn" onClick={async () => { await api.clearCache(); api.storage().then(setStorage); say(t('saved')) }}>{t('clearCache')}</button></div>
              {storage && <><div className="text-2xl font-black mb-2">{(storage.total / 1073741824).toFixed(2)} GB</div><div className="flex h-3 rounded-full overflow-hidden mb-3">{storage.items.map((x: any, i: number) => <div key={x.key} style={{ width: (x.bytes / Math.max(1, storage.total)) * 100 + '%', background: ['#22d3ee', '#a855f7', '#f59e0b', '#f43f5e'][i] }} />)}</div>
                <div className="flex gap-4 flex-wrap text-sm mb-3">{storage.items.map((x: any, i: number) => <span key={x.key}><span className="inline-block w-2.5 h-2.5 rounded-full mr-1.5" style={{ background: ['#22d3ee', '#a855f7', '#f59e0b', '#f43f5e'][i] }} />{t(['profiles', 'gameFiles', 'java', 'cache'][i])} — {(x.bytes / 1048576).toFixed(0)} MB</span>)}</div>
                {storage.profiles.map((p: any) => <div key={p.name} className="row !cursor-default"><span className="flex-1">Minecraft {p.name}</span><span className="muted text-sm">{(p.bytes / 1048576).toFixed(0)} MB</span></div>)}</>}</div>
          </div>}
          {tab === 'worlds' && <Worlds key={worldsView} initial={worldsView as any} ver={ver} versions={verOpts} play={play} say={say} running={running} />}
          {tab === 'visuals' && <VisualsTab plan="PREMIUM" bg={visBg} say={say} ver={ASTERIA[ver] === 'full' ? ver : rec} onBuy={shop} />}
        </main>
      </div>

      <Modal open={addOpen} onClose={() => setAddOpen(false)} title={t('addAccount')} w={660}>
        <div className="grid grid-cols-2 gap-4"><div className="card rounded-2xl p-5 flex flex-col gap-3"><svg width="34" height="34" viewBox="0 0 24 24"><rect width="11" height="11" fill="#f25022" /><rect x="13" width="11" height="11" fill="#7fba00" /><rect y="13" width="11" height="11" fill="#00a4ef" /><rect x="13" y="13" width="11" height="11" fill="#ffb900" /></svg>
          <div className="font-bold">{t('microsoft')}</div><div className="muted text-sm flex-1">{t('msDesc')}</div><button className="btn btn-primary" onClick={async () => { const r = await api.addMicrosoft(); if (r?.error) say(r.error, 'err'); else if (r?.cancelled) say(r.info, 'info'); else { refresh(); setAddOpen(false); say('Аккаунт Microsoft добавлен') } }}>{t('addMs')}</button></div>
          <div className="card rounded-2xl p-5 flex flex-col gap-3"><Gem s={34} /><div className="font-bold">Offline</div><div className="muted text-sm flex-1">{t('offDesc')}</div><input placeholder={t('nick')} value={offNick} maxLength={16} className={offNick && !/^[A-Za-z0-9_]{3,16}$/.test(offNick) ? '!border-rose-500' : ''} onChange={(e) => setOffNick(e.target.value.trim())} onKeyDown={(e) => e.key === 'Enter' && (document.getElementById('addOffBtn') as HTMLButtonElement)?.click()} />
            {offNick && !/^[A-Za-z0-9_]{3,16}$/.test(offNick) && <div className="text-xs text-rose-400 -mt-1">{/[^A-Za-z0-9_]/.test(offNick) ? 'Только латинские буквы, цифры и _ — без русских букв, пробелов и символов' : 'Ник должен быть от 3 до 16 символов'}</div>}
            <button id="addOffBtn" className="btn btn-primary" onClick={async () => { if (/^[A-Za-z0-9_]{3,16}$/.test(offNick)) { await api.addOffline(offNick); setOffNick(''); refresh(); setAddOpen(false); say('Аккаунт ' + offNick + ' добавлен') } else say(/[^A-Za-z0-9_]/.test(offNick) ? 'Неверный ник: можно только латинские буквы, цифры и _ (3–16 символов)' : 'Неверный ник: нужно от 3 до 16 символов', 'err') }}>{t('addOff')}</button></div></div></Modal>
      {news && (() => { const n = conf.news.find((x: any) => x.id === news); return n ? <Modal open onClose={() => setNews('')} w={640}><div className="-m-6 mb-0 relative h-60 overflow-hidden rounded-t-3xl"><NewsImg v={n.image} cls="w-full h-full object-cover" /><div className="absolute inset-0 bg-gradient-to-t from-black/80 to-transparent" /><h3 className="absolute bottom-4 left-6 right-6 text-2xl font-black text-white">{tx(n.title)}</h3></div>
        <p className="mt-5 mb-4 whitespace-pre-line">{tx(n.text)}</p><div className="flex gap-2"><button className="btn btn-primary" onClick={() => api.openLink(n.url || 'https://t.me/safimine_grief')}>{t('openTg')}</button><button className="btn" onClick={() => setNews('')}>Закрыть</button></div></Modal> : null })()}
      <Modal open={profModal} onClose={() => setProfModal(false)} title={t('newProfile')} w={440}><div className="flex flex-col gap-3">
        <input placeholder="PvP / PvE / Гриф" value={pName} onChange={(e) => setPName(e.target.value)} autoFocus /><Dropdown value={pVer} set={setPVer} options={verOpts} w={380} className="w-full" />
        <p className="muted text-xs">Профиль — отдельная сборка со своими модами, настройками и мирами.</p>
        <button className="btn btn-primary" onClick={async () => { if (!pName.trim()) return; const p = await api.profAdd({ name: pName.trim(), version: pVer }); setVer(p.version); setProfiles(await api.profList()); setPName(''); setProfModal(false) }}>{t('create')}</button></div></Modal>
      <Modal open={!!prev} onClose={() => setPrev(null)} title={prev?.title} w={780}>{prev && <>
        <div className="flex gap-3 items-start mb-3"><Pic src={prev.icon_url} cls="w-16 h-16 rounded-2xl object-cover shrink-0" /><div className="min-w-0"><div className="text-sm">{prevInfo ? prevInfo.desc : <span className="muted">Загружаем описание…</span>}</div>
          <div className="muted text-xs mt-1 flex gap-3 flex-wrap"><span className="flex items-center gap-1"><Icon n="download" s={11} />{(prevInfo?.downloads ?? prev.downloads)?.toLocaleString()}</span>{prevInfo?.categories && <span>{prevInfo.categories.slice(0, 5).join(' · ')}</span>}</div></div></div>
        {prevInfo?.text && <div className="card rounded-2xl p-3 mb-3 text-sm whitespace-pre-line overflow-auto" style={{ maxHeight: 220 }}>{prevInfo.text}</div>}
        <div className="grid grid-cols-2 gap-2">{(prevInfo?.gallery?.length ? prevInfo.gallery : prev.gallery?.length ? prev.gallery : [prev.featured_gallery].filter(Boolean)).slice(0, 6).map((u: string) => <Pic key={u} src={u} cls="w-full h-36 rounded-xl object-cover" />)}</div><p className="muted text-sm mt-3">{prev.description}</p>
        <button className="btn btn-primary mt-3" onClick={() => { install(prev.slug); setPrev(null) }}>{t('install')}</button></>}</Modal>
      {shotView && <div className="fixed inset-0 z-[75] flex items-center justify-center modal-bg" onClick={() => setShotView(null)}><div className="glass rounded-3xl p-4 relative pop-in" style={{ maxWidth: '92vw' }} onClick={(e) => e.stopPropagation()}><button className="absolute top-6 right-6 z-10 w-10 h-10 rounded-full flex items-center justify-center text-white" style={{ background: 'rgba(0,0,0,.55)', backdropFilter: 'blur(6px)' }} title="Закрыть (Esc)" onClick={() => setShotView(null)}><Icon n="close" s={18} /></button><ShotImg file={shotView.file} w={1400} cls="rounded-2xl max-h-[72vh]" />
        {shotList.length > 1 && <><button className="shot-nav left-6" title="Предыдущий (←)" onClick={() => shotStep(-1)}><Icon n="left" s={22} /></button><button className="shot-nav right-6" title="Следующий (→)" onClick={() => shotStep(1)}><Icon n="right" s={22} /></button>
          <div className="absolute bottom-[72px] left-1/2 -translate-x-1/2 text-xs text-white px-3 py-1 rounded-full" style={{ background: 'rgba(0,0,0,.55)' }}>{shotList.findIndex((x) => x.file === shotView.file) + 1} / {shotList.length}</div></>}
        <div className="flex gap-2 mt-3"><button className="btn btn-ghost flex items-center gap-2 mr-auto" onClick={() => setShotView(null)}><Icon n="left" s={16} />Назад</button><button className="btn flex items-center gap-2" onClick={() => { api.shotAct(shotView.file, 'copy'); say(t('copiedImg')) }}><Icon n="copy" s={16} /> {t('copyImg')}</button><button className="btn flex items-center gap-2" onClick={() => api.shotAct(shotView.file, 'open')}><Icon n="folder" s={16} /> {t('openIn')}</button>
          <button className="btn flex items-center gap-2" onClick={async () => { await api.shotAct(shotView.file, 'delete'); setShots(shots.filter((x) => x.file !== shotView.file)); setShotView(null) }}><Icon n="trash" s={16} /> {t('del')}</button></div></div></div>}
      {edit && <SkinStudio mode={edit === 'skin' ? 'skin' : 'cape'} src={edit === 'skin' ? skinUrl : (capeUrl || '')} skin={skinUrl} cape={capeUrl} slim={variant === 'slim'} pickImage={() => api.pickImage()} onClose={() => setEdit('')}
        onSave={async (d) => { const kind = edit === 'skin' ? 'skin' : 'cape'; if (kind === 'skin') { setSkin(d); addSkin(d) } else { const it = { id: 'my' + Date.now().toString(36), data: d }; setMyCapes([it, ...myCapes]); setCape(it.id) } setEdit(''); const r = await api.saveTexture({ data: kind === 'cape' ? await withElytra(d) : d, kind, slim: variant === 'slim' }); say(r?.uploaded ? t('uploaded') : t('savedLocal')) }} />}
      <Palette cmds={cmds} open={pal} setOpen={setPal} />
      {friend.open && <Modal open onClose={() => setFriend({ open: false, code: '', busy: false })} title="Код от друга">
        <p className="muted text-sm mb-3">Друг нажал «Поделиться» — сборкой модов, скином, плащом или прицелом — и прислал вам 6 символов.</p>
        <input className="w-full text-center font-mono text-2xl tracking-[.3em]" autoFocus value={friend.code} placeholder="K7Q2XD" onChange={(e) => { const v = e.target.value.trim(); setFriend({ ...friend, code: v.startsWith('ASH1:') ? v : v.toUpperCase().slice(0, 6), got: undefined }) }} />
        {friend.got && <div className="card rounded-2xl p-3 mt-3 text-sm">{friend.got.kind === 'modpack' ? <><b>Сборка для Minecraft {friend.got.data.ver}</b><div className="muted">Моды: {friend.got.data.mods.length} · ресурспаки: {friend.got.data.packs.length} · шейдеры: {friend.got.data.shaders.length}. Поставим на {ver}.</div></>
          : friend.got.kind === 'crosshair' ? <b>Прицел</b> : <div className="flex items-center gap-3"><img src={friend.got.data} className="h-12 rounded" style={{ imageRendering: 'pixelated' }} /><b>{friend.got.kind === 'skin' ? 'Скин' : 'Плащ'}</b></div>}</div>}
        <div className="flex gap-2 justify-end mt-4">{!friend.got ? <button className="btn btn-primary" disabled={(friend.code.length !== 6 && !friend.code.startsWith('ASH1:')) || friend.busy} onClick={async () => { setFriend({ ...friend, busy: true }); const r = await api.shareGet(friend.code); r?.error ? (say(r.error, 'err'), setFriend({ ...friend, busy: false })) : setFriend({ ...friend, busy: false, got: r }) }}>{friend.busy ? '…' : 'Найти'}</button>
          : <button className="btn btn-primary" onClick={() => applyFriend(friend.got)}>Установить</button>}</div></Modal>}
      {capeSrc && <CapeMaker src={capeSrc} onCancel={() => setCapeSrc('')} onDone={(png) => { setCapeSrc(''); addCape(png) }} />}
      {gifSrc && <GifCapeMaker src={gifSrc} onCancel={() => setGifSrc('')} onDone={(png, n) => { setGifSrc(''); addCape(png); say('Анимированный плащ: ' + n + ' кадров') }} onError={(m) => { setGifSrc(''); say(m, 'err') }} />}
      <LaunchOverlay prog={prog} img={hero} t={t} /><LogModal open={logOpen} onClose={() => setLogOpen(false)} t={t} /><UpdateModal upd={upd} t={t} compact={running} />{shotAsk && <div className='ask-bg' onClick={() => setShotAsk(false)}><div className='ask' onClick={(e) => e.stopPropagation()}><b className='block text-lg'>Удалить все скриншоты?</b>
        <p className='muted text-sm mt-2'>{shots.length} скриншотов из всех версий переместятся в корзину Windows.</p>
        <div className='flex gap-2 mt-4 justify-end'><button className='btn' onClick={() => setShotAsk(false)}>Отмена</button><button className='btn-danger' onClick={async () => { setShotAsk(false); const n = await api.shotsClear(); setShots([]); say('В корзине: ' + n + ' скриншотов') }}><Icon n='trash' s={14} />Удалить все</button></div></div></div>}
      {verAsk && <div className='ask-bg' onClick={() => setVerAsk('')}><div className='ask' onClick={(e) => e.stopPropagation()}><b className='block text-lg'>{verAsk === 'reinstall' ? 'Переустановить Minecraft ' + ver + '?' : 'Удалить Minecraft ' + ver + '?'}</b>
        <p className='muted text-sm mt-2'>{verAsk === 'reinstall' ? 'Файлы игры скачаются заново. Миры, моды и настройки останутся.' : 'Файлы игры уйдут в корзину. Миры, моды и настройки можно оставить или удалить тоже.'}</p>
        <div className='flex gap-2 mt-4 justify-end flex-wrap'><button className='btn' onClick={() => setVerAsk('')}>Отмена</button>{verAsk === 'reinstall' ? <button className='btn-add' onClick={() => verDo('reinstall')}><span className='ic'><Icon n='reset' s={13} /></span>Переустановить</button>
          : <><button className='btn' onClick={() => verDo('remove')}>Только файлы игры</button><button className='btn-danger' onClick={() => verDo('remove', true)}><Icon n='trash' s={14} />Вместе с мирами</button></>}</div></div></div>}<Splash pct={boot.pct} msg={boot.msg} />
      {toast && <div className="toastx pop-in"><span className={'ic ' + toastKind}>{toastKind === 'err' ? <Icon n="close" s={18} /> : toastKind === 'info' ? <Icon n="eye" s={18} /> : <Gem s={20} glow={false} />}</span><div className="min-w-0"><b className="block text-sm">{toastKind === 'err' ? 'Ошибка' : toastKind === 'info' ? 'Внимание' : 'Готово'}</b><span className="muted text-xs block">{toast}</span></div><button className="iconbtn !w-7 !h-7" onClick={() => setToast('')}><Icon n="close" s={12} /></button></div>}
    </div>
  )
}
