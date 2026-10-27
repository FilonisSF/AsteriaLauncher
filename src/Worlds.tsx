import { useEffect, useState } from 'react'
import { Icon } from './Icons'
import { Dropdown, Switch } from './ui'

type World = { path: string; name: string; version: string; mtime: number; size: number; icon: string | null }
type MapItem = { name: string; desc?: string; img?: string; url: string; ver?: string }
type Pres = { online: boolean; ver?: string; host?: string; srv?: string }
const api = (window as any).api
const mb = (b: number) => b > 1 << 30 ? (b / (1 << 30)).toFixed(1) + ' ГБ' : Math.max(0.1, b / (1 << 20)).toFixed(1) + ' МБ'
const ago = (t: number) => { const d = (Date.now() - t) / 60000; return d < 60 ? Math.max(1, Math.round(d)) + ' мин назад' : d < 1440 ? Math.round(d / 60) + ' ч назад' : new Date(t).toLocaleDateString('ru') }

/** Worlds: own worlds of every version (play straight into one, copy to another version, export, delete), import, maps catalog, friends and playing together */
export function Worlds({ ver, versions, play, say, running }: { ver: string; versions: { value: string; label: string }[]; play: (v: string, join?: boolean, server?: any, extra?: any) => void; say: (s: string, k?: 'ok' | 'err' | 'info') => void; running: boolean }) {
  const [view, setView] = useState<'mine' | 'maps' | 'friends'>('mine')
  const [worlds, setWorlds] = useState<World[]>([]), [maps, setMaps] = useState<MapItem[]>([]), [target, setTarget] = useState(ver), [busy, setBusy] = useState('')
  const [together, setTogether] = useState(false), [q, setQ] = useState('')
  const load = () => api.worlds().then(setWorlds)
  useEffect(() => { load(); api.maps().then(setMaps) }, [])
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
        <button className="btn !py-2 flex items-center gap-2" onClick={imp}><Icon n="upload" s={15} />Добавить мир (.zip / папка)</button>
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
              <button className="pill !text-xs" title="В корзину" onClick={async () => { if (!confirm('Переместить мир «' + w.name + '» в корзину?')) return; await api.worldRemove(w.path); load() }}><Icon n="trash" s={12} /></button></div></div></div>)}</div>
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
  </div>
}

const Avatar = ({ n, on }: { n: string; on?: boolean }) => <span className="relative shrink-0"><img src={'https://mc-heads.net/avatar/' + n + '/40'} className="w-10 h-10 rounded-xl" style={{ background: 'var(--chip)' }} onError={(e) => { (e.target as HTMLImageElement).style.opacity = '0.2' }} />
  {on !== undefined && <i className="absolute -right-1 -bottom-1 w-3 h-3 rounded-full" style={{ background: on ? '#3ee08f' : '#6b7280', border: '2px solid var(--bg)' }} />}</span>
