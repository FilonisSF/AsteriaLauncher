// Minecraft settings shared by every version: one set kept by the launcher in a version-neutral form and written into each
// version's options.txt in that version's own format. A version can be switched to "its own settings" and is then left alone.
//   graphics: fancyGraphics:true|false (before 1.16)  <->  graphicsMode:0|1|2
//   smooth lighting: ao:0|1|2 (before 1.19)  <->  ao:true|false
//   keys: LWJGL2 numbers (1.8.9 / 1.12.2)  <->  key.keyboard.<name> / key.mouse.<button>
//   strings: 1.19+ writes some values in quotes ("right"), older versions without
const fs = require('fs'), path = require('path')

/** 1.16.5 -> 1605, 1.8.9 -> 809, 26.1 -> 2601 (same as the launcher UI) */
const vnum = (v) => { const [a, b, c] = String(v).split('.').map((x) => parseInt(x) || 0); return a >= 26 ? 2600 + b : b * 100 + c }
const legacy = (v) => vnum(v) < 1300

/** the settings the launcher shows (and shares): anything else in options.txt stays per version (resource packs, last server, ...) */
const SHARED = new Set(['renderDistance', 'maxFps', 'graphicsMode', 'fancyGraphics', 'ao', 'gamma', 'renderClouds', 'particles', 'guiScale', 'biomeBlendRadius',
  'entityDistanceScaling', 'mipmapLevels', 'simulationDistance', 'prioritizeChunkUpdates', 'attackIndicator', 'menuBackgroundBlurriness', 'enableVsync', 'entityShadows',
  'bobView', 'fov', 'mouseSensitivity', 'invertYMouse', 'rawMouseInput', 'autoJump', 'toggleSprint', 'toggleCrouch', 'advancedItemTooltips', 'mouseWheelSensitivity',
  'mainHand', 'chatOpacity', 'textBackgroundOpacity', 'chatScale', 'chatWidth', 'chatHeightFocused', 'chatHeightUnfocused', 'chatLineSpacing', 'chatDelay', 'chatVisibility',
  'chatColors', 'chatLinks', 'autoSuggestions', 'hideMatchedNames', 'onlyShowSecureChat', 'fovEffectScale', 'screenEffectScale', 'darknessEffectScale',
  'damageTiltStrength', 'glintStrength', 'glintSpeed', 'showSubtitles', 'hideLightningFlashes', 'backgroundForChatOnly'])
const shared = (k) => SHARED.has(k) || /^soundCategory_\w+$/.test(k) || /^modelPart_\w+$/.test(k) || /^key_key\.[\w.]+$/.test(k)

// LWJGL2 key codes <-> modern key names
const LW2 = { escape: 1, 1: 2, 2: 3, 3: 4, 4: 5, 5: 6, 6: 7, 7: 8, 8: 9, 9: 10, 0: 11, minus: 12, equal: 13, backspace: 14, tab: 15, q: 16, w: 17, e: 18, r: 19, t: 20, y: 21, u: 22,
  i: 23, o: 24, p: 25, 'left.bracket': 26, 'right.bracket': 27, enter: 28, 'left.control': 29, a: 30, s: 31, d: 32, f: 33, g: 34, h: 35, j: 36, k: 37, l: 38, semicolon: 39,
  apostrophe: 40, 'grave.accent': 41, 'left.shift': 42, backslash: 43, z: 44, x: 45, c: 46, v: 47, b: 48, n: 49, m: 50, comma: 51, period: 52, slash: 53, 'right.shift': 54,
  'keypad.multiply': 55, 'left.alt': 56, space: 57, 'caps.lock': 58, f1: 59, f2: 60, f3: 61, f4: 62, f5: 63, f6: 64, f7: 65, f8: 66, f9: 67, f10: 68, f11: 87, f12: 88,
  'right.control': 157, 'right.alt': 184, up: 200, left: 203, right: 205, down: 208, home: 199, end: 207, 'page.up': 201, 'page.down': 209, insert: 210, delete: 211 }
const LW2_NAME = Object.fromEntries(Object.entries(LW2).map(([k, v]) => [v, k]))
const MOUSE = { left: -100, right: -99, middle: -98, 4: -97, 5: -96 }, MOUSE_NAME = Object.fromEntries(Object.entries(MOUSE).map(([k, v]) => [v, k]))

const unq = (v) => String(v).replace(/^"(.*)"$/, '$1')

/** one setting as written by version ver -> neutral form ([key, value] or null when it cannot be shared) */
function toShared(k, v, ver) { if (!shared(k)) return null; v = unq(v); const n = vnum(ver)
  if (k === 'fancyGraphics') return ['graphicsMode', v === 'true' ? '1' : '0']
  if (k === 'ao' && n < 1900) return ['ao', String(+v > 0)]
  if (k.startsWith('key_key.') && legacy(ver)) { const c = +v; if (c === 0) return [k, 'key.keyboard.unknown']; if (MOUSE_NAME[c]) return [k, 'key.mouse.' + MOUSE_NAME[c]]; return LW2_NAME[c] ? [k, 'key.keyboard.' + LW2_NAME[c]] : null }
  return [k, v] }

/** neutral -> the format of version ver; was = that version's current value (keeps its quoting); null = this version has no such setting */
function fromShared(k, v, ver, was) { const n = vnum(ver)
  if (k === 'graphicsMode' && n < 1600) return ['fancyGraphics', String(+v > 0)]
  if (k === 'ao' && n < 1900) return ['ao', v === 'true' ? '2' : '0']
  if (k.startsWith('key_key.') && legacy(ver)) { if (v === 'key.keyboard.unknown') return [k, '0']; const m = /^key\.mouse\.(.+)$/.exec(v); if (m) return MOUSE[m[1]] ? [k, String(MOUSE[m[1]])] : null
    const c = LW2[v.replace(/^key\.keyboard\./, '')]; return c ? [k, String(c)] : null }
  if (k === 'renderDistance' && legacy(ver)) v = String(Math.min(+v, 32))
  return [k, was != null && String(was).startsWith('"') ? '"' + v + '"' : v] }

const read = (f) => { const o = {}; try { for (const l of fs.readFileSync(f, 'utf8').split(/\r?\n/)) { const i = l.indexOf(':'); if (i > 0) o[l.slice(0, i)] = l.slice(i + 1) } } catch {} return o }
/** a patch written by version ver -> neutral patch */
function normalize(patch, ver) { const o = {}; for (const [k, v] of Object.entries(patch)) { const r = toShared(k, v, ver); if (r) o[r[0]] = r[1] } return o }
/** the neutral set -> a patch for version ver, given that version's current options (only what differs) */
function forVersion(sharedSet, ver, cur = {}) { const o = {}
  for (const [k, v] of Object.entries(sharedSet)) { const r = fromShared(k, v, ver, cur[k] ?? cur[k === 'graphicsMode' ? 'fancyGraphics' : k]); if (r && cur[r[0]] !== r[1]) o[r[0]] = r[1] } return o }

module.exports = { vnum, shared, normalize, forVersion, read }
