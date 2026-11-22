package dev.ev6.ccnc_media;
final class MarqueeTiming {
 static final long START_PAUSE_MS=3000L,END_PAUSE_MS=3000L;
 static long travelMs(float overflow,float speed){return Math.max(1L,(long)Math.ceil(1000d*overflow/speed));}
 static float offset(long elapsed,float overflow,float speed){
  if(overflow<=0||speed<=0)return 0;
  long travel=travelMs(overflow,speed),phase=Math.max(0L,elapsed)%(START_PAUSE_MS+travel+END_PAUSE_MS);
  if(phase<START_PAUSE_MS)return 0;
  phase-=START_PAUSE_MS;
  return phase>=travel?overflow:overflow*phase/travel;
 }
}
