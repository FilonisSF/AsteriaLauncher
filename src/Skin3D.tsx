import { useEffect, useRef, useState } from 'react'

/** A cube of the Minecraft player model. u,v = texture origin (box UV layout), w,h,d = size in texture pixels,
 *  x,y,z = centre in model pixels, e = inflation of the outer layer, layer = which layer it belongs to. */
type Part = { w: number; h: number; d: number; u: number; v: number; x: number; y: number; z?: number; e?: number; layer: 'base' | 'over' | 'cape'; tw?: number; th?: number }
export type Hit = { u: number; v: number; layer: string; first: boolean }
type Face = [string, number, number, number, number, string, number] // name, tx, ty, tw, th, transform, shade

function faces(p: Part, S: number): Face[] {
  const e = p.e ?? 0, { w, h, d, u, v } = p
  return [ // shading like Minecraft's directional light: top brightest, bottom darkest, sides in between
    ['front', u + d, v + d, w, h, `translateZ(${(d + 2 * e) * S / 2}px)`, 1],
    ['back', u + 2 * d + w, v + d, w, h, `rotateY(180deg) translateZ(${(d + 2 * e) * S / 2}px)`, 0.86],
    ['right', u, v + d, d, h, `rotateY(-90deg) translateZ(${(w + 2 * e) * S / 2}px)`, 0.8],
    ['left', u + d + w, v + d, d, h, `rotateY(90deg) translateZ(${(w + 2 * e) * S / 2}px)`, 0.8],
    ['top', u + d, v, w, d, `rotateX(90deg) translateZ(${(h + 2 * e) * S / 2}px)`, 1.06],
    ['bottom', u + d + w, v, w, d, `rotateX(-90deg) translateZ(${(h + 2 * e) * S / 2}px)`, 0.66]]
}

function Cube({ p, tex, S, paint, onHit, dim, hidden }: { p: Part; tex: string; S: number; paint: boolean; onHit?: (h: Hit) => void; dim?: boolean; hidden?: boolean }) {
  const e = p.e ?? 0, TW = p.tw ?? 64, TH = p.th ?? 64
  if (hidden) return null
  return <div style={{ position: 'absolute', left: 0, top: 0, transformStyle: 'preserve-3d', transform: `translate3d(${p.x * S}px,${p.y * S}px,${(p.z ?? 0) * S}px)` }}>
    {faces(p, S).map(([n, tx, ty, tw, th, tf, shade]) => {
      const kx = (tw + 2 * e) / tw, ky = (th + 2 * e) / th, W = (tw + 2 * e) * S, H = (th + 2 * e) * S
      const hit = (ev: React.PointerEvent, first: boolean) => { // pointer position on this face -> texture pixel
        const el = ev.currentTarget as HTMLElement, ox = (ev.nativeEvent as any).offsetX, oy = (ev.nativeEvent as any).offsetY
        const fu = Math.min(tw - 1, Math.max(0, Math.floor((ox / el.clientWidth) * tw))), fv = Math.min(th - 1, Math.max(0, Math.floor((oy / el.clientHeight) * th)))
        onHit?.({ u: tx + fu, v: ty + fv, layer: p.layer, first })
      }
      return <div key={n} data-face={n}
        onPointerDown={paint ? (ev) => { if (ev.button !== 0) return; ev.stopPropagation(); hit(ev, true) } : undefined}
        onPointerMove={paint ? (ev) => { if (ev.buttons & 1) { ev.stopPropagation(); hit(ev, false) } } : undefined}
        style={{ position: 'absolute', width: W, height: H, left: -W / 2, top: -H / 2,
          backgroundImage: `url(${tex})`, backgroundSize: `${TW * S * kx}px ${TH * S * ky}px`, backgroundPosition: `${-tx * S * kx}px ${-ty * S * ky}px`,
          imageRendering: 'pixelated', transform: tf, backfaceVisibility: 'hidden', filter: `brightness(${shade})`, opacity: dim ? 0.35 : 1, pointerEvents: dim ? 'none' : undefined,
          cursor: paint ? 'crosshair' : undefined }} />
    })}
  </div>
}

export type View = { showOver?: boolean; showCape?: boolean; showBody?: boolean; dimOver?: boolean; animate?: boolean }

/** 3D Minecraft player (CSS 3D). Drag to rotate, wheel to zoom. With `paint`, a left click on the model reports the texture pixel
 *  (onHit); rotate with the right mouse button then. The cape is a real 10x16x1 box with the Minecraft cape UV layout. */
