import { useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'

/** our own question window instead of the grey system confirm(): a dimmed blurred backdrop, Enter = yes, Esc / a click outside = no */
function Ask({ text, sub, ok, danger, done }: { text: string; sub?: string; ok: string; danger: boolean; done: (v: boolean) => void }) {
  const [out, setOut] = useState(false), close = (v: boolean) => { setOut(true); setTimeout(() => done(v), 120) }
  useEffect(() => { const k = (e: KeyboardEvent) => { if (e.key === 'Escape') close(false); if (e.key === 'Enter') close(true) }; addEventListener('keydown', k); return () => removeEventListener('keydown', k) }, [])
  return <div className="ask-bg" style={out ? { opacity: 0, transition: 'opacity .12s' } : undefined} onMouseDown={(e) => e.target === e.currentTarget && close(false)}>
    <div className="ask flex flex-col gap-3 !w-[400px] max-w-[92vw]">
      <b className="text-lg leading-snug">{text}</b>{sub && <p className="muted text-sm -mt-1">{sub}</p>}
      <div className="flex gap-2 justify-end mt-1"><button className="btn" onClick={() => close(false)}>Отмена</button>
        <button className={'btn ' + (danger ? 'btn-danger' : 'btn-primary')} autoFocus onClick={() => close(true)}>{ok}</button></div></div></div>
}
export function askConfirm(text: string, o: { sub?: string; ok?: string; danger?: boolean } = {}): Promise<boolean> {
  return new Promise((res) => { const host = document.createElement('div'); document.body.appendChild(host); const root = createRoot(host)
    root.render(<Ask text={text} sub={o.sub} ok={o.ok || 'Да'} danger={o.danger ?? true} done={(v) => { root.unmount(); host.remove(); res(v) }} />) })
}
