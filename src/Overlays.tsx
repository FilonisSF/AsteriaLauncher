import { useEffect, useState , useRef } from 'react'
import { Gem, Wordmark, Icon } from './Icons'
import appIcon from './assets/logo.png'
declare global { interface Window { api: any } }
export const Splash = ({ pct, msg }: { pct: number; msg: string }) => <div className="fixed inset-0 z-[100] flex flex-col items-center justify-center transition-opacity duration-700" style={{ opacity: pct >= 100 ? 0 : 1, pointerEvents: pct >= 100 ? 'none' : 'auto', background: 'radial-gradient(circle at 50% 40%,#0b2f3f,#03090f 70%)', color: '#e6fbff' }}>
  <div className="pulse"><img src={appIcon} className="w-32 h-32" style={{ filter: 'drop-shadow(0 0 40px color-mix(in srgb,var(--a2) 60%,transparent))' }} /></div><div className="mt-4 scale-150"><Wordmark onDark /></div>
  <div className="w-72 h-1.5 rounded-full mt-10 overflow-hidden" style={{ background: 'rgba(255,255,255,.1)' }}><div className="h-full rounded-full transition-all duration-300" style={{ width: pct + '%', background: 'linear-gradient(90deg,var(--a1),var(--a2))' }} /></div>
  <div className="text-xs mt-3 opacity-70">{msg}</div></div>
