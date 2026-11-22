package dev.ev6.ccnc_media;
public final class WarmerSwipeMathTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  check(WarmerSwipeMath.matches(960,719,970,640,300,1920,720));
  check(!WarmerSwipeMath.matches(960,719,1200,640,300,1920,720));
  check(!WarmerSwipeMath.matches(960,600,960,680,300,1920,720));
  check(!WarmerSwipeMath.matches(100,650,100,300,300,1920,720));
  check(!WarmerSwipeMath.matches(960,719,960,640,1600,1920,720));
  check(WarmerSwipeMath.matches(960,719,960,300,300,1920,720));
  check(!WarmerSwipeMath.matches(960,500,960,280,300,1920,720));
  check(!WarmerSwipeMath.matches(960,650,960,500,300,1920,720));
  check(!WarmerSwipeMath.matches(960,719,960,650,300,1920,720));
  check(!WarmerSwipeMath.matches(960,680,960,590,300,1920,720));
  check(!WarmerSwipeMath.matches(100,719,100,640,300,1920,720));
  System.out.println("PASS: deliberate bottom-centre upward warmer swipe");
 }
}
