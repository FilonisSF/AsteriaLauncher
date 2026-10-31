const zlib = require('zlib')
const { app, BrowserWindow, ipcMain, shell, dialog, net, nativeImage, clipboard, screen, Notification } = require('electron')
const { spawnSync, execFileSync } = require('child_process')
app.commandLine.appendSwitch('proxy-bypass-list', '<local>')
const os = require('os'), crypto = require('crypto')
const path = require('path'), fs = require('fs')
const Store = require('electron-store')
const { Client, Authenticator } = require('minecraft-launcher-core')
const store = new Store({ encryptionKey: 'safimine-local-key' }) // tokens stay in main process
const HOST = 'mc.safimine.ru', PORT = 25565
const API_DEFAULT = process.env.ASTERIA_API || 'https://asteria-api-1ndm.onrender.com/asteria' // the Asteria server on Render (render.yaml): online list, tab icon, skins, pets, config codes
const API_LOCAL = ['http://127.0.0.1:8080/asteria', 'http://localhost:8080/asteria'] // start-backend.bat on this PC is found automatically
const LINKS = /^https?:\/\//
let win
const defaults = { discord: true, discordClientId: '', ramSet: false, ramMb: 3072, width: 1280, height: 720, fullscreen: false, fabric: true, jvm: '', java: '', dir: path.join(app.getPath('appData'), '.asteria'), mods: {}, api: '' }
const settings = () => ({ ...defaults, ...store.get('settings', {}) })
function offlineAuth(name) { // same UUID the server derives for offline players (UUID v3 of "OfflinePlayer:<name>")
  const b = crypto.createHash('md5').update('OfflinePlayer:' + name).digest(); b[6] = (b[6] & 0x0f) | 0x30; b[8] = (b[8] & 0x3f) | 0x80
  const h = b.toString('hex'), uuid = `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`
  return { access_token: uuid, client_token: uuid, uuid, name, user_properties: '{}', meta: { type: 'mojang', demo: false } }
}
const pub = (a) => ({ id: a.id, name: a.name, type: a.type })
let apiNow = null, apiAt = 0
const clean = (u) => String(u || '').trim().replace(/\/+$/, '')
const apiBase = () => apiNow || clean((store.get('remoteConf') || {}).api) || API_DEFAULT
async function apiAlive(b) { try { const r = await net.fetch(b + '/', { signal: AbortSignal.timeout(2500) }); return !!(await r.json()).ok } catch { return false } }
async function resolveApi(force = false) { // finds a working backend; re-checked every 2 minutes
  if (!force && apiNow && Date.now() - apiAt < 120000) return apiNow
  const list = [clean((store.get('remoteConf') || {}).api), API_DEFAULT, ...API_LOCAL].filter(Boolean)
  const ok = await Promise.all(list.map(apiAlive)); apiNow = list[ok.indexOf(true)] || null; apiAt = Date.now()
  try { win?.webContents.send('api', { base: apiBase(), online: !!apiNow }) } catch {}
  return apiBase() }
ipcMain.handle('api:info', async (_, force) => { await resolveApi(!!force); return { base: apiBase(), online: !!apiNow, local: API_LOCAL.includes(apiNow) } })

function create() {
  app.setAppUserModelId('ru.safimine.asteria')
  const wa = screen.getPrimaryDisplay().workAreaSize, saved = store.get('bounds')
  const w = Math.min(1440, Math.max(980, Math.round(wa.width * 0.74))), h = Math.min(900, Math.max(620, Math.round(wa.height * 0.8)))
  win = new BrowserWindow({ width: saved?.width || w, height: saved?.height || h, minWidth: 960, minHeight: 600, frame: false, backgroundColor: '#050b12', show: false,
    icon: path.join(__dirname, '../build/icon.ico'),
    webPreferences: { preload: path.join(__dirname, 'preload.cjs'), contextIsolation: true, nodeIntegration: false } })
  if (store.get('maximized')) win.maximize()
  win.once('ready-to-show', () => win.show())
  win.on('close', () => { try { store.set('maximized', win.isMaximized()); if (!win.isMaximized()) store.set('bounds', win.getBounds()) } catch {} })
  process.env.NODE_ENV === 'development' ? win.loadURL('http://127.0.0.1:5173') : win.loadFile(path.join(__dirname, '../dist/index.html'))
}
// Server status: direct ping AND two public status services at once (works behind a VPN / when the direct ping is blocked);
// the first answer wins. The main server is shown as down only after two failed rounds in a row.
async function srvStatus(host, port = 25565) {
  host = String(host).trim(); port = +port || 25565
  const direct = (async () => { const { status } = require('minecraft-server-util'), t = Date.now(), r = await status(host, port, { timeout: 8000, enableSRV: true })
    return { online: true, ping: r.roundTripLatency ?? Date.now() - t, players: r.players.online, max: r.players.max, motd: r.motd?.clean, icon: r.favicon || null, version: r.version?.name, via: 'direct' } })()
  const web = async (url, map) => { const t = Date.now(), r = await net.fetch(url, { signal: AbortSignal.timeout(9000), headers: { 'user-agent': 'AsteriaLauncher' } }); if (!r.ok) throw 0
    const j = await r.json(), m = map(j); if (!m) throw 0; return { ping: Math.max(1, Math.round((Date.now() - t) / 2)), ...m } }
  const hp = port === 25565 ? host : host + ':' + port
  const a = web('https://api.mcstatus.io/v2/status/java/' + hp, (j) => j.online && { online: true, players: j.players?.online ?? 0, max: j.players?.max ?? 0, motd: j.motd?.clean, icon: j.icon || null, version: j.version?.name_clean, via: 'mcstatus' })
  const b = web('https://api.mcsrvstat.us/3/' + hp, (j) => j.online && { online: true, players: j.players?.online ?? 0, max: j.players?.max ?? 0, motd: (j.motd?.clean || []).join('\n'), icon: j.icon || null, version: j.version, via: 'mcsrvstat' })
  try { return await Promise.any([direct, a, b]) } catch { return { online: false } }
}
let lastSt = null, failRun = 0
async function poll() {
  const r = await srvStatus(HOST, PORT)
  if (r.online) { failRun = 0; lastSt = { online: true, ping: r.ping || 0, players: r.players || 0, max: r.max || 500 } }
  else if (++failRun >= 2 || !lastSt) lastSt = { online: false, ping: 0, players: 0, max: lastSt?.max || 500 }
  win?.webContents.send('status', lastSt)
}
ipcMain.handle('srv:status', (_, host, port) => srvStatus(host, port))
/** servers next to SafiMine in the launcher and in the game: partners (built in) and the player's own */
const PARTNER_SERVERS = [{ name: 'GrumWorld', ip: 'mc.GrumWorld.space' }]
const myServers = () => (store.get('myServers', []) || []).filter((x) => x && x.ip)
ipcMain.handle('srv:mine', (_, list) => { if (Array.isArray(list)) store.set('myServers', list.filter((x) => x && /^[a-z0-9.\-]{3,80}$/i.test(String(x.ip || ''))).slice(0, 40).map((x) => ({ name: String(x.name || x.ip).slice(0, 40), ip: String(x.ip), port: +x.port || 25565, desc: String(x.desc || '').slice(0, 300), version: String(x.version || '') }))); return myServers() })
async function addServerEntry(dir, name, ip) { // adds one more server to the in-game list (once)
  const nbt = require('prismarine-nbt'), f = path.join(dir, 'servers.dat'); if (!fs.existsSync(f)) return
  const { parsed } = await nbt.parse(fs.readFileSync(f)), list = parsed.value.servers.value.value
  if (list.some((s) => s.ip.value === ip)) return
  list.push({ name: nbt.string(name), ip: nbt.string(ip) }); fs.writeFileSync(f, nbt.writeUncompressed(parsed, 'big')) }
async function addServer(dir) {
  const nbt = require('prismarine-nbt'), f = path.join(dir, 'servers.dat')
  const entry = { name: nbt.string('SafiMine'), ip: nbt.string(HOST) }
  let parsed
  if (fs.existsSync(f)) {
    ({ parsed } = await nbt.parse(fs.readFileSync(f)))
    const list = parsed.value.servers.value.value
    if (list.some((s) => s.ip.value === HOST)) return
    list.push(entry)
  } else parsed = nbt.comp({ servers: nbt.list(nbt.comp([entry])) })
  fs.mkdirSync(dir, { recursive: true }); fs.writeFileSync(f, nbt.writeUncompressed(parsed, 'big'))
}
// ---------- Asteria mods. Free mod (tab star, skins/capes, live settings): GitHub release "mods" or your backend.
// Asteria Visuals: free, shipped with the launcher (extraResources) or downloaded from the GitHub release "mods".
const MODS_GH = 'https://github.com/FilonisSF/AsteriaLauncher/releases/download/mods'
const modCache = () => { const d = path.join(app.getPath('userData'), 'asteria-mods'); fs.mkdirSync(d, { recursive: true }); return d }
async function fetchJar(urls, file, maxAgeMs) {
  if (fs.existsSync(file) && Date.now() - fs.statSync(file).mtimeMs < maxAgeMs) return file
  for (const u of urls) {
    try { const r = await net.fetch(u, { signal: AbortSignal.timeout(15000), headers: { 'user-agent': 'AsteriaLauncher' } }); if (!r.ok) continue
      const b = Buffer.from(await r.arrayBuffer()); if (b.subarray(0, 2).toString() !== 'PK') continue
      fs.writeFileSync(file, b); return file } catch {}
  }
  if (fs.existsSync(file)) { fs.utimesSync(file, new Date(), new Date()); return file } // offline: keep using the cached copy
  return null
}
// MCLC downloads Mojang's log4j config; a failed download leaves an empty file and the game then writes no log at all
const LOG4J = '<?xml version="1.0" encoding="UTF-8"?>\n<Configuration status="WARN" packages="">\n <Appenders>\n  <Console name="SysOut" target="SYSTEM_OUT"><PatternLayout pattern="[%d{HH:mm:ss}] [%t/%level]: %msg%n"/></Console>\n  <RollingRandomAccessFile name="File" fileName="logs/latest.log" filePattern="logs/%d{yyyy-MM-dd}-%i.log.gz"><PatternLayout pattern="[%d{HH:mm:ss}] [%t/%level]: %msg%n"/><Policies><TimeBasedTriggeringPolicy/><OnStartupTriggeringPolicy/></Policies></RollingRandomAccessFile>\n </Appenders>\n <Loggers><Root level="info"><AppenderRef ref="SysOut"/><AppenderRef ref="File"/></Root></Loggers>\n</Configuration>\n'
function fixLog4j(root) { for (const n of ['log4j2_112-116.xml', 'log4j2_17-111.xml']) { const f = path.join(root, n); try { if (!fs.existsSync(f) || fs.statSync(f).size < 50) fs.writeFileSync(f, LOG4J) } catch {} } }
async function coreMod(version) {
  const name = `asteria-core-${version}.jar`
  const local = [path.join(process.resourcesPath || '', 'mods', name), path.join(__dirname, '..', 'mods', 'dist', name)].find((f) => fs.existsSync(f))
  if (local) return local // a jar you built yourself wins (mods/dist/ while developing)
  const miss = store.get('modMiss', {}); if (miss[version] && Date.now() - miss[version] < 3600000 && !fs.existsSync(path.join(modCache(), name))) return null
  const base = (store.get('remoteConf') || {}).modsBase
  const got = await fetchJar([base && `${String(base).replace(/\/+$/, '')}/${name}`, `${apiBase()}/mods/${name}`, `${MODS_GH}/${name}`].filter(Boolean), path.join(modCache(), name), 3600000)
  if (!got) { miss[version] = Date.now(); store.set('modMiss', miss) } else if (miss[version]) { delete miss[version]; store.set('modMiss', miss) }
  return got
}
async function visualsMod(version) { // Asteria Visuals: free for everyone, shipped with the launcher (or downloaded like the core mod)
  const name = `asteria-visuals-${version}.jar`
  const local = [path.join(process.resourcesPath || '', 'mods', name), path.join(__dirname, '..', 'mods', 'dist', name)].find((f) => fs.existsSync(f))
  if (local) return local
  const base = (store.get('remoteConf') || {}).modsBase
  return fetchJar([base && `${String(base).replace(/\/+$/, '')}/${name}`, `${MODS_GH}/${name}`].filter(Boolean), path.join(modCache(), name), 3600000)
}
const instanceDirs = () => { try { const b = path.join(settings().dir, 'instances'); return fs.readdirSync(b).map((n) => path.join(b, n)) } catch { return [] } }
function writeVisuals(dir, v = store.get('visuals')) { if (!v) return; try { const c = path.join(dir, 'config'); fs.mkdirSync(c, { recursive: true }); fs.writeFileSync(path.join(c, 'asteria-visuals.json'), JSON.stringify(v, null, 1)) } catch {} }
const visLatest = () => { let best = store.get('visuals', null), t = store.get('visualsAt', 0)
  for (const d of instanceDirs()) { const f = path.join(d, 'config', 'asteria-visuals.json'); try { const m = fs.statSync(f).mtimeMs; if (m > t + 1500) { best = JSON.parse(fs.readFileSync(f, 'utf8')); t = m } } catch {} }
  if (best !== store.get('visuals', null)) { store.set('visuals', best); store.set('visualsAt', t) }
  return best }
ipcMain.handle('vis:get', () => visLatest())
const visSave = (v) => { store.set('visuals', v); store.set('visualsAt', Date.now()); for (const d of instanceDirs()) writeVisuals(d, v); setTimeout(() => beatNow?.(), 50); return { live: !!game } } // the mod re-reads the file every second
ipcMain.handle('vis:set', (_, v) => visSave(v))
/** one module changed (Skins page cards): merged into the freshest config, so cards and the Visuals page never overwrite each other with an old copy */
ipcMain.handle('vis:patch', (_, id, patch) => { const v = visLatest() || { modules: {} }; v.modules = v.modules || {}; const o = v.modules[id] || {}
  v.modules[id] = { ...o, ...patch, s: { ...(o.s || {}), ...((patch && patch.s) || {}) } }; return visSave(v) })
// visuals presets: the list lives in the launcher store and is mirrored to config/asteria-visuals-presets.json, so the in-game menu can switch them
function writePresets(dir, p = store.get('visPresets')) { if (!p) return; try { const c = path.join(dir, 'config'); fs.mkdirSync(c, { recursive: true }); fs.writeFileSync(path.join(c, 'asteria-visuals-presets.json'), JSON.stringify(p)) } catch {} }
ipcMain.handle('visp:get', () => { const p = store.get('visPresets', null); if (!p) return null; let t = store.get('visPresetsAt', 0)
  let newer = null
  for (const d of instanceDirs()) { const f = path.join(d, 'config', 'asteria-visuals-presets.json'); try { const m = fs.statSync(f).mtimeMs; if (m > t + 1500) { const j = JSON.parse(fs.readFileSync(f, 'utf8')); if (Array.isArray(j.list) && j.list.length) { newer = j; t = m } } } catch {} }
  if (newer) { store.set('visPresets', newer); store.set('visPresetsAt', t); return newer } // the game saved config changes: they win
  return p })
ipcMain.handle('visp:set', (_, p) => { store.set('visPresets', p); store.set('visPresetsAt', Date.now()); for (const d of instanceDirs()) writePresets(d, p) })
// share codes: AST1:<base64url(deflate(json))>; short codes go through the backend (/vcfg)
const PERSONAL = ['pet', 'music', 'nameprotect', 'killmsg', 'markers']
const visCode = (cfg) => { const c = { ...cfg, modules: { ...(cfg?.modules || {}) } }; for (const k of PERSONAL) delete c.modules[k]; return 'AST1:' + zlib.deflateRawSync(Buffer.from(JSON.stringify(c))).toString('base64url') }
const visDecode = (s) => { try { s = String(s || '').trim(); if (!s.startsWith('AST1:')) return null; const o = JSON.parse(zlib.inflateRawSync(Buffer.from(s.slice(5), 'base64url')).toString()); return o && o.modules ? o : null } catch { return null } }
ipcMain.handle('vis:code', (_, cfg) => { const c = visCode(cfg); clipboard.writeText(c); return c })
ipcMain.handle('vis:share', async (_, cfg) => { const code = visCode(cfg); await resolveApi().catch(() => {})
  const r = await net.fetch(apiBase() + '/vcfg', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ code }), signal: AbortSignal.timeout(6000) }).then((x) => x.json()).catch(() => null)
  const out = r?.id || code; clipboard.writeText(out); return { id: r?.id || null, code } })
// ---------- sharing with a friend: a 6-letter code for a mod pack, skin, cape or crosshair
const longShare = (kind, data) => 'ASH1:' + zlib.deflateRawSync(Buffer.from(JSON.stringify({ kind, data }))).toString('base64url')
ipcMain.handle('share:make', async (_, kind, data) => { await resolveApi().catch(() => {})
  const r = await net.fetch(apiBase() + '/share', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ kind, data }), signal: AbortSignal.timeout(20000) }).then((x) => x.ok ? x.json() : null).catch(() => null)
  if (r?.id) { clipboard.writeText(r.id); return { id: r.id } }
  const code = longShare(kind, data); clipboard.writeText(code); return { id: code, long: true } })
