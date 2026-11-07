// Core mod for Minecraft 26.x: 26.x is not obfuscated and Fabric runs it with Mojang's names, so the mod is compiled straight
// against the game jar (no Yarn, no remapping) and one jar serves every 26.x release.
//   node mods/toolchain/build-26.cjs [work dir] [game version to compile against, default 26.1]  ->  mods/dist/asteria-core-26.jar
const fs = require('fs'), path = require('path'), os = require('os'), { execFileSync } = require('child_process')
const MODS = path.resolve(__dirname, '..'), WORK = path.resolve(process.argv[2] || path.join(os.tmpdir(), 'asteria-build-26')), V = process.argv[3] || '26.1'
const ASTERIA = path.join(process.env.APPDATA || '', '.asteria')
const JDK = process.env.ASTERIA_JDK || 'C:/Program Files/Java/jdk-21.0.12.1', JAVAC = path.join(JDK, 'bin/javac.exe'), JAR = path.join(JDK, 'bin/jar.exe')
const walk = (d, f = () => true) => fs.existsSync(d) ? fs.readdirSync(d, { withFileTypes: true }).flatMap((e) => e.isDirectory() ? walk(path.join(d, e.name), f) : f(e.name) ? [path.join(d, e.name)] : []) : []
const run = (cmd, args, opt) => { try { return execFileSync(cmd, args, { stdio: ['ignore', 'pipe', 'pipe'], maxBuffer: 1 << 28, ...opt }).toString() } catch (e) { throw new Error(`${path.basename(cmd)} failed:\n${e.stdout || ''}${e.stderr || ''}`) } }

const vd = path.join(ASTERIA, 'versions', V), vj = JSON.parse(fs.readFileSync(path.join(vd, V + '.json'), 'utf8')), game = path.join(vd, V + '.jar')
if (!fs.existsSync(game)) throw new Error('Сначала запустите Minecraft ' + V + ' из лаунчера (нужен ' + game + ')')
const libs = vj.libraries.map((l) => l.downloads?.artifact?.path).filter((p) => p && !p.includes('natives')).map((p) => path.join(ASTERIA, 'libraries', p)).filter((f) => fs.existsSync(f))
const loader = walk(path.join(ASTERIA, 'libraries/net/fabricmc/fabric-loader'), (f) => f.endsWith('.jar')).sort().pop()
/** javac 21 refuses class files of Java 25 (major 69): a compile-only copy with the version lowered (javac reads only signatures) */
function lowered(jar) { const AdmZip = require('adm-zip'), dst = path.join(WORK, 'lowered', path.basename(jar))
  if (fs.existsSync(dst) && fs.statSync(dst).mtimeMs >= fs.statSync(jar).mtimeMs) return dst
  const z = new AdmZip(jar), o = new AdmZip(); let n = 0
  for (const e of z.getEntries()) { if (e.isDirectory || !e.entryName.endsWith('.class')) continue; const b = e.getData()
    if (b.readUInt32BE(0) === 0xCAFEBABE && b.readUInt16BE(6) > 65) { b.writeUInt16BE(65, 6); n++ } o.addFile(e.entryName, b) }
  if (!n) return jar; fs.mkdirSync(path.dirname(dst), { recursive: true }); o.writeZip(dst); return dst }
const W = path.join(WORK, 'core'); fs.rmSync(W, { recursive: true, force: true }); const out = path.join(W, 'out'); fs.mkdirSync(out, { recursive: true })
const src = [...walk(path.join(MODS, '26/core/src/main/java'), (f) => f.endsWith('.java')), ...walk(path.join(MODS, 'common-core/ru'), (f) => f.endsWith('.java'))]
fs.writeFileSync(path.join(W, 'sources.txt'), src.map((f) => '"' + f.replace(/\\/g, '/') + '"').join('\n'))
// --release 21: runs on the game's Java 25; javac only warns that the game's classes are newer than it knows
run(JAVAC, ['-nowarn', '-encoding', 'UTF-8', '--release', '21', '-proc:none', '-d', out, '-cp', [game, loader, ...libs].map(lowered).join(';'), '@' + path.join(W, 'sources.txt')])
fs.mkdirSync(path.join(out, 'assets/asteria'), { recursive: true }); fs.copyFileSync(path.join(MODS, '1.21.11/core/src/main/resources/assets/asteria/icon.png'), path.join(out, 'assets/asteria/icon.png'))
fs.writeFileSync(path.join(out, 'fabric.mod.json'), JSON.stringify({ schemaVersion: 1, id: 'asteria', version: fs.readFileSync(path.join(MODS, 'VERSION'), 'utf8').trim(), name: 'AsteriaLauncher',
  description: 'SafiMine / AsteriaLauncher for Minecraft 26.x: settings from the launcher applied live, joining a friend from the launcher.', authors: ['SafiMine'], contact: { homepage: 'https://safimine.ru' },
  icon: 'assets/asteria/icon.png', environment: 'client', entrypoints: { client: ['ru.safimine.asteria.AsteriaClient'] }, depends: { fabricloader: '>=0.18', minecraft: '>=26.1 <27' } }))
const jar = path.join(MODS, 'dist', 'asteria-core-26.jar'); fs.rmSync(jar, { force: true }); run(JAR, ['--create', '--file', jar, '-C', out, '.'])
console.log(`built ${jar} (${Math.round(fs.statSync(jar).size / 1024)} KB)`)
