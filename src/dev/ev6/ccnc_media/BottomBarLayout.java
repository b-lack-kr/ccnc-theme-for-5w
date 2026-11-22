package dev.ev6.ccnc_media;

/* JADX INFO: loaded from: classes.dex */
public final class BottomBarLayout {
    private static volatile boolean compact = true;
    static void setCompact(boolean value) { compact = value; }

    public static final class Result {
        public final BarPlacement.Box artwork;
        public final BarPlacement.Box details;
        public final BarPlacement.Box next;
        public final BarPlacement.Box playPause;
        public final BarPlacement.Box previous;
        public final BarPlacement.Box row;

        private Result(BarPlacement.Box box, BarPlacement.Box box2, BarPlacement.Box box3, BarPlacement.Box box4, BarPlacement.Box box5, BarPlacement.Box box6) {
            this.row = box;
            this.artwork = box2;
            this.details = box3;
            this.previous = box4;
            this.playPause = box5;
            this.next = box6;
        }
    }

    public static BarPlacement.Box provisionalGuidanceButton(boolean z) {
        int i = z ? 1280 : 1920;
        return new BarPlacement.Box(i - 138, 635, i, 720);
    }

    public static Result resolve(int i, int i2, int i3, boolean z) {
        return resolve(i, i2, i3, z, null);
    }

    public static Result resolve(int i, int i2, int i3, boolean z, BarPlacement.Box box) {
        if ((i != 1920 && (!z || i != 1280)) || i2 != 720 || i3 != 240) {
            return null;
        }
        int iMin = z ? 1280 : 1920;
        int i4 = iMin - 515;
        boolean proxy = box != null && GuidanceProxy.active() && box.left == iMin - 138 && box.right == iMin && box.top == 635 && box.bottom == 720;
        if (!proxy && box != null && box.left < box.right && box.top < box.bottom && box.top < 720 && box.bottom > 635 && box.left < iMin && box.right > i4) {
            iMin = Math.min(iMin, box.left - 4);
            i4 = iMin - 515;
        }
        if (i4 < 284) {
            return null;
        }
        int rowLeft = iMin - (compact ? 519 : 644);
        if (rowLeft < 288) return null;
        BarPlacement.Box box2 = new BarPlacement.Box(rowLeft, 635, iMin - 4, 720);
        BarPlacement.Box box3 = new BarPlacement.Box(rowLeft, 635, rowLeft + 85, 720);
        int i5 = iMin - 126;
        BarPlacement.Box box4 = new BarPlacement.Box(iMin - 189, 635, i5, 720);
        int i6 = iMin - 63;
        BarPlacement.Box box5 = new BarPlacement.Box(i5, 635, i6, 720);
        BarPlacement.Box box6 = new BarPlacement.Box(i6, 635, iMin, 720);
        BarPlacement.Box box7 = new BarPlacement.Box(box3.right + 6, 639, box4.left - 4, 716);
        if (box7.right <= box7.left) {
            return null;
        }
        return new Result(box2, box3, box7, box4, box5, box6);
    }

    private BottomBarLayout() {
    }
}