function Friends({ play, say, running }: { play: (v: string, join?: boolean, server?: any) => void; say: (s: string, k?: 'ok' | 'err' | 'info') => void; running: boolean }) {
  const [d, setD] = useState<{ friends: string[]; incoming: string[]; outgoing: string[]; pres: Record<string, Pres>; host: string } | null>(null), [err, setErr] = useState(''), [nick, setNick] = useState(''), [host, setHost] = useState('')
  const take = (r: any) => { if (r?.error) { setErr(r.error); return false } setErr(''); setD(r); if (r?.host) setHost(r.host); return true }
  const load = () => api.friendsGet().then(take)
  useEffect(() => { load(); const id = setInterval(load, 15000); const off = api.onTogether?.((h: string) => setHost(h)); const off2 = api.onFriendNotes?.(() => load()); return () => { clearInterval(id); off?.(); off2?.() } }, [])
  const act = async (a: string, who: string, ok?: boolean, msg?: string) => { if (take(await api.friendsAct(a, who, ok)) && msg) say(msg) }
  const add = () => { const n = nick.trim(); if (!/^\w{3,16}$/.test(n)) return say('Ник: 3–16 латинских букв, цифр или _', 'err'); setNick(''); act('request', n, false, 'Заявка отправлена: ' + n) }
  const pres = d?.pres || {}, sorted = [...(d?.friends || [])].sort((a, b) => +!!pres[b]?.online - +!!pres[a]?.online)
  return <div className="flex flex-col gap-3">
    {host && <div className="card rounded-3xl p-4 flex items-center gap-3"><Icon n="link" s={18} /><div className="flex-1"><b>Ваш мир открыт для друзей</b><div className="muted text-xs">Адрес: {host} — друзья увидят кнопку «Присоединиться»</div></div>
      <button className="pill" onClick={() => { navigator.clipboard.writeText(host); say('Адрес скопирован') }}><Icon n="copy" s={13} />Копировать</button></div>}
    <div className="card rounded-3xl p-4 flex gap-2 items-center"><input className="flex-1" placeholder="Ник игрока" value={nick} onChange={(e) => setNick(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && add()} />
      <button className="btn !py-2 flex items-center gap-2" onClick={add}><Icon n="plus" s={15} />Отправить заявку</button></div>
    {err && <div className="card rounded-2xl p-3 text-sm" style={{ color: '#ff8a8a' }}>{err}</div>}
    {!!d?.incoming.length && <div className="card rounded-3xl p-4 flex flex-col gap-2"><b className="text-sm">Заявки в друзья · {d.incoming.length}</b>
      {d.incoming.map((n) => <div key={n} className="flex items-center gap-3"><Avatar n={n} /><b className="flex-1 truncate">{n}</b>
        <button className="btn btn-primary !py-1 !px-3 text-xs" onClick={() => act('answer', n, true, n + ' теперь ваш друг')}>Принять</button>
        <button className="pill !text-xs" onClick={() => act('answer', n, false)}>Отклонить</button></div>)}</div>}
    {!!d?.outgoing.length && <div className="card rounded-3xl p-4 flex flex-col gap-2"><b className="text-sm muted">Ожидают ответа</b>
      {d.outgoing.map((n) => <div key={n} className="flex items-center gap-3"><Avatar n={n} /><span className="flex-1 truncate">{n}</span><span className="muted text-[11px]">заявка отправлена</span>
        <button className="pill !text-xs" title="Отменить заявку" onClick={() => act('remove', n)}><Icon n="close" s={11} /></button></div>)}</div>}
    {d && !d.friends.length && !d.incoming.length && <div className="card rounded-3xl p-8 text-center muted">Отправьте заявку по нику — когда её примут, увидите, кто в игре, на какой версии, и сможете зайти в мир друга одной кнопкой</div>}
    <div className="grid gap-2" style={{ gridTemplateColumns: 'repeat(auto-fill,minmax(340px,1fr))' }}>
      {sorted.map((n) => { const p = pres[n] || { online: false }; return <div key={n} className="card rounded-2xl p-3 flex items-center gap-3"><Avatar n={n} on={p.online} />
        <div className="flex-1 min-w-0"><b className="block truncate">{n}</b><div className="muted text-[11px]">{!p.online ? 'Не в игре' : p.host ? 'Открыл мир · ' + p.ver : p.srv ? 'На сервере ' + p.srv + ' · ' + p.ver : 'В игре · ' + (p.ver || '')}</div></div>
        {p.online && p.host && p.ver && <button className="btn !py-1 !px-3 text-xs" disabled={running} onClick={() => play(p.ver!, true, { host: p.host, port: 25565 })}>Присоединиться</button>}
        {p.online && !p.host && p.srv && p.ver && <button className="pill !text-xs" disabled={running} onClick={() => play(p.ver!, true, { host: p.srv, port: 25565 })}>К нему</button>}
        <button className="pill !text-xs" title="Удалить из друзей" onClick={() => confirm('Удалить ' + n + ' из друзей?') && act('remove', n)}><Icon n="close" s={11} /></button></div> })}</div>
    <div className="muted text-[11px]">Статус виден, когда друг играет через AsteriaLauncher. Игра вместе: включите «Играть вместе с друзьями» во вкладке «Мои миры», зайдите в мир и нажмите ESC → «Открыть для сети».</div></div>
}
