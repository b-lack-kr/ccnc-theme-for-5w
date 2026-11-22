package dev.ev6.ccnc_media;

import android.R;
import android.app.Activity;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.view.View;
import android.view.ViewTreeObserver;
import java.util.concurrent.atomic.AtomicLong;

/* JADX INFO: loaded from: classes.dex */
public final class PrototypeViewport {
    private static final String KEY = "split_screen_status";
    private final Activity activity;
    private final Target bar;
    private final AtomicLong generation;
    private View lastContent;
    private int lastHeight;
    private int lastWidth;
    private int lastX;
    private int lastY;
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener;
    private ViewTreeObserver layoutObserver;
    private final ViewTreeObserver.OnWindowFocusChangeListener focusListener =
            new ViewTreeObserver.OnWindowFocusChangeListener() {
        @Override public void onWindowFocusChanged(boolean hasFocus) {
            if (hasFocus && !stopped) refresh();
        }
    };
    private final PrototypeEventLog log;
    private volatile boolean observeOsd;
    private final ContentObserver observer;
    private volatile Boolean osdVisible;
    private volatile boolean stopped;
    private final HandlerThread thread;
    private final Handler ui;
    private final Handler worker;
    private int observedMode = -999;
    private final Runnable modePoll = new Runnable() { public void run() {
        if (stopped) return;
        int mode = AndroidRealMode.read(activity.getClassLoader());
        if (mode != observedMode) { observedMode = mode; refresh(); }
        worker.postDelayed(this, 1000L);
    }};

    public interface Target {
        void setRequiredNavigationViewport(BarPlacement.Box box);
    }

    public PrototypeViewport(Activity activity, CcncMediaBar ccncMediaBar, Handler handler) {
        this(activity, ccncMediaBar, handler, (PrototypeEventLog) null);
    }

    public PrototypeViewport(Activity activity, final CcncMediaBar ccncMediaBar, Handler handler, PrototypeEventLog prototypeEventLog) {
        this(activity, new Target() { // from class: dev.ev6.ccnc_media.PrototypeViewport.2
            @Override // dev.ev6.ccnc_media.PrototypeViewport.Target
            public void setRequiredNavigationViewport(BarPlacement.Box box) {
                ccncMediaBar.setRequiredNavigationViewport(box);
            }
        }, handler, prototypeEventLog);
    }

