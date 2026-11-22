package dev.ev6.ccnc_media;

/* JADX INFO: loaded from: classes.dex */
public final class WidgetState {
    private String artist;
    private Source source = Source.NONE;
    private String title;

    public enum Source {
        NONE,
        BLUETOOTH,
        MEDIA,
        OSD
    }

    public void select(Source source) {
        if (source == null) {
            throw new IllegalArgumentException("source == null");
        }
        if (this.source != source) {
            this.source = source;
            this.title = null;
            this.artist = null;
        }
    }

    public void update(Source source, boolean z, String str, boolean z2, String str2) {
        if (this.source == Source.NONE || source != this.source) {
            return;
        }
        if (z) {
            this.title = str;
        }
        if (z2) {
            this.artist = str2;
        }
    }

    public Source source() {
        return this.source;
    }

    public String title() {
        return this.title;
    }

    public String artist() {
        return this.artist;
    }
}
