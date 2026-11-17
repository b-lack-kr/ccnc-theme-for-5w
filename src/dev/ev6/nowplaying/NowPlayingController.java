package dev.ev6.nowplaying;

import android.content.Intent;

/* JADX INFO: loaded from: classes.dex */
public final class NowPlayingController {
    private final NowPlayingSurface bar;
    private boolean barAttached;
    private long generation;
    private boolean sessionActive;
    private final WidgetState state = new WidgetState();
    private boolean visible;

    public NowPlayingController(NowPlayingSurface nowPlayingSurface) {
        if (nowPlayingSurface == null) {
            throw new IllegalArgumentException("bar == null");
        }
        this.bar = nowPlayingSurface;
    }

    public void show() {
        if (this.visible) {
            return;
        }
        this.visible = true;
        render();
    }

    public void hide() {
        if (this.visible) {
            this.visible = false;
            this.sessionActive = false;
            this.state.select(WidgetState.Source.NONE);
            if (this.barAttached) {
                this.bar.detach();
                this.barAttached = false;
            }
        }
    }

    public long beginConnection() {
        if (!this.visible) {
            return -1L;
        }
        long j = this.generation;
        if (j == Long.MAX_VALUE) {
            return -1L;
        }
        this.generation = j + 1;
        this.sessionActive = true;
        this.state.select(WidgetState.Source.NONE);
        render();
        return this.generation;
    }

    public void selectVerifiedSource(long j, WidgetState.Source source) {
        if (!isCurrent(j) || source == null) {
            return;
        }
        this.state.select(source);
        render();
    }

    public void onTrustedWidget(long j, Intent intent) {
        if (isCurrent(j) && WidgetIntentDecoder.apply(this.state, intent)) {
            render();
        }
    }

    public void onAuthenticatedFrame(long j, MediaFrameCodec.Frame frame) {
        if (isCurrent(j) && frame != null && this.state.source() == frame.source) {
            this.state.update(frame.source, true, frame.title, true, frame.artist);
            render();
            NowPlayingSurface nowPlayingSurface = this.bar;
            if (nowPlayingSurface instanceof PlaybackStateSurface) {
                ((PlaybackStateSurface) nowPlayingSurface).setPlaybackState(frame.rawPlayState);
            }
            NowPlayingSurface nowPlayingSurface2 = this.bar;
            if (nowPlayingSurface2 instanceof ProgressSurface) {
                ((ProgressSurface) nowPlayingSurface2).setProgress(frame.positionMs, frame.durationMs);
            }
            if (!this.barAttached || frame.artwork == null) {
                return;
            }
            NowPlayingSurface nowPlayingSurface3 = this.bar;
            if (nowPlayingSurface3 instanceof ArtworkSurface) {
                ((ArtworkSurface) nowPlayingSurface3).setEncodedArtwork(frame.artwork);
            }
        }
    }

    public void onConnectionClosed(long j) {
        if (isCurrent(j)) {
            this.sessionActive = false;
            this.state.select(WidgetState.Source.NONE);
            render();
        }
    }

    private boolean isCurrent(long j) {
        return this.visible && this.sessionActive && j >= 0 && j == this.generation;
    }

    private void render() {
        String strTitle = this.state.title();
        if (!this.visible || !this.sessionActive || this.state.source() == WidgetState.Source.NONE) {
            if (this.barAttached) {
                this.bar.detach();
                this.barAttached = false;
                return;
            }
            return;
        }
        if (strTitle == null || strTitle.trim().length() == 0) strTitle = "재생 정보 없음";
        if (!this.barAttached) {
            this.bar.attach();
            this.barAttached = true;
        }
        this.bar.update(this.state.source() == WidgetState.Source.BLUETOOTH ? "BT" : "MEDIA", strTitle, this.state.artist());
    }
}
