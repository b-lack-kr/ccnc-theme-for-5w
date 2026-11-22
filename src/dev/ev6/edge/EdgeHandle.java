package dev.ev6.edge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.content.res.Configuration;
import dev.ev6.ccnc_media.ThemeMode;
import hmgapi.hook.LoadedApp;
import hmgapi.hook.ModContext;
import hmgapi.hook.ModEntry;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

/* Reuses the stock window and original touch listener, including its visibility and safety gates. */
public final class EdgeHandle extends ModEntry {
 private final Handler ui=new Handler(Looper.getMainLooper());
 private ModContext mod;
 private FrameLayout root;
 private View first,second;
 private float firstAlpha,secondAlpha;
 private Handle handle;
 private View scrollGuide; private float guideAlpha;
 private PageIndicator pageIndicator; private ViewGroup indicatorParent;
 private EdgeHandlePager pager;
 private int originalX;
 private final Runnable scan=new Runnable(){public void run(){
  if(mod==null||!mod.enabled()){remove();return;}
  try{FrameLayout current=find(); if(current!=root){remove();if(current!=null)install(current);}position();hideGuide();updateIndicator();}catch(Exception ignored){}
  ui.postDelayed(this,250);
 }};
 @Override public void appLoaded(ModContext m,LoadedApp app){
  if(app==null||!"com.mobis.splitscreen.right".equals(app.packageName))return;
  mod=m;if(pager==null){pager=new EdgeHandlePager(m);pager.install(app);}ui.removeCallbacks(scan);ui.post(scan);
 }
 @Override public void onEnabled(ModContext m,boolean enabled){
  if(mod==null)return;ui.removeCallbacks(scan);if(enabled)ui.post(scan);else ui.post(new Runnable(){public void run(){remove();}});
 }
 private static View findWindow(String title) throws Exception {
  Class<?> cls=Class.forName("android.view.WindowManagerGlobal");Object global=cls.getMethod("getInstance").invoke(null);
  Field field=cls.getDeclaredField("mViews");field.setAccessible(true);Object views=field.get(global);
  int count=views instanceof List?((List<?>)views).size():Array.getLength(views);
  for(int i=0;i<count;i++){Object item=views instanceof List?((List<?>)views).get(i):Array.get(views,i);
   if(!(item instanceof View))continue;View v=(View)item;ViewGroup.LayoutParams p=v.getLayoutParams();
   if(p instanceof WindowManager.LayoutParams){WindowManager.LayoutParams w=(WindowManager.LayoutParams)p;
    if(title.contentEquals(w.getTitle())||("com.mobis.splitscreen.right".equals(title)&&w.type==2045&&v.getWidth()>0))return v;
   }
  }return null;
 }
 private static FrameLayout find() throws Exception {View v=findWindow("SS_Button_Window");return v instanceof FrameLayout?(FrameLayout)v:null;}
 private void hideGuide() throws Exception {
  View panel=findWindow("com.mobis.splitscreen.right");View candidate=null;
  if(panel!=null){int id=panel.getResources().getIdentifier("scrollBtnLayer","id","com.mobis.splitscreen.right");if(id!=0)candidate=panel.findViewById(id);}
  if(candidate!=scrollGuide){if(scrollGuide!=null)scrollGuide.setAlpha(guideAlpha);scrollGuide=candidate;if(candidate!=null)guideAlpha=candidate.getAlpha();}
  if(scrollGuide!=null&&scrollGuide.getAlpha()!=0f)scrollGuide.setAlpha(0f);
 }
 private void updateIndicator() throws Exception {
  View panel=findWindow("com.mobis.splitscreen.right");
  if(panel==null){removeIndicator();return;}
  int mainId=panel.getResources().getIdentifier("main_view","id","com.mobis.splitscreen.right");
  int sliderId=panel.getResources().getIdentifier("slidingView","id","com.mobis.splitscreen.right");
  View main=mainId==0?null:panel.findViewById(mainId),slider=sliderId==0?null:panel.findViewById(sliderId);
  if(!(main instanceof FrameLayout)||!(slider instanceof ViewGroup)||pager==null){removeIndicator();return;}
  ViewGroup group=(ViewGroup)main;int[] page=pager.currentPage((ViewGroup)slider);int index=page==null?-1:page[0],count=page==null?0:page[1];
  if(pageIndicator==null||indicatorParent!=group){removeIndicator();pageIndicator=new PageIndicator(group.getContext());pageIndicator.setClickable(false);pageIndicator.setFocusable(false);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(24,200,Gravity.LEFT|Gravity.CENTER_VERTICAL);p.leftMargin=23;group.addView(pageIndicator,p);indicatorParent=group;}
  if(count<=0||index<0||index>=count){pageIndicator.setVisibility(View.GONE);return;}
  boolean light=ThemeMode.isLight(group.getResources().getConfiguration().uiMode);
  pageIndicator.update(index,count,light);pageIndicator.setVisibility(View.VISIBLE);
 }
 private void removeIndicator(){if(pageIndicator!=null&&indicatorParent!=null)indicatorParent.removeView(pageIndicator);pageIndicator=null;indicatorParent=null;}
 private static final class PageIndicator extends View {
  private final Paint pen=new Paint(3);private int selected=-1,count;private boolean light;
  PageIndicator(Context context){super(context);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
  void update(int index,int total,boolean day){if(selected!=index||count!=total||light!=day){selected=index;count=total;light=day;setContentDescription("위젯 "+(index+1)+" / "+total);invalidate();}}
  protected void onDraw(Canvas c){super.onDraw(c);if(count<=0)return;float spacing=Math.min(14f,Math.max(6f,(getHeight()-8f)/Math.max(1,count-1)));float top=(getHeight()-spacing*(count-1))/2f;for(int i=0;i<count;i++){pen.setColor(i==selected?(light?0xff344247:0xfff2f5f6):(light?0x77606b70:0x778f9da2));c.drawCircle(getWidth()/2f,top+i*spacing,i==selected?4f:3.5f,pen);}}
 }
 private void install(FrameLayout v){
  if(v.getChildCount()!=2||v.getWidth()!=80||v.getHeight()!=192||!(v.getChildAt(0) instanceof android.widget.ImageButton)||!(v.getChildAt(1) instanceof android.widget.ImageButton))return;
  root=v;first=v.getChildAt(0);second=v.getChildAt(1);firstAlpha=first.getAlpha();secondAlpha=second.getAlpha();
  originalX=((WindowManager.LayoutParams)v.getLayoutParams()).x;
  first.setAlpha(0f);second.setAlpha(0f);handle=new Handle(v.getContext());v.addView(handle,new FrameLayout.LayoutParams(80,192));
 }
 private void position() throws Exception {
  if(root==null||handle==null)return;
  int state=Settings.System.getInt(root.getContext().getContentResolver(),"split_screen_status",-1);
  if(state!=0&&state!=1){handle.setVisibility(View.INVISIBLE);return;}
  int x=originalX;
  if(state==1){
   View panel=findWindow("com.mobis.splitscreen.right");
   if(panel==null||!panel.isShown()||panel.getWidth()<=0){handle.setVisibility(View.INVISIBLE);return;}
   int[] location=new int[2];panel.getLocationOnScreen(location);
   // Keep the open handle just inside the panel's left edge.
   x=Math.max(0,location[0]-51);
  }
  WindowManager.LayoutParams p=(WindowManager.LayoutParams)root.getLayoutParams();
  if(p.x!=x){p.x=x;((WindowManager)root.getContext().getSystemService(Context.WINDOW_SERVICE)).updateViewLayout(root,p);}
  handle.setVisibility(View.VISIBLE);
 }
 private void remove(){
  removeIndicator();
  if(scrollGuide!=null){scrollGuide.setAlpha(guideAlpha);scrollGuide=null;}
  if(root!=null){try{WindowManager.LayoutParams p=(WindowManager.LayoutParams)root.getLayoutParams();p.x=originalX;((WindowManager)root.getContext().getSystemService(Context.WINDOW_SERVICE)).updateViewLayout(root,p);}catch(RuntimeException ignored){}}
  if(root!=null&&handle!=null)root.removeView(handle);
  if(first!=null)first.setAlpha(firstAlpha);if(second!=null)second.setAlpha(secondAlpha);
  root=null;handle=null;first=null;second=null;
 }
 private final class Handle extends View {
  private final Paint paint=new Paint(3);private float x,y;private int pointer=-1;
  Handle(Context c){super(c);setContentDescription("오른쪽 창 열기 또는 닫기");setClickable(true);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);paint.setColor(0xffb5b8ba);float cx=getWidth()*0.88f,top=getHeight()*0.12f,bottom=getHeight()*0.88f;c.drawRoundRect(new RectF(cx-3,top,cx+3,bottom),3,3,paint);}
  @Override public boolean onTouchEvent(MotionEvent e){
   switch(e.getActionMasked()){
    case MotionEvent.ACTION_DOWN:pointer=e.getPointerId(0);x=e.getX();y=e.getY();return true;
    case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_CANCEL:pointer=-1;return true;
    case MotionEvent.ACTION_MOVE:return true;
    case MotionEvent.ACTION_UP:
     if(pointer==e.getPointerId(0)&&mod!=null&&mod.enabled()){
      float dx=e.getX()-x,dy=e.getY()-y;int state=Settings.System.getInt(getContext().getContentResolver(),"split_screen_status",-1);
      boolean swipe=Math.abs(dx)>=24&&Math.abs(dx)>Math.abs(dy)*1.5f&&((state==0&&dx<0)||(state==1&&dx>0));
      if(swipe)stockPress();
     }pointer=-1;return true;
    default:return true;
   }
  }
  private void stockPress(){
   View target=first!=null&&first.getVisibility()==View.VISIBLE?first:second!=null&&second.getVisibility()==View.VISIBLE?second:null;
   if(target==null||!target.isShown())return;
   long t=SystemClock.uptimeMillis();float px=target.getWidth()/2f,py=target.getHeight()/2f;
   MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,px,py,0),up=MotionEvent.obtain(t,t+30,MotionEvent.ACTION_UP,px,py,0);
   try{target.dispatchTouchEvent(down);target.dispatchTouchEvent(up);}finally{down.recycle();up.recycle();}
  }
 }
}
