package dev.ev6.ccnc_media;
public final class MarqueeTimingTest {
 public static void main(String[] args){float d=90,speed=30;
  check(MarqueeTiming.offset(0,d,speed)==0);
  check(MarqueeTiming.offset(2999,d,speed)==0);
  check(MarqueeTiming.offset(3000,d,speed)==0);
  check(MarqueeTiming.offset(4500,d,speed)==45);
  check(MarqueeTiming.offset(6000,d,speed)==90);
  check(MarqueeTiming.offset(8999,d,speed)==90);
  check(MarqueeTiming.offset(9000,d,speed)==0);
  check(MarqueeTiming.offset(1000,0,speed)==0);
  System.out.println("PASS: three-second start and end pauses, scrolling and immediate reset");
 }
 private static void check(boolean value){if(!value)throw new AssertionError();}
}
