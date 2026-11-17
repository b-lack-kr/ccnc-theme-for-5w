package dev.ev6.nowplaying;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/* A dismissible notice while the active navigation view hides its media bar. */
final class TransientNotice {
 private final View owner;private final Handler ui;private String previousTitle,previousArtist;
 private WindowManager windows;private FrameLayout root;private LinearLayout panel;private TextView title,artist;private ImageView artworkSource,artwork;private boolean added;private boolean light;
 private String pendingTitle,pendingArtist;
 private final Runnable present=new Runnable(){public void run(){show();}};
 private final Runnable scopeCheck=new Runnable(){public void run(){Context c=owner.getContext();if(!added||!(c instanceof Activity)||owner.isShown()||!((Activity)c).hasWindowFocus()){remove();return;}ui.postDelayed(this,150);}};
 private final Runnable hide=new Runnable(){public void run(){if(panel!=null&&added)panel.animate().setListener(null).cancel();if(panel!=null&&added)panel.animate().translationY(85).setDuration(350).setListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){remove();}}).start();}};
 TransientNotice(View owner,Handler ui){this.owner=owner;this.ui=ui;}
 void setArtworkSource(ImageView source){artworkSource=source;}
 void artworkChanged(){
  if(artwork==null||artworkSource==null)return;try{Drawable original=artworkSource.getDrawable(),copy=null;
   if(original instanceof BitmapDrawable){android.graphics.Bitmap bitmap=((BitmapDrawable)original).getBitmap();if(bitmap!=null&&!bitmap.isRecycled())copy=new BitmapDrawable(artwork.getResources(),bitmap);}
   else if(original!=null&&original.getConstantState()!=null)copy=original.getConstantState().newDrawable(artwork.getResources());
   artwork.setImageDrawable(copy);
  }catch(RuntimeException ignored){}catch(LinkageError ignored){}
 }
 void update(String nextTitle,String nextArtist,boolean lightTheme){
  if(nextTitle==null||nextTitle.trim().length()==0)return;
  boolean initialized=previousTitle!=null;
  boolean changed=initialized&&(!nextTitle.equals(previousTitle)||!same(nextArtist,previousArtist));
  previousTitle=nextTitle;previousArtist=nextArtist;
  if(!changed||owner.isShown())return;
  pendingTitle=nextTitle;pendingArtist=nextArtist;light=lightTheme;ui.removeCallbacks(present);ui.postDelayed(present,250);
 }
 void reset(){previousTitle=null;previousArtist=null;pendingTitle=null;pendingArtist=null;ui.removeCallbacks(present);ui.removeCallbacks(hide);remove();}
 void ownerShown(){ui.removeCallbacks(present);remove();}
 private static boolean same(Object a,Object b){return a==b||(a!=null&&a.equals(b));}
 private void show(){
  Context context=owner.getContext();if(!(context instanceof Activity)||owner.isShown())return;Activity activity=(Activity)context;
  if(!activity.hasWindowFocus()||activity.getResources().getDisplayMetrics().densityDpi!=240)return;
  View content=activity.findViewById(android.R.id.content);if(content==null||content.getHeight()!=720||(content.getWidth()!=1920&&content.getWidth()!=1280))return;
  int right=content.getWidth();try{if(Settings.System.getInt(activity.getContentResolver(),"split_screen_status",-1)==1)right=1280;}catch(RuntimeException ignored){}
  if(right!=1280&&right!=1920)return;remove();
  try{
   windows=(WindowManager)activity.getSystemService(Context.WINDOW_SERVICE);if(windows==null)return;
   root=new FrameLayout(activity);root.setClipChildren(true);
   root.setClickable(true);root.setOnTouchListener(new View.OnTouchListener(){public boolean onTouch(View v,MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN){v.setVisibility(View.INVISIBLE);ui.post(new Runnable(){public void run(){remove();}});}return true;}});
   panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.HORIZONTAL);panel.setGravity(Gravity.CENTER_VERTICAL);panel.setBackgroundColor(light?0xffe4e6e8:0xff333537);
   artwork=new ImageView(activity);artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);artwork.setBackgroundColor(light?0xffccd0d3:0xff3b404b);panel.addView(artwork,new LinearLayout.LayoutParams(85,-1));artworkChanged();
   LinearLayout details=new LinearLayout(activity);details.setOrientation(LinearLayout.VERTICAL);details.setGravity(Gravity.CENTER_VERTICAL);details.setPadding(9,0,9,0);panel.addView(details,new LinearLayout.LayoutParams(0,-1,1));
   title=text(activity,19,false,light?0xff30383b:0xffffffff);artist=text(activity,16,false,light?0xff5c6568:0xffe2e7ec);
   title.setText(pendingTitle);artist.setText(pendingArtist==null?"":pendingArtist);details.addView(title,new LinearLayout.LayoutParams(-1,-2));details.addView(artist,new LinearLayout.LayoutParams(-1,-2));
   root.addView(panel,new FrameLayout.LayoutParams(-1,-1));
   WindowManager.LayoutParams p=new WindowManager.LayoutParams(515,85,WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
   p.gravity=Gravity.TOP|Gravity.LEFT;p.x=right-519;p.y=635;p.setTitle("EV6 Media Notice");panel.setTranslationY(85);windows.addView(root,p);added=true;
   final LinearLayout animatedPanel=panel;ui.removeCallbacks(hide);
   ui.removeCallbacks(scopeCheck);ui.postDelayed(scopeCheck,150);
   panel.animate().translationY(0).setDuration(350).setListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(added&&panel==animatedPanel)ui.postDelayed(hide,3000);}}).start();
  }catch(RuntimeException ignored){remove();}catch(LinkageError ignored){remove();}
 }
 private static TextView text(Context context,int size,boolean bold,int color){TextView v=new TextView(context);v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.create("devicedefault",bold?Typeface.BOLD:Typeface.NORMAL));v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.MARQUEE);v.setMarqueeRepeatLimit(-1);v.setSelected(true);return v;}
 private void remove(){ui.removeCallbacks(hide);ui.removeCallbacks(scopeCheck);LinearLayout oldPanel=panel;FrameLayout oldRoot=root;WindowManager manager=windows;boolean wasAdded=added;panel=null;root=null;title=null;artist=null;artwork=null;added=false;windows=null;if(oldPanel!=null)oldPanel.animate().setListener(null).cancel();if(oldRoot!=null&&wasAdded&&manager!=null)try{manager.removeViewImmediate(oldRoot);}catch(RuntimeException ignored){}}
}
