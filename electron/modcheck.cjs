// Before a launch: every Fabric mod's own requirements (fabric.mod.json "depends") are checked against the game version, the loader
// and the other mods. Modrinth tags are not enough (Iris 1.11.4 is tagged for 26.1 but needs Sodium 0.9, which exists only for
// 26.1.2): a mismatch is fixed with a fitting version of the dependency or of the mod itself, and a mod that cannot fit is switched
// off with a note, so "Incompatible mods found!" never reaches the player.
const fs = require('fs'), path = require('path')

// ------------------------------------------------------------------ Fabric's version predicates (the subset mods use)
/** "1.11.4+mc26.1.2" -> { n: [1, 11, 4], pre: '' }; "0.9.3-beta.3" -> pre 'beta.3'; null if no number */
function parse(v) { const m = /^\s*v?(\d+(?:\.\d+)*)(?:-([0-9A-Za-z.-]+))?(?:\+.*)?\s*$/.exec(String(v || '')); if (!m) return null; return { n: m[1].split('.').map(Number), pre: m[2] || '' } }
function cmp(a, b) { for (let i = 0; i < Math.max(a.n.length, b.n.length); i++) { const x = a.n[i] || 0, y = b.n[i] || 0; if (x !== y) return x < y ? -1 : 1 }
  if (a.pre && !b.pre) return -1; if (!a.pre && b.pre) return 1; return a.pre < b.pre ? -1 : a.pre > b.pre ? 1 : 0 }
/** one predicate term: "*", "0.9.x", "~26.1", "^1.2", ">=0.18.4", "<27", "26.1.2" (a trailing "-" only lets pre-releases in) */
function term(t, v) { t = t.trim().replace(/-$/, ''); if (!t || t === '*') return true
  const m = /^(>=|<=|>|<|=|~|\^)?(.*)$/.exec(t), op = m[1] || '', raw = m[2].trim()
  if (/(^|\.)[xX*](\.|$)/.test(raw)) { const parts = raw.split('.'), fixed = []; for (const p of parts) { if (/^[xX*]$/.test(p)) break; fixed.push(+p) }
    if (!fixed.length) return true; const lo = { n: fixed, pre: '' }, hi = { n: [...fixed.slice(0, -1), fixed[fixed.length - 1] + 1], pre: '' }; return cmp({ ...v, pre: '' }, lo) >= 0 && cmp({ ...v, pre: '' }, hi) < 0 }
  const r = parse(raw); if (!r) return true; const c = cmp(v, r)
  if (op === '>=') return c >= 0; if (op === '<=') return c <= 0; if (op === '>') return c > 0; if (op === '<') return c < 0
  if (op === '~') { const hi = { n: r.n.length > 1 ? [r.n[0], r.n[1] + 1] : [r.n[0] + 1], pre: '' }; return c >= 0 && cmp(v, hi) < 0 }
  if (op === '^') { const hi = { n: [r.n[0] + 1], pre: '' }; return c >= 0 && cmp(v, hi) < 0 }
  return cmp({ ...v, pre: '' }, { ...r, pre: '' }) === 0 }
/** does version v satisfy the predicate (a string with space-separated AND terms, or an array = any of them) */
function satisfies(version, pred) { const v = parse(version); if (!v) return true
  const any = Array.isArray(pred) ? pred : [pred]; return any.some((p) => String(p).trim().split(/\s+/).every((t) => term(t, v))) }

// ------------------------------------------------------------------ the mods of a game folder
/** fabric.mod.json of a jar (and its nested jars' ids: Fabric API ships its modules inside) */
function readMod(file) { try { const AdmZip = require('adm-zip'), z = new AdmZip(file), e = z.getEntry('fabric.mod.json'); if (!e) return null
  const j = JSON.parse(e.getData().toString('utf8').replace(/^﻿/, '').replace(/[\u0000-\u0019]+/g, ' ')), nested = []
  for (const n of z.getEntries()) if (/^META-INF\/jars\/.+\.jar$/.test(n.entryName)) try { const zz = new AdmZip(n.getData()), ee = zz.getEntry('fabric.mod.json'); if (ee) { const jj = JSON.parse(ee.getData().toString('utf8').replace(/[\u0000-\u0019]+/g, ' ')); nested.push([jj.id, jj.version]); for (const p of jj.provides || []) nested.push([p, jj.version]) } } catch {}
  return { id: j.id, version: String(j.version || ''), depends: j.depends || {}, provides: j.provides || [], nested } } catch { return null } }
