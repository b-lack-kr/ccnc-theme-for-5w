package dev.ev6.ccnc_media;
/** Eligibility remains independent of whether the right-hand panel is open. */
final class ArrivalInfoPolicy {
 static boolean shouldShow(boolean wanted,boolean navigationAllowed,Boolean guidance,boolean viewportKnown){return wanted&&navigationAllowed&&Boolean.TRUE.equals(guidance)&&viewportKnown;}
 private ArrivalInfoPolicy(){}
}
