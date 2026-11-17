package dev.ev6.nowplaying;

import android.app.ActivityManager;
import android.content.Context;
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

/** Optional experimental NAV mapping. Input bindings remain disabled until vehicle validation. */
final class NavKeySwap {
 private static final String MOD="blackr-ccnc-theme-for-5w", NAV="com.mnsoft.navi";
 private final ModContext mod;
 private final ThreadLocal<Boolean> ownShort=new ThreadLocal<Boolean>();
 private final AtomicInteger generation=new AtomicInteger();
 private volatile String latestScene;
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
   Method send=input.getDeclaredMethod("send",Integer.TYPE,Boolean.TYPE,KeyEvent.class);
   Bridge.hookMethod(fire,new MethodHook(){
    protected void beforeHookedMethod(Param p){try{boolean match=mod.enabled()&&p.args!=null&&p.args.length==3&&MOD.equals(bindingMod.get(p.args[0]))&&"short".equals(p.args[1]);ownShort.set(Boolean.valueOf(match));if(match){Context c=(Context)contextField.get(null);Handler h=(Handler)handlerField.get(null);if(c!=null&&h!=null)watchSearch(c,h);}}catch(Exception ignored){ownShort.remove();}}
    protected void afterHookedMethod(Param p){ownShort.remove();}
   });
   Bridge.hookMethod(send,new MethodHook(){protected void beforeHookedMethod(Param p){try{if(!mod.enabled()||!Boolean.TRUE.equals(ownShort.get())||p.args==null||p.args.length!=3||!Boolean.TRUE.equals(p.args[1])||((Integer)p.args[0]).intValue()!=KeyEvent.keyCodeFromString("KEYCODE_SHORTCUT_NAVI"))return;if(sendLong((Integer)p.args[0],(KeyEvent)p.args[2]))p.setResult(null);}catch(Exception ignored){}}});
   mod.log("NAV experimental hook prepared; binding remains disabled by default");
  }catch(Exception e){mod.log("NAV experimental hook unavailable; leave binding disabled");}catch(LinkageError e){mod.log("NAV experimental hook unavailable; leave binding disabled");}
 }
 private boolean sendLong(final int code,KeyEvent from){
  try{
   final Handler h=(Handler)handlerField.get(null);if(h==null||from==null)return false;
   final int device=from.getDeviceId(),source=from.getSource();final long down=SystemClock.uptimeMillis();
   sending.invoke(null,Integer.valueOf(code),Integer.valueOf(1));
   try{inject.invoke(null,new KeyEvent(down,down,KeyEvent.ACTION_DOWN,code,0,0,device,0,0,source));}catch(Exception e){sending.invoke(null,Integer.valueOf(code),Integer.valueOf(-1));return false;}
   Runnable release=new Runnable(){public void run(){try{inject.invoke(null,new KeyEvent(down,SystemClock.uptimeMillis(),KeyEvent.ACTION_DOWN,code,1,0,device,0,KeyEvent.FLAG_LONG_PRESS,source));}catch(Exception ignored){}finally{try{inject.invoke(null,new KeyEvent(down,SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,code,0,0,device,0,0,source));}catch(Exception ignored){}try{sending.invoke(null,Integer.valueOf(code),Integer.valueOf(-1));}catch(Exception ignored){}}}};
   if(!h.postDelayed(release,50))release.run();
   return true;
  }catch(Exception e){return false;}
 }
 private int navPid(Context c){try{ActivityManager a=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);List<ActivityManager.RunningAppProcessInfo> p=a.getRunningAppProcesses();if(p!=null)for(ActivityManager.RunningAppProcessInfo x:p)if(NAV.equals(x.processName))return x.pid;}catch(RuntimeException ignored){}return -1;}
 private boolean searchForeground(Context c){try{ActivityManager a=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);List<ActivityManager.RunningTaskInfo> tasks=a.getRunningTasks(1);return tasks!=null&&!tasks.isEmpty()&&tasks.get(0).topActivity!=null&&NAV.equals(tasks.get(0).topActivity.getPackageName());}catch(RuntimeException e){return false;}}
 private void watchSearch(final Context c,final Handler h){
  final int id=generation.incrementAndGet();final long startWall=System.currentTimeMillis(),startElapsed=SystemClock.elapsedRealtime();latestScene=null;
  Thread t=new Thread(new Runnable(){public void run(){java.lang.Process process=null;try{
   process=new ProcessBuilder("/system/bin/logcat","-v","threadtime","-s","ExSL:V","*:S").redirectErrorStream(true).start();final java.lang.Process running=process;h.postDelayed(new Runnable(){public void run(){running.destroy();}},5000);
   BufferedReader r=new BufferedReader(new InputStreamReader(process.getInputStream(),"UTF-8"));String line;boolean scheduled=false;
   while(id==generation.get()&&(line=r.readLine())!=null&&SystemClock.elapsedRealtime()-startElapsed<5000){int pid=navPid(c);if(pid<0)continue;String scene=NavScrollState.sceneName(line,pid,startWall,System.currentTimeMillis());if(scene==null)continue;latestScene=scene;if(!scheduled&&"Search_Places".equals(scene)){scheduled=true;h.postDelayed(new Runnable(){public void run(){if(id==generation.get()&&SystemClock.elapsedRealtime()-startElapsed<5000&&"Search_Places".equals(latestScene)&&searchForeground(c))tapMicrophone(c);}},400);}}
  }catch(Exception ignored){}finally{if(process!=null)process.destroy();}}},"ccnc-nav-scene");t.setDaemon(true);t.start();
 }
 private void tapMicrophone(Context c){
  try{WindowManager w=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);DisplayMetrics d=new DisplayMetrics();w.getDefaultDisplay().getRealMetrics(d);if(d.widthPixels!=1920||d.heightPixels!=720||d.densityDpi!=240)return;
   Object manager=Class.forName("android.hardware.input.InputManager").getMethod("getInstance").invoke(null);Method m=manager.getClass().getMethod("injectInputEvent",InputEvent.class,Integer.TYPE);long t=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,1838,135,0),up=MotionEvent.obtain(t,t+40,MotionEvent.ACTION_UP,1838,135,0);down.setSource(InputDevice.SOURCE_TOUCHSCREEN);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{m.invoke(manager,down,0);m.invoke(manager,up,0);}finally{down.recycle();up.recycle();}
  }catch(Exception ignored){}
 }
}
