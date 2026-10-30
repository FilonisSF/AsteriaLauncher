import { useEffect, useRef, useState } from 'react'
import { Icon } from './Icons'
import { Dropdown, Switch } from './ui'

type World = { path: string; name: string; version: string; mtime: number; size: number; icon: string | null }
type MapItem = { name: string; desc?: string; img?: string; url: string; ver?: string }
type Pres = { online: boolean; ver?: string; host?: string; srv?: string }
const api = (window as any).api
const mb = (b: number) => b > 1 << 30 ? (b / (1 << 30)).toFixed(1) + ' ГБ' : Math.max(0.1, b / (1 << 20)).toFixed(1) + ' МБ'
const ago = (t: number) => { const d = (Date.now() - t) / 60000; return d < 60 ? Math.max(1, Math.round(d)) + ' мин назад' : d < 1440 ? Math.round(d / 60) + ' ч назад' : new Date(t).toLocaleDateString('ru') }

/** Worlds: own worlds of every version (play straight into one, copy to another version, export, delete), import, maps catalog, friends and playing together */
export function Worlds({ ver, versions, play, say, running, initial }: { initial?: 'mine' | 'maps' | 'friends'; ver: string; versions: { value: string; label: string }[]; play: (v: string, join?: boolean, server?: any, extra?: any) => void; say: (s: string, k?: 'ok' | 'err' | 'info') => void; running: boolean }) {
  const [view, setView] = useState<'mine' | 'maps' | 'friends'>(initial || 'mine')
  const [worlds, setWorlds] = useState<World[]>([]), [maps, setMaps] = useState<MapItem[]>([]), [target, setTarget] = useState(ver), [busy, setBusy] = useState('')
  const [together, setTogether] = useState(false), [q, setQ] = useState(''), [ask, setAsk] = useState<World | null>(null)
  const load = () => api.worlds().then(setWorlds)
  useEffect(() => { load(); api.maps().then(setMaps); return api.onFiles?.((d: any) => { if (d.kind === 'saves') load() }) }, []) // worlds made or deleted in the game appear at once
  const instKey = (v: string) => v // instances are named after the version
  const shown = worlds.filter((w) => !q || w.name.toLowerCase().includes(q.toLowerCase()))
  const imp = async () => { const r = await api.worldImport(target); if (!r) return; if (r.error) return say(r.error, 'err'); say('Мир «' + r.name + '» добавлен в ' + target); load() }
  const getMap = async (m: MapItem) => { setBusy(m.url); const v = m.ver && versions.some((x) => x.value === m.ver) ? m.ver : target; const r = await api.mapGet(m.url, v, m.name); setBusy('')
    if (r.error) return say(r.error, 'err'); say('Карта «' + r.name + '» установлена в ' + v); load(); setView('mine') }
  const tabs: [typeof view, string, string][] = [['mine', 'Мои миры', 'folder'], ['maps', 'Карты', 'download'], ['friends', 'Друзья', 'user']]
  return <div>
    <div className="ph"><div><h1 className="grad-text">Миры</h1><div className="sub">Свои миры всех версий, карты и игра вместе с друзьями</div></div>
      <div className="flex gap-2 items-center flex-wrap justify-end"><Dropdown value={target} set={setTarget} options={versions} w={220} /><div className="seg">{tabs.map(([k, n, i]) => <button key={k} className={(view === k ? "on " : "") + "whitespace-nowrap"} onClick={() => setView(k)}><Icon n={i} s={13} /> {n}</button>)}</div></div></div>

    {view === 'mine' && <div className="flex flex-col gap-3">
      <div className="card rounded-3xl p-4 flex flex-wrap items-center gap-2">
        <div className="relative w-64"><input className="w-full !pl-10" placeholder="Поиск мира" value={q} onChange={(e) => setQ(e.target.value)} /><span className="absolute left-3 top-2.5 muted"><Icon n="search" s={18} /></span></div>
        <button className="btn-add" onClick={imp}><span className="ic"><Icon n="plus" s={14} /></span>Добавить мир</button>
        <button className="pill" onClick={() => api.worldOpenSaves(instKey(target))}><Icon n="folder" s={13} />Папка миров {target}</button>
        <div className="ml-auto w-[330px]"><Switch on={together} set={setTogether} label="Играть вместе с друзьями" hint="Ставит мод e4mc. В игре: ESC → «Открыть для сети»" /></div></div>
      {!shown.length && <div className="card rounded-3xl p-8 text-center muted">Миров пока нет — создайте мир в игре, добавьте свой архив или скачайте карту</div>}
      <div className="grid gap-3" style={{ gridTemplateColumns: 'repeat(auto-fill,minmax(300px,1fr))' }}>
        {shown.map((w) => <div key={w.path} className="card rounded-3xl p-3 flex gap-3 items-center">
          <span className="w-16 h-16 rounded-2xl overflow-hidden shrink-0 flex items-center justify-center" style={{ background: 'var(--chip)' }}>{w.icon ? <img src={w.icon} className="w-full h-full object-cover" style={{ imageRendering: 'auto' }} /> : <Icon n="cube" s={24} />}</span>
          <div className="flex-1 min-w-0"><b className="block truncate">{w.name}</b><div className="muted text-[11px]">Minecraft {w.version} · {mb(w.size)} · {ago(w.mtime)}</div>
            <div className="flex flex-wrap gap-1 mt-2">
              <button className="btn !py-1 !px-3 text-xs flex items-center gap-1" disabled={running} onClick={() => play(w.version, false, undefined, { world: w.name, together })}><Icon n="play" s={12} />Играть</button>
              <button className="pill !text-xs" title="Открыть папку" onClick={() => api.worldOpen(w.path)}><Icon n="folder" s={12} /></button>
              <button className="pill !text-xs" title="Скопировать в выбранную версию" disabled={w.version === target} onClick={async () => { await api.worldCopy(w.path, target); say('Скопировано в ' + target); load() }}><Icon n="copy" s={12} />{target}</button>
              <button className="pill !text-xs" title="Экспорт в .zip" onClick={() => api.worldExport(w.path)}><Icon n="download" s={12} /></button>
              <button className="pill !text-xs" title="Удалить мир" onClick={() => setAsk(w)}><Icon n="trash" s={12} /></button></div></div></div>)}</div>
      <div className="muted text-[11px]">На 1.20 и новее «Играть» сразу открывает мир; на старых версиях откроется меню, мир будет первым в списке одиночной игры.</div></div>}

    {view === 'maps' && <div className="flex flex-col gap-3">
      {!maps.length && <div className="card rounded-3xl p-8 text-center muted">Каталог карт пока пуст — его заполняет администратор в админке (раздел «maps» конфига). Свою карту можно добавить во вкладке «Мои миры».</div>}
      <div className="grid gap-3" style={{ gridTemplateColumns: 'repeat(auto-fill,minmax(260px,1fr))' }}>
        {maps.map((m) => <div key={m.url} className="card rounded-3xl overflow-hidden flex flex-col">
          {m.img ? <img src={m.img} className="h-32 w-full object-cover" /> : <div className="h-32 flex items-center justify-center" style={{ background: 'var(--chip)' }}><Icon n="swords" s={28} /></div>}
          <div className="p-3 flex flex-col gap-1 flex-1"><b>{m.name}</b>{m.desc && <div className="muted text-xs">{m.desc}</div>}
            <div className="flex items-center gap-2 mt-auto pt-2"><span className="muted text-[11px]">{m.ver ? 'Для ' + m.ver : 'Любая версия'}</span>
              <button className="btn !py-1 !px-3 text-xs ml-auto flex items-center gap-1" disabled={!!busy} onClick={() => getMap(m)}><Icon n="download" s={12} />{busy === m.url ? 'Скачивание…' : 'Скачать'}</button></div></div></div>)}</div></div>}

    {view === 'friends' && <Friends play={play} say={say} running={running} />}
    {ask && <div className="ask-bg" onClick={() => setAsk(null)}><div className="ask" onClick={(e) => e.stopPropagation()}>
      <div className="flex items-center gap-3"><span className="w-14 h-14 rounded-2xl overflow-hidden shrink-0 flex items-center justify-center" style={{ background: 'var(--chip)' }}>{ask.icon ? <img src={ask.icon} className="w-full h-full object-cover" /> : <Icon n="cube" s={22} />}</span>
        <div className="min-w-0"><b className="block text-lg truncate">Удалить «{ask.name}»?</b><div className="muted text-xs">Minecraft {ask.version} · {mb(ask.size)}</div></div></div>
      <p className="muted text-sm mt-3">Мир переместится в корзину Windows — его можно будет восстановить.</p>
      <div className="flex gap-2 mt-4 justify-end"><button className="btn" onClick={() => setAsk(null)}>Отмена</button>
        <button className="btn-danger" onClick={async () => { const w = ask; setAsk(null); await api.worldRemove(w.path); say('Мир «' + w.name + '» в корзине'); load() }}><Icon n="trash" s={14} />Удалить</button></div></div></div>}
  </div>
}

