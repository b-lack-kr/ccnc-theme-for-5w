package dev.ev6.nowplaying;

import android.app.Activity;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.os.Handler;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.Layout;
import android.text.StaticLayout;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/* JADX INFO: loaded from: classes.dex */
public final class BottomMediaView extends LinearLayout {
    private final TextView artist;
    private final ImageView artwork;
    private Controls controls;
    private final TransportIcon next;
    private final TransportIcon play;
    private Boolean playing;
    private final TransportIcon previous;
    private float progress;
    private boolean progressEnabled;
    private final Paint progressPaint;
    private final TextView title;
    private final Handler mediaHandler = new Handler();
    private final TransientNotice notice=new TransientNotice(this,mediaHandler);
    private Boolean lightTheme;
    private boolean projectionFallback;
    private boolean touchFeedback;
    private final Runnable clearTouchFeedback=new Runnable(){public void run(){touchFeedback=false;invalidate();}};
    private View guideButton;
    private final Runnable guideListener = new Runnable() { public void run() { mediaHandler.post(new Runnable() { public void run() { if (guideButton != null) { guideButton.setVisibility(View.INVISIBLE);Diagnostic.set("guide","hidden"); } } }); } };
    private int currentMode = -999;
    private String liveTitle, liveArtist;
    private long livePosition = -1, liveDuration = -1;
    private long artworkGeneration;
    private ExecutorService artExecutor;
    private boolean registered;
    private boolean artReceiverRegistered;
    private byte[] pendingGenieArt; private String pendingGenieTitle;
    private byte[] pendingRadioArt; private float pendingRadioFrequency=Float.NaN,liveRadioFrequency=Float.NaN;
    private final BroadcastReceiver genieArtReceiver = new BroadcastReceiver() { public void onReceive(Context c,Intent i) {
        if(i!=null && "dev.ev6.nowplaying.RADIO_ART".equals(i.getAction())){
            Diagnostic.set("radio_receive","called");
            if(currentMode!=2&&currentMode!=3&&currentMode!=4)return;
            try{byte[] image=i.getByteArrayExtra("image");float frequency=i.getFloatExtra("frequency",Float.NaN);if(i.getIntExtra("mode",-1)!=currentMode||image==null||image.length==0||image.length>524288||Float.isNaN(frequency))return;pendingRadioArt=image;pendingRadioFrequency=frequency;applyRadioArtwork();}catch(RuntimeException ignored){}return;
        }
        Diagnostic.set("art_receive", currentMode == 131 ? "genie" : "other_mode");
        if(currentMode!=131 || i==null || !"dev.ev6.nowplaying.GENIE_ART".equals(i.getAction()))return;
        try {String name=i.getStringExtra("title");byte[] image=i.getByteArrayExtra("image");if(name==null||name.length()>256||image==null||image.length==0||image.length>524288)return;pendingGenieTitle=name;pendingGenieArt=image;applyGenieArtwork();}catch(RuntimeException ignored){}
    }};
    private final Runnable mediaPoll = new Runnable() { public void run() {
        updateTheme();
        int mode = AndroidRealMode.read(getContext().getClassLoader());
        if (mode != currentMode) { currentMode = mode; clearLive(); requestStock(); }
        mediaHandler.postDelayed(this, 1000L);
    }};
    private final BroadcastReceiver mediaReceiver = new BroadcastReceiver() { public void onReceive(Context c, Intent intent) { receiveStock(intent); } };
    private float downX, downY;
    private long downTime;
    private boolean moved;
    private int pointer = -1;

    public interface Controls {
        void next();

        void playPause();

        void previous();
    }

