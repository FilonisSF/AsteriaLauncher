// AsteriaLauncher backend. Start:  node backend/server.cjs   (or start-backend.bat)
// Admin panel:  http://<address>:8080/admin   (key is printed on start and saved to backend/data/admin-key.txt)
// Env (optional): PORT=8080  ADMIN_KEY=...  STATS_KEY=...
const http = require('http'), fs = require('fs'), path = require('path'), crypto = require('crypto')
const VERSION = '1.6.2'
const DATA = path.join(__dirname, 'data'); fs.mkdirSync(DATA, { recursive: true })
for (const d of ['premium', 'mods', 'skins']) fs.mkdirSync(path.join(DATA, d), { recursive: true })
const rd = (f, d) => { try { return JSON.parse(fs.readFileSync(path.join(DATA, f), 'utf8')) } catch { return d } }
const wr = (f, o) => fs.writeFileSync(path.join(DATA, f), JSON.stringify(o, null, 1))
const secret = (env, file) => { if (process.env[env]) return process.env[env]; const f = path.join(DATA, file); if (!fs.existsSync(f)) fs.writeFileSync(f, crypto.randomBytes(18).toString('base64url')); return fs.readFileSync(f, 'utf8').trim() }
const ADMIN = secret('ADMIN_KEY', 'admin-key.txt'), SKEY = secret('STATS_KEY', 'stats-key.txt')

// ---- PREMIUM tickets are signed with this key; its public half is compiled into the visuals mod (tools/gen-keys.cjs)
const KEYFILE = path.join(DATA, 'ticket-private.pem')
// jars uploaded in the admin panel win; otherwise the ones built in this project (mods/dist) are served
const DIST = path.join(__dirname, '..', 'mods', 'dist')
const jarFile = (sub, name) => [path.join(DATA, sub, name), path.join(DIST, name)].find((f) => fs.existsSync(f)) || path.join(DATA, sub, name)
if (!fs.existsSync(KEYFILE)) console.warn('!! backend/data/ticket-private.pem is missing: run `node tools/gen-keys.cjs` and rebuild the mods, otherwise PREMIUM visuals stay locked')
const signTicket = (name, until) => { const p = Buffer.from(`v1|${name}|${until}`); return p.toString('base64url') + '.' + crypto.sign('sha256', p, fs.readFileSync(KEYFILE)).toString('base64url') }

