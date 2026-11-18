package dev.ev6.nowplaying;
public final class ThemeModeTest {
 public static void main(String[] args) {
  if(ThemeMode.isLight(0)||ThemeMode.isLight(0x20)||!ThemeMode.isLight(0x10)||!ThemeMode.isLight(0x13)||ThemeMode.isLight(0x30))throw new AssertionError("theme fallback");
  System.out.println("PASS: explicit light and default dark themes");
 }
}
