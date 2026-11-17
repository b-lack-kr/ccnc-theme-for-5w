package dev.ev6.nowplaying;
import java.io.BufferedReader;import java.io.InputStreamReader;
final class NavScrollMonitor {
 private volatile boolean closed; private volatile java.lang.Process process; private final Sink sink;
 interface Sink {void changed(Boolean b);void guidance(Boolean b);}
 NavScrollMonitor(Sink sink){this.sink=sink;}
 void start(){Thread t=new Thread(new Runnable(){public void run(){
  long earliest=System.currentTimeMillis()-1000; Boolean scene=null,scroll=null; int pid=android.os.Process.myPid();
  try {
   if(closed)return;
   java.lang.Process p=new ProcessBuilder("/system/bin/logcat","-v","threadtime","-s","ExSL:V","*:S").redirectErrorStream(true).start();process=p;
   if(closed){p.destroy();return;}
   BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream(),"UTF-8"));String line;
   while(!closed&&(line=r.readLine())!=null){
    long now=System.currentTimeMillis();Boolean sceneEvent=NavScrollState.sceneBlocked(line,pid,earliest,now);Boolean scrollEvent=NavScrollState.parse(line,pid,earliest,now);Boolean guide=NavScrollState.guidance(line,pid,earliest,now);
    if(sceneEvent!=null)scene=sceneEvent;
    if(scrollEvent!=null)scroll=scrollEvent;
    if(sceneEvent!=null)Diagnostic.set("scene",sceneEvent.booleanValue()?"blocked":"normal");
    if(scrollEvent!=null)Diagnostic.set("scroll",scrollEvent.booleanValue()?"active":"inactive");
    if(scene==null && guide!=null && line.contains("checkOverviewWhileStop session(Front) activity(Mainview)"))scene=Boolean.FALSE;
    if(!closed&&(sceneEvent!=null||scrollEvent!=null||guide!=null)) {
      if(Boolean.TRUE.equals(scene))sink.changed(Boolean.TRUE);else if(Boolean.FALSE.equals(scene))sink.changed(scroll);else sink.changed(null);
      if(guide!=null)sink.guidance(guide);
    }
   }
 } catch(Exception ignored){Diagnostic.set("monitor","error");} catch(LinkageError ignored){Diagnostic.set("monitor","error");} finally {java.lang.Process p=process;process=null;if(p!=null)p.destroy();if(!closed){sink.changed(null);sink.guidance(null);}}
 }},"ev6-scroll-monitor");t.setDaemon(true);t.start();}
 void close(){closed=true;final java.lang.Process p=process;if(p!=null){Thread t=new Thread(new Runnable(){public void run(){p.destroy();}},"ev6-scroll-close");t.setDaemon(true);t.start();}}
}
