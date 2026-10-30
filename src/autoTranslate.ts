/**
 * Whole-launcher translation: every Russian text on screen (labels, buttons, hints, news) is replaced by its translation
 * into the launcher language. The launcher ships full dictionaries (assets/tr/<lang>.json: Russian text -> translation), so the
 * interface is translated at once and offline; texts that are not in them (news, server texts) come from the main process
 * (machine translation, cached on disk). Russian = the original texts.
 */
const BUNDLED = import.meta.glob('./assets/tr/*.json', { eager: true, import: 'default' }) as Record<string, Record<string, string>>
const CYR = /[А-Яа-яЁё]/
const orig = new WeakMap<Node, string>(), shown = new WeakMap<Node, string>(), origAttr = new WeakMap<Element, Record<string, string>>()
let lang = 'ru', cache: Record<string, string> = {}, templates: { re: RegExp; out: string }[] = [], queue = new Set<string>(), timer = 0, observer: MutationObserver | null = null
const ATTRS = ['placeholder', 'title', 'aria-label']
const skip = (el: Element | null) => !!el?.closest('input,textarea,[contenteditable],[data-notr],script,style,code,pre')
const skipAttr = (el: Element | null) => !!el?.closest('[data-notr],script,style,code,pre') // placeholders of inputs are translated too
const esc = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

