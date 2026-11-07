// "Play together" without anyone else's servers: a friend's game reaches the host's opened world through our own backend.
// Everything goes over WebSocket on the backend's normal HTTPS address (port 443), so it works from any country and network.
//   host launcher  --ws /tunnel/host-->  backend  <--ws /tunnel/join?code--  joiner launcher <--tcp-- joiner's game
//         \--ws /tunnel/data?sid--> (one per joining player, piped to the joiner's socket) --tcp--> host's LAN port
// Used by the launcher (electron/main.cjs) and by the backend (backend/server.cjs): only Node's own modules, a minimal WebSocket
// (binary frames, ping / pong / close) is enough because both ends are ours.
const crypto = require('crypto'), net = require('net'), http = require('http'), https = require('https'), { EventEmitter } = require('events')
const GUID = '258EAFA5-E914-47DA-95CA-C5AB0DC85B11'

/** a WebSocket over a socket: emits 'message' (Buffer), 'close'; send(Buffer | string) */
class Ws extends EventEmitter {
  constructor(sock, client) { super(); this.sock = sock; this.client = client; this.buf = Buffer.alloc(0); this.open = true
    sock.setNoDelay(true); sock.on('data', (d) => { this.buf = this.buf.length ? Buffer.concat([this.buf, d]) : d; this.parse() })
    const end = () => { if (!this.open) return; this.open = false; clearInterval(this.ka); this.emit('close') }
    sock.on('close', end); sock.on('error', end); sock.on('end', end)
    this.ka = setInterval(() => this.frame(9, Buffer.alloc(0)), 20000) } // keeps proxies (Render) from closing a quiet tunnel
  parse() { for (;;) { const b = this.buf; if (b.length < 2) return; const op = b[0] & 15, masked = b[1] & 128; let len = b[1] & 127, o = 2
      if (len === 126) { if (b.length < 4) return; len = b.readUInt16BE(2); o = 4 } else if (len === 127) { if (b.length < 10) return; len = Number(b.readBigUInt64BE(2)); o = 10 }
      if (len > 64 << 20) return this.close()
      const mo = o; if (masked) o += 4; if (b.length < o + len) return
      let p = b.subarray(o, o + len); if (masked) { const k = b.subarray(mo, mo + 4); p = Buffer.from(p); for (let i = 0; i < p.length; i++) p[i] ^= k[i & 3] }
      this.buf = b.subarray(o + len)
      if (op === 8) return this.close(); if (op === 9) { this.frame(10, p); continue } if (op === 10) continue
      this.emit('message', p, op === 1) } }
  frame(op, p) { if (!this.open) return; const n = p.length, m = this.client ? 4 : 0, h = Buffer.alloc((n < 126 ? 2 : n < 65536 ? 4 : 10) + m)
    h[0] = 128 | op; let o = 2; if (n < 126) h[1] = n; else if (n < 65536) { h[1] = 126; h.writeUInt16BE(n, 2); o = 4 } else { h[1] = 127; h.writeBigUInt64BE(BigInt(n), 2); o = 10 }
    let body = p; if (m) { h[1] |= 128; const k = crypto.randomBytes(4); k.copy(h, o); body = Buffer.from(p); for (let i = 0; i < body.length; i++) body[i] ^= k[i & 3] }
    try { this.sock.write(Buffer.concat([h, body])) } catch { this.close() } }
  send(d) { typeof d === 'string' ? this.frame(1, Buffer.from(d)) : this.frame(2, d) }
  close() { if (!this.open) return; this.frame(8, Buffer.alloc(0)); this.open = false; clearInterval(this.ka); try { this.sock.end(); setTimeout(() => this.sock.destroy(), 1000) } catch {} this.emit('close') }
}
/** server side: answers the WebSocket handshake of an 'upgrade' request */
function accept(req, sock, head) {
  const key = req.headers['sec-websocket-key']; if (!key || String(req.headers.upgrade).toLowerCase() !== 'websocket') { sock.destroy(); return null }
  sock.write('HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: ' + crypto.createHash('sha1').update(key + GUID).digest('base64') + '\r\n\r\n')
  const ws = new Ws(sock, false); if (head && head.length) sock.emit('data', head); return ws
}
/** client side: opens ws(s)://... (an https:// or http:// base works too) */
function connect(url) { return new Promise((ok, no) => {
  const u = new URL(url.replace(/^ws/, 'http')), key = crypto.randomBytes(16).toString('base64'), mod = u.protocol === 'https:' ? https : http
  const req = mod.request({ hostname: u.hostname, port: u.port || (u.protocol === 'https:' ? 443 : 80), path: u.pathname + u.search, headers: { Connection: 'Upgrade', Upgrade: 'websocket', 'Sec-WebSocket-Key': key, 'Sec-WebSocket-Version': '13', 'User-Agent': 'AsteriaLauncher' }, timeout: 20000 })
  req.on('upgrade', (res, sock, head) => { if (res.headers['sec-websocket-accept'] !== crypto.createHash('sha1').update(key + GUID).digest('base64')) { sock.destroy(); return no(new Error('bad handshake')) }
    const ws = new Ws(sock, true); if (head && head.length) sock.emit('data', head); ok(ws) })
  req.on('response', (res) => { no(new Error('relay answered ' + res.statusCode)); res.resume() })
  req.on('timeout', () => { req.destroy(new Error('relay timeout')) }); req.on('error', no); req.end() }) }
