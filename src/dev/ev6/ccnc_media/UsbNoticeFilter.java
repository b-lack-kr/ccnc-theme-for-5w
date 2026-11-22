package dev.ev6.ccnc_media;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import hmgapi.hook.Bridge;
import hmgapi.hook.LoadedApp;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import java.lang.reflect.Method;

/** Suppresses the two informational USB posts made by stock SystemUI. */
final class UsbNoticeFilter {
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final String TAG = "StorageNotification";
    private static boolean installed;

    static synchronized void install(final ModContext mod, LoadedApp app) {
        if (installed || app == null || !SYSTEM_UI.equals(app.packageName) || app.context == null) return;
        try {
            final Resources res = app.context.getResources();
            final int reading = res.getIdentifier("usb_reading", "string", SYSTEM_UI);
            final int unsupported = res.getIdentifier("usb_not_supported", "string", SYSTEM_UI);
            if (reading == 0 && unsupported == 0) {
                mod.log("USB notice strings unavailable in SystemUI");
                return;
            }
            Method notify = NotificationManager.class.getDeclaredMethod("notify", String.class, int.class, Notification.class);
            Bridge.hookMethod(notify, new MethodHook() {
                @Override protected void beforeHookedMethod(Param call) {
                    Object[] args = call.args;
                    if (!mod.enabled() || args == null || args.length != 3 || !TAG.equals(args[0])
                            || !(args[1] instanceof Integer) || !(args[2] instanceof Notification)) return;
                    int id = (Integer)args[1];
                    int resource = id == 0 ? reading : id == 1 ? unsupported : 0;
                    if (resource == 0) return;
                    try {
                        // Resolve text per post, including reposts after a language change.
                        if (!matchesText((Notification)args[2], res.getString(resource))
                                && !fromKnownSource(id, reading, unsupported)) return;
                        call.setResult(null);
                        Log.i("ccNCStartupNotice", id == 0 ? "USB reading notice suppressed" : "Unsupported USB notice suppressed");
                    } catch (RuntimeException ignored) {
                        // A missing resource or unreadable notification leaves stock behaviour intact.
                    }
                }
            });
            installed = true;
            mod.log("SystemUI USB notice filter installed");
        } catch (Exception | LinkageError e) {
            mod.log("SystemUI USB notice filter unavailable");
        }
    }

    private static boolean matchesText(Notification item, String expected) {
        if (expected.isEmpty()) return false;
        if (item.tickerText != null && expected.contentEquals(item.tickerText)) return true;
        Bundle extras = item.extras;
        if (extras == null) return false;
        for (String key : new String[]{Notification.EXTRA_TEXT, Notification.EXTRA_TITLE, Notification.EXTRA_BIG_TEXT}) {
            CharSequence value = extras.getCharSequence(key);
            if (value != null && expected.contentEquals(value)) return true;
        }
        return false;
    }

    private static boolean fromKnownSource(int id, int reading, int unsupported) {
        // This firmware can build custom OSDs without filling Android's public text extras.
        if (reading != 0x7f0a00c8 || unsupported != 0x7f0a00c9) return false;
        String method = id == 0 ? "eJ" : "eK";
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if ("com.android.systemui.usb.l".equals(frame.getClassName()) && method.equals(frame.getMethodName())) return true;
        }
        return false;
    }

    private UsbNoticeFilter() {}
}
