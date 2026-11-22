package dev.ev6.ccnc_media;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.security.MessageDigest;

/** Read only the validated 5W navigation build; unavailable or changing data is rejected. */
final class ArrivalInfoReader {
 private static final String LIB="/data/data/com.mnsoft.navi/BIN/libExSLNavi.so";
 private static final String HASH="de162d7015504906105037ae306d1514655791bc04d5e8a5caed10ff476b73b3";
 private static boolean checked,valid;
 private static boolean supported(){
  if(checked)return valid;checked=true;
  try{MessageDigest d=MessageDigest.getInstance("SHA-256");FileInputStream in=new FileInputStream(LIB);try{byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}finally{in.close();}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(java.util.Locale.US,"%02x",b&255));valid=HASH.equals(s.toString());}catch(Exception ignored){}return valid;
 }
 private static long word(RandomAccessFile f,long addr)throws Exception{if(addr<65536||addr>0xffffffffL)throw new java.io.IOException();f.seek(addr);int a=f.read(),b=f.read(),c=f.read(),d=f.read();if((a|b|c|d)<0)throw new java.io.IOException();return ((long)a|((long)b<<8)|((long)c<<16)|((long)d<<24))&0xffffffffL;}
 private static long base()throws Exception{BufferedReader r=new BufferedReader(new FileReader("/proc/self/maps"));try{String s;while((s=r.readLine())!=null){if(!s.contains(LIB))continue;String[] parts=s.trim().split("\\s+");if(parts.length>=6&&"00000000".equals(parts[2]))return Long.parseLong(parts[0].split("-")[0],16);}}finally{r.close();}return 0;}
 private static long service(RandomAccessFile f,long node,int depth)throws Exception{if(node==0||depth>16)return 0;long key=word(f,node+16);if(key==16)return word(f,node+20);long x=service(f,word(f,node+8),depth+1);return x!=0?x:service(f,word(f,node+12),depth+1);}
 static int[] read(){
  if(!supported())return null;
  try{long base=base();if(base==0)return null;RandomAccessFile f=new RandomAccessFile("/proc/self/mem","r");try{
   long map=base+0x45e68d0L,svc=service(f,word(f,map+8),0);if(svc==0)return null;long ctrl=word(f,svc+112);if(ctrl==0)return null;long data=word(f,ctrl+20);if(data==0)return null;
   long vector=data+8+3184*2+3168,start=word(f,vector),end=word(f,vector+4),size=end-start;if(start==0||size<=0||size>3144L*16||size%3144!=0)return null;
   int destinationCount=0;long soc=-1;for(long p=start;p<end;p+=3144){if((word(f,p)&0xffffL)==0){destinationCount++;soc=word(f,p+512);}}if(destinationCount!=1||soc>100)return null;
   long list=base+0x463a5a4L,begin=word(f,list),finish=word(f,list+4);if(begin==0||finish-begin<=0||finish-begin>64||(finish-begin)%4!=0)return null;
   int minutes=-1,destTimes=0;for(long p=begin;p<finish;p+=4){long obj=word(f,p);if(word(f,obj)==4){destTimes++;long value=word(f,obj+4);if(value<=10080)minutes=(int)value;}}
   if(destTimes!=1||minutes<0||word(f,vector)!=start||word(f,vector+4)!=end||word(f,list)!=begin||word(f,list+4)!=finish)return null;
   return new int[]{(int)soc,minutes};
  }finally{f.close();}}catch(Exception ignored){return null;}
 }
}
