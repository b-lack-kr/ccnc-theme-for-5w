package dev.ev6.nowplaying;

/** The theme uses the native Bluetooth OSD suppression on the map. */
public final class OsdModePolicy {
    public enum Mode { MOVE, HIDE_BT }
    public static Mode parse(String ignored) { return Mode.HIDE_BT; }
    public static boolean moveForOsd(Mode mode, boolean visible) { return mode == Mode.MOVE && visible; }
    public static boolean suppressBtRequest(Mode mode, boolean map, boolean bluetooth) {
        return mode == Mode.HIDE_BT && map && bluetooth;
    }
    private OsdModePolicy() {}
}
