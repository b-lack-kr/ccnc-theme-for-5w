package dev.ev6.ccnc_media;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import hmgapi.hook.Bridge;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

/** Separate, touch-through panel; never swaps the stock toast's view or window. */
final class VolumePanelHook {
 private final ModContext mod;
 private final WeakHashMap<Object,State> states=new WeakHashMap<Object,State>();
 private boolean installed;
 VolumePanelHook(ModContext m){mod=m;}
 void install(){try{
  Class<?> loader=Class.forName("com.hkmc.internal.Loader");Method load=loader.getDeclaredMethod("getAutomotiveServiceJar",Context.class);
  final Field classLoader=loader.getDeclaredField("mClassLoader");classLoader.setAccessible(true);
  Bridge.hookMethod(load,new MethodHook(){@Override protected void afterHookedMethod(Param p){try{Object result=p.getResult();if(result!=null){ClassLoader cl=(ClassLoader)classLoader.get(result);if(cl!=null)installPanel(cl);}}catch(Exception e){mod.log("Media volume hook unavailable");}catch(LinkageError e){mod.log("Media volume hook unavailable");}}});
 }catch(Exception e){mod.log("Media volume loader unavailable");}catch(LinkageError e){mod.log("Media volume loader unavailable");}}
 private synchronized void installPanel(ClassLoader cl)throws Exception{
  if(installed)return;Class<?> type=Class.forName("com.hkmc.micom.MicomVolumePanel",false,cl);
  final Field viewField=type.getDeclaredField("mView");viewField.setAccessible(true);
  Method show=type.getDeclaredMethod("onShowVolumeChangedNew",int.class,int.class,int.class,int.class,int.class,int.class,int.class);show.setAccessible(true);
  try {
   Method legacy=type.getDeclaredMethod("onShowVolumeChanged",int.class,int.class);
   legacy.setAccessible(true);
   Bridge.hookMethod(legacy,new MethodHook(){@Override protected void beforeHookedMethod(Param p){
    try {
     if(p.args==null || p.args.length!=2)return;
     State state=states.get(p.thisObject);
     if(mod.enabled() && (((Integer)p.args[1]) & 2)!=0){
      View stock=(View)viewField.get(p.thisObject);
      if(stock!=null)stateFor(p.thisObject,stock).show(0,0,1,true,"음소거",0);
     }else if(state!=null){state.hide();}
    }catch(Exception ignored){}catch(LinkageError ignored){}
   }});
  }catch(Exception ignored){}catch(LinkageError ignored){}
  Bridge.hookMethod(show,new MethodHook(){@Override protected void beforeHookedMethod(Param p){try{
   if(p.args==null||p.args.length!=7)return;View stock=(View)viewField.get(p.thisObject);if(stock==null)return;
   int labelId=((Integer)p.args[3]).intValue();int media=stock.getResources().getIdentifier("volume_media","string","android");boolean muted=((Integer)p.args[6]).intValue()==2;boolean supported=isSupported(stock,labelId);boolean custom=mod.enabled()&&(supported||muted);String label=muted?"음소거":"미디어";if(!muted&&supported&&labelId!=media){try{label=stock.getResources().getString(labelId);}catch(RuntimeException ignored){}}
   try{String name=stock.getResources().getResourceEntryName(labelId);android.util.Log.i("ccNCVolumeType","modern type="+name+" selected="+custom+" mute="+muted);}catch(RuntimeException ignored){}
   if(custom){
    int value=muted?0:(Integer)p.args[0];
    int icon=muted||labelId==media?0:(Integer)p.args[1];
    stateFor(p.thisObject,stock).show(value,muted?0:(Integer)p.args[4],(Integer)p.args[5],muted,label,icon);
   }else{State state=states.get(p.thisObject);if(state!=null)state.hide();}
  }catch(Exception ignored){}catch(LinkageError ignored){}}});installed=true;
 }
 private State stateFor(Object owner,View stock){
  State state=states.get(owner);
  if(state==null){state=new State(stock);states.put(owner,state);}
  return state;
 }
 private static boolean isSupported(View stock,int id){if(id==0)return false;String[] names={"volume_media","volume_navi","volume_guidance","volume_naviguidance","volume_voiceguidance","volume_voiceprompts","volume_voiceprompts_short","volume_vui","volume_voicerecognition","volume_voicerecognition_prompts","volume_voicerecognition_prompts_short","volume_uvo","volume_bluelink","volume_call","volume_bluetooth_call"};for(String name:names)if(id==stock.getResources().getIdentifier(name,"string","android"))return true;return false;}
 private static final class State {
  final View stock;final WindowManager wm;final Handler handler;final float stockAlpha;final int stockVisibility;Panel panel;boolean added;ValueAnimator animation;
  final Runnable timeout=new Runnable(){public void run(){fadeOut();}};
  State(View v){stock=v;stockAlpha=v.getAlpha();stockVisibility=v.getVisibility();wm=(WindowManager)v.getContext().getSystemService(Context.WINDOW_SERVICE);handler=new Handler(Looper.myLooper());}
  void show(int value,int min,int max,boolean muted,String label,int iconId){if(wm==null)return;handler.removeCallbacks(timeout);
   try{if(panel==null)panel=new Panel(stock.getContext());panel.update(value,min,max,muted,label,iconId);
    DisplayMetrics m=new DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(m);int screen=m.widthPixels>0?m.widthPixels:1920,art=screen*1401/1920;
    int width=Math.max(1,Math.min(screen,2*(art-screen/2))),height=Math.max(1,(m.heightPixels>0?m.heightPixels:720)*85/720);
    WindowManager.LayoutParams lp=new WindowManager.LayoutParams(width,height,2037,0x118,android.graphics.PixelFormat.TRANSLUCENT);lp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;lp.setTitle("ccNC media volume");
    if(added){wm.updateViewLayout(panel,lp);}else{panel.setOpacity(0);wm.addView(panel,lp);added=true;}
    animateTo(255,false);
    stock.setVisibility(View.INVISIBLE);handler.postDelayed(timeout,2000);
   }catch(RuntimeException e){hide();}catch(LinkageError e){hide();}
  }
  void cancelAnimation(){ValueAnimator old=animation;animation=null;if(old!=null)old.cancel();}
  void animateTo(int target,final boolean removeAtEnd){
   cancelAnimation();if(panel==null)return;final Panel current=panel;
   final ValueAnimator next=ValueAnimator.ofInt(current.opacity,target);animation=next;next.setDuration(350);
   next.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){public void onAnimationUpdate(ValueAnimator a){if(panel==current)current.setOpacity(((Integer)a.getAnimatedValue()).intValue());}});
   next.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(animation!=next)return;animation=null;if(removeAtEnd)dismiss(false);}});next.start();
  }
  void fadeOut(){if(!added||panel==null){dismiss(false);return;}animateTo(0,true);}
  void hide(){dismiss(true);}
  void dismiss(boolean restore){handler.removeCallbacks(timeout);cancelAnimation();if(restore){stock.setAlpha(stockAlpha);stock.setVisibility(stockVisibility);}if(added&&panel!=null){try{wm.removeViewImmediate(panel);}catch(RuntimeException ignored){}added=false;}panel=null;}
 }
 private static final class Panel extends View {
  final Paint paint=new Paint(3);int value,min,max;boolean muted;int opacity=255;String label="미디어";Drawable typeIcon;
  Panel(Context c){super(c);setWillNotDraw(false);}
  void setOpacity(int alpha){opacity=Math.max(0,Math.min(255,alpha));invalidate();}
  void update(int v,int low,int high,boolean isMuted,String text,int iconId){value=v;min=low;max=high;muted=isMuted;label=text;typeIcon=null;if(iconId!=0)try{typeIcon=getResources().getDrawable(iconId);}catch(RuntimeException ignored){}invalidate();}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),cy=h*.50f;int layer=c.saveLayerAlpha(0,0,w,h,opacity,Canvas.ALL_SAVE_FLAG);boolean light=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_NO;
   int fg=light?0xff263a48:0xfff1f5f8,accent=light?0xff087b9f:0xff63d6f6;
   paint.setStyle(Paint.Style.FILL);paint.setColor(light?0xffd5e0e8:0xff24282d);c.drawRoundRect(new RectF(0,0,w,h+14),12,12,paint);
   paint.setColor(fg);paint.setTypeface(Typeface.create("devicedefault",Typeface.NORMAL));paint.setTextSize(h*.36f);paint.setTextAlign(Paint.Align.LEFT);float labelWidth=paint.measureText(label);if(labelWidth>w*.28f){paint.setTextSize(paint.getTextSize()*(w*.28f/labelWidth));labelWidth=paint.measureText(label);}float baseline=cy-(paint.ascent()+paint.descent())/2;c.drawText(label,w*.14f,baseline,paint);
   float ih=h*1.07f,sx=Math.max(w*.265f,w*.14f+labelWidth+h*.12f);float shift=sx-w*.265f;Path speaker=new Path();speaker.moveTo(sx,cy-ih*.075f);speaker.lineTo(sx+ih*.08f,cy-ih*.075f);speaker.lineTo(sx+ih*.18f,cy-ih*.16f);speaker.lineTo(sx+ih*.18f,cy+ih*.16f);speaker.lineTo(sx+ih*.08f,cy+ih*.075f);speaker.lineTo(sx,cy+ih*.075f);speaker.close();if(typeIcon==null)c.drawPath(speaker,paint);else{int size=(int)(h*.38f);typeIcon.setBounds((int)sx,(int)(cy-size/2), (int)sx+size,(int)(cy+size/2));typeIcon.draw(c);}
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.7f);paint.setStrokeCap(Paint.Cap.ROUND);
   if(muted){
    paint.setColor(light ? 0xffbd3746 : 0xffff6976);
    paint.setStrokeWidth(ih*.038f);
    c.drawLine(sx-ih*.035f,cy+ih*.16f,sx+ih*.24f,cy-ih*.16f,paint);
   }
   else if(typeIcon==null){float x=sx+ih*.15f;for(int n=0;n<2;n++){float r=ih*(.11f+n*.07f);c.drawArc(new RectF(x-r,cy-r,x+r,cy+r),-45,90,false,paint);}}
   paint.setStyle(Paint.Style.FILL);
   paint.setColor(accent);paint.setTypeface(Typeface.create("devicedefault",Typeface.BOLD));paint.setTextSize(h*.36f);float numberBaseline=cy-(paint.ascent()+paint.descent())/2;c.drawText(String.valueOf(value),w*.315f+shift,numberBaseline,paint);float start=Math.min(w*.78f,w*.375f+shift),end=w*.86f;
   paint.setStrokeWidth(5.3f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(light?0xffa7b5c0:0xff72869a);c.drawLine(start,cy,end,cy,paint);
   float ratio=max>min?Math.max(0f,Math.min(1f,(float)(value-min)/(max-min))):0f;paint.setColor(accent);if(ratio>0)c.drawLine(start,cy,start+(end-start)*ratio,cy,paint);c.restoreToCount(layer);
  }
 }
}
