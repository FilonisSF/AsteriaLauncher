import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Static link check of a built mod jar against the intermediary Minecraft jar (+ libraries):
 * every referenced Minecraft class, field and method must exist (searching superclasses / interfaces),
 * every Mixin target method selector and @At target must resolve, no Yarn (named) class name may remain.
 *   java Verify <mod.jar> <mc-intermediary.jar> [other runtime jars...]
 */
public class Verify {
    static final Map<String, ClassNode> CP = new HashMap<>();
    static void load(Path jar) throws IOException {
        try (ZipFile z = new ZipFile(jar.toFile())) {
            for (ZipEntry e : Collections.list(z.entries())) if (e.getName().endsWith(".class")) {
                ClassNode cn = new ClassNode(); new ClassReader(z.getInputStream(e)).accept(cn, ClassReader.SKIP_CODE); CP.putIfAbsent(cn.name, cn);
            }
        }
    }
    static boolean hasMethod(String owner, String name, String desc, Set<String> seen) {
        if (owner == null || !seen.add(owner)) return false;
        if (owner.startsWith("java/") || owner.startsWith("javax/") || owner.startsWith("sun/")) return true; // JDK: trust
        ClassNode cn = CP.get(owner); if (cn == null) return false;
        for (MethodNode m : cn.methods) if (m.name.equals(name) && (desc == null || m.desc.equals(desc))) return true;
        if (hasMethod(cn.superName, name, desc, seen)) return true;
        for (String i : cn.interfaces) if (hasMethod(i, name, desc, seen)) return true;
        return false;
    }
    static boolean hasField(String owner, String name, Set<String> seen) {
        if (owner == null || !seen.add(owner)) return false;
        if (owner.startsWith("java/")) return true;
        ClassNode cn = CP.get(owner); if (cn == null) return false;
        for (FieldNode f : cn.fields) if (f.name.equals(name)) return true;
        if (hasField(cn.superName, name, seen)) return true;
        for (String i : cn.interfaces) if (hasField(i, name, seen)) return true;
        return false;
    }
    static boolean external(String owner) { return owner.startsWith("net/minecraft/") || owner.startsWith("com/mojang/") || owner.startsWith("net/fabricmc/") || owner.startsWith("org/lwjgl/"); }

