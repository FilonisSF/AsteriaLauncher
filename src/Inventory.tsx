import { useEffect, useState } from 'react'
import { Icon } from './Icons'

/** Inventory: what the player won or got on safimine.ru (roulette, rewards, vouchers, gifts), taken into the game on demand;
 *  the purchases and the balance of the site account. Signed in with the site account (the same e-mail and password) */
const api = (window as any).api
type Item = { id: number; title: string; icon: string | null; rarity: string | null; source: string; status: 'stored' | 'sent'; created_at: string; claimed_at: string | null }
type Order = { id: string; title: string; price: number; status: string; kind: string; created_at: string }
type Inv = { signedIn: boolean; error?: string; email?: string; nick?: string; verified?: boolean; items?: Item[]; orders?: Order[]; balance?: number }
const SITE = 'https://safimine.ru'
const RARITY: Record<string, [string, string]> = { common: ['Обычный', '#94a3b8'], rare: ['Редкий', '#38bdf8'], epic: ['Эпический', '#a855f7'], legendary: ['Легендарный', '#f59e0b'] }
const SOURCE: Record<string, string> = { roulette: 'Рулетка', reward: 'Награда', voucher: 'Промокод', gift: 'Подарок' }
const ORDER: Record<string, [string, string]> = { paid: ['Оплачен', '#3ee08f'], pending: ['Ожидает оплаты', '#f59e0b'], canceled: ['Отменён', '#94a3b8'], refunded: ['Возврат', '#94a3b8'] }
const day = (t: string | null) => t ? new Date(t).toLocaleString('ru', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : ''

function Pic({ src }: { src: string | null }) {
  const [d, setD] = useState<string | null>(null)
  useEffect(() => { let off = false; if (src) api.img((/^https?:/.test(src) ? '' : SITE) + src).then((x: string | null) => { if (!off) setD(x) }).catch(() => {}); return () => { off = true } }, [src])
  return d ? <img src={d} className="w-14 h-14 object-contain" style={{ imageRendering: 'pixelated' }} /> : <Icon n="gift" s={26} />
}

export function Inventory({ say }: { say: (s: string, k?: 'ok' | 'err' | 'info') => void }) {
  const [inv, setInv] = useState<Inv | null>(null), [email, setEmail] = useState(''), [pass, setPass] = useState(''), [show, setShow] = useState(false), [busy, setBusy] = useState(''), [code, setCode] = useState(''), [view, setView] = useState<'items' | 'history' | 'orders'>('items')
  const load = () => api.siteInventory().then(setInv)
  useEffect(() => { load() }, [])
  const login = async () => { if (!email.trim() || !pass) return say('Введите почту и пароль от safimine.ru', 'err'); setBusy('login'); const r = await api.siteLogin(email, pass); setBusy(''); if (r?.error) return say(r.error, 'err'); setPass(''); say('Вы вошли в аккаунт safimine.ru', 'ok'); load() }
  const claim = async (ids: number[]) => { if (!ids.length) return; setBusy(ids.length > 1 ? 'all' : String(ids[0])); const r = await api.siteClaim(ids); setBusy('')
    if (r?.done?.length) say(r.done.length > 1 ? `Забрано: ${r.done.length} — придут в игру, как только вы будете на сервере` : `«${r.done[0]}» придёт в игру, как только вы будете на сервере`, 'ok')
    if (r?.error) say(r.error, 'err'); load() }
  const redeem = async () => { if (!code.trim()) return; setBusy('code'); const r = await api.siteVoucher(code); setBusy(''); if (r?.error) return say(r.error, 'err'); setCode(''); say('Промокод активирован — награда в инвентаре', 'ok'); load() }

  if (!inv) return <div className="card rounded-3xl p-8 text-center muted">Загрузка…</div>
  if (!inv.signedIn) return <div className="max-w-[440px] mx-auto mt-6 card rounded-3xl p-6 flex flex-col gap-3">
    <div className="flex items-center gap-3"><span className="cmd-ic !w-11 !h-11"><Icon n="gift" s={20} /></span><div><b className="text-lg block">Инвентарь SafiMine</b><div className="muted text-xs">Призы рулетки, награды, промокоды и покупки с safimine.ru</div></div></div>
    <input placeholder="Почта аккаунта safimine.ru" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
    <div className="relative"><input className="w-full !pr-11" type={show ? 'text' : 'password'} placeholder="Пароль" value={pass} onChange={(e) => setPass(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && login()} autoComplete="current-password" />
      <button className="absolute right-2 top-1.5 pill !p-1.5" title={show ? 'Скрыть пароль' : 'Показать пароль'} onClick={() => setShow(!show)}><Icon n={show ? 'eyeOff' : 'eye'} s={14} /></button></div>
    <button className="btn btn-primary" disabled={busy === 'login'} onClick={login}>{busy === 'login' ? 'Входим…' : 'Войти'}</button>
    <div className="muted text-[11px]">Нет аккаунта или забыли пароль — <a className="underline cursor-pointer" onClick={() => api.openLink(SITE + '/account')}>safimine.ru/account</a></div></div>

  const items = inv.items || [], stored = items.filter((x) => x.status === 'stored'), sent = items.filter((x) => x.status === 'sent')
  return <div className="flex flex-col gap-3">
    <div className="ph"><div><h1 className="grad-text">Инвентарь</h1><div className="sub">Призы, награды и покупки с safimine.ru — забирайте в игру, когда удобно</div></div>
      <div className="flex gap-2 items-center flex-wrap justify-end"><div className="seg">{([['items', 'Предметы · ' + stored.length, 'gift'], ['history', 'Забранные', 'reset'], ['orders', 'Покупки', 'shop']] as const).map(([k, n, i]) => <button key={k} className={view === k ? 'on' : ''} onClick={() => setView(k)}><Icon n={i} s={13} /> {n}</button>)}</div></div></div>
    <div className="grid gap-3" style={{ gridTemplateColumns: 'repeat(auto-fit,minmax(240px,1fr))' }}>
      <div className="card rounded-3xl p-4 flex items-center gap-3"><Icon n="user" s={18} /><div className="min-w-0 flex-1"><b className="block truncate">{inv.nick || inv.email}</b>
        <div className="muted text-[11px] truncate">{inv.verified ? 'Ник подтверждён — предметы придут в игру' : 'Подтвердите ник в кабинете на сайте, чтобы забирать предметы'}</div></div>
        <button className="pill !text-xs" title="Выйти из аккаунта сайта" onClick={async () => { await api.siteLogout(); load() }}><Icon n="close" s={11} /></button></div>
      <div className="card rounded-3xl p-4 flex items-center gap-3"><Icon n="shop" s={18} /><div className="flex-1"><div className="muted text-[11px]">Баланс на сайте</div><b className="text-lg">{(inv.balance || 0).toLocaleString('ru')} ₽</b></div>
        <button className="pill !text-xs" onClick={() => api.openLink(SITE + '/shop')}>Магазин</button></div>
      <div className="card rounded-3xl p-4 flex items-center gap-2"><input className="flex-1 font-mono uppercase" placeholder="Промокод" value={code} onChange={(e) => setCode(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && redeem()} />
        <button className="btn !py-1.5 !px-3 text-sm" disabled={busy === 'code'} onClick={redeem}>Активировать</button></div></div>
    {inv.error && <div className="card rounded-2xl p-3 text-sm" style={{ color: '#ff8a8a' }}>{inv.error}</div>}

    {view === 'items' && <>
      {stored.length > 1 && <div className="flex justify-end"><button className="btn btn-primary !py-1.5 !px-4 text-sm flex items-center gap-2" disabled={!!busy || !inv.verified} onClick={() => claim(stored.map((x) => x.id))}><Icon n="download" s={13} />{busy === 'all' ? 'Забираем…' : 'Забрать всё · ' + stored.length}</button></div>}
      {!stored.length && <div className="card rounded-3xl p-8 text-center muted">Пока пусто — здесь появятся призы рулетки, награды за подписку и промокоды</div>}
      <div className="grid gap-3" style={{ gridTemplateColumns: 'repeat(auto-fill,minmax(190px,1fr))' }}>
        {stored.map((x) => { const r = RARITY[x.rarity || ''] || RARITY.common; return <div key={x.id} className="card rounded-3xl p-3 flex flex-col items-center gap-2 text-center" style={{ boxShadow: `inset 0 0 0 1px ${r[1]}55` }}>
          <div className="w-20 h-20 rounded-2xl flex items-center justify-center" style={{ background: `radial-gradient(circle at 50% 40%, ${r[1]}44, transparent 70%)` }}><Pic src={x.icon} /></div>
          <b className="text-sm leading-tight line-clamp-2 min-h-[2.5em]">{x.title}</b>
          <div className="text-[10px] font-semibold uppercase tracking-wide" style={{ color: r[1] }}>{r[0]} · {SOURCE[x.source] || x.source}</div>
          <button className="btn btn-primary w-full !py-1.5 text-xs" disabled={!!busy || !inv.verified} onClick={() => claim([x.id])}>{busy === String(x.id) ? 'Забираем…' : 'Забрать в игру'}</button></div> })}</div></>}

    {view === 'history' && <div className="card rounded-3xl p-2">{!sent.length ? <div className="p-6 text-center muted">Вы ещё ничего не забирали</div>
      : sent.map((x) => <div key={x.id} className="flex items-center gap-3 p-2 border-b last:border-0" style={{ borderColor: 'var(--line)' }}><div className="w-10 h-10 flex items-center justify-center shrink-0"><Pic src={x.icon} /></div>
        <div className="min-w-0 flex-1"><b className="text-sm block truncate">{x.title}</b><div className="muted text-[11px]">{SOURCE[x.source] || x.source} · получено {day(x.created_at)}</div></div><div className="muted text-[11px] whitespace-nowrap">забрано {day(x.claimed_at)}</div></div>)}</div>}

    {view === 'orders' && <div className="card rounded-3xl p-2">{!(inv.orders || []).length ? <div className="p-6 text-center muted">Покупок пока нет</div>
      : (inv.orders || []).map((o) => { const st = ORDER[o.status] || [o.status, '#94a3b8']; return <div key={o.id} className="flex items-center gap-3 p-2.5 border-b last:border-0" style={{ borderColor: 'var(--line)' }}><Icon n="shop" s={16} />
        <div className="min-w-0 flex-1"><b className="text-sm block truncate">{o.title}</b><div className="muted text-[11px]">{day(o.created_at)}{o.status === 'paid' ? ' · выдаётся автоматически' : ''}</div></div>
        <b className="text-sm whitespace-nowrap">{(+o.price).toLocaleString('ru')} ₽</b><span className="text-[11px] font-semibold whitespace-nowrap" style={{ color: st[1] }}>{st[0]}</span></div> })}</div>}
  </div>
}
