package dev.ev6.ccnc_media;
public final class ArrivalInfoPolicyTest {
 public static void main(String[] args){
  if(!ArrivalInfoPolicy.shouldShow(true,true,Boolean.TRUE,true))throw new AssertionError("known map viewport should allow arrival data");
  if(ArrivalInfoPolicy.shouldShow(true,true,Boolean.TRUE,false))throw new AssertionError("unknown viewport must hide");
  if(ArrivalInfoPolicy.shouldShow(true,false,Boolean.TRUE,true))throw new AssertionError("non-map screen must hide");
  if(ArrivalInfoPolicy.shouldShow(true,true,null,true)||ArrivalInfoPolicy.shouldShow(true,true,Boolean.FALSE,true))throw new AssertionError("unconfirmed guidance must hide");
  System.out.println("PASS: arrival information remains eligible with a known split or full map");
 }
}
