import { ReactNode } from 'react'
import { Gem, Icon } from './Icons'
import { Dropdown, LoaderIcon, Tilt } from './ui'
import { VINFO } from './versionInfo'

// Version families like in Lunar: one card per major version, the exact release is picked in the card.
export const FAMILIES: { id: string; subs: string[] }[] = [
  { id: '26', subs: ['26.1'] }, { id: '1.21', subs: ['1.21.11', '1.21.4'] }, { id: '1.20', subs: ['1.20.1'] }, { id: '1.19', subs: ['1.19.4'] }, { id: '1.18', subs: ['1.18.2'] },
  { id: '1.17', subs: ['1.17.1'] }, { id: '1.16', subs: ['1.16.5'] }, { id: '1.12', subs: ['1.12.2'] }, { id: '1.8', subs: ['1.8.9'] },
]
export const ALL_VERSIONS = FAMILIES.flatMap((f) => f.subs)
/** what our own mod offers per version: full = star/skins/capes + PREMIUM visuals, core = star/skins/capes */
export const ASTERIA: Record<string, 'full' | 'core'> = { '1.16.5': 'full', '1.20.1': 'full', '1.21.4': 'full', '1.21.11': 'core' }
export const fabricFor = (v: string) => /^(1\.(1[4-9]|2\d)|2\d\.)/.test(v)
export type Loader = 'fabric' | 'vanilla'
export const loaderOf = (cfg: any, v: string): Loader => !fabricFor(v) ? 'vanilla' : (cfg?.loaders?.[v] ?? (cfg?.fabric === false ? 'vanilla' : 'fabric'))

type P = { ver: string; setVer: (v: string) => void; cfg: any; setCfg: (c: any) => void; running: boolean; play: () => void; VImg: (p: { v: string; cls: string }) => JSX.Element; profiles: ReactNode; recommended: string }
export default function Versions({ ver, setVer, cfg, setCfg, running, play, VImg, profiles, recommended, label }: P & { label?: string }) {
  const sel = FAMILIES.find((f) => f.subs.includes(ver)) ?? FAMILIES[0], info = VINFO[ver], ld = loaderOf(cfg, ver), ast = ASTERIA[ver]
  const setLoader = (v: string, l: Loader) => cfg && setCfg({ ...cfg, loaders: { ...(cfg.loaders || {}), [v]: l } })
  return <div className="fade">
    <div className="ph"><div><h1 className="grad-text">Версии</h1><div className="sub">Выберите версию и загрузчик — лаунчер сам скачает игру, Java и моды</div></div>{profiles}</div>
    {/* selected version */}
    <div className="hero mb-6" style={{ minHeight: 280 }}><VImg v={ver} cls="bg" />
      <div className="relative p-8 text-white flex flex-col justify-end min-h-[280px]">
        <div className="flex gap-2 mb-2">{ver === recommended && <span className="badge rec"><Gem s={11} glow={false} />Рекомендуем · SafiMine</span>}{ast && <span className="badge ast"><LoaderIcon k="asteria" s={12} />Значок AL в табе</span>}{ast === 'full' ? <span className="badge ast"><Icon n="sparkles" s={11} />Визуалы Asteria</span> : <span className="badge soft" title="Визуалы Asteria работают на 1.16.5, 1.20.1 и 1.21.4">Без визуалов</span>}</div>
        <div className="flex items-end gap-4"><h2 className="text-6xl font-black tracking-tight leading-none" style={{ textShadow: '0 4px 30px rgba(0,0,0,.5)' }}>{ver}</h2><div className="pb-1.5 text-lg font-bold opacity-90">{info?.name}</div></div>
        <ul className="text-sm mt-3 mb-5 flex flex-col gap-1 opacity-90 max-w-xl">{(info?.ru ?? []).map((b) => <li key={b} className="flex gap-2"><span style={{ color: 'var(--a2)' }}><Icon n="check" s={14} /></span>{b}</li>)}</ul>
        <div className="flex items-center gap-3 flex-wrap">
          <button className={'playbtn ' + (running ? 'stop' : '')} onClick={play}><Icon n={running ? 'close' : 'play'} s={20} fill={!running} />{running ? 'Закрыть игру' : label || 'Играть ' + ver}</button>
          <div className="loaders !gap-2">{(['fabric', 'vanilla'] as Loader[]).map((l) => <button key={l} disabled={l === 'fabric' && !fabricFor(ver)} className={'loader !bg-white/10 !text-white ' + (ld === l ? 'on' : '')} onClick={() => setLoader(ver, l)}
            title={l === 'fabric' && !fabricFor(ver) ? 'Для этой версии Fabric не нужен: моды для неё пока не поддерживаются' : ''}><LoaderIcon k={l} s={16} />{l === 'fabric' ? 'Fabric' : 'Vanilla'}</button>)}</div>
          <span className="text-xs opacity-75">Java {info?.java ?? '—'} · {ld === 'fabric' ? 'моды, FPS-буст, Asteria' : 'чистая игра'}</span>
        </div></div></div>

    <div className="section">Все версии</div>
    <div className="vgrid stagger">{FAMILIES.map((f) => { const v = f.subs.includes(ver) ? ver : f.subs[0], on = f === sel, a = ASTERIA[v]
      return <Tilt key={f.id} className={'vcard ' + (on ? 'on' : '')} onClick={() => setVer(v)} max={5}>
        <div className="art"><VImg v={v} cls="" /><div className="ver">{f.id === '26' ? '26' : f.id}</div>
          <div className="tagline flex gap-1">{f.subs.includes(recommended) && <span className="badge rec"><Gem s={10} glow={false} /></span>}{a && <span className="badge ast" title="Значок AL в табе и над головой, скины и плащи игроков Asteria"><LoaderIcon k="asteria" s={11} />Значок</span>}{a === 'full' && <span className="badge ast" title="Визуалы Asteria: HUD, частицы, питомцы, музыка и др."><Icon n="sparkles" s={10} />Визуалы</span>}</div></div>
        <div className="p-3 flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
          <div className="flex-1 min-w-0"><Dropdown value={v} w={200} className="w-full !min-w-0 !py-1.5" set={(x) => setVer(x)} options={f.subs.map((s) => ({ value: s, label: s, hint: VINFO[s]?.name, icon: <LoaderIcon k={ASTERIA[s] ? 'asteria' : 'vanilla'} s={14} /> }))} /></div>
        </div>
        <div className="px-3 pb-3 -mt-1 muted text-xs truncate">{VINFO[v]?.name}</div>
      </Tilt> })}</div>
  </div>
}