ipcMain.handle('share:get', async (_, code) => { const raw = String(code || '').trim()
  if (raw.startsWith('ASH1:')) { try { return JSON.parse(zlib.inflateRawSync(Buffer.from(raw.slice(5), 'base64url')).toString()) } catch { return { error: 'Код повреждён — скопируйте его целиком' } } }
  const t = raw.toUpperCase(); if (!/^[A-Z0-9]{6}$/.test(t)) return { error: 'Код — 6 букв и цифр (или длинный код ASH1:…)' }
  await resolveApi().catch(() => {}); const r = await net.fetch(`${apiBase()}/share/${t}`, { signal: AbortSignal.timeout(15000) }).then((x) => x.ok ? x.json() : null).catch(() => null)
  return r || { error: 'Код не найден' } })
/** what a version has from Modrinth: mods, resource packs, shaders (slugs) */
ipcMain.handle('pack:export', (_, ver) => { const d = inst(settings(), ver), ls = (sub, re) => { try { return fs.readdirSync(path.join(d, sub)).map((f) => (f.match(re) || [])[1]).filter(Boolean) } catch { return [] } }
  return { ver, mods: ls('mods', /^mod-(.+)\.jar$/).filter((x) => !DEFAULTS.includes(x) && x !== 'fabric-api'), packs: ls('resourcepacks', /^(.+)\.zip$/), shaders: ls('shaderpacks', /^(.+)\.zip$/) } })
// ---------- markers (instances/<v>/config/asteria-marks.json, the mod reads it every couple of seconds) and the own crosshair picture
// ---------- own 3D models (pet / hat / back / wings): Blockbench .bbmodel or a resource pack .zip with an item model
const MODEL_SLOTS = ['pet', 'hat', 'back', 'wings']
ipcMain.handle('model:import', async (_, slot) => { if (!MODEL_SLOTS.includes(slot)) return { error: 'bad slot' }
  const r = await dialog.showOpenDialog(win, { properties: ['openFile', 'openDirectory'], filters: [{ name: 'Модель Blockbench, ресурспак или папка аватара Figura', extensions: ['bbmodel', 'zip', 'json'] }] }); let f = r.filePaths[0]; if (!f) return null
  if (fs.statSync(f).isDirectory()) { const all = []; const walk = (d) => { for (const e of fs.readdirSync(d, { withFileTypes: true })) { const p = path.join(d, e.name); if (e.isDirectory()) walk(p); else if (/.bbmodel$/i.test(e.name)) all.push([p, fs.statSync(p).size]) } }; walk(f)
    if (!all.length) return { error: 'В папке нет .bbmodel' }; f = all.sort((a, b) => b[1] - a[1])[0][0] } // a Figura avatar folder: its biggest model
  let m; try { m = require('./models.cjs').convert(fs.readFileSync(f), f, slot) } catch (e) { return { error: 'Не удалось прочитать модель: ' + (e?.message || e) } }
  const json = JSON.stringify(m); if (json.length > 1_800_000) return { error: 'Модель слишком большая (больше 1.8 МБ)' }
  const d = path.join(settings().dir, 'skins', 'models'); fs.mkdirSync(d, { recursive: true }); fs.writeFileSync(path.join(d, slot + '.json'), json)
  const meta = store.get('models', {}); meta[slot] = { name: path.basename(f).replace(/\.\w+$/, ''), polys: m.polys.length }; store.set('models', meta)
  for (const dir of instanceDirs()) { try { fs.mkdirSync(path.join(dir, 'config'), { recursive: true }); fs.writeFileSync(path.join(dir, 'config', 'asteria-model-' + slot + '.json'), json) } catch {} }
  let uploaded = false; try { const acc = store.get('accounts', []).find((a) => a.id === store.get('active')); await resolveApi()
    const tok = acc ? await getToken(acc.name).catch(() => null) : null; const res = acc && await fetch(apiBase() + '/model', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ name: acc.name, token: tok, slot, data: json }), signal: AbortSignal.timeout(20000) }).then((x) => x.json()).catch(() => null); uploaded = !!res?.ok } catch {}
  return { ok: true, name: meta[slot].name, polys: m.polys.length, uploaded } })
ipcMain.handle('model:list', () => store.get('models', {}))
ipcMain.handle('model:remove', (_, slot) => { const meta = store.get('models', {}); delete meta[slot]; store.set('models', meta); fs.rmSync(path.join(settings().dir, 'skins', 'models', slot + '.json'), { force: true })
  for (const dir of instanceDirs()) fs.rmSync(path.join(dir, 'config', 'asteria-model-' + slot + '.json'), { force: true }); return true })
ipcMain.handle('bg:set', (_, orig, blur) => { const dec = (d) => d ? Buffer.from(String(d).split(',')[1], 'base64') : null, a = dec(orig), b = dec(blur)
  for (const dir of instanceDirs()) { const c = path.join(dir, 'config'); try { fs.mkdirSync(c, { recursive: true }); for (const [n, buf] of [['asteria-bg.png', a], ['asteria-bg-blur.png', b]]) buf ? fs.writeFileSync(path.join(c, n), buf) : fs.rmSync(path.join(c, n), { force: true }) } catch {} }
  const keep = path.join(app.getPath('userData'), 'menu-bg'); fs.mkdirSync(keep, { recursive: true }) // kept by the launcher too: new or reinstalled versions get it at launch
  for (const [n, buf] of [['asteria-bg.png', a], ['asteria-bg-blur.png', b]]) buf ? fs.writeFileSync(path.join(keep, n), buf) : fs.rmSync(path.join(keep, n), { force: true })
  orig ? store.set('menuBg', orig) : store.delete('menuBg'); return true })
app.whenReady().then(() => { try { const keep = path.join(app.getPath('userData'), 'menu-bg'), o = store.get('menuBg', null); if (o && !fs.existsSync(path.join(keep, 'asteria-bg.png'))) { fs.mkdirSync(keep, { recursive: true }); fs.writeFileSync(path.join(keep, 'asteria-bg.png'), Buffer.from(String(o).split(',')[1], 'base64')) } } catch {} }) // a background set before this version
// ------------------------------------------------------------------ the announcer: phrases spoken by a Windows voice (WinRT, offline), WAV files the mod plays
function voiceRun(env) { return new Promise((ok, no) => { const script = fs.readFileSync(path.join(__dirname, 'voice.ps1'), 'utf8'), enc = Buffer.from(script, 'utf16le').toString('base64')
  require('child_process').execFile('powershell.exe', ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', enc], { env: { ...process.env, ...env }, windowsHide: true, maxBuffer: 4 << 20, timeout: 240000 },
    (e, out, err) => e ? no(new Error(String(err || e.message).slice(0, 300))) : ok(String(out))) }) }
const voiceDir = () => path.join(app.getPath('userData'), 'voice')
function writeVoice(dir) { const src = voiceDir(), dst = path.join(dir, 'config', 'asteria-voice'); try { fs.rmSync(dst, { recursive: true, force: true }); if (fs.existsSync(src)) fs.cpSync(src, dst, { recursive: true }) } catch {} }
ipcMain.handle('voice:list', async () => { try { const out = await voiceRun({ AV_MODE: 'list' }); const l = JSON.parse(out.trim().replace(/^\uFEFF/, '') || '[]'); return { voices: Array.isArray(l) ? l : [l], chosen: store.get('voice', null) } } catch (e) { return { error: 'Голоса Windows недоступны: ' + e.message, voices: [] } } })
ipcMain.handle('voice:test', async (_, o) => { const tmp = path.join(app.getPath('temp'), 'asteria-voice-test'); fs.rmSync(tmp, { recursive: true, force: true }); fs.mkdirSync(tmp, { recursive: true })
  fs.writeFileSync(path.join(tmp, 'in.json'), JSON.stringify([{ id: 'test', text: String(o.text || 'Функция «Траектория» включена') }]))
  try { await voiceRun({ AV_MODE: 'make', AV_VOICE: String(o.voice || ''), AV_RATE: String(+o.rate || 1), AV_PITCH: String(+o.pitch || 1), AV_IN: path.join(tmp, 'in.json'), AV_OUT: tmp })
    return 'data:audio/wav;base64,' + fs.readFileSync(path.join(tmp, 'test.wav')).toString('base64') } catch (e) { return null } })
ipcMain.handle('voice:make', async (_, o) => { const out = voiceDir(), tmp = out + '-new'; fs.rmSync(tmp, { recursive: true, force: true }); fs.mkdirSync(tmp, { recursive: true })
  const items = (o.items || []).filter((x) => /^[a-z0-9_]{1,40}$/.test(x.id) && x.text).map((x) => ({ id: x.id, text: String(x.text).slice(0, 120) }))
  fs.writeFileSync(path.join(tmp, 'in.json'), JSON.stringify(items))
  try { await voiceRun({ AV_MODE: 'make', AV_VOICE: String(o.voice || ''), AV_RATE: String(+o.rate || 1), AV_PITCH: String(+o.pitch || 1), AV_IN: path.join(tmp, 'in.json'), AV_OUT: tmp }) } catch (e) { return { error: 'Не получилось озвучить: ' + e.message } }
  fs.rmSync(path.join(tmp, 'in.json'), { force: true }); fs.rmSync(out, { recursive: true, force: true }); fs.renameSync(tmp, out)
  store.set('voice', { voice: o.voice, rate: o.rate, pitch: o.pitch }); for (const d of instanceDirs()) writeVoice(d)
  return { ok: true, count: fs.readdirSync(out).length } })
ipcMain.handle('voice:clear', () => { fs.rmSync(voiceDir(), { recursive: true, force: true }); store.delete('voice'); for (const d of instanceDirs()) writeVoice(d); return true })
/** own 3D models into a game folder (new or reinstalled versions get them too) */
function writeModels(dir) { const src = path.join(settings().dir, 'skins', 'models'); for (const slot of ['pet', 'hat', 'back', 'wings']) { const a = path.join(src, slot + '.json'), b = path.join(dir, 'config', 'asteria-model-' + slot + '.json'); try { if (fs.existsSync(a)) { fs.mkdirSync(path.dirname(b), { recursive: true }); fs.copyFileSync(a, b) } else fs.rmSync(b, { force: true }) } catch {} } }
/** own menu background into a game folder (or removed from it) */
function writeBg(dir) { const keep = path.join(app.getPath('userData'), 'menu-bg'), c = path.join(dir, 'config'); try { fs.mkdirSync(c, { recursive: true })
  for (const n of ['asteria-bg.png', 'asteria-bg-blur.png']) { const src = path.join(keep, n); if (fs.existsSync(src)) fs.copyFileSync(src, path.join(c, n)); else fs.rmSync(path.join(c, n), { force: true }) } } catch {} }
ipcMain.handle('bg:get', () => store.get('menuBg', null))
ipcMain.handle('binds:get', () => store.get('cmdBinds', []))
ipcMain.handle('binds:set', (_, list) => { store.set('cmdBinds', list); for (const d of instanceDirs()) { try { fs.mkdirSync(path.join(d, 'config'), { recursive: true }); fs.writeFileSync(path.join(d, 'config', 'asteria-binds.json'), JSON.stringify(list)) } catch {} } return true })
ipcMain.handle('marks:get', (_, ver) => { try { return JSON.parse(fs.readFileSync(path.join(inst(settings(), ver), 'config', 'asteria-marks.json'), 'utf8')) } catch { return {} } })
ipcMain.handle('marks:set', (_, ver, data) => { for (const dir of [inst(settings(), ver), ...instanceDirs()]) { const d = path.join(dir, 'config'); try { fs.mkdirSync(d, { recursive: true }); fs.writeFileSync(path.join(d, 'asteria-marks.json'), JSON.stringify(data)) } catch {} } return true }) // the same marks in every version
ipcMain.handle('xh:set', (_, data) => { const png = data ? Buffer.from(String(data).split(',')[1], 'base64') : null
  for (const d of instanceDirs()) { const f = path.join(d, 'config', 'asteria-crosshair.png'); try { fs.mkdirSync(path.dirname(f), { recursive: true }); png ? fs.writeFileSync(f, png) : fs.rmSync(f, { force: true }) } catch {} }
  png ? store.set('crosshairPng', data) : store.delete('crosshairPng'); return true })
ipcMain.handle('xh:get', () => store.get('crosshairPng', null))
ipcMain.handle('vis:decode', async (_, text) => { const t = String(text ?? clipboard.readText()).trim()
  if (/^[A-Z0-9]{6}$/i.test(t)) { await resolveApi().catch(() => {}); const r = await net.fetch(`${apiBase()}/vcfg/${t.toUpperCase()}`, { signal: AbortSignal.timeout(6000) }).then((x) => x.ok ? x.json() : null).catch(() => null); return r ? visDecode(r.code) : null }
  return visDecode(t) })