    public PrototypeViewport(Activity activity, Target target, Handler handler, PrototypeEventLog prototypeEventLog) {
        this.generation = new AtomicLong();
        this.lastX = Integer.MIN_VALUE;
        this.layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: dev.ev6.ccnc_media.PrototypeViewport.1
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                if (PrototypeViewport.this.stopped) {
                    return;
                }
                View viewFindViewById = PrototypeViewport.this.activity.findViewById(R.id.content);
                int[] iArr = new int[2];
                if (viewFindViewById != null) {
                    viewFindViewById.getLocationOnScreen(iArr);
                }
                int width = viewFindViewById == null ? 0 : viewFindViewById.getWidth();
                int height = viewFindViewById == null ? 0 : viewFindViewById.getHeight();
                if (viewFindViewById == PrototypeViewport.this.lastContent && iArr[0] == PrototypeViewport.this.lastX && iArr[1] == PrototypeViewport.this.lastY && width == PrototypeViewport.this.lastWidth && height == PrototypeViewport.this.lastHeight) {
                    return;
                }
                PrototypeViewport.this.lastContent = viewFindViewById;
                PrototypeViewport.this.lastX = iArr[0];
                PrototypeViewport.this.lastY = iArr[1];
                PrototypeViewport.this.lastWidth = width;
                PrototypeViewport.this.lastHeight = height;
                PrototypeViewport.this.refresh();
            }
        };
        this.activity = activity;
        this.bar = target;
        this.ui = handler;
        this.log = prototypeEventLog;
        HandlerThread handlerThread = new HandlerThread("ev6-prototype-viewport");
        this.thread = handlerThread;
        handlerThread.start();
        Handler handler2 = new Handler(handlerThread.getLooper());
        this.worker = handler2;
        this.observer = new ContentObserver(handler2) { // from class: dev.ev6.ccnc_media.PrototypeViewport.3
            @Override // android.database.ContentObserver
            public void onChange(boolean z) {
                PrototypeViewport.this.refresh();
            }
        };
    }

    public void start() {
        this.ui.post(new Runnable() { // from class: dev.ev6.ccnc_media.PrototypeViewport.4
            @Override // java.lang.Runnable
            public void run() {
                View decorView;
                if (PrototypeViewport.this.stopped || PrototypeViewport.this.layoutObserver != null || (decorView = PrototypeViewport.this.activity.getWindow().getDecorView()) == null) {
                    return;
                }
                ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
                if (viewTreeObserver.isAlive()) {
                    PrototypeViewport.this.layoutObserver = viewTreeObserver;
                    viewTreeObserver.addOnGlobalLayoutListener(PrototypeViewport.this.layoutListener);
                    viewTreeObserver.addOnWindowFocusChangeListener(PrototypeViewport.this.focusListener);
                }
            }
        });
        this.worker.post(new Runnable() { // from class: dev.ev6.ccnc_media.PrototypeViewport.5
            @Override // java.lang.Runnable
            public void run() {
                if (PrototypeViewport.this.stopped) {
                    return;
                }
                try {
                    PrototypeViewport.this.activity.getContentResolver().registerContentObserver(Settings.System.getUriFor(PrototypeViewport.KEY), false, PrototypeViewport.this.observer);
                    PrototypeViewport.this.refresh();
                } catch (LinkageError unused) {
                    PrototypeViewport.this.hide();
                } catch (RuntimeException unused2) {
                    PrototypeViewport.this.hide();
                }
            }
        });
        this.worker.post(this.modePoll);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hide() {
        this.ui.post(new Runnable() { // from class: dev.ev6.ccnc_media.PrototypeViewport.6
            @Override // java.lang.Runnable
            public void run() {
                if (PrototypeViewport.this.stopped) {
                    return;
                }
                PrototypeViewport.this.bar.setRequiredNavigationViewport(null);
            }
        });
    }

    public void requireOsdState() {
        if (this.stopped || this.observeOsd) {
            return;
        }
        this.observeOsd = true;
        refresh();
    }

    public void setOsdVisible(Boolean bool) {
        Diagnostic.set("osd", bool == null ? "unknown" : bool.booleanValue() ? "visible" : "hidden");
        if (this.stopped || this.osdVisible == bool) {
            return;
        }
        if (this.osdVisible == null || !this.osdVisible.equals(bool)) {
            this.osdVisible = bool;
            refresh();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refresh() {
        final long jIncrementAndGet = this.generation.incrementAndGet();
        PrototypeEventLog prototypeEventLog = this.log;
        if (prototypeEventLog != null) {
            prototypeEventLog.event(BoundedEventStore.Event.VIEW_UNKNOWN, null);
        }
        hide();
        this.worker.postDelayed(new Runnable() { // from class: dev.ev6.ccnc_media.PrototypeViewport.7
            @Override // java.lang.Runnable
            public void run() {
                if (PrototypeViewport.this.stopped || PrototypeViewport.this.generation.get() != jIncrementAndGet) {
                    return;
                }
                try {
                    int i = Settings.System.getInt(PrototypeViewport.this.activity.getContentResolver(), PrototypeViewport.KEY, -1);
                    Diagnostic.set("split_setting", i == 0 ? "closed" : i == 1 ? "open" : "unknown");
                    if (i == 0 || i == 1) {
                        Boolean osd = PrototypeViewport.this.observeOsd ? PrototypeViewport.this.osdVisible : Boolean.FALSE;
                        /* This reader tracks the Bluetooth OSD.  For confirmed non-Bluetooth
                           sources it may be unavailable after a panel restart. */
                        int mode = PrototypeViewport.this.observedMode;
                        if (osd == null && (mode == 2 || mode == 3 || mode == 4 || mode == 49 || mode == 115 || mode == 116 || mode == 131)) osd = Boolean.FALSE;
                        final OsdViewportArbiter.Result resultResolve = OsdViewportArbiter.resolve(Boolean.valueOf(i == 1), osd, OsdModePolicy.Mode.MOVE);
                        Diagnostic.set("viewport_reason", resultResolve == OsdViewportArbiter.Result.UNKNOWN ? "osd_unknown" : resultResolve == OsdViewportArbiter.Result.RIGHT_PANEL ? "right_panel" : "full");
                        if (resultResolve == OsdViewportArbiter.Result.UNKNOWN) {
                            return;
                        }
                        PrototypeViewport.this.ui.post(new Runnable() { // from class: dev.ev6.ccnc_media.PrototypeViewport.7.1
                            @Override // java.lang.Runnable
                            public void run() {
                                View viewFindViewById;
                                if (PrototypeViewport.this.stopped || PrototypeViewport.this.generation.get() != jIncrementAndGet || (viewFindViewById = PrototypeViewport.this.activity.findViewById(R.id.content)) == null) {
                                    return;
                                }
                                int[] iArr = new int[2];
                                viewFindViewById.getLocationOnScreen(iArr);
                                int width = viewFindViewById.getWidth();
                                Diagnostic.set("content", width == 1920 ? "full" : width == 1280 ? "split" : "other");
                                boolean z = width == 1920 || (resultResolve == OsdViewportArbiter.Result.RIGHT_PANEL && width == 1280);
                                if (iArr[0] == 0 && iArr[1] == 0 && z && viewFindViewById.getHeight() == 720 && PrototypeViewport.this.activity.getResources().getDisplayMetrics().densityDpi == 240) {
                                    PrototypeViewport.this.bar.setRequiredNavigationViewport(new BarPlacement.Box(0, 0, resultResolve != OsdViewportArbiter.Result.RIGHT_PANEL ? 1920 : 1280, 720));
                                    if (PrototypeViewport.this.log != null) {
                                        PrototypeViewport.this.log.event(resultResolve == OsdViewportArbiter.Result.RIGHT_PANEL ? BoundedEventStore.Event.VIEW_OPEN : BoundedEventStore.Event.VIEW_CLOSED, null);
                                    }
                                }
                            }
                        });
                    }
                } catch (LinkageError | RuntimeException unused) {
                }
            }
        }, 250L);
    }

    public void stop() {
        if (this.stopped) {
            return;
        }
        this.stopped = true;
        this.generation.incrementAndGet();
        this.ui.post(new Runnable() { // from class: dev.ev6.ccnc_media.PrototypeViewport.8
            @Override // java.lang.Runnable
            public void run() {
                if (PrototypeViewport.this.layoutObserver != null && PrototypeViewport.this.layoutObserver.isAlive()) {
                    PrototypeViewport.this.layoutObserver.removeOnGlobalLayoutListener(PrototypeViewport.this.layoutListener);
                    PrototypeViewport.this.layoutObserver.removeOnWindowFocusChangeListener(PrototypeViewport.this.focusListener);
                }
                PrototypeViewport.this.layoutObserver = null;
                PrototypeViewport.this.lastContent = null;
            }
        });
        this.worker.post(new Runnable() { // from class: dev.ev6.ccnc_media.PrototypeViewport.9
            @Override // java.lang.Runnable
            public void run() {
                try {
                    PrototypeViewport.this.activity.getContentResolver().unregisterContentObserver(PrototypeViewport.this.observer);
                } catch (LinkageError | RuntimeException unused) {
                }
                PrototypeViewport.this.thread.quit();
            }
        });
    }
}
