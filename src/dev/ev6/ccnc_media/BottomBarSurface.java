package dev.ev6.ccnc_media;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
public final class BottomBarSurface implements CcncMediaSurface, PrototypeViewport.Target, PlaybackStateSurface, ArtworkSurface, ProgressSurface {
    private final Activity activity;
    private String artist;
    private BottomArtworkReceiver artwork;
    private boolean attached;
    private int attempts;
    private BottomMediaView.Controls controls;
    private Bitmap currentArt;
    private Long durationMs;
    private Boolean guidance;
    private final BottomBarHost host;
    private final GuideOverlay guideOverlay;
    private final ArrivalInfoOverlay arrivalOverlay;
    private boolean navigationScreenAllowed;
    private Boolean open;
    private boolean arrivalViewportKnown;
    private byte[] pendingArt;
    private Boolean playing;
    private Long positionMs;
    private boolean progressEnabled;
    private Runnable retry;
    private String title;
    private final Handler ui;
    private boolean wanted;

    public BottomBarSurface(Activity activity, Handler handler) {
        this(activity, handler, null);
    }

    public BottomBarSurface(Activity activity, Handler handler, View view) {
        this.navigationScreenAllowed = true;
        this.progressEnabled = true;
        if (activity == null || handler == null) {
            throw new IllegalArgumentException();
        }
        this.activity = activity;
        BottomBarHost bottomBarHost = new BottomBarHost(activity);
        this.host = bottomBarHost;
        bottomBarHost.setNavigationView(view);
        this.ui = handler;
        this.guideOverlay = new GuideOverlay(activity, handler);
        this.arrivalOverlay = new ArrivalInfoOverlay(activity, handler);
    }