    public BottomMediaView(Context context) {
        super(context);
        this.progressPaint = new Paint();
        this.progress = -1.0f;
        this.progressEnabled = true;
        setOrientation(0);
        setGravity(16);
        setPadding(0, 0, 0, 0);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(0xff333537);
        setBackground(gradientDrawable);
        guideButton = new View(context) {
            private final Paint pen = new Paint(3);
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas); float cx=getWidth()/2f, cy=getHeight()/2f;
                pen.setColor(Boolean.TRUE.equals(lightTheme) ? 0xff3b4549 : 0xffd3d9de); pen.setStrokeWidth(3f); pen.setStyle(Paint.Style.STROKE);
                canvas.drawRect(cx-39,cy-20,cx+39,cy+20,pen);
                canvas.drawLine(cx+5,cy-20,cx+5,cy+20,pen);
                canvas.drawLine(cx+10,cy-10,cx+32,cy-10,pen); canvas.drawLine(cx+10,cy,cx+32,cy,pen); canvas.drawLine(cx+10,cy+10,cx+32,cy+10,pen);
                pen.setStyle(Paint.Style.FILL); Boolean expanded=GuidanceProxy.expanded();
                if (expanded != null) { android.graphics.Path arrow=new android.graphics.Path();
                    if (expanded.booleanValue()) { arrow.moveTo(cx-29,cy-6); arrow.lineTo(cx-13,cy-6); arrow.lineTo(cx-13,cy-14); arrow.lineTo(cx+1,cy); arrow.lineTo(cx-13,cy+14); arrow.lineTo(cx-13,cy+6); arrow.lineTo(cx-29,cy+6); }
                    else { arrow.moveTo(cx+1,cy-6); arrow.lineTo(cx-15,cy-6); arrow.lineTo(cx-15,cy-14); arrow.lineTo(cx-29,cy); arrow.lineTo(cx-15,cy+14); arrow.lineTo(cx-15,cy+6); arrow.lineTo(cx+1,cy+6); }
                    arrow.close(); canvas.drawPath(arrow,pen);
                }
            }
        };
        guideButton.setContentDescription("길안내 목록 열기 또는 접기");
        guideButton.setVisibility(View.INVISIBLE);
        guideButton.setOnTouchListener(new View.OnTouchListener(){public boolean onTouch(View v,MotionEvent e){int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){v.animate().cancel();v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start();}else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){v.animate().cancel();v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();}return false;}});
        guideButton.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ toggleStockGuidance(); }});
        addView(guideButton,new LinearLayout.LayoutParams(125,-1));
        ImageView imageView = new ImageView(context);
        this.artwork = imageView;
        notice.setArtworkSource(imageView);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundColor(-12893877);
        imageView.setContentDescription("앨범아트");
        addView(imageView, new LinearLayout.LayoutParams(85, -1));
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(16);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -1, 1.0f);
        layoutParams.leftMargin = dp(6);
        layoutParams.rightMargin = dp(4);
        addView(linearLayout, layoutParams);
        TextView textViewText = text(19, -1, 1);
        this.title = textViewText;
        TextView textViewText2 = text(16, -1906708, 1);
        this.artist = textViewText2;
        linearLayout.addView(textViewText, new LinearLayout.LayoutParams(-1, -2));
        linearLayout.addView(textViewText2, new LinearLayout.LayoutParams(-1, -2));
        TransportIcon transportIconButton = button(0, "이전 곡");
        this.previous = transportIconButton;
        TransportIcon transportIconButton2 = button(4, "재생 또는 일시정지");
        this.play = transportIconButton2;
        TransportIcon transportIconButton3 = button(3, "다음 곡");
        this.next = transportIconButton3;
        transportIconButton.setOnClickListener(new View.OnClickListener() { // from class: dev.ev6.nowplaying.BottomMediaView.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (BottomMediaView.this.controls != null) {
                    BottomMediaView.this.controls.previous();
                }
            }
        });
        transportIconButton2.setOnClickListener(new View.OnClickListener() { // from class: dev.ev6.nowplaying.BottomMediaView.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (BottomMediaView.this.controls != null) {
                    BottomMediaView.this.controls.playPause();
                }
            }
        });
        transportIconButton3.setOnClickListener(new View.OnClickListener() { // from class: dev.ev6.nowplaying.BottomMediaView.3
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (BottomMediaView.this.controls != null) {
                    BottomMediaView.this.controls.next();
                }
            }
        });
        setMetadata(null, null, null);
        setControls(null);
        setPlaying(null);
        updateTheme();
    }

    private void updateTheme() {
        int mode=getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean light=mode != Configuration.UI_MODE_NIGHT_YES;
        if(lightTheme!=null && lightTheme.booleanValue()==light)return;
        lightTheme=Boolean.valueOf(light);
        Diagnostic.set("theme",light ? "light" : "dark");
        setBackgroundColor(light ? 0xffe4e6e8 : 0xff333537);
        title.setTextColor(light ? 0xff30383b : 0xffffffff);
        artist.setTextColor(light ? 0xff5c6568 : 0xffe2e7ec);
        artwork.setBackgroundColor(light ? 0xffccd0d3 : -12893877);
        if(guideButton!=null)guideButton.invalidate();
        if(projectionFallback)showArtwork(null);
        invalidate();
    }

    private void showArtwork(Bitmap bitmap) {
        if(bitmap!=null){projectionFallback=false;Diagnostic.set("projection_art","artwork");artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);artwork.setImageBitmap(bitmap);notice.artworkChanged();return;}
        projectionFallback=currentMode==115||currentMode==116;
        if(!projectionFallback){artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);artwork.setImageDrawable(null);notice.artworkChanged();return;}
        Drawable icon=null;String pkg=currentMode==115?"com.daudio.app.dipo":"com.google.android.projection.sink";
        try{android.content.pm.PackageManager pm=getContext().getPackageManager();android.content.pm.ApplicationInfo info=pm.getApplicationInfo(pkg,0);if(info.icon!=0)icon=pm.getApplicationIcon(info);}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}catch(RuntimeException ignored){}
        Diagnostic.set("projection_art",icon==null?(currentMode==115?"carplay_label":"auto_label"):(currentMode==115?"carplay_icon":"auto_icon"));
        if(icon==null)icon=new ProjectionFallback(currentMode==115,Boolean.TRUE.equals(lightTheme));
        artwork.setScaleType(ImageView.ScaleType.FIT_XY);
        artwork.setImageDrawable(icon);
        notice.artworkChanged();
    }

    private static final class ProjectionFallback extends Drawable {
        private final boolean carPlay,light;private final Paint paint=new Paint(3);
        ProjectionFallback(boolean carPlay,boolean light){this.carPlay=carPlay;this.light=light;}
        @Override public void draw(Canvas canvas){
            android.graphics.Rect r=getBounds();float s=Math.min(r.width(),r.height());float x=r.exactCenterX(),top=r.top+(r.height()-s)/2f;
            int foreground=light?0xff37454b:0xffeef4f6;paint.setColor(foreground);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(s*0.045f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
            if(carPlay){
                canvas.drawRoundRect(new android.graphics.RectF(x-s*0.27f,top+s*0.14f,x+s*0.27f,top+s*0.57f),s*0.08f,s*0.08f,paint);
                canvas.drawLine(x-s*0.20f,top+s*0.43f,x+s*0.20f,top+s*0.43f,paint);
                canvas.drawLine(x-s*0.13f,top+s*0.43f,x-s*0.09f,top+s*0.30f,paint);
                canvas.drawLine(x-s*0.09f,top+s*0.30f,x+s*0.09f,top+s*0.30f,paint);
                canvas.drawLine(x+s*0.09f,top+s*0.30f,x+s*0.13f,top+s*0.43f,paint);
                paint.setStyle(Paint.Style.FILL);canvas.drawCircle(x-s*0.12f,top+s*0.49f,s*0.025f,paint);canvas.drawCircle(x+s*0.12f,top+s*0.49f,s*0.025f,paint);
                paint.setTextSize(s*0.21f);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));canvas.drawText("CarPlay",x,top+s*0.83f,paint);
            }else{
                paint.setColor(0xff45a9ed);paint.setStrokeWidth(s*0.09f);canvas.drawLine(x-s*0.21f,top+s*0.53f,x,top+s*0.14f,paint);canvas.drawLine(x,top+s*0.14f,x+s*0.21f,top+s*0.53f,paint);
                paint.setColor(foreground);paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));paint.setTextSize(s*0.145f);canvas.drawText("Android Auto",x,top+s*0.82f,paint);
            }
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
        @Override public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);}
        @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
        @Override public int getIntrinsicWidth(){return 80;}
        @Override public int getIntrinsicHeight(){return 80;}
    }

    @Override protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateTheme();
    }

    private TextView text(int i, int i2, int i3) {
        TextView textView = new MarqueeTextView(getContext());
        textView.setTextSize(i);
        textView.setTextColor(i2);
        textView.setTypeface(Typeface.create("devicedefault", i3));
        textView.setSingleLine(true);
        textView.setEllipsize(null);
        return textView;
    }

    private static final class MarqueeTextView extends TextView {
        private String cachedText;
        private int cachedWidth=-1;
        private float cachedTextSize=-1;
        private Typeface cachedTypeface;
        private StaticLayout line;
        private float contentWidth;
        private long started;
        MarqueeTextView(Context context){super(context);}
        @Override protected void onDraw(Canvas canvas){
            String value=getText()==null?"":getText().toString().replace('\n',' ').replace('\r',' ');
            int width=getWidth();if(width<=0)return;
            if(line==null||!value.equals(cachedText)||cachedWidth!=width||cachedTextSize!=getTextSize()||cachedTypeface!=getTypeface()){
                cachedText=value;cachedWidth=width;cachedTextSize=getTextSize();cachedTypeface=getTypeface();
                contentWidth=Layout.getDesiredWidth(value,getPaint());
                int layoutWidth=Math.max(width,Math.max(1,(int)Math.ceil(contentWidth)));
                line=new StaticLayout(value,getPaint(),layoutWidth,Layout.Alignment.ALIGN_NORMAL,1f,0f,false);
                started=android.os.SystemClock.uptimeMillis();
            }
            getPaint().setColor(getCurrentTextColor());
            float overflow=Math.max(0f,contentWidth-width),offset=0f;
            if(overflow>0.5f){float speed=20f*getResources().getDisplayMetrics().density;long elapsed=android.os.SystemClock.uptimeMillis()-started;long travel=MarqueeTiming.travelMs(overflow,speed);long phase=elapsed%(MarqueeTiming.START_PAUSE_MS+travel+MarqueeTiming.END_PAUSE_MS);offset=MarqueeTiming.offset(elapsed,overflow,speed);boolean paused=phase<MarqueeTiming.START_PAUSE_MS||phase>=MarqueeTiming.START_PAUSE_MS+travel;if(isShown())postInvalidateDelayed(paused?100L:33L);}
            int save=canvas.save();canvas.clipRect(0,0,width,getHeight());canvas.translate(-offset,Math.max(0,(getHeight()-line.getHeight())/2f));line.draw(canvas);canvas.restoreToCount(save);
        }
    }

    private TransportIcon button(int i, String str) {
        TransportIcon transportIcon = new TransportIcon(getContext(), i);
        transportIcon.setContentDescription(str);

        return transportIcon;
    }

    public void setMetadata(String str, String str2, Bitmap bitmap) {
        if (currentMode != 54) return;
        TextView textView = this.title;
        if (str == null || str.trim().isEmpty()) {
            str = "재생 정보 없음";
        }
        textView.setText(str);
        TextView textView2 = this.artist;
        if (str2 == null) {
            str2 = "";
        }
        textView2.setText(str2);
        showArtwork(bitmap);
        notice.update(str,str2,Boolean.TRUE.equals(lightTheme));
    }

    public void setPlaying(Boolean bool) {
        int i;
        this.playing = bool;
        TransportIcon transportIcon = this.play;
        if (bool == null) {
            i = 4;
        } else {
            i = bool.booleanValue() ? 2 : 1;
        }
        transportIcon.setIcon(i);
    }

    public void setProgress(Long l, Long l2) {
        if (currentMode != 54) return;
        float f;
        if (l == null || l2 == null || l.longValue() < 0 || l2.longValue() <= 0 || l.longValue() > l2.longValue()) {
            f = -1.0f;
        } else {
            double dLongValue = l.longValue();
            double dLongValue2 = l2.longValue();
            Double.isNaN(dLongValue);
            Double.isNaN(dLongValue2);
            f = (float) (dLongValue / dLongValue2);
        }
        this.progress = f;
        invalidate();
    }

    public void setProgressEnabled(boolean z) {
        if (this.progressEnabled != z) {
            this.progressEnabled = z;
            invalidate();
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        float progressStart = guideButton != null ? guideButton.getRight() : 0f;
        if(this.progressEnabled && this.progress >= 0.0f){
            int iMax=Math.max(0,getHeight()-3);this.progressPaint.setColor(Boolean.TRUE.equals(lightTheme)?0x663f484b:1728053247);float f=iMax;
            canvas.drawRect(progressStart,f,getWidth(),getHeight(),this.progressPaint);this.progressPaint.setColor(-10887960);
            canvas.drawRect(progressStart,f,progressStart+(getWidth()-progressStart)*this.progress,getHeight(),this.progressPaint);
        }
        if(touchFeedback){this.progressPaint.setColor(Boolean.TRUE.equals(lightTheme)?0x26293438:0x40ffffff);canvas.drawRect(progressStart,0,getWidth(),getHeight(),this.progressPaint);}
    }

    public void setControls(Controls controls) {
        this.controls = controls;
        boolean z = controls != null;
        this.previous.setEnabled(z);
        this.play.setEnabled(z);
        this.next.setEnabled(z);
        float f = z ? 1.0f : 0.4f;
        this.previous.setAlpha(f);
        this.play.setAlpha(f);
        this.next.setAlpha(f);
    }


    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        GuidanceProxy.listen(guideListener);
        if (!registered) {
            IntentFilter f = new IntentFilter();
            f.addAction("com.hkmc.radio.widgetupdate");
            f.addAction("com.hkmc.audio.usb.widgetupdate");
            f.addAction("com.hkmc.dipoaudio.widgetupdate");
            f.addAction("com.hkmc.gpmaudio.widgetupdate");
            f.addAction("com.hkmc.genie_music.widgetupdate");
            try { getContext().registerReceiver(mediaReceiver, f); registered = true; } catch (RuntimeException ignored) {}
        }
        if (!artReceiverRegistered) { try { IntentFilter filter=new IntentFilter("dev.ev6.nowplaying.GENIE_ART");filter.addAction("dev.ev6.nowplaying.RADIO_ART");getContext().registerReceiver(genieArtReceiver,filter,"android.permission.DUMP",null);artReceiverRegistered=true; } catch(RuntimeException ignored){} }
        artExecutor = Executors.newSingleThreadExecutor();
        currentMode = -999; mediaHandler.post(mediaPoll);
    }
    @Override protected void onDetachedFromWindow() {
        GuidanceProxy.unlisten(guideListener);
        notice.reset();
        mediaHandler.removeCallbacks(mediaPoll);
        mediaHandler.removeCallbacks(clearTouchFeedback);touchFeedback=false;
        if (registered) { try { getContext().unregisterReceiver(mediaReceiver); } catch (RuntimeException ignored) {} registered = false; }
        if (artReceiverRegistered) { try { getContext().unregisterReceiver(genieArtReceiver); } catch (RuntimeException ignored) {} artReceiverRegistered=false; }
        pendingGenieArt=null;pendingGenieTitle=null;pendingRadioArt=null;
        artworkGeneration++;
        if (artExecutor != null) { artExecutor.shutdownNow(); artExecutor = null; }
        super.onDetachedFromWindow();
    }
    private void clearLive() {
        notice.reset();
        artworkGeneration++; pendingGenieArt=null; pendingGenieTitle=null; pendingRadioArt=null;liveRadioFrequency=Float.NaN;pendingRadioFrequency=Float.NaN;liveTitle = null; liveArtist = null; livePosition = -1; liveDuration = -1;
        showArtwork(null); progress = -1f; invalidate();
        if (currentMode != 54) { title.setText("재생 정보 없음"); artist.setText(""); }
    }
    private void requestStock() {
        String pkg = currentMode == 2 || currentMode == 3 || currentMode == 4 ? "com.hkmc.av.app.radioge" : currentMode == 54 ? "com.daudio.av.app.btaudio.ui" : currentMode == 49 ? "com.daudio.av.app.usbmusic" : currentMode == 115 ? "com.daudio.app.dipo" : currentMode == 116 ? "com.google.android.projection.sink" : currentMode == 131 ? "com.daudio.app.musicstreaming" : null;
        if (pkg == null) return;
        try { Intent request = new Intent("com.hkmc.request.widgetupdate"); request.setPackage(pkg); getContext().sendBroadcast(request); } catch (RuntimeException ignored) {}
    }
    private boolean accepts(String action) {
        return (currentMode == 2 || currentMode == 3 || currentMode == 4) && "com.hkmc.radio.widgetupdate".equals(action)
            || currentMode == 49 && "com.hkmc.audio.usb.widgetupdate".equals(action)
            || currentMode == 115 && "com.hkmc.dipoaudio.widgetupdate".equals(action)
            || currentMode == 116 && "com.hkmc.gpmaudio.widgetupdate".equals(action)
            || currentMode == 131 && "com.hkmc.genie_music.widgetupdate".equals(action);
    }
    private void receiveStock(Intent intent) {
        if (intent == null || !accepts(intent.getAction())) return;
        try {
            if (currentMode == 2 || currentMode == 3 || currentMode == 4) {
                if (intent.hasExtra("frq_total")) { float f = intent.getFloatExtra("frq_total", 0); if(Float.isNaN(liveRadioFrequency)||Math.abs(liveRadioFrequency-f)>0.01f){artworkGeneration++;artwork.setImageDrawable(null);}liveRadioFrequency=f; String band = intent.getStringExtra("radio_mode"); liveTitle = (band == null ? "FM" : band) + " " + String.format(Locale.US, "%.1f", f);applyRadioArtwork(); }
                if (intent.hasExtra("broad_name")) liveArtist = intent.getStringExtra("broad_name");
            } else {
                if (intent.hasExtra("filename")) {
                    String nextTitle = intent.getStringExtra("filename");
                    if (nextTitle == null || !nextTitle.equals(liveTitle)) { liveArtist = null; artworkGeneration++; showArtwork(null); livePosition = -1; liveDuration = -1; }
                    liveTitle = nextTitle;
                }
                if (intent.hasExtra("artist_name")) liveArtist = intent.getStringExtra("artist_name");
                if (intent.hasExtra("audio_current_time")) livePosition = number(intent, "audio_current_time");
                if (intent.hasExtra("audio_total_time")) liveDuration = number(intent, "audio_total_time");
                if (intent.hasExtra("artwork_raw_image")) {
                    final byte[] bytes = intent.getByteArrayExtra("artwork_raw_image"); final long generation = ++artworkGeneration;
                    if (bytes == null || bytes.length == 0 || bytes.length > 131072) showArtwork(null);
                    else if (artExecutor != null) artExecutor.execute(new Runnable(){ public void run(){
                        final BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true; BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
                        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth > 4096 || bounds.outHeight > 4096) return;
                        BitmapFactory.Options opts = new BitmapFactory.Options(); int sample=1; while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2; opts.inSampleSize=sample;
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
                        mediaHandler.post(new Runnable(){ public void run(){if (registered && generation == artworkGeneration && bitmap != null) showArtwork(bitmap);}});
                    }});
                }
            }
            applyGenieArtwork();
            title.setText(liveTitle == null || liveTitle.trim().isEmpty() ? "재생 정보 없음" : liveTitle);
            artist.setText(liveArtist == null ? "" : liveArtist);
            notice.update(liveTitle,liveArtist,Boolean.TRUE.equals(lightTheme));
            progress = liveDuration > 0 && livePosition >= 0 && livePosition <= liveDuration ? (float)((double)livePosition / liveDuration) : -1f; invalidate();
        } catch (RuntimeException ignored) {}
    }
    private void applyGenieArtwork() {
        Diagnostic.set("art_match", currentMode!=131 ? "other_mode" : pendingGenieArt==null ? "no_pending" : liveTitle==null ? "no_title" : !liveTitle.equals(pendingGenieTitle) ? "different_title" : artExecutor==null ? "no_executor" : "matched");
        if(currentMode!=131 || pendingGenieArt==null || liveTitle==null || !liveTitle.equals(pendingGenieTitle) || artExecutor==null)return;
        Diagnostic.set("art_processing", "matched");
        final byte[] data=pendingGenieArt;pendingGenieArt=null;pendingGenieTitle=null;final long generation=++artworkGeneration;
        artExecutor.execute(new Runnable(){public void run(){try { BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,bounds);if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>4096||bounds.outHeight>4096)return;BitmapFactory.Options opts=new BitmapFactory.Options();int sample=1;while(bounds.outWidth/sample>256||bounds.outHeight/sample>256)sample*=2;opts.inSampleSize=sample;final Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,opts);mediaHandler.post(new Runnable(){public void run(){if(registered&&currentMode==131&&generation==artworkGeneration&&bitmap!=null)showArtwork(bitmap);}}); }catch(RuntimeException ignored){}catch(OutOfMemoryError ignored){}}});
    }
    private void applyRadioArtwork(){
        if(!Float.isNaN(liveRadioFrequency)&&!Float.isNaN(pendingRadioFrequency)){float a=liveRadioFrequency,b=pendingRadioFrequency;Diagnostic.set("radio_scale",Math.abs(a-b)<0.01f?"same":Math.abs(a-b*10)<0.1f||Math.abs(b-a*10)<0.1f?"ten":Math.abs(a-b*100)<1||Math.abs(b-a*100)<1?"hundred":Math.abs(a-b*1000)<10||Math.abs(b-a*1000)<10?"thousand":"other");}
        Diagnostic.set("radio_match",pendingRadioArt==null?"no_pending":Float.isNaN(liveRadioFrequency)?"no_frequency":!sameRadioFrequency(liveRadioFrequency,pendingRadioFrequency)?"different_frequency":"matched");
        if((currentMode!=2&&currentMode!=3&&currentMode!=4)||pendingRadioArt==null||!sameRadioFrequency(liveRadioFrequency,pendingRadioFrequency)||artExecutor==null)return;
        final byte[] data=pendingRadioArt;pendingRadioArt=null;final long generation=++artworkGeneration;
        artExecutor.execute(new Runnable(){public void run(){try{BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,b);if(b.outWidth<1||b.outHeight<1||b.outWidth>4096||b.outHeight>4096)return;BitmapFactory.Options o=new BitmapFactory.Options();int sample=1;while(b.outWidth/sample>256||b.outHeight/sample>256)sample*=2;o.inSampleSize=sample;final Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,o);mediaHandler.post(new Runnable(){public void run(){if(registered&&(currentMode==2||currentMode==3||currentMode==4)&&generation==artworkGeneration&&bitmap!=null){showArtwork(bitmap);Diagnostic.set("radio_display","applied");}}});}catch(RuntimeException ignored){}catch(OutOfMemoryError ignored){}}});
    }
    private static boolean sameRadioFrequency(float a,float b){
        if(Float.isNaN(a)||Float.isNaN(b)||Float.isInfinite(a)||Float.isInfinite(b)||a<=0||b<=0)return false;
        if(Math.abs(a-b)<=0.02f)return true;
        return a>2000&&Math.abs(a/100f-b)<=0.02f||b>2000&&Math.abs(b/100f-a)<=0.02f;
    }
    private void toggleStockGuidance() {
        if (!GuidanceProxy.active() || !(getContext() instanceof Activity)) return;
        Activity a=(Activity)getContext(); View content=a.findViewById(android.R.id.content);
        if (!a.hasWindowFocus() || content==null || content.getHeight()!=720 || (content.getWidth()!=1920 && content.getWidth()!=1280)) return;
        int[] loc=new int[2]; getLocationOnScreen(loc); int right=loc[0]+getWidth()+4;
        if (right!=1920 && right!=1280) return;
        long t=android.os.SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,right-70,675,0);
        MotionEvent up=MotionEvent.obtain(t,t+60,MotionEvent.ACTION_UP,right-70,675,0);
        try { a.onTouchEvent(down); a.onTouchEvent(up); } finally {down.recycle();up.recycle();}
    }
    private static long number(Intent intent, String key) { Object v=intent.getExtras().get(key); return v instanceof Number ? ((Number)v).longValue() : -1; }

    @Override protected void onVisibilityChanged(View changedView,int visibility){super.onVisibilityChanged(changedView,visibility);if(notice!=null&&visibility==View.VISIBLE&&isShown())notice.ownerShown();}

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            mediaHandler.removeCallbacks(clearTouchFeedback);touchFeedback=true;invalidate();pointer = event.getPointerId(0); downX = event.getX(); downY = event.getY(); downTime = event.getEventTime(); moved = false; return true;
        case MotionEvent.ACTION_MOVE:
            if (Math.abs(event.getX() - downX) > ViewConfiguration.get(getContext()).getScaledTouchSlop() || Math.abs(event.getY() - downY) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) { moved = true;touchFeedback=false;invalidate(); }
            return true;
        case MotionEvent.ACTION_POINTER_DOWN: pointer = -1;touchFeedback=false;invalidate(); return true;
        case MotionEvent.ACTION_CANCEL: pointer = -1;touchFeedback=false;invalidate(); return true;
        case MotionEvent.ACTION_UP:
            if (pointer == event.getPointerId(0)) {
                float dx = event.getX() - downX, dy = event.getY() - downY;
                float threshold = Math.max(dp(48), ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2);
                if (Math.abs(dx) >= threshold && Math.abs(dx) > Math.abs(dy) * 1.5f && controls != null) {
                    if (dx < 0) controls.next(); else controls.previous();
                } else if (!moved && Math.abs(dx) <= ViewConfiguration.get(getContext()).getScaledTouchSlop() && Math.abs(dy) <= ViewConfiguration.get(getContext()).getScaledTouchSlop() && event.getEventTime() - downTime <= 350L && controls != null) {
                    controls.playPause();
                }
            }
            pointer = -1;if(touchFeedback)mediaHandler.postDelayed(clearTouchFeedback,120); return true;
        default: return true;
        }
    }

    private int dp(int i) {
        return (int) ((getResources().getDisplayMetrics().density * i) + 0.5f);
    }
}
