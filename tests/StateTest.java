package dev.ev6.ccnc_media;
import java.text.SimpleDateFormat;import java.util.*;
public final class StateTest {
 public static void main(String[] args) {
  long now=System.currentTimeMillis();String stamp=new SimpleDateFormat("MM-dd HH:mm:ss.SSS",Locale.US).format(new Date(now));
  String prefix=stamp+" 2133 2644 I ExSL : [2644][mBool CMapControl::isOverviewW: 55973] [MC] checkOverviewWhileStop session(Front) activity(Mainview), whileStop(1), isGuide(1), isOnRoute(1), isShowTBT(";
  GuidanceProxy.set(true);NavScrollState.guidance(prefix+"1), tripVisible(0)",2133,now-1000,now);if(!Boolean.TRUE.equals(GuidanceProxy.expanded()))throw new AssertionError("open");
  NavScrollState.guidance(prefix+"0), tripVisible(0)",2133,now-1000,now);if(!Boolean.FALSE.equals(GuidanceProxy.expanded()))throw new AssertionError("closed");
  NavScrollState.guidance(prefix+"1), tripVisible(0)",9999,now-1000,now);if(!Boolean.FALSE.equals(GuidanceProxy.expanded()))throw new AssertionError("wrong PID");
  String scene=stamp+" 2133 2644 I ExSL : [2644][mBool SessionInteraction::upda: 1209] top scene updated: screen(1), updateScene(Map_Overview), prevScene(Search_PrevDestination), curScene(Map_Overview), getForegroundStatus(1)";
  if(!Boolean.TRUE.equals(NavScrollState.sceneBlocked(scene,2133,now-1000,now)))throw new AssertionError("overview hidden");
  if(!Boolean.FALSE.equals(NavScrollState.sceneBlocked(scene.replace("curScene(Map_Overview)","curScene(Map_Normal)"),2133,now-1000,now)))throw new AssertionError("normal visible");
  if(NavScrollState.sceneBlocked(scene,9999,now-1000,now)!=null)throw new AssertionError("scene wrong pid");
  String search=scene.replace("curScene(Map_Overview)","curScene(Search_Places)");
  if(!"Search_Places".equals(NavScrollState.sceneName(search,2133,now-1000,now)))throw new AssertionError("search scene");
  if(NavScrollState.sceneName(search,9999,now-1000,now)!=null)throw new AssertionError("search wrong pid");
  if(NavScrollState.sceneName(search,2133,now+1000,now)!=null)throw new AssertionError("stale search");
  String state=prefix+"0), tripVisible(0), isOffRoute(1), other(0)";
  NavScrollState.guidance(state,2133,now-1000,now);if(GuidanceProxy.displayable())throw new AssertionError("off route hides button");
  NavScrollState.guidance(state.replace("isOffRoute(1)","isOffRoute(0)"),2133,now-1000,now);if(!GuidanceProxy.displayable())throw new AssertionError("on route enables button");
  NavScrollState.guidance(state,9999,now-1000,now);if(!GuidanceProxy.displayable())throw new AssertionError("off route wrong pid");
  GuidanceProxy.set(null);if(GuidanceProxy.displayable())throw new AssertionError("unknown visibility");if(GuidanceProxy.expanded()!=null)throw new AssertionError("unknown");System.out.println("PASS: observed open, closed, wrong process and unknown state");
 }
}