    @Override // dev.ev6.ccnc_media.CcncMediaSurface
    public void attach() {
        if (this.wanted) {
            return;
        }
        this.wanted = true;
        this.arrivalOverlay.start();
        updateArrivalVisibility();
        this.attempts = 0;
        tryAttach();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryAttach() {
        Diagnostic.set("attach", "attempted");
        if (!this.wanted || this.attached) {
            return;
        }
        if (this.host.attach()) {
            Diagnostic.set("attach", "attached");
            this.attached = true;
            this.guideOverlay.allow(this.navigationScreenAllowed && Boolean.FALSE.equals(this.open));
            this.guideOverlay.start();
            this.host.setMetadata(this.title, this.artist, this.currentArt);
            this.host.setControls(this.controls);
            this.host.setPlaying(this.playing);
            this.host.setProgress(this.positionMs, this.durationMs);
            this.host.setProgressEnabled(this.progressEnabled);
            this.host.setSideOpen(this.navigationScreenAllowed ? this.open : null);
            BottomArtworkReceiver bottomArtworkReceiver = new BottomArtworkReceiver(this.activity, this.ui, new ArtworkPipeline.Sink<Bitmap>() { // from class: dev.ev6.ccnc_media.BottomBarSurface.1
                @Override // dev.ev6.ccnc_media.ArtworkPipeline.Sink
                public void onArtwork(Bitmap bitmap) {
                    BottomBarSurface.this.currentArt = bitmap;
                    if (BottomBarSurface.this.attached) {
                        BottomBarSurface.this.host.setMetadata(BottomBarSurface.this.title, BottomBarSurface.this.artist, BottomBarSurface.this.currentArt);
                    }
                }
            });
            this.artwork = bottomArtworkReceiver;
            bottomArtworkReceiver.start();
            this.artwork.setTrack(this.title, this.artist);
            byte[] bArr = this.pendingArt;
            if (bArr != null) {
                this.artwork.submitVerified(bArr);
                this.pendingArt = null;
                return;
            }
            return;
        }
        int i = this.attempts;
        this.attempts = i + 1;
        if (i >= 10) {
            return;
        }
        Runnable runnable = new Runnable() { // from class: dev.ev6.ccnc_media.BottomBarSurface.2
            @Override // java.lang.Runnable
            public void run() {
                BottomBarSurface.this.retry = null;
                BottomBarSurface.this.tryAttach();
            }
        };
        this.retry = runnable;
        if (this.ui.postDelayed(runnable, 250L)) {
            return;
        }
        this.retry = null;
    }

    @Override // dev.ev6.ccnc_media.CcncMediaSurface
    public void detach() {
        this.wanted = false;
        this.arrivalOverlay.stop();
        this.guideOverlay.stop();
        Runnable runnable = this.retry;
        if (runnable != null) {
            this.ui.removeCallbacks(runnable);
        }
        this.retry = null;
        BottomArtworkReceiver bottomArtworkReceiver = this.artwork;
        if (bottomArtworkReceiver != null) {
            bottomArtworkReceiver.close();
            this.artwork = null;
        }
        this.currentArt = null;
        this.pendingArt = null;
        this.host.detach();
        this.attached = false;
    }

    @Override // dev.ev6.ccnc_media.CcncMediaSurface
    public void update(String str, String str2, String str3) {
        if (!same(this.title, str2) || !same(this.artist, str3)) {
            this.pendingArt = null;
        }
        this.title = str2;
        this.artist = str3;
        BottomArtworkReceiver bottomArtworkReceiver = this.artwork;
        if (bottomArtworkReceiver != null) {
            bottomArtworkReceiver.setTrack("BT".equals(str) ? str2 : null, str3);
        }
        if (this.attached) {
            this.host.setMetadata(str2, str3, this.currentArt);
        }
    }

    @Override // dev.ev6.ccnc_media.ArtworkSurface
    public void setEncodedArtwork(byte[] bArr) {
        if (bArr == null || bArr.length == 0 || bArr.length > 131072) {
            return;
        }
        BottomArtworkReceiver bottomArtworkReceiver = this.artwork;
        if (bottomArtworkReceiver != null) {
            bottomArtworkReceiver.submitVerified(bArr);
        } else {
            this.pendingArt = (byte[]) bArr.clone();
        }
    }

    private static boolean same(Object obj, Object obj2) {
        if (obj != obj2) {
            return obj != null && obj.equals(obj2);
        }
        return true;
    }

    @Override // dev.ev6.ccnc_media.PlaybackStateSurface
    public void setPlaybackState(Integer num) {
        Boolean bool = null;
        if (num != null) {
            if (num.intValue() == 1) {
                bool = Boolean.TRUE;
            } else if (num.intValue() == 0) {
                bool = Boolean.FALSE;
            }
        }
        this.playing = bool;
        if (this.attached) {
            this.host.setPlaying(bool);
        }
    }

    @Override // dev.ev6.ccnc_media.ProgressSurface
    public void setProgress(Long l, Long l2) {
        this.positionMs = l;
        this.durationMs = l2;
        if (this.attached) {
            this.host.setProgress(l, l2);
        }
    }

    public void setProgressEnabled(boolean z) {
        this.progressEnabled = true;
        if (this.attached) {
            this.host.setProgressEnabled(true);
        }
    }

    public void setNavigationReserved(BarPlacement.Box box) {
        this.host.setNavigationReserved(box);
    }

    public void setGuidance(Boolean bool) {
        this.guidance = bool;
        updateArrivalVisibility();
        GuidanceProxy.set(bool);
        updateGuidanceReserved();
    }

    private void updateGuidanceReserved() {
        this.host.setNavigationReserved(Boolean.FALSE.equals(this.guidance) ? null : BottomBarLayout.provisionalGuidanceButton(Boolean.TRUE.equals(this.open)));
    }

    public void setControls(BottomMediaView.Controls controls) {
        this.controls = controls;
        if (this.attached) {
            this.host.setControls(controls);
        }
    }

    public void setNavigationScreenAllowed(boolean z) {
        Diagnostic.set("screen", z ? "allowed" : "blocked");
        this.navigationScreenAllowed = z;
        updateArrivalVisibility();
        this.guideOverlay.allow(z && Boolean.FALSE.equals(this.open));
        if (this.attached) {
            this.host.setSideOpen(z ? this.open : null);
        }
    }

    @Override // dev.ev6.ccnc_media.PrototypeViewport.Target
    public void setRequiredNavigationViewport(BarPlacement.Box box) {
        Diagnostic.set("viewport", box == null ? "unknown" : box.right == 1280 ? "split" : box.right == 1920 ? "full" : "other");
        this.arrivalViewportKnown = box != null && box.left == 0 && box.top == 0 && box.bottom == 720 && (box.right == 1280 || box.right == 1920);
        if (box != null && box.left == 0 && box.top == 0 && box.bottom == 720 && (box.right == 1280 || box.right == 1920)) {
            this.open = box.right == 1280 ? null : Boolean.FALSE;
        } else {
            this.open = null;
        }
        updateArrivalVisibility();
        this.guideOverlay.allow(this.navigationScreenAllowed && Boolean.FALSE.equals(this.open));
        updateGuidanceReserved();
        if (this.attached && !this.host.isAttached()) {
            BottomArtworkReceiver bottomArtworkReceiver = this.artwork;
            if (bottomArtworkReceiver != null) {
                bottomArtworkReceiver.close();
                this.artwork = null;
            }
            this.host.detach();
            this.guideOverlay.stop();
            this.attached = false;
        }
        if (this.wanted && !this.attached && this.open != null && this.retry == null) {
            this.attempts = 0;
            tryAttach();
        }
        if (this.attached) {
            this.host.setSideOpen(this.navigationScreenAllowed ? this.open : null);
        }
    }
    private void updateArrivalVisibility() {
        this.arrivalOverlay.allow(ArrivalInfoPolicy.shouldShow(this.wanted, this.navigationScreenAllowed, this.guidance, this.arrivalViewportKnown));
    }
}
