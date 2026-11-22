package dev.ev6.ccnc_media;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import hmgapi.hook.Bridge;
import hmgapi.hook.LoadedApp;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Media changes outside navigation; projection screens and the navigation host use their own UI. */
final class GlobalMediaNotice {
 private final ModContext mod;private final LoadedApp app;private final Context context;private final Handler ui=new Handler(Looper.getMainLooper());
 private int pendingMode=Integer.MIN_VALUE,lastMode=Integer.MIN_VALUE,cacheMode=Integer.MIN_VALUE;private String lastTitle,lastArtist,pendingTitle,pendingArtist,cachedTitle,cachedArtist;private byte[] pendingArt;private WindowManager wm;private FrameLayout root;private LinearLayout panel;
 private final Runnable present=new Runnable(){public void run(){show();}};
 private final Runnable hide=new Runnable(){public void run(){if(panel==null)return;final View old=panel;old.animate().translationY(85).setDuration(350).withEndAction(new Runnable(){public void run(){if(panel==old)remove();}}).start();}};
 private final Runnable check=new Runnable(){public void run(){if(root==null)return;if(!mod.enabled()||!allowedForeground()){remove();return;}ui.postDelayed(this,200);}};
 private GlobalMediaNotice(ModContext m,LoadedApp a){mod=m;app=a;context=a.context;}
 static void install(ModContext mod,LoadedApp app){if(app.context==null)return;try{GlobalMediaNotice n=new GlobalMediaNotice(mod,app);n.register();n.hookBluetooth();}catch(RuntimeException e){mod.log("Global media notice unavailable");}catch(LinkageError e){mod.log("Global media notice unavailable");}}
 private void register(){IntentFilter f=new IntentFilter();f.addAction("com.hkmc.radio.widgetupdate");f.addAction("com.hkmc.audio.usb.widgetupdate");f.addAction("com.hkmc.dipoaudio.widgetupdate");f.addAction("com.hkmc.gpmaudio.widgetupdate");f.addAction("com.hkmc.genie_music.widgetupdate");context.registerReceiver(new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){receive(i);}},f);}
 private boolean accepted(int mode,String action){return (mode==2||mode==3||mode==4)&&"com.hkmc.radio.widgetupdate".equals(action)||mode==49&&"com.hkmc.audio.usb.widgetupdate".equals(action)||mode==115&&"com.hkmc.dipoaudio.widgetupdate".equals(action)||mode==116&&"com.hkmc.gpmaudio.widgetupdate".equals(action)||mode==131&&"com.hkmc.genie_music.widgetupdate".equals(action);}
 private void receive(Intent intent){try{if(!mod.enabled()||intent==null)return;int mode=AndroidRealMode.read(app.classLoader);if(!accepted(mode,intent.getAction()))return;if(cacheMode!=mode){cacheMode=mode;cachedTitle=null;cachedArtist=null;}
  if(mode==2||mode==3||mode==4){if(intent.hasExtra("frq_total")){float f=intent.getFloatExtra("frq_total",Float.NaN);if(!Float.isNaN(f)&&!Float.isInfinite(f)){String band=intent.getStringExtra("radio_mode");String name=(band==null?"FM":band)+" "+String.format(java.util.Locale.US,"%.1f",f);if(!name.equals(cachedTitle))cachedArtist=null;cachedTitle=name;}}if(intent.hasExtra("broad_name"))cachedArtist=intent.getStringExtra("broad_name");}
  else{if(intent.hasExtra("filename")){String name=intent.getStringExtra("filename");if(name==null||!name.equals(cachedTitle))cachedArtist=null;cachedTitle=name;}if(intent.hasExtra("artist_name"))cachedArtist=intent.getStringExtra("artist_name");}
  byte[] art=intent.getByteArrayExtra("artwork_raw_image");if(art!=null&&(art.length==0||art.length>131072))art=null;update(mode,cachedTitle,cachedArtist,art);
 }catch(RuntimeException ignored){}catch(LinkageError ignored){}}
 private void hookBluetooth(){try{Class<?> status=Class.forName("com.hkmc.splitaidl.BtAudioSplitStatus",false,app.classLoader),receiver=Class.forName("com.mobis.splitscreen.right.k.w",false,app.classLoader);final Field title=status.getDeclaredField("mSongTitle"),artist=status.getDeclaredField("mArtistName");title.setAccessible(true);artist.setAccessible(true);Method m=receiver.getDeclaredMethod("updateBtAudioSplitStatus",status);m.setAccessible(true);Bridge.hookMethod(m,new MethodHook(){@Override protected void beforeHookedMethod(Param p){try{if(!mod.enabled()||Binder.getCallingUid()!=1000||p.args==null||p.args.length!=1||AndroidRealMode.read(app.classLoader)!=54)return;final String name=(String)title.get(p.args[0]),performer=(String)artist.get(p.args[0]);ui.post(new Runnable(){public void run(){update(54,name,performer,null);}});}catch(Exception ignored){}catch(LinkageError ignored){}}});}catch(Exception e){mod.log("Global Bluetooth notice unavailable");}catch(LinkageError e){mod.log("Global Bluetooth notice unavailable");}}
 private void update(int mode,String name,String performer,byte[] art){if(name==null||name.trim().isEmpty()||name.length()>256||(performer!=null&&performer.length()>256))return;if(mode!=lastMode){ui.removeCallbacks(present);pendingTitle=null;remove();lastMode=mode;lastTitle=name;lastArtist=performer;return;}boolean changed=lastTitle!=null&&(!name.equals(lastTitle)||!same(performer,lastArtist));lastTitle=name;lastArtist=performer;if(!changed)return;pendingMode=mode;pendingTitle=name;pendingArtist=mode==115?BottomMediaView.CarPlayBadge.artist(performer):performer;pendingArt=art==null?null:art.clone();ui.removeCallbacks(present);ui.postDelayed(present,250);}
 private static boolean same(String a,String b){return a==b||(a!=null&&a.equals(b));}
 private boolean allowedForeground(){try{ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);List<ActivityManager.RunningTaskInfo> tasks=manager==null?null:manager.getRunningTasks(1);if(tasks==null||tasks.isEmpty()||tasks.get(0).topActivity==null)return false;String pkg=tasks.get(0).topActivity.getPackageName();return GlobalNoticePolicy.canShow(pkg);}catch(RuntimeException e){return false;}}
 private void show(){if(!mod.enabled()||!allowedForeground()||pendingTitle==null)return;remove();try{wm=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);if(wm==null)return;android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(metrics);if(metrics.widthPixels!=1920||metrics.heightPixels!=720)return;boolean light=(context.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_NO;
  root=new FrameLayout(context);root.setClipChildren(true);root.setOnTouchListener(new View.OnTouchListener(){public boolean onTouch(View v,MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){v.setVisibility(View.INVISIBLE);ui.post(new Runnable(){public void run(){remove();}});}return true;}});
  panel=new LinearLayout(context);panel.setOrientation(LinearLayout.HORIZONTAL);panel.setGravity(Gravity.CENTER_VERTICAL);panel.setBackgroundColor(light?0xffe4e6e8:0xff333537);ImageView cover=new ImageView(context);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackgroundColor(light?0xffccd0d3:0xff3b404b);if(pendingArt!=null){BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(pendingArt,0,pendingArt.length,b);if(b.outWidth>0&&b.outHeight>0&&b.outWidth<=4096&&b.outHeight<=4096){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=Math.max(1,Math.max(b.outWidth,b.outHeight)/256);Bitmap bitmap=BitmapFactory.decodeByteArray(pendingArt,0,pendingArt.length,o);if(bitmap!=null)cover.setImageBitmap(bitmap);}}panel.addView(cover,new LinearLayout.LayoutParams(85,-1));
  if(pendingMode==115)BottomMediaView.CarPlayBadge.addTo(panel,light);
  LinearLayout details=new LinearLayout(context);details.setOrientation(LinearLayout.VERTICAL);details.setGravity(Gravity.CENTER_VERTICAL);details.setPadding(9,0,9,0);panel.addView(details,new LinearLayout.LayoutParams(0,-1,1));TextView title=text(19,light?0xff30383b:0xffffffff),artist=text(16,light?0xff5c6568:0xffe2e7ec);title.setText(pendingTitle);artist.setText(pendingArtist==null?"":pendingArtist);details.addView(title,new LinearLayout.LayoutParams(-1,-2));details.addView(artist,new LinearLayout.LayoutParams(-1,-2));root.addView(panel,new FrameLayout.LayoutParams(-1,-1));
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(515,85,WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.x=1401;p.y=635;p.setTitle("ccNC Media Notice");panel.setTranslationY(85);wm.addView(root,p);final View current=panel;panel.animate().translationY(0).setDuration(350).withEndAction(new Runnable(){public void run(){if(panel==current)ui.postDelayed(hide,TransientNotice.DWELL_MS);}}).start();ui.postDelayed(check,200);
 }catch(RuntimeException e){remove();}catch(OutOfMemoryError e){remove();}catch(LinkageError e){remove();}}
 private TextView text(int size,int color){TextView t=new TextView(context);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("devicedefault",Typeface.NORMAL));t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.MARQUEE);t.setMarqueeRepeatLimit(-1);t.setSelected(true);return t;}
 private void remove(){ui.removeCallbacks(hide);ui.removeCallbacks(check);if(panel!=null)panel.animate().cancel();if(root!=null&&wm!=null)try{wm.removeViewImmediate(root);}catch(RuntimeException ignored){}root=null;panel=null;wm=null;}
}
