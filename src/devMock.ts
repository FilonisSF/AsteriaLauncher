/** `npx vite` in a normal browser has no Electron: a stand-in window.api so the interface can be looked at (dev only) */
if ((import.meta as any).env?.DEV && !(window as any).api) {
  const lists = new Set(['modsSearch', 'pkSearch', 'modsInstalled', 'pkList', 'modsMine', 'tgFeed', 'modsTargets', 'screens', 'events', 'clans', 'shots'])
  const values: Record<string, any> = {
    accounts: { list: [{ id: '1', name: 'Filonis', type: 'offline' }], active: '1' },
    getSettings: { ramMb: 4096, jvm: '', dir: 'C:/Users/you/AppData/Roaming/.asteria', width: 1280, height: 720, discord: true, optimize: true, fabric: true },
    stats: { playMs: 0, launches: 0, perVer: {} }, gameState: { running: false }, verStatus: { installed: true, update: false },
    packExport: { ver: '1.21.4', mods: [], packs: [], shaders: [] }, profList: { list: [], active: null }, sysInfo: { totalMb: 16384, freeMb: 8000, cpus: 8 }, storage: { items: [], total: 0, profiles: [] },
  }
  ;(window as any).api = new Proxy({}, { get: (_, k: string) => (window as any).__mock?.[k] ?? (k.startsWith('on') ? () => () => {} : async () => lists.has(k) ? [] : values[k] ?? null) })
}
export {}
