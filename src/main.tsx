import { Component, ReactNode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './index.css'
class EB extends Component<{ children: ReactNode }, { err: boolean }> {
  state = { err: false }
  static getDerivedStateFromError() { return { err: true } }
  render() { return this.state.err ? <div style={{ padding: 40, color: '#fff', background: '#03090f', height: '100vh' }}><h2>Something went wrong</h2><button onClick={() => location.reload()} style={{ marginTop: 16, padding: '8px 16px', borderRadius: 12, background: '#0ea5e9' }}>Reload</button></div> : this.props.children }
}
createRoot(document.getElementById('root')!).render(<EB><App /></EB>)
