package dev.ev6.nowplaying;
import hmgapi.hook.*;
import android.content.Intent;
import android.os.Binder;
import java.lang.reflect.*;
import java.io.*;
import java.util.concurrent.*;
import android.graphics.*;

/* Copy only authenticated stock radio image bytes to the navigation process. */
final class RadioArtBridge {
 private static final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static volatile long generation;
 private static int lastMode=-1;private static float lastFrequency=Float.NaN;private static String lastPath;private static long lastLogoRequest;
 private static synchronized long trackState(int mode,float freq,String path){if(lastMode!=mode||Float.compare(lastFrequency,freq)!=0||lastPath==null||!lastPath.equals(path)){lastMode=mode;lastFrequency=freq;lastPath=path;generation++;lastLogoRequest=0;}return generation;}
 private static synchronized boolean scheduleLogo(){long now=android.os.SystemClock.elapsedRealtime();if(lastLogoRequest!=0&&now-lastLogoRequest<1000)return false;lastLogoRequest=now;return true;}
 static void install(final ModContext mod,final LoadedApp app){try{
  final Class<?> status=Class.forName("com.hkmc.splitaidl.RadioSplitStatus",false,app.classLoader);
  Class<?> receiver=Class.forName("com.mobis.splitscreen.right.k.w",false,app.classLoader);
  Method method=receiver.getDeclaredMethod("updateRadioSplitStatus",status);method.setAccessible(true);
  final Field art=status.getDeclaredField("mChannelArt"),frequency=status.getDeclaredField("mCurrentFrequency"),channel=status.getDeclaredField("mChannelName");art.setAccessible(true);frequency.setAccessible(true);channel.setAccessible(true);
  Bridge.hookMethod(method,new MethodHook(){protected void beforeHookedMethod(Param p){try{
   Diagnostic.set("radio_caller",Binder.getCallingUid()==1000?"system":"other");
   if(!mod.enabled()||Binder.getCallingUid()!=1000||p.args==null||p.args.length!=1||!status.isInstance(p.args[0]))return;
   final int current=AndroidRealMode.read(app.classLoader);if(current!=2&&current!=3&&current!=4)return;
   Object value=art.get(p.args[0]);Diagnostic.set("radio_art_source",value instanceof String&&((String)value).length()>0?"present":"absent");final String path=value instanceof String?(String)value:"";
   final float freq=frequency.getFloat(p.args[0]);if(path.length()>1024||Float.isNaN(freq)||Float.isInfinite(freq))return;
   Object label=channel.get(p.args[0]);final String name=label instanceof String?(String)label:"";if(name.length()>256)return;
   final long stamp=trackState(current,freq,path+"\u0000"+name);
   if(path.length()==0){if(scheduleLogo())worker.execute(new Runnable(){public void run(){loadSquareLogo(mod,app,current,freq,stamp,name);}});return;}
   worker.execute(new Runnable(){public void run(){try{
    if(!mod.enabled())return;File file=new File(path).getCanonicalFile();long size=file.length();if(!file.isFile()||size<16||size>524288)return;
    ByteArrayOutputStream out=new ByteArrayOutputStream((int)size);InputStream in=new FileInputStream(file);
    try{byte[] buffer=new byte[8192];int n,total=0;while((n=in.read(buffer))>0){total+=n;if(total>524288)return;out.write(buffer,0,n);}}finally{in.close();}
    byte[] data=out.toByteArray();Diagnostic.set("radio_file","read");boolean png=data.length>8&&(data[0]&255)==0x89&&data[1]=='P'&&data[2]=='N'&&data[3]=='G';
    boolean jpeg=data.length>3&&(data[0]&255)==0xff&&(data[1]&255)==0xd8&&(data[2]&255)==0xff;
    boolean webp=data.length>12&&data[0]=='R'&&data[1]=='I'&&data[2]=='F'&&data[3]=='F'&&data[8]=='W'&&data[9]=='E'&&data[10]=='B'&&data[11]=='P';
    if(!png&&!jpeg&&!webp){Diagnostic.set("radio_format","unsupported");return;}Diagnostic.set("radio_format","supported");
    int mode=AndroidRealMode.read(app.classLoader);if(mode!=current||generation!=stamp)return;
    Intent intent=new Intent("dev.ev6.nowplaying.RADIO_ART");intent.setPackage("com.mnsoft.navi");intent.putExtra("frequency",freq);intent.putExtra("mode",mode);intent.putExtra("image",data);app.context.sendBroadcast(intent);Diagnostic.set("radio_forward","sent");
   }catch(Exception ignored){}catch(OutOfMemoryError ignored){}catch(LinkageError ignored){}}});
  }catch(Exception ignored){}catch(LinkageError ignored){}}});
 }catch(Exception e){mod.log("radio image bridge unavailable");}catch(LinkageError e){mod.log("radio image bridge unavailable");}}
 private static void loadSquareLogo(final ModContext mod,final LoadedApp app,final int mode,final float freq,final long stamp,String name){
  Bitmap image=null;
  try{
   if(!mod.enabled()||generation!=stamp||AndroidRealMode.read(app.classLoader)!=mode)return;
   int bracket=name.indexOf('[');String key=(bracket>=0?name.substring(0,bracket):name).trim();if(key.length()==0)return;
   Class<?> logos=Class.forName("com.mobis.splitscreen.right.i.l",false,app.classLoader);Object catalog=logos.getMethod("dP").invoke(null);int id=((Integer)logos.getMethod("o",String.class).invoke(catalog,key)).intValue();
   if(id==0){Diagnostic.set("radio_square_logo","unavailable");return;}
   BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeResource(app.context.getResources(),id,bounds);if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>4096||bounds.outHeight>4096)return;
   BitmapFactory.Options opts=new BitmapFactory.Options();int sample=1;while(bounds.outWidth/sample>256||bounds.outHeight/sample>256)sample*=2;opts.inSampleSize=sample;image=BitmapFactory.decodeResource(app.context.getResources(),id,opts);if(image==null)return;
   ByteArrayOutputStream out=new ByteArrayOutputStream();if(!image.compress(Bitmap.CompressFormat.PNG,100,out))return;byte[] data=out.toByteArray();if(data.length==0||data.length>524288||generation!=stamp||AndroidRealMode.read(app.classLoader)!=mode)return;
   Intent i=new Intent("dev.ev6.nowplaying.RADIO_ART");i.setPackage("com.mnsoft.navi");i.putExtra("frequency",freq);i.putExtra("mode",mode);i.putExtra("image",data);app.context.sendBroadcast(i);Diagnostic.set("radio_square_logo","sent");
  }catch(Exception ignored){}catch(OutOfMemoryError ignored){}catch(LinkageError ignored){}finally{if(image!=null)image.recycle();}
 }
}
