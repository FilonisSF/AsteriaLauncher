package ru.safimine.asteria.visuals;

/** real round trip to the server: time of a TCP connect, measured in the background every 10 s */
public final class Ping {
    private Ping() {}
    private static volatile String host; private static volatile int ms = -1; private static long at; private static volatile boolean busy;
    public static int get(String address) {
        if (address == null || address.isEmpty()) return -1;
        if (!address.equals(host)) { host = address; ms = -1; at = 0; }
        long now = System.currentTimeMillis();
        if (!busy && now - at > 10000) { at = now; busy = true; final String a = address;
            Thread t = new Thread(() -> { try {
                String h = a; int port = 25565; int i = a.lastIndexOf(':'); if (i > 0 && a.indexOf(']') < i) { h = a.substring(0, i); try { port = Integer.parseInt(a.substring(i + 1)); } catch (Exception ignored) {} }
                java.net.InetSocketAddress sa = new java.net.InetSocketAddress(h, port); // resolve first: DNS time is not ping
                long best = Long.MAX_VALUE;
                for (int k = 0; k < 2; k++) { long t0 = System.nanoTime(); try (java.net.Socket s = new java.net.Socket()) { s.connect(sa, 3000); best = Math.min(best, (System.nanoTime() - t0) / 1_000_000); } }
                if (best != Long.MAX_VALUE && a.equals(host)) ms = (int) Math.max(1, best);
            } catch (Throwable ignored) {} finally { busy = false; } }, "asteria-ping"); t.setDaemon(true); t.start(); }
        return ms;
    }
}
