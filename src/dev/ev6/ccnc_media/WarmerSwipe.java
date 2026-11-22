package dev.ev6.ccnc_media;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import hmgapi.hook.Bridge;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Observes activity touches without consuming the original application's input. */
final class WarmerSwipe {
 private static final String CLIMATE="com.hkmc.telematics.service.apps.climate";
 private final ModContext mod;
 private final WeakHashMap<Activity,State> states=new WeakHashMap<Activity,State>();
 private long lastOpen;
 private static final class State {float x,y;long time;int pointer,width,height;boolean valid;}
 WarmerSwipe(ModContext context){mod=context;}
 void install(){try{Bridge.hookMethod(Activity.class.getDeclaredMethod("dispatchTouchEvent",MotionEvent.class),new MethodHook(){
  @Override protected void afterHookedMethod(Param p){
   if(!mod.enabled()||!(p.thisObject instanceof Activity)||p.args==null||p.args.length!=1||!(p.args[0] instanceof MotionEvent))return;
   try{observe((Activity)p.thisObject,(MotionEvent)p.args[0]);}catch(RuntimeException ignored){}catch(LinkageError ignored){}
  }
 });mod.log("Warmer swipe: activity input hook prepared");}catch(Exception e){mod.log("Warmer swipe: input hook unavailable");}catch(LinkageError e){mod.log("Warmer swipe: input hook unavailable");}}
 private void observe(final Activity activity,MotionEvent event){
  if(CLIMATE.equals(activity.getPackageName())||activity.isFinishing())return;
  int action=event.getActionMasked();State s=states.get(activity);
  if(action==MotionEvent.ACTION_DOWN){
   if(!activity.hasWindowFocus())return;
   DisplayMetrics m=new DisplayMetrics();activity.getWindowManager().getDefaultDisplay().getRealMetrics(m);
   s=new State();s.x=event.getRawX();s.y=event.getRawY();s.time=event.getEventTime();s.pointer=event.getPointerId(0);s.width=m.widthPixels;s.height=m.heightPixels;s.valid=true;states.put(activity,s);return;
  }
  if(s==null||!s.valid)return;
  if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_CANCEL){s.valid=false;return;}
  if(action!=MotionEvent.ACTION_UP)return;
  states.remove(activity);
  if(event.getPointerCount()!=1||event.getPointerId(0)!=s.pointer||!activity.hasWindowFocus())return;
  if(!WarmerSwipeMath.matches(s.x,s.y,event.getRawX(),event.getRawY(),event.getEventTime()-s.time,s.width,s.height))return;
  long now=SystemClock.uptimeMillis();if(now-lastOpen<1500)return;lastOpen=now;
  final WeakReference<Activity> owner=new WeakReference<Activity>(activity);
  new Handler(Looper.getMainLooper()).post(new Runnable(){public void run(){Activity a=owner.get();if(a==null||a.isFinishing()||!a.hasWindowFocus()||!mod.enabled())return;
   try{AppSlot.runSwipe(a);}
   catch(RuntimeException e){mod.log("Warmer swipe: popup unavailable");}
  }});
 }
 static void open(Activity a){if(a==null||a.isFinishing())return;try{Intent i=new Intent();i.setClassName(CLIMATE,CLIMATE+".activities.SeatHeatVentPopup");i.setFlags(0x54010000);a.startActivity(i);}catch(RuntimeException ignored){}}
}