const tokens = rd('tokens.json', {}), stats = rd('stats.json', {}), seenDb = rd('seen.json', {})
let plans = rd('plans.json', {})
let clans = rd('clans.json', null), events = rd('events.json', null) // pushed by the SafiClan plugin / AsteriaBridge
// old format: { name: 'PREMIUM' } -> { name: { plan, until } }
for (const [k, v] of Object.entries(plans)) if (typeof v === 'string') plans[k] = { plan: v, until: 0 }
const online = new Map(), cosm = new Map(), presence = new Map()
// change stamps: the mods poll the tiny /rev, /revs and re-download a player's skin / cape / cosmetics only when they changed
const revs = new Map(); let revAll = Date.now(); const bump = (n) => { revAll = Date.now(); revs.set(n, revAll) }, setCosm = (n, c) => { if (JSON.stringify(cosm.get(n)) !== JSON.stringify(c)) bump(n); cosm.set(n, c) }
const NAME = /^\w{3,16}$/, MC = /^\d+\.\d+(\.\d+)?$/, PNG = Buffer.from([0x89, 0x50, 0x4e, 0x47])
const clanOf = (n) => (clans?.clans || []).find((c) => (c.members || []).some((x) => String(x.name).toLowerCase() === n))
const authed = (n, t) => !!(tokens[n] && t && tokens[n] === t)
// a launcher that already holds a token for a nick this backend has never seen (new backend / restored data): adopt it
const adopt = (n, t) => { if (!tokens[n] && typeof t === 'string' && /^[0-9a-f]{32}$/.test(t)) { tokens[n] = t; wr('tokens.json', tokens) } }
const planOf = (n) => { const p = plans[n]; if (!p || p.plan !== 'PREMIUM') return { plan: 'FREE', until: 0 }; if (p.until && p.until < Date.now()) return { plan: 'FREE', until: p.until, expired: true }; return { plan: 'PREMIUM', until: p.until || 0 } }
setInterval(() => wr('seen.json', seenDb), 60000)
// friends: { nick: { f: [Nick...], in: [Nick...] requests to me, out: [Nick...] my requests, notes: [{ k: 'accepted' | 'declined' | 'request', who, t }] } }
const friendsDb = rd('friends.json', {}); let friendsDirty = false; setInterval(() => { if (friendsDirty) { wr('friends.json', friendsDb); friendsDirty = false } }, 5000)
const seenName = {} // nick as the player types it (case), for requests by code
const fr = (n) => (friendsDb[n] = friendsDb[n] || { f: [], in: [], out: [], notes: [] })
const without = (l, who) => l.filter((x) => x.toLowerCase() !== who.toLowerCase())
// friend codes: requests only by code (a nick alone is not enough), XXXX-XXXX without look-alike letters
const fcodes = rd('friend-codes.json', {}), fbyCode = Object.fromEntries(Object.entries(fcodes).map(([n, c]) => [c, n]))
const fcode = (n) => { if (!fcodes[n]) { const A = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; let c; do { c = Array.from(crypto.randomBytes(8), (b) => A[b % A.length]).join(''); c = c.slice(0, 4) + '-' + c.slice(4) } while (fbyCode[c]); fcodes[n] = c; fbyCode[c] = n; wr('friend-codes.json', fcodes) } return fcodes[n] }
const note = (n, k, who, x = {}) => { const d = fr(n); d.notes = [...d.notes, { k, who, t: Date.now(), ...x }].slice(-30); friendsDirty = true }
// chat between friends: the last 300 messages of each pair
const chats = rd('chats.json', {}); let chatsDirty = false; setInterval(() => { if (chatsDirty) { wr('chats.json', chats); chatsDirty = false } }, 5000)
const pair = (a, b) => [a.toLowerCase(), b.toLowerCase()].sort().join('|')

// maps: CurseForge worlds (gameId 432, classId 17); answers are cached for 10 minutes
const CF = 'https://api.curseforge.com/v1', cfCache = new Map()
async function cf(p) { const key = process.env.CF_API_KEY; if (!key) return { error: 'no key' }
  const hit = cfCache.get(p); if (hit && Date.now() - hit.t < 600000) return hit.j
  const r = await fetch(CF + p, { headers: { 'x-api-key': key, accept: 'application/json' }, signal: AbortSignal.timeout(15000) }); if (r.status === 403) return { error: 'forbidden' }; if (!r.ok) return { error: 'curseforge ' + r.status }
  const j = await r.json(); cfCache.set(p, { t: Date.now(), j }); if (cfCache.size > 500) cfCache.delete(cfCache.keys().next().value); return j }
const mapOf = (m) => ({ id: m.id, name: m.name, desc: m.summary, img: m.screenshots?.[0]?.thumbnailUrl || m.logo?.thumbnailUrl || '', logo: m.logo?.thumbnailUrl || '', downloads: m.downloadCount, author: m.authors?.[0]?.name || '',
  url: m.links?.websiteUrl || '', updated: m.dateModified, cats: (m.categories || []).map((c) => c.name).slice(0, 3),
  files: (m.latestFilesIndexes || []).filter((f) => /^d+.d+(.d+)?$/.test(f.gameVersion)).map((f) => ({ id: f.fileId, ver: f.gameVersion, name: f.filename })).slice(0, 30), free: m.allowModDistribution !== false })