/** a TCP socket and a WebSocket become one pipe (backpressure: the socket pauses while it has a lot unsent) */
function pipe(ws, tcp) {
  ws.on('message', (d) => { if (!tcp.write(d)) ws.sock.pause() }); tcp.on('drain', () => ws.sock.resume())
  tcp.on('data', (d) => ws.send(d))
  const end = () => { try { tcp.destroy() } catch {} ws.close() }; ws.on('close', end); tcp.on('close', end); tcp.on('error', end)
}

// ------------------------------------------------------------------ the relay (backend)
/** handles /tunnel/host, /tunnel/join, /tunnel/data upgrades; authHost(name, token) says whether a host may open a world */
function relay(authHost) {
  const hosts = new Map(), waiting = new Map() // code -> { ws, name }, sid -> { ws, t }
  const newCode = () => { const A = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; let c; do c = Array.from(crypto.randomBytes(6), (b) => A[b % A.length]).join(''); while (hosts.has(c)); return c }
  setInterval(() => { const now = Date.now(); for (const [sid, w] of waiting) if (now - w.t > 20000) { waiting.delete(sid); w.ws.close() } }, 5000)
  return function upgrade(req, sock, head, url, q) {
    if (url === '/tunnel/host') { const name = String(q.get('name') || ''); if (!authHost(name, q.get('token'))) { sock.end('HTTP/1.1 403 Forbidden\r\n\r\n'); return true }
      const ws = accept(req, sock, head); if (!ws) return true; const code = newCode(); hosts.set(code, { ws, name })
      ws.send(JSON.stringify({ code })); ws.on('close', () => { if (hosts.get(code)?.ws === ws) hosts.delete(code) }); return true }
    if (url === '/tunnel/join') { const h = hosts.get(String(q.get('code') || '').toUpperCase()); if (!h) { sock.end('HTTP/1.1 404 Not Found\r\n\r\n'); return true }
      const ws = accept(req, sock, head); if (!ws) return true; const sid = crypto.randomBytes(12).toString('hex')
      waiting.set(sid, { ws, t: Date.now() }); ws.sock.pause(); h.ws.send(JSON.stringify({ open: sid })); ws.on('close', () => waiting.delete(sid)); return true }
    if (url === '/tunnel/data') { const w = waiting.get(String(q.get('sid') || '')); if (!w) { sock.end('HTTP/1.1 404 Not Found\r\n\r\n'); return true }
      const ws = accept(req, sock, head); if (!ws) return true; waiting.delete(String(q.get('sid'))); w.ws.sock.resume()
      w.ws.on('message', (d) => ws.send(d)); ws.on('message', (d) => w.ws.send(d)); w.ws.on('close', () => ws.close()); ws.on('close', () => w.ws.close()); return true }
    return false }
}

// ------------------------------------------------------------------ the two launchers
/** the host: keeps a control line to the relay, and for every friend who joins opens a data line piped to the local LAN port.
 *  onCode(code) once the relay names this world; returns { close() } */
function host(base, name, token, lanPort, onCode, onError) {
  let ctrl = null, closed = false
  const start = async () => { try { ctrl = await connect(`${base}/tunnel/host?name=${encodeURIComponent(name)}&token=${encodeURIComponent(token || '')}`) } catch (e) { if (!closed) onError?.(e); return }
    ctrl.on('message', async (d) => { let m; try { m = JSON.parse(String(d)) } catch { return }
      if (m.code) onCode(m.code)
      if (m.open) { const tcp = net.connect(lanPort, '127.0.0.1'); tcp.on('error', () => {}); try { const ws = await connect(`${base}/tunnel/data?sid=${m.open}`); pipe(ws, tcp) } catch { tcp.destroy() } } })
    ctrl.on('close', () => { if (!closed) setTimeout(start, 3000) }) } // the relay restarted (or slept): open the world again, a new code follows
  start(); return { close() { closed = true; ctrl?.close() } }
}
/** the joiner: a local port the game connects to; every connection goes through the relay to the friend's world. Resolves the port */
function join(base, code) { return new Promise((ok, no) => {
  const srv = net.createServer(async (tcp) => { tcp.pause(); try { const ws = await connect(`${base}/tunnel/join?code=${encodeURIComponent(code)}`); pipe(ws, tcp); tcp.resume() } catch { tcp.destroy() } })
  srv.on('error', no); srv.listen(0, '127.0.0.1', () => { ok({ port: srv.address().port, close: () => srv.close() }) }) }) }

module.exports = { Ws, accept, connect, pipe, relay, host, join }
