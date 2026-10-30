import { useState } from 'react'
import { Icon } from './Icons'

const api = (window as any).api
type Say = (s: string, k?: 'ok' | 'err' | 'info') => void
/** what the window keeps itself (skin and cape libraries, chosen language, page settings) goes into the backup too */
const localDump = () => { const o: Record<string, string> = {}; try { for (let i = 0; i < localStorage.length; i++) { const k = localStorage.key(i)!; if (!k.startsWith('tr:')) o[k] = localStorage.getItem(k) || '' } } catch {} return o }

/** one code (or a file) with everything of the launcher: to move to another computer, or to give a friend your setup */
export function BackupCard({ say }: { say: Say }) {
  const [mode, setMode] = useState<'own' | 'friend'>('own'), [code, setCode] = useState(''), [made, setMade] = useState(''), [busy, setBusy] = useState(false)
  const make = async () => { setBusy(true); const r = await api.backupCode(mode, mode === 'own' ? localDump() : Object.fromEntries(Object.entries(localDump()).filter(([k]) => /^(mySkins|myCapes|lang|theme)/.test(k)))); setBusy(false)
    if (r?.error) return say(r.error, 'err'); setMade(r.code); navigator.clipboard.writeText(r.code); say('Код скопирован') }
  const file = async () => { const r = await api.backupFile(mode, localDump()); if (r?.file) say('Сохранено в файл') }
  const restore = async (fromFile: boolean) => { setBusy(true); const r = await api.backupRestore(fromFile ? '' : code); setBusy(false); if (!r) return; if (r.error) return say(r.error, 'err')
    try { for (const [k, v] of Object.entries(r.local || {})) localStorage.setItem(k, v as string) } catch {}
    say('Всё восстановлено — лаунчер перезагрузится'); setTimeout(() => location.reload(), 1200) }
  return <div className="card rounded-3xl p-5 flex flex-col gap-3">
    <div className="font-bold flex items-center gap-2"><Icon n="share" s={18} />Сохранение и перенос</div>
    <div className="muted text-sm">Всё, что есть в лаунчере, одним кодом: скины, плащи, визуалы, пресеты, бинды, метки, настройки. Введите код на новом компьютере — и всё вернётся.</div>
    <div className="seg self-start"><button className={mode === 'own' ? 'on' : ''} onClick={() => { setMode('own'); setMade('') }}>Для себя</button><button className={mode === 'friend' ? 'on' : ''} onClick={() => { setMode('friend'); setMade('') }}>Для друга</button></div>
    <div className="muted text-[11px]">{mode === 'own' ? 'С офлайн-аккаунтами и вашими никами. Вход Microsoft не копируется — войдите заново. Никому не отдавайте этот код.' : 'Только оформление: визуалы, пресеты, бинды, метки, скины и плащи. Без аккаунтов — можно смело отправить другу.'}</div>
    <div className="flex gap-2 flex-wrap"><button className="btn-add" disabled={busy} onClick={make}><span className="ic"><Icon n="link" s={13} /></span>Создать код</button><button className="btn flex items-center gap-2" onClick={file}><Icon n="download" s={14} />В файл</button></div>
    {made && <div className="mcard !p-3 flex items-center gap-3"><span className="font-mono text-lg font-bold tracking-widest flex-1">{made}</span><button className="pill" onClick={() => { navigator.clipboard.writeText(made); say('Код скопирован') }}><Icon n="copy" s={13} />Копировать</button></div>}
    <div className="flex gap-2 pt-2" style={{ borderTop: '1px solid var(--line)' }}><input className="flex-1 min-w-0 font-mono tracking-widest uppercase" maxLength={11} placeholder="Код: XXXXX-XXXXX" value={code} onChange={(e) => setCode(e.target.value)} />
      <button className="btn shrink-0" disabled={busy || code.replace(/[^A-Za-z0-9]/g, '').length !== 10} onClick={() => restore(false)}>Восстановить</button><button className="btn shrink-0" disabled={busy} title="Из файла" onClick={() => restore(true)}><Icon n="folder" s={14} /></button></div>
  </div>
}

/** remove: everything (data and the app), only the data (the launcher starts clean), only the app (data stays) */
export function UninstallCard() {
  const [ask, setAsk] = useState<'' | 'all' | 'data' | 'app'>(''), [word, setWord] = useState(''), [err, setErr] = useState('')
  const T = { all: ['Удалить всё', 'Лаунчер, все версии Minecraft, миры, моды, скины, настройки — всё будет удалено с компьютера. Вернуть нельзя.'],
    data: ['Удалить данные лаунчера', 'Все версии, миры, моды, скины и настройки удалятся, лаунчер откроется как новый. Само приложение останется.'],
    app: ['Удалить только приложение', 'Удалится только программа. Миры, версии, скины и настройки останутся — после установки всё будет на месте.'] } as const
  const go = async () => { setErr(''); const r = await api.uninstall(ask); if (r?.error) setErr(r.error) }
  return <div className="card rounded-3xl p-5 flex flex-col gap-3">
    <div className="font-bold flex items-center gap-2"><Icon n="trash" s={18} />Удаление</div>
    <div className="flex flex-col gap-2">{(['app', 'data', 'all'] as const).map((k) => <button key={k} className={'mcard !p-3 text-left flex items-center gap-3 ' + (k === 'all' ? 'hover:!border-red-400' : '')} onClick={() => { setAsk(k); setWord('') }}>
      <Icon n={k === 'app' ? 'min' : k === 'data' ? 'reset' : 'trash'} s={16} /><div><b className="text-sm block" style={k === 'all' ? { color: '#ff8a8a' } : undefined}>{T[k][0]}</b><span className="muted text-[11px]">{T[k][1]}</span></div></button>)}</div>
    {ask && <div className="ask-bg" onClick={() => setAsk('')}><div className="ask" onClick={(e) => e.stopPropagation()}><b className="block text-lg">{T[ask][0]}?</b><p className="muted text-sm mt-2">{T[ask][1]}</p>
      {ask !== 'app' && <><p className="text-sm mt-3">Чтобы подтвердить, напишите <b>УДАЛИТЬ</b></p><input className="w-full mt-1" value={word} onChange={(e) => setWord(e.target.value)} autoFocus /></>}
      {err && <p className="text-sm mt-2" style={{ color: '#ff8a8a' }}>{err}</p>}
      <div className="flex gap-2 mt-4 justify-end"><button className="btn" onClick={() => setAsk('')}>Отмена</button><button className="btn-danger" disabled={ask !== 'app' && word.trim().toUpperCase() !== 'УДАЛИТЬ'} onClick={go}><Icon n="trash" s={14} />{T[ask][0]}</button></div></div></div>}
  </div>
}
