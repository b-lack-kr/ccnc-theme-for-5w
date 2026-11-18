package dev.ev6.nowplaying;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import hmgapi.hook.Bridge;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Optional experimental NAV mapping. This mapping is being validated in the vehicle. */
final class NavKeySwap {
 private static final String MOD="blackr-ccnc-theme-for-5w", NAV="com.mnsoft.navi";
 private final ModContext mod;
 private int shortCount; private long lastShort; private Runnable pendingShort;
 private final AtomicInteger generation=new AtomicInteger();
 private volatile String latestScene;
 private volatile long watchStartWall, watchUntil; private volatile int watchId;
 private boolean monitorStarted;
 private Class<?> input;
 private Method inject,sending;
 private Field contextField,handlerField,bindingMod;
 NavKeySwap(ModContext mod){this.mod=mod;}
 void install(){
  try{
   input=Class.forName("hmgapi.hook.Input");Class<?> binding=Class.forName("hmgapi.hook.Input$Binding");
   bindingMod=binding.getDeclaredField("mod");bindingMod.setAccessible(true);
   contextField=input.getDeclaredField("context");contextField.setAccessible(true);
   handlerField=input.getDeclaredField("handler");handlerField.setAccessible(true);
   inject=input.getDeclaredMethod("inject",KeyEvent.class);inject.setAccessible(true);
   sending=input.getDeclaredMethod("sending",Integer.TYPE,Integer.TYPE);sending.setAccessible(true);
   Method fire=input.getDeclaredMethod("fire",binding,String.class,KeyEvent.class);
   Method load=input.getDeclaredMethod("load");
   Bridge.hookMethod(load,new MethodHook(){protected void afterHookedMethod(Param p){try{Context c=(Context)contextField.get(null);Handler h=(Handler)handlerField.get(null);if(c!=null&&h!=null)ensureMonitor(c,h);}catch(Exception ignored){}}});
   Bridge.hookMethod(fire,new MethodHook(){
    protected void beforeHookedMethod(Param p){try{
     if(!mod.enabled()||p.args==null||p.args.length!=3||!MOD.equals(bindingMod.get(p.args[0]))||!"short".equals(p.args[1])||!(p.args[2] instanceof KeyEvent))return;
     Context c=(Context)contextField.get(null);Handler h=(Handler)handlerField.get(null);if(c==null||h==null)return;
     queueShort(c,h,new KeyEvent((KeyEvent)p.args[2]));p.setResult(null);
    }catch(Exception ignored){}}
   });
   mod.log("NAV experimental hook prepared; test mapping available");
  }catch(Exception e){mod.log("NAV experimental hook unavailable; leave binding disabled");}catch(LinkageError e){mod.log("NAV experimental hook unavailable; leave binding disabled");}
 }
 private void queueShort(final Context c,final Handler h,final KeyEvent event){
  final long now=SystemClock.uptimeMillis();
  if(lastShort==0||now-lastShort>1500)shortCount=0;
  lastShort=now;shortCount++;
  if(pendingShort!=null)h.removeCallbacks(pendingShort);
  if(shortCount>=3){shortCount=0;lastShort=0;pendingShort=null;generation.incrementAndGet();
   try{Intent i=new Intent(Intent.ACTION_MAIN);i.setClassName("com.nemesis.manager","com.nemesis.manager.MainActivity");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);c.startActivity(i);mod.log("NAV triple: NEMESIS requested");}catch(RuntimeException e){mod.log("NAV triple: NEMESIS launch unavailable");}return;}
  if(shortCount==1){mod.log("NAV short: opening search");watchSearch(c,h);if(!sendLong(KeyEvent.keyCodeFromString("KEYCODE_SHORTCUT_NAVI"),event))mod.log("NAV short: injection unavailable");}
  pendingShort=new Runnable(){public void run(){pendingShort=null;shortCount=0;lastShort=0;}};
  h.postDelayed(pendingShort,1500);
 }
 private boolean sendLong(final int code,KeyEvent from){
  try{
   final Handler h=(Handler)handlerField.get(null);if(h==null||from==null)return false;
   final int device=from.getDeviceId(),source=from.getSource();final long down=SystemClock.uptimeMillis();
   sending.invoke(null,Integer.valueOf(code),Integer.valueOf(1));
   try{inject.invoke(null,new KeyEvent(down,down,KeyEvent.ACTION_DOWN,code,0,0,device,0,0,source));}catch(Exception e){sending.invoke(null,Integer.valueOf(code),Integer.valueOf(-1));return false;}
   Runnable release=new Runnable(){public void run(){try{inject.invoke(null,new KeyEvent(down,SystemClock.uptimeMillis(),KeyEvent.ACTION_DOWN,code,1,0,device,0,KeyEvent.FLAG_LONG_PRESS,source));}catch(Exception ignored){}finally{try{inject.invoke(null,new KeyEvent(down,SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,code,0,0,device,0,0,source));}catch(Exception ignored){}try{sending.invoke(null,Integer.valueOf(code),Integer.valueOf(-1));}catch(Exception ignored){}}}};
   if(!h.postDelayed(release,500))release.run();
   return true;
  }catch(Exception e){return false;}
 }
 private int navPid(Context c){try{ActivityManager a=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);List<ActivityManager.RunningAppProcessInfo> p=a.getRunningAppProcesses();if(p!=null)for(ActivityManager.RunningAppProcessInfo x:p)if(NAV.equals(x.processName))return x.pid;}catch(RuntimeException ignored){}return -1;}
 private boolean searchForeground(Context c){try{ActivityManager a=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);List<ActivityManager.RunningTaskInfo> tasks=a.getRunningTasks(1);return tasks!=null&&!tasks.isEmpty()&&tasks.get(0).topActivity!=null&&NAV.equals(tasks.get(0).topActivity.getPackageName());}catch(RuntimeException e){return false;}}
 private void watchSearch(final Context c,final Handler h){
  ensureMonitor(c,h);watchStartWall=System.currentTimeMillis();watchUntil=SystemClock.elapsedRealtime()+5000;latestScene=null;watchId=generation.incrementAndGet();mod.log("NAV voice: watching search");
 }
 private synchronized void ensureMonitor(final Context c,final Handler h){
  if(monitorStarted)return;monitorStarted=true;
  Thread t=new Thread(new Runnable(){public void run(){
   try{while(true){java.lang.Process process=null;try{
    process=new ProcessBuilder("/system/bin/logcat","-v","threadtime","-s","ExSL:V","*:S").redirectErrorStream(true).start();
    BufferedReader r=new BufferedReader(new InputStreamReader(process.getInputStream(),"UTF-8"));String line;int scheduled=-1;
    while((line=r.readLine())!=null){
     if(!line.contains("top scene updated: screen(1)"))continue;
     int id=watchId;if(id==0||id!=generation.get()||SystemClock.elapsedRealtime()>=watchUntil)continue;
     int pid=navPid(c);if(pid<0)continue;String scene=NavScrollState.sceneName(line,pid,watchStartWall,System.currentTimeMillis());if(scene==null)continue;
     latestScene=scene;
     if("Search_Places".equals(scene)&&scheduled!=id){scheduled=id;final int target=id;mod.log("NAV voice: search scene detected");
      h.postDelayed(new Runnable(){public void run(){if(target==generation.get()&&SystemClock.elapsedRealtime()<watchUntil&&"Search_Places".equals(latestScene)&&searchForeground(c)){mod.log("NAV voice: tapping microphone");tapMicrophone(c);}else mod.log("NAV voice: search no longer foreground");}},900);
     }
    }
   }catch(Exception e){mod.log("NAV voice: monitor failed "+e.getClass().getSimpleName());}finally{if(process!=null)process.destroy();}
   try{Thread.sleep(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
   }}finally{synchronized(NavKeySwap.this){monitorStarted=false;}}
  }},"ccnc-nav-scene");t.setDaemon(true);t.start();
 }
 private void tapMicrophone(Context c){
  try{WindowManager w=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);DisplayMetrics d=new DisplayMetrics();w.getDefaultDisplay().getRealMetrics(d);if(d.widthPixels!=1920||d.heightPixels!=720||d.densityDpi!=240){mod.log("NAV voice: unsupported display");return;}
   Object manager=Class.forName("android.hardware.input.InputManager").getMethod("getInstance").invoke(null);Method m=manager.getClass().getMethod("injectInputEvent",InputEvent.class,Integer.TYPE);long t=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,1838,135,0),up=MotionEvent.obtain(t,t+40,MotionEvent.ACTION_UP,1838,135,0);down.setSource(InputDevice.SOURCE_TOUCHSCREEN);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{Object a=m.invoke(manager,down,0),b=m.invoke(manager,up,0);mod.log("NAV voice: input "+a+"/"+b);}finally{down.recycle();up.recycle();}
  }catch(Exception e){mod.log("NAV voice: input failed "+e.getClass().getSimpleName());}
 }
}
