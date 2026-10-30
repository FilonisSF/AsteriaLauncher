import { useEffect, useState } from 'react'
import { Icon } from './Icons'
import { Dropdown } from './ui'

type CfMap = { id: number; name: string; desc: string; img: string; logo: string; downloads: number; author: string; url: string; cats: string[]; files: { id: number; ver: string; name: string }[]; free: boolean }
type MapItem = { name: string; desc?: string; img?: string; url: string; ver?: string }
const api = (window as any).api
const cmpVer = (a: string, b: string) => { const x = a.split('.').map(Number), y = b.split('.').map(Number); for (let i = 0; i < 3; i++) if ((x[i] || 0) !== (y[i] || 0)) return (x[i] || 0) - (y[i] || 0); return 0 }
/** the file for the chosen version: the same one, else the newest older one (a world opens in newer Minecraft, not in older) */
const fileFor = (m: CfMap, ver: string) => m.files.find((f) => f.ver === ver) || [...m.files].filter((f) => cmpVer(f.ver, ver) <= 0).sort((a, b) => cmpVer(b.ver, a.ver))[0]

/** Maps in the catalog section: search the big CurseForge "Worlds" catalog (through the Asteria server) + maps picked by the admin; one click puts a map into the saves of the chosen version */
export function MapsCatalog({ ver, say, openWorlds }: { ver: string; say: (s: string, k?: 'ok' | 'err' | 'info') => void; openWorlds: () => void }) {
  const [q, setQ] = useState(''), [sort, setSort] = useState('popular'), [onlyVer, setOnlyVer] = useState(false), [page, setPage] = useState(0)
  const [hits, setHits] = useState<CfMap[]>([]), [total, setTotal] = useState(0), [err, setErr] = useState(''), [loading, setLoading] = useState(false), [busy, setBusy] = useState(0)
  const [picked, setPicked] = useState<MapItem[]>([])
  const search = async (p = 0, qq = q) => { setLoading(true); setErr(''); const r = await api.mapsSearch({ q: qq, sort: sort === 'popular' ? '' : sort, page: p, ver: onlyVer ? ver : '' }); setLoading(false)
    if (r.error) { setErr(r.error); if (!p) setHits([]); return } setTotal(r.total); setPage(p); setHits(p ? [...hits, ...r.maps] : r.maps) }
  useEffect(() => { search(0) }, [sort, onlyVer, ver])
  useEffect(() => { api.maps().then(setPicked) }, [])
  const get = async (m: CfMap) => { const f = fileFor(m, ver); if (!f) return say('Эта карта только для более новых версий: ' + m.files.map((x) => x.ver).join(', '), 'err')
    setBusy(m.id); const r = await api.mapCf(m.id, f.id, ver, m.name); setBusy(0)
    if (r.error) { say(r.error, 'err'); if (r.page && m.url) window.open(m.url); return } say('Карта «' + r.name + '» установлена в Minecraft ' + ver + ' — она в «Мирах»', 'ok') }
  const getPicked = async (m: MapItem) => { setBusy(-1); const r = await api.mapGet(m.url, ver, m.name); setBusy(0); if (r.error) return say(r.error, 'err'); say('Карта «' + r.name + '» установлена в Minecraft ' + ver, 'ok') }

  return <div className="flex flex-col gap-3">
    <div className="flex gap-2"><div className="relative flex-1"><input className="w-full !pl-10" placeholder="Поиск карт: паркур, выживание, хоррор, PVP-арена…" value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && search(0)} /><span className="absolute left-3 top-2.5 muted"><Icon n="search" s={18} /></span></div>
      <Dropdown value={sort} set={setSort} w={170} options={[{ value: 'popular', label: 'Популярные' }, { value: 'downloads', label: 'По скачиваниям' }, { value: 'newest', label: 'Новые' }, { value: 'updated', label: 'Обновлённые' }]} /></div>
    <div className="flex gap-1.5 flex-wrap items-center">{['Паркур', 'Выживание', 'Приключения', 'Хоррор', 'PVP', 'Мини-игры', 'Головоломки', 'Город'].map((c) => {
      const en: Record<string, string> = { 'Паркур': 'parkour', 'Выживание': 'survival', 'Приключения': 'adventure', 'Хоррор': 'horror', 'PVP': 'pvp', 'Мини-игры': 'minigame', 'Головоломки': 'puzzle', 'Город': 'city' }
      return <button key={c} className={'pill ' + (q === en[c] ? 'on' : '')} onClick={() => { const v = q === en[c] ? '' : en[c]; setQ(v); search(0, v) }}>{c}</button> })}
      <button className={'pill ' + (onlyVer ? 'on' : '')} onClick={() => setOnlyVer(!onlyVer)} title="Показывать только карты, сделанные для этой версии"><Icon n="check" s={12} />Только для {ver}</button>
      <button className="pill ml-auto" onClick={openWorlds}><Icon n="folder" s={13} />Мои миры</button></div>

    {picked.length > 0 && <div><div className="font-bold mb-2">Выбор SafiMine</div><div className="grid gap-3" style={{ gridTemplateColumns: 'repeat(auto-fill,minmax(240px,1fr))' }}>
      {picked.map((m) => <div key={m.url} className="card rounded-3xl overflow-hidden flex flex-col">{m.img ? <img src={m.img} className="h-28 w-full object-cover" /> : <div className="h-28 flex items-center justify-center" style={{ background: 'var(--chip)' }}><Icon n="cube" s={26} /></div>}
        <div className="p-3 flex flex-col gap-1 flex-1"><b className="truncate">{m.name}</b>{m.desc && <div className="muted text-xs line-clamp-2">{m.desc}</div>}
          <button className="btn btn-primary !py-1 !px-3 text-xs mt-auto self-end flex items-center gap-1" disabled={!!busy} onClick={() => getPicked(m)}><Icon n="download" s={12} />Скачать</button></div></div>)}</div></div>}

    {err === 'nokey' && <div className="card rounded-3xl p-6 text-center"><b className="block mb-1">Большой каталог карт ещё не подключён</b><span className="muted text-sm">Владельцу сервера Asteria нужно добавить ключ CurseForge (переменная CF_API_KEY на Render). Пока можно поставить свою карту из архива во вкладке «Мои миры».</span></div>}
    {err === 'forbidden' && <div className="card rounded-3xl p-6 text-center"><b className="block mb-1">CurseForge пока не разрешает поиск этому ключу</b><span className="muted text-sm">Ключ рабочий, но поиск для него закрыт — в console.curseforge.com проверьте, что заявка на ключ одобрена. Свою карту можно поставить из архива в «Мирах».</span></div>}
    {err === 'old' && <div className="card rounded-3xl p-6 text-center muted text-sm">Каталог карт заработает после обновления сервера Asteria.</div>}
    {err && err !== 'nokey' && err !== 'old' && err !== 'forbidden' && <div className="card rounded-3xl p-6 text-center muted text-sm">Не получилось загрузить карты: {err} <button className="pill ml-2" onClick={() => search(0)}>Ещё раз</button></div>}
    {!err && !loading && !hits.length && <div className="card rounded-3xl p-6 text-center muted text-sm">Ничего не нашлось — попробуйте другое слово (лучше по-английски: parkour, survival, horror)</div>}

    <div className="grid gap-3" style={{ gridTemplateColumns: 'repeat(auto-fill,minmax(260px,1fr))' }}>
      {hits.map((m) => { const f = fileFor(m, ver), vers = [...new Set(m.files.map((x) => x.ver))].sort(cmpVer)
        return <div key={m.id} className="card rounded-3xl overflow-hidden flex flex-col">
          <button className="relative h-36 w-full shrink-0" onClick={() => m.url && window.open(m.url)} title="Страница карты">{m.img ? <img src={m.img} className="h-full w-full object-cover" loading="lazy" /> : <div className="h-full flex items-center justify-center" style={{ background: 'var(--chip)' }}><Icon n="cube" s={26} /></div>}
            {m.logo && m.logo !== m.img && <img src={m.logo} className="absolute left-2 bottom-2 w-10 h-10 rounded-xl object-cover border border-white/30" loading="lazy" />}</button>
          <div className="p-3 flex flex-col gap-1 flex-1"><b className="truncate" title={m.name}>{m.name}</b>
            <div className="muted text-[11px] flex items-center gap-2"><span className="truncate">{m.author}</span><span className="flex items-center gap-1 shrink-0"><Icon n="download" s={11} />{m.downloads?.toLocaleString('ru')}</span></div>
            <div className="muted text-xs line-clamp-2">{m.desc}</div>
            <div className="flex items-center gap-2 mt-auto pt-2"><span className="muted text-[11px] truncate" title={vers.join(', ')}>{vers.length ? (vers.length > 3 ? vers[0] + ' – ' + vers[vers.length - 1] : vers.join(', ')) : 'Версия не указана'}</span>
              {f ? <button className="btn btn-primary !py-1 !px-3 text-xs ml-auto flex items-center gap-1 shrink-0" disabled={!!busy} onClick={() => get(m)}>{busy === m.id ? <><span className="dlspin" />Скачиваем</> : <><Icon n="download" s={12} />В {ver}</>}</button>
                : <span className="text-[11px] ml-auto shrink-0" style={{ color: '#f59e0b' }} title={'Сделана для ' + vers.join(', ')}>Для новее {ver}</span>}</div></div></div> })}</div>
    {hits.length > 0 && hits.length < total && <button className="pill self-center" disabled={loading} onClick={() => search(page + 1)}>{loading ? 'Загружаем…' : 'Показать ещё'}</button>}
    {loading && !hits.length && <div className="muted text-sm text-center py-6"><span className="dlspin" /> Ищем карты…</div>}
    <div className="muted text-[11px]">Карты — из каталога CurseForge, авторы — их создатели. Карта ставится в миры выбранной версии; карты для старых версий открываются и в новых.</div>
  </div>
}
