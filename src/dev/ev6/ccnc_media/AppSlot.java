package dev.ev6.ccnc_media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Optional shortcut, using the same user-launchable-app criteria as NEMESIS home. */
final class AppSlot extends android.widget.FrameLayout {
 private static final String SHORTCUTS="dev.blackr.ccnc.shortcuts";
 private static final String PREFS="blackr_ccnc_theme", MODE="slot_mode", FAVORITE="favorite_package", POSITION="slot_position";
 private static final String SWIPE_PREFS="blackr_ccnc_swipe", SWIPE_MODE="mode", SWIPE_FAVORITE="favorite";
 private final SharedPreferences prefs;
 private final ImageView icon;
 private final Paint borderPaint=new Paint(3);
 private Runnable mediaAction;
 private Runnable placementChanged;
 private Boolean playing;
 private final Drawable mediaIcon=new android.graphics.drawable.Drawable(){
  private final Paint paint=new Paint(3);
  public void draw(Canvas c){android.graphics.Rect b=getBounds();float s=Math.min(b.width(),b.height()),x=b.exactCenterX(),y=b.exactCenterY();paint.setColor(isLight()?0xff30383b:0xffe5eaed);if(playing==null){c.drawRect(x-s*.19f,y-s*.035f,x+s*.19f,y+s*.035f,paint);}else if(playing.booleanValue()){c.drawRoundRect(new android.graphics.RectF(x-s*.2f,y-s*.27f,x-s*.06f,y+s*.27f),s*.03f,s*.03f,paint);c.drawRoundRect(new android.graphics.RectF(x+s*.06f,y-s*.27f,x+s*.2f,y+s*.27f),s*.03f,s*.03f,paint);}else{android.graphics.Path p=new android.graphics.Path();p.moveTo(x-s*.16f,y-s*.28f);p.lineTo(x+s*.27f,y);p.lineTo(x-s*.16f,y+s*.28f);p.close();c.drawPath(p,paint);}}
  public void setAlpha(int a){} public void setColorFilter(android.graphics.ColorFilter f){} public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 };
 private final Drawable gridIcon=new android.graphics.drawable.Drawable(){
  private final Paint paint=new Paint(3);
  public void draw(Canvas c){android.graphics.Rect b=getBounds();float s=Math.min(b.width(),b.height()),r=s*.075f,cx=b.exactCenterX(),cy=b.exactCenterY();paint.setColor(isLight()?0xff30383b:0xffe5eaed);for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)c.drawCircle(cx+x*s*.23f,cy+y*s*.23f,r,paint);}
  public void setAlpha(int alpha){} public void setColorFilter(android.graphics.ColorFilter f){} public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 };
 private final Drawable warmerIcon=new android.graphics.drawable.Drawable(){
  private final Paint paint=new Paint(3);
  public void draw(Canvas c){android.graphics.Rect b=getBounds();float s=Math.min(b.width(),b.height()),x=b.exactCenterX(),y=b.exactCenterY();paint.setColor(isLight()?0xff30383b:0xffe5eaed);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(s*.065f);c.drawRoundRect(new android.graphics.RectF(x-s*.25f,y-s*.12f,x+s*.25f,y+s*.29f),s*.09f,s*.09f,paint);for(int n=-1;n<=1;n++)c.drawLine(x+n*s*.16f,y-s*.42f,x+n*s*.16f,y-s*.20f,paint);paint.setStyle(Paint.Style.FILL);}
  public void setAlpha(int alpha){} public void setColorFilter(android.graphics.ColorFilter f){} public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 };
 AppSlot(Context context){super(context);setWillNotDraw(false);prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);icon=new ImageView(context);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);LayoutParams p=new LayoutParams(52,52,Gravity.CENTER);addView(icon,p);setOnTouchListener(new OnTouchListener(){public boolean onTouch(View v,MotionEvent e){int a=e.getActionMasked();if(a==MotionEvent.ACTION_DOWN){v.animate().cancel();v.animate().scaleX(.94f).scaleY(.94f).setDuration(80).start();}else if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){v.animate().cancel();v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();}return false;}});setOnClickListener(new OnClickListener(){public void onClick(View v){if("apps".equals(mode()))showApps(false);else if("favorite".equals(mode()))launch(prefs.getString(FAVORITE,""));else if("control".equals(mode())&&mediaAction!=null)mediaAction.run();else if("warm".equals(mode()))WarmerSwipe.open(activity());}});refresh();}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(getWidth()<2||getHeight()<2)return;borderPaint.setStyle(Paint.Style.STROKE);borderPaint.setStrokeWidth(1f);borderPaint.setColor(isLight()?0xffa0a9ad:0xff849096);float x=onRight()?.5f:getWidth()-.5f;canvas.drawLine(x,0,x,getHeight(),borderPaint);}
 private boolean isLight(){return ThemeMode.isLight(getResources().getConfiguration().uiMode);}
 private String mode(){String m=prefs.getString(MODE,"none");return "apps".equals(m)||"favorite".equals(m)||"control".equals(m)||"warm".equals(m)?m:"none";}
 void setMediaAction(Runnable action){mediaAction=action;}
 void setPlacementChanged(Runnable action){placementChanged=action;}
 boolean onRight(){return "right".equals(prefs.getString(POSITION,"left"));}
 boolean isNone(){return "none".equals(mode());}
 void setPlaying(Boolean value){playing=value;if("control".equals(mode())){mediaIcon.invalidateSelf();icon.invalidate();}}
 void refresh(){String m=mode();BottomBarLayout.setCompact("none".equals(m));setVisibility("none".equals(m)?View.GONE:View.VISIBLE);setContentDescription("apps".equals(m)?"앱스":"control".equals(m)?"재생 또는 일시정지":"warm".equals(m)?"워머":"즐겨찾기 앱");Drawable d="control".equals(m)?mediaIcon:"warm".equals(m)?warmerIcon:gridIcon;if("favorite".equals(m)){try{String saved=prefs.getString(FAVORITE,""); android.content.ComponentName component=android.content.ComponentName.unflattenFromString(saved);d=component!=null?getContext().getPackageManager().getActivityIcon(component):getContext().getPackageManager().getApplicationIcon(saved);}catch(Exception ignored){}}icon.setImageDrawable(null);icon.setImageDrawable(d);invalidate();if(placementChanged!=null)placementChanged.run();}
 private Activity activity(){return getContext() instanceof Activity?(Activity)getContext():null;}
 private AlertDialog.Builder dialogBuilder(Activity a){return new AlertDialog.Builder(a,isLight()?AlertDialog.THEME_HOLO_LIGHT:AlertDialog.THEME_HOLO_DARK);}
 private int dialogTextColor(){return isLight()?0xff30383b:0xffe5eaed;}
 private ColorStateList radioTextColors(){int normal=dialogTextColor(),disabled=isLight()?0xff92999d:0xff80898d;return new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{disabled,normal});}
 @SuppressWarnings("deprecation") static SharedPreferences swipePrefs(Context c){try{Context nav=c.createPackageContext("com.mnsoft.navi",0);return nav.getSharedPreferences(SWIPE_PREFS,Context.MODE_WORLD_READABLE|Context.MODE_MULTI_PROCESS);}catch(Exception e){return null;}}
 static void runSwipe(Activity a){if(a==null||a.isFinishing())return;SharedPreferences p=swipePrefs(a);String m=p==null?"warm":p.getString(SWIPE_MODE,"warm");if("none".equals(m))return;if("warm".equals(m)){WarmerSwipe.open(a);return;}if("control".equals(m)){new StockBtControlBridge().togglePlayback();return;}AppSlot slot=new AppSlot(a);if("apps".equals(m))slot.showApps(false);else if("favorite".equals(m))slot.launch(p==null?"":p.getString(SWIPE_FAVORITE,""));}
 void showSettings(){final Activity a=activity();if(a==null||a.isFinishing())return;final String m=mode();
  try{
   AlertDialog.Builder builder=dialogBuilder(a);Context themed=builder.getContext();
   LinearLayout content=new LinearLayout(themed);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(32,12,32,12);
   TextView modeTitle=new TextView(themed);modeTitle.setTextColor(dialogTextColor());modeTitle.setText("버튼 기능");content.addView(modeTitle);
   final android.widget.RadioGroup modes=new android.widget.RadioGroup(themed);String[] labels={"None","Apps","Favorites App","Media Control","WARMER"};for(int i=0;i<labels.length;i++){android.widget.RadioButton b=new android.widget.RadioButton(themed);b.setTextColor(radioTextColors());b.setId(100+i);b.setText(labels[i]);modes.addView(b);}content.addView(modes);
   TextView positionTitle=new TextView(themed);positionTitle.setTextColor(dialogTextColor());positionTitle.setText("버튼 위치");content.addView(positionTitle);
   final android.widget.RadioGroup positions=new android.widget.RadioGroup(themed);positions.setOrientation(LinearLayout.HORIZONTAL);for(int i=0;i<2;i++){android.widget.RadioButton b=new android.widget.RadioButton(themed);b.setTextColor(radioTextColors());b.setId(200+i);b.setText(i==0?"왼쪽":"오른쪽");b.setEnabled(!"none".equals(m));positions.addView(b);}content.addView(positions);
   final SharedPreferences swipe=swipePrefs(a);TextView swipeTitle=new TextView(themed);swipeTitle.setTextColor(dialogTextColor());swipeTitle.setText("하단 중앙 스와이프");content.addView(swipeTitle);
   final android.widget.RadioGroup gestures=new android.widget.RadioGroup(themed);for(int i=0;i<labels.length;i++){android.widget.RadioButton b=new android.widget.RadioButton(themed);b.setTextColor(radioTextColors());b.setId(300+i);b.setText(labels[i]);gestures.addView(b);}content.addView(gestures);
   String sm=swipe==null?"warm":swipe.getString(SWIPE_MODE,"warm");gestures.check("apps".equals(sm)?301:"favorite".equals(sm)?302:"control".equals(sm)?303:"warm".equals(sm)?304:300);
   modes.check("apps".equals(m)?101:"favorite".equals(m)?102:"control".equals(m)?103:"warm".equals(m)?104:100);positions.check(onRight()?201:200);
   android.widget.ScrollView scroll=new android.widget.ScrollView(themed);scroll.addView(content);
   final AlertDialog dialog=builder.setTitle("미디어 창 설정").setView(scroll).setNegativeButton("닫기",null).create();
   modes.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener(){public void onCheckedChanged(android.widget.RadioGroup g,int id){dialog.dismiss();if(id==102){showApps(true);return;}prefs.edit().putString(MODE,id==101?"apps":id==103?"control":id==104?"warm":"none").apply();refresh();}});
   gestures.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener(){public void onCheckedChanged(android.widget.RadioGroup g,int id){if(swipe==null)return;dialog.dismiss();if(id==302){showApps(true,true);return;}swipe.edit().putString(SWIPE_MODE,id==301?"apps":id==303?"control":id==304?"warm":"none").commit();}});
   positions.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener(){public void onCheckedChanged(android.widget.RadioGroup g,int id){if("none".equals(mode()))return;prefs.edit().putString(POSITION,id==201?"right":"left").apply();refresh();}});
   dialog.show();
  }catch(RuntimeException ignored){}
 }
 private static final class Entry{final String pkg,label;final Drawable icon;Entry(String p,String l,Drawable i){pkg=p;label=l;icon=i;}}
 private List<Entry> apps(){PackageManager pm=getContext().getPackageManager();List<Entry> out=new ArrayList<Entry>();List<String> seen=new ArrayList<String>();Intent q=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
  try{addDirect(out,pm,"com.nemesis.manager","com.nemesis.manager.MainActivity","NEMESIS");addDirect(out,pm,"hmgapi.app","hmgapi.app.MainActivity","HMG-API");for(ResolveInfo ri:pm.queryIntentActivities(q,0)){if(ri.activityInfo==null||ri.activityInfo.applicationInfo==null||!ri.activityInfo.exported||!ri.activityInfo.enabled||!ri.activityInfo.applicationInfo.enabled)continue;String p=ri.activityInfo.packageName;int flags=ri.activityInfo.applicationInfo.flags;if((flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0||seen.contains(p)||SHORTCUTS.equals(p)||"com.nemesis.manager".equals(p)||"hmgapi.app".equals(p))continue;Intent launch=pm.getLaunchIntentForPackage(p);if(launch==null)continue;seen.add(p);CharSequence label=ri.loadLabel(pm);Drawable icon=ri.loadIcon(pm);String key=SHORTCUTS.equals(p)?new android.content.ComponentName(p,ri.activityInfo.name).flattenToString():p;out.add(new Entry(key,label==null?p:label.toString(),icon));}}catch(RuntimeException ignored){}
  Collections.sort(out,new Comparator<Entry>(){public int compare(Entry a,Entry b){return a.label.compareToIgnoreCase(b.label);}});return out;
 }
 private void addDirect(List<Entry> out,PackageManager pm,String pkg,String cls,String label){try{android.content.ComponentName c=new android.content.ComponentName(pkg,cls);android.content.pm.ActivityInfo info=pm.getActivityInfo(c,0);if(!info.enabled||!info.exported)return;out.add(new Entry(c.flattenToString(),label,info.loadIcon(pm)));}catch(PackageManager.NameNotFoundException ignored){}catch(RuntimeException ignored){}}
 private void showApps(final boolean choose){showApps(choose,false);}
 private void showApps(final boolean choose,final boolean forSwipe){final Activity a=activity();if(a==null||a.isFinishing())return;final List<Entry> list=apps();if(list.isEmpty()){Toast.makeText(a,"실행 가능한 앱이 없습니다",Toast.LENGTH_SHORT).show();return;}
  AlertDialog.Builder builder=dialogBuilder(a);final Context themed=builder.getContext();
  final GridView grid=new GridView(themed);grid.setNumColumns(4);grid.setGravity(Gravity.CENTER);grid.setPadding(16,12,16,12);grid.setAdapter(new BaseAdapter(){public int getCount(){return list.size();}public Object getItem(int n){return list.get(n);}public long getItemId(int n){return n;}public View getView(int n,View old,ViewGroup parent){Entry e=list.get(n);LinearLayout box=new LinearLayout(themed);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setPadding(8,12,8,12);ImageView image=new ImageView(themed);image.setImageDrawable(e.icon);box.addView(image,new LinearLayout.LayoutParams(64,64));TextView text=new TextView(themed);text.setTextColor(dialogTextColor());text.setGravity(Gravity.CENTER);text.setText(e.label);text.setMaxLines(2);box.addView(text,new LinearLayout.LayoutParams(-1,-2));return box;}});
  try{final AlertDialog dialog=builder.setTitle(choose?"즐겨찾기 앱 선택":"Apps").setView(grid).setNegativeButton("닫기",null).create();grid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener(){public void onItemClick(android.widget.AdapterView<?> p,View v,int n,long id){Entry e=list.get(n);dialog.dismiss();if(choose&&forSwipe){SharedPreferences sp=swipePrefs(a);if(sp!=null)sp.edit().putString(SWIPE_FAVORITE,e.pkg).putString(SWIPE_MODE,"favorite").commit();}else if(choose){prefs.edit().putString(FAVORITE,e.pkg).putString(MODE,"favorite").apply();refresh();}else launch(e.pkg);}});dialog.show();}catch(RuntimeException ignored){}
 }
 private void launch(String pkg){try{android.content.ComponentName c=android.content.ComponentName.unflattenFromString(pkg);Intent i=c!=null?new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(c):getContext().getPackageManager().getLaunchIntentForPackage(pkg);if(i==null)throw new IllegalStateException();i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);getContext().startActivity(i);}catch(RuntimeException e){Toast.makeText(getContext(),"앱을 열 수 없습니다. 설정에서 다시 선택해 주세요.",Toast.LENGTH_SHORT).show();}}
}
