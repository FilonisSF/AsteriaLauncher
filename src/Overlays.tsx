import { useEffect, useState } from 'react'
import { Gem, Wordmark, Icon } from './Icons'
import appIcon from './assets/logo.png'
declare global { interface Window { api: any } }
export const Splash = ({ pct, msg }: { pct: number; msg: string }) => <div className="fixed inset-0 z-[100] flex flex-col items-center justify-center transition-opacity duration-700" style={{ opacity: pct >= 100 ? 0 : 1, pointerEvents: pct >= 100 ? 'none' : 'auto', background: 'radial-gradient(circle at 50% 40%,#0b2f3f,#03090f 70%)', color: '#e6fbff' }}>
  <div className="pulse"><img src={appIcon} className="w-32 h-32" style={{ filter: 'drop-shadow(0 0 40px color-mix(in srgb,var(--a2) 60%,transparent))' }} /></div><div className="mt-4 scale-150"><Wordmark /></div>
  <div className="w-72 h-1.5 rounded-full mt-10 overflow-hidden" style={{ background: 'rgba(255,255,255,.1)' }}><div className="h-full rounded-full transition-all duration-300" style={{ width: pct + '%', background: 'linear-gradient(90deg,var(--a1),var(--a2))' }} /></div>
  <div className="text-xs mt-3 opacity-70">{msg}</div></div>
export function LaunchOverlay({ prog, img, t }: { prog: any; img: string; t: (k: string) => string }) {
  if (!prog.stage) return null
  const steps = [0, 1, 2, 3, 4, 5].map((i) => t('ls' + i))
  return <div className="fixed inset-0 z-[80] flex items-center justify-center" style={{ background: 'rgba(2,8,14,.82)', backdropFilter: 'blur(10px)', color: '#e6fbff' }}>
    <div className="relative rounded-3xl overflow-hidden w-[620px] shadow-2xl" style={{ border: '1px solid rgba(255,255,255,.12)' }}>
      <img src={img} className="absolute inset-0 w-full h-full object-cover opacity-30" /><div className="relative p-8"><div className="flex items-center gap-4 mb-6"><div className="pulse"><Gem s={52} /></div>
        <div><div className="text-2xl font-extrabold">{prog.stage}</div><div className="text-xs opacity-60 truncate w-96">{prog.detail}</div></div></div>
      <div className="flex flex-col gap-2 mb-6">{steps.map((s, i) => <div key={i} className="flex items-center gap-3 text-sm" style={{ opacity: i > prog.step ? .35 : 1 }}>
        <span className="w-5 h-5 rounded-full flex items-center justify-center" style={{ background: i < prog.step ? 'var(--a2)' : 'rgba(255,255,255,.1)', color: '#021' }}>{i < prog.step ? <Icon n="check" s={13} /> : i === prog.step ? <span className="spin" /> : null}</span>{s}</div>)}</div>
      <div className="h-2 rounded-full overflow-hidden" style={{ background: 'rgba(255,255,255,.1)' }}><div className="h-full rounded-full transition-all" style={{ width: Math.max(4, prog.pct) + '%', background: 'linear-gradient(90deg,var(--a1),var(--a2))' }} /></div>
      <div className="text-right text-xs opacity-60 mt-1">{prog.pct}%</div></div></div></div>
}
export const UpdateModal = ({ upd, t }: { upd: any; t: (k: string) => string }) => !upd ? null : <div className="fixed inset-0 z-[90] flex items-center justify-center" style={{ background: 'rgba(2,8,14,.88)', backdropFilter: 'blur(12px)', color: '#e6fbff' }}>
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
