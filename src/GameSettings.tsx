import { useEffect, useRef, useState } from 'react'
import { Icon } from './Icons'
import { Dropdown } from './ui'
import { VINFO } from './versionInfo'
declare global { interface Window { api: any } }

// ---- version helpers: Minecraft stores some settings differently in different versions
const vnum = (v: string) => { const [a, b, c] = v.split('.').map((x) => parseInt(x) || 0); return a >= 26 ? 2600 + b : b * 100 + c } // 1.16.5 -> 1605, 26.1 -> 2601
const legacyKeys = (v: string) => vnum(v) < 1300   // 1.8.9 / 1.12.2: LWJGL2 numeric key codes
type O = Record<string, string>

// browser KeyboardEvent.code -> modern key name (key.keyboard.<name>) and LWJGL2 code
const MODERN: Record<string, string> = { Space: 'space', ShiftLeft: 'left.shift', ShiftRight: 'right.shift', ControlLeft: 'left.control', ControlRight: 'right.control', AltLeft: 'left.alt', AltRight: 'right.alt', Tab: 'tab', Escape: 'escape', Enter: 'enter', Backspace: 'backspace', CapsLock: 'caps.lock', Backquote: 'grave.accent', Minus: 'minus', Equal: 'equal', BracketLeft: 'left.bracket', BracketRight: 'right.bracket', Semicolon: 'semicolon', Quote: 'apostrophe', Comma: 'comma', Period: 'period', Slash: 'slash', Backslash: 'backslash', ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right', Insert: 'insert', Delete: 'delete', Home: 'home', End: 'end', PageUp: 'page.up', PageDown: 'page.down' }
const LW2: Record<string, number> = { Escape: 1, Digit1: 2, Digit2: 3, Digit3: 4, Digit4: 5, Digit5: 6, Digit6: 7, Digit7: 8, Digit8: 9, Digit9: 10, Digit0: 11, Minus: 12, Equal: 13, Backspace: 14, Tab: 15,
  KeyQ: 16, KeyW: 17, KeyE: 18, KeyR: 19, KeyT: 20, KeyY: 21, KeyU: 22, KeyI: 23, KeyO: 24, KeyP: 25, BracketLeft: 26, BracketRight: 27, Enter: 28, ControlLeft: 29, KeyA: 30, KeyS: 31, KeyD: 32, KeyF: 33, KeyG: 34, KeyH: 35, KeyJ: 36, KeyK: 37, KeyL: 38, Semicolon: 39, Quote: 40, Backquote: 41, ShiftLeft: 42, Backslash: 43,
  KeyZ: 44, KeyX: 45, KeyC: 46, KeyV: 47, KeyB: 48, KeyN: 49, KeyM: 50, Comma: 51, Period: 52, Slash: 53, ShiftRight: 54, AltLeft: 56, Space: 57, CapsLock: 58, F1: 59, F2: 60, F3: 61, F4: 62, F5: 63, F6: 64, F7: 65, F8: 66, F9: 67, F10: 68, F11: 87, F12: 88, ControlRight: 157, AltRight: 184, ArrowUp: 200, ArrowLeft: 203, ArrowRight: 205, ArrowDown: 208, Delete: 211 }
const LW2NAME = Object.fromEntries(Object.entries(LW2).map(([k, v]) => [v, k.replace(/^Key|^Digit/, '').replace('Left', ' L').replace('Right', ' R').replace('Arrow', '')]))
const modernName = (c: string) => c.startsWith('Key') ? c.slice(3).toLowerCase() : c.startsWith('Digit') ? c.slice(5) : /^F\d+$/.test(c) ? c.toLowerCase() : MODERN[c] ?? c.toLowerCase()
const MOUSE_M = ['left', 'middle', 'right', '4', '5'], MOUSE_L = [-100, -98, -99, -97, -96]
function keyLabel(v: string | undefined, legacy: boolean) {
  if (v == null || v === '') return '—'
  if (legacy) { const n = +v; if (n < 0) return ['ЛКМ', 'ПКМ', 'СКМ', 'Мышь 4', 'Мышь 5'][[-100, -99, -98, -97, -96].indexOf(n)] ?? 'Мышь'; return n === 0 ? '—' : LW2NAME[n] ?? String(n) }
  if (v === 'key.keyboard.unknown') return '—'
  const m = v.match(/^key\.mouse\.(.+)$/); if (m) return ({ left: 'ЛКМ', right: 'ПКМ', middle: 'СКМ' } as any)[m[1]] ?? 'Мышь ' + m[1]
  const k = v.replace('key.keyboard.', '').replace('left.', 'L ').replace('right.', 'R ')
  return k.length === 1 ? k.toUpperCase() : k.charAt(0).toUpperCase() + k.slice(1)
}
// Minecraft's default keys, shown until the game writes its own settings file
const DEF_M: Record<string, string> = { 'asteria.menu': 'right.shift', 'asteria.zoom': 'c', forward: 'w', back: 's', left: 'a', right: 'd', jump: 'space', sneak: 'left.shift', sprint: 'left.control', inventory: 'e', drop: 'q', swapOffhand: 'f', chat: 't', command: 'slash', playerlist: 'tab', togglePerspective: 'f5', screenshot: 'f2', smoothCamera: 'unknown', fullscreen: 'f11', ...Object.fromEntries([1, 2, 3, 4, 5, 6, 7, 8, 9].map((i) => ['hotbar.' + i, String(i)])) }
const DEF_L: Record<string, number> = { forward: 17, back: 31, left: 30, right: 32, jump: 57, sneak: 42, sprint: 29, attack: -100, use: -99, pickItem: -98, inventory: 18, drop: 16, swapOffhand: 33, chat: 20, command: 53, playerlist: 15, togglePerspective: 63, screenshot: 60, smoothCamera: 0, fullscreen: 87, ...Object.fromEntries([1, 2, 3, 4, 5, 6, 7, 8, 9].map((i) => ['hotbar.' + i, i + 1])) }
const defKey = (id: string, legacy: boolean) => legacy ? String(DEF_L[id] ?? 0) : id === 'attack' ? 'key.mouse.left' : id === 'use' ? 'key.mouse.right' : id === 'pickItem' ? 'key.mouse.middle' : 'key.keyboard.' + (DEF_M[id] ?? 'unknown')
const BINDS: [string, string][] = [['forward', 'Вперёд'], ['back', 'Назад'], ['left', 'Влево'], ['right', 'Вправо'], ['jump', 'Прыжок'], ['sneak', 'Присесть'], ['sprint', 'Бег'], ['attack', 'Атака / ломать'], ['use', 'Использовать'],
  ['pickItem', 'Выбрать блок'], ['inventory', 'Инвентарь'], ['drop', 'Выбросить'], ['swapOffhand', 'Во вторую руку'], ['chat', 'Чат'], ['command', 'Команда'], ['playerlist', 'Список игроков (таб)'], ['togglePerspective', 'Вид (F5)'], ['screenshot', 'Скриншот'], ['smoothCamera', 'Плавная камера'], ['fullscreen', 'Полный экран'],
  ...[1, 2, 3, 4, 5, 6, 7, 8, 9].map((i) => ['hotbar.' + i, 'Слот ' + i] as [string, string])]

type Setter = (p: O) => void
function Slider({ label, k, min, max, step = 1, value, show, write, set }: { label: string; k: string; min: number; max: number; step?: number; value: number; show?: (v: number) => string; write?: (v: number) => O; set: Setter }) {
  return <label className="block"><div className="flex justify-between text-sm mb-1"><span>{label}</span><b>{show ? show(value) : value}</b></div>
    <input type="range" className="w-full" min={min} max={max} step={step} value={value} onChange={(e) => set(write ? write(+e.target.value) : { [k]: String(+e.target.value) })} /></label>
}
function Switch({ label, k, value, hint, set }: { label: string; k: string; value: boolean; hint?: string; set: Setter }) {
  return <button className="flex items-center justify-between gap-3 w-full text-left py-1" onClick={() => set({ [k]: String(!value) })}><span className="text-sm">{label}{hint && <span className="muted text-xs block">{hint}</span>}</span><span className={'switch ' + (value ? 'on' : '')} /></button>
}
function Seg({ label, items, value, pick }: { label: string; items: [string | number, string][]; value: string | number; pick: (v: any) => void }) {
  return <div><div className="text-sm mb-1">{label}</div><div className="seg flex w-full">{items.map(([v, l]) => <button key={String(v)} className={'flex-1 ' + (String(value) === String(v) ? 'on' : '')} onClick={() => pick(v)}>{l}</button>)}</div></div>
}

export default function GameSettings({ ver, versions, setVer, say }: { ver: string; versions: string[]; setVer: (v: string) => void; say: (m: string) => void }) {
  const [o, setO] = useState<O | null>(null), [sec, setSec] = useState('video'), [listen, setListen] = useState(''), [running, setRunning] = useState<any>({}), timers = useRef<Record<string, any>>({})
  const L = legacyKeys(ver), n = vnum(ver)
  useEffect(() => { setO(null); window.api.optsGet(ver).then(setO); window.api.gameState().then(setRunning) }, [ver])
  useEffect(() => { const id = setInterval(() => window.api.gameState().then(setRunning), 3000); return () => clearInterval(id) }, [])
  const live = running.running && running.version === ver
  /** writes right away in the UI; to disk (and into the running game) after a short pause, so sliders stay smooth */
  const set = (patch: O) => {
    setO((x) => ({ ...(x || {}), ...patch }))
    const key = Object.keys(patch).join(',')
    clearTimeout(timers.current[key]); timers.current[key] = setTimeout(async () => { const r = await window.api.optsSet(ver, patch); if (r?.live) say('Применено в игре') }, 250)
  }
  useEffect(() => { if (!listen) return
    const done = (v: string) => { set({ ['key_key.' + listen]: v }); setListen('') }
    const kd = (e: KeyboardEvent) => { e.preventDefault(); e.stopPropagation(); if (e.code === 'Escape') return done(L ? '0' : 'key.keyboard.unknown'); done(L ? String(LW2[e.code] ?? 0) : 'key.keyboard.' + modernName(e.code)) }
    const md = (e: MouseEvent) => { if (e.button > 4) return; e.preventDefault(); e.stopPropagation(); done(L ? String(MOUSE_L[e.button]) : 'key.mouse.' + MOUSE_M[e.button]) }
    addEventListener('keydown', kd, true); const tm = setTimeout(() => addEventListener('mousedown', md, true), 120)
    return () => { removeEventListener('keydown', kd, true); removeEventListener('mousedown', md, true); clearTimeout(tm) } }, [listen])
  if (!o) return <div className="card rounded-2xl p-5 muted">…</div>

  const num = (k: string, d: number) => { const v = parseFloat(o[k]); return isNaN(v) ? d : v }
  const bool = (k: string, d: boolean) => o[k] == null ? d : o[k] === 'true'
  const fov = Math.round(70 + num('fov', 0) * 40) // stored as (fov - 70) / 40 in every version
  const graphics = o.graphicsMode != null || n >= 1600 ? Math.round(num('graphicsMode', 1)) : bool('fancyGraphics', true) ? 1 : 0
  const aoBool = o.ao === 'true' || o.ao === 'false' || (o.ao == null && n >= 1900), ao = aoBool ? (bool('ao', true) ? 2 : 0) : Math.round(num('ao', 2))
  const clouds = o.renderClouds ?? 'true'

  const pct = (v: number) => Math.round(v * 100) + '%'

  const SECS: [string, string, string][] = [['video', 'Графика', 'image'], ['controls', 'Управление', 'cursor'], ['sound', 'Звук', 'volume'], ['chat', 'Чат', 'chat'], ['keys', 'Клавиши', 'keyboard']]
  return <div className="card rounded-2xl p-5">
    <div className="flex items-center gap-3 flex-wrap mb-4">
      <div className="font-bold text-lg flex items-center gap-2"><Icon n="sliders" s={18} />Игровые настройки</div>
      <Dropdown value={ver} set={setVer} w={240} className="!py-1.5" options={versions.map((v) => ({ value: v, label: 'Minecraft ' + v, hint: VINFO[v]?.name }))} />
      <span className="pill ml-auto" style={live ? { background: 'rgba(16,185,129,.16)', color: '#059669' } : {}}><span className="inline-block w-2 h-2 rounded-full" style={{ background: live ? '#10b981' : 'var(--muted)' }} />
        {live ? 'Игра запущена — применяется сразу' : running.running ? 'Запущена другая версия — применится при её запуске' : 'Сохраняется автоматически'}</span></div>
    <div className="seg flex mb-5">{SECS.map(([k, l, ic]) => <button key={k} className={'flex-1 flex items-center justify-center gap-2 ' + (sec === k ? 'on' : '')} onClick={() => setSec(k)}><Icon n={ic} s={15} />{l}</button>)}</div>

    {sec === 'video' && <div className="grid grid-cols-2 gap-x-8 gap-y-5">
      <Slider set={set} label="Дальность прорисовки" k="renderDistance" min={2} max={32} value={Math.round(num('renderDistance', 12))} show={(v) => v + ' чанков'} />
      <Slider set={set} label="Макс. FPS" k="maxFps" min={10} max={260} step={10} value={Math.round(num('maxFps', 120))} show={(v) => v >= 260 ? 'Без ограничений' : String(v)} />
      <Seg label="Графика" items={n >= 1600 ? [[0, 'Быстрая'], [1, 'Детальная'], [2, 'Изумительная']] : [[0, 'Быстрая'], [1, 'Детальная']]} value={graphics} pick={(v) => set(o.graphicsMode != null || n >= 1600 ? { graphicsMode: String(v) } : { fancyGraphics: String(v > 0) })} />
      <Seg label="Мягкое освещение" items={aoBool ? [[0, 'Выкл'], [2, 'Вкл']] : [[0, 'Выкл'], [1, 'Мин'], [2, 'Макс']]} value={ao} pick={(v) => set({ ao: aoBool ? String(v > 0) : String(v) })} />
      <Slider set={set} label="Яркость" k="gamma" min={0} max={1} step={0.01} value={num('gamma', 0.5)} show={(v) => v <= 0 ? 'Мрачно' : v >= 1 ? 'Ярко' : pct(v)} />
      <Seg label="Облака" items={[['false', 'Нет'], ['fast', 'Быстрые'], ['true', 'Детальные']]} value={clouds} pick={(v) => set({ renderClouds: String(v) })} />
      <Seg label="Частицы" items={[[0, 'Все'], [1, 'Меньше'], [2, 'Минимум']]} value={Math.round(num('particles', 0))} pick={(v) => set({ particles: String(v) })} />
      <Seg label="Масштаб интерфейса" items={[[0, 'Авто'], [1, '1'], [2, '2'], [3, '3'], [4, '4']]} value={Math.round(num('guiScale', 0))} pick={(v) => set({ guiScale: String(v) })} />
      {n >= 1300 && <Slider set={set} label="Смешивание биомов" k="biomeBlendRadius" min={0} max={7} value={Math.round(num('biomeBlendRadius', 2))} show={(v) => v === 0 ? 'Выкл' : `${v * 2 + 1}×${v * 2 + 1}`} />}
      {n >= 1600 && <Slider set={set} label="Дальность сущностей" k="entityDistanceScaling" min={0.5} max={5} step={0.25} value={num('entityDistanceScaling', 1)} show={pct} />}
      <Slider set={set} label="Уровни MIP-текстур" k="mipmapLevels" min={0} max={4} value={Math.round(num('mipmapLevels', 4))} show={(v) => v === 0 ? 'Выкл' : String(v)} />
      <div className="flex flex-col gap-1"><Switch set={set} label="Вертикальная синхронизация" k="enableVsync" value={bool('enableVsync', true)} /><Switch set={set} label="Тени сущностей" k="entityShadows" value={bool('entityShadows', true)} />
        <Switch set={set} label="Покачивание камеры" k="bobView" value={bool('bobView', true)} /></div>
    </div>}

    {sec === 'controls' && <div className="grid grid-cols-2 gap-x-8 gap-y-5">
      <Slider set={set} label="Поле зрения (FOV)" k="fov" min={30} max={110} value={fov} show={(v) => v === 70 ? '70 · обычное' : v === 110 ? '110 · Quake Pro' : String(v)} write={(v) => ({ fov: ((v - 70) / 40).toFixed(4) })} />
      <Slider set={set} label="Чувствительность мыши" k="mouseSensitivity" min={0} max={200} value={Math.round(num('mouseSensitivity', 0.5) * 200)} show={(v) => v === 0 ? 'Сонная' : v === 200 ? 'ГИПЕРСКОРОСТЬ!' : v + '%'} write={(v) => ({ mouseSensitivity: (v / 200).toFixed(4) })} />
      <div className="flex flex-col gap-1"><Switch set={set} label="Инвертировать мышь" k="invertYMouse" value={bool('invertYMouse', false)} />{n >= 1500 && <Switch set={set} label="Сырой ввод мыши" hint="Без ускорения Windows — точнее в PvP" k="rawMouseInput" value={bool('rawMouseInput', true)} />}
        {n >= 1100 && <Switch set={set} label="Автопрыжок" k="autoJump" value={bool('autoJump', false)} />}</div>
      <div className="flex flex-col gap-1">{n >= 1500 ? <><Switch set={set} label="Бег — переключением" k="toggleSprint" value={bool('toggleSprint', false)} /><Switch set={set} label="Присед — переключением" k="toggleCrouch" value={bool('toggleCrouch', false)} /></> : <p className="muted text-sm">В этой версии нет режима переключения бега/приседа.</p>}
        <Switch set={set} label="Подсказки (F3+H)" k="advancedItemTooltips" value={bool('advancedItemTooltips', false)} /></div>
    </div>}

    {sec === 'sound' && <div className="grid grid-cols-2 gap-x-8 gap-y-5">{([['master', 'Общая громкость'], ['music', 'Музыка'], ['record', 'Музыкальные блоки'], ['weather', 'Погода'], ['block', 'Блоки'], ['hostile', 'Враждебные'], ['neutral', 'Дружелюбные'], ['player', 'Игроки'], ['ambient', 'Окружение'], ['voice', 'Голос']] as [string, string][]).map(([k, l]) =>
      <Slider set={set} key={k} label={l} k={'soundCategory_' + k} min={0} max={1} step={0.01} value={num('soundCategory_' + k, 1)} show={(v) => v === 0 ? 'Выкл' : pct(v)} />)}</div>}

    {sec === 'chat' && <div className="grid grid-cols-2 gap-x-8 gap-y-5">
      <Slider set={set} label="Прозрачность текста" k="chatOpacity" min={0.1} max={1} step={0.01} value={num('chatOpacity', 1)} show={pct} />
      {n >= 1400 && <Slider set={set} label="Фон текста" k="textBackgroundOpacity" min={0} max={1} step={0.01} value={num('textBackgroundOpacity', 0.5)} show={pct} />}
      <Slider set={set} label="Размер чата" k="chatScale" min={0} max={1} step={0.01} value={num('chatScale', 1)} show={pct} />
      <Slider set={set} label="Ширина чата" k="chatWidth" min={0} max={1} step={0.01} value={num('chatWidth', 1)} show={(v) => Math.round(40 + v * 280) + ' px'} />
      <div className="flex flex-col gap-1"><Switch set={set} label="Цвета в чате" k="chatColors" value={bool('chatColors', true)} /><Switch set={set} label="Ссылки в чате" k="chatLinks" value={bool('chatLinks', true)} /></div>
    </div>}

    {sec === 'keys' && <><p className="muted text-sm mb-3">Нажмите на действие, затем клавишу или кнопку мыши. Esc — снять назначение.</p>
      {!L && ['1.16.5', '1.20.1', '1.21.4'].includes(ver) && <><div className="label mb-2">Asteria</div><div className="grid grid-cols-3 gap-2 mb-4">{([['asteria.menu', 'Меню визуалов Asteria'], ['asteria.zoom', 'Зум Asteria']] as [string, string][]).map(([id, ru]) => <button key={id} className={'btn flex justify-between items-center !py-2 ' + (listen === id ? 'btn-on' : '')} onClick={() => setListen(id)}>
        <span className="text-sm">{ru}</span>{listen === id ? <span className="text-xs">нажмите…</span> : <kbd>{keyLabel(o['key_key.' + id] ?? defKey(id, L), L)}</kbd>}</button>)}</div><div className="label mb-2">Minecraft</div></>}
      <div className="grid grid-cols-3 gap-2">{BINDS.filter(([id]) => !(id === 'swapOffhand' && n < 900)).map(([id, ru]) => <button key={id} className={'btn flex justify-between items-center !py-2 ' + (listen === id ? 'btn-on' : '')} onClick={() => setListen(id)}>
        <span className="text-sm">{ru}</span>{listen === id ? <span className="text-xs">нажмите…</span> : <kbd>{keyLabel(o['key_key.' + id] ?? defKey(id, L), L)}</kbd>}</button>)}</div></>}
  </div>
}
