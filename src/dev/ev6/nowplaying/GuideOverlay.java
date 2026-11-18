package dev.ev6.nowplaying;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/** Separate map control; its input still uses the stock guide-list action. */
final class GuideOverlay {
    private final Activity activity; private final Handler handler;
    private final WindowManager windows; private final Icon icon;
    private boolean running, allowed, added; private Boolean expanded, expected; private long expectedUntil; private int theme=-1; private int shownX=-1, shownY=-1;
    GuideOverlay(Activity a, Handler h) {activity=a;handler=h;windows=(WindowManager)a.getSystemService(Context.WINDOW_SERVICE);icon=new Icon(a);
        icon.setContentDescription("길안내 상세 목록 열기 또는 접기");
        icon.setOnTouchListener(new View.OnTouchListener(){public boolean onTouch(View v,MotionEvent e){int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){v.animate().cancel();v.animate().scaleX(.94f).scaleY(.94f).setDuration(80).start();}else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){v.animate().cancel();v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();}return false;}});
        icon.setOnClickListener(new View.OnClickListener(){public void onClick(View v){toggle();}});
    }
    void start(){if(running)return;running=true;handler.post(tick);}
    void stop(){running=false;handler.removeCallbacks(tick);remove();}
    void allow(boolean value){allowed=value;if(!allowed)remove();}
    private final Runnable tick=new Runnable(){public void run(){if(!running)return;update();handler.postDelayed(this,80);}};
    private void update(){
        View content=activity.findViewById(android.R.id.content);
        boolean ready=allowed&&GuidanceProxy.displayable()&&activity.hasWindowFocus()&&content!=null&&content.getHeight()==720&&(content.getWidth()==1920||content.getWidth()==1280);
        if(!ready){remove();return;}
        Boolean actual=GuidanceProxy.expanded();
        if(expected!=null && (expected.equals(actual)||SystemClock.uptimeMillis()>=expectedUntil)) expected=null;
        Boolean state=expected!=null?expected:actual;
        int mode=activity.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK;
        int x=Boolean.TRUE.equals(state)?content.getWidth()-572:content.getWidth()-56;
        int y=Boolean.TRUE.equals(state)?18:94;
        if(!added){try{WindowManager.LayoutParams p=new WindowManager.LayoutParams(48,48,2003,296,android.graphics.PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.x=x;p.y=y;windows.addView(icon,p);added=true;shownX=x;shownY=y;}catch(RuntimeException e){return;}}
        else if(x!=shownX||y!=shownY){try{WindowManager.LayoutParams p=(WindowManager.LayoutParams)icon.getLayoutParams();p.x=x;p.y=y;windows.updateViewLayout(icon,p);shownX=x;shownY=y;}catch(RuntimeException ignored){}}
        if(!state.equals(expanded)||mode!=theme){expanded=state;theme=mode;boolean light=mode!=Configuration.UI_MODE_NIGHT_YES;GradientDrawable bg=new GradientDrawable();bg.setColor(light?0xffe4e8ea:0xff303a42);bg.setCornerRadius(8);bg.setStroke(1,light?0xff79878d:0xff91a0a8);icon.setBackground(bg);icon.light=light;icon.expanded=state;icon.invalidate();}
    }
    private void remove(){if(added){try{windows.removeViewImmediate(icon);}catch(RuntimeException ignored){}added=false;}expanded=null;expected=null;shownX=-1;shownY=-1;theme=-1;}
    private void toggle(){View content=activity.findViewById(android.R.id.content);if(!allowed||!GuidanceProxy.displayable()||!activity.hasWindowFocus()||content==null||content.getHeight()!=720||(content.getWidth()!=1920&&content.getWidth()!=1280))return;
        long t=SystemClock.uptimeMillis();float x=content.getWidth()-70;MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x,675,0),up=MotionEvent.obtain(t,t+60,MotionEvent.ACTION_UP,x,675,0);try{activity.onTouchEvent(down);activity.onTouchEvent(up);Boolean state=expected!=null?expected:GuidanceProxy.expanded();if(state!=null){expected=Boolean.valueOf(!state.booleanValue());expectedUntil=SystemClock.uptimeMillis()+3000;update();}}finally{down.recycle();up.recycle();}}
    private static final class Icon extends View {final Paint p=new Paint(3);boolean light;Boolean expanded;Icon(Context c){super(c);}protected void onDraw(Canvas c){super.onDraw(c);p.setColor(light?0xff34434a:0xffeef4f6);p.setStrokeWidth(3);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.SQUARE);float x=getWidth()/2f,y=getHeight()/2f;
        if(Boolean.TRUE.equals(expanded)){c.drawLine(x-15,y-4,x-5,y-4,p);c.drawLine(x-5,y-4,x-5,y-14,p);c.drawLine(x+15,y+4,x+5,y+4,p);c.drawLine(x+5,y+4,x+5,y+14,p);}else{c.drawLine(x-15,y-5,x-15,y-15,p);c.drawLine(x-15,y-15,x-5,y-15,p);c.drawLine(x+15,y+5,x+15,y+15,p);c.drawLine(x+15,y+15,x+5,y+15,p);} }}
}
