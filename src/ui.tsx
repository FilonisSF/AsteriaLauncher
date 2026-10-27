import { ReactNode, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import ColorWheel from './ColorWheel'
import { Icon , Gem } from './Icons'

/** On/off switch. Optimistic: flips instantly, the caller saves in the background (no flicker while lists reload). */
export function Switch({ on, set, label, hint, disabled }: { on: boolean; set: (v: boolean) => void; label?: ReactNode; hint?: ReactNode; disabled?: boolean }) {
  const [v, setV] = useState(on); useEffect(() => setV(on), [on])
  const flip = () => { if (disabled) return; setV(!v); set(!v) }
  const sw = <span role="switch" aria-checked={v} className={'switch ' + (v ? 'on' : '') + (disabled ? ' opacity-40' : '')} />
  if (!label) return <button type="button" className="sw-btn" onClick={(e) => { e.stopPropagation(); flip() }}>{sw}</button>
  return <button type="button" className="flex items-center justify-between gap-3 w-full text-left py-1.5 sw-btn" onClick={flip}>
    <span className="text-sm">{label}{hint && <span className="muted text-xs block">{hint}</span>}</span>{sw}</button>
}

/** Square check box drawn by us (the native one flashes white on dark themes). */
export function Check({ on, set, label }: { on: boolean; set: (v: boolean) => void; label?: ReactNode }) {
  return <button type="button" className="flex items-center gap-2.5 text-sm text-left sw-btn" onClick={() => set(!on)}>
    <span className={'chk ' + (on ? 'on' : '')}>{on && <Icon n="check" s={13} />}</span>{label}</button>
}

const usePop = () => {
  const [r, setR] = useState<DOMRect | null>(null), ref = useRef<HTMLButtonElement>(null)
  useEffect(() => { if (!r) return; const k = (e: KeyboardEvent) => e.key === 'Escape' && setR(null); addEventListener('keydown', k); const s = () => setR(null); addEventListener('resize', s); return () => { removeEventListener('keydown', k); removeEventListener('resize', s) } }, [r])
  return { r, ref, open: () => setR(ref.current!.getBoundingClientRect()), close: () => setR(null), toggle: () => setR(r ? null : ref.current!.getBoundingClientRect()) }
}
/** keeps a popup inside the window */
function Floating({ at, w, children, onClose }: { at: DOMRect; w: number; children: ReactNode; onClose: () => void }) {
  const box = useRef<HTMLDivElement>(null), [pos, setPos] = useState({ left: at.left, top: at.bottom + 6 })
  useLayoutEffect(() => { const h = box.current?.offsetHeight || 0, left = Math.max(8, Math.min(at.left, innerWidth - w - 8)), below = at.bottom + 6 + h < innerHeight - 8
    setPos({ left, top: below ? at.bottom + 6 : Math.max(8, at.top - 6 - h) }) }, [at, w])
  // rendered into <body>: inside a tilted card (CSS transform) a fixed popup is clipped and drawn under the next cards
  return createPortal(<><div className="fixed inset-0 z-[90]" onMouseDown={onClose} /><div ref={box} className="floating z-[91]" style={{ ...pos, width: w }}>{children}</div></>, document.body)
}

export const PRESETS = ['#22d3ee', '#0ea5e9', '#6366f1', '#a855f7', '#ec4899', '#f43f5e', '#f97316', '#facc15', '#84cc16', '#10b981', '#ffffff', '#0b1220']
/** Colour field: a swatch that opens a ring-and-square picker with presets and a hex box (replaces the browser's colour input). */
export function ColorPick({ value, set, label, size = 'md' }: { value: string; set: (hex: string) => void; label?: ReactNode; size?: 'sm' | 'md' }) {
  const p = usePop(), [hex, setHex] = useState(value); useEffect(() => setHex(value), [value])
  return <span className="inline-flex items-center gap-2">
    <button ref={p.ref} type="button" className={'swatch ' + size} style={{ background: value }} onClick={p.toggle} title={value} />
    {label && <span className="text-sm">{label}</span>}
    {p.r && <Floating at={p.r} w={232} onClose={p.close}><div className="flex flex-col items-center gap-3 p-1">
      <ColorWheel value={/^#[0-9a-f]{6}$/i.test(value) ? value : '#22d3ee'} onChange={set} size={200} />
      <div className="grid grid-cols-6 gap-1.5 w-full">{PRESETS.map((c) => <button key={c} type="button" className={'swatch sm ' + (c === value.toLowerCase() ? 'sel' : '')} style={{ background: c }} onClick={() => set(c)} />)}</div>
      <div className="flex gap-2 w-full items-center"><span className="swatch sm" style={{ background: value }} /><input className="flex-1 min-w-0 w-full !py-1.5 font-mono text-sm" value={hex} spellCheck={false}
        onChange={(e) => { const v = e.target.value.trim(); setHex(v); if (/^#?[0-9a-f]{6}$/i.test(v)) set((v.startsWith('#') ? v : '#' + v).toLowerCase()) }} /></div></div></Floating>}
  </span>
}

export type Opt = { value: string; label: ReactNode; hint?: ReactNode; icon?: ReactNode; disabled?: boolean }
/** Drop-down list styled like the rest of the launcher (instead of the system <select>). */
export function Dropdown({ value, options, set, w = 220, className = '', placeholder }: { value: string; options: Opt[]; set: (v: string) => void; w?: number; className?: string; placeholder?: string }) {
  const p = usePop(), cur = options.find((o) => o.value === value)
  return <>
    <button ref={p.ref} type="button" className={'dd ' + className} onClick={p.toggle}>{cur?.icon}<span className="truncate">{cur?.label ?? placeholder ?? '—'}</span><span className={'transition ' + (p.r ? 'rotate-180' : '')}><Icon n="chevron" s={14} /></span></button>
    {p.r && <Floating at={p.r} w={Math.max(w, p.r.width)} onClose={p.close}><div className="flex flex-col gap-0.5 max-h-[320px] overflow-auto">{options.map((o) =>
      <button key={o.value} type="button" disabled={o.disabled} className={'row ' + (o.value === value ? 'sel' : '') + (o.disabled ? ' opacity-40' : '')} onClick={() => { set(o.value); p.close() }}>
        {o.icon}<span className="flex-1 min-w-0"><span className="block truncate font-semibold text-sm">{o.label}</span>{o.hint && <span className="muted text-xs block truncate">{o.hint}</span>}</span>{o.value === value && <Icon n="check" s={15} />}</button>)}</div></Floating>}
  </>
}

/** Card that leans toward the mouse (subtle 3D) with a moving light sheen. */
export function Tilt({ children, className = '', max = 6, onClick, style }: { children: ReactNode; className?: string; max?: number; onClick?: () => void; style?: React.CSSProperties }) {
  const el = useRef<HTMLDivElement>(null)
  const move = (e: React.MouseEvent) => { const r = el.current!.getBoundingClientRect(), x = (e.clientX - r.left) / r.width, y = (e.clientY - r.top) / r.height
    el.current!.style.setProperty('--rx', ((0.5 - y) * max).toFixed(2) + 'deg'); el.current!.style.setProperty('--ry', ((x - 0.5) * max).toFixed(2) + 'deg')
    el.current!.style.setProperty('--mx', (x * 100).toFixed(1) + '%'); el.current!.style.setProperty('--my', (y * 100).toFixed(1) + '%') }
  const leave = () => { el.current!.style.setProperty('--rx', '0deg'); el.current!.style.setProperty('--ry', '0deg') }
  return <div ref={el} className={'tilt ' + className} style={style} onMouseMove={move} onMouseLeave={leave} onClick={onClick}>{children}<span className="sheen" /></div>
}

export function Modal({ open, onClose, children, w = 560, title }: { open: boolean; onClose: () => void; children: ReactNode; w?: number; title?: ReactNode }) {
  useEffect(() => { if (!open) return; const k = (e: KeyboardEvent) => e.key === 'Escape' && onClose(); addEventListener('keydown', k); return () => removeEventListener('keydown', k) }, [open])
  if (!open) return null
  return <div className="fixed inset-0 z-[80] flex items-center justify-center modal-bg" onMouseDown={onClose}>
    <div className="glass rounded-3xl p-6 pop-in max-h-[88vh] overflow-auto" style={{ width: w }} onMouseDown={(e) => e.stopPropagation()}>
      {title && <div className="flex justify-between items-center mb-4"><h2 className="text-xl font-extrabold">{title}</h2><button className="iconbtn" onClick={onClose}><Icon n="close" s={16} /></button></div>}
      {children}</div></div>
}

/** Ctrl+F: jump anywhere / run an action by typing. */
export type Cmd = { id: string; label: string; hint?: string; icon: string; run: () => void }
export function Palette({ cmds, open, setOpen }: { cmds: Cmd[]; open: boolean; setOpen: (v: boolean) => void }) {
  const [q, setQ] = useState(''), [i, setI] = useState(0)
  useEffect(() => { const k = (e: KeyboardEvent) => { if ((e.ctrlKey || e.metaKey) && e.code === 'KeyF') { e.preventDefault(); setOpen(!open) } }; addEventListener('keydown', k); return () => removeEventListener('keydown', k) }, [open])
  useEffect(() => { setQ(''); setI(0) }, [open])
  if (!open) return null
  const list = q ? cmds.filter((c) => (c.label + ' ' + (c.hint || '')).toLowerCase().includes(q.toLowerCase())).slice(0, 9) : cmds.slice(8, 17)
  const go = (c?: Cmd) => { if (!c) return; setOpen(false); c.run() }
  return <div className="fixed inset-0 z-[95] flex justify-center modal-bg" style={{ paddingTop: '14vh' }} onMouseDown={() => setOpen(false)}>
    <div className="glass rounded-3xl p-2 w-[560px] h-fit pop-in" onMouseDown={(e) => e.stopPropagation()}>
      <div className="relative"><span className="absolute left-4 top-3.5 muted"><Icon n="search" s={18} /></span>
        <input autoFocus className="w-full !pl-11 !py-3 !text-base !border-0 !shadow-none !bg-transparent" placeholder="Куда перейти или что сделать…" value={q} onChange={(e) => { setQ(e.target.value); setI(0) }}
          onKeyDown={(e) => { if (e.key === 'ArrowDown') { e.preventDefault(); setI((i + 1) % Math.max(1, list.length)) } else if (e.key === 'ArrowUp') { e.preventDefault(); setI((i - 1 + list.length) % Math.max(1, list.length)) } else if (e.key === 'Enter') go(list[i]); else if (e.key === 'Escape') setOpen(false) }} /></div>
      {!q && <div className="grid grid-cols-4 gap-1.5 px-2 pt-2 pb-1" style={{ borderTop: '1px solid var(--line)' }}>{cmds.slice(0, 8).map((c) =>
        <button key={c.id} className="quick-tile" onClick={() => go(c)}><span className="cmd-ic"><Icon n={c.icon} s={17} /></span><span className="text-[12px] font-semibold leading-tight text-center line-clamp-2">{c.label.replace(/^Открыть:\s*/, '')}</span></button>)}</div>}
      {!q && list.length > 0 && <div className="label px-3 pt-2">Ещё команды</div>}
      <div className="flex flex-col gap-0.5 p-1" style={q ? { borderTop: '1px solid var(--line)' } : {}}>{list.map((c, n) => <button key={c.id} className={'row ' + (n === i ? 'sel' : '')} onMouseEnter={() => setI(n)} onClick={() => go(c)}>
        <span className="cmd-ic"><Icon n={c.icon} s={16} /></span><span className="flex-1 font-semibold text-sm">{c.label}</span>{c.hint && <span className="muted text-xs">{c.hint}</span>}</button>)}
        {!list.length && <div className="muted text-sm p-3">Ничего не найдено</div>}</div>
      <div className="muted text-[11px] px-3 pb-1.5 pt-1 flex gap-3"><span><kbd>↑</kbd> <kbd>↓</kbd> выбрать</span><span><kbd>Enter</kbd> открыть</span><span><kbd>Esc</kbd> закрыть</span></div></div></div>
}

/** loader marks used on version cards */
export const LoaderIcon = ({ k, s = 18 }: { k: 'fabric' | 'vanilla' | 'asteria' | 'legacy'; s?: number }) => {
  if (k === 'fabric') return <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M13.5 2.5l8 8-4 4-1-1-2 2 1 1-4 4-8-8z" fill="#dbd0b4" /><path d="M13.5 2.5l8 8-4 4-8-8z" fill="#c6bba5" /><path d="M9 12l3 3M11 10l3 3M7 14l3 3" stroke="#38342a" strokeWidth="1.4" strokeLinecap="round" /></svg>
  if (k === 'legacy') return <svg width={s} height={s} viewBox="0 0 24 24" fill="none"><path d="M13.5 2.5l8 8-4 4-1-1-2 2 1 1-4 4-8-8z" fill="#c7a46b" /><path d="M9 12l3 3M11 10l3 3M7 14l3 3" stroke="#3b2a12" strokeWidth="1.4" strokeLinecap="round" /></svg>
  if (k === 'asteria') return <Gem s={s * 0.85} glow={false} />
  return <svg width={s} height={s} viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2" fill="#5b8c32" /><rect x="3" y="3" width="18" height="6" fill="#79c05a" /><rect x="3" y="12" width="18" height="9" fill="#8b5a2b" /><rect x="6" y="14" width="3" height="3" fill="#6d4520" /><rect x="14" y="16" width="3" height="3" fill="#6d4520" /></svg>
}
