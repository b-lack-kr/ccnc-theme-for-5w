import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Merge {
    // Apply the migration while reading the hash-pinned binary input. DexPool rebuilds
    // indices for types, debug info, instructions and annotations from this view.
    static String rename(String value) {
        return value.replace("EV6 Now Playing", "ccnc_media")
                .replace("NowPlaying", "CcncMedia")
                .replace("nowPlaying", "ccncMedia")
                .replace("nowplaying", "ccnc_media")
                .replace("now-playing", "ccnc_media");
    }

    static final class MediaDex extends DexBackedDexFile {
        MediaDex(Opcodes op, byte[] bytes) { super(op, bytes); }
        private final OptionalIndexedSection<String> original = super.getStringSection();
        private final OptionalIndexedSection<String> renamed = new OptionalIndexedSection<String>() {
            public int size() { return original.size(); }
            public int getOffset(int index) { return original.getOffset(index); }
            public String get(int index) { return rename(original.get(index)); }
            public String getOptional(int index) { return index == -1 ? null : get(index); }
        };
        @Override public OptionalIndexedSection<String> getStringSection() { return renamed; }
    }

    // Keep the installed entry points when the working tree also contains
    // unreleased integrations. The USB registration delegates to UsbNoticeFilter.
    static ClassDef updateUsbRegistration(ClassDef baseline, ClassDef update) {
        Method registration = null;
        for (Method method : update.getDirectMethods()) {
            if (method.getName().equals("suppressStartupUsbNotices")) registration = method;
        }
        List<Method> methods = new ArrayList<>();
        boolean found = false;
        for (Method method : baseline.getMethods()) {
            if (method.equals(registration)) {
                methods.add(registration);
                found = true;
            } else {
                methods.add(method);
            }
        }
        if (!found) throw new IllegalStateException("Missing compatible USB registration");
        return new ImmutableClassDef(baseline.getType(), baseline.getAccessFlags(), baseline.getSuperclass(),
                baseline.getInterfaces(), baseline.getSourceFile(), baseline.getAnnotations(), baseline.getFields(), methods);
    }

    public static void main(String[] args) throws Exception {
        Opcodes op = Opcodes.forApi(19);
        MediaDex original = new MediaDex(op, Files.readAllBytes(Paths.get(args[0])));
        DexBackedDexFile replacements = DexFileFactory.loadDexFile(new File(args[1]), op);
        Set<String> replaced = new HashSet<>();
        DexPool pool = new DexPool(op);
        boolean release = args.length == 4 && "--release".equals(args[3]);
        ClassDef entry = null;
        for (ClassDef cls : replacements.getClasses()) {
            if (release && cls.getType().equals("Ldev/ev6/ccnc_media/UniversalEntry;")) {
                entry = cls;
                continue;
            }
            replaced.add(cls.getType());
            pool.internClass(cls);
        }
        boolean updatedEntry = false;
        for (ClassDef cls : original.getClasses()) {
            String type = cls.getType();
            if (entry != null && type.equals(entry.getType())) {
                pool.internClass(updateUsbRegistration(cls, entry));
                updatedEntry = true;
                continue;
            }
            int inner = type.indexOf('$');
            if (replaced.contains(type) || (inner >= 0 && replaced.contains(type.substring(0, inner) + ";"))) continue;
            pool.internClass(cls);
        }
        if (release && !updatedEntry) throw new IllegalStateException("Missing release entry");
        File out = new File(args[2]);
        FileDataStore store = new FileDataStore(out);
        try { pool.writeTo(store); } finally { store.close(); }
        DexBackedDexFile check = DexFileFactory.loadDexFile(out, op);
        for (String value : check.getStringSection()) {
            if (!rename(value).equals(value)) throw new IllegalStateException("Unmigrated media name: " + value);
        }
    }
}
