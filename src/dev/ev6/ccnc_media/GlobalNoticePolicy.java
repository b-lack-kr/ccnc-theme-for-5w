package dev.ev6.ccnc_media;
/** The navigation process owns its existing notice; projection screens never show this overlay. */
final class GlobalNoticePolicy {
 static boolean canShow(String pkg){return pkg!=null&&!pkg.isEmpty()&&!pkg.equals("com.mnsoft.navi")&&!pkg.equals("com.daudio.app.dipo")&&!pkg.startsWith("com.google.android.projection")&&!pkg.startsWith("com.google.android.apps.automotive");}
 private GlobalNoticePolicy(){}
}
