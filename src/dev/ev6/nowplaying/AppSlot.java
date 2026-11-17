package dev.ev6.nowplaying;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
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
 private static final String PREFS="blackr_ccnc_theme", MODE="slot_mode", FAVORITE="favorite_package", POSITION="slot_position";
 private final SharedPreferences prefs;
 private final ImageView icon;
 private Runnable mediaAction;
 private Runnable placementChanged;
 private Boolean playing;
 private final Drawable mediaIcon=new android.graphics.drawable.Drawable(){
  private final Paint paint=new Paint(3);
  public void draw(Canvas c){android.graphics.Rect b=getBounds();float s=Math.min(b.width(),b.height()),x=b.exactCenterX(),y=b.exactCenterY();paint.setColor(isLight()?0xff394247:0xffe5eaed);if(playing==null){c.drawRect(x-s*.19f,y-s*.035f,x+s*.19f,y+s*.035f,paint);}else if(playing.booleanValue()){c.drawRoundRect(new android.graphics.RectF(x-s*.2f,y-s*.27f,x-s*.06f,y+s*.27f),s*.03f,s*.03f,paint);c.drawRoundRect(new android.graphics.RectF(x+s*.06f,y-s*.27f,x+s*.2f,y+s*.27f),s*.03f,s*.03f,paint);}else{android.graphics.Path p=new android.graphics.Path();p.moveTo(x-s*.16f,y-s*.28f);p.lineTo(x+s*.27f,y);p.lineTo(x-s*.16f,y+s*.28f);p.close();c.drawPath(p,paint);}}
  public void setAlpha(int a){} public void setColorFilter(android.graphics.ColorFilter f){} public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 };
 private final Drawable gridIcon=new android.graphics.drawable.Drawable(){
  private final Paint paint=new Paint(3);
  public void draw(Canvas c){android.graphics.Rect b=getBounds();float s=Math.min(b.width(),b.height()),r=s*.075f,cx=b.exactCenterX(),cy=b.exactCenterY();paint.setColor(isLight()?0xff394247:0xffe5eaed);for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)c.drawCircle(cx+x*s*.23f,cy+y*s*.23f,r,paint);}
  public void setAlpha(int alpha){} public void setColorFilter(android.graphics.ColorFilter f){} public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 };
 AppSlot(Context context){super(context);prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);icon=new ImageView(context);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);LayoutParams p=new LayoutParams(52,52,Gravity.CENTER);addView(icon,p);setOnClickListener(new OnClickListener(){public void onClick(View v){if("apps".equals(mode()))showApps(false);else if("favorite".equals(mode()))launch(prefs.getString(FAVORITE,""));else if("control".equals(mode())&&mediaAction!=null)mediaAction.run();}});refresh();}
 private boolean isLight(){return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES;}
 private String mode(){String m=prefs.getString(MODE,"none");return "apps".equals(m)||"favorite".equals(m)||"control".equals(m)?m:"none";}
 void setMediaAction(Runnable action){mediaAction=action;}
 void setPlacementChanged(Runnable action){placementChanged=action;}
 boolean onRight(){return "right".equals(prefs.getString(POSITION,"left"));}
 boolean isNone(){return "none".equals(mode());}
 void setPlaying(Boolean value){playing=value;if("control".equals(mode())){mediaIcon.invalidateSelf();icon.invalidate();}}
 void refresh(){String m=mode();BottomBarLayout.setCompact("none".equals(m));setVisibility("none".equals(m)?View.GONE:View.VISIBLE);setContentDescription("apps".equals(m)?"앱스":"control".equals(m)?"재생 또는 일시정지":"즐겨찾기 앱");Drawable d="control".equals(m)?mediaIcon:gridIcon;if("favorite".equals(m)){try{d=getContext().getPackageManager().getApplicationIcon(prefs.getString(FAVORITE,""));}catch(Exception ignored){}}icon.setImageDrawable(null);icon.setImageDrawable(d);invalidate();if(placementChanged!=null)placementChanged.run();}
 private Activity activity(){return getContext() instanceof Activity?(Activity)getContext():null;}
 void showSettings(){final Activity a=activity();if(a==null||a.isFinishing())return;final String m=mode();
  try{
   LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(32,12,32,12);
   TextView modeTitle=new TextView(a);modeTitle.setText("버튼 기능");content.addView(modeTitle);
   final android.widget.RadioGroup modes=new android.widget.RadioGroup(a);String[] labels={"None","Apps","Favorites App","Media Control"};for(int i=0;i<labels.length;i++){android.widget.RadioButton b=new android.widget.RadioButton(a);b.setId(100+i);b.setText(labels[i]);modes.addView(b);}content.addView(modes);
   TextView positionTitle=new TextView(a);positionTitle.setText("버튼 위치");content.addView(positionTitle);
   final android.widget.RadioGroup positions=new android.widget.RadioGroup(a);positions.setOrientation(LinearLayout.HORIZONTAL);for(int i=0;i<2;i++){android.widget.RadioButton b=new android.widget.RadioButton(a);b.setId(200+i);b.setText(i==0?"왼쪽":"오른쪽");b.setEnabled(!"none".equals(m));positions.addView(b);}content.addView(positions);
   modes.check("apps".equals(m)?101:"favorite".equals(m)?102:"control".equals(m)?103:100);positions.check(onRight()?201:200);
   final AlertDialog dialog=new AlertDialog.Builder(a).setTitle("미디어 창 설정").setView(content).setNegativeButton("닫기",null).create();
   modes.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener(){public void onCheckedChanged(android.widget.RadioGroup g,int id){dialog.dismiss();if(id==102){showApps(true);return;}prefs.edit().putString(MODE,id==101?"apps":id==103?"control":"none").apply();refresh();}});
   positions.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener(){public void onCheckedChanged(android.widget.RadioGroup g,int id){if("none".equals(mode()))return;prefs.edit().putString(POSITION,id==201?"right":"left").apply();refresh();}});
   dialog.show();
  }catch(RuntimeException ignored){}
 }
 private static final class Entry{final String pkg,label;final Drawable icon;Entry(String p,String l,Drawable i){pkg=p;label=l;icon=i;}}
 private List<Entry> apps(){PackageManager pm=getContext().getPackageManager();List<Entry> out=new ArrayList<Entry>();List<String> seen=new ArrayList<String>();Intent q=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
  try{for(ResolveInfo ri:pm.queryIntentActivities(q,0)){if(ri.activityInfo==null||ri.activityInfo.applicationInfo==null||!ri.activityInfo.exported||!ri.activityInfo.enabled||!ri.activityInfo.applicationInfo.enabled)continue;String p=ri.activityInfo.packageName;int flags=ri.activityInfo.applicationInfo.flags;if((flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0||seen.contains(p)||"com.nemesis.manager".equals(p))continue;Intent launch=pm.getLaunchIntentForPackage(p);if(launch==null)continue;seen.add(p);CharSequence label=ri.loadLabel(pm);Drawable icon=ri.loadIcon(pm);out.add(new Entry(p,label==null?p:label.toString(),icon));}}catch(RuntimeException ignored){}
  Collections.sort(out,new Comparator<Entry>(){public int compare(Entry a,Entry b){return a.label.compareToIgnoreCase(b.label);}});return out;
 }
 private void showApps(final boolean choose){final Activity a=activity();if(a==null||a.isFinishing())return;final List<Entry> list=apps();if(list.isEmpty()){Toast.makeText(a,"실행 가능한 앱이 없습니다",Toast.LENGTH_SHORT).show();return;}
  final GridView grid=new GridView(a);grid.setNumColumns(4);grid.setGravity(Gravity.CENTER);grid.setPadding(16,12,16,12);grid.setAdapter(new BaseAdapter(){public int getCount(){return list.size();}public Object getItem(int n){return list.get(n);}public long getItemId(int n){return n;}public View getView(int n,View old,ViewGroup parent){Entry e=list.get(n);LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setPadding(8,12,8,12);ImageView image=new ImageView(a);image.setImageDrawable(e.icon);box.addView(image,new LinearLayout.LayoutParams(64,64));TextView text=new TextView(a);text.setGravity(Gravity.CENTER);text.setText(e.label);text.setMaxLines(2);box.addView(text,new LinearLayout.LayoutParams(-1,-2));return box;}});
  try{final AlertDialog dialog=new AlertDialog.Builder(a).setTitle(choose?"즐겨찾기 앱 선택":"Apps").setView(grid).setNegativeButton("닫기",null).create();grid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener(){public void onItemClick(android.widget.AdapterView<?> p,View v,int n,long id){Entry e=list.get(n);dialog.dismiss();if(choose){prefs.edit().putString(FAVORITE,e.pkg).putString(MODE,"favorite").apply();refresh();}else launch(e.pkg);}});dialog.show();}catch(RuntimeException ignored){}
 }
 private void launch(String pkg){try{Intent i=getContext().getPackageManager().getLaunchIntentForPackage(pkg);if(i==null)throw new IllegalStateException();i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);getContext().startActivity(i);}catch(RuntimeException e){Toast.makeText(getContext(),"앱을 열 수 없습니다. 설정에서 다시 선택해 주세요.",Toast.LENGTH_SHORT).show();}}
}