const server = http.createServer((req, res) => {
  res.setHeader('access-control-allow-origin', '*'); res.setHeader('access-control-allow-headers', 'content-type,x-admin,x-key')
  if (req.method === 'OPTIONS') { res.statusCode = 204; return res.end() }
  const u = new URL(req.url, 'http://x'), url = u.pathname.replace(/\/+$/, '').replace(/^\/asteria(?=\/|$)/, '') || '/', q = u.searchParams
  const json = (o, c = 200) => { res.statusCode = c; res.setHeader('content-type', 'application/json; charset=utf-8'); res.end(JSON.stringify(o)) }
  const file = (f, type) => { if (!fs.existsSync(f)) { res.statusCode = 404; return res.end() } res.setHeader('content-type', type); res.setHeader('cache-control', 'no-cache'); fs.createReadStream(f).pipe(res) }
  const admin = () => req.headers['x-admin'] === ADMIN
  const body = (limit, cb) => { const ch = []; let n = 0; req.on('data', (c) => { n += c.length; if (n > limit) { json({ error: 'too large' }, 413); req.destroy() } else ch.push(c) }).on('end', () => cb(Buffer.concat(ch))) }
  const jbody = (cb) => body(4_000_000, (b) => { let j; try { j = JSON.parse(b.toString() || '{}') } catch { return json({ error: 'bad json' }, 400) } cb(j) })

  // ------------------------------------------------------------------ public
  if (req.method === 'GET') {
    if (url === '/' ) return json({ ok: true, name: 'AsteriaLauncher backend', version: VERSION })
    if (url === '/admin') return file(path.join(__dirname, 'admin.html'), 'text/html; charset=utf-8')
    let m
    if ((m = url.match(/^\/model\/(\w{3,16})\/(pet|hat|back|wings)\.json$/))) return file(path.join(DATA, 'models', m[1].toLowerCase() + '-' + m[2] + '.json'), 'application/json') // own 3D models of players
    if ((m = url.match(/^\/backup\/([A-Z0-9]{10})$/i))) return file(path.join(DATA, 'backups', m[1].toUpperCase() + '.json'), 'application/json') // everything of a launcher, by its code
    if ((m = url.match(/^\/share\/([A-Z0-9]{6})$/i))) return file(path.join(DATA, 'share', m[1].toUpperCase() + '.json'), 'application/json') // things shared between friends
    if ((m = url.match(/^\/vcfg\/([A-Z0-9]{6})$/i))) return file(path.join(DATA, 'vcfg', m[1].toUpperCase() + '.json'), 'application/json') // shared Asteria Visuals configs
    if ((m = url.match(/^\/(skin|cape|mini)\/(\w{3,16})\.png$/))) return file(path.join(DATA, 'skins', `${m[1]}-${m[2].toLowerCase()}.png`), 'image/png')
    if ((m = url.match(/^\/skinmeta\/(\w{3,16})$/))) return json({ slim: fs.existsSync(path.join(DATA, 'skins', `slim-${m[1].toLowerCase()}`)) })
    if ((m = url.match(/^\/player\/(\w{3,16})$/))) { const n = m[1].toLowerCase(), c = clanOf(n); return json({ ...planOf(n), ...(stats[n] || { stats: null }), clan: c ? { name: c.name, display: c.display, level: c.level, role: c.members.find((x) => x.name.toLowerCase() === n)?.roleText } : null }) }
    if (url === '/clans') return json(clans || { clans: [], time: 0 })
    if ((m = url.match(/^\/clans\/(\w{1,32})$/))) { const c = (clans?.clans || []).find((x) => x.name.toLowerCase() === m[1].toLowerCase()); return c ? json(c) : json({ error: 'not found' }, 404) }
    if (url === '/events') return json(events || { events: {}, time: 0 })
    if ((m = url.match(/^\/mods\/(asteria-core-[\w.]+\.jar|AsteriaBridge\.jar)$/))) return file(jarFile('mods', m[1]), 'application/java-archive')
    if ((m = url.match(/^\/img\/([0-9a-f]{16}\.(png|jpg|webp|gif))$/))) return file(path.join(DATA, 'img', m[1]), 'image/' + (m[2] === 'jpg' ? 'jpeg' : m[2]))
    if (url === '/presence') { const now = Date.now(), out = {} // friends in the launcher: in game?, version, server, e4mc address of an open world
      for (const nm of String(q.get('names') || '').split(',').slice(0, 100)) { const k = nm.toLowerCase(); if (!NAME.test(nm)) continue; const t = online.get(k), p = presence.get(k) || {}; out[nm] = { online: !!t && now - t < 120000, ...(t && now - t < 120000 ? p : {}) } }
      return json(out) }
    if (url === '/online') { const now = Date.now(); return json([...online].filter(([, t]) => now - t < 120000).map(([n]) => n)) }
    if (url === '/cosmetics') return json(Object.fromEntries(cosm))
    if (url === '/rev') return json({ t: revAll })
    if (url === '/revs') { const now = Date.now(), out = {}; for (const [n, t] of online) if (now - t < 120000) out[n] = revs.get(n) || 0; return json(out) }
    if (url === '/maps/search') { const p = new URLSearchParams({ gameId: '432', classId: '17', pageSize: '24', index: String(Math.max(0, Math.min(400, +q.get('page') || 0)) * 24), sortOrder: 'desc', sortField: { downloads: '6', newest: '11', updated: '3' }[q.get('sort')] || '2' })
      if (q.get('q')) p.set('searchFilter', String(q.get('q')).slice(0, 60)); if (MC.test(q.get('ver') || '')) p.set('gameVersion', q.get('ver'))
      cf('/mods/search?' + p).then((j) => j.error ? json(j, j.error === 'no key' ? 503 : 502) : json({ maps: (j.data || []).map(mapOf), total: j.pagination?.totalCount || 0 })).catch((e) => json({ error: String(e.message || e) }, 502)); return }
    if (url === '/maps/file') { const mod = +q.get('mod'), fid = +q.get('file'); if (!mod || !fid) return json({ error: 'bad id' }, 400)
      cf('/mods/' + mod + '/files/' + fid).then((j) => { if (j.error) return json(j, j.error === 'no key' ? 503 : 502); const f = j.data; if (!f || f.modId !== mod) return json({ error: 'not found' }, 404); json({ url: f.downloadUrl || '', name: f.fileName, size: f.fileLength }) }).catch((e) => json({ error: String(e.message || e) }, 502)); return }
    if (url === '/config') return fs.existsSync(path.join(DATA, 'launcher-config.json')) ? file(path.join(DATA, 'launcher-config.json'), 'application/json') : json({}, 404)
    if (url === '/premium/mod.jar') { // the PREMIUM mod is given only to PREMIUM players (and is useless without a signed ticket anyway)
      const n = (q.get('name') || '').toLowerCase(), mc = q.get('mc') || '1.16.5'; adopt(n, q.get('token'))
      if (!MC.test(mc) || planOf(n).plan !== 'PREMIUM' || !authed(n, q.get('token'))) return json({ error: 'forbidden' }, 403)
      return file(jarFile('premium', `asteria-visuals-${mc}.jar`), 'application/java-archive') }
    // ---- admin (read)
    if (url === '/admin/api/players') { if (!admin()) return json({ error: 'forbidden' }, 403)
      const names = new Set([...Object.keys(plans), ...Object.keys(seenDb), ...Object.keys(stats), ...Object.keys(tokens)])
      const now = Date.now()
      return json([...names].map((n) => ({ name: n, ...planOf(n), seen: seenDb[n] || 0, online: now - (online.get(n) || 0) < 120000, stats: stats[n]?.stats || null })).sort((a, b) => (b.online - a.online) || (b.seen - a.seen))) }
    if (url === '/admin/api/files') { if (!admin()) return json({ error: 'forbidden' }, 403)
      const ls = (d, re) => { const out = new Map(); for (const [dir, src] of [[DIST, 'build'], [path.join(DATA, d), 'upload']]) { try { for (const f of fs.readdirSync(dir).filter((x) => re.test(x))) out.set(f, { file: f, size: fs.statSync(path.join(dir, f)).size, mtime: fs.statSync(path.join(dir, f)).mtimeMs, src }) } catch {} } return [...out.values()] }
      return json({ premium: ls('premium', /^asteria-visuals-.*\.jar$/), mods: ls('mods', /^(asteria-core-.*|AsteriaBridge)\.jar$/), keyOk: fs.existsSync(KEYFILE), statsKey: SKEY }) }
    if (url === '/admin/api/config') { if (!admin()) return json({ error: 'forbidden' }, 403) // empty -> start from the launcher's built-in content; new sections (events…) are filled in from it too
      let def = {}; try { def = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'src', 'config.default.json'), 'utf8')) } catch {}
      const c = rd('launcher-config.json', null) || def; for (const k of ['events', 'eventsIntro']) if (c[k] === undefined && def[k] !== undefined) c[k] = def[k]
      return json(c) }
    return json({ error: 'not found' }, 404)
  }
  if (req.method !== 'POST') return json({ error: 'not found' }, 404)

  // ------------------------------------------------------------------ admin (write)
  if (url === '/admin/image') { // images for news / servers, served at /img/<name>
    if (!admin()) return json({ error: 'forbidden' }, 403)
    return body(8_000_000, (b) => { const ext = b.subarray(0, 4).equals(PNG) ? 'png' : b[0] === 0xff && b[1] === 0xd8 ? 'jpg' : b.subarray(8, 12).toString() === 'WEBP' ? 'webp' : b.subarray(0, 3).toString() === 'GIF' ? 'gif' : null
      if (!ext) return json({ error: 'not an image' }, 400)
      const name = crypto.createHash('sha1').update(b).digest('hex').slice(0, 16) + '.' + ext; fs.mkdirSync(path.join(DATA, 'img'), { recursive: true }); fs.writeFileSync(path.join(DATA, 'img', name), b)
      json({ ok: true, url: '/img/' + name }) }) }
  if (url === '/admin/premium-jar' || url === '/admin/mod-jar') {
    if (!admin()) return json({ error: 'forbidden' }, 403)
    const mc = q.get('mc') || ''; if (!MC.test(mc) && url === '/admin/premium-jar') return json({ error: 'bad mc version' }, 400)
    const name = url === '/admin/premium-jar' ? `premium/asteria-visuals-${mc}.jar` : q.get('file') === 'AsteriaBridge.jar' ? 'mods/AsteriaBridge.jar' : MC.test(mc) ? `mods/asteria-core-${mc}.jar` : null
    if (!name) return json({ error: 'bad file' }, 400)
    return body(30_000_000, (b) => { if (b.subarray(0, 2).toString() !== 'PK') return json({ error: 'not a jar' }, 400); fs.writeFileSync(path.join(DATA, name), b); json({ ok: true, file: name, size: b.length }) })
  }
  jbody((j) => {
    if (url === '/admin/plan') { // { name, plan: 'PREMIUM' | 'FREE', days?: number (0 / missing = forever) }
      if (!admin()) return json({ error: 'forbidden' }, 403)
      if (!NAME.test(j.name || '')) return json({ error: 'bad name' }, 400)
      const n = j.name.toLowerCase()
      if (j.plan === 'PREMIUM') { const cur = planOf(n), base = cur.plan === 'PREMIUM' && cur.until ? cur.until : Date.now(); plans[n] = { plan: 'PREMIUM', until: +j.days > 0 ? base + j.days * 86400000 : 0 } }
      else delete plans[n]
      wr('plans.json', plans); return json({ ok: true, ...planOf(n) })
    }
    if (url === '/admin/api/config') { if (!admin()) return json({ error: 'forbidden' }, 403); if (typeof j !== 'object' || !j) return json({ error: 'bad config' }, 400); wr('launcher-config.json', j); return json({ ok: true }) }
    if (url === '/server/stats') { // from the AsteriaBridge server plugin
      if (req.headers['x-key'] !== SKEY) return json({ error: 'forbidden' }, 403)
      for (const p of j.players || []) if (NAME.test(p.name)) stats[p.name.toLowerCase()] = { stats: p.stats, updated: Date.now() }
      wr('stats.json', stats); return json({ ok: true })
    }
    if (url === '/server/clans' || url === '/server/events') { // SafiClan (launcher-sync) and AsteriaBridge (events)
      if (req.headers['x-key'] !== SKEY) return json({ error: 'forbidden' }, 403)
      if (url === '/server/clans') { if (!Array.isArray(j.clans)) return json({ error: 'bad data' }, 400); clans = { time: Date.now(), icons: j.icons || {}, clans: j.clans.slice(0, 2000) }; wr('clans.json', clans) }
      else { if (typeof j.events !== 'object' || !j.events) return json({ error: 'bad data' }, 400); events = { time: Date.now(), events: j.events }; wr('events.json', events) }
      return json({ ok: true })
    }
    if (url === '/model') { // { name, token, slot, data } — a player's own 3D model, others download it
      const n2 = String(j.name || '').toLowerCase(); if (!NAME.test(n2) || !['pet', 'hat', 'back', 'wings'].includes(j.slot)) return json({ error: 'bad' }, 400)
      if (tokens[n2] && !authed(n2, j.token)) return json({ error: 'owned' }, 403); const d = String(j.data || ''); if (d.length > 1_900_000) return json({ error: 'too large' }, 413)
      try { const o = JSON.parse(d); if (!Array.isArray(o.polys) || !Array.isArray(o.tex)) throw 0 } catch { return json({ error: 'bad model' }, 400) }
      const dir = path.join(DATA, 'models'); fs.mkdirSync(dir, { recursive: true }); fs.writeFileSync(path.join(dir, n2 + '-' + j.slot + '.json'), d); bump(n2); return json({ ok: true })
    }
    if (url === '/share') { // { kind, data } -> { id }: a mod pack, skin, cape or crosshair for a friend
      if (!['modpack', 'skin', 'cape', 'crosshair'].includes(j.kind)) return json({ error: 'bad kind' }, 400)
      const c = JSON.stringify({ kind: j.kind, data: j.data }); if (c.length > 3_500_000) return json({ error: 'too large' }, 413)
      const dir = path.join(DATA, 'share'); fs.mkdirSync(dir, { recursive: true })
      const A = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789', h = crypto.createHash('sha1').update(c).digest()
      for (let k = 0; k < 8; k++) { let id = ''; for (let i = 0; i < 6; i++) id += A[h[(i + k * 6) % h.length] % A.length]; const f = path.join(dir, id + '.json')
        try { if (fs.readFileSync(f, 'utf8') === c) return json({ id }) } catch { fs.writeFileSync(f, c); return json({ id }) } }
      return json({ error: 'busy' }, 500)
    }
    if (url === '/backup') { // { data: '<json>' } -> { code }: moving the launcher to another computer, or a setup for a friend
      const d = String(j.data || ''); if (d.length < 10 || d.length > 3_900_000) return json({ error: 'bad size' }, 400); try { JSON.parse(d) } catch { return json({ error: 'bad data' }, 400) }
      const dir = path.join(DATA, 'backups'); fs.mkdirSync(dir, { recursive: true }); const A = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; let code
      do { code = Array.from(crypto.randomBytes(10), (b) => A[b % A.length]).join('') } while (fs.existsSync(path.join(dir, code + '.json')))
      fs.writeFileSync(path.join(dir, code + '.json'), d); return json({ code }) }
    if (url === '/vcfg') { // { code: 'AST1:...' } -> { id: 'K7Q2XD' } short share code for a visuals config
      const c = String(j.code || ''); if (!/^AST1:[\w-]{8,60000}$/.test(c)) return json({ error: 'bad code' }, 400)
      const dir = path.join(DATA, 'vcfg'); fs.mkdirSync(dir, { recursive: true })
      const A = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789', h = crypto.createHash('sha1').update(c).digest()
      for (let k = 0; k < 8; k++) { let id = ''; for (let i = 0; i < 6; i++) id += A[h[(i + k * 6) % h.length] % A.length]; const f = path.join(dir, id + '.json')
        try { if (JSON.parse(fs.readFileSync(f, 'utf8')).code === c) return json({ id }) } catch { fs.writeFileSync(f, JSON.stringify({ code: c, at: Date.now() })); return json({ id }) } }
      return json({ error: 'busy' }, 500)
    }
    // ------------------------------------------------------------------ launcher
    if (!NAME.test(j.name || '')) return json({ error: 'bad name' }, 400)
    const n = j.name.toLowerCase()
    if (url === '/claim') { // first launcher to use a nick gets its token; skins/cosmetics/tickets for that nick need the token
      if (!tokens[n]) { tokens[n] = crypto.randomBytes(16).toString('hex'); wr('tokens.json', tokens); return json({ token: tokens[n] }) }
      return authed(n, j.token) ? json({ ok: true }) : json({ error: 'owned' }, 403)
    }
    if (url === '/friends') { // { name, token, act: 'get' | 'request' | 'answer' | 'remove', who?, accept? }
      if (!authed(n, j.token)) return json({ error: 'owned' }, 403)
      let who = String(j.who || '')
      if (j.act === 'request') { const c = String(j.code || '').toUpperCase().replace(/[^A-Z0-9]/g, ''), key = c.slice(0, 4) + '-' + c.slice(4, 8), owner = fbyCode[key]
        if (!owner) return json({ error: 'bad code' }, 404); who = (seenName[owner] || owner) }
      const me = fr(n), w = who.toLowerCase()
      if (j.act && j.act !== 'get') { if (!NAME.test(who) || w === n) return json({ error: 'bad name' }, 400); const other = fr(w)
        if (j.act === 'request') { if (me.f.some((x) => x.toLowerCase() === w)) return json({ error: 'already' }, 400)
          if (me.in.some((x) => x.toLowerCase() === w)) { me.in = without(me.in, w); other.out = without(other.out, n); me.f.push(who); other.f.push(j.name); note(w, 'accepted', j.name) } // they asked first: friends at once
          else if (!me.out.some((x) => x.toLowerCase() === w)) { me.out.push(who); other.in.push(j.name); note(w, 'request', j.name) } }
        if (j.act === 'answer') { if (!me.in.some((x) => x.toLowerCase() === w)) return json({ error: 'no request' }, 400); me.in = without(me.in, w); other.out = without(other.out, n)
          if (j.accept) { me.f.push(who); other.f.push(j.name) } note(w, j.accept ? 'accepted' : 'declined', j.name) }
        const isFriend = me.f.some((x) => x.toLowerCase() === w)
        if (j.act === 'invite') { if (!isFriend) return json({ error: 'not friend' }, 400); const p = j.where || {}
          const where = { kind: p.kind === 'world' ? 'world' : 'server', host: String(p.host || '').toLowerCase(), port: Math.max(1, Math.min(65535, +p.port || 25565)), ver: MC.test(String(p.ver || '')) ? String(p.ver) : '', label: String(p.label || '').slice(0, 40) }
          if (!/^([a-z0-9-]+\.)+[a-z0-9-]{2,}$/.test(where.host) || where.host.length > 80) return json({ error: 'bad address' }, 400)
          note(w, 'invite', j.name, { where }); return json({ ok: true }) }
        if (j.act === 'msg') { if (!isFriend) return json({ error: 'not friend' }, 400); const text = String(j.text || '').replace(/[\u0000-\u001f]/g, ' ').trim().slice(0, 500); if (!text) return json({ error: 'empty' }, 400)
          const k = pair(n, w), m = { f: j.name, x: text, t: Date.now() }; chats[k] = [...(chats[k] || []), m].slice(-300); chatsDirty = true; note(w, 'msg', j.name, { text: text.slice(0, 140) }); return json({ ok: true, m }) }
        if (j.act === 'chat') { if (!isFriend) return json({ error: 'not friend' }, 400); const since = +j.after || 0; return json({ msgs: (chats[pair(n, w)] || []).filter((m) => m.t > since) }) }
        if (j.act === 'remove') { me.f = without(me.f, w); me.out = without(me.out, w); other.f = without(other.f, n); other.in = without(other.in, n) }
        me.f = me.f.slice(0, 200); friendsDirty = true }
      const since = +j.since || 0, now = Date.now(), pres = {}
      for (const x of me.f) { const k = x.toLowerCase(), t = online.get(k), on = !!t && now - t < 120000; pres[x] = { online: on, ...(on ? presence.get(k) || {} : {}) } }
      seenName[n] = j.name
      return json({ code: fcode(n), friends: me.f, incoming: me.in, outgoing: me.out, notes: me.notes.filter((x) => x.t > since), pres }) }
    if (url === '/heartbeat') {
      if (tokens[n] && !authed(n, j.token)) return json({ error: 'owned' }, 403)
      online.set(n, Date.now()); seenDb[n] = Date.now(); presence.set(n, { ver: /^[0-9.]{3,10}$/.test(String(j.ver || '')) ? String(j.ver) : '', host: /^([a-z0-9-]+\.)+e4mc\.link$/.test(String(j.host || '')) ? String(j.host) : '', srv: /^[a-z0-9.-]{0,60}$/i.test(String(j.srv || '')) ? String(j.srv) : '' }); setCosm(n, { cape: String(j.cape || ''), badge: String(j.badge || ''), pet: /^\d{1,2}:\d(\.\d+)?(:m)?$/.test(String(j.pet || '')) ? String(j.pet) : '', fx: /^[a-z0-9:.,]{0,60}$/.test(String(j.fx || '')) ? String(j.fx || '') : '' }) // pet: "<kind>:<size>" from the visuals, '' = none; return json({ ok: true, ...planOf(n) })
    }
    if (url === '/ticket') { // signed PREMIUM ticket, valid 3 days or until the subscription ends
      adopt(n, j.token); const p = planOf(n); if (p.plan !== 'PREMIUM') return json({ error: 'no premium', ...p }, 403)
      if (!authed(n, j.token)) return json({ error: 'owned' }, 403)
      if (!fs.existsSync(KEYFILE)) return json({ error: 'no signing key on the backend' }, 500)
      const until = Math.min(p.until || Infinity, Date.now() + 3 * 86400000)
      return json({ ticket: signTicket(j.name, until), until, plan: 'PREMIUM' })
    }
    if (url === '/skin') {
      if (tokens[n] && !authed(n, j.token)) return json({ error: 'name is owned by another launcher' }, 403)
      let token = j.token; if (!tokens[n]) { token = crypto.randomBytes(16).toString('hex'); tokens[n] = token; wr('tokens.json', tokens) }
      const kind = j.kind === 'cape' ? 'cape' : j.kind === 'mini' ? 'mini' : 'skin', f = path.join(DATA, 'skins', `${kind}-${n}.png`)
      if (j.png === null) { fs.rmSync(f, { force: true }); bump(n); return json({ ok: true, token }) }
      const png = Buffer.from(String(j.png || ''), 'base64')
      if (png.length > 2_500_000 || !png.subarray(0, 4).equals(PNG)) return json({ error: 'bad png' }, 400) // HD capes up to 2.5 MB
      fs.writeFileSync(f, png); bump(n)
      if (kind === 'skin') { const sf = path.join(DATA, 'skins', `slim-${n}`); j.slim ? fs.writeFileSync(sf, '1') : fs.rmSync(sf, { force: true }) }
      return json({ ok: true, token })
    }
    json({ error: 'not found' }, 404)
  })
})
const PORT = +process.env.PORT || 8080
server.on('error', (e) => {
  if (e.code === 'EADDRINUSE') { console.log(`\n!! Порт ${PORT} уже занят: сервер Asteria (возможно, старой версии из другой папки) уже запущен.\n   Закройте его окно и запустите start-backend.bat ещё раз — он сам закроет старый сервер.`); process.exit(1) }
  throw e })
server.listen(PORT, () => {
  console.log(`AsteriaLauncher backend: http://127.0.0.1:${PORT}`)
  console.log(`Admin panel:  http://127.0.0.1:${PORT}/admin`)
  console.log(`Admin key:    ${ADMIN}`)
  console.log(`Stats key (for the AsteriaBridge plugin config.yml "secret"): ${SKEY}`)
})
