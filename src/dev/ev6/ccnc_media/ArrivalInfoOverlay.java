package dev.ev6.ccnc_media;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.MotionEvent;
import hmgapi.hook.Bridge;
import hmgapi.hook.MethodHook;

/** Displays alternate route information while allowing touches through to the stock bar. */
final class ArrivalInfoOverlay {
 private final Activity activity;private final Handler ui;private HandlerThread thread;private Handler worker;private final WindowManager wm;private final Display view;
 private volatile boolean running,allowed;private boolean added;private long started,suppressUntil;private int[] values;
 private static boolean touchHookInstalled;private static java.lang.ref.WeakReference<ArrivalInfoOverlay> active;
 ArrivalInfoOverlay(Activity a,Handler h){activity=a;ui=h;wm=(WindowManager)a.getSystemService(Context.WINDOW_SERVICE);view=new Display(a);}
 private final Runnable poll=new Runnable(){public void run(){final Handler current=worker;if(!running||current==null)return;final int[] next=allowed?ArrivalInfoReader.read():null;ui.post(new Runnable(){public void run(){if(running&&worker==current){values=next;render();}}});if(running&&worker==current)current.postDelayed(this,2000);}};
 private final Runnable tick=new Runnable(){public void run(){if(!running)return;render();ui.postDelayed(this,500);}};
 void start(){if(running)return;installTouchHook();active=new java.lang.ref.WeakReference<ArrivalInfoOverlay>(this);thread=new HandlerThread("ccnc-arrival-read");thread.start();worker=new Handler(thread.getLooper());running=true;started=SystemClock.uptimeMillis();worker.post(poll);ui.post(tick);}
 void allow(boolean value){if(allowed!=value){allowed=value;started=SystemClock.uptimeMillis();values=null;render();}}
 private void render(){boolean show=running&&allowed&&values!=null&&SystemClock.uptimeMillis()>=suppressUntil&&((SystemClock.uptimeMillis()-started)/10000)%2==1;if(!show){if(added)view.setVisibility(View.GONE);return;}view.setData(values[0],values[1]);if(!added){try{WindowManager.LayoutParams p=new WindowManager.LayoutParams(274,85,WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,0x118,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.x=282;p.y=635;p.setTitle("ccNC arrival information");wm.addView(view,p);added=true;}catch(RuntimeException ignored){return;}}view.setVisibility(View.VISIBLE);}
 private static synchronized void installTouchHook(){if(touchHookInstalled)return;try{java.lang.reflect.Method m=Activity.class.getDeclaredMethod("dispatchTouchEvent",MotionEvent.class);Bridge.hookMethod(m,new MethodHook(){protected void beforeHookedMethod(Param p){ArrivalInfoOverlay a=active==null?null:active.get();if(a==null||p.thisObject!=a.activity||p.args==null||p.args.length!=1||!(p.args[0] instanceof MotionEvent))return;MotionEvent e=(MotionEvent)p.args[0];if(e.getActionMasked()==MotionEvent.ACTION_DOWN&&e.getRawX()>=282&&e.getRawX()<556&&e.getRawY()>=635){a.suppressUntil=SystemClock.uptimeMillis()+6000;a.render();}}});touchHookInstalled=true;}catch(Exception ignored){}catch(LinkageError ignored){}}
 void stop(){running=false;if(worker!=null)worker.removeCallbacksAndMessages(null);ui.removeCallbacks(tick);if(added){try{wm.removeViewImmediate(view);}catch(RuntimeException ignored){}added=false;}if(thread!=null)thread.quit();thread=null;worker=null;}
 private static final class Display extends View{
  private final Paint p=new Paint(3);private int soc,minutes;Display(Context c){super(c);setFocusable(false);setClickable(false);}
  void setData(int s,int m){soc=s;minutes=m;invalidate();}
  protected void onDraw(Canvas c){super.onDraw(c);boolean light=ThemeMode.isLight(getResources().getConfiguration().uiMode);p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,0,0,getHeight(),light?0xffe8e8e9:0xff363737,light?0xffe3e3e3:0xff2e2e2f,Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),p);p.setShader(null);p.setColor(light?0xff30383b:0xffe6ecef);p.setTypeface(Typeface.create("devicedefault",Typeface.NORMAL));p.setTextAlign(Paint.Align.RIGHT);
   p.setColor(light?0xffa0a4a7:0xff686b6d);p.setStrokeWidth(1);c.drawLine(1,0,1,getHeight(),p);p.setColor(light?0xff30383b:0xffe6ecef);
   c.save();c.translate(0,-2);float right=getWidth()-12;p.setTextSize(23);c.drawText("%",right,35,p);float numberRight=right-p.measureText("%")-3;p.setTextSize(34);c.drawText(Integer.toString(soc),numberRight,36,p);
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);c.drawRoundRect(new android.graphics.RectF(22,32,51,52),3,3,p);c.drawLine(54,38,54,46,p);p.setStyle(Paint.Style.FILL);c.drawRect(25,35,25+23*soc/100f,49,p);
   p.setStrokeWidth(1);c.drawLine(78,42,getWidth()-12,42,p);p.setTextSize(32);String t=minutes>=60?(minutes/60)+"시간 "+(minutes%60)+"분":minutes+"분";while(p.getTextSize()>20&&p.measureText(t)>getWidth()-102)p.setTextSize(p.getTextSize()-1);c.drawText(t,right,75,p);c.restore();}
 }
}