export function LaunchOverlay({ prog, img, t }: { prog: any; img: string; t: (k: string) => string }) {
  const [pos, setPos] = useState<{ x: number; y: number } | null>(() => { try { return JSON.parse(localStorage.getItem('launchCard') || 'null') } catch { return null } })
  const drag = useRef<{ dx: number; dy: number } | null>(null)
  if (!prog.stage) return null
  const steps = [0, 1, 2, 3, 4, 5].map((i) => t('ls' + i)), pct = Math.min(100, Math.max(0, Math.round(prog.pct || 0)))
  const place = pos ? { left: Math.max(8, Math.min(innerWidth - 348, pos.x)), top: Math.max(8, Math.min(innerHeight - 110, pos.y)) } : { right: 24, bottom: 24 }
  return <div className="fixed z-[80] w-[340px] rounded-2xl overflow-hidden shadow-2xl pop-in select-none" style={{ ...place, cursor: 'grab', border: '1px solid color-mix(in srgb,var(--a2) 35%,rgba(255,255,255,.1))', background: 'rgba(4,14,20,.92)', backdropFilter: 'blur(14px)', color: '#e6fbff', touchAction: 'none' }}
    title="Карточку можно перетащить"
    onPointerDown={(e) => { const r = (e.currentTarget as HTMLElement).getBoundingClientRect(); drag.current = { dx: e.clientX - r.left, dy: e.clientY - r.top }; (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId) }}
    onPointerMove={(e) => { const d = drag.current; if (!d) return; const p = { x: e.clientX - d.dx, y: e.clientY - d.dy }; setPos(p) }}
    onPointerUp={() => { drag.current = null; try { localStorage.setItem('launchCard', JSON.stringify(pos)) } catch {} }}>
    <img src={img} className="absolute inset-0 w-full h-full object-cover opacity-15 pointer-events-none" />
    <div className="absolute inset-0 pointer-events-none launch-glow" />
    <div className="relative p-4"><div className="flex items-center gap-3"><div className="pulse"><Gem s={30} /></div>
      <div className="flex-1 min-w-0"><div className="font-extrabold truncate">{steps[Math.min(steps.length - 1, prog.step || 0)]}</div><div className="text-[11px] opacity-60 truncate">Minecraft готовится к запуску</div></div>
      <span className="text-sm font-bold tabular-nums">{pct}%</span></div>
      <div className="h-2 rounded-full overflow-hidden mt-3 relative" style={{ background: 'rgba(255,255,255,.1)' }}><div className="h-full rounded-full transition-all duration-300 launch-bar" style={{ width: Math.max(4, pct) + '%' }} /></div>
      <div className="flex gap-1.5 mt-3">{steps.map((s, i) => <span key={i} className="h-1 flex-1 rounded-full transition-all" style={{ background: i <= (prog.step || 0) ? 'linear-gradient(90deg,var(--a1),var(--a2))' : 'rgba(255,255,255,.12)' }} />)}</div></div></div>
}
export const UpdateModal = ({ upd, t, compact }: { upd: any; t: (k: string) => string; compact?: boolean }) => !upd ? null : compact
  ? <div className="fixed left-6 bottom-6 z-[85] w-[320px] rounded-2xl p-4 pop-in shadow-2xl flex items-center gap-3" style={{ background: 'rgba(4,14,20,.94)', border: '1px solid color-mix(in srgb,var(--a2) 35%,rgba(255,255,255,.1))', color: '#e6fbff' }}>
    <Gem s={30} /><div className="flex-1 min-w-0"><b className="block text-sm">{t('updTitle')}{upd.version ? ' ' + upd.version : ''}</b>
      {upd.state === 'ready' ? <span className="text-xs opacity-70">Установится, когда закроете игру и лаунчер</span> : <span className="text-xs opacity-70">{t('updDownloading')} {upd.pct || 0}%</span>}</div>
    {upd.state === 'ready' && <button className="btn btn-primary !py-1.5 text-sm" onClick={() => window.api.installUpdate()}>{t('updRestart')}</button>}</div>
  : <div className="fixed inset-0 z-[90] flex items-center justify-center" style={{ background: 'rgba(2,8,14,.88)', backdropFilter: 'blur(12px)', color: '#e6fbff' }}>
  <div className="absolute top-3 right-3 flex gap-1.5">{(['min', 'max', 'close'] as const).map((a) => <button key={a} title={a === 'min' ? 'Свернуть' : a === 'max' ? 'Развернуть' : 'Закрыть — обновление поставится при следующем запуске'} onClick={() => window.api.win(a)} className="w-9 h-9 rounded-xl flex items-center justify-center hover:bg-white/10"><Icon n={a} s={15} /></button>)}</div>
  <div className="glass rounded-3xl p-8 w-[460px] text-center"><div className="pulse inline-block"><Gem s={64} /></div><h2 className="text-2xl font-extrabold mt-3">{t('updTitle')}{upd.version ? ' ' + upd.version : ''}</h2><p className="opacity-70 text-sm mt-1 mb-5">{t('updDesc')}</p>
    {upd.state === 'ready' ? <button className="btn btn-primary w-full" onClick={() => window.api.installUpdate()}>{t('updRestart')}</button>
      : <><div className="h-2 rounded-full overflow-hidden" style={{ background: 'rgba(255,255,255,.1)' }}><div className="h-full rounded-full transition-all" style={{ width: (upd.pct || 3) + '%', background: 'linear-gradient(90deg,var(--a1),var(--a2))' }} /></div><div className="text-xs opacity-60 mt-2">{t('updDownloading')} {upd.pct || 0}%</div></>}</div></div>
export function LogModal({ open, onClose, t }: { open: boolean; onClose: () => void; t: (k: string) => string }) {
  const [log, setLog] = useState('')
  useEffect(() => { if (open) window.api.gameLog().then(setLog) }, [open])
  if (!open) return null
  return <div className="fixed inset-0 z-[70] flex items-center justify-center" style={{ background: 'rgba(0,0,0,.6)' }} onClick={onClose}><div className="glass rounded-3xl p-5 w-[760px]" onClick={(e) => e.stopPropagation()}>
    <div className="flex justify-between items-center mb-3"><h3 className="text-xl font-bold">{t('journal')}</h3><div className="flex gap-2"><button className="btn" onClick={() => navigator.clipboard.writeText(log)}><Icon n="copy" s={16} /></button><button className="btn" onClick={onClose}><Icon n="close" s={16} /></button></div></div>
    <pre className="card rounded-2xl p-3 text-xs overflow-auto" style={{ height: 380, userSelect: 'text', whiteSpace: 'pre-wrap' }}>{log || '—'}</pre></div></div>
}
