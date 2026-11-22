package dev.ev6.ccnc_media;
public final class GlobalNoticePolicyTest {
 public static void main(String[] args){
  expect(false,GlobalNoticePolicy.canShow(null));
  expect(false,GlobalNoticePolicy.canShow("com.mnsoft.navi"));
  expect(false,GlobalNoticePolicy.canShow("com.daudio.app.dipo"));
  expect(false,GlobalNoticePolicy.canShow("com.google.android.projection.sink"));
  expect(false,GlobalNoticePolicy.canShow("com.google.android.projection.gearhead"));
  expect(true,GlobalNoticePolicy.canShow("com.hkmc.av.app.radioge"));
  expect(true,GlobalNoticePolicy.canShow("com.android.settings"));
  System.out.println("PASS: notices exclude projection and navigation hosts");
 }
 private static void expect(boolean want,boolean got){if(want!=got)throw new AssertionError("expected "+want+" got "+got);}
}
