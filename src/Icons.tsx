import logo from './assets/logo.png'
import alMark from './assets/al.png'
const P: Record<string, string> = {
  sliders: 'M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1M15 4v4M9 10v4M17 16v4', cursor: 'M5 3l14 7-6 2-2 6L5 3z', volume: 'M4 9h4l5-4v14l-5-4H4V9zM17 9a4 4 0 010 6M19.5 6.5a8 8 0 010 11',
  chat: 'M4 5h16v11H9l-5 4V5z', keyboard: 'M3 6h18v12H3zM7 10h.01M11 10h.01M15 10h.01M7 14h10', sparkles: 'M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3zM19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15z',
  lock: 'M6 11h12v9H6zM8 11V8a4 4 0 018 0v3', reset: 'M4 12a8 8 0 1 0 2.3-5.6M4 4v4h4', move: 'M12 3v18M3 12h18M12 3l-3 3M12 3l3 3M12 21l-3-3M12 21l3-3M3 12l3-3M3 12l3 3M21 12l-3-3M21 12l-3 3',
    home: 'M3 11l9-8 9 8M5 10v10h14V10M10 20v-6h4v6', versions: 'M6 8h12a4 4 0 014 4v2a3 3 0 01-5 2l-1.5-2h-7L7 16a3 3 0 01-5-2v-2a4 4 0 014-4zM8 10v4M6 12h4M15.5 11h.01M18 13h.01',
  mods: 'M10 3a2 2 0 014 0v2h4a1 1 0 011 1v4h-2a2 2 0 100 4h2v4a1 1 0 01-1 1h-4v-2a2 2 0 10-4 0v2H6a1 1 0 01-1-1v-4h2a2 2 0 100-4H5V6a1 1 0 011-1h4V3z',
  skins: 'M8 3l-5 3 2 4 3-1v11h8V9l3 1 2-4-5-3a4 4 0 01-8 0z', servers: 'M12 3a9 9 0 100 18 9 9 0 000-18zM3 12h18M12 3a14 14 0 010 18M12 3a14 14 0 000 18',
  settings: 'M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0M14 4v4M8 10v4M16 16v4', shop: 'M3 4h2l2.5 11h10L20 7H6M9 20h.01M17 20h.01',
  discord: 'M4 5h16v11H9l-5 4V5zM9 10h.01M15 10h.01', telegram: 'M21 3L3 11l7 2 2 7 9-17zM10 13l11-10', rules: 'M6 3h9l4 4v14H6V3zM14 3v5h5M9 13h7M9 17h7',
  shield: 'M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3zM9 12l2 2 4-4', bot: 'M5 8h14v10H5V8zM12 4v4M9 13h.01M15 13h.01M9 17h6M3 12v3M21 12v3',
  support: 'M4 14v-2a8 8 0 0116 0v2M4 14h3v5H4v-5zM17 14h3v5h-3v-5zM20 19c0 2-3 2-6 2', play: 'M7 4l13 8-13 8V4z', pause: 'M8 5v14M16 5v14', prev: 'M18 5L8 12l10 7V5zM6 5v14', next: 'M6 5l10 7-10 7V5zM18 5v14', music: 'M9 18V5l11-2v13M9 18a3 3 0 11-6 0 3 3 0 016 0zM20 16a3 3 0 11-6 0 3 3 0 016 0z', share: 'M4 13v6h16v-6M12 3v12M7 8l5-5 5 5', link: 'M10 14a4 4 0 005.7 0l3-3a4 4 0 00-5.7-5.7l-1 1M14 10a4 4 0 00-5.7 0l-3 3a4 4 0 005.7 5.7l1-1', close: 'M5 5l14 14M19 5L5 19', min: 'M5 12h14',
  max: 'M5 5h14v14H5z', check: 'M5 12l5 5 9-10', star: 'M12 3l2.8 5.7 6.2.9-4.5 4.4 1 6.2L12 17.3 6.5 20.2l1-6.2L3 9.6l6.2-.9L12 3z', search: 'M11 4a7 7 0 100 14 7 7 0 000-14zM21 21l-5-5',
  image: 'M4 5h16v14H4zM4 15l4-4 4 4 3-3 5 5M9 9h.01', pencil: 'M4 20l4-1 11-11-3-3L5 16l-1 4z', eraser: 'M4 15l8-8 6 6-5 5H8l-4-3z', bucket: 'M5 12l7-7 7 7-6 6-8-6zM20 16c1 1.5 1 3-1 3s-2-1.5-1-3', picker: 'M14 5l5 5-9 9H5v-5l9-9z',
  undo: 'M9 7H4v5M4 12c2-4 6-6 10-5 3 1 5 4 5 8', line: 'M5 19L19 5', rect: 'M5 6h14v12H5z', noise: 'M5 5h3v3H5zM11 8h3v3h-3zM16 5h3v3h-3zM6 13h3v3H6zM13 15h3v3h-3zM17 12h2v2h-2z', mirror: 'M12 3v18M9 7l-5 5 5 5zM15 7l5 5-5 5z', zoom: 'M11 4a7 7 0 100 14 7 7 0 000-14zM21 21l-5-5M8 11h6M11 8v6', grid: 'M4 4h16v16H4zM4 12h16M12 4v16', eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zM12 9a3 3 0 100 6 3 3 0 000-6z', upload: 'M12 16V5M7 9l5-5 5 5M5 20h14', cpu: 'M7 7h10v10H7zM9 3v4M15 3v4M9 17v4M15 17v4M3 9h4M3 15h4M17 9h4M17 15h4',
  copy: 'M9 9h11v11H9zM5 15V4h11', sun: 'M12 8a4 4 0 100 8 4 4 0 000-8zM12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5L19 19M5 19l1.5-1.5M17.5 6.5L19 5',
  moon: 'M20 14A8 8 0 019.5 4 8 8 0 1020 14z', palette: 'M12 3a9 9 0 100 18c1.5 0 2-1 1.5-2.2-.5-1.3.2-2.3 1.5-2.3H17a4 4 0 004-4A9 9 0 0012 3zM7.5 11h.01M10 7h.01M15 7h.01',
  swords: 'M14.5 17.5L3 6V3h3l11.5 11.5M13 19l6-6M16 16l4 4M19 21l2-2M9.5 6.5L14 2h3v3l-4.5 4.5M5 14l-2 2 2 2M8 17l-3 3', crown: 'M3 8l4 4 5-7 5 7 4-4-2 11H5L3 8z', clans: 'M4 4h16v6c0 5-3.5 8.5-8 10-4.5-1.5-8-5-8-10V4zM9 9l3 3 3-3M12 12v4', clock: 'M12 3a9 9 0 100 18 9 9 0 000-18zM12 7v5l3 2',
  chevron: 'M6 9l6 6 6-6', plus: 'M12 5v14M5 12h14', trash: 'M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3', download: 'M12 4v11M7 11l5 5 5-5M5 20h14',
  folder: 'M3 6h6l2 2h10v11H3V6z', user: 'M12 12a4 4 0 100-8 4 4 0 000 8zM4 21a8 8 0 0116 0', arrow: 'M5 12h14M13 6l6 6-6 6', left: 'M15 6l-6 6 6 6', right: 'M9 6l6 6-6 6', cube: 'M12 3l8 4.5v9L12 21l-8-4.5v-9L12 3zM12 12l8-4.5M12 12v9M12 12L4 7.5',
}
export const Icon = ({ n, s = 20, fill = false }: { n: string; s?: number; fill?: boolean }) => n === 'sparkles' || n === 'star' ? <span style={{ display: 'inline-block', flex: 'none', width: s * 1.2, height: s * 1.2, margin: -s * 0.1, background: 'currentColor', WebkitMask: `url(${alMark}) center / contain no-repeat`, mask: `url(${alMark}) center / contain no-repeat` }} /> /* the AL mark instead of any star */ : <svg width={s} height={s} viewBox="0 0 24 24" fill={fill ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d={P[n]} /></svg>
/** Asteria star: soft glassy four-point star with a bright core (matches the app icon), coloured by the accent */
/** the AL mark (letters with the orbit), used wherever the Asteria star used to be */
export const Gem = ({ s = 24, glow = true }: { s?: number; glow?: boolean }) => <span style={{ display: 'inline-block', flex: 'none', width: s * 1.25, height: s * 1.25, margin: -s * 0.125, background: 'linear-gradient(135deg, var(--a2), var(--a1))', WebkitMask: `url(${alMark}) center / contain no-repeat`, mask: `url(${alMark}) center / contain no-repeat`, ...(glow ? { filter: 'drop-shadow(0 0 6px color-mix(in srgb, var(--a2) 60%, transparent))' } : {}) }} />
export const OldGem = ({ s = 24, glow = true }: { s?: number; glow?: boolean }) => { const id = 'gs' + s
  return <svg width={s} height={s} viewBox="0 0 64 64" style={glow ? { filter: 'drop-shadow(0 0 6px color-mix(in srgb, var(--a2) 70%, transparent))', overflow: 'visible' } : undefined}>
    <defs><radialGradient id={id} cx=".5" cy=".5" r=".5"><stop offset="0" stopColor="#fff" /><stop offset=".25" stopColor="color-mix(in srgb, var(--a2) 35%, white)" /><stop offset=".7" stopColor="var(--a2)" /><stop offset="1" stopColor="var(--a1)" /></radialGradient></defs>
    <path d="M32 3C34.5 21 43 29.5 61 32C43 34.5 34.5 43 32 61C29.5 43 21 34.5 3 32C21 29.5 29.5 21 32 3Z" fill={`url(#${id})`} />
    <path d="M32 3C33.2 17 36 25 41 28L32 32Z" fill="#fff" opacity=".35" /></svg> }
export const Wordmark = ({ tag = false, big = false }: { tag?: boolean; big?: boolean }) => <span className="flex items-center gap-2.5"><img src={logo} className={(big ? 'w-11 h-11' : 'w-6 h-6') + ' rounded-lg'} style={{ filter: 'drop-shadow(0 0 10px color-mix(in srgb,var(--a2) 55%,transparent))' }} />
  <span className="flex flex-col leading-none"><span style={{ fontWeight: 800, letterSpacing: '.01em', fontSize: big ? 17 : 15 }}><span style={{ color: 'var(--text)' }}>Asteria</span><span style={{ color: 'var(--a2)' }}>Launcher</span></span>
    {tag && <span className="muted" style={{ fontSize: 11, marginTop: 5, letterSpacing: '.04em' }}>Play. Explore. Create.</span>}</span></span>
const f = (b: string) => 'data:image/svg+xml;utf8,' + encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 30 20">${b}</svg>`)
const h = (c: string[]) => c.map((x, i) => `<rect y="${i * 20 / c.length}" width="30" height="${20 / c.length + .1}" fill="${x}"/>`).join('')
const v = (c: string[]) => c.map((x, i) => `<rect x="${i * 10}" width="10.1" height="20" fill="${x}"/>`).join('')
export const FLAGS: Record<string, string> = {
  ru: f(h(['#fff', '#1c57a5', '#d52b1e'])), en: f('<rect width="30" height="20" fill="#012169"/><path d="M0 0L30 20M30 0L0 20" stroke="#fff" stroke-width="4"/><path d="M0 0L30 20M30 0L0 20" stroke="#c8102e" stroke-width="1.5"/><path d="M15 0v20M0 10h30" stroke="#fff" stroke-width="6"/><path d="M15 0v20M0 10h30" stroke="#c8102e" stroke-width="3.5"/>'),
  uk: f(h(['#0057b7', '#ffd700'])), pl: f(h(['#fff', '#dc143c'])), de: f(h(['#000', '#dd0000', '#ffce00'])), es: f('<rect width="30" height="20" fill="#aa151b"/><rect y="5" width="30" height="10" fill="#f1bf00"/>'),
  fr: f(v(['#0055a4', '#fff', '#ef4135'])), it: f(v(['#009246', '#fff', '#ce2b37'])), pt: f('<rect width="30" height="20" fill="#f00"/><rect width="12" height="20" fill="#060"/><circle cx="12" cy="10" r="4" fill="#fc0"/>'),
  tr: f('<rect width="30" height="20" fill="#e30a17"/><circle cx="11" cy="10" r="5" fill="#fff"/><circle cx="12.4" cy="10" r="4" fill="#e30a17"/><circle cx="17" cy="10" r="1.6" fill="#fff"/>'),
  zh: f('<rect width="30" height="20" fill="#de2910"/><polygon points="5,3 6.2,6.6 10,6.6 7,8.8 8.1,12.4 5,10.2 1.9,12.4 3,8.8 0,6.6 3.8,6.6" fill="#ffde00"/>'),
  ja: f('<rect width="30" height="20" fill="#fff"/><circle cx="15" cy="10" r="6" fill="#bc002d"/>'), ko: f('<rect width="30" height="20" fill="#fff"/><path d="M9 10a6 6 0 0112 0z" fill="#cd2e3a"/><path d="M9 10a6 6 0 0012 0z" fill="#0047a0"/>'),
  nl: f(h(['#ae1c28', '#fff', '#21468b'])), cs: f('<rect width="30" height="10" fill="#fff"/><rect y="10" width="30" height="10" fill="#d7141a"/><polygon points="0,0 15,10 0,20" fill="#11457e"/>'),
}
