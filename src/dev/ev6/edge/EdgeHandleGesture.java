package dev.ev6.edge;

final class EdgeHandleGesture {
 static int direction(float dx,float dy,float threshold){
  if(Math.abs(dx)>threshold&&Math.abs(dx)>Math.abs(dy)*1.5f)return dx<0?1:-1;
  if(Math.abs(dy)>threshold&&Math.abs(dy)>Math.abs(dx)*1.5f)return 2;
  return 0;
 }
 static int target(int current,int count,int direction){
  int next=current+direction;
  return current>=0&&current<count&&(direction==1||direction==-1)&&next>=0&&next<count?next:-1;
 }
}