ipcMain.handle('vis:export', async (_, cfg, name) => { const r = await dialog.showSaveDialog(win, { defaultPath: String(name || 'config').replace(/[\/:*?"<>|]/g, '_') + '.asteria', filters: [{ name: 'Asteria config', extensions: ['asteria'] }] })
  if (!r.filePath) return false; fs.writeFileSync(r.filePath, visCode(cfg)); return true })
ipcMain.handle('vis:import', async () => { const r = await dialog.showOpenDialog(win, { filters: [{ name: 'Asteria config', extensions: ['asteria', 'json', 'txt'] }] }); const f = r.filePaths[0]; if (!f) return null
  const t = fs.readFileSync(f, 'utf8'); let o = visDecode(t); if (!o) try { o = JSON.parse(t); if (!o.modules) o = null } catch {}; return o ? { ...o, name: o.name || path.basename(f).replace(/\.\w+$/, '') } : { error: true } })
// lets offline accounts open Multiplayer. The services host stays real: the game takes Mojang's public key from it to verify
// signed skins of other players and NPCs (without it everyone looks like Steve); a bad token there just falls back to offline mode.
const OFFLINE = ['auth', 'account'].map((h) => `-Dminecraft.api.${h}.host=https://invalid.invalid`)
const LICENSED = [['auth', 'https://authserver.mojang.com'], ['account', 'https://api.mojang.com']].map(([h, u]) => `-Dminecraft.api.${h}.host=${u}`)
// Mojang's session and services hosts are often unreachable directly (Russia), and the game ignores the Windows proxy for them:
// without the public keys every signed skin (SkinsRestorer, licensed players, NPCs) is thrown away and everyone looks like Steve.
// The game talks to this local relay instead; the relay goes out through Electron's network stack (system proxy) and
// answers /publickeys from a bundled copy when Mojang can't be reached at all.
const UP = { session: 'https://sessionserver.mojang.com', services: 'https://api.minecraftservices.com' }
let relayPort = 0, keysCache = null
const relayStart = () => relayPort ? Promise.resolve(relayPort) : new Promise((ok) => {
  const srv = require('http').createServer(async (req, res) => {
    const m = /^\/(session|services)(\/.*)$/.exec(req.url || ''); if (!m) { res.writeHead(404); return res.end() }
    const body = await new Promise((r) => { const c = []; req.on('data', (d) => c.push(d)); req.on('end', () => r(Buffer.concat(c))) })
    // "is multiplayer allowed / is chat allowed": answered here, so an offline account (or Mojang being unreachable) never
    // makes Minecraft hide Multiplayer or refuse the "Join SafiMine" quick play
    if (m[1] === 'services' && /^\/(privileges|player\/attributes|privacy\/blocklist)/.test(m[2])) {
      const ok = m[2].startsWith('/privacy') ? { blockedProfiles: [] } : { privileges: { onlineChat: { enabled: true }, multiplayerServer: { enabled: true }, multiplayerRealms: { enabled: false }, telemetry: { enabled: false } }, profanityFilterPreferences: { profanityFilterOn: false } }
      res.writeHead(200, { 'content-type': 'application/json' }); return res.end(JSON.stringify(ok)) }
    const keys = m[1] === 'services' && m[2].startsWith('/publickeys')
    if (keys && keysCache) { res.writeHead(200, { 'content-type': 'application/json' }); return res.end(keysCache) }
    try {
      const h = {}; for (const k of ['content-type', 'authorization', 'accept']) if (req.headers[k]) h[k] = req.headers[k]
      const r = await net.fetch(UP[m[1]] + m[2], { method: req.method, headers: h, body: ['GET', 'HEAD'].includes(req.method) ? undefined : body, signal: AbortSignal.timeout(10000) })
      const buf = Buffer.from(await r.arrayBuffer()); if (keys && r.ok) keysCache = buf
      res.writeHead(r.status, { 'content-type': r.headers.get('content-type') || 'application/json' }); res.end(buf)
    } catch {
      if (keys) { res.writeHead(200, { 'content-type': 'application/json' }); return res.end(fs.readFileSync(path.join(__dirname, 'mojang-keys.json'))) }
      res.writeHead(503); res.end()
    }
  })
  srv.on('error', () => ok(0)); srv.listen(0, '127.0.0.1', () => { relayPort = srv.address().port; ok(relayPort) })
})
const relayArgs = (port) => port ? [`-Dminecraft.api.session.host=http://127.0.0.1:${port}/session`, `-Dminecraft.api.services.host=http://127.0.0.1:${port}/services`] : []
const ENC = encodeURIComponent
const mrJ = async (u) => (await net.fetch(u, { headers: { 'user-agent': 'AsteriaLauncher/1.0' } })).json()
const instKey = (ver) => { const a = store.get('activeProfile'); return a && a.version === ver ? a.id : ver }
const inst = (s, ver) => path.join(s.dir, 'instances', instKey(ver))
const instOf = (key) => path.join(settings().dir, 'instances', path.basename(String(key)))
ipcMain.handle('mods:targets', () => { const profs = store.get('profiles', []), out = [], seen = new Set()
  for (const p of profs) { out.push({ key: p.id, version: p.version, name: p.name, profile: true }); seen.add(p.id) }
  for (const d of instanceDirs()) { const k = path.basename(d); if (!seen.has(k) && /^\d/.test(k)) out.push({ key: k, version: k, name: 'Minecraft ' + k, profile: false }) }
  return out })
ipcMain.handle('mods:installTo', async (_, slug, targets) => { const res = []
  for (const t of targets || []) { try { const md = path.join(instOf(t.key), 'mods'); fs.mkdirSync(md, { recursive: true })
    res.push({ key: t.key, ok: await installSlug(slug, t.version, md) }) } catch { res.push({ key: t.key, ok: false }) } }
  return res })
const DEFAULTS = ['sodium', 'lithium', 'entityculling', 'ferrite-core', 'iris', 'krypton', 'immediatelyfast', 'modernfix', 'modmenu']
async function installSlug(slug, ver, md, depth = 0) { // installs a Fabric mod + its required dependencies
  const f = path.join(md, `mod-${slug}.jar`)
  if (fs.existsSync(f) || fs.existsSync(f + '.disabled')) return true
  const v = (await mrJ(`https://api.modrinth.com/v2/project/${slug}/version?game_versions=${ENC(JSON.stringify([ver]))}&loaders=${ENC('["fabric"]')}`))[0]
  if (!v) return false
  const file = v.files.find((x) => x.primary) || v.files[0]
  fs.writeFileSync(f, Buffer.from(await (await net.fetch(file.url)).arrayBuffer()))
  { const hs = store.get('hashes', {}); hs[f] = file.hashes?.sha1; store.set('hashes', hs) }
  if (depth < 2) for (const d of v.dependencies || []) if (d.dependency_type === 'required' && d.project_id) {
    const p = await mrJ(`https://api.modrinth.com/v2/project/${d.project_id}`); await installSlug(p.slug, ver, md, depth + 1).catch(() => {}) }
  return true
}
/** a Fabric profile already downloaded earlier (newest loader), used when the Fabric servers can't be reached */
function cachedFabric(s, ver, legacy) { try { const pre = `${legacy ? 'legacy-' : ''}fabric-loader-`
  return fs.readdirSync(path.join(s.dir, 'versions')).filter((n) => n.startsWith(pre) && n.endsWith('-' + ver) && fs.existsSync(path.join(s.dir, 'versions', n, n + '.json'))).sort().pop() || null } catch { return null } }
async function prepareFabric(s, ver, send, legacy = false) { // legacy = Legacy Fabric for 1.8.9 / 1.12.2 (only our own mods there)
  const meta = legacy ? 'https://meta.legacyfabric.net' : 'https://meta.fabricmc.net'
  let id
  try {
    const loaders = await mrJ(`${meta}/v2/versions/loader/${ver}`)
    if (!loaders.length) return cachedFabric(s, ver, legacy)
    id = `${legacy ? 'legacy-' : ''}fabric-loader-${loaders[0].loader.version}-${ver}`; const vd = path.join(s.dir, 'versions', id)
    if (!fs.existsSync(path.join(vd, id + '.json'))) { fs.mkdirSync(vd, { recursive: true })
      const prof = await mrJ(`${meta}/v2/versions/loader/${ver}/${loaders[0].loader.version}/profile/json`); prof.id = id
      fs.writeFileSync(path.join(vd, id + '.json'), JSON.stringify(prof)) }
  } catch { id = cachedFabric(s, ver, legacy); if (!id) throw new Error('fabric meta unreachable') } // Fabric servers down: keep the profile we have, mods below still come from Modrinth
  const md = path.join(inst(s, ver), 'mods'); fs.mkdirSync(md, { recursive: true })
  if (legacy) return id
  send('Fabric API', 40); await installSlug('fabric-api', ver, md).catch(() => {})
  { // optimization mods (Sodium, Lithium...): while "Optimization" is on, any of them that is missing is downloaded again; off = switched off (.disabled)
    const on = s.optimize !== false, file = (slug) => path.join(md, `mod-${slug}.jar`)
    for (const slug of DEFAULTS) {
      if (on) { if (fs.existsSync(file(slug) + '.disabled')) fs.renameSync(file(slug) + '.disabled', file(slug))
        if (!fs.existsSync(file(slug))) { send('Mods: ' + slug, 60); await installSlug(slug, ver, md).catch(() => undefined) } }
      else if (fs.existsSync(file(slug))) fs.renameSync(file(slug), file(slug) + '.disabled') } }
  return id
}
const trCache = new Map()
async function translate(text, lang) { if (!text || !lang || lang === 'en') return text; const k = lang + '|' + text; if (trCache.has(k)) return trCache.get(k)
  try { const r = await net.fetch(`https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=${ENC(lang)}&dt=t&q=${ENC(text.slice(0, 4500))}`, { signal: AbortSignal.timeout(8000) }).then((x) => x.json())
    const out = (r[0] || []).map((p) => p[0]).join(''); trCache.set(k, out); return out } catch { return text } }
/** interface texts: Russian -> lang, many at once, remembered between launches */
// several services: one of them may be blocked or slow for the player; null = nothing answered (the window asks again later)
const TR_SERVICES = [
  async (t, l) => { const r = await net.fetch(`https://translate.googleapis.com/translate_a/single?client=gtx&sl=ru&tl=${ENC(l)}&dt=t&q=${ENC(t)}`, { signal: AbortSignal.timeout(7000) }).then((x) => x.json()); return (r[0] || []).map((p) => p[0]).join('') },
  async (t, l) => { const r = await net.fetch(`https://lingva.ml/api/v1/ru/${ENC(l)}/${ENC(t)}`, { signal: AbortSignal.timeout(7000) }).then((x) => x.json()); return r.translation || '' },
  async (t, l) => { const r = await net.fetch(`https://api.mymemory.translated.net/get?q=${ENC(t)}&langpair=ru|${ENC(l)}`, { signal: AbortSignal.timeout(7000) }).then((x) => x.json()); return r.responseStatus === 200 ? r.responseData?.translatedText || '' : '' },
]
let trBest = 0
async function trUi(text, lang) { const mem = store.get('trui:' + lang, {}); if (mem[text]) return mem[text]
  for (let k = 0; k < TR_SERVICES.length; k++) { const i = (trBest + k) % TR_SERVICES.length
    try { const out = String(await TR_SERVICES[i](text, lang) || '').trim(); if (out && out !== text) { trBest = i; const m2 = store.get('trui:' + lang, {}); m2[text] = out; store.set('trui:' + lang, m2); return out } } catch {} }
  return null }
ipcMain.handle('tr:batch', async (_, list, lang) => { const out = new Array(list.length); let i = 0
  const worker = async () => { while (i < list.length) { const k = i++; out[k] = await trUi(String(list[k]), String(lang)) } }
  await Promise.all(Array.from({ length: 6 }, worker)); return out })
const plain = (md) => String(md || '').replace(/<[^>]+>/g, ' ').replace(/!\[[^\]]*\]\([^)]*\)/g, '').replace(/\[([^\]]*)\]\([^)]*\)/g, '$1').replace(/[#*_`>|]/g, '').replace(/\n{3,}/g, '\n\n').trim()
ipcMain.handle('mr:project', async (_, slug, lang) => { try { const p = await mrJ(`https://api.modrinth.com/v2/project/${ENC(slug)}`), body = plain(p.body).slice(0, 2500)
  const [desc, text] = await Promise.all([translate(p.description, lang), translate(body, lang)])
  return { title: p.title, desc, text, gallery: (p.gallery || []).map((g) => g.url).slice(0, 8), downloads: p.downloads, followers: p.followers, categories: p.categories, icon: p.icon_url, updated: p.updated } } catch { return null } })
ipcMain.handle('mods:search', async (_, { q, ver, sort, cat, pvp }) => {
  try { const fa = [['project_type:mod'], ['categories:fabric'], ['versions:' + ver]]; if (cat) fa.push(['categories:' + cat])
    return (await mrJ(`https://api.modrinth.com/v2/search?query=${ENC(((q || '') + (pvp ? ' pvp' : '')).trim())}&facets=${ENC(JSON.stringify(fa))}&index=${sort || 'downloads'}&limit=20`)).hits } catch { return [] }
})
ipcMain.handle('img:get', async (_, url) => { // fetch via Chromium network stack (honours system proxy/VPN), cache on disk
  if (!/^https:\/\//.test(url) && !String(url).startsWith(apiBase() + '/img/')) return null // https only (plus pictures from your own backend)
  const dir = path.join(app.getPath('userData'), 'imgcache'); fs.mkdirSync(dir, { recursive: true })
  const f = path.join(dir, crypto.createHash('sha1').update(url).digest('hex'))
  try { if (fs.existsSync(f)) return fs.readFileSync(f, 'utf8')
    const r = await net.fetch(url, { signal: AbortSignal.timeout(25000) }); if (!r.ok) return null
    if (!(r.headers.get('content-type') || '').startsWith('image/')) return null
    const d = `data:${r.headers.get('content-type') || 'image/png'};base64,` + Buffer.from(await r.arrayBuffer()).toString('base64')
    fs.writeFileSync(f, d); return d } catch { return null }
})
ipcMain.handle('sys:info', () => ({ totalMb: Math.floor(os.totalmem() / 1048576) }))
ipcMain.handle('mods:install', async (_, slug, ver) => {
  try { const md = path.join(inst(settings(), ver), 'mods'); fs.mkdirSync(md, { recursive: true })
    return (await installSlug(slug, ver, md)) ? { ok: true } : { error: 'Нет версии под ' + ver } } catch { return { error: 'Ошибка загрузки' } }
})
// own mods: jars the player added by hand. Name, description and icon come from the jar itself (fabric.mod.json / quilt.mod.json / mods.toml)
const JMETA = new Map()
function jarMeta(f) { try { const st = fs.statSync(f), k = f + ':' + st.mtimeMs; if (JMETA.has(k)) return JMETA.get(k)
    const AdmZip = require('adm-zip'), z = new AdmZip(f), rd = (n) => { const e = z.getEntry(n); return e ? z.readAsText(e) : null }
    let m = null, raw = rd('fabric.mod.json')
    if (raw) { const j = JSON.parse(raw.replace(/^\uFEFF/, '').replace(/[\u0000-\u001f]+/g, ' ')); m = { title: j.name || j.id, desc: j.description || '', version: j.version, mc: j.depends?.minecraft, icon: typeof j.icon === 'string' ? j.icon : j.icon && Object.values(j.icon).pop(), loader: 'Fabric' } }
    else if ((raw = rd('quilt.mod.json'))) { const j = JSON.parse(raw).quilt_loader || {}; m = { title: j.metadata?.name || j.id, desc: j.metadata?.description || '', version: j.version, icon: j.metadata?.icon, loader: 'Quilt' } }
    else if ((raw = rd('META-INF/mods.toml') || rd('META-INF/neoforge.mods.toml'))) { const g = (k) => (raw.match(new RegExp('^\\s*' + k + '\\s*=\\s*"([^"]*)"', 'm')) || [])[1]; m = { title: g('displayName') || g('modId'), desc: (g('description') || '').trim(), version: g('version'), icon: g('logoFile'), loader: 'Forge' } }
    if (m && m.icon) { const e = z.getEntry(String(m.icon).replace(/^\//, '')); m.icon = e ? 'data:image/png;base64,' + e.getData().toString('base64') : undefined }
    JMETA.set(k, m); return m } catch { return null } }
const myModsDir = () => { const d = path.join(app.getPath('userData'), 'my-mods'); fs.mkdirSync(d, { recursive: true }); return d }
const isJar = (f) => { try { const b = Buffer.alloc(2), fd = fs.openSync(f, 'r'); fs.readSync(fd, b, 0, 2, 0); fs.closeSync(fd); return b.toString() === 'PK' } catch { return false } }
/** adds jars to "My mods" (kept in the launcher) and installs them into the given builds */
ipcMain.handle('mods:upload', async (_, paths, targets) => {
  if (!paths) { const r = await dialog.showOpenDialog(win, { title: 'Свои моды', properties: ['openFile', 'multiSelections'], filters: [{ name: 'Моды (.jar)', extensions: ['jar'] }] }); paths = r.filePaths }
  const added = [], bad = []
  for (const f of paths || []) { const n = path.basename(f); if (!/\.jar$/i.test(n) || !isJar(f)) { bad.push(n); continue }
    fs.copyFileSync(f, path.join(myModsDir(), n)); added.push(n)
    for (const t of targets || []) { try { const md = path.join(instOf(t), 'mods'); fs.mkdirSync(md, { recursive: true }); fs.copyFileSync(f, path.join(md, n)) } catch {} } }
  return { added, bad } })
ipcMain.handle('mods:mine', (_, key) => { const md = path.join(instOf(key), 'mods'), d = myModsDir()
  return fs.readdirSync(d).filter((n) => /\.jar$/i.test(n)).map((file) => ({ file, ...(jarMeta(path.join(d, file)) || { title: file.replace(/\.jar$/i, ''), desc: '' }), size: fs.statSync(path.join(d, file)).size, installed: fs.existsSync(path.join(md, file)) || fs.existsSync(path.join(md, file + '.disabled')) })) })
ipcMain.handle('mods:mineInstall', (_, file, targets) => { const f = path.join(myModsDir(), path.basename(file)); for (const t of targets || []) { const md = path.join(instOf(t), 'mods'); fs.mkdirSync(md, { recursive: true }); fs.copyFileSync(f, path.join(md, path.basename(file))) } })
ipcMain.handle('mods:mineRemove', (_, file) => fs.rmSync(path.join(myModsDir(), path.basename(file)), { force: true }))
ipcMain.handle('mods:openMine', () => shell.openPath(myModsDir()))
ipcMain.handle('mods:list', async (_, ver) => {
  const md = path.join(inst(settings(), ver), 'mods'); if (!fs.existsSync(md)) return []
  const meta = store.get('meta', {}), out = []
  for (const file of fs.readdirSync(md).filter((n) => /\.jar(\.disabled)?$/.test(n) && !/^asteria-/.test(n))) {
    const slug = (file.match(/^mod-(.+?)\.jar/) || [])[1]
    if (slug && !meta[slug]) { try { const p = await mrJ(`https://api.modrinth.com/v2/project/${slug}`); meta[slug] = { title: p.title, icon: p.icon_url, desc: p.description } } catch {} }
    const own = !slug || !meta[slug], jm = own ? jarMeta(path.join(md, file)) : null
    out.push({ file, slug, own, enabled: !file.endsWith('.disabled'), ...(meta[slug] || jm || { title: file.replace(/\.jar(\.disabled)?$/, ''), desc: '' }) })
  }
  store.set('meta', meta); return out
})
ipcMain.handle('mods:toggle', (_, file, ver) => { const d = path.join(inst(settings(), ver), 'mods'), f = path.join(d, path.basename(file))
  fs.renameSync(f, f.endsWith('.disabled') ? f.slice(0, -9) : f + '.disabled') })
ipcMain.handle('mods:remove', (_, file, ver) => fs.rmSync(path.join(inst(settings(), ver), 'mods', path.basename(file)), { force: true }))
ipcMain.handle('cosm:set', (_, c) => { store.set('cosmetics', c); beatNow?.() })
app.whenReady().then(() => { create(); poll().then(() => !lastSt?.online && setTimeout(poll, 4000)); setInterval(poll, 20000) })
ipcMain.on('win', (_, a) => { if (a === 'min') win.minimize(); else if (a === 'max') win.isMaximized() ? win.unmaximize() : win.maximize(); else win.close() })
ipcMain.handle('link', (_, u) => LINKS.test(u) && shell.openExternal(u))
ipcMain.handle('settings:get', settings)
ipcMain.handle('settings:set', (_, s) => { store.set('settings', s); return settings() })
ipcMain.handle('acc:list', () => ({ list: store.get('accounts', []).map(pub), active: store.get('active') }))
ipcMain.handle('acc:offline', (_, name) => {
  const a = { id: 'off-' + Date.now(), name, type: 'offline', auth: offlineAuth(name) }
  store.set('accounts', [...store.get('accounts', []), a]); store.set('active', a.id); return a.id
})
ipcMain.handle('acc:ms', async () => { try {
  const { Auth } = require('msmc') // msmc v5 API; adjust if you install another major
  const x = await new Auth('select_account').launch('electron')
  const mc = await x.getMinecraft()
  const a = { id: 'ms-' + mc.profile.id, name: mc.profile.name, type: 'microsoft', auth: mc.mclc(), refresh: x.save() }
  store.set('accounts', [...store.get('accounts', []).filter((v) => v.id !== a.id), a]); store.set('active', a.id); return a.id
  } catch (e) { const m = String(e?.message || e || '')
    if (/gui\.closed|closed|cancel/i.test(m)) return { cancelled: true, info: 'Вход через Microsoft отменён' }
    if (/no\.?mc|not.?own|xbox|child|ban/i.test(m)) return { error: 'На этом аккаунте Microsoft нет лицензии Minecraft (или он не прошёл проверку Xbox)' }
    return { error: 'Не удалось войти через Microsoft: ' + m } }
})
const TEX_FILES = ['current.png', 'cape.png', 'mini.png', 'slim.flag']
const accTexDir = (name) => path.join(settings().dir, 'skins', 'accounts', String(name || '_').toLowerCase().replace(/[^a-z0-9_]/g, '_'))
/** puts the textures of the active account where the game and the launcher read them */
function useAccountTextures() { try { const acc = store.get('accounts', []).find((a) => a.id === store.get('active')); if (!acc) return
  const src = accTexDir(acc.name), dst = path.join(settings().dir, 'skins'); fs.mkdirSync(dst, { recursive: true })
  if (!fs.existsSync(src)) { const others = fs.readdirSync(path.dirname(src)).length; fs.mkdirSync(src, { recursive: true })
    if (!others) { for (const f of TEX_FILES) if (fs.existsSync(path.join(dst, f))) fs.copyFileSync(path.join(dst, f), path.join(src, f)); return } // the very first account keeps the files it already had
    for (const f of TEX_FILES) fs.rmSync(path.join(dst, f), { force: true }); return } // a new account starts clean: it never gets the skin / cape of another one
  for (const f of TEX_FILES) { const a = path.join(src, f), b = path.join(dst, f); if (fs.existsSync(a)) fs.copyFileSync(a, b); else fs.rmSync(b, { force: true }) } } catch {} }
ipcMain.handle('acc:select', (_, id) => { store.set('active', id); useAccountTextures(); return true })
ipcMain.handle('acc:textures', () => useAccountTextures())
ipcMain.handle('acc:remove', (_, id) => store.set('accounts', store.get('accounts', []).filter((a) => a.id !== id)))
ipcMain.handle('dir:open', () => shell.openPath(settings().dir))
ipcMain.handle('java:pick', async () => (await dialog.showOpenDialog(win, { filters: [{ name: 'java', extensions: ['exe'] }] })).filePaths[0])
ipcMain.handle('skin:pick', async () => {
  const r = await dialog.showOpenDialog(win, { filters: [{ name: 'PNG', extensions: ['png'] }] })
  if (!r.filePaths[0]) return null
  const f = path.join(settings().dir, 'skins'); fs.mkdirSync(f, { recursive: true })
  fs.copyFileSync(r.filePaths[0], path.join(f, 'current.png'))
  return 'data:image/png;base64,' + fs.readFileSync(r.filePaths[0]).toString('base64')
})
ipcMain.handle('skin:apply', async (_, variant) => { // Microsoft accounts only
  const acc = store.get('accounts', []).find((a) => a.id === store.get('active'))
  const f = path.join(settings().dir, 'skins', 'current.png')
  if (!acc || acc.type !== 'microsoft') return { error: 'Смена скина на сервере Mojang доступна только для Microsoft-аккаунта' }
  if (!fs.existsSync(f)) return { error: 'Сначала выберите PNG' }
  const fd = new FormData(); fd.append('variant', variant === 'slim' ? 'slim' : 'classic')
  fd.append('file', new Blob([fs.readFileSync(f)], { type: 'image/png' }), 'skin.png')
  try {
    const r = await fetch('https://api.minecraftservices.com/minecraft/profile/skins', { method: 'POST', headers: { Authorization: 'Bearer ' + acc.auth.access_token }, body: fd })
    return r.ok ? { ok: true } : { error: 'Mojang отклонил запрос (' + r.status + '). Войдите в аккаунт заново' }
  } catch { return { error: 'Нет связи с Mojang' } }
})
const LANGMAP = { ru: 'ru_ru', en: 'en_us', uk: 'uk_ua', pl: 'pl_pl', de: 'de_de', es: 'es_es', fr: 'fr_fr', it: 'it_it', pt: 'pt_br', tr: 'tr_tr', zh: 'zh_cn', ja: 'ja_jp', ko: 'ko_kr', nl: 'nl_nl', cs: 'cs_cz', ar: 'ar_sa', hi: 'hi_in', id: 'id_id', vi: 'vi_vn', th: 'th_th' }
function setOpts(dir, o) { // syncs the in-game language with the launcher language (options.txt)
  try { const f = path.join(dir, 'options.txt'); let t = fs.existsSync(f) ? fs.readFileSync(f, 'utf8') : ''
    for (const [k, v] of Object.entries(o)) { const re = new RegExp('^' + k + ':.*$', 'm'); t = re.test(t) ? t.replace(re, k + ':' + v) : (t ? t.replace(/\n?$/, '\n') : '') + k + ':' + v + '\n' }
    fs.writeFileSync(f, t) } catch {}
}
function findJava(s) { // javaw has no console window
  if (s.java) return s.java.replace(/java\.exe$/i, 'javaw.exe')
  try { return require('child_process').execFileSync('where', ['javaw'], { encoding: 'utf8' }).split(/\r?\n/)[0].trim() || undefined } catch { return undefined }
}
let game = null, gameVer = null, gameDirNow = null, logBuf = [], pendingOpts = {}
// "Now playing": a small PowerShell reader of the Windows media controls (electron/media.ps1) runs while the launcher is open.
// The track goes to the launcher UI ('media' events) and, while a game runs, to <game dir>/config/asteria-media.json (+ cover png);
// play/pause/next/prev come from the UI or from the mod (config/asteria-media-cmd.txt). -EncodedCommand: no script file, no policy prompt.
let mediaProc = null, media = { playing: false }, mediaCover = null, mediaCoverKey = '', coverWritten = {}, mediaRetry = 0
const mediaScript = () => fs.readFileSync(path.join(__dirname, 'media.ps1'), 'utf8')
const enc = (t) => Buffer.from(t, 'utf16le').toString('base64')
function liveGameDir() { if (game && gameDirNow) return gameDirNow; return extGame?.dir || null }
function mediaStart() {
  if (process.platform !== 'win32' || mediaProc) return
  try {
    mediaProc = require('child_process').spawn('powershell.exe', ['-NoProfile', '-NonInteractive', '-EncodedCommand', enc(mediaScript())], { windowsHide: true, env: { ...process.env, ASTERIA_MEDIA_CMD: mediaCmdFile() } }); let buf = ''
    mediaProc.stdout.setEncoding('utf8') // keeps Cyrillic letters split between chunks intact
    mediaProc.stdout.on('data', (d) => { buf += d; let i; while ((i = buf.indexOf('\n')) >= 0) { const line = buf.slice(0, i).trim(); buf = buf.slice(i + 1); try { mediaLine(JSON.parse(line)) } catch {} } })
    mediaProc.on('exit', () => { mediaProc = null; if (mediaRetry++ < 20) setTimeout(mediaStart, 5000) })
  } catch { mediaProc = null } }
function mediaLine(o) {
  mediaRetry = 0
  if (o.cover !== undefined) { mediaCover = o.cover ? Buffer.from(o.cover, 'base64') : null; mediaCoverKey = o.key || ''; delete o.cover }
  o.at = Date.now()
  // players report the position with ~0.5 s of noise (and lines arrive late while the launcher is busy): a small difference from the
  // running clock is pulled in gently instead of jumping back and forth; a seek or a new track (> 1.5 s off) still jumps at once
  if (o.playing && media.playing && o.key === media.key && o.dur === media.dur && typeof o.pos === 'number' && typeof media.pos === 'number') {
    const ran = media.pos + (o.at - media.at) / 1000, err = o.pos - ran; if (Math.abs(err) < 1.5) o.pos = ran + err * 0.08 }
  media = o
  try { win?.webContents.send('media', { ...o, cover: mediaCover ? 'data:image/png;base64,' + mediaCover.toString('base64') : '' }) } catch {}
  const dir = liveGameDir(); if (!dir) return
  try { const cfg = path.join(dir, 'config'); fs.mkdirSync(cfg, { recursive: true })
    if (coverWritten[dir] !== mediaCoverKey) { coverWritten[dir] = mediaCoverKey; const cf = path.join(cfg, 'asteria-media-cover.png'); if (mediaCover) fs.writeFileSync(cf, mediaCover); else fs.rmSync(cf, { force: true }) }
    fs.writeFileSync(path.join(cfg, 'asteria-media.json'), JSON.stringify({ ...o, cover: mediaCover ? mediaCoverKey : '' }))
  } catch {} }
// buttons / keys in the game: config/asteria-media-cmd.txt is checked every 100 ms (not only when the track info arrives)
setInterval(() => { const dir = liveGameDir(); if (!dir) return; const cmdF = path.join(dir, 'config', 'asteria-media-cmd.txt')
  try { if (fs.existsSync(cmdF)) { const c = fs.readFileSync(cmdF, 'utf8').trim(); fs.rmSync(cmdF, { force: true }); mediaCmd(c) } } catch {} }, 100)
const mediaCmdFile = () => path.join(app.getPath('userData'), 'media-cmd.txt')
/** play/pause/skip: the running reader picks the file up within 0.4 s and answers with the new track */
let mediaLast = { c: '', t: 0 }
function mediaCmd(c) {
  if (process.platform !== 'win32' || !['toggle', 'next', 'prev', 'volup', 'voldown', 'mute'].includes(c)) return
  const now = Date.now(); if (c === mediaLast.c && now - mediaLast.t < 400 && c !== 'volup' && c !== 'voldown') return; mediaLast = { c, t: now } // one press = one track, even if two buttons or keys fire at once
  try { fs.writeFileSync(mediaCmdFile(), c) } catch {} if (!mediaProc) mediaStart() }
ipcMain.handle('vis:enabled', (_, v) => { if (typeof v === 'boolean') store.set('visualsOn', v); return store.get('visualsOn', true) !== false })
ipcMain.handle('media:get', () => ({ ...media, cover: mediaCover ? 'data:image/png;base64,' + mediaCover.toString('base64') : '' }))
ipcMain.handle('media:cmd', (_, c) => mediaCmd(String(c)))
ipcMain.handle('game:kill', () => { if (!game && extGame) { try { process.kill(extGame.pid) } catch {} extGame = null; extAt = 0; try { win?.webContents.send('game', false) } catch {} return }
  try { game?.kill() } catch {} if (game && (game.exitCode !== null || game.killed)) { game = null; gameVer = null; try { win?.webContents.send('game', false) } catch {} } })
ipcMain.handle('game:log', () => logBuf.slice(-400).join('\n'))
ipcMain.handle('stats:get', () => ({ playMs: store.get('playMs', 0), launches: store.get('launches', 0), perVer: store.get('perVer', {}) }))
// ---------- "Download": a version is installed completely before "Play" (Java, game jar, libraries, assets, Fabric and its
// libraries, our mods and the optimization mods). "Update" shows up when the launcher brings newer mods / visuals.
let pf = { stop: false, job: null }
async function stopPrefetch() { pf.stop = true; const j = pf.job; if (j) await j.catch(() => {}); pf.stop = false }
async function dl(url, file, sha1) {
  if (fs.existsSync(file)) return
  const r = await net.fetch(url, { signal: AbortSignal.timeout(60000) }); if (!r.ok) throw new Error('HTTP ' + r.status)
  const b = Buffer.from(await r.arrayBuffer()); if (sha1 && crypto.createHash('sha1').update(b).digest('hex') !== sha1) throw new Error('sha1')
  fs.mkdirSync(path.dirname(file), { recursive: true }); const tmp = file + '.part'; fs.writeFileSync(tmp, b); fs.renameSync(tmp, file) // atomic: a launch never sees half a file
}
/** jobs [url, file, sha1] for one version json (vanilla or a Fabric profile with maven libraries) */
function filesOf(s, id) {
  const jf = path.join(s.dir, 'versions', id, id + '.json'); if (!fs.existsSync(jf)) return { jobs: [], index: null }
  const v = JSON.parse(fs.readFileSync(jf, 'utf8')), jobs = []
  if (v.downloads?.client) jobs.push([v.downloads.client.url, path.join(s.dir, 'versions', id, id + '.jar'), v.downloads.client.sha1])
  for (const lib of v.libraries || []) { const a = lib.downloads?.artifact
    if (a?.url && a.path) jobs.push([a.url, path.join(s.dir, 'libraries', a.path), a.sha1])
    else if (lib.name && lib.url) { const [g, n, ver] = lib.name.split(':'), p = `${g.replace(/\./g, '/')}/${n}/${ver}/${n}-${ver}.jar`; jobs.push([lib.url.replace(/\/?$/, '/') + p, path.join(s.dir, 'libraries', p), lib.sha1]) }
    const nk = lib.natives?.windows?.replace('${arch}', '64'), nat = nk && lib.downloads?.classifiers?.[nk]; if (nat) jobs.push([nat.url, path.join(s.dir, 'libraries', nat.path), nat.sha1]) }
  return { jobs, index: v.assetIndex || null }
}
async function fetchAll(s, ids, prog) {
  const jobs = []; let index = null
  for (const id of ids) { const r = filesOf(s, id); jobs.push(...r.jobs); index = index || r.index }
  if (index) { const idx = path.join(s.dir, 'assets', 'indexes', index.id + '.json')
    try { await dl(index.url, idx, index.sha1) } catch {}
    try { for (const o of Object.values(JSON.parse(fs.readFileSync(idx, 'utf8')).objects || {})) { const h = o.hash; jobs.push([`https://resources.download.minecraft.net/${h.slice(0, 2)}/${h}`, path.join(s.dir, 'assets', 'objects', h.slice(0, 2), h), h]) } } catch {} }
  const todo = jobs.filter((j) => !fs.existsSync(j[1])), total = todo.length; let done = 0, failed = 0
  const worker = async () => { while (todo.length && !pf.stop) { const j = todo.shift(); try { await dl(...j) } catch { failed++ } done++; if (done % 20 === 0 || !todo.length) prog(Math.round(done / total * 100), done + ' / ' + total) } }
  if (total) await Promise.all(Array.from({ length: 8 }, worker))
  return failed
}
/** what our mods for this version look like now: a launcher update with the same jars changes nothing */
const sigCache = {}
function pkgSig(ver) {
  return ['core', 'visuals'].map((k) => { const f = [path.join(process.resourcesPath || '', 'mods', `asteria-${k}-${ver}.jar`), path.join(__dirname, '..', 'mods', 'dist', `asteria-${k}-${ver}.jar`)].find((x) => fs.existsSync(x)); if (!f) return '-'
    const st = fs.statSync(f), key = f + st.size + st.mtimeMs; if (!sigCache[key]) sigCache[key] = crypto.createHash('sha1').update(fs.readFileSync(f)).digest('hex').slice(0, 12); return sigCache[key] }).join('.')
}
function markInstalled(ver) { const m = store.get('installed', {}); m[ver] = pkgSig(ver); store.set('installed', m) }
async function installGame(version, send) {
  const s = settings(); await resolveApi().catch(() => {})
  await ensureJava(s, version, send); await cacheVanillaJson(s, version)
  send('Загрузка Minecraft', 0, '', 3); let failed = await fetchAll(s, [version], (p, d) => send('Загрузка Minecraft', p, d, 3))
  const modern = /^(1\.(1[6-9]|2\d)|26\.)/.test(version), legacy = /^1\.(8|12)\./.test(version)
  const wantFabric = (s.loaders || {})[version] ? s.loaders[version] === 'fabric' : s.fabric !== false
  if (wantFabric && (modern || legacy)) {
    send('Asteria', 30, 'моды и визуалы', 2); const [core] = await Promise.all([coreMod(version).catch(() => null), visualsMod(version).catch(() => null)])
    if (modern || core) { const id = await prepareFabric(s, version, (st, p) => send('Fabric', p, st, 2), legacy).catch(() => null)
      if (id) failed += await fetchAll(s, [id], (p, d) => send('Библиотеки Fabric', p, d, 3)) } }
  if (failed) return { error: `Не скачалось файлов: ${failed}. Проверьте интернет и нажмите «Скачать» ещё раз` }
  markInstalled(version); send('', 0); return { ok: true }
}
// ------------------------------------------------------------------ more windows: the same version with another account at the same time (an AFK farm on one, playing on the other)
const alts = new Map() // account id -> game process
ipcMain.handle('game:alt', async (_, { accountId, version, join }) => {
  const s = settings(), acc = store.get('accounts', []).find((a) => a.id === accountId); if (!acc) return { error: 'Нет такого аккаунта' }
  if (alts.has(accountId)) return { error: acc.name + ' уже играет в другом окне' }
  if (!versionFilesOk(s, version)) return { error: 'Сначала один раз запустите ' + version + ' обычной кнопкой «Играть»' }
  const dir = inst(s, version), fab = fs.readdirSync(path.join(s.dir, 'versions')).find((n) => n.startsWith('fabric-loader-') && n.endsWith('-' + version))
  const wantFabric = (s.loaders || {})[version] ? s.loaders[version] === 'fabric' : s.fabric !== false
  const javaPath = await ensureJava(s, version, () => {}), relay = await relayStart(), tex = accTexDir(acc.name)
  const l = new Client()
  const opts = { root: s.dir, overrides: { gameDirectory: dir }, authorization: acc.type === 'offline' ? offlineAuth(acc.name) : acc.auth, version: { number: version, type: 'release', ...(wantFabric && fab ? { custom: fab } : {}) },
    memory: { max: Math.max(2048, Math.round(s.ramMb * 0.75)) + 'M', min: '1024M' }, customLaunchArgs: ['--width', '1100', '--height', '640'], javaPath,
    customArgs: [...s.jvm.split(/\s+/).filter(Boolean), '-Djava.net.useSystemProxies=true', '-Dasteria.launcher=1', '-Dasteria.api=' + apiBase(), '-Dasteria.user=' + acc.name, '-Dasteria.gamedir=' + dir, '-Dasteria.root=' + s.dir,
      '-Dasteria.skin=' + path.join(tex, 'current.png'), '-Dasteria.cape=' + path.join(tex, 'cape.png'), ...(acc.type === 'offline' ? OFFLINE : LICENSED), ...relayArgs(relay)],
    ...(join ? { quickPlay: { type: /^(1\.2\d|2\d\.)/.test(version) ? 'multiplayer' : 'legacy', identifier: HOST + ':' + PORT } } : {}) }
  try { const p = await l.launch(opts); if (!p) return { error: 'Не удалось запустить' }
    alts.set(accountId, p); p.on('close', () => { alts.delete(accountId); try { win?.webContents.send('alts', [...alts.keys()]) } catch {} }); try { win?.webContents.send('alts', [...alts.keys()]) } catch {}
    return { ok: true } } catch (e) { return { error: 'Не удалось запустить: ' + (e?.message || e) } } })
ipcMain.handle('game:alts', () => [...alts.keys()])
// the whole process tree: java may sit under a helper process, so a plain kill() left the window open
ipcMain.handle('game:altStop', (_, id) => { const p = alts.get(id); if (!p) return true
  try { require('child_process').execFile('taskkill', ['/PID', String(p.pid), '/T', '/F'], { windowsHide: true }, () => {}) } catch {} try { p.kill() } catch {}
  setTimeout(() => { alts.delete(id); try { win?.webContents.send('alts', [...alts.keys()]) } catch {} }, 1500); return true })
// ------------------------------------------------------------------ backup: everything of the launcher as one code (or a file)
// "own": for moving to another computer (offline accounts and nick ownership included); "friend": only the setup (visuals, presets, binds, marks, skin and cape libraries), no accounts
const BACKUP_SKIP = /^(trui:|installed$|visualsAt$|visPresetsAt$|remoteConf$|apiNow|perVer$|playMs$|launches$|friendNotes:|menuBg$)/
function backupMake(mode, local) { const all = store.store, keep = {}
  for (const [k, v] of Object.entries(all)) { if (BACKUP_SKIP.test(k)) continue; if (mode === 'friend' && /^(accounts|active|tokens|friends|settings)$/.test(k)) continue
    if (k === 'accounts') { keep[k] = (v || []).filter((a) => a.type === 'offline'); continue } keep[k] = v } // Microsoft sign-ins are never copied: log in again on the new computer
  const files = {}, s = settings(), skins = path.join(s.dir, 'skins'), add = (abs, rel) => { try { const st = fs.statSync(abs); if (st.isFile() && st.size < 1_500_000) files[rel] = fs.readFileSync(abs).toString('base64') } catch {} }
  const walk = (d, rel) => { let l = []; try { l = fs.readdirSync(d, { withFileTypes: true }) } catch {} for (const e of l) { const p = path.join(d, e.name), r = rel ? rel + '/' + e.name : e.name; if (e.isDirectory()) { if (mode !== 'friend' || e.name !== 'accounts') walk(p, r) } else add(p, 'skins/' + r) } }
  walk(skins, ''); const marks = path.join(inst(s, '1.21.4'), 'config', 'asteria-marks.json'); for (const d of instanceDirs()) { const f = path.join(d, 'config', 'asteria-marks.json'); if (fs.existsSync(f)) { add(f, 'marks.json'); break } }
  return JSON.stringify({ v: 1, mode, at: Date.now(), store: keep, local: local || {}, files }) }
function backupApply(json) { const b = JSON.parse(json); if (!b || b.v !== 1) throw new Error('Это не код AsteriaLauncher'); const s = settings()
  for (const [k, v] of Object.entries(b.store || {})) { if (k === 'accounts') { const cur = store.get('accounts', []); const names = new Set(cur.map((a) => a.name.toLowerCase())); store.set('accounts', [...cur, ...v.filter((a) => !names.has(a.name.toLowerCase()))]); continue }
    if (k === 'settings') { store.set('settings', { ...v, dir: s.dir }); continue } store.set(k, v) } // the game folder stays where it is on this computer
  for (const [rel, data] of Object.entries(b.files || {})) { if (rel.includes('..')) continue; const buf = Buffer.from(data, 'base64')
    if (rel === 'marks.json') { for (const d of instanceDirs()) { try { fs.mkdirSync(path.join(d, 'config'), { recursive: true }); fs.writeFileSync(path.join(d, 'config', 'asteria-marks.json'), buf) } catch {} } continue }
    const f = path.join(s.dir, rel); try { fs.mkdirSync(path.dirname(f), { recursive: true }); fs.writeFileSync(f, buf) } catch {} }
  const v = store.get('visuals'); if (v) for (const d of instanceDirs()) writeVisuals(d, v); useAccountTextures()
  return { local: b.local || {}, mode: b.mode } }
ipcMain.handle('backup:code', async (_, mode, local) => { try { await resolveApi().catch(() => {}); const data = backupMake(mode, local)
    const r = await net.fetch(apiBase() + '/backup', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ data }) }); const j = await r.json().catch(() => ({}))
    if (!r.ok || !j.code) return { error: r.status === 404 ? 'Коды заработают после обновления сервера Asteria — пока сохраните в файл' : 'Сервер не принял: ' + (j.error || r.status) }
    return { code: j.code.slice(0, 5) + '-' + j.code.slice(5) } } catch (e) { return { error: 'Сервер Asteria недоступен — сохраните в файл' } } })
ipcMain.handle('backup:file', async (_, mode, local) => { const r = await dialog.showSaveDialog(win, { defaultPath: path.join(app.getPath('documents'), 'AsteriaLauncher-' + new Date().toISOString().slice(0, 10) + '.asteria-backup'), filters: [{ name: 'Сохранение AsteriaLauncher', extensions: ['asteria-backup'] }] })
  if (!r.filePath) return null; fs.writeFileSync(r.filePath, backupMake(mode, local)); shell.showItemInFolder(r.filePath); return { file: r.filePath } })
ipcMain.handle('backup:restore', async (_, code) => { try { let json
    if (code) { const c = String(code).toUpperCase().replace(/[^A-Z0-9]/g, ''); if (c.length !== 10) return { error: 'Код — 10 символов, например K7Q2X-D4MPR' }
      await resolveApi().catch(() => {}); const r = await net.fetch(apiBase() + '/backup/' + c); if (!r.ok) return { error: r.status === 404 ? 'Такого кода нет' : 'Сервер: ' + r.status }; json = await r.text() }
    else { const r = await dialog.showOpenDialog(win, { filters: [{ name: 'Сохранение AsteriaLauncher', extensions: ['asteria-backup', 'json'] }] }); if (!r.filePaths[0]) return null; json = fs.readFileSync(r.filePaths[0], 'utf8') }
    return backupApply(json) } catch (e) { return { error: e.message || 'Не удалось восстановить' } } })
// ------------------------------------------------------------------ uninstall: everything / only the data / only the app
ipcMain.handle('app:uninstall', async (_, mode) => { // mode: 'all' | 'data' | 'app'
  if ((mode === 'all' || mode === 'app') && !app.isPackaged) return { error: 'Это тестовая сборка — приложение не установлено' }
  const s = settings(), def = path.join(app.getPath('appData'), '.asteria'), uninst = path.join(path.dirname(app.getPath('exe')), 'Uninstall AsteriaLauncher.exe')
  if ((mode === 'all' || mode === 'app') && !fs.existsSync(uninst)) return { error: 'Не найден деинсталлятор: ' + uninst }
  const del = []; if (mode !== 'app') { del.push(app.getPath('userData'))
    // the game folder: the whole default one; a folder chosen by the player loses only what the launcher put there (never other files)
    if (path.resolve(s.dir) === path.resolve(def)) del.push(s.dir); else for (const n of ['instances', 'versions', 'libraries', 'assets', 'runtime', 'skins', 'cache', 'natives', 'logs', 'config', 'servers.dat', 'launcher_profiles.json', 'log4j2_112-116.xml', 'log4j2_17-111.xml']) del.push(path.join(s.dir, n)) }
  const q = (p) => "'" + String(p).replace(/'/g, "''") + "'"
  const ps = ['$ErrorActionPreference = "SilentlyContinue"', 'Wait-Process -Id ' + process.pid + ' -Timeout 30', 'Start-Sleep -Milliseconds 800',
    ...del.map((p) => 'Remove-Item -LiteralPath ' + q(p) + ' -Recurse -Force'),
    mode === 'data' ? 'Start-Process -FilePath ' + q(app.getPath('exe')) : 'Start-Process -FilePath ' + q(uninst) + ' -ArgumentList "/S"'].join('; ')
  require('child_process').spawn('powershell.exe', ['-NoProfile', '-WindowStyle', 'Hidden', '-Command', ps], { detached: true, stdio: 'ignore', windowsHide: true }).unref()
  setTimeout(() => app.exit(0), 300); return { ok: true } })
/** delete a version: its game files (and, if asked, its folder with worlds, mods and settings) go to the recycle bin */
ipcMain.handle('ver:remove', async (_, ver, withData) => { if (game || pf.job || !/^[\w.\-]+$/.test(String(ver))) return { error: 'Подождите — идёт загрузка или игра запущена' }
  const s = settings(), vd = path.join(s.dir, 'versions'), m = store.get('installed', {}); delete m[ver]; store.set('installed', m)
  try { for (const n of fs.readdirSync(vd)) if (n === ver || (n.startsWith('fabric-loader-') && n.endsWith('-' + ver))) await shell.trashItem(path.join(vd, n)).catch(() => fs.rmSync(path.join(vd, n), { recursive: true, force: true })) } catch {}
  if (withData) { const d = inst(s, ver); if (fs.existsSync(d)) await shell.trashItem(d).catch(() => {}) }
  return { ok: true } })
ipcMain.handle('game:install', async (_, ver) => {
  if (game || pf.job || !/^[\w.\-]+$/.test(String(ver))) return { error: 'Подождите — идёт загрузка или игра запущена' }
  const send = (stage, pct, detail = '', step = 0) => { try { win?.webContents.send('progress', { stage, pct: Math.max(0, Math.min(100, Math.round(+pct || 0))), detail, step }) } catch {} }
  pf.job = installGame(ver, send).catch((e) => ({ error: 'Не удалось скачать: ' + String(e?.message || e) })).finally(() => { pf.job = null; send('', 0) })
  return pf.job
})
/** the files a version needs are really there (the player may have deleted folders by hand): the game jar and json, the Fabric profile when Fabric is on */
function versionFilesOk(s, ver) { const vd = path.join(s.dir, 'versions', String(ver)); if (!fs.existsSync(path.join(vd, ver + '.jar')) || !fs.existsSync(path.join(vd, ver + '.json'))) return false
  const wantFabric = (s.loaders || {})[ver] ? s.loaders[ver] === 'fabric' : s.fabric !== false
  if (wantFabric && /^(1\.(1[6-9]|2\d)|26\.)/.test(ver)) { try { if (!fs.readdirSync(path.join(s.dir, 'versions')).some((n) => n.startsWith('fabric-loader-') && n.endsWith('-' + ver) && fs.existsSync(path.join(s.dir, 'versions', n, n + '.json')))) return false } catch { return false } }
  return true }
ipcMain.handle('game:status', (_, ver) => { const s = settings(), rec = store.get('installed', {})[ver]
  const has = versionFilesOk(s, ver) && (!!rec || fs.existsSync(path.join(inst(s, ver), 'mods')))
  if (!has && rec) { const m = store.get('installed', {}); delete m[ver]; store.set('installed', m) } // deleted by hand: "Download" again instead of a "Play" that fails
  return { installed: has, update: has && !!rec && rec !== pkgSig(ver), busy: !!pf.job } })
async function cacheVanillaJson(s, ver) {
  const f = path.join(s.dir, 'versions', ver, ver + '.json'); if (fs.existsSync(f)) return
  try { const m = await mrJ('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'), v = m.versions.find((x) => x.id === ver); if (!v) return
    const j = await (await net.fetch(v.url)).text(); JSON.parse(j); fs.mkdirSync(path.dirname(f), { recursive: true }); fs.writeFileSync(f, j) } catch {}
}
// a Minecraft started by this launcher may still be running after the launcher was closed / updated: find it by our -Dasteria.gamedir
let extGame = null, extAt = 0, extBusy = null
/** cached result; refreshed in the background (PowerShell takes ~1 s, so never on the main thread's critical path) */
function externalGame(force) {
  if (process.platform !== 'win32') return Promise.resolve(null)
  if (extGame && !alive(extGame.pid)) extGame = null
  if (!extGame) { const a = aliveFile(); if (a) { extGame = a; extAt = Date.now(); return Promise.resolve(a) } }
  if (!force && Date.now() - extAt < 15000) return Promise.resolve(extGame)
  if (extBusy) return extBusy
  extAt = Date.now()
  extBusy = new Promise((res) => require('child_process').execFile('powershell.exe', ['-NoProfile', '-NonInteractive', '-Command',
      "Get-CimInstance Win32_Process -Filter \"Name like 'java%'\" | Where-Object { $_.CommandLine -like '*-Dasteria.gamedir=*' } | ForEach-Object { \"$($_.ProcessId)|$($_.CommandLine)\" }"],
    { encoding: 'utf8', windowsHide: true, timeout: 20000 }, (err, out) => {
      const line = !err && String(out).split(/\r?\n/).find((l) => l.includes('|'))
      extGame = line ? { pid: +line.split('|')[0], version: (line.match(/--version (\S+)/) || [])[1] || null, dir: (line.match(/-Dasteria\.gamedir=(.+?) -D/) || [])[1] || null } : aliveFile()
      extBusy = null; res(extGame) }))
  return extBusy
}
const alive = (pid) => { try { process.kill(pid, 0); return true } catch { return false } }
/** instances/<id>/config/asteria-alive: "pid|time", rewritten by our mod every 4 s while the game runs */
function aliveFile() {
  try { const root = path.join(settings().dir, 'instances')
    for (const id of fs.readdirSync(root)) { const f = path.join(root, id, 'config', 'asteria-alive'); if (!fs.existsSync(f)) continue
      const [pid, at] = fs.readFileSync(f, 'utf8').split('|').map(Number)
      if (Date.now() - at < 15000 && pid && alive(pid)) { const v = /^\d+\.\d+(\.\d+)?$/.test(id) ? id : (store.get('profiles', []).find((p) => p.id === id)?.version || null); return { pid, version: v, dir: path.join(root, id) } } } } catch {}
  return null
}
// ------------------------------------------------------------------ worlds: every instance's saves, import / export / copy, maps from the catalog
const savesOf = (ver) => { const d = path.join(inst(settings(), ver), 'saves'); fs.mkdirSync(d, { recursive: true }); return d }
const inInstances = (p) => { const b = path.resolve(settings().dir, 'instances') + path.sep, r = path.resolve(String(p || '')); return r.startsWith(b) && r.split(path.sep).includes('saves') ? r : null }
const worldSize = (d, cap = 4000) => { let n = 0, s = 0; const walk = (x) => { for (const e of fs.readdirSync(x, { withFileTypes: true })) { if (++n > cap) return; const f = path.join(x, e.name); if (e.isDirectory()) walk(f); else try { s += fs.statSync(f).size } catch {} } }; try { walk(d) } catch {} return s }
ipcMain.handle('worlds:list', () => { const out = [], base = path.join(settings().dir, 'instances')
  try { for (const v of fs.readdirSync(base)) { const sd = path.join(base, v, 'saves'); let names = []; try { names = fs.readdirSync(sd) } catch {}
    for (const n of names) { const p = path.join(sd, n), lvl = path.join(p, 'level.dat'); if (!fs.existsSync(lvl)) continue; let icon = null
      try { const ic = path.join(p, 'icon.png'); if (fs.existsSync(ic)) icon = 'data:image/png;base64,' + fs.readFileSync(ic).toString('base64') } catch {}
      const pr = /^\d+\.\d+(\.\d+)?$/.test(v) ? null : store.get('profiles', []).find((x) => x.id === v); if (!pr && !/^\d/.test(v)) continue
      out.push({ path: p, name: n, version: pr ? pr.version : v, profile: pr ? pr.name : '', mtime: fs.statSync(lvl).mtimeMs, size: worldSize(p), icon }) } } } catch {}
  return out.sort((a, b) => b.mtime - a.mtime) })
ipcMain.handle('worlds:openSaves', (_, ver) => shell.openPath(savesOf(ver)))
ipcMain.handle('worlds:open', (_, p) => { const r = inInstances(p); return r ? shell.openPath(r) : null })
ipcMain.handle('worlds:remove', async (_, p) => { const r = inInstances(p); if (!r) return false; await shell.trashItem(r); return true }) // to the recycle bin: can be restored
ipcMain.handle('worlds:copy', (_, p, ver) => { const r = inInstances(p); if (!r) return false; let to = path.join(savesOf(ver), path.basename(r)), i = 2; while (fs.existsSync(to)) to = path.join(savesOf(ver), path.basename(r) + ' (' + i++ + ')'); fs.cpSync(r, to, { recursive: true }); return true })
/** a world zip (level.dat at the root or one folder deep) -> saves/<name> of the version; entries leaving the folder are skipped */
function unpackWorld(zipFile, ver, name) { const AdmZip = require('adm-zip'), z = new AdmZip(zipFile), es = z.getEntries()
  const lvl = es.filter((e) => /(^|\/)level\.dat$/.test(e.entryName)).sort((a, b) => a.entryName.length - b.entryName.length)[0]; if (!lvl) throw new Error('В архиве нет мира (level.dat)')
  const pre = lvl.entryName.slice(0, -'level.dat'.length), base = (name || pre.replace(/\/$/, '').split('/').pop() || path.basename(zipFile, '.zip')).replace(/[\\/:*?"<>|]/g, '_').slice(0, 60) || 'Мир'
  let to = path.join(savesOf(ver), base), i = 2; while (fs.existsSync(to)) to = path.join(savesOf(ver), base + ' (' + i++ + ')')
  for (const e of es) { if (e.isDirectory || !e.entryName.startsWith(pre)) continue; const f = path.resolve(to, e.entryName.slice(pre.length)); if (!f.startsWith(path.resolve(to) + path.sep)) continue
    fs.mkdirSync(path.dirname(f), { recursive: true }); fs.writeFileSync(f, e.getData()) }
  return path.basename(to) }
ipcMain.handle('worlds:import', async (_, ver) => { const r = await dialog.showOpenDialog(win, { title: 'Импорт мира', properties: ['openFile', 'openDirectory'], filters: [{ name: 'Мир (.zip или папка)', extensions: ['zip'] }] }); const f = r.filePaths[0]; if (!f) return null
  try { if (fs.statSync(f).isDirectory()) { if (!fs.existsSync(path.join(f, 'level.dat'))) return { error: 'В папке нет level.dat — это не мир' }; let to = path.join(savesOf(ver), path.basename(f)), i = 2; while (fs.existsSync(to)) to = path.join(savesOf(ver), path.basename(f) + ' (' + i++ + ')'); fs.cpSync(f, to, { recursive: true }); return { name: path.basename(to) } }
    return { name: unpackWorld(f, ver) } } catch (e) { return { error: e.message } } })
ipcMain.handle('worlds:export', async (_, p) => { const r = inInstances(p); if (!r) return null; const s = await dialog.showSaveDialog(win, { defaultPath: path.join(app.getPath('downloads'), path.basename(r) + '.zip'), filters: [{ name: 'ZIP', extensions: ['zip'] }] }); if (!s.filePath) return null
  const AdmZip = require('adm-zip'), z = new AdmZip(); z.addLocalFolder(r, path.basename(r)); z.writeZip(s.filePath); shell.showItemInFolder(s.filePath); return s.filePath })
/** maps catalog: filled on the backend (launcher config "maps": [{ name, desc, img, url, ver }]) — only those links are downloaded */
ipcMain.handle('maps:list', () => ((store.get('remoteConf') || {}).maps || []).filter((m) => m && /^https:\/\//.test(m.url || '')))
ipcMain.handle('maps:get', async (_, url, ver, name) => { const m = ((store.get('remoteConf') || {}).maps || []).find((x) => x.url === url); if (!m) return { error: 'Карты нет в каталоге' }
  const tmp = path.join(app.getPath('temp'), 'asteria-map-' + Date.now() + '.zip')
  try { const res = await net.fetch(url); if (!res.ok) throw new Error('HTTP ' + res.status); fs.writeFileSync(tmp, Buffer.from(await res.arrayBuffer())); return { name: unpackWorld(tmp, ver, name || m.name) } }
  catch (e) { return { error: 'Не скачалось: ' + e.message } } finally { fs.rmSync(tmp, { force: true }) } })
/** the big maps catalog (CurseForge "Worlds") through the Asteria server */
ipcMain.handle('maps:search', async (_, o = {}) => { await resolveApi().catch(() => {})
  const p = new URLSearchParams({ q: String(o.q || '').slice(0, 60), sort: o.sort || '', page: String(o.page || 0), ...(o.ver ? { ver: o.ver } : {}) })
  try { const r = await net.fetch(apiBase() + '/maps/search?' + p, { signal: AbortSignal.timeout(20000) }); const j = await r.json().catch(() => ({}))
    if (r.status === 503 || j.error === 'no key') return { error: 'nokey' }; if (r.status === 404) return { error: 'old' }; if (!r.ok) return { error: j.error || 'HTTP ' + r.status }; return j }
  catch (e) { return { error: 'Нет связи с сервером Asteria' } } })
ipcMain.handle('maps:cf', async (_, mod, file, ver, name) => { await resolveApi().catch(() => {})
  const tmp = path.join(app.getPath('temp'), 'asteria-map-' + Date.now() + '.zip')
  try { const j = await net.fetch(apiBase() + '/maps/file?mod=' + (+mod) + '&file=' + (+file), { signal: AbortSignal.timeout(20000) }).then((x) => x.json())
    if (!j.url) return { error: j.error ? 'Сервер: ' + j.error : 'Автор запретил скачивание из лаунчеров — откройте страницу карты', page: true }
    const h = new URL(j.url).hostname; if (!/(^|.)forgecdn.net$/.test(h)) return { error: 'Неожиданный адрес файла' }
    if (!/.zip$/i.test(j.name || j.url)) return { error: 'Это не архив мира (' + (j.name || '?') + ')', page: true }
    const res = await net.fetch(j.url); if (!res.ok) throw new Error('HTTP ' + res.status); fs.writeFileSync(tmp, Buffer.from(await res.arrayBuffer()))
    return { name: unpackWorld(tmp, ver, String(name || '').replace(/[\/:*?"<>|]/g, '').slice(0, 60) || undefined) } }
  catch (e) { return { error: 'Не скачалось: ' + e.message } } finally { fs.rmSync(tmp, { force: true }) } })
// ------------------------------------------------------------------ friends: a local list, presence (in game, version, e4mc address) from the Asteria server
let hostNow = '', beatNow = null // beatNow: send the heartbeat at once (cosmetics changed in the launcher) // the e4mc address of the world this player opened to friends
// requests go through the Asteria server under the active account; new notices (request / accepted / declined) are pushed to the window
let friendsLast = null
let playNow = null // { ver, srv } while a game of ours runs: what "invite a friend" points to
/** a Windows notification with sound + a banner inside the running game */
function friendAlert(x) {
  const body = x.k === 'invite' ? x.who + ' зовёт вас ' + (x.where?.kind === 'world' ? 'в свой мир' : 'на сервер ' + (x.where?.label || x.where?.host || '')) : x.k === 'msg' ? x.who + ': ' + (x.text || '') : ''
  if (!body) return
  try { new Notification({ title: x.k === 'invite' ? 'Приглашение от друга' : 'Сообщение от друга', body, silent: false }).show() } catch {}
  const dir = liveGameDir(); if (dir) try { fs.mkdirSync(path.join(dir, 'config'), { recursive: true }); fs.writeFileSync(path.join(dir, 'config', 'asteria-notify.json'), JSON.stringify({ t: Date.now(), title: x.k === 'invite' ? 'Приглашение' : x.who, text: x.k === 'invite' ? body : x.text || '', kind: x.k })) } catch {} }
async function friendsCall(act = 'get', who = '', accept = false, code = '', extra = {}) { const acc = store.get('accounts', []).find((a) => a.id === store.get('active')); if (!acc) return { error: 'Нет аккаунта' }
  await resolveApi().catch(() => {}); const token = await getToken(acc.name).catch(() => null); if (!token) return { error: 'Нет связи с сервером Asteria' }
  const seenK = 'friendNotes:' + acc.name.toLowerCase(), since = store.get(seenK, Date.now() - 86400000)
  try { const r = await net.fetch(apiBase() + '/friends', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ name: acc.name, token, act, who, accept, since, code, ...extra }) })
    const j = await r.json(); if (!r.ok) return { error: j.error === 'bad code' ? 'Нет игрока с таким кодом дружбы' : j.error === 'bad name' ? 'Это ваш собственный код' : j.error === 'already' ? 'Уже в друзьях' : j.error === 'not friend' ? 'Это не ваш друг' : j.error === 'bad address' ? 'Неверный адрес сервера' : j.error === 'owned' ? 'Ник занят другим лаунчером' : r.status === 404 || j.error === 'not found' ? 'Друзья заработают после обновления сервера Asteria (он ещё старой версии)' : 'Сервер: ' + (j.error || r.status) }
    if (j.notes?.length) { store.set(seenK, Math.max(...j.notes.map((x) => x.t))); try { win?.webContents.send('friendNotes', j.notes) } catch {}
      for (const x of j.notes) if (x.k === 'invite' || x.k === 'msg') friendAlert(x)
      if (!win?.isFocused()) for (const x of j.notes.filter((y) => y.k !== 'invite' && y.k !== 'msg')) try { new Notification({ title: 'AsteriaLauncher', body: x.k === 'request' ? x.who + ' хочет добавить вас в друзья' : x.k === 'accepted' ? x.who + ' принял(а) вашу заявку в друзья' : x.who + ' отклонил(а) заявку в друзья' }).show() } catch {} }
    if (act !== 'get' && j.friends === undefined) return j // invite / msg / chat: their own answer
    friendsLast = { ...j, host: hostNow }; return friendsLast } catch (e) { return { error: 'Сервер Asteria недоступен' } } }
ipcMain.handle('friends:get', () => friendsCall('get'))
ipcMain.handle('friends:act', (_, act, who, accept) => act === 'request' ? friendsCall('request', '', false, String(who || '').trim()) : friendsCall(act, String(who || '').trim(), !!accept)) // requests: who = the friend code
setInterval(() => { if (store.get('accounts', []).length && (!friendsLast || friendsLast.friends?.length || friendsLast.incoming?.length || friendsLast.outgoing?.length)) friendsCall('get').catch(() => {}) }, 8000) // invites and messages arrive within seconds, even with the Friends page closed
/** invite a friend to where you are: your world opened to friends, the server you joined from the launcher, or SafiMine */
ipcMain.handle('friends:invite', (_, who, kind) => {
  const where = kind === 'world' ? (hostNow ? { kind: 'world', host: hostNow, port: 25565, ver: playNow?.ver || '' } : null)
    : kind === 'here' ? (playNow?.srv ? { kind: 'server', host: playNow.srv.split(':')[0], port: +playNow.srv.split(':')[1] || 25565, ver: playNow.ver } : null)
    : { kind: 'server', host: HOST, port: PORT, ver: playNow?.ver || '1.16.5', label: 'SafiMine' }
  if (!where) return { error: kind === 'world' ? 'Сначала откройте мир для друзей: ESC → «Открыть для сети»' : 'Сначала зайдите на сервер через лаунчер' }
  return friendsCall('invite', String(who || ''), false, '', { where }) })
ipcMain.handle('friends:msg', (_, who, text) => friendsCall('msg', String(who || ''), false, '', { text: String(text || '').slice(0, 500) }))
ipcMain.handle('friends:chat', (_, who, after) => friendsCall('chat', String(who || ''), false, '', { after: +after || 0 }))
ipcMain.handle('friends:where', () => ({ world: !!hostNow, here: playNow?.srv || '', ver: playNow?.ver || '' }))
ipcMain.handle('game:launch', async (_, { version, join, lang, server, world, together }) => {
  const srv = server && server.host ? { host: String(server.host), port: +server.port || 25565 } : { host: HOST, port: PORT }
  if (game) return { error: 'Игра уже запущена' }
  const ext = await externalGame(true); if (ext) { try { win?.webContents.send('game', true) } catch {} return { error: `Minecraft ${ext.version || ''} уже запущен — закройте его или нажмите «Остановить»` } }
  const s = settings(), acc = store.get('accounts', []).find((a) => a.id === store.get('active'))
  if (!acc) return { error: 'Выберите аккаунт' }
  const send = (stage, pct, detail = '', step = 0) => { try { win?.webContents.send('progress', { stage, pct: Math.max(0, Math.min(100, Math.round(+pct || 0))), detail, step }) } catch {} }
  // window size: never the whole screen — otherwise F11 (fullscreen -> window) gives a window whose title bar and buttons are off-screen
  const wa = screen.getPrimaryDisplay().workAreaSize
  const winW = s.fullscreen ? Math.round(wa.width * 0.72) : Math.min(+s.width || 1280, wa.width - 40), winH = s.fullscreen ? Math.round(wa.height * 0.72) : Math.min(+s.height || 720, wa.height - 70)
  const dir = inst(s, version); fs.mkdirSync(dir, { recursive: true }); setOpts(dir, { lang: LANGMAP[lang] || 'en_us', fullscreen: !!s.fullscreen, overrideWidth: 0, overrideHeight: 0 })
  fixLog4j(s.dir)
  const l = new Client(); let started = false, t0 = 0, hb = null; logBuf = []; pendingOpts = {}
  const md = path.join(dir, 'mods'); fs.mkdirSync(md, { recursive: true })
  for (const f of ['asteria-mod.jar', 'asteria-core.jar', 'asteria-visuals.jar']) fs.rmSync(path.join(md, f), { force: true })
  try {
    send('Подготовка', 0, '', 0); let custom = null, core = null, vis = null; await stopPrefetch(); useAccountTextures(); await resolveApi().catch(() => {})
    const javaPath = await ensureJava(s, version, send); await cacheVanillaJson(s, version)
    const modern = /^(1\.(1[6-9]|2\d)|26\.)/.test(version), legacy = /^1\.(8|12)\./.test(version)
    const wantFabric = (s.loaders || {})[version] ? s.loaders[version] === 'fabric' : s.fabric !== false
    if (wantFabric && (modern || legacy)) { // our mods come first: the loader for 1.8.9 / 1.12.2 is installed only if our mod exists for it
      send('Asteria', 2, '', 2); [core, vis] = await Promise.all([coreMod(version).catch(() => null), visualsMod(version).catch(() => null)]) // in parallel: a slow backend must not delay the game
      if (modern || core) { send('Fabric', 5, '', 2); custom = await prepareFabric(s, version, (st, p) => send('Fabric', p, st, 2), legacy).catch(() => null) } }
    if (custom && core) fs.copyFileSync(core, path.join(md, 'asteria-core.jar'))
    if (custom && vis && store.get('visualsOn', true) !== false) fs.copyFileSync(vis, path.join(md, 'asteria-visuals.jar')) // 'Visuals off' = play without the mod
    let togetherWarn = ''; hostNow = ''
    if (together) { if (!custom) togetherWarn = 'Игра вместе работает через Fabric-мод e4mc — Fabric не установился'; else { send('e4mc', 8, '', 2); await installSlug('e4mc', version, md).catch(() => { togetherWarn = 'Мод e4mc для ' + version + ' не найден — для игры вместе выберите 1.20.1 или 1.21.4' }) } }
    const warn = (wantFabric && modern && !custom ? 'Fabric не установился (нет связи с meta.fabricmc.net?) — игра запущена без модов: без звезды, визуалов и FPS-буста' : '') || togetherWarn
    writeVisuals(dir); writePresets(dir); writeBg(dir); writeVoice(dir); writeModels(dir); const relay = await relayStart()
    const opts = { root: s.dir, overrides: { gameDirectory: dir }, authorization: acc.type === 'offline' ? offlineAuth(acc.name) : acc.auth, version: { number: version, type: 'release', ...(custom ? { custom } : {}) },
      memory: { max: s.ramMb + 'M', min: Math.min(s.ramMb, 2048) + 'M' }, customLaunchArgs: ['--width', String(winW), '--height', String(winH), ...(s.fullscreen ? ['--fullscreen'] : [])], javaPath,
      customArgs: [...s.jvm.split(/\s+/).filter(Boolean), '-Djava.net.useSystemProxies=true' /* skins, Mojang keys and our API go through the Windows proxy, like in the browser */, '-Dasteria.launcher=1', '-Dasteria.api=' + apiBase(), '-Dasteria.user=' + acc.name, '-Dasteria.gamedir=' + dir, '-Dasteria.root=' + s.dir,
        ...(acc.type === 'offline' ? OFFLINE : LICENSED), ...relayArgs(relay)],
      // 'Join SafiMine': MCLC ignores the old server option, quickPlay works (1.20+ understands --quickPlayMultiplayer, older versions --server/--port)
      ...(join ? { quickPlay: { type: /^(1\.2\d|2\d\.)/.test(version) ? 'multiplayer' : 'legacy', identifier: srv.host + ':' + srv.port } } : {}),
      // straight into a world (1.20+ understands --quickPlaySingleplayer; older versions open the title screen)
      ...(!join && world && /^(1\.2\d|2\d\.)/.test(version) ? { quickPlay: { type: 'singleplayer', identifier: path.basename(String(world)) } } : {}) }
    l.on('progress', (e) => send('Загрузка файлов', e.total ? Math.round((e.task / e.total) * 100) : 0, e.type || '', 3))
    l.on('download-status', (e) => send('Загрузка библиотек', e.total ? Math.round((e.current / e.total) * 100) : 0, e.name || '', 3))
    l.on('package-extract', () => send('Распаковка', 100, '', 4))
    l.on('debug', (m) => { logBuf.push(String(m)); if (logBuf.length > 1000) logBuf.splice(0, 300) })
    l.on('data', (m) => { // NOTE: no per-line IPC — joining a world prints thousands of lines
      logBuf.push(String(m).trimEnd()); if (logBuf.length > 1000) logBuf.splice(0, 300)
      const e4 = /([a-z0-9-]+\.)+e4mc\.link/i.exec(String(m)); if (e4 && e4[0] !== hostNow) { hostNow = e4[0].toLowerCase(); try { win?.webContents.send('together', hostNow) } catch {} } // the world is open to friends
      if (!started) { started = true; t0 = Date.now(); gameDirNow = dir; coverWritten = {}; rpcGame(version, dir, join ? srv.host : null); send('', 0); try { win?.webContents.send('game', true) } catch {}; if (s.hideOnLaunch !== false) win?.minimize() } })
    l.on('close', () => { if (hb) clearInterval(hb); beatNow = null; game = null; gameVer = null; gameDirNow = null; hostNow = ''
      fs.rmSync(path.join(dir, 'asteria-live.flag'), { force: true })
      if (Object.keys(pendingOpts).length) { setOpts(dir, pendingOpts); pendingOpts = {} } // the game may rewrite options.txt on exit: keep what was changed in the launcher
      if (t0) { const el = Date.now() - t0, pv = store.get('perVer', {}); store.set('playMs', store.get('playMs', 0) + el); pv[version] = (pv[version] || 0) + el; store.set('perVer', pv); store.set('launches', store.get('launches', 0) + 1) }
      rpcIdle()
      playNow = null; try { send('', 0); win?.webContents.send('game', false); if (win?.isMinimized()) win.restore() } catch {} })
    const tok = await getToken(acc.name).catch(() => null)
    const visNow = () => { try { return JSON.parse(fs.readFileSync(path.join(dir, 'config', 'asteria-visuals.json'), 'utf8')) } catch { return null } }
    const petOf = () => { const p = visNow()?.modules?.pet; if (!p?.on) return '' // other Asteria players see your pet (":m" = mini-me with a skin from the launcher)
      const mini = +p.s?.kind === 9 && +p.s?.miniskin === 1 && fs.existsSync(path.join(s.dir, 'skins', 'mini.png'))
      return `${+p.s?.kind || 0}:${(+p.s?.size || 1).toFixed(2)}${mini ? ':m' : ''}` }
    const fxOf = () => { const m = visNow()?.modules || {}; const c = m.cosmetics; const cos = c?.on ? `c:${+c.s?.wings || 0}.${+c.s?.hat || 0}.${+c.s?.back || 0}${c.s?.tint === false && c.s?.color ? '.' + String(c.s.color).replace('#', 'ff') : ''}` : ''
      return [m.halo?.on && 'halo', m.chinahat?.on && 'hat', m.trails?.on && 'trail', cos].filter(Boolean).join(',') }
    const beat = () => fetch(apiBase() + '/heartbeat', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ name: acc.name, token: tok, ...store.get('cosmetics', {}), pet: petOf(), fx: fxOf(), ver: version, host: hostNow, srv: join ? srv.host : '' }) }).catch(() => {}); playNow = { ver: version, srv: join ? srv.host + (srv.port && srv.port !== 25565 ? ':' + srv.port : '') : '' }; beat(); beatNow = beat; let cfgM = 0; const cfgF = path.join(dir, 'config', 'asteria-visuals.json')
    hb = setInterval(() => { let m = 0; try { m = fs.statSync(cfgF).mtimeMs } catch {} if (m !== cfgM && cfgM) beat(); cfgM = m || cfgM; if (++hbN % 20 === 0) beat() }, 1000); let hbN = 0 // registers right away, so other launcher players see the icon at once
    await addServer(dir).catch(() => {}); for (const x of [...PARTNER_SERVERS, ...((store.get('remoteConf') || {}).servers || []), ...myServers()]) if (x.ip && x.ip !== HOST) await addServerEntry(dir, x.name || x.ip, x.port && +x.port !== 25565 ? x.ip + ':' + x.port : x.ip).catch(() => {}) // put back every time, even if deleted in the game
    game = await l.launch(opts); gameVer = version
    if (!game) { clearInterval(hb); send('', 0); return { error: 'Не удалось запустить игру' } }
    send('Запуск игры', 100, '', 5); if (!warn) markInstalled(version)
    return { ok: true, warn }
  } catch (e) { if (hb) clearInterval(hb); game = null; send('', 0); try { fs.appendFileSync(path.join(s.dir, 'launcher-errors.log'), new Date().toISOString() + ' launch ' + version + ': ' + (e?.stack || e) + '\n') } catch {} return { error: 'Не удалось запустить: ' + String(e?.message || e) } }
})
let autoUpdater = null
function setupUpdates() { // forced updates via electron-updater (generic provider, see package.json build.publish)
  if (!app.isPackaged) return
  try { ({ autoUpdater } = require('electron-updater')) } catch { return }
  const emit = (d) => { try { win?.webContents.send('update', d) } catch {} }
  autoUpdater.autoDownload = true
  autoUpdater.on('update-available', (i) => emit({ state: 'downloading', pct: 0, version: i.version }))
  autoUpdater.on('download-progress', (p) => emit({ state: 'downloading', pct: Math.round(p.percent) }))
  autoUpdater.on('update-downloaded', (i) => emit({ state: 'ready', version: i.version }))
  autoUpdater.on('error', () => {})
  let lastCheck = 0, ready = false
  const check = () => { if (ready || Date.now() - lastCheck < 20000) return; lastCheck = Date.now(); autoUpdater.checkForUpdates().catch(() => {}) }
  autoUpdater.on('update-downloaded', () => { ready = true })
  check(); setInterval(check, 60 * 1000) // an open launcher notices a new release within a minute
  autoUpdater.on('update-not-available', () => {}); app.on('browser-window-focus', check)
  ipcMain.handle('update:check', () => check()) // also every time the window comes back to the front
}
ipcMain.handle('update:install', () => autoUpdater?.quitAndInstall(true, true)) // silent: no installer window, launcher restarts by itself
process.on('uncaughtException', (e) => { try { logBuf.push('[launcher] ' + (e?.stack || e)) } catch {} })
process.on('unhandledRejection', (e) => { try { logBuf.push('[launcher] ' + e) } catch {} })
app.on('render-process-gone', () => { try { win?.reload() } catch {} })
app.whenReady().then(setupUpdates)
app.whenReady().then(() => setTimeout(mediaStart, 300)) // 'Now playing' in the launcher and the game
app.on('will-quit', () => { mediaRetry = 99; try { mediaProc?.kill() } catch {} })

// ---------- Java auto-install (Adoptium Temurin JRE, version taken from Mojang's per-version metadata)
function sysJava() { try { const r = spawnSync('java', ['-version'], { encoding: 'utf8', windowsHide: true }); const m = (r.stderr || '').match(/version "(\d+)(?:\.(\d+))?/); return m ? { major: m[1] === '1' ? +m[2] : +m[1] } : null } catch { return null } }
async function requiredJava(version) {
  try { const man = await mrJ('https://launchermeta.mojang.com/mc/game/version_manifest_v2.json'), v = man.versions.find((x) => x.id === version)
    if (v) { const m = (await mrJ(v.url)).javaVersion?.majorVersion; if (m) return m } } catch {}
  return /^26\./.test(version) ? 25 : /^1\.2[1-9]/.test(version) ? 21 : /^1\.(1[7-9]|20)/.test(version) ? 17 : 8
}
const findJavaw = (root) => { try { for (const d of fs.readdirSync(root)) { const p = path.join(root, d, 'bin', 'javaw.exe'); if (fs.existsSync(p)) return p } } catch {} return null }
async function ensureJava(s, version, send) {
  if (s.java) return s.java.replace(/java\.exe$/i, 'javaw.exe')
  const raw = await requiredJava(version), need = raw <= 8 ? 8 : raw <= 17 ? 17 : raw <= 21 ? 21 : 25, sj = sysJava()
  if (sj && (sj.major === need || (need >= 17 && sj.major >= need))) { try { const w = execFileSync('where', ['javaw'], { encoding: 'utf8', windowsHide: true }).split(/\r?\n/)[0].trim(); if (w) return w } catch {} }
  const root = path.join(s.dir, 'runtime', 'java-' + need), have = findJavaw(root); if (have) return have
  if (javaJobs[need]) { send('Java ' + need, 50, 'докачивается…', 1); return javaJobs[need] }
  javaJobs[need] = downloadJava(root, need, send).finally(() => { delete javaJobs[need] }); return javaJobs[need]
}
const javaJobs = {}
async function downloadJava(root, need, send) {
  fs.mkdirSync(root, { recursive: true }); send('Java ' + need, 0, '', 1)
  const r = await net.fetch(`https://api.adoptium.net/v3/binary/latest/${need}/ga/windows/x64/jre/hotspot/normal/eclipse`)
  if (!r.ok) throw new Error('Java download failed: HTTP ' + r.status)
  const total = +r.headers.get('content-length') || 0, zip = path.join(root, 'jre.zip'), out = fs.createWriteStream(zip), rd = r.body.getReader(); let got = 0
  for (;;) { const { done, value } = await rd.read(); if (done) break; out.write(Buffer.from(value)); got += value.length; send('Java ' + need, total ? Math.round((got / total) * 100) : 50, (got / 1048576).toFixed(0) + ' MB', 1) }
  await new Promise((res) => out.end(res)); send('Java ' + need, 100, '', 1)
  execFileSync('powershell', ['-NoProfile', '-Command', `Expand-Archive -LiteralPath '${zip}' -DestinationPath '${root}' -Force`], { windowsHide: true })
  fs.rmSync(zip, { force: true }); const e = findJavaw(root); if (!e) throw new Error('Java unpack failed'); return e
}
// ---------- resource packs & shaders (Modrinth)
const PK = { resourcepack: 'resourcepacks', shader: 'shaderpacks' }
ipcMain.handle('pk:search', async (_, { q, ver, sort, kind, pvp }) => { try { const fa = [['project_type:' + kind], ['versions:' + ver]]; if (pvp && kind === 'resourcepack') fa.push(['categories:combat'])
  return (await mrJ(`https://api.modrinth.com/v2/search?query=${ENC(((q || '') + (pvp && kind !== 'resourcepack' ? ' pvp' : '')).trim())}&facets=${ENC(JSON.stringify(fa))}&index=${sort || 'downloads'}&limit=20`)).hits } catch { return [] } })
ipcMain.handle('pk:install', async (_, slug, ver, kind) => { try { const d = path.join(inst(settings(), ver), PK[kind]); fs.mkdirSync(d, { recursive: true })
  const v = (await mrJ(`https://api.modrinth.com/v2/project/${slug}/version?game_versions=${ENC(JSON.stringify([ver]))}`))[0]; if (!v) return { error: 'Нет версии под ' + ver }
  const f = v.files.find((x) => x.primary) || v.files[0]; if (/\.mrpack$/i.test(f.filename)) return { error: 'Формат .mrpack не поддерживается' }
  fs.writeFileSync(path.join(d, slug + '.zip'), Buffer.from(await (await net.fetch(f.url)).arrayBuffer()))
  try { const p = await mrJ(`https://api.modrinth.com/v2/project/${slug}`), m = store.get('meta', {}); m['pk:' + slug] = { title: p.title, icon: p.icon_url, desc: p.description }; store.set('meta', m) } catch {}
  return { ok: true } } catch { return { error: 'Ошибка загрузки' } } })
ipcMain.handle('pk:list', (_, ver, kind) => { const d = path.join(inst(settings(), ver), PK[kind]); if (!fs.existsSync(d)) return []; const m = store.get('meta', {})
  return fs.readdirSync(d).filter((n) => /\.zip$/i.test(n)).map((file) => { const slug = file.replace(/\.zip$/i, ''); return { file, slug, enabled: true, ...(m['pk:' + slug] || { title: slug, desc: '' }) } }) })
ipcMain.handle('pk:remove', (_, file, ver, kind) => fs.rmSync(path.join(inst(settings(), ver), PK[kind], path.basename(file)), { force: true }))
// ---------- screenshots
const inGame = (f) => path.resolve(f).startsWith(path.resolve(settings().dir))
ipcMain.handle('shots:list', () => { const s = settings(), out = [], base = path.join(s.dir, 'instances')
  const scan = (d, version) => { try { for (const n of fs.readdirSync(d)) if (/\.png$/i.test(n)) { const f = path.join(d, n); out.push({ file: f, name: n, version, mtime: fs.statSync(f).mtimeMs }) } } catch {} }
  try { for (const v of fs.readdirSync(base)) scan(path.join(base, v, 'screenshots'), v) } catch {}
  return out.sort((a, b) => b.mtime - a.mtime) })
ipcMain.handle('shots:thumb', (_, f, w) => { if (!inGame(f)) return null; try { return nativeImage.createFromPath(f).resize({ width: w || 360 }).toDataURL() } catch { return null } })
ipcMain.handle('shots:clear', async () => { let n = 0; const base = path.join(settings().dir, 'instances') // every screenshot of every version to the recycle bin
  try { for (const v of fs.readdirSync(base)) { const d = path.join(base, v, 'screenshots'); let l = []; try { l = fs.readdirSync(d) } catch {} for (const f of l) if (/\.png$/i.test(f)) { await shell.trashItem(path.join(d, f)).catch(() => {}); n++ } } } catch {}
  return n })
ipcMain.handle('shots:act', (_, f, a) => { if (!inGame(f)) return; if (a === 'copy') clipboard.writeImage(nativeImage.createFromPath(f)); else if (a === 'open') shell.showItemInFolder(f); else if (a === 'delete') return shell.trashItem(f) })
// ---------- storage overview
function dirSize(d) { let t = 0; try { for (const e of fs.readdirSync(d, { withFileTypes: true })) { const p = path.join(d, e.name); t += e.isDirectory() ? dirSize(p) : fs.statSync(p).size } } catch {} return t }
ipcMain.handle('storage:get', () => { const s = settings(), j = (...a) => path.join(s.dir, ...a)
  const profiles = (() => { try { return fs.readdirSync(j('instances')).map((n) => ({ name: n, bytes: dirSize(j('instances', n)) })) } catch { return [] } })()
  const items = [{ key: 'profiles', bytes: profiles.reduce((a, b) => a + b.bytes, 0) }, { key: 'game', bytes: dirSize(j('libraries')) + dirSize(j('assets')) + dirSize(j('versions')) },
    { key: 'java', bytes: dirSize(j('runtime')) }, { key: 'cache', bytes: dirSize(path.join(app.getPath('userData'), 'imgcache')) }]
  return { items, profiles, total: items.reduce((a, b) => a + b.bytes, 0) } })
ipcMain.handle('storage:clear', () => { fs.rmSync(path.join(app.getPath('userData'), 'imgcache'), { recursive: true, force: true }) })
// ---------- custom skin / cape: save locally + upload to the Asteria backend (shown to other launcher users by the Fabric mod)
ipcMain.handle('img:pick', async () => { const r = await dialog.showOpenDialog(win, { filters: [{ name: 'Images', extensions: ['png', 'jpg', 'jpeg', 'webp', 'gif'] }] }); const f = r.filePaths[0]; if (!f) return null
  const ext = path.extname(f).slice(1).toLowerCase(), mime = ext === 'jpg' ? 'jpeg' : ext; return `data:image/${mime};base64,` + fs.readFileSync(f).toString('base64') })
ipcMain.handle('png:pick', async () => { const r = await dialog.showOpenDialog(win, { filters: [{ name: 'PNG', extensions: ['png'] }] }); return r.filePaths[0] ? 'data:image/png;base64,' + fs.readFileSync(r.filePaths[0]).toString('base64') : null })
ipcMain.handle('skin:current', () => { const f = path.join(settings().dir, 'skins', 'current.png'); return fs.existsSync(f) ? 'data:image/png;base64,' + fs.readFileSync(f).toString('base64') : null }) // the skin set in the launcher survives a restart
ipcMain.handle('mini:get', () => { const f = path.join(settings().dir, 'skins', 'mini.png'); return fs.existsSync(f) ? 'data:image/png;base64,' + fs.readFileSync(f).toString('base64') : null })
ipcMain.handle('tex:save', async (_, { data, kind, slim }) => {
  const acc = store.get('accounts', []).find((a) => a.id === store.get('active')); const png = data ? Buffer.from(data.split(',')[1], 'base64') : null
  const d = path.join(settings().dir, 'skins'); fs.mkdirSync(d, { recursive: true }); const lf = path.join(d, kind === 'cape' ? 'cape.png' : kind === 'mini' ? 'mini.png' : 'current.png')
  if (png) fs.writeFileSync(lf, png); else fs.rmSync(lf, { force: true })
  if (acc) { const ad = accTexDir(acc.name); fs.mkdirSync(ad, { recursive: true }); png ? fs.writeFileSync(path.join(ad, path.basename(lf)), png) : fs.rmSync(path.join(ad, path.basename(lf)), { force: true }) }
  if (kind === 'skin') { store.set('slim', !!slim); for (const sf of [path.join(d, 'slim.flag'), ...(acc ? [path.join(accTexDir(acc.name), 'slim.flag')] : [])]) slim ? fs.writeFileSync(sf, '1') : fs.rmSync(sf, { force: true }) }
  if (!acc) return { uploaded: false }
  try { await resolveApi(); const tokens = store.get('tokens', {}), r = await fetch(apiBase() + '/skin', { method: 'POST', headers: { 'content-type': 'application/json' }, signal: AbortSignal.timeout(8000),
      body: JSON.stringify({ name: acc.name, kind, slim: !!slim, png: png ? png.toString('base64') : null, token: tokens[acc.name] }) }), j = await r.json()
    if (j.token) { tokens[acc.name] = j.token; store.set('tokens', tokens) } return { uploaded: !!j.ok, error: j.error } } catch { return { uploaded: false } }
})

// ---------- launch helpers: token for the nick, Discord RPC
// Discord Rich Presence (built in, on by default). The mod writes the current server to asteria-status.json every few seconds.
// Images: server icon straight from its address (Discord shows https images), launcher logo otherwise.
const RPC_DEFAULT_ID = process.env.ASTERIA_DISCORD_ID || '1564239727278686230' // Discord Application ID of AsteriaLauncher (README-SETUP.md → Discord)
// 'logo' = image uploaded in the Discord Developer Portal (Rich Presence -> Art Assets, name: logo): shows instantly, no '?'
// Discord takes https pictures directly: the icon from the public repo, no Art Asset upload needed in the Developer Portal
const LOGO_URL = 'https://raw.githubusercontent.com/FilonisSF/AsteriaLauncher/main/build/icon.png?v=al2' // ?v: Discord caches pictures by URL (it kept showing the old star)
let rpc = null, rpcReady = null, rpcT = Date.now(), rpcTimer = null, rpcLast = ''
const rpcId = () => settings().discordClientId || (store.get('remoteConf') || {}).discordClientId || RPC_DEFAULT_ID
async function rpcSend(act) {
  try { const s = settings(); if (s.discord === false || !rpcId()) return
    const key = JSON.stringify(act); if (key === rpcLast && rpc) return
    if (!rpc) { const { Client } = require('@xhayper/discord-rpc'); rpc = new Client({ clientId: rpcId() }); rpc.on('disconnected', () => { rpc = null; rpcReady = null }); rpcReady = rpc.login().catch(() => { rpc = null; rpcReady = null }) }
    await rpcReady; if (!rpc?.user) return
    await rpc.user.setActivity(act); rpcLast = key } catch { rpc = null; rpcReady = null } }
const btns = () => { const c = store.get('remoteConf') || {}, d = (c.links || []).find((l) => l.icon === 'discord'); return [{ label: 'AsteriaLauncher', url: c.site || 'https://safimine.ru' }, ...(d ? [{ label: 'Discord', url: d.url }] : [])].slice(0, 2) }
function rpcIdle() { clearInterval(rpcTimer); rpcT = Date.now(); rpcSend({ details: 'В лаунчере', state: 'Выбирает версию', startTimestamp: rpcT, largeImageKey: LOGO_URL, largeImageText: 'AsteriaLauncher', buttons: btns() }) }
function rpcGame(version, dir, joinHost) {
  clearInterval(rpcTimer); const start = Date.now(); try { fs.rmSync(path.join(dir, 'asteria-status.json'), { force: true }) } catch {}
  const tick = () => { let st = joinHost ? 'mp:' + joinHost : 'menu'
    try { const j = JSON.parse(fs.readFileSync(path.join(dir, 'asteria-status.json'), 'utf8')); if (j.ts >= start - 5000) st = j.state } catch {} // written by the mod in THIS game session (it rewrites the file on every change)
    if (st === 'mp:') st = 'mp:' + (joinHost || 'сервер') // quick-join via --server leaves no server entry in the game
    const conf = store.get('remoteConf') || {}, servers = [{ name: 'SafiMine', ip: HOST }, ...(conf.servers || [])]
    if (st.startsWith('mp:') && st.length > 3) { const ip = st.slice(3).replace(/:25565$/, ''), host = ip.split(':')[0].toLowerCase()
      const known = servers.find((x) => x.ip && host.endsWith(String(x.ip).toLowerCase().split(':')[0])), name = known?.name || ip
      return rpcSend({ details: 'Играет на ' + name, state: (settings().streamer ? 'Сервер скрыт' : ip) + ' · ' + version, startTimestamp: start,
        largeImageKey: known?.icon || `https://api.mcsrvstat.us/icon/${host}`, largeImageText: name, smallImageKey: LOGO_URL, smallImageText: 'Minecraft ' + version + ' · AsteriaLauncher', buttons: btns() }) }
    rpcSend({ details: st === 'sp' ? 'Одиночная игра' : 'В главном меню', state: 'Minecraft ' + version, startTimestamp: start, largeImageKey: LOGO_URL, largeImageText: 'AsteriaLauncher', buttons: btns() }) }
  tick(); rpcTimer = setInterval(tick, 5000) }
app.whenReady().then(() => setTimeout(() => { resolveApi().then(() => rpcIdle()) }, 2500))
ipcMain.handle('rpc:refresh', () => { rpcLast = ''; try { rpc?.destroy() } catch {} rpc = null; rpcReady = null; game ? null : rpcIdle() })
async function getToken(name) { const t = store.get('tokens', {}); if (t[name]) return t[name]
  try { const j = await (await net.fetch(apiBase() + '/claim', { method: 'POST', headers: { 'content-type': 'application/json' }, signal: AbortSignal.timeout(6000), body: JSON.stringify({ name }) })).json(); if (j.token) { t[name] = j.token; store.set('tokens', t); return j.token } } catch {} return null }
// ---------- player stats from the server plugin (via backend), remote config, Telegram feed
// clans (SafiClan launcher-sync) and server events (AsteriaBridge); the last good answer is kept for offline starts
const cachedGet = (key, url) => async () => { try { await resolveApi(); const r = await net.fetch(apiBase() + url, { signal: AbortSignal.timeout(7000) }); if (!r.ok) throw 0; const j = await r.json(); store.set(key, j); return j } catch { return store.get(key, null) } }
ipcMain.handle('clans:get', cachedGet('clansCache', '/clans'))
// how many people play through AsteriaLauncher right now (their heartbeat came in the last 2 minutes)
ipcMain.handle('online:launcher', async () => { try { await resolveApi(); const r = await net.fetch(apiBase() + '/online', { signal: AbortSignal.timeout(6000) }); const l = await r.json(); return Array.isArray(l) ? l.length : null } catch { return null } })
ipcMain.handle('events:get', cachedGet('eventsCache', '/events'))
ipcMain.handle('player:get', async (_, name) => { try { await resolveApi(); return await (await net.fetch(`${apiBase()}/player/${name}`, { signal: AbortSignal.timeout(7000) })).json() } catch { return null } })
const confFile = () => path.join(settings().dir, 'launcher-config.json')
ipcMain.handle('config:get', async () => { let c = null // priority: local file (edited by the owner of this PC) > backend file > last cached backend file
  try { await resolveApi(); const r = await net.fetch(apiBase() + '/config', { signal: AbortSignal.timeout(6000) })
    if (r.status === 404) { store.delete('remoteConf'); c = null } // the server has no config of its own: the texts built into the launcher are used (an old cached one would hide new news)
    else { if (!r.ok) throw 0; c = await r.json(); store.set('remoteConf', c) } } catch { c = store.get('remoteConf', null) }
  try { if (fs.existsSync(confFile())) c = { ...(c || {}), ...JSON.parse(fs.readFileSync(confFile(), 'utf8')) } } catch {}
  if (c) try { c = JSON.parse(JSON.stringify(c).replace(/"\/img\//g, '"' + apiBase() + '/img/')) } catch {} // pictures uploaded in the admin panel
  return c })
ipcMain.handle('config:open', (_, def) => { try { if (!fs.existsSync(confFile())) { fs.mkdirSync(path.dirname(confFile()), { recursive: true }); fs.writeFileSync(confFile(), def) } return shell.openPath(confFile()) } catch { return null } })
// official Minecraft art per version (the pictures from Mojang's own launcher patch notes); cached, falls back to the bundled posters
ipcMain.handle('versions:art', async () => { try { const j = await mrJ('https://launchercontent.mojang.com/v2/javaPatchNotes.json'), out = {}
  for (const e of j.entries || []) { const v = e.version || ((e.title || '').match(/(\d+\.\d+(?:\.\d+)?)\s*$/) || [])[1], u = e.image?.url
    if (v && u && !out[v]) out[v] = /^https?:/.test(u) ? u : 'https://launchercontent.mojang.com' + u }
  if (Object.keys(out).length) store.set('art', out); return out } catch { return store.get('art', {}) } })
const dec = (t) => t.replace(/<br\s*\/?>/g, '\n').replace(/<[^>]+>/g, '').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&nbsp;/g, ' ').replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(+n)).replace(/&#x([0-9a-f]+);/gi, (_, n) => String.fromCodePoint(parseInt(n, 16))).replace(/\n{3,}/g, '\n\n').trim()
ipcMain.handle('tg:feed', async (_, ch) => { try { const html = await (await net.fetch(`https://t.me/s/${ch}`, { signal: AbortSignal.timeout(9000) })).text(), out = []
  for (const blk of html.split('tgme_widget_message_wrap').slice(1)) {
    const text = (blk.match(/tgme_widget_message_text[^>]*>(.*?)<\/div>/s) || [])[1], link = (blk.match(/href="(https:\/\/t\.me\/[^"]+\/\d+)"/) || [])[1], date = (blk.match(/<time[^>]*datetime="([^"]+)"/) || [])[1], img = (blk.match(/tgme_widget_message_photo_wrap[^>]*background-image:url\('([^']+)'\)/) || [])[1]
    if (link && (text || img)) out.push({ text: text ? dec(text).slice(0, 700) : '', link, date, img }) }
  const r = out.slice(-6).reverse(); store.set('tg', r); return r } catch { return store.get('tg', []) } })
// ---------- launch profiles
ipcMain.handle('prof:list', () => ({ list: store.get('profiles', []), active: store.get('activeProfile') || null }))
ipcMain.handle('prof:add', (_, { name, version }) => { const id = (name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'profile') + '-' + (Date.now() % 10000), p = { id, name, version }
  store.set('profiles', [...store.get('profiles', []), p]); store.set('activeProfile', p); return p })
ipcMain.handle('prof:set', (_, id) => { const p = store.get('profiles', []).find((x) => x.id === id) || null; store.set('activeProfile', p); return p })
ipcMain.handle('prof:del', (_, id) => { store.set('profiles', store.get('profiles', []).filter((x) => x.id !== id)); if (store.get('activeProfile')?.id === id) store.set('activeProfile', null) })
// ---------- options.txt editor (FOV, sensitivity, key binds ...)
// changes made inside the game (options menu, F11, Asteria menu) reach the launcher at once: options.txt / visuals config are watched
const seenM = new Map()
setInterval(() => { if (!win) return; const s = settings()
  for (const d of instanceDirs()) for (const [kind, f] of [['opts', path.join(d, 'options.txt')], ['vis', path.join(d, 'config', 'asteria-visuals.json')], ['shots', path.join(d, 'screenshots')], ['saves', path.join(d, 'saves')], ['mods', path.join(d, 'mods')], ['packs', path.join(d, 'resourcepacks')], ['packs', path.join(d, 'shaderpacks')]]) {
    let m = 0; try { m = fs.statSync(f).mtimeMs } catch { continue }
    const k = f, old = seenM.get(k); seenM.set(k, m)
    if (old !== undefined && m !== old) try { win.webContents.send('files', { kind, dir: d, ver: path.basename(d) }) } catch {} } }, 1000)
/** the folder the settings of this version live in: the running game's own folder when it plays this version (a profile has its own) */
const optsDir = (ver) => game && gameDirNow && (gameVer === ver || path.resolve(gameDirNow) === path.resolve(inst(settings(), ver))) ? gameDirNow : inst(settings(), ver)
ipcMain.handle('opts:get', (_, ver) => { const f = path.join(optsDir(ver), 'options.txt'), o = {}; try { for (const l of fs.readFileSync(f, 'utf8').split(/\r?\n/)) { const i = l.indexOf(':'); if (i > 0) o[l.slice(0, i)] = l.slice(i + 1) } } catch {} return o })
ipcMain.handle('opts:set', (_, ver, patch) => { const d = optsDir(ver); fs.mkdirSync(d, { recursive: true }); setOpts(d, patch)
  const ext = !game && extGame && alive(extGame.pid) && extGame.dir && path.resolve(extGame.dir) === path.resolve(d)
  if ((game && d === gameDirNow) || ext) { if (game) pendingOpts = { ...pendingOpts, ...patch }; try { fs.writeFileSync(path.join(d, 'asteria-live.flag'), String(Date.now())) } catch {} return { live: true, running: true } }
  return { live: false, running: !!game } })
ipcMain.handle('game:state', async () => { if (game) return { running: true, version: gameVer }; const e = await externalGame(); return { running: !!e, version: e?.version || null, external: !!e } })
ipcMain.handle('rpc:test', () => ({ id: !!rpcId(), connected: !!rpc?.user }))
// ---------- mod integrity check (Modrinth hashes): re-downloads corrupted jars, updates outdated ones
ipcMain.handle('mods:verify', async (_, ver, fix) => { const md = path.join(inst(settings(), ver), 'mods'); if (!fs.existsSync(md)) return { total: 0, ok: 0, updated: 0, fixed: 0 }
  const files = fs.readdirSync(md).filter((n) => /^mod-.+\.jar$/.test(n)), hashes = {}; for (const f of files) hashes[f] = crypto.createHash('sha1').update(fs.readFileSync(path.join(md, f))).digest('hex')
  let upd = {}; try { upd = await (await net.fetch('https://api.modrinth.com/v2/version_files/update', { method: 'POST', headers: { 'content-type': 'application/json', 'user-agent': 'AsteriaLauncher/1.0' },
    body: JSON.stringify({ hashes: Object.values(hashes), algorithm: 'sha1', loaders: ['fabric'], game_versions: [ver] }) })).json() } catch {}
  const exp = store.get('hashes', {}); let ok = 0, updated = 0, fixed = 0
  for (const f of files) { const full = path.join(md, f), h = hashes[f], nv = upd[h], nf = nv && (nv.files.find((x) => x.primary) || nv.files[0])
    if (nf && nf.hashes.sha1 !== h) { if (fix) fs.writeFileSync(full, Buffer.from(await (await net.fetch(nf.url)).arrayBuffer())); updated++ }
    else if (!nf && exp[full] && exp[full] !== h) { if (fix) { fs.rmSync(full); await installSlug(f.match(/^mod-(.+)\.jar$/)[1], ver, md).catch(() => {}) } fixed++ }
    else ok++ }
  return { total: files.length, ok, updated, fixed } })
