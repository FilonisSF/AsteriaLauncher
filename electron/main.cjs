const { app, BrowserWindow, ipcMain, shell, dialog, net, nativeImage, clipboard, screen } = require('electron')
const { spawnSync, execFileSync } = require('child_process')
app.commandLine.appendSwitch('proxy-bypass-list', '<local>')
const os = require('os'), crypto = require('crypto')
const path = require('path'), fs = require('fs')
const Store = require('electron-store')
const { Client, Authenticator } = require('minecraft-launcher-core')
const store = new Store({ encryptionKey: 'safimine-local-key' }) // tokens stay in main process
const HOST = 'mc.safimine.ru', PORT = 25565
const API_DEFAULT = process.env.ASTERIA_API || 'https://api.safimine.ru/asteria' // your public backend (README-SETUP.md)
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
const apiBase = () => clean(settings().api) || apiNow || clean((store.get('remoteConf') || {}).api) || API_DEFAULT
async function apiAlive(b) { try { const r = await net.fetch(b + '/', { signal: AbortSignal.timeout(2500) }); return !!(await r.json()).ok } catch { return false } }
async function resolveApi(force = false) { // finds a working backend; re-checked every 2 minutes
  if (clean(settings().api)) return apiBase()
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
async function poll() {
  let d
  try {
    const { status } = require('minecraft-server-util')
    const t = Date.now(), r = await status(HOST, PORT, { timeout: 5000 })
    d = { online: true, ping: r.roundTripLatency ?? Date.now() - t, players: r.players.online, max: r.players.max || 500 }
  } catch { d = { online: false, ping: 0, players: 0, max: 500 } }
  win?.webContents.send('status', d)
}
ipcMain.handle('srv:status', async (_, host, port) => { try { const { status } = require('minecraft-server-util'), t = Date.now(), r = await status(String(host), +port || 25565, { timeout: 5000 })
  return { online: true, ping: r.roundTripLatency ?? Date.now() - t, players: r.players.online, max: r.players.max, motd: r.motd?.clean, icon: r.favicon || null, version: r.version?.name } } catch { return { online: false } } })
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
// PREMIUM visuals: only from the backend, only for PREMIUM players, plus a signed ticket; removed from mods/ when the game closes.
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
async function premiumMod(acc, version) { // -> { ticket, jar } for PREMIUM players
  const token = await getToken(acc.name); if (!token) return {}
  const r = await (await fetch(apiBase() + '/ticket', { method: 'POST', headers: { 'content-type': 'application/json' }, signal: AbortSignal.timeout(5000), body: JSON.stringify({ name: acc.name, token }) })).json().catch(() => ({}))
  if (!r.ticket) return {}
  const jar = await fetchJar([`${apiBase()}/premium/mod.jar?mc=${ENC(version)}&name=${ENC(acc.name)}&token=${token}`], path.join(modCache(), `premium-${version}.jar`), 6 * 3600000)
    || (!app.isPackaged && [path.join(__dirname, '..', 'mods', 'dist', `asteria-visuals-${version}.jar`)].find((f) => fs.existsSync(f))) || null
  return { ticket: r.ticket, jar }
}
const instanceDirs = () => { try { const b = path.join(settings().dir, 'instances'); return fs.readdirSync(b).map((n) => path.join(b, n)) } catch { return [] } }
function writeVisuals(dir, v = store.get('visuals')) { if (!v) return; try { const c = path.join(dir, 'config'); fs.mkdirSync(c, { recursive: true }); fs.writeFileSync(path.join(c, 'asteria-visuals.json'), JSON.stringify(v, null, 1)) } catch {} }
ipcMain.handle('vis:get', () => { let best = store.get('visuals', null), t = store.get('visualsAt', 0)
  for (const d of instanceDirs()) { const f = path.join(d, 'config', 'asteria-visuals.json'); try { const m = fs.statSync(f).mtimeMs; if (m > t + 1500) { best = JSON.parse(fs.readFileSync(f, 'utf8')); t = m } } catch {} }
  if (best !== store.get('visuals', null)) { store.set('visuals', best); store.set('visualsAt', t) }
  return best })
ipcMain.handle('vis:set', (_, v) => { store.set('visuals', v); store.set('visualsAt', Date.now()); for (const d of instanceDirs()) writeVisuals(d, v); return { live: !!game } }) // the mod re-reads the file every second
const OFFLINE = ['auth', 'account', 'session', 'services'].map((h) => `-Dminecraft.api.${h}.host=https://invalid.invalid`) // lets offline accounts open Multiplayer
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
async function prepareFabric(s, ver, send, legacy = false) { // legacy = Legacy Fabric for 1.8.9 / 1.12.2 (only our own mods there)
  const meta = legacy ? 'https://meta.legacyfabric.net' : 'https://meta.fabricmc.net'
  const loaders = await mrJ(`${meta}/v2/versions/loader/${ver}`)
  if (!loaders.length) return null
  const id = `${legacy ? 'legacy-' : ''}fabric-loader-${loaders[0].loader.version}-${ver}`, vd = path.join(s.dir, 'versions', id)
  fs.mkdirSync(vd, { recursive: true })
  const prof = await mrJ(`${meta}/v2/versions/loader/${ver}/${loaders[0].loader.version}/profile/json`); prof.id = id
  fs.writeFileSync(path.join(vd, id + '.json'), JSON.stringify(prof))
  const md = path.join(inst(s, ver), 'mods'); fs.mkdirSync(md, { recursive: true })
  if (legacy) return id
  send('Fabric API', 40); await installSlug('fabric-api', ver, md).catch(() => {})
  { // default mods are installed once per slug and version (a mod added to DEFAULTS later still reaches old instances); afterwards the user manages them in the Mods tab
    const all = store.get('defaultsDone', {}), done = all[ver] || []
    for (const slug of DEFAULTS) if (!done.includes(slug)) { send('Mods: ' + slug, 60); const r = await installSlug(slug, ver, md).catch(() => undefined); if (r !== undefined) done.push(slug) }
    all[ver] = done; store.set('defaultsDone', all) }
  return id
}
ipcMain.handle('mods:search', async (_, { q, ver, sort, cat }) => {
  try { const fa = [['project_type:mod'], ['categories:fabric'], ['versions:' + ver]]; if (cat) fa.push(['categories:' + cat])
    return (await mrJ(`https://api.modrinth.com/v2/search?query=${ENC(q || '')}&facets=${ENC(JSON.stringify(fa))}&index=${sort || 'downloads'}&limit=20`)).hits } catch { return [] }
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
ipcMain.handle('mods:list', async (_, ver) => {
  const md = path.join(inst(settings(), ver), 'mods'); if (!fs.existsSync(md)) return []
  const meta = store.get('meta', {}), out = []
  for (const file of fs.readdirSync(md).filter((n) => /\.jar(\.disabled)?$/.test(n) && !/^asteria-/.test(n))) {
    const slug = (file.match(/^mod-(.+?)\.jar/) || [])[1]
    if (slug && !meta[slug]) { try { const p = await mrJ(`https://api.modrinth.com/v2/project/${slug}`); meta[slug] = { title: p.title, icon: p.icon_url, desc: p.description } } catch {} }
    out.push({ file, slug, enabled: !file.endsWith('.disabled'), ...(meta[slug] || { title: file.replace(/\.jar(\.disabled)?$/, ''), desc: '' }) })
  }
  store.set('meta', meta); return out
})
ipcMain.handle('mods:toggle', (_, file, ver) => { const d = path.join(inst(settings(), ver), 'mods'), f = path.join(d, path.basename(file))
  fs.renameSync(f, f.endsWith('.disabled') ? f.slice(0, -9) : f + '.disabled') })
ipcMain.handle('mods:remove', (_, file, ver) => fs.rmSync(path.join(inst(settings(), ver), 'mods', path.basename(file)), { force: true }))
ipcMain.handle('cosm:set', (_, c) => store.set('cosmetics', c))
app.whenReady().then(() => { create(); poll(); setInterval(poll, 30000) })
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
  } catch (e) { return { error: 'Microsoft: ' + (e?.message || e) } }
})
ipcMain.handle('acc:select', (_, id) => store.set('active', id))
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
const LANGMAP = { ru: 'ru_ru', en: 'en_us', uk: 'uk_ua', pl: 'pl_pl', de: 'de_de', es: 'es_es', fr: 'fr_fr', it: 'it_it', pt: 'pt_br', tr: 'tr_tr', zh: 'zh_cn', ja: 'ja_jp', ko: 'ko_kr', nl: 'nl_nl', cs: 'cs_cz' }
function setOpts(dir, o) { // syncs the in-game language with the launcher language (options.txt)
  try { const f = path.join(dir, 'options.txt'); let t = fs.existsSync(f) ? fs.readFileSync(f, 'utf8') : ''
    for (const [k, v] of Object.entries(o)) { const re = new RegExp('^' + k + ':.*$', 'm'); t = re.test(t) ? t.replace(re, k + ':' + v) : (t ? t.replace(/\n?$/, '\n') : '') + k + ':' + v + '\n' }
    fs.writeFileSync(f, t) } catch {}
}
function findJava(s) { // javaw has no console window
  if (s.java) return s.java.replace(/java\.exe$/i, 'javaw.exe')
  try { return require('child_process').execFileSync('where', ['javaw'], { encoding: 'utf8' }).split(/\r?\n/)[0].trim() || undefined } catch { return undefined }
}
let game = null, gameVer = null, logBuf = [], pendingOpts = {}
ipcMain.handle('game:kill', () => { try { game?.kill() } catch {} })
ipcMain.handle('game:log', () => logBuf.slice(-400).join('\n'))
ipcMain.handle('stats:get', () => ({ playMs: store.get('playMs', 0), launches: store.get('launches', 0), perVer: store.get('perVer', {}) }))
ipcMain.handle('game:launch', async (_, { version, join, lang, server }) => {
  const srv = server && server.host ? { host: String(server.host), port: +server.port || 25565 } : { host: HOST, port: PORT }
  if (game) return { error: 'Игра уже запущена' }
  const s = settings(), acc = store.get('accounts', []).find((a) => a.id === store.get('active'))
  if (!acc) return { error: 'Выберите аккаунт' }
  const send = (stage, pct, detail = '', step = 0) => { try { win?.webContents.send('progress', { stage, pct, detail, step }) } catch {} }
  const dir = inst(s, version); fs.mkdirSync(dir, { recursive: true }); setOpts(dir, { lang: LANGMAP[lang] || 'en_us', fullscreen: !!s.fullscreen, overrideWidth: s.fullscreen ? 0 : s.width, overrideHeight: s.fullscreen ? 0 : s.height })
  const l = new Client(); let started = false, t0 = 0, hb = null; logBuf = []; pendingOpts = {}
  const md = path.join(dir, 'mods'); fs.mkdirSync(md, { recursive: true })
  for (const f of ['asteria-mod.jar', 'asteria-core.jar', 'asteria-visuals.jar']) fs.rmSync(path.join(md, f), { force: true })
  try {
    send('Подготовка', 0, '', 0); let custom = null, core = null, prem = {}; await resolveApi()
    const javaPath = await ensureJava(s, version, send)
    const modern = /^(1\.(1[6-9]|2\d)|26\.)/.test(version), legacy = /^1\.(8|12)\./.test(version)
    const wantFabric = (s.loaders || {})[version] ? s.loaders[version] === 'fabric' : s.fabric !== false
    if (wantFabric && (modern || legacy)) { // our mods come first: the loader for 1.8.9 / 1.12.2 is installed only if our mod exists for it
      send('Asteria', 2, '', 2); [core, prem] = await Promise.all([coreMod(version).catch(() => null), premiumMod(acc, version).catch(() => ({}))]) // in parallel: a slow backend must not delay the game
      if (modern || core) { send('Fabric', 5, '', 2); custom = await prepareFabric(s, version, (st, p) => send('Fabric', p, st, 2), legacy).catch(() => null) } }
    if (custom && core) fs.copyFileSync(core, path.join(md, 'asteria-core.jar'))
    if (custom && prem.jar) fs.copyFileSync(prem.jar, path.join(md, 'asteria-visuals.jar'))
    writeVisuals(dir)
    const opts = { root: s.dir, overrides: { gameDirectory: dir }, authorization: acc.type === 'offline' ? offlineAuth(acc.name) : acc.auth, version: { number: version, type: 'release', ...(custom ? { custom } : {}) },
      memory: { max: s.ramMb + 'M', min: Math.min(s.ramMb, 2048) + 'M' }, customLaunchArgs: ['--width', String(s.width), '--height', String(s.height), ...(s.fullscreen ? ['--fullscreen'] : [])], javaPath,
      customArgs: [...s.jvm.split(/\s+/).filter(Boolean), '-Dasteria.launcher=1', '-Dasteria.api=' + apiBase(), '-Dasteria.user=' + acc.name, '-Dasteria.gamedir=' + dir, '-Dasteria.root=' + s.dir,
        ...(prem.ticket ? ['-Dasteria.ticket=' + prem.ticket] : []), ...(acc.type === 'offline' ? OFFLINE : [])],
      ...(join ? { server: { host: srv.host, port: String(srv.port) } } : {}) }
    l.on('progress', (e) => send('Загрузка файлов', e.total ? Math.round((e.task / e.total) * 100) : 0, e.type || '', 3))
    l.on('download-status', (e) => send('Загрузка библиотек', e.total ? Math.round((e.current / e.total) * 100) : 0, e.name || '', 3))
    l.on('package-extract', () => send('Распаковка', 100, '', 4))
    l.on('debug', (m) => { logBuf.push(String(m)); if (logBuf.length > 1000) logBuf.splice(0, 300) })
    l.on('data', (m) => { // NOTE: no per-line IPC — joining a world prints thousands of lines
      logBuf.push(String(m).trimEnd()); if (logBuf.length > 1000) logBuf.splice(0, 300)
      if (!started) { started = true; t0 = Date.now(); rpcGame(version, dir, join ? srv.host : null); send('', 0); try { win?.webContents.send('game', true) } catch {}; if (s.hideOnLaunch !== false) win?.minimize() } })
    l.on('close', () => { if (hb) clearInterval(hb); game = null; gameVer = null
      fs.rmSync(path.join(md, 'asteria-visuals.jar'), { force: true }); fs.rmSync(path.join(dir, 'asteria-live.flag'), { force: true })
      if (Object.keys(pendingOpts).length) { setOpts(dir, pendingOpts); pendingOpts = {} } // the game may rewrite options.txt on exit: keep what was changed in the launcher
      if (t0) { const el = Date.now() - t0, pv = store.get('perVer', {}); store.set('playMs', store.get('playMs', 0) + el); pv[version] = (pv[version] || 0) + el; store.set('perVer', pv); store.set('launches', store.get('launches', 0) + 1) }
      rpcIdle()
      try { send('', 0); win?.webContents.send('game', false); if (win?.isMinimized()) win.restore() } catch {} })
    const tok = await getToken(acc.name).catch(() => null)
    const beat = () => fetch(apiBase() + '/heartbeat', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ name: acc.name, token: tok, ...store.get('cosmetics', {}) }) }).catch(() => {}); beat(); hb = setInterval(beat, 45000) // registers right away, so other launcher players see the icon at once
    await addServer(dir).catch(() => {}); for (const x of (store.get('remoteConf') || {}).servers || []) if (x.ip && x.ip !== HOST) await addServerEntry(dir, x.name || x.ip, x.port && +x.port !== 25565 ? x.ip + ':' + x.port : x.ip).catch(() => {})
    game = await l.launch(opts); gameVer = version
    if (!game) { clearInterval(hb); send('', 0); return { error: 'Не удалось запустить игру' } }
    send('Запуск игры', 100, '', 5)
    return { ok: true }
  } catch (e) { if (hb) clearInterval(hb); game = null; send('', 0); return { error: String(e?.message || e) } }
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
  const check = () => autoUpdater.checkForUpdates().catch(() => {}); check(); setInterval(check, 30 * 60 * 1000)
}
ipcMain.handle('update:install', () => autoUpdater?.quitAndInstall())
process.on('uncaughtException', (e) => { try { logBuf.push('[launcher] ' + (e?.stack || e)) } catch {} })
process.on('unhandledRejection', (e) => { try { logBuf.push('[launcher] ' + e) } catch {} })
app.on('render-process-gone', () => { try { win?.reload() } catch {} })
app.whenReady().then(setupUpdates)

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
ipcMain.handle('pk:search', async (_, { q, ver, sort, kind }) => { try { const fa = [['project_type:' + kind], ['versions:' + ver]]
  return (await mrJ(`https://api.modrinth.com/v2/search?query=${ENC(q || '')}&facets=${ENC(JSON.stringify(fa))}&index=${sort || 'downloads'}&limit=20`)).hits } catch { return [] } })
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
ipcMain.handle('tex:save', async (_, { data, kind, slim }) => {
  const acc = store.get('accounts', []).find((a) => a.id === store.get('active')); const png = data ? Buffer.from(data.split(',')[1], 'base64') : null
  const d = path.join(settings().dir, 'skins'); fs.mkdirSync(d, { recursive: true }); const lf = path.join(d, kind === 'cape' ? 'cape.png' : 'current.png')
  if (png) fs.writeFileSync(lf, png); else fs.rmSync(lf, { force: true })
  if (kind === 'skin') { store.set('slim', !!slim); const sf = path.join(d, 'slim.flag'); slim ? fs.writeFileSync(sf, '1') : fs.rmSync(sf, { force: true }) }
  if (!acc) return { uploaded: false }
  try { await resolveApi(); const tokens = store.get('tokens', {}), r = await fetch(apiBase() + '/skin', { method: 'POST', headers: { 'content-type': 'application/json' }, signal: AbortSignal.timeout(8000),
      body: JSON.stringify({ name: acc.name, kind, slim: !!slim, png: png ? png.toString('base64') : null, token: tokens[acc.name] }) }), j = await r.json()
    if (j.token) { tokens[acc.name] = j.token; store.set('tokens', tokens) } return { uploaded: !!j.ok, error: j.error } } catch { return { uploaded: false } }
})

