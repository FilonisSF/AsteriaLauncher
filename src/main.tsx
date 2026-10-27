import { Component, ReactNode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import '@fontsource-variable/manrope'
import '@fontsource-variable/unbounded'
import './index.css'

/** a crash in the UI shows what happened and how to get out of it, instead of a blank page */
class EB extends Component<{ children: ReactNode }, { err: Error | null; copied: boolean }> {
  state = { err: null as Error | null, copied: false }
  static getDerivedStateFromError(err: Error) { return { err } }
  componentDidCatch(err: Error, info: { componentStack?: string }) { (this as any).stack = (err?.stack || String(err)) + (info?.componentStack || '') }
  render() {
    const { err, copied } = this.state
    if (!err) return this.props.children
    const details = (this as any).stack || err.stack || String(err)
    const btn = (bg: string, fg: string) => ({ padding: '11px 18px', borderRadius: 999, border: 0, background: bg, color: fg, font: '700 14px "Manrope Variable",system-ui', cursor: 'pointer' })
    return <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'radial-gradient(ellipse at 70% 0%,rgba(34,211,238,.16),transparent 60%),#02070a', color: '#e6fbff', font: '14px "Manrope Variable",system-ui', padding: 24 }}>
      <div style={{ width: 'min(560px,100%)', padding: 28, borderRadius: 26, background: 'rgba(10,30,36,.6)', border: '1px solid rgba(140,255,240,.12)', boxShadow: '0 30px 80px rgba(0,0,0,.5)' }}>
        <div style={{ width: 52, height: 52, borderRadius: 16, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(135deg,#22d3ee,#0ea5e9)', color: '#032a2e', fontSize: 26, fontWeight: 900, marginBottom: 16 }}>!</div>
        <div style={{ font: '800 22px "Unbounded Variable",system-ui', marginBottom: 6 }}>Лаунчер споткнулся</div>
        <div style={{ color: '#86aab0', lineHeight: 1.5, marginBottom: 14 }}>Часть интерфейса упала с ошибкой. Игра и ваши файлы в порядке — обычно помогает перезапуск. Если ошибка повторяется, скопируйте её и отправьте разработчику.</div>
        <div style={{ font: '12px ui-monospace,Consolas,monospace', color: '#fca5a5', background: 'rgba(0,0,0,.35)', borderRadius: 14, padding: 12, marginBottom: 16, maxHeight: 120, overflow: 'auto', whiteSpace: 'pre-wrap', userSelect: 'text' }}>{String(err.message || err)}</div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <button style={btn('linear-gradient(135deg,#22d3ee,#0ea5e9)', '#032a2e')} onClick={() => location.reload()}>Перезапустить</button>
          <button style={btn('rgba(34,211,238,.12)', '#e6fbff')} onClick={() => { navigator.clipboard?.writeText(details); this.setState({ copied: true }) }}>{copied ? 'Скопировано' : 'Скопировать ошибку'}</button>
          <button style={btn('transparent', '#86aab0')} title="Сбрасывает только вид лаунчера (тема, вкладки), аккаунты и игра не трогаются"
            onClick={() => { try { localStorage.clear() } catch {} location.reload() }}>Сбросить настройки интерфейса</button>
        </div>
      </div>
    </div>
  }
}
createRoot(document.getElementById('root')!).render(<EB><App /></EB>)
