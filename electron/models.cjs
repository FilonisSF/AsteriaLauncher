// Custom 3D models for pets and cosmetics: a Blockbench .bbmodel (cubes and meshes, embedded textures) or a resource pack
// .zip with a Java item model (elements from/to) is turned into a small ready-to-draw file the mod reads:
//   { v: 1, tex: [png base64...], polys: [{ t: texture index, p: [[x, y, z, u, v] x 3 or 4] }] }
// Positions are in blocks with the model standing on y = 0, centred on x / z, looking at +z; u / v are 0..1.
const AdmZip = require('adm-zip')

const rad = (d) => (d || 0) * Math.PI / 180
/** rotation matrix Rz*Ry*Rx (Blockbench order ZYX: X applied first) */
function rotM(r) { const [x, y, z] = (r || [0, 0, 0]).map(rad), cx = Math.cos(x), sx = Math.sin(x), cy = Math.cos(y), sy = Math.sin(y), cz = Math.cos(z), sz = Math.sin(z)
  return [[cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx], [sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx], [-sy, cy * sx, cy * cx]] }
const apply = (m, p) => [m[0][0] * p[0] + m[0][1] * p[1] + m[0][2] * p[2], m[1][0] * p[0] + m[1][1] * p[1] + m[1][2] * p[2], m[2][0] * p[0] + m[2][1] * p[1] + m[2][2] * p[2]]
const rotAround = (p, origin, rot) => { if (!rot || (!rot[0] && !rot[1] && !rot[2])) return p; const q = apply(rotM(rot), [p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]]); return [q[0] + origin[0], q[1] + origin[1], q[2] + origin[2]] }

/** the 4 corners of a cube face as seen from outside (top-left, top-right, bottom-right, bottom-left) */
function faceCorners(f, a, b) { const [x0, y0, z0] = a, [x1, y1, z1] = b
  return { north: [[x1, y1, z0], [x0, y1, z0], [x0, y0, z0], [x1, y0, z0]], south: [[x0, y1, z1], [x1, y1, z1], [x1, y0, z1], [x0, y0, z1]],
    east: [[x1, y1, z1], [x1, y1, z0], [x1, y0, z0], [x1, y0, z1]], west: [[x0, y1, z0], [x0, y1, z1], [x0, y0, z1], [x0, y0, z0]],
    up: [[x0, y1, z0], [x1, y1, z0], [x1, y1, z1], [x0, y1, z1]], down: [[x0, y0, z1], [x1, y0, z1], [x1, y0, z0], [x0, y0, z0]] }[f] }
function faceUv(uv, w, h, rot) { const [u0, v0, u1, v1] = uv; let c = [[u0 / w, v0 / h], [u1 / w, v0 / h], [u1 / w, v1 / h], [u0 / w, v1 / h]]
  for (let r = ((rot || 0) / 90) % 4; r > 0; r--) c = [c[3], c[0], c[1], c[2]]; return c }

