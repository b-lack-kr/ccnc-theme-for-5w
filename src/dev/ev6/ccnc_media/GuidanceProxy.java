package dev.ev6.ccnc_media;
final class GuidanceProxy {
 private static volatile Boolean guidance, expanded, offRoute;
 private static volatile Runnable listener;
 static boolean active(){ return Boolean.TRUE.equals(guidance) && expanded != null; }
 static boolean displayable(){return active() && Boolean.FALSE.equals(offRoute);}
 static Boolean expanded(){return expanded;}
 static void set(Boolean value){guidance=value;if(!Boolean.TRUE.equals(value)){expanded=null;offRoute=null;}Runnable r=listener;if(r!=null)r.run();}
 static void setExpanded(Boolean value){Boolean old=expanded;if(old==value||(old!=null&&old.equals(value)))return;expanded=value;Runnable r=listener;if(r!=null)r.run();}
 static void setOffRoute(Boolean value){Boolean old=offRoute;if(old==value||(old!=null&&old.equals(value)))return;offRoute=value;Runnable r=listener;if(r!=null)r.run();}
 static void listen(Runnable value){listener=value;if(value!=null)value.run();}
 static void unlisten(Runnable value){if(listener==value)listener=null;}
}
