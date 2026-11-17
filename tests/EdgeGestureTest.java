package dev.ev6.edge;
public final class EdgeGestureTest {
 private static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  check(EdgeHandleGesture.direction(-90,10,24)==1);
  check(EdgeHandleGesture.direction(90,10,24)==-1);
  check(EdgeHandleGesture.direction(8,-90,24)==2);
  check(EdgeHandleGesture.direction(30,28,24)==0);
  check(EdgeHandleGesture.target(1,3,1)==2);
  check(EdgeHandleGesture.target(0,3,-1)==-1);
  check(EdgeHandleGesture.target(2,3,1)==-1);
  System.out.println("PASS: widget swipe direction, diagonal rejection and page bounds");
 }
}