    public static void main(String[] a) throws Exception {
        for (int i = 1; i < a.length; i++) load(Paths.get(a[i]));
        List<String> err = new ArrayList<>(); int refs = 0;
        Map<String, ClassNode> mine = new HashMap<>();
        try (ZipFile z = new ZipFile(a[0])) {
            for (ZipEntry e : Collections.list(z.entries())) if (e.getName().endsWith(".class")) {
                ClassNode cn = new ClassNode(); new ClassReader(z.getInputStream(e)).accept(cn, 0); mine.put(cn.name, cn); CP.put(cn.name, cn);
            }
        }
        for (ClassNode cn : mine.values()) {
            List<String> targets = new ArrayList<>();
            if (cn.invisibleAnnotations != null) for (AnnotationNode an : cn.invisibleAnnotations) if (an.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
                for (int i = 0; i < an.values.size(); i += 2) if (an.values.get(i).equals("value")) for (Object t : (List<?>) an.values.get(i + 1)) targets.add(((Type) t).getInternalName());
            if (cn.visibleAnnotations != null) for (AnnotationNode an : cn.visibleAnnotations) if (an.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
                for (int i = 0; i < an.values.size(); i += 2) if (an.values.get(i).equals("value")) for (Object t : (List<?>) an.values.get(i + 1)) targets.add(((Type) t).getInternalName());
            for (String t : targets) if (!CP.containsKey(t)) err.add(cn.name + ": mixin target class missing " + t);
            for (MethodNode m : cn.methods) {
                // selectors
                for (List<AnnotationNode> l : Arrays.asList(m.visibleAnnotations, m.invisibleAnnotations)) if (l != null) for (AnnotationNode an : l) checkAnn(cn, an, targets, err);
                if (m.instructions == null) continue;
                for (AbstractInsnNode in : m.instructions) {
                    if (in instanceof MethodInsnNode) { MethodInsnNode mi = (MethodInsnNode) in; String o = mi.owner.startsWith("[") ? "java/lang/Object" : mi.owner;
                        if (external(o) || mine.containsKey(o)) { refs++; if (!hasMethod(o, mi.name, mi.desc, new HashSet<>()) && !isMixinSelf(o, mine)) err.add(cn.name + "." + m.name + ": missing method " + o + "." + mi.name + mi.desc); } }
                    else if (in instanceof FieldInsnNode) { FieldInsnNode fi = (FieldInsnNode) in;
                        if (external(fi.owner) || mine.containsKey(fi.owner)) { refs++; if (!hasField(fi.owner, fi.name, new HashSet<>()) && !isMixinSelf(fi.owner, mine)) err.add(cn.name + "." + m.name + ": missing field " + fi.owner + "." + fi.name); } }
                    else if (in instanceof TypeInsnNode) { String t = ((TypeInsnNode) in).desc; if (external(t) && !CP.containsKey(t)) err.add(cn.name + ": missing class " + t); }
                }
            }
            for (FieldNode f : cn.fields) if (f.visibleAnnotations != null) for (AnnotationNode an : f.visibleAnnotations) if (an.desc.contains("Shadow"))
                { boolean ok = false; for (String t : targets) ok |= hasField(t, f.name, new HashSet<>()); if (!ok) err.add(cn.name + ": @Shadow field not in target: " + f.name); }
        }
        // named (Yarn) leftovers: a class under net/minecraft that is not an intermediary name
        for (ClassNode cn : mine.values()) {
            ClassWriter cw = new ClassWriter(0); cn.accept(cw);
            String s = new String(cw.toByteArray(), "ISO-8859-1");
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("net/minecraft/(client|util|entity|text|world|item|block|server|sound|network|screen|recipe|resource|nbt|particle|scoreboard|tag|village|fluid|stat|state|structure|loot|predicate|command)/[A-Za-z/$]+").matcher(s);
            while (mm.find()) err.add(cn.name + ": Yarn name left in class: " + mm.group());
        }
        System.out.println("checked " + mine.size() + " classes, " + refs + " Minecraft/Fabric references");
        if (err.isEmpty()) { System.out.println("OK"); return; }
        new LinkedHashSet<>(err).forEach(e -> System.out.println("ERROR " + e)); System.exit(1);
    }
    static boolean isMixinSelf(String owner, Map<String, ClassNode> mine) { return mine.containsKey(owner); }
    static void checkAnn(ClassNode cn, AnnotationNode an, List<String> targets, List<String> err) {
        if (an.values == null) return;
        for (int i = 0; i < an.values.size(); i += 2) {
            String k = (String) an.values.get(i); Object v = an.values.get(i + 1);
            if (k.equals("method") && v instanceof List && an.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/")) for (Object o : (List<?>) v) {
                String sel = (String) o; int p = sel.indexOf('('); String name = p < 0 ? sel : sel.substring(0, p), desc = p < 0 ? null : sel.substring(p);
                boolean ok = false; for (String t : targets) ok |= hasMethod(t, name, desc, new HashSet<>());
                if (!ok) err.add(cn.name + ": mixin selector not found in " + targets + ": " + sel);
            }
            if (k.equals("target") && an.desc.equals("Lorg/spongepowered/asm/mixin/injection/At;")) {
                String s = (String) v; int semi = s.indexOf(';'); String owner = s.substring(1, semi), rest = s.substring(semi + 1);
                int p = rest.indexOf('('), c = rest.indexOf(':');
                if (p >= 0 && !hasMethod(owner, rest.substring(0, p), rest.substring(p), new HashSet<>())) err.add(cn.name + ": @At target missing: " + s);
                if (p < 0 && c >= 0 && !hasField(owner, rest.substring(0, c), new HashSet<>())) err.add(cn.name + ": @At field missing: " + s);
            }
            if (k.equals("value") && an.desc.equals("Lorg/spongepowered/asm/mixin/gen/Accessor;")) { boolean ok = false; for (String t : targets) ok |= hasField(t, (String) v, new HashSet<>()); if (!ok) err.add(cn.name + ": @Accessor field missing: " + v); }
            if (v instanceof AnnotationNode) checkAnn(cn, (AnnotationNode) v, targets, err);
            if (v instanceof List) for (Object o : (List<?>) v) if (o instanceof AnnotationNode) checkAnn(cn, (AnnotationNode) o, targets, err);
        }
    }
}
