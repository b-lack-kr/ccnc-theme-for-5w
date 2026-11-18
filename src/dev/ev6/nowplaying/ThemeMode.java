package dev.ev6.nowplaying;
/** Uses light colors only when the system explicitly reports its light theme. */
public final class ThemeMode {
 public static boolean isLight(int uiMode) { return (uiMode & 0x30) == 0x10; }
 private ThemeMode() {}
}
