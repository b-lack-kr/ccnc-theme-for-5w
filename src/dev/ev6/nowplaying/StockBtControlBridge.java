package dev.ev6.nowplaying;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public final class StockBtControlBridge {
    private Field listenerField;
    private Method method;
    private WeakReference<Object> owner = new WeakReference<>(null);

    public enum Command {
        PLAY_PAUSE(0),
        PREVIOUS(1),
        NEXT(2);

        final int code;

        Command(int i) {
            this.code = i;
        }
    }

    public synchronized boolean capture(Object obj) {
        if (obj != null) {
            if ("com.mobis.splitscreen.right.k.w".equals(obj.getClass().getName())) {
                try {
                    Field declaredField = obj.getClass().getDeclaredField("Rl");
                    declaredField.setAccessible(true);
                    Object obj2 = declaredField.get(obj);
                    Class<?> type = declaredField.getType();
                    if (obj2 != null && "com.mobis.splitscreen.right.k.q".equals(type.getName()) && type.isInstance(obj2)) {
                        Method declaredMethod = type.getDeclaredMethod("at", Integer.TYPE);
                        if (declaredMethod.getReturnType() != Void.TYPE) {
                            return false;
                        }
                        Field declaredField2 = type.getDeclaredField("AW");
                        if (!"com.hkmc.splitaidl.ISplitBtAudioListener".equals(declaredField2.getType().getName())) {
                            return false;
                        }
                        declaredMethod.setAccessible(true);
                        declaredField2.setAccessible(true);
                        this.owner = new WeakReference<>(obj2);
                        this.method = declaredMethod;
                        this.listenerField = declaredField2;
                        return true;
                    }
                    return false;
                } catch (LinkageError unused) {
                    return false;
                } catch (ReflectiveOperationException unused2) {
                    return false;
                } catch (SecurityException unused3) {
                    return false;
                }
            }
        }
        return false;
    }

    public synchronized boolean invoke(Command command) {
        if (command == null) return false;
        int code = command == Command.PREVIOUS ? 166 : command == Command.NEXT ? 167 : android.view.KeyEvent.keyCodeFromString("KEYCODE_SHORTCUT_MEDIA");
        if (code == android.view.KeyEvent.KEYCODE_UNKNOWN) return false;
        try {
            Class<?> cls = Class.forName("android.hardware.input.InputManager");
            Object manager = cls.getMethod("getInstance").invoke(null);
            Method inject = cls.getMethod("injectInputEvent", android.view.InputEvent.class, int.class);
            long time = android.os.SystemClock.uptimeMillis();
            android.view.KeyEvent down = new android.view.KeyEvent(time, time, android.view.KeyEvent.ACTION_DOWN, code, 0);
            android.view.KeyEvent up = new android.view.KeyEvent(time, android.os.SystemClock.uptimeMillis(), android.view.KeyEvent.ACTION_UP, code, 0);
            boolean accepted = Boolean.TRUE.equals(inject.invoke(manager, down, 0));
            boolean released = Boolean.TRUE.equals(inject.invoke(manager, up, 0));
            return accepted && released;
        } catch (Exception e) { return false; } catch (LinkageError e) { return false; }
    }

    public synchronized void clear() {
        this.owner.clear();
        this.method = null;
        this.listenerField = null;
    }
}
