// Windows build of the Asteria Visuals jars without Gradle (same steps as build.sh: javac against a Yarn-named Minecraft,
// then Remap.java back to intermediary). Everything it needs is taken from this PC or downloaded once into a work folder:
//   - Minecraft libraries: from the launcher's .asteria/libraries, else from Mojang (version json)
//   - Yarn-named / intermediary Minecraft: Loom's cache (~/.gradle/caches/fabric-loom), or built here from
//     the client-intermediary.jar Fabric made in .asteria plus a Yarn checkout from GitHub
//   - Fabric API (1.16.5 only): a fabric-api jar, its nested modules remapped to Yarn names
//
//   node mods/toolchain/build-win.cjs <work dir> [1.16.5 1.20.1 1.21.4]   ->  mods/dist/asteria-visuals-<v>.jar
const fs = require('fs'), path = require('path'), os = require('os'), { execFileSync } = require('child_process')
const MODS = path.resolve(__dirname, '..'), WORK = path.resolve(process.argv[2] || path.join(os.tmpdir(), 'asteria-build'))
const VERS = process.argv.slice(3).length ? process.argv.slice(3) : ['1.16.5', '1.20.1', '1.21.4']
const HOME = os.homedir(), ASTERIA = path.join(process.env.APPDATA || '', '.asteria'), LOOM = path.join(HOME, '.gradle/caches/fabric-loom')
const JDK = process.env.ASTERIA_JDK || 'C:/Program Files/Java/jdk-21.0.12.1', JAVAC = path.join(JDK, 'bin/javac.exe'), JAVA = path.join(JDK, 'bin/java.exe'), JAR = path.join(JDK, 'bin/jar.exe')
const REL = { '1.16.5': 8, '1.20.1': 17, '1.21.4': 21 }
const sh = (cmd, args, opt = {}) => execFileSync(cmd, args, { stdio: ['ignore', 'pipe', 'pipe'], maxBuffer: 1 << 28, ...opt }).toString()
const run = (cmd, args, opt) => { try { return sh(cmd, args, opt) } catch (e) { throw new Error(`${path.basename(cmd)} failed:\n${(e.stdout || '').toString()}${(e.stderr || '').toString()}`) } }
const mk = (d) => (fs.mkdirSync(d, { recursive: true }), d)
const walk = (d, f = () => true) => fs.existsSync(d) ? fs.readdirSync(d, { withFileTypes: true }).flatMap((e) => e.isDirectory() ? walk(path.join(d, e.name), f) : f(e.name) ? [path.join(d, e.name)] : []) : []
const curl = (url, out) => { if (fs.existsSync(out) && fs.statSync(out).size > 0) return out; mk(path.dirname(out)); run('curl', ['-sSL', '--retry', '8', '--retry-all-errors', '--retry-delay', '2', '--max-time', '600', '-o', out + '.part', url]); fs.renameSync(out + '.part', out); return out }

