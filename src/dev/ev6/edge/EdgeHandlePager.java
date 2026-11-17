package dev.ev6.edge;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageView;
import hmgapi.hook.Bridge;
import hmgapi.hook.LoadedApp;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Uses the stock page selection and presents the transition horizontally. */
final class EdgeHandlePager {
 private final ModContext mod;
 private Field pageIndex;
 private Method setPage;
 private float downX,downY;
 private int pointer=-1;
 private boolean horizontal,vertical,animating;
 EdgeHandlePager(ModContext mod){this.mod=mod;}
 void install(LoadedApp app){
  try{
   Class<?> type=Class.forName("com.mobis.splitscreen.right.SlidingView",false,app.classLoader);
   pageIndex=type.getDeclaredField("fx");pageIndex.setAccessible(true);
   setPage=type.getDeclaredMethod("s",Integer.TYPE);setPage.setAccessible(true);
   Method intercept=type.getDeclaredMethod("onInterceptTouchEvent",MotionEvent.class);
   Method touch=type.getDeclaredMethod("onTouchEvent",MotionEvent.class);
   Bridge.hookMethod(intercept,new MethodHook(){protected void beforeHookedMethod(Param p){handleIntercept(p);}});
   Bridge.hookMethod(touch,new MethodHook(){protected void beforeHookedMethod(Param p){handleTouch(p);}});
  }catch(Exception e){mod.log("Widget horizontal pager unavailable");}catch(LinkageError e){mod.log("Widget horizontal pager unavailable");}
 }
 private MotionEvent event(MethodHook.Param p){return p.args!=null&&p.args.length==1&&p.args[0] instanceof MotionEvent?(MotionEvent)p.args[0]:null;}
 private void begin(MotionEvent e){pointer=e.getPointerId(0);downX=e.getX();downY=e.getY();horizontal=false;vertical=false;}
 private void classify(View v,MotionEvent e){if(pointer<0||e.findPointerIndex(pointer)<0)return;int i=e.findPointerIndex(pointer);float dx=e.getX(i)-downX,dy=e.getY(i)-downY;float limit=Math.max(24,ViewConfiguration.get(v.getContext()).getScaledTouchSlop()*2);if(!horizontal&&!vertical){int d=EdgeHandleGesture.direction(dx,dy,limit);if(d==1||d==-1)horizontal=true;else if(d==2)vertical=true;}}
 private void handleIntercept(MethodHook.Param p){
  if(!mod.enabled()||!(p.thisObject instanceof View))return;MotionEvent e=event(p);if(e==null)return;int a=e.getActionMasked();
  if(a==MotionEvent.ACTION_DOWN){if(animating){p.setResult(true);return;}begin(e);}
  else if(a==MotionEvent.ACTION_MOVE){classify((View)p.thisObject,e);if(horizontal)p.setResult(true);else if(vertical)p.setResult(false);}
  else if(a==MotionEvent.ACTION_POINTER_DOWN||a==MotionEvent.ACTION_CANCEL){pointer=-1;horizontal=false;vertical=false;}
 }
 private void handleTouch(MethodHook.Param p){
  if(!mod.enabled()||!(p.thisObject instanceof ViewGroup))return;MotionEvent e=event(p);if(e==null)return;int a=e.getActionMasked();ViewGroup v=(ViewGroup)p.thisObject;
  if(animating){p.setResult(true);return;}
  if(a==MotionEvent.ACTION_DOWN){if(pointer<0)begin(e);return;}
  if(a==MotionEvent.ACTION_MOVE){classify(v,e);if(horizontal||vertical)p.setResult(true);return;}
  if(a==MotionEvent.ACTION_UP){if(horizontal&&pointer==e.getPointerId(0)){float dx=e.getX()-downX;changePage(v,dx<0?1:-1);p.setResult(true);}else if(vertical)p.setResult(true);pointer=-1;horizontal=false;vertical=false;}
  else if(a==MotionEvent.ACTION_CANCEL||a==MotionEvent.ACTION_POINTER_DOWN){pointer=-1;horizontal=false;vertical=false;}
 }
 private void changePage(final ViewGroup pager,int direction){
  if(pageIndex==null||setPage==null||pager.getWidth()<=0||pager.getHeight()<=0)return;
  try{
   int current=pageIndex.getInt(null),next=EdgeHandleGesture.target(current,pager.getChildCount(),direction);
   if(next<0)return;
   final ViewGroup parent=pager.getParent() instanceof ViewGroup?(ViewGroup)pager.getParent():null;
   Bitmap image=null;try{image=Bitmap.createBitmap(pager.getWidth(),pager.getHeight(),Bitmap.Config.ARGB_8888);pager.draw(new Canvas(image));}catch(RuntimeException ignored){}catch(OutOfMemoryError ignored){}
   setPage.invoke(pager,Integer.valueOf(next));
   if(parent==null||image==null){if(image!=null)image.recycle();return;}
   final Bitmap snapshot=image;final ImageView old=new ImageView(pager.getContext());old.setImageBitmap(snapshot);old.setScaleType(ImageView.ScaleType.FIT_XY);old.setClickable(false);
   FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(pager.getWidth(),pager.getHeight());params.leftMargin=pager.getLeft();params.topMargin=pager.getTop();
   parent.addView(old,params);animating=true;float width=pager.getWidth();pager.setTranslationX(direction*width);
   pager.animate().translationX(0).setDuration(240).start();
   old.animate().translationX(-direction*width).setDuration(240).setListener(new AnimatorListenerAdapter(){public void onAnimationEnd(Animator a){finish();}public void onAnimationCancel(Animator a){finish();}private void finish(){old.animate().setListener(null);if(old.getParent()==parent)parent.removeView(old);old.setImageDrawable(null);if(!snapshot.isRecycled())snapshot.recycle();pager.setTranslationX(0);animating=false;}}).start();
  }catch(Exception e){pager.setTranslationX(0);animating=false;}catch(LinkageError e){pager.setTranslationX(0);animating=false;}
 }
}
