import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.*;
import java.util.zip.*;

/**
 * Small, dependency-free replacement for Loom's remapping, used to build the Asteria mods offline.
 *   named   = Yarn names (what the mod source is written in)
 *   inter   = Fabric intermediary names (what Fabric Loader uses at runtime)
 *
 * Modes:
 *   mc     <yarn mappings dir> <in: intermediary jar> <out: named jar>             build the named Minecraft jar to compile against
 *   lib    <yarn dir> <mc intermediary jar> <in jar> <out jar>                     remap a mod library (e.g. Fabric API) to named
 *   mod    <yarn dir> <mc intermediary jar> <in classes dir> <out dir> [cp jars]   remap the compiled mod back to intermediary,
 *                                                                                  incl. Mixin selectors, @Shadow members and @At targets
 * Mappings: a Yarn checkout (enigma .mapping files) or a .tiny v2 file (named<->intermediary columns).
 */
public class Remap {
    // ---------------------------------------------------------------- mappings (always stored as intermediary -> named)
    static final Map<String, String> CLS = new HashMap<>();          // inter class -> named class
    static final Map<String, String> MTH = new HashMap<>();          // interOwner/name interDesc -> named name
    static final Map<String, String> FLD = new HashMap<>();          // interOwner/name -> named name

    static void loadEnigma(Path dir) throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(dir)) { files = s.filter(p -> p.toString().endsWith(".mapping")).collect(Collectors.toList()); }
        for (Path f : files) {
            Deque<Object[]> stack = new ArrayDeque<>(); // {depth, inter, named} of enclosing CLASS lines
            for (String line : Files.readAllLines(f)) {
                if (line.trim().isEmpty()) continue;
                int depth = 0; while (depth < line.length() && line.charAt(depth) == '\t') depth++;
                String[] t = line.trim().split(" ");
                while (!stack.isEmpty() && (int) stack.peek()[0] >= depth) stack.pop();
                if (t[0].equals("CLASS")) {
                    String inter, named;
                    if (stack.isEmpty()) { inter = t[1]; named = t.length > 2 ? t[2] : t[1]; }
                    else { inter = stack.peek()[1] + "$" + t[1]; named = stack.peek()[2] + "$" + (t.length > 2 ? t[2] : t[1]); }
                    CLS.put(inter, named); stack.push(new Object[]{depth, inter, named});
                } else if (!stack.isEmpty() && (int) stack.peek()[0] == depth - 1) {
                    String owner = (String) stack.peek()[1];
                    if (t[0].equals("METHOD") && t.length >= 4) MTH.put(owner + "/" + t[1] + " " + t[3], t[2]);
                    else if (t[0].equals("FIELD") && t.length >= 4) FLD.put(owner + "/" + t[1], t[2]);
                }
            }
        }
    }
    /** tiny v2 with columns e.g. "intermediary named" */
    static void loadTiny(Path f) throws IOException {
        List<String> lines = Files.readAllLines(f);
        String[] head = lines.get(0).split("\t");
        if (head[0].equals("v1")) { // tiny v1 "official intermediary": used to turn the obfuscated client jar into the intermediary one
            for (String l : lines.subList(1, lines.size())) {
                String[] t = l.split("\t");
                if (t[0].equals("CLASS")) CLS.put(t[1], t[2]);
                else if (t[0].equals("METHOD")) MTH.put(t[1] + "/" + t[3] + " " + t[2], t[4]);
                else if (t[0].equals("FIELD")) FLD.put(t[1] + "/" + t[3], t[4]);
            }
            return;
        }
        int ci = Arrays.asList(head).indexOf("intermediary") - 3, cn = Arrays.asList(head).indexOf("named") - 3;
        String cls = null;
        for (String l : lines.subList(1, lines.size())) {
            String[] t = l.split("\t", -1);
            if (l.startsWith("c\t")) { cls = t[1 + ci]; CLS.put(cls, t[1 + cn]); }
            else if (l.startsWith("\tm\t")) MTH.put(cls + "/" + t[3 + ci] + " " + mapDescFromOfficial(t[2], head, ci), t[3 + cn]);
            else if (l.startsWith("\tf\t")) FLD.put(cls + "/" + t[3 + ci], t[3 + cn]);
        }
    }
    static String mapDescFromOfficial(String d, String[] head, int ci) { return d; } // tiny descs are in the first namespace; we only use files whose first column is intermediary

    /** Yarn leaves most enum constants unmapped: their real name is the string passed to the enum constructor in <clinit>. */
    static void enumNames(Path jar) throws IOException {
        int n = 0;
        try (ZipFile z = new ZipFile(jar.toFile())) {
            for (ZipEntry e : Collections.list(z.entries())) {
                if (!e.getName().endsWith(".class")) continue;
                ClassNode cn = new ClassNode(); new ClassReader(readAll(z.getInputStream(e))).accept(cn, 0);
                if ((cn.access & Opcodes.ACC_ENUM) == 0) continue;
                for (MethodNode m : cn.methods) if (m.name.equals("<clinit>")) {
                    String pending = null; boolean armed = false;
                    for (AbstractInsnNode in : m.instructions) {
                        if (in.getOpcode() == Opcodes.NEW && ((TypeInsnNode) in).desc.equals(cn.name)) { pending = null; armed = true; }
                        else if (armed && in instanceof LdcInsnNode && ((LdcInsnNode) in).cst instanceof String) { String v = (String) ((LdcInsnNode) in).cst; armed = false; if (v.matches("[A-Za-z_$][A-Za-z0-9_$]*")) pending = v; }
                        else if (in.getOpcode() == Opcodes.PUTSTATIC) {
                            FieldInsnNode f = (FieldInsnNode) in;
                            if (pending != null && f.owner.equals(cn.name) && f.desc.equals("L" + cn.name + ";") && !FLD.containsKey(f.owner + "/" + f.name)) { FLD.put(f.owner + "/" + f.name, pending); n++; }
                            pending = null;
                        }
                    }
                }
            }
        }
        System.out.println("enum constants named from <clinit>: " + n);
    }

    // ---------------------------------------------------------------- class hierarchy (needed to resolve inherited members)
    static final class Info { String sup; String[] itf; final Set<String> methods = new HashSet<>(), fields = new HashSet<>(); }
    static final Map<String, Info> H = new HashMap<>();
    static void index(byte[] b) {
        ClassReader r = new ClassReader(b); Info i = new Info(); i.sup = r.getSuperName(); i.itf = r.getInterfaces();
        r.accept(new ClassVisitor(Opcodes.ASM9) {
            public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) { i.methods.add(n + " " + d); return null; }
            public FieldVisitor visitField(int a, String n, String d, String s, Object v) { i.fields.add(n); return null; }
        }, ClassReader.SKIP_CODE);
        H.put(r.getClassName(), i);
    }
    static void indexJar(Path jar) throws IOException {
        try (ZipFile z = new ZipFile(jar.toFile())) { for (ZipEntry e : Collections.list(z.entries())) if (e.getName().endsWith(".class")) index(readAll(z.getInputStream(e))); }
    }

    /** remapper inter -> named (reverse = false) or named -> inter (reverse = true) */
    static final class R extends Remapper {
        final Map<String, String> cls, mth, fld, mthByName;
        R(boolean reverse) {
            if (!reverse) { cls = CLS; mth = MTH; fld = FLD; }
            else {
                cls = new HashMap<>(); mth = new HashMap<>(); fld = new HashMap<>();
                CLS.forEach((a, b) -> cls.put(b, a));
                Remapper fwd = new R(false);
                MTH.forEach((k, v) -> { int sl = k.lastIndexOf('/', k.indexOf(' ')), sp = k.indexOf(' ');
                    mth.put(fwd.map(k.substring(0, sl)) + "/" + v + " " + fwd.mapMethodDesc(k.substring(sp + 1)), k.substring(sl + 1, sp)); });
                FLD.forEach((k, v) -> { int sl = k.lastIndexOf('/'); fld.put(fwd.map(k.substring(0, sl)) + "/" + v, k.substring(sl + 1)); });
            }
            mthByName = new HashMap<>();
            mth.forEach((k, v) -> mthByName.merge(k.substring(0, k.indexOf(' ')), v, (a, b) -> a.equals(b) ? a : "\0"));
        }
        @Override public String map(String n) {
            String m = cls.get(n); if (m != null) return m;
            int d = n.lastIndexOf('$'); if (d > 0) { String o = map(n.substring(0, d)); if (!o.equals(n.substring(0, d))) return o + n.substring(d); }
            return n;
        }
        String findMethod(String owner, String name, String desc, Set<String> seen) {
            if (owner == null || !seen.add(owner)) return null;
            String v = mth.get(owner + "/" + name + " " + desc); if (v != null) return v;
            Info i = H.get(owner); if (i == null) return null;
            v = findMethod(i.sup, name, desc, seen); if (v != null) return v;
            for (String it : i.itf) { v = findMethod(it, name, desc, seen); if (v != null) return v; }
            return null;
        }
        String findField(String owner, String name, Set<String> seen) {
            if (owner == null || !seen.add(owner)) return null;
            String v = fld.get(owner + "/" + name); if (v != null) return v;
            Info i = H.get(owner); if (i == null) return null;
            if (i.fields.contains(name)) return null; // declared here without a mapping
            v = findField(i.sup, name, seen); if (v != null) return v;
            for (String it : i.itf) { v = findField(it, name, seen); if (v != null) return v; }
            return null;
        }
        @Override public String mapMethodName(String owner, String name, String desc) {
            if (name.startsWith("<")) return name;
            String v = findMethod(owner, name, desc, new HashSet<>()); return v != null ? v : name;
        }
        @Override public String mapFieldName(String owner, String name, String desc) { String v = findField(owner, name, new HashSet<>()); return v != null ? v : name; }
        @Override public String mapInvokeDynamicMethodName(String name, String desc) { // lambda -> SAM of the functional interface
            String owner = Type.getReturnType(desc).getInternalName();
            return samName(owner, name, new HashSet<>());
        }
        String samName(String owner, String name, Set<String> seen) {
            if (owner == null || !seen.add(owner)) return name;
            String v = mthByName.get(owner + "/" + name); if (v != null && !v.equals("\0")) return v;
            Info i = H.get(owner); if (i == null) return name;
            for (String it : i.itf) { String r = samName(it, name, seen); if (!r.equals(name)) return r; }
            return name;
        }
    }

    // ---------------------------------------------------------------- jar helpers
    static byte[] readAll(InputStream in) throws IOException { ByteArrayOutputStream o = new ByteArrayOutputStream(); byte[] b = new byte[65536]; int n; while ((n = in.read(b)) > 0) o.write(b, 0, n); in.close(); return o.toByteArray(); }
    static byte[] remapClass(byte[] b, Remapper r) { ClassReader cr = new ClassReader(b); ClassWriter cw = new ClassWriter(0); cr.accept(new ClassRemapper(cw, r), 0); return cw.toByteArray(); }
    static void remapJar(Path in, Path out, Remapper r) throws IOException {
        try (ZipFile z = new ZipFile(in.toFile()); ZipOutputStream o = new ZipOutputStream(Files.newOutputStream(out))) {
            Set<String> done = new HashSet<>();
            for (ZipEntry e : Collections.list(z.entries())) {
                if (e.isDirectory()) continue;
                byte[] b = readAll(z.getInputStream(e)); String n = e.getName();
                if (n.endsWith(".class")) { b = remapClass(b, r); n = new ClassReader(b).getClassName() + ".class"; }
                else if (n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA"))) continue;
                if (!done.add(n)) continue;
                o.putNextEntry(new ZipEntry(n)); o.write(b); o.closeEntry();
            }
        }
    }

    // ---------------------------------------------------------------- mixin-aware remap of the compiled mod (named -> intermediary)
    static final List<String> ERR = new ArrayList<>();
    static String MOD_CLASS_PREFIX = "";
    static void remapMod(Path inDir, Path outDir) throws IOException {
        R fwd = new R(false), rev = new R(true);
        List<Path> files; try (Stream<Path> s = Files.walk(inDir)) { files = s.filter(Files::isRegularFile).collect(Collectors.toList()); }
        Map<String, ClassNode> nodes = new LinkedHashMap<>();
        for (Path f : files) if (f.toString().endsWith(".class")) { byte[] b = Files.readAllBytes(f); index(b); ClassNode cn = new ClassNode(); new ClassReader(b).accept(cn, 0); nodes.put(cn.name, cn); }
        for (ClassNode cn : nodes.values()) {
            List<String> targets = mixinTargets(cn);
            if (targets.isEmpty()) continue;
            // @Shadow / @Overwrite members take the name the target has at runtime
            for (MethodNode m : cn.methods) if (has(m.visibleAnnotations, "Lorg/spongepowered/asm/mixin/Shadow;") || has(m.visibleAnnotations, "Lorg/spongepowered/asm/mixin/Overwrite;")) {
                String inter = resolveMethod(rev, targets, m.name, m.desc, cn.name);
                if (inter != null) rev.mth.put(cn.name + "/" + m.name + " " + m.desc, inter.substring(0, inter.indexOf('(')));
            }
            for (FieldNode f : cn.fields) if (has(f.visibleAnnotations, "Lorg/spongepowered/asm/mixin/Shadow;")) {
                String inter = null; for (String t : targets) { inter = rev.findField(t, f.name, new HashSet<>()); if (inter != null) break; }
                if (inter != null) rev.fld.put(cn.name + "/" + f.name, inter); else if (!targetHasField(targets, f.name)) ERR.add(cn.name + ": @Shadow field not found: " + f.name);
            }
            // selector strings
            for (MethodNode m : cn.methods) for (List<AnnotationNode> al : Arrays.asList(m.visibleAnnotations, m.invisibleAnnotations)) if (al != null) for (AnnotationNode a : al) fixAnnotation(a, rev, targets, cn.name, m);
        }
        rev.mthByName.clear(); rev.mth.forEach((k, v) -> rev.mthByName.merge(k.substring(0, k.indexOf(' ')), v, (a, b) -> a.equals(b) ? a : "\0"));
        for (ClassNode cn : nodes.values()) {
            ClassWriter cw = new ClassWriter(0); cn.accept(new ClassRemapper(cw, rev));
            Path o = outDir.resolve(cn.name + ".class"); Files.createDirectories(o.getParent()); Files.write(o, cw.toByteArray());
        }
        for (Path f : files) if (!f.toString().endsWith(".class")) { Path o = outDir.resolve(inDir.relativize(f).toString()); Files.createDirectories(o.getParent()); Files.copy(f, o, StandardCopyOption.REPLACE_EXISTING); }
    }
    static boolean targetHasField(List<String> targets, String name) { for (String t : targets) { Info i = H.get(t); if (i != null && i.fields.contains(name)) return true; } return false; }
    static boolean has(List<AnnotationNode> l, String d) { if (l == null) return false; for (AnnotationNode a : l) if (a.desc.equals(d)) return true; return false; }
    static List<String> mixinTargets(ClassNode cn) {
        List<String> out = new ArrayList<>();
        for (List<AnnotationNode> al : Arrays.asList(cn.visibleAnnotations, cn.invisibleAnnotations)) if (al != null) for (AnnotationNode a : al) if (a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;") && a.values != null)
            for (int i = 0; i < a.values.size(); i += 2) {
                if (a.values.get(i).equals("value")) for (Object t : (List<?>) a.values.get(i + 1)) out.add(((Type) t).getInternalName());
                if (a.values.get(i).equals("targets")) for (Object t : (List<?>) a.values.get(i + 1)) out.add(((String) t).replace('.', '/'));
            }
        return out;
    }
    /** "name" or "name(desc)" (named) -> "interName(interDesc)" resolved in the target classes */
    static String resolveMethod(R rev, List<String> targets, String name, String desc, String where) {
        if (name.startsWith("<")) return name + (desc != null ? desc : "");
        for (String t : targets) {
            Info i = H.get(t);
            List<String> found = new ArrayList<>();
            for (Set<String> seen = new HashSet<>(); t != null; ) { // walk up the hierarchy for inherited targets
                Info ii = H.get(t); if (ii == null) break;
                for (String md : ii.methods) { String n = md.substring(0, md.indexOf(' ')), d = md.substring(md.indexOf(' ') + 1); if (n.equals(name) && (desc == null || d.equals(desc))) found.add(t + " " + d); }
                if (!found.isEmpty() || !seen.add(t)) break; t = ii.sup;
            }
            if (found.size() > 1) { ERR.add(where + ": ambiguous mixin target " + name + " (" + found + ") — add the descriptor"); return null; }
            if (found.size() == 1) { String owner = found.get(0).split(" ")[0], d = found.get(0).split(" ")[1];
                String inter = rev.mapMethodName(owner, name, d); return inter + rev.mapMethodDesc(d); }
        }
        ERR.add(where + ": mixin target method not found: " + name + (desc != null ? desc : "") + " in " + targets); return null;
    }
    static void fixAnnotation(AnnotationNode a, R rev, List<String> targets, String where, MethodNode m) {
        if (a.values == null) return;
        String d = a.desc;
        boolean injector = d.startsWith("Lorg/spongepowered/asm/mixin/injection/") && !d.endsWith("/At;") && !d.endsWith("/Slice;");
        for (int i = 0; i < a.values.size(); i += 2) {
            String k = (String) a.values.get(i); Object v = a.values.get(i + 1);
            if (injector && k.equals("method")) {
                List<Object> nl = new ArrayList<>();
                for (Object s : (List<?>) v) { String sel = (String) s; int p = sel.indexOf('(');
                    String r = resolveMethod(rev, targets, p < 0 ? sel : sel.substring(0, p), p < 0 ? null : sel.substring(p), where + "#" + m.name);
                    nl.add(r != null ? r : sel); }
                a.values.set(i + 1, nl);
            } else if (d.equals("Lorg/spongepowered/asm/mixin/injection/At;") && k.equals("target")) {
                a.values.set(i + 1, mapMemberRef((String) v, rev, where));
            } else if ((d.equals("Lorg/spongepowered/asm/mixin/gen/Accessor;") || d.equals("Lorg/spongepowered/asm/mixin/gen/Invoker;")) && k.equals("value")) {
                String n = (String) v; String r = null;
                if (d.endsWith("Accessor;")) { for (String t : targets) { r = rev.findField(t, n, new HashSet<>()); if (r != null) break; } }
                else { String full = resolveMethod(rev, targets, n, null, where); if (full != null) r = full.substring(0, full.indexOf('(')); }
                if (r != null) a.values.set(i + 1, r);
            }
            if (v instanceof AnnotationNode) fixAnnotation((AnnotationNode) v, rev, targets, where, m);
            if (v instanceof List) for (Object o : (List<?>) v) if (o instanceof AnnotationNode) fixAnnotation((AnnotationNode) o, rev, targets, where, m);
        }
    }
    /** "Lowner;name(desc)ret" or "Lowner;field:Ltype;" (named) -> intermediary */
    static String mapMemberRef(String s, R rev, String where) {
        int semi = s.indexOf(';'); if (!s.startsWith("L") || semi < 0) return s;
        String owner = s.substring(1, semi), rest = s.substring(semi + 1);
        int p = rest.indexOf('('), c = rest.indexOf(':');
        if (p >= 0) { String name = rest.substring(0, p), desc = rest.substring(p);
            if (!name.startsWith("<") && rev.findMethod(owner, name, desc, new HashSet<>()) == null && !owner.startsWith("java/") && rev.cls.containsKey(owner)) ERR.add(where + ": @At target not mapped: " + s);
            return "L" + rev.map(owner) + ";" + rev.mapMethodName(owner, name, desc) + rev.mapMethodDesc(desc); }
        if (c >= 0) { String name = rest.substring(0, c), desc = rest.substring(c + 1);
            return "L" + rev.map(owner) + ";" + rev.mapFieldName(owner, name, desc) + ":" + rev.mapDesc(desc); }
        return s;
    }

    public static void main(String[] a) throws Exception {
        String mode = a[0]; Path map = Paths.get(a[1]);
        if (Files.isDirectory(map)) loadEnigma(map); else loadTiny(map);
        System.out.println("mappings: " + CLS.size() + " classes, " + MTH.size() + " methods, " + FLD.size() + " fields");
        if (a.length > 2 && Files.exists(Paths.get(a[2]))) enumNames(Paths.get(a[2]));
        switch (mode) {
            case "mc": indexJar(Paths.get(a[2])); remapJar(Paths.get(a[2]), Paths.get(a[3]), new R(false)); break;
            case "lib": indexJar(Paths.get(a[2])); remapJar(Paths.get(a[3]), Paths.get(a[4]), new R(false)); break;
            case "mod": {
                // hierarchy in NAMED names: index the named Minecraft jar + named libs given on the command line
                for (int i = 5; i < a.length; i++) indexJar(Paths.get(a[i]));
                remapMod(Paths.get(a[3]), Paths.get(a[4]));
                if (!ERR.isEmpty()) { ERR.forEach(e -> System.err.println("ERROR " + e)); System.exit(2); }
                break;
            }
            default: throw new IllegalArgumentException(mode);
        }
        System.out.println("ok");
    }
}