function want(s: string) { const k = s.replace(/\s+/g, ' ').trim(); if (!k || !CYR.test(k) || k.length > 900) return null; return k }
/** a known translation: the exact text, or a text with numbers / names in it ("5 мин назад" -> "{0} min ago") */
function find(k: string): string | undefined {
  const hit = cache[k]; if (hit !== undefined) return hit
  for (const t of templates) { const m = t.re.exec(k); if (m) return t.out.replace(/\{(\d)\}/g, (_, i) => m[+i + 1] ?? '') }
  // a number or version next to a known phrase: "Играть 1.16.5", "0.0 ч", "Для Minecraft 1.20.1"
  let m = /^(.*\S)\s+([^А-Яа-яЁё]+)$/.exec(k); if (m) { const a = cache[m[1]]; if (a !== undefined) return a + ' ' + m[2] } // also a name after it: "Зайти на GrumWorld"
  m = /^([\d.,:×%+\-]+[^\sА-Яа-яЁё]*)\s+(.+)$/.exec(k); if (m) { const b = cache[m[2]]; if (b !== undefined) return m[1] + ' ' + b }
  return undefined
}
/** a bundled dictionary has this Russian text for the language — then show the Russian and let it be translated */
export const knows = (l: string, ru: string) => !!want(ru) && BUNDLED[`./assets/tr/${l}.json`]?.[want(ru)!] !== undefined
/** the translation with the spaces around the original kept ("Для Minecraft " + version stays two words apart) */
function put(cur: string, _k: string, tr: string) { return cur.slice(0, Math.max(0, cur.search(/\S/))) + tr + cur.slice(cur.trimEnd().length) }
function apply(node: Text) {
  const cur = node.nodeValue || ''
  if (shown.get(node) === cur) return // our own text
  orig.set(node, cur)
  if (lang === 'ru') return
  const k = want(cur); if (!k) return
  const tr = find(k); if (tr === undefined) { queue.add(k); schedule(); return }
  if (tr === k) return
  const out = put(cur, k, tr); shown.set(node, out); node.nodeValue = out
}
function applyAttrs(el: Element) {
  for (const a of ATTRS) { const v = el.getAttribute(a); if (!v) continue
    const o = origAttr.get(el) || {}; if (o['_' + a] === v) continue; o[a] = v
    const k = want(v); if (!k || lang === 'ru') { origAttr.set(el, o); continue }
    const tr = find(k); if (tr === undefined) { queue.add(k); schedule(); origAttr.set(el, o); continue }
    const out = put(v, k, tr); o['_' + a] = out; origAttr.set(el, o); if (out !== v) el.setAttribute(a, out) }
}
function walk(root: Node) {
  if (root.nodeType === 3) { if (!skip(root.parentElement)) apply(root as Text); return }
  if (root.nodeType !== 1) return
  if (!skipAttr(root as Element)) applyAttrs(root as Element)
  if (skip(root as Element)) return
  const w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT)
  for (let n = w.nextNode(); n; n = w.nextNode()) { if (n.nodeType === 3) { if (!skip(n.parentElement)) apply(n as Text) } else if (!skipAttr(n as Element)) applyAttrs(n as Element) }
}
function schedule() { if (timer) return; timer = window.setTimeout(flushQueue, 250) }
async function flushQueue() {
  timer = 0; const forLang = lang, list = [...queue].slice(0, 120); list.forEach((s) => queue.delete(s)); if (!list.length || forLang === 'ru') return
  const res: string[] | null = await (window as any).api?.trBatch?.(list, forLang).catch(() => null) || null
  if (forLang !== lang) return // the language changed meanwhile
  if (!res) { window.setTimeout(() => { list.forEach((s) => queue.add(s)); schedule() }, 10000); return } // no answer: try again later, never remember Russian as the translation
  const again: string[] = [], learnt: Record<string, string> = {}; list.forEach((s, i) => { if (res[i] && res[i] !== s) cache[s] = learnt[s] = res[i]; else again.push(s) })
  if (again.length) window.setTimeout(() => { again.forEach((s) => queue.add(s)); schedule() }, 30000) // a service did not answer: ask again later, never keep Russian
  try { const old = JSON.parse(localStorage.getItem('tr:' + lang) || '{}'); localStorage.setItem('tr:' + lang, JSON.stringify({ ...old, ...learnt })) } catch {}
  walk(document.body); if (queue.size) schedule()
}
/** call when the launcher language changes */
export function setUiLang(l: string) {
  if (l === lang && observer) return
  lang = l; queue.clear()
  const bundled = BUNDLED['./assets/tr/' + l + '.json'] || {}
  let learnt: Record<string, string> = {}; try { learnt = JSON.parse(localStorage.getItem('tr:' + l) || '{}') } catch {}
  cache = { ...learnt, ...bundled } // the shipped dictionary wins over older machine translations
  templates = Object.entries(bundled).filter(([k]) => /\{\d\}/.test(k)).map(([k, out]) => ({ re: new RegExp('^' + esc(k).replace(/\\\{(\d)\\\}/g, '(.+?)') + '$'), out }))
  if (!observer) { observer = new MutationObserver((ms) => { for (const m of ms) { if (m.type === 'characterData') { if (!skip(m.target.parentElement)) apply(m.target as Text) } else if (m.type === 'attributes') { if (!skipAttr(m.target as Element)) applyAttrs(m.target as Element) } else m.addedNodes.forEach(walk) } })
    observer.observe(document.body, { childList: true, subtree: true, characterData: true, attributes: true, attributeFilter: ATTRS }) }
  if (observer) for (const m of observer.takeRecords()) { if (m.type === 'characterData') { const n = m.target as Text; if (shown.get(n) !== n.nodeValue) orig.set(n, n.nodeValue || '') } } // texts React has just changed are the new originals
  restore() // always from the Russian originals: switching between two other languages never leaves the old one on screen
  if (l !== 'ru') walk(document.body)
}
/** back to the Russian originals */
function restore() {
  const w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT)
  for (let n = w.nextNode(); n; n = w.nextNode()) {
    if (n.nodeType === 3) { const o = orig.get(n); if (o !== undefined && shown.get(n) === n.nodeValue && n.nodeValue !== o) n.nodeValue = o; shown.delete(n) }
    else { const o = origAttr.get(n as Element); if (o) { for (const a of ATTRS) { const cur = (n as Element).getAttribute(a); if (o[a] && o['_' + a] !== undefined && cur === o['_' + a]) (n as Element).setAttribute(a, o[a]); delete o['_' + a] } } }
  }
}