function asmJars() {
  const base = path.join(HOME, '.gradle/caches/modules-2/files-2.1/org.ow2.asm')
  return ['asm', 'asm-commons', 'asm-tree'].map((n) => walk(path.join(base, n), (f) => /^asm.*-9\.\d+(\.\d+)?\.jar$/.test(f)).sort().pop())
}
function toolchain() {
  const tc = mk(path.join(WORK, 'tc')), asm = asmJars()
  if (!fs.existsSync(path.join(tc, 'Remap.class'))) run(JAVAC, ['-nowarn', '-d', tc, '-cp', asm.join(';'), path.join(__dirname, 'Remap.java')])
  return [tc, ...asm].join(';')
}
/** the vanilla libraries Minecraft <v> compiles against */
function libraries(v) {
  const manifest = JSON.parse(fs.readFileSync(curl('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json', path.join(WORK, 'version_manifest.json')), 'utf8'))
  const vj = JSON.parse(fs.readFileSync(curl(manifest.versions.find((x) => x.id === v).url, path.join(WORK, v, 'version.json')), 'utf8'))
  const out = []
  for (const l of vj.libraries) { const a = l.downloads?.artifact; if (!a || l.name.includes('natives')) continue
    const local = path.join(ASTERIA, 'libraries', a.path)
    out.push(fs.existsSync(local) ? local : curl(a.url, path.join(WORK, 'libraries', a.path))) }
  // Fabric Loader + Mixin (annotations for our mixins)
  for (const d of ['net/fabricmc/fabric-loader', 'net/fabricmc/sponge-mixin']) out.push(walk(path.join(ASTERIA, 'libraries', d), (f) => f.endsWith('.jar')).sort().pop())
  return { libs: out.filter(Boolean), clientUrl: vj.downloads.client.url }
}
/** Yarn mappings + named and intermediary Minecraft jars */
function minecraft(v, cp) {
  const loomDir = fs.existsSync(path.join(LOOM, v)) && fs.readdirSync(path.join(LOOM, v)).find((n) => n.startsWith('net.fabricmc.yarn'))
  if (loomDir) {
    const maven = path.join(LOOM, 'minecraftMaven/net/minecraft')
    const named = walk(path.join(maven, 'minecraft-merged'), (f) => f.includes(v) && f.endsWith('.jar'))[0], inter = walk(path.join(maven, 'minecraft-merged-intermediary'), (f) => f.includes(v) && f.endsWith('.jar'))[0]
    if (named && inter) return { yarn: path.join(LOOM, v, loomDir, 'mappings.tiny'), named, inter }
  }
  // no Loom cache: Yarn from GitHub + the intermediary client Fabric already made for the game
  const yz = path.join(WORK, `yarn-${v}.zip`), yd = path.join(WORK, `yarn-${v}`, 'mappings')
  if (!fs.existsSync(yd)) { curl(`https://codeload.github.com/FabricMC/yarn/zip/refs/heads/${v}`, yz); run(JAR, ['xf', yz], { cwd: WORK }); if (!fs.existsSync(yd)) run('tar', ['-xf', yz, '-C', WORK]) }
  const inter = walk(path.join(ASTERIA), (f) => f === 'client-intermediary.jar').find((f) => f.includes(`minecraft-${v}-`))
  if (!inter) throw new Error(`Нет client-intermediary.jar для ${v}: запустите ${v} с Fabric один раз из лаунчера`)
  const named = path.join(WORK, v, 'mc-named.jar')
  if (!fs.existsSync(named)) run(JAVA, ['-Xmx3G', '-cp', cp, 'Remap', 'mc', yd, inter, named])
  return { yarn: yd, named, inter }
}
/** Fabric API modules (nested jars) remapped to Yarn names, for versions that use it */
function fabricApi(v, cp, mc) {
  if (!fs.existsSync(path.join(MODS, v, 'needs-fabric-api'))) return []
  const fat = process.env.FABRIC_API || walk(path.join(HOME, '.lunarclient'), (f) => /^fabric-api-.*\+1\.16\.jar$/.test(f))[0] || walk(path.join(ASTERIA, 'instances', v, 'mods'), (f) => /^fabric-api.*\.jar$/.test(f))[0]
  if (!fat) throw new Error('Нужен jar Fabric API для ' + v + ' (переменная FABRIC_API)')
  const ex = mk(path.join(WORK, v, 'fapi')), out = mk(path.join(WORK, v, 'fapi-named'))
  run(JAR, ['xf', fat, 'META-INF/jars'], { cwd: ex })
  return walk(path.join(ex, 'META-INF/jars'), (f) => f.endsWith('.jar')).map((j) => { const o = path.join(out, 'named-' + path.basename(j))
    if (!fs.existsSync(o)) run(JAVA, ['-cp', cp, 'Remap', 'lib', mc.yarn, mc.inter, j, o]); return o })
}
function build(v) {
  const cp = toolchain(), { libs } = libraries(v), mc = minecraft(v, cp), fapi = fabricApi(v, cp, mc)
  const W = path.join(WORK, v, 'visuals'); fs.rmSync(W, { recursive: true, force: true }); const named = mk(path.join(W, 'named')), inter = mk(path.join(W, 'inter'))
  const src = [...walk(path.join(MODS, v, 'visuals/src/main/java'), (f) => f.endsWith('.java')), ...walk(path.join(MODS, 'common-visuals/ru'), (f) => f.endsWith('.java'))]
  fs.writeFileSync(path.join(W, 'sources.txt'), src.map((f) => '"' + f.replace(/\\/g, '/') + '"').join('\n'))
  run(JAVAC, ['-nowarn', '-encoding', 'UTF-8', '--release', String(REL[v]), '-proc:none', '-d', named, '-cp', [mc.named, ...fapi, ...libs].join(';'), '@' + path.join(W, 'sources.txt')])
  run(JAVA, ['-Xmx3G', '-cp', cp, 'Remap', 'mod', mc.yarn, mc.inter, named, inter, mc.named, ...fapi])
  for (const r of [path.join(MODS, v, 'visuals/src/main/resources'), path.join(MODS, 'common-visuals/resources')]) if (fs.existsSync(r)) fs.cpSync(r, inter, { recursive: true })
  const mixDir = path.join(inter, 'ru/safimine/asteria/visuals/mc/mixin')
  const mix = fs.readdirSync(mixDir).filter((f) => f.endsWith('.class') && !f.includes('$')).map((f) => f.slice(0, -6)).sort()
  const ver = fs.readFileSync(path.join(MODS, 'VERSION'), 'utf8').trim(), mcdep = fs.existsSync(path.join(MODS, v, 'mcdep')) ? fs.readFileSync(path.join(MODS, v, 'mcdep'), 'utf8').trim() : v
  fs.writeFileSync(path.join(inter, 'asteria_visuals.mixins.json'), JSON.stringify({ required: true, minVersion: '0.8', package: 'ru.safimine.asteria.visuals.mc.mixin', compatibilityLevel: 'JAVA_' + REL[v], client: mix, injectors: { defaultRequire: 1 } }))
  fs.writeFileSync(path.join(inter, 'fabric.mod.json'), JSON.stringify({ schemaVersion: 1, id: 'asteria_visuals', version: ver, name: 'Asteria Visuals',
    description: 'Visuals of AsteriaLauncher: HUD, Click GUI (Right Shift), trails, particles, target marker, zoom and more.', authors: ['SafiMine'], contact: { homepage: 'https://safimine.ru' },
    icon: 'assets/asteria_visuals/icon.png', environment: 'client', entrypoints: { client: ['ru.safimine.asteria.visuals.mc.VisualsClient'] }, mixins: ['asteria_visuals.mixins.json'],
    depends: { fabricloader: '>=0.11', minecraft: mcdep, ...(fapi.length ? { fabric: '*' } : {}) } }))
  const out = path.join(MODS, 'dist', `asteria-visuals-${v}.jar`); fs.rmSync(out, { force: true })
  run(JAR, ['--create', '--file', out, '-C', inter, '.'])
  console.log(`built ${out} (${Math.round(fs.statSync(out).size / 1024)} KB, mixins: ${mix.join(', ')})`)
}
let bad = 0
for (const v of VERS) { try { build(v) } catch (e) { bad++; console.error(`\n===== ${v} FAILED =====\n` + e.message.slice(0, 20000)) } }
process.exit(bad ? 1 : 0)
