/**
 * Whole-launcher translation: every Russian text on screen (labels, buttons, hints, news) is replaced by its translation
 * into the launcher language. Translations come from the main process (machine translation, cached on disk) and are
 * applied to the DOM, so new screens and future texts are covered without a dictionary. Russian = the original texts.
 */
const CYR = /[А-Яа-яЁё]/
const orig = new WeakMap<Node, string>(), shown = new WeakMap<Node, string>(), origAttr = new WeakMap<Element, Record<string, string>>()
let lang = 'ru', cache: Record<string, string> = {}, queue = new Set<string>(), timer = 0, observer: MutationObserver | null = null
const ATTRS = ['placeholder', 'title', 'aria-label']
const skip = (el: Element | null) => !!el?.closest('input,textarea,[contenteditable],[data-notr],script,style,code,pre')

function want(s: string) { const k = s.trim(); if (!k || !CYR.test(k) || k.length > 900) return null; return k }
function apply(node: Text) {
  const cur = node.nodeValue || ''
  if (shown.get(node) === cur) return // our own text
  orig.set(node, cur)
  if (lang === 'ru') return
  const k = want(cur); if (!k) return
  const tr = cache[k]; if (tr === undefined) { queue.add(k); schedule(); return }
  const out = cur.replace(k, tr); shown.set(node, out); node.nodeValue = out
}
function applyAttrs(el: Element) {
  for (const a of ATTRS) { const v = el.getAttribute(a); if (!v) continue
    const o = origAttr.get(el) || {}; if (o['_' + a] === v) continue; o[a] = v
    const k = want(v); if (!k || lang === 'ru') { origAttr.set(el, o); continue }
    const tr = cache[k]; if (tr === undefined) { queue.add(k); schedule(); origAttr.set(el, o); continue }
    const out = v.replace(k, tr); o['_' + a] = out; origAttr.set(el, o); el.setAttribute(a, out) }
}
function walk(root: Node) {
  if (root.nodeType === 3) { if (!skip(root.parentElement)) apply(root as Text); return }
  if (root.nodeType !== 1 || skip(root as Element)) return
  applyAttrs(root as Element)
  const w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT)
  for (let n = w.nextNode(); n; n = w.nextNode()) { if (n.nodeType === 3) { if (!skip(n.parentElement)) apply(n as Text) } else if (!skip(n as Element)) applyAttrs(n as Element) }
}
function schedule() { if (timer) return; timer = window.setTimeout(flushQueue, 250) }
async function flushQueue() {
  timer = 0; const list = [...queue].slice(0, 120); list.forEach((s) => queue.delete(s)); if (!list.length) return
  const res: string[] | null = await (window as any).api?.trBatch?.(list, lang).catch(() => null) || null
  if (!res) { window.setTimeout(() => { list.forEach((s) => queue.add(s)); schedule() }, 10000); return } // no answer: try again later, never remember Russian as the translation
  const again: string[] = []; list.forEach((s, i) => { if (res[i] && res[i] !== s) cache[s] = res[i]; else again.push(s) })
  if (again.length) window.setTimeout(() => { again.forEach((s) => queue.add(s)); schedule() }, 8000) // a service did not answer: ask again, never keep Russian
  try { localStorage.setItem('tr:' + lang, JSON.stringify(cache)) } catch {}
  walk(document.body); if (queue.size) schedule()
}
/** call when the launcher language changes */
export function setUiLang(l: string) {
  if (l === lang && observer) return
  const prev = lang; lang = l
  try { cache = JSON.parse(localStorage.getItem('tr:' + l) || '{}') } catch { cache = {} }
  if (prev !== 'ru' && l !== prev) restore()
  if (!observer) { observer = new MutationObserver((ms) => { for (const m of ms) { if (m.type === 'characterData') { if (!skip(m.target.parentElement)) apply(m.target as Text) } else if (m.type === 'attributes') { if (!skip(m.target as Element)) applyAttrs(m.target as Element) } else m.addedNodes.forEach(walk) } })
    observer.observe(document.body, { childList: true, subtree: true, characterData: true, attributes: true, attributeFilter: ATTRS }) }
  if (l === 'ru') restore(); else walk(document.body)
}
/** back to the Russian originals */
function restore() {
  const w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT)
  for (let n = w.nextNode(); n; n = w.nextNode()) {
    if (n.nodeType === 3) { const o = orig.get(n); if (o !== undefined && n.nodeValue !== o) { shown.set(n, o); n.nodeValue = o } }
    else { const o = origAttr.get(n as Element); if (o) for (const a of ATTRS) if (o[a]) (n as Element).setAttribute(a, o[a]) }
  }
}