// ---------- launch helpers: token for the nick, Discord RPC
// Discord Rich Presence (built in, on by default). The mod writes the current server to asteria-status.json every few seconds.
// Images: server icon straight from its address (Discord shows https images), launcher logo otherwise.
const RPC_DEFAULT_ID = process.env.ASTERIA_DISCORD_ID || '1564239727278686230' // Discord Application ID of AsteriaLauncher (README-SETUP.md → Discord)
const LOGO_URL = 'https://raw.githubusercontent.com/FilonisSF/AsteriaLauncher/main/build/icon.png'
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
  clearInterval(rpcTimer); const start = Date.now()
  const tick = () => { let st = joinHost ? 'mp:' + joinHost : 'menu'
    try { const j = JSON.parse(fs.readFileSync(path.join(dir, 'asteria-status.json'), 'utf8')); if (Date.now() - j.ts < 20000) st = j.state } catch {}
    const conf = store.get('remoteConf') || {}, servers = [{ name: 'SafiMine', ip: HOST }, ...(conf.servers || [])]
    if (st.startsWith('mp:') && st.length > 3) { const ip = st.slice(3).replace(/:25565$/, ''), host = ip.split(':')[0].toLowerCase()
      const known = servers.find((x) => x.ip && host.endsWith(String(x.ip).toLowerCase().split(':')[0])), name = known?.name || ip
      return rpcSend({ details: 'Играет на ' + name, state: (settings().streamer ? 'Сервер скрыт' : ip) + ' · ' + version, startTimestamp: start,
        largeImageKey: known?.icon || `https://api.mcsrvstat.us/icon/${host}`, largeImageText: name, smallImageKey: LOGO_URL, smallImageText: 'Minecraft ' + version + ' · AsteriaLauncher', buttons: btns() }) }
    rpcSend({ details: st === 'sp' ? 'Одиночная игра' : 'В главном меню', state: 'Minecraft ' + version, startTimestamp: start, largeImageKey: LOGO_URL, largeImageText: 'AsteriaLauncher', buttons: btns() }) }
  tick(); rpcTimer = setInterval(tick, 15000) }
