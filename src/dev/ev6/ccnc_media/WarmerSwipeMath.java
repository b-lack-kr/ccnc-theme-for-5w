package dev.ev6.ccnc_media;

/** Identifies a deliberate upward swipe beginning at the lower centre of the display. */
final class WarmerSwipeMath {
 static boolean matches(float startX,float startY,float endX,float endY,long duration,int width,int height){
  if(width<=0||height<=0||duration<40||duration>1200)return false;
  float up=startY-endY,side=Math.abs(endX-startX);
  return startX>=width*.40f&&startX<=width*.60f&&startY>=height*.95f&&startY<height
      &&endY>=0&&up>=height*.10f&&side<=up*.60f;
 }
 private WarmerSwipeMath(){}
}