const Avatar = ({ n, on }: { n: string; on?: boolean }) => <span className="relative shrink-0"><img src={'https://mc-heads.net/avatar/' + n + '/40'} className="w-10 h-10 rounded-xl" style={{ background: 'var(--chip)' }} onError={(e) => { (e.target as HTMLImageElement).style.opacity = '0.2' }} />
  {on !== undefined && <i className="absolute -right-1 -bottom-1 w-3 h-3 rounded-full" style={{ background: on ? '#3ee08f' : '#6b7280', border: '2px solid var(--bg)' }} />}</span>
/** a chat with one friend: the last 300 messages are kept on the Asteria server; new ones come every 2.5 s while it is open */
function Chat({ who, close, say }: { who: string; close: () => void; say: (s: string, k?: 'ok' | 'err' | 'info') => void }) {
  const [msgs, setMsgs] = useState<{ f: string; x: string; t: number }[]>([]), [text, setText] = useState(''), [sending, setSending] = useState(false), box = useRef<HTMLDivElement>(null), last = useRef(0)
  const pull = async () => { const r = await api.friendChat(who, last.current); if (r?.error) return; if (r?.msgs?.length) { last.current = r.msgs[r.msgs.length - 1].t; setMsgs((m) => [...m, ...r.msgs.filter((x: any) => !m.some((y) => y.t === x.t && y.f === x.f))]) } }
  useEffect(() => { (window as any).__chatWith = who; last.current = 0; setMsgs([]); pull(); const id = setInterval(pull, 2500); return () => { clearInterval(id); (window as any).__chatWith = '' } }, [who])
  useEffect(() => { box.current?.scrollTo({ top: 1e9, behavior: 'smooth' }) }, [msgs.length])
  const send = async () => { const t = text.trim(); if (!t || sending) return; setSending(true); const r = await api.friendMsg(who, t); setSending(false); if (r?.error) return say(r.error, 'err'); setText(''); pull() }
  const day = (t: number) => new Date(t).toLocaleTimeString('ru', { hour: '2-digit', minute: '2-digit' })
  return <div className="ask-bg" onClick={close}><div className="ask !p-0 flex flex-col overflow-hidden" style={{ width: 460, height: 560 }} onClick={(e) => e.stopPropagation()}>
    <div className="flex items-center gap-3 p-4 border-b" style={{ borderColor: 'var(--line, rgba(127,127,127,.2))' }}><Avatar n={who} /><b className="flex-1 truncate">{who}</b><button className="iconbtn" onClick={close}><Icon n="close" s={16} /></button></div>
    <div ref={box} className="flex-1 overflow-y-auto p-4 flex flex-col gap-1.5">
      {!msgs.length && <div className="muted text-sm text-center my-auto">Сообщений пока нет — напишите первым</div>}
      {msgs.map((m, i) => { const mine = m.f.toLowerCase() !== who.toLowerCase(); return <div key={m.t + '-' + i} className={'max-w-[78%] rounded-2xl px-3 py-2 text-sm break-words ' + (mine ? 'self-end text-white' : 'self-start')} style={{ background: mine ? 'linear-gradient(135deg,var(--a1),var(--a2))' : 'var(--chip)' }}>
        {m.x}<span className={'block text-[10px] mt-0.5 ' + (mine ? 'text-white/70 text-right' : 'muted')}>{day(m.t)}</span></div> })}</div>
    <div className="p-3 flex gap-2 border-t" style={{ borderColor: 'var(--line, rgba(127,127,127,.2))' }}><input className="flex-1" autoFocus maxLength={500} placeholder={'Сообщение для ' + who} value={text} onChange={(e) => setText(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && send()} />
      <button className="btn btn-primary" disabled={!text.trim() || sending} onClick={send}>Отправить</button></div></div></div>
}
function Friends({ play, say, running }: { play: (v: string, join?: boolean, server?: any) => void; say: (s: string, k?: 'ok' | 'err' | 'info') => void; running: boolean }) {
  const [d, setD] = useState<{ code?: string; friends: string[]; incoming: string[]; outgoing: string[]; pres: Record<string, Pres>; host: string } | null>(null), [err, setErr] = useState(''), [nick, setNick] = useState(''), [host, setHost] = useState('')
  const take = (r: any) => { if (r?.error) { setErr(r.error); return false } setErr(''); setD(r); if (r?.host) setHost(r.host); return true }
  const load = () => api.friendsGet().then(take)
  useEffect(() => { load(); const id = setInterval(load, 15000); const off = api.onTogether?.((h: string) => setHost(h)); const off2 = api.onFriendNotes?.(() => load()); return () => { clearInterval(id); off?.(); off2?.() } }, [])
  const act = async (a: string, who: string, ok?: boolean, msg?: string) => { if (take(await api.friendsAct(a, who, ok)) && msg) say(msg) }
  const add = () => { const c = nick.trim().toUpperCase().replace(/[^A-Z0-9]/g, ''); if (c.length !== 8) return say('Код дружбы — 8 символов, например K7Q2-XD4M', 'err'); setNick(''); act('request', c, false, 'Заявка отправлена') }
  const pres = d?.pres || {}, sorted = [...(d?.friends || [])].sort((a, b) => +!!pres[b]?.online - +!!pres[a]?.online)
  const [chat, setChat] = useState(''), [unread, setUnread] = useState<Record<string, number>>({}), [inv, setInv] = useState(''), [where, setWhere] = useState<{ world: boolean; here: string; ver: string }>({ world: false, here: '', ver: '' })
  useEffect(() => api.onFriendNotes?.((l: any[]) => { const add: Record<string, number> = {}; for (const x of l) if (x.k === 'msg' && x.who.toLowerCase() !== (chat || '').toLowerCase()) add[x.who] = (add[x.who] || 0) + 1; if (Object.keys(add).length) setUnread((u) => { const n = { ...u }; for (const k in add) n[k] = (n[k] || 0) + add[k]; return n }) }), [chat])
  const invite = async (who: string, kind: string) => { setInv(''); const r = await api.friendInvite(who, kind); if (r?.error) return say(r.error, 'err'); say('Приглашение отправлено: ' + who, 'ok') }
  return <div className="flex flex-col gap-3">
    {host && <div className="card rounded-3xl p-4 flex items-center gap-3"><Icon n="link" s={18} /><div className="flex-1"><b>Ваш мир открыт для друзей</b><div className="muted text-xs">Адрес: {host} — друзья увидят кнопку «Присоединиться»</div></div>
      <button className="pill" onClick={() => { navigator.clipboard.writeText(host); say('Адрес скопирован') }}><Icon n="copy" s={13} />Копировать</button></div>}
    {d?.code && <div className="card rounded-3xl p-4 flex items-center gap-3"><Icon n="user" s={18} /><div className="flex-1"><b>Ваш код дружбы</b><div className="muted text-xs">Дайте его другу — по нику добавить нельзя, только по коду</div></div>
      <span className="font-mono text-lg font-bold tracking-widest">{d.code}</span><button className="pill" onClick={() => { navigator.clipboard.writeText(d.code!); say('Код скопирован') }}><Icon n="copy" s={13} />Копировать</button></div>}
    <div className="card rounded-3xl p-4 flex gap-2 items-center"><input className="flex-1 font-mono tracking-widest uppercase" maxLength={9} placeholder="Код дружбы друга: XXXX-XXXX" value={nick} onChange={(e) => setNick(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && add()} />
      <button className="btn-add" onClick={add}><span className="ic"><Icon n="plus" s={14} /></span>Отправить заявку</button></div>
    {err && <div className="card rounded-2xl p-3 text-sm" style={{ color: '#ff8a8a' }}>{err}</div>}
    {!!d?.incoming.length && <div className="card rounded-3xl p-4 flex flex-col gap-2"><b className="text-sm">Заявки в друзья · {d.incoming.length}</b>
      {d.incoming.map((n) => <div key={n} className="flex items-center gap-3"><Avatar n={n} /><b className="flex-1 truncate">{n}</b>
        <button className="btn btn-primary !py-1 !px-3 text-xs" onClick={() => act('answer', n, true, n + ' теперь ваш друг')}>Принять</button>
        <button className="pill !text-xs" onClick={() => act('answer', n, false)}>Отклонить</button></div>)}</div>}
    {!!d?.outgoing.length && <div className="card rounded-3xl p-4 flex flex-col gap-2"><b className="text-sm muted">Ожидают ответа</b>
      {d.outgoing.map((n) => <div key={n} className="flex items-center gap-3"><Avatar n={n} /><span className="flex-1 truncate">{n}</span><span className="muted text-[11px]">заявка отправлена</span>
        <button className="pill !text-xs" title="Отменить заявку" onClick={() => act('remove', n)}><Icon n="close" s={11} /></button></div>)}</div>}
    {d && !d.friends.length && !d.incoming.length && <div className="card rounded-3xl p-8 text-center muted">Отправьте заявку по коду дружбы — когда её примут, увидите, кто в игре, на какой версии, и сможете зайти в мир друга одной кнопкой</div>}
    <div className="grid gap-2" style={{ gridTemplateColumns: 'repeat(auto-fill,minmax(340px,1fr))' }}>
      {sorted.map((n) => { const p = pres[n] || { online: false }; return <div key={n} className="card rounded-2xl p-3 flex items-center gap-3"><Avatar n={n} on={p.online} />
        <div className="flex-1 min-w-0"><b className="block truncate">{n}</b><div className="muted text-[11px]">{!p.online ? 'Не в игре' : p.host ? 'Открыл мир · ' + p.ver : p.srv ? 'На сервере ' + p.srv + ' · ' + p.ver : 'В игре · ' + (p.ver || '')}</div></div>
        {p.online && p.host && p.ver && <button className="btn !py-1 !px-3 text-xs" disabled={running} onClick={() => play(p.ver!, true, { host: p.host, port: 25565 })}>Присоединиться</button>}
        {p.online && !p.host && p.srv && p.ver && <button className="pill !text-xs" disabled={running} onClick={() => play(p.ver!, true, { host: p.srv, port: 25565 })}>К нему</button>}
        <button className="pill !text-xs relative" title="Написать" onClick={() => { setChat(n); setUnread((u) => ({ ...u, [n]: 0 })) }}><Icon n="chat" s={12} />Чат{!!unread[n] && <span className="absolute -top-1.5 -right-1.5 min-w-[18px] h-[18px] rounded-full text-[10px] font-bold text-white flex items-center justify-center" style={{ background: '#ef4444' }}>{unread[n]}</span>}</button>
        <div className="relative"><button className="pill !text-xs" title="Позвать к себе" onClick={async () => { setWhere(await api.friendWhere()); setInv(inv === n ? '' : n) }}><Icon n="user" s={12} />Позвать</button>
          {inv === n && <><div className="fixed inset-0 z-40" onClick={() => setInv('')} /><div className="pop z-50 right-0" style={{ width: 250 }}><div className="muted text-xs px-2 pb-1">Позвать {n}</div>
            <button className="target w-full text-left" disabled={!where.world} onClick={() => invite(n, 'world')}><b className="text-sm">В мой мир</b><span className="muted text-[11px] block">{where.world ? 'Мир открыт для друзей' : 'Сначала ESC → «Открыть для сети»'}</span></button>
            <button className="target w-full text-left" disabled={!where.here} onClick={() => invite(n, 'here')}><b className="text-sm">На мой сервер</b><span className="muted text-[11px] block">{where.here || 'Зайдите на сервер через лаунчер'}</span></button>
            <button className="target w-full text-left" onClick={() => invite(n, 'safimine')}><b className="text-sm">На SafiMine</b><span className="muted text-[11px] block">mc.safimine.ru</span></button></div></>}</div>
        <button className="pill !text-xs" title="Удалить из друзей" onClick={() => act('remove', n, false, n + ' удалён из друзей')}><Icon n="close" s={11} /></button></div> })}</div>
    {chat && <Chat who={chat} close={() => setChat('')} say={say} />}
    <div className="muted text-[11px]">Статус виден, когда друг играет через AsteriaLauncher. Игра вместе: включите «Играть вместе с друзьями» во вкладке «Мои миры», зайдите в мир и нажмите ESC → «Открыть для сети».</div></div>
}