app.whenReady().then(() => setTimeout(() => { resolveApi().then(() => rpcIdle()) }, 2500))
ipcMain.handle('rpc:refresh', () => { rpcLast = ''; try { rpc?.destroy() } catch {} rpc = null; rpcReady = null; game ? null : rpcIdle() })
async function getToken(name) { const t = store.get('tokens', {}); if (t[name]) return t[name]
  try { const j = await (await net.fetch(apiBase() + '/claim', { method: 'POST', headers: { 'content-type': 'application/json' }, signal: AbortSignal.timeout(6000), body: JSON.stringify({ name }) })).json(); if (j.token) { t[name] = j.token; store.set('tokens', t); return j.token } } catch {} return null }
// ---------- player stats from the server plugin (via backend), remote config, Telegram feed
// clans (SafiClan launcher-sync) and server events (AsteriaBridge); the last good answer is kept for offline starts
const cachedGet = (key, url) => async () => { try { await resolveApi(); const r = await net.fetch(apiBase() + url, { signal: AbortSignal.timeout(7000) }); if (!r.ok) throw 0; const j = await r.json(); store.set(key, j); return j } catch { return store.get(key, null) } }
ipcMain.handle('clans:get', cachedGet('clansCache', '/clans'))
ipcMain.handle('events:get', cachedGet('eventsCache', '/events'))
ipcMain.handle('player:get', async (_, name) => { try { await resolveApi(); return await (await net.fetch(`${apiBase()}/player/${name}`, { signal: AbortSignal.timeout(7000) })).json() } catch { return null } })
const confFile = () => path.join(settings().dir, 'launcher-config.json')
ipcMain.handle('config:get', async () => { let c = null // priority: local file (edited by the owner of this PC) > backend file > last cached backend file
  try { await resolveApi(); const r = await net.fetch(apiBase() + '/config', { signal: AbortSignal.timeout(6000) }); if (!r.ok) throw 0; c = await r.json(); store.set('remoteConf', c) } catch { c = store.get('remoteConf', null) }
  try { if (fs.existsSync(confFile())) c = { ...(c || {}), ...JSON.parse(fs.readFileSync(confFile(), 'utf8')) } } catch {}
  if (c) try { c = JSON.parse(JSON.stringify(c).replace(/"\/img\//g, '"' + apiBase() + '/img/')) } catch {} // pictures uploaded in the admin panel
  return c })
ipcMain.handle('config:open', (_, def) => { try { if (!fs.existsSync(confFile())) { fs.mkdirSync(path.dirname(confFile()), { recursive: true }); fs.writeFileSync(confFile(), def) } return shell.openPath(confFile()) } catch { return null } })
// official Minecraft art per version (the pictures from Mojang's own launcher patch notes); cached, falls back to the bundled posters
ipcMain.handle('versions:art', async () => { try { const j = await mrJ('https://launchercontent.mojang.com/v2/javaPatchNotes.json'), out = {}
  for (const e of j.entries || []) { const v = e.version || ((e.title || '').match(/(\d+\.\d+(?:\.\d+)?)\s*$/) || [])[1], u = e.image?.url
    if (v && u && !out[v]) out[v] = /^https?:/.test(u) ? u : 'https://launchercontent.mojang.com' + u }
  if (Object.keys(out).length) store.set('art', out); return out } catch { return store.get('art', {}) } })
const dec = (t) => t.replace(/<br\s*\/?>/g, '\n').replace(/<[^>]+>/g, '').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&nbsp;/g, ' ').trim()
ipcMain.handle('tg:feed', async (_, ch) => { try { const html = await (await net.fetch(`https://t.me/s/${ch}`, { signal: AbortSignal.timeout(9000) })).text(), out = []
  for (const blk of html.split('tgme_widget_message_wrap').slice(1)) {
    const text = (blk.match(/tgme_widget_message_text[^>]*>(.*?)<\/div>/s) || [])[1], link = (blk.match(/href="(https:\/\/t\.me\/[^"]+\/\d+)"/) || [])[1], date = (blk.match(/<time[^>]*datetime="([^"]+)"/) || [])[1], img = (blk.match(/tgme_widget_message_photo_wrap[^>]*background-image:url\('([^']+)'\)/) || [])[1]
    if (link && (text || img)) out.push({ text: text ? dec(text).slice(0, 220) : '', link, date, img }) }
  const r = out.slice(-6).reverse(); store.set('tg', r); return r } catch { return store.get('tg', []) } })
// ---------- launch profiles
ipcMain.handle('prof:list', () => ({ list: store.get('profiles', []), active: store.get('activeProfile') || null }))
ipcMain.handle('prof:add', (_, { name, version }) => { const id = (name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'profile') + '-' + (Date.now() % 10000), p = { id, name, version }
  store.set('profiles', [...store.get('profiles', []), p]); store.set('activeProfile', p); return p })
ipcMain.handle('prof:set', (_, id) => { const p = store.get('profiles', []).find((x) => x.id === id) || null; store.set('activeProfile', p); return p })
ipcMain.handle('prof:del', (_, id) => { store.set('profiles', store.get('profiles', []).filter((x) => x.id !== id)); if (store.get('activeProfile')?.id === id) store.set('activeProfile', null) })
// ---------- options.txt editor (FOV, sensitivity, key binds ...)
ipcMain.handle('opts:get', (_, ver) => { const f = path.join(inst(settings(), ver), 'options.txt'), o = {}; try { for (const l of fs.readFileSync(f, 'utf8').split(/\r?\n/)) { const i = l.indexOf(':'); if (i > 0) o[l.slice(0, i)] = l.slice(i + 1) } } catch {} return o })
ipcMain.handle('opts:set', (_, ver, patch) => { const d = inst(settings(), ver); fs.mkdirSync(d, { recursive: true }); setOpts(d, patch)
  if (game && gameVer === ver) { pendingOpts = { ...pendingOpts, ...patch }; try { fs.writeFileSync(path.join(d, 'asteria-live.flag'), String(Date.now())) } catch {} return { live: true, running: true } }
  return { live: false, running: !!game } })
ipcMain.handle('game:state', () => ({ running: !!game, version: gameVer }))
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
