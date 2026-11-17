package dev.ev6.nowplaying;
import hmgapi.hook.*;
import android.app.Notification;
import android.app.NotificationManager;
import java.lang.reflect.*;
public final class UniversalEntry extends ModEntry {
 private final NavigationHooks base=new NavigationHooks();
 private dev.ev6.edge.EdgeHandle edge;
 @Override public void zygoteInit(ModContext m)throws Throwable{base.zygoteInit(m);new NavKeySwap(m).install();}
 @Override public void onEnabled(ModContext m,boolean enabled)throws Throwable{base.onEnabled(m,enabled);if(edge!=null)edge.onEnabled(m,enabled);}
 @Override public void onSettingsChanged(ModContext m)throws Throwable{base.onSettingsChanged(m);}
 @Override public void appLoaded(final ModContext mod,final LoadedApp app)throws Throwable {
  base.appLoaded(mod,app);
  if(app==null||app.packageName==null)return;
  if("com.mobis.splitscreen.right".equals(app.packageName)){
   if(edge==null)edge=new dev.ev6.edge.EdgeHandle();
   edge.appLoaded(mod,app);
   suppress(mod,app,"com.mobis.splitscreen.right.p.j","w",new Class<?>[0],new int[]{131});
   suppress(mod,app,"com.mobis.splitscreen.right.media.a","w",new Class<?>[0],new int[]{49});
   suppress(mod,app,"com.mobis.splitscreen.right.v.a","eR",new Class<?>[0],new int[]{54});
   suppress(mod,app,"com.mobis.splitscreen.right.i.A","aa",new Class<?>[]{Boolean.TYPE},new int[]{2,3,4});
   suppress(mod,app,"com.mobis.splitscreen.right.i.D","aa",new Class<?>[]{Boolean.TYPE},new int[]{2,3,4});
   suppressConnectivity(mod,app);
   installGenieArtwork(mod,app);
   RadioArtBridge.install(mod,app);
  }
  if("com.daudio.app.dipo".equals(app.packageName)){
   try { Method m=NotificationManager.class.getDeclaredMethod("notify",String.class,Integer.TYPE,Notification.class);
    Bridge.hookMethod(m,new MethodHook(){protected void beforeHookedMethod(Param p){if(mod.enabled() && p.args!=null && p.args.length==3 && "carplaymusic_osd".equals(p.args[0]) && Integer.valueOf(1002).equals(p.args[1]))p.setResult(null);}});
   } catch(Exception e){mod.log("CarPlay music notification hook unavailable");} catch(LinkageError e){mod.log("CarPlay music notification hook unavailable");}
  }
 }
 private static final java.util.concurrent.ExecutorService artWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
 private static int lastArtHash; private static String lastArtTitle; private static boolean hadArt; private static Boolean artObserved; private static long lastArtSent;
 private static void installGenieArtwork(final ModContext mod,final LoadedApp app){
  try {
   final Class<?> status=Class.forName("com.hkmc.splitaidl.MusicStreamingSplitStatus",false,app.classLoader);
   Class<?> receiver=Class.forName("com.mobis.splitscreen.right.k.w",false,app.classLoader);
   Method target=receiver.getDeclaredMethod("updateMusicStreamingSplitStatus",status);if(target.getReturnType()!=Void.TYPE||Modifier.isStatic(target.getModifiers()))return;target.setAccessible(true);
   final Field image=status.getDeclaredField("mAlbumArt"), title=status.getDeclaredField("mSongTitle"); image.setAccessible(true);title.setAccessible(true);
   Bridge.hookMethod(target,new MethodHook(){protected void beforeHookedMethod(Param p){
    try {
     Diagnostic.set("art_hook","called");
     if(!mod.enabled() || android.os.Binder.getCallingUid()!=1000 || AndroidRealMode.read(app.classLoader)!=131 || p.args==null || p.args.length!=1 || !status.isInstance(p.args[0]))return;
     Object value=image.get(p.args[0]); Object name=title.get(p.args[0]);
     boolean present=value instanceof byte[] && ((byte[])value).length>0;
     Diagnostic.set("art_source",present?"present":"absent");
     synchronized(UniversalEntry.class){if(artObserved==null||artObserved.booleanValue()!=present){artObserved=Boolean.valueOf(present);android.util.Log.i("BlackrMedia",present?"genie artwork present":"genie artwork absent");}}
     if(!(value instanceof byte[])||!(name instanceof String))return;
     byte[] raw=(byte[])value;final String song=(String)name;if(raw.length<32||raw.length>524288||song.length()==0||song.length()>256)return;
     int hash=java.util.Arrays.hashCode(raw);synchronized(UniversalEntry.class){long now=android.os.SystemClock.elapsedRealtime();if(hadArt&&hash==lastArtHash&&song.equals(lastArtTitle)&&now-lastArtSent<10000L)return;hadArt=true;lastArtHash=hash;lastArtTitle=song;lastArtSent=now;}
     final byte[] copy=raw.clone();artWorker.execute(new Runnable(){public void run(){try {
      byte[] bytes=copy;
      if(!mod.enabled()||AndroidRealMode.read(app.classLoader)!=131)return;
      android.content.Intent intent=new android.content.Intent("dev.ev6.nowplaying.GENIE_ART");intent.setPackage("com.mnsoft.navi");intent.putExtra("title",song);intent.putExtra("image",bytes);app.context.sendBroadcast(intent);
      Diagnostic.set("art_forward","sent");
      android.util.Log.i("BlackrMedia","genie artwork forwarded");
     }catch(Exception ignored){}catch(OutOfMemoryError ignored){}catch(LinkageError ignored){}}});
    }catch(Exception ignored){}catch(LinkageError ignored){}
   }});
  }catch(Exception e){mod.log("Genie artwork bridge unavailable");}catch(LinkageError e){mod.log("Genie artwork bridge unavailable");}
 }
 private static boolean in(int mode,int[] modes){for(int value:modes)if(mode==value)return true;return false;}
 private static void suppress(final ModContext mod,final LoadedApp app,String name,String method,Class<?>[] args,final int[] modes){
  try {Class<?> cls=Class.forName(name,false,app.classLoader);Method target=cls.getDeclaredMethod(method,args);if(Modifier.isStatic(target.getModifiers())||target.getReturnType()!=Void.TYPE)return;target.setAccessible(true);
   Bridge.hookMethod(target,new MethodHook(){protected void beforeHookedMethod(Param p){try{if(mod.enabled()&&in(AndroidRealMode.read(app.classLoader),modes))p.setResult(null);}catch(RuntimeException ignored){}catch(LinkageError ignored){}}});
  }catch(Exception e){mod.log("media popup hook unavailable: "+name);}catch(LinkageError e){mod.log("media popup hook unavailable: "+name);}
 }
 private static void suppressConnectivity(final ModContext mod,final LoadedApp app){
  try {Class<?> cls=Class.forName("com.mobis.splitscreen.right.connectivity.n",false,app.classLoader);Method target=cls.getDeclaredMethod("cr",Integer.TYPE);if(Modifier.isStatic(target.getModifiers())||target.getReturnType()!=Void.TYPE)return;target.setAccessible(true);
   final Method metadata=cls.getDeclaredMethod("lS");metadata.setAccessible(true);Class<?> meta=Class.forName("com.mobis.splitscreen.right.connectivity.e",false,app.classLoader);final Field status=meta.getDeclaredField("iU");status.setAccessible(true);
   Bridge.hookMethod(target,new MethodHook(){protected void beforeHookedMethod(Param p){try {int mode=AndroidRealMode.read(app.classLoader);if(!mod.enabled()||(mode!=115&&mode!=116))return;Object m=metadata.invoke(p.thisObject);Object st=m==null?null:status.get(m);if(st==null)return;int play=((Integer)st.getClass().getMethod("getPlayState").invoke(st)).intValue();int call=((Integer)st.getClass().getMethod("getCallState").invoke(st)).intValue();if((play==5||play==10)&&call==0)p.setResult(null);}catch(Exception ignored){}catch(LinkageError ignored){}}});
  }catch(Exception e){mod.log("connectivity music popup hook unavailable");}catch(LinkageError e){mod.log("connectivity music popup hook unavailable");}
 }
}