/** Blockbench .bbmodel -> polys (rest pose, every parent group rotation applied) */
function fromBbmodel(j) {
  const tex = (j.textures || []).map((t) => ({ png: String(t.source || '').replace(/^data:image\/png;base64,/, ''), w: t.uv_width || j.resolution?.width || 16, h: t.uv_height || j.resolution?.height || 16 }))
  const parents = {}; // element uuid -> list of groups from the inside out
  const walk = (nodes, chain) => { for (const n of nodes || []) { if (typeof n === 'string') parents[n] = chain; else if (n && n.visibility !== false) walk(n.children, [n, ...chain]) } }
  walk(j.outliner, [])
  const polys = []
  for (const e of j.elements || []) {
    if (e.visibility === false || !(e.uuid in parents)) continue
    const place = (p) => { let q = rotAround(p, e.origin || [0, 0, 0], e.rotation); for (const g of parents[e.uuid]) q = rotAround(q, g.origin || [0, 0, 0], g.rotation); return q }
    if (e.type === 'mesh') {
      for (const f of Object.values(e.faces || {})) { if (f.texture === null || f.texture === undefined) continue; const T = tex[f.texture] || tex[0]; if (!T) continue
        let keys = f.vertices || []; if (keys.length < 3) continue
        let pts = keys.map((k) => ({ k, p: e.vertices[k] }))
        if (pts.length === 4) { // order the corners around the face centre (Blockbench keeps them in any order)
          const c = [0, 1, 2].map((i) => pts.reduce((s, q) => s + q.p[i], 0) / 4), a = pts[0].p.map((v, i) => v - c[i]), b0 = pts[1].p.map((v, i) => v - c[i])
          const nrm = [a[1] * b0[2] - a[2] * b0[1], a[2] * b0[0] - a[0] * b0[2], a[0] * b0[1] - a[1] * b0[0]], ax = a, ay = [nrm[1] * ax[2] - nrm[2] * ax[1], nrm[2] * ax[0] - nrm[0] * ax[2], nrm[0] * ax[1] - nrm[1] * ax[0]]
          pts.sort((p, q) => { const d = (r) => { const v = r.p.map((x, i) => x - c[i]); return Math.atan2(v[0] * ay[0] + v[1] * ay[1] + v[2] * ay[2], v[0] * ax[0] + v[1] * ax[1] + v[2] * ax[2]) }; return d(p) - d(q) }) }
        polys.push({ t: f.texture in tex ? f.texture : 0, p: pts.map(({ k, p }) => { const q = place(p), uv = (f.uv || {})[k] || [0, 0]; return [...q, uv[0] / T.w, uv[1] / T.h] }) }) }
      continue }
    const inf = e.inflate || 0, a = e.from.map((v) => v - inf), b = e.to.map((v) => v + inf)
    for (const [name, f] of Object.entries(e.faces || {})) { if (!f || f.texture === null || f.texture === undefined || !f.uv) continue; const T = tex[f.texture] || tex[0]; if (!T) continue
      const cs = faceCorners(name, a, b), uv = faceUv(f.uv, T.w, T.h, f.rotation)
      polys.push({ t: f.texture in tex ? f.texture : 0, p: cs.map((c, i) => [...place(c), ...uv[i]]) }) }
  }
  return { tex: tex.map((t) => t.png), polys }
}
/** resource pack zip with a Java item / block model (elements from / to in 0..16, uv in 0..16) */
function fromPackZip(buf) {
  const zip = new AdmZip(buf), entries = zip.getEntries(), byName = Object.fromEntries(entries.map((e) => [e.entryName, e]))
  const modelE = entries.find((e) => /assets\/[^/]+\/models\/.+\.json$/.test(e.entryName) && /"elements"/.test(e.getData().toString('utf8')))
  if (!modelE) throw new Error('В архиве нет модели с элементами')
  const j = JSON.parse(modelE.getData().toString('utf8')), ns = modelE.entryName.split('/')[1], keys = Object.keys(j.textures || {}), tex = [], idx = {}
  for (const k of keys) { if (k === 'particle') continue; const ref = String(j.textures[k]); const [n2, p2] = ref.includes(':') ? ref.split(':') : [ns, ref]
    const e = byName[`assets/${n2}/textures/${p2}.png`] || byName[`assets/minecraft/textures/${p2}.png`]; if (!e) continue; idx['#' + k] = tex.length; tex.push(e.getData().toString('base64')) }
  const polys = []
  for (const e of j.elements || []) { const rot = e.rotation, place = (p) => { if (!rot) return p; const r = [0, 0, 0]; r['xyz'.indexOf(rot.axis)] = rot.angle; return rotAround(p, rot.origin || [8, 8, 8], r) }
    for (const [name, f] of Object.entries(e.faces || {})) { const t = idx[f.texture]; if (t === undefined) continue
      const uv = faceUv(f.uv || [0, 0, 16, 16], 16, 16, f.rotation), cs = faceCorners(name, e.from, e.to); polys.push({ t, p: cs.map((c, i) => [...place(c), ...uv[i]]) }) } }
  return { tex, polys }
}
/** normalise: stand on y = 0, centred, scaled so the largest side is `size` blocks */
function normalise(m, size) {
  let mn = [1e9, 1e9, 1e9], mx = [-1e9, -1e9, -1e9]; for (const p of m.polys) for (const v of p.p) for (let i = 0; i < 3; i++) { mn[i] = Math.min(mn[i], v[i]); mx[i] = Math.max(mx[i], v[i]) }
  const k = size / Math.max(mx[0] - mn[0], mx[1] - mn[1], mx[2] - mn[2], 1e-6), cx = (mn[0] + mx[0]) / 2, cz = (mn[2] + mx[2]) / 2
  for (const p of m.polys) for (const v of p.p) { v[0] = +((v[0] - cx) * k).toFixed(4); v[1] = +((v[1] - mn[1]) * k).toFixed(4); v[2] = +((v[2] - cz) * k).toFixed(4); v[3] = +v[3].toFixed(4); v[4] = +v[4].toFixed(4) }
  return { v: 1, tex: m.tex, polys: m.polys }
}
/** file (bbmodel / zip / json) -> ready model; slot decides the size: pet 0.7 blocks, hat 0.6, back 1.6, wings 1.8 */
function convert(buf, name, slot) {
  const size = { pet: 0.7, hat: 0.62, back: 1.7, wings: 1.9 }[slot] || 0.8
  const m = /\.zip$/i.test(name) ? fromPackZip(buf) : fromBbmodel(JSON.parse(buf.toString('utf8')))
  if (!m.polys.length) throw new Error('В модели нет видимых частей'); if (!m.tex.length) throw new Error('В модели нет текстуры')
  return normalise(m, size)
}
module.exports = { convert }