export default function Skin3D({ skin, slim, cape, paint = false, onSkinHit, onCapeHit, view = {}, height = 420, scale = 7, spin = true }: {
  skin: string; slim: boolean; cape?: string; paint?: boolean | 'skin' | 'cape'; onSkinHit?: (h: Hit) => void; onCapeHit?: (h: Hit) => void; view?: View; height?: number; scale?: number; spin?: boolean }) {
  const [tm, setTm] = useState(0), [rot, setRot] = useState({ x: -10, y: paint === 'cape' ? 200 : 28 }), [zoom, setZoom] = useState(1)
  const drag = useRef<{ x: number; y: number } | null>(null), auto = useRef(!paint), spinR = useRef(spin); spinR.current = spin
  const [capeSize, setCapeSize] = useState<[number, number]>([64, 32])
  useEffect(() => { let r = 0; const f = () => { if (auto.current && spinR.current) setRot((o) => ({ ...o, y: o.y + 0.35 })); setTm(performance.now() / 1000); r = requestAnimationFrame(f) }; r = requestAnimationFrame(f); return () => cancelAnimationFrame(r) }, [])
  useEffect(() => { if (!cape) return; const im = new Image(); im.onload = () => setCapeSize([im.width, im.height]); im.src = cape }, [cape])
  const S = scale * zoom, a = slim ? 3 : 4, showOver = view.showOver !== false, body = view.showBody !== false
  const base: Part[] = [{ w: 8, h: 8, d: 8, u: 0, v: 0, x: 0, y: -12, layer: 'base' }, { w: 8, h: 12, d: 4, u: 16, v: 16, x: 0, y: -2, layer: 'base' },
    { w: a, h: 12, d: 4, u: 40, v: 16, x: -(4 + a / 2), y: -2, layer: 'base' }, { w: a, h: 12, d: 4, u: 32, v: 48, x: 4 + a / 2, y: -2, layer: 'base' },
    { w: 4, h: 12, d: 4, u: 0, v: 16, x: -2, y: 10, layer: 'base' }, { w: 4, h: 12, d: 4, u: 16, v: 48, x: 2, y: 10, layer: 'base' }]
  const over: Part[] = [{ w: 8, h: 8, d: 8, u: 32, v: 0, x: 0, y: -12, e: 0.5, layer: 'over' }, { w: 8, h: 12, d: 4, u: 16, v: 32, x: 0, y: -2, e: 0.25, layer: 'over' },
    { w: a, h: 12, d: 4, u: 40, v: 32, x: -(4 + a / 2), y: -2, e: 0.25, layer: 'over' }, { w: a, h: 12, d: 4, u: 48, v: 48, x: 4 + a / 2, y: -2, e: 0.25, layer: 'over' },
    { w: 4, h: 12, d: 4, u: 0, v: 32, x: -2, y: 10, e: 0.25, layer: 'over' }, { w: 4, h: 12, d: 4, u: 0, v: 48, x: 2, y: 10, e: 0.25, layer: 'over' }]
  // cape: hangs from the shoulders behind the body and sways a little, like in game (6-10 degrees)
  const swing = view.animate === false ? 7 : 7 + 3.2 * Math.sin(tm * 1.6) + 1.2 * Math.sin(tm * 3.7)
  const capePart: Part = { w: 10, h: 16, d: 1, u: 0, v: 0, x: 0, y: 8, z: 0.5, layer: 'cape', tw: 64, th: 32 }
  const paintSkin = paint === true || paint === 'skin', paintCape = paint === true || paint === 'cape'
  return (
    <div className="relative flex justify-center items-center rounded-3xl card overflow-hidden" style={{ height, perspective: 1100, cursor: paint ? 'default' : 'grab', touchAction: 'none' }}
      onContextMenu={(e) => e.preventDefault()}
      onWheel={(e) => setZoom((z) => Math.max(0.6, Math.min(2.4, z * (e.deltaY < 0 ? 1.1 : 0.9))))}
      onPointerDown={(e) => { if (paint && e.button === 0) return; drag.current = { x: e.clientX, y: e.clientY }; auto.current = false; (e.currentTarget as Element).setPointerCapture?.(e.pointerId) }}
      onPointerMove={(e) => { if (!drag.current) return; const dx = e.clientX - drag.current.x, dy = e.clientY - drag.current.y; drag.current = { x: e.clientX, y: e.clientY }; setRot((o) => ({ x: Math.max(-70, Math.min(70, o.x - dy * 0.5)), y: o.y + dx * 0.6 })) }}
      onPointerUp={() => { drag.current = null; if (!paint) auto.current = true }}>
      <div className="absolute inset-0 pointer-events-none" style={{ background: 'radial-gradient(circle at 50% 38%, color-mix(in srgb,var(--a2) 22%,transparent), transparent 60%)' }} />
      <div className="absolute pointer-events-none rounded-[50%]" style={{ width: 150 * zoom, height: 26 * zoom, bottom: height / 2 - 23 * S + 6, background: 'radial-gradient(closest-side, rgba(0,0,0,.35), transparent)' }} />
      <div style={{ position: 'relative', width: 0, height: 0, transformStyle: 'preserve-3d', transform: `rotateX(${rot.x}deg) rotateY(${rot.y}deg)` }}>
        {body && base.map((p, i) => <Cube key={i} p={p} tex={skin} S={S} paint={paintSkin} onHit={onSkinHit} />)}
        {body && showOver && over.map((p, i) => <Cube key={'o' + i} p={p} tex={skin} S={S} paint={paintSkin && !view.dimOver} onHit={onSkinHit} dim={view.dimOver} />)}
        {cape && view.showCape !== false && <div style={{ position: 'absolute', left: 0, top: 0, transformStyle: 'preserve-3d', transform: `translate3d(0px,${-8 * S}px,${-2.1 * S}px) rotateX(${-swing}deg) rotateY(180deg)` }}>
          <Cube p={{ ...capePart, tw: 64, th: 32 }} tex={cape} S={S} paint={paintCape} onHit={onCapeHit} />
        </div>}
      </div>
      {paint && <div className="absolute bottom-3 left-0 right-0 text-center text-xs muted pointer-events-none">ЛКМ — рисовать · ПКМ — вращать · колесо — масштаб{capeSize[0] > 64 ? ' · HD-плащ ' + capeSize[0] + '×' + capeSize[1] : ''}</div>}
    </div>)
}