/** id -> version of everything present: the game, the loader, java, every mod with what it provides and nests */
function present(mods, game, loader) { const have = new Map([['minecraft', game], ['fabricloader', loader], ['java', '25']])
  if (satisfies(loader, '>=0.17')) have.set('mixinextras', '0.5.5'); else if (satisfies(loader, '>=0.15')) have.set('mixinextras', '0.3.5') // the loader ships MixinExtras itself
  for (const m of mods) { if (!m.info) continue; have.set(m.info.id, m.info.version); for (const p of m.info.provides) have.set(p, m.info.version); for (const [id, v] of m.info.nested) if (id && !have.has(id)) have.set(id, v) } return have }
/** what is wrong: [{ mod, dep, want, has }] (has = null: missing) */
function problems(mods, game, loader) { const have = present(mods, game, loader), out = []
  for (const m of mods) { if (!m.info) continue; for (const [dep, want] of Object.entries(m.info.depends)) { if (dep === 'java') continue; const h = have.get(dep)
      if (h === undefined) out.push({ mod: m, dep, want, has: null }); else if (!satisfies(h, want)) out.push({ mod: m, dep, want, has: h }) } }
  return out }

/** checks and fixes the mods of md for this game version; mr(url) = Modrinth JSON, download(url) = Buffer, note(text) for the player.
 *  Only the launcher's own files (mod-<slug>.jar) are replaced; any other jar that cannot fit is switched off. Returns the notes */
async function fixMods(md, game, loader, mr, download, note = () => {}) {
  const notes = [], say = (t) => { notes.push(t); note(t) }
  const load = () => fs.readdirSync(md).filter((f) => f.endsWith('.jar')).map((f) => ({ file: path.join(md, f), slug: (/^mod-(.+)\.jar$/.exec(f) || [])[1] || null, info: readMod(path.join(md, f)) }))
  const versionsOf = async (slug) => { try { return (await mr(`https://api.modrinth.com/v2/project/${slug}/version?game_versions=${encodeURIComponent(JSON.stringify([game]))}&loaders=${encodeURIComponent('["fabric"]')}`)) || [] } catch { return [] } }
  /** tries the versions of slug for this game (newest first) until one fits with the others; true if it was swapped in */
  const refit = async (slug, mods, test) => { for (const v of (await versionsOf(slug)).slice(0, 10)) { const f = v.files.find((x) => x.primary) || v.files[0]; if (!f) continue
      const tmp = path.join(md, `.check-${slug}.tmp`); try { fs.writeFileSync(tmp, await download(f.url)); const info = readMod(tmp); if (!info) continue
        const trial = mods.map((m) => m.slug === slug ? { ...m, info } : m); if (!test(trial, info)) continue
        const dst = path.join(md, `mod-${slug}.jar`); fs.copyFileSync(tmp, dst); return { info, version: v.version_number } } catch {} finally { fs.rmSync(tmp, { force: true }) } }
    return null }
  const fabricModule = (id) => /^fabric(-|$)/.test(id) && id !== 'fabricloader'
  for (let pass = 0; pass < 12; pass++) {
    const mods = load(), bad = problems(mods, game, loader); if (!bad.length) return notes
    const p = bad[0], name = p.mod.info.id
    // 1) a newer / older version of the dependency that fits (a mod we installed ourselves)
    const depMod = mods.find((m) => m.info && (m.info.id === p.dep || m.info.provides.includes(p.dep)))
    if (depMod?.slug && p.has !== null && p.dep !== 'minecraft') { const r = await refit(depMod.slug, mods, (trial) => !problems(trial, game, loader).some((x) => x.dep === p.dep && x.mod.info.id === name)); if (r) { say(`${depMod.info.id}: поставлена версия ${r.info.version} (нужна для ${name})`); continue } }
    // 2) a missing dependency: install it if Modrinth has it under that id
    if (p.has === null && !fabricModule(p.dep) && !['minecraft', 'fabricloader'].includes(p.dep)) { const r = await refit(p.dep, [...mods, { slug: p.dep, file: path.join(md, `mod-${p.dep}.jar`), info: null }], (trial) => !problems(trial, game, loader).some((x) => x.dep === p.dep)); if (r) { say(`${p.dep}: добавлен (нужен для ${name})`); continue } }
    // 3) another version of the mod itself that fits this game and these mods
    if (p.mod.slug) { const r = await refit(p.mod.slug, mods, (trial) => !problems(trial, game, loader).some((x) => x.mod.info.id === name)); if (r) { say(`${name}: поставлена версия ${r.info.version}, подходящая к ${p.dep === 'minecraft' ? 'Minecraft ' + game : p.dep}`); continue } }
    // 4) nothing fits: off, so the game still starts
    fs.renameSync(p.mod.file, p.mod.file + '.disabled'); say(`${name} выключен: нужен ${p.dep} ${Array.isArray(p.want) ? p.want.join(' или ') : p.want}, а ${p.has === null ? 'его нет' : 'есть ' + p.has}`)
  }
  return notes
}

module.exports = { parse, satisfies, readMod, problems, fixMods }
