package dev.ev6.edge;

import hmgapi.hook.LoadedApp;
import hmgapi.hook.ModContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.view.View;
import android.view.ViewGroup;

/** Reads the stock vertical widget page position; stock swipe handling is untouched. */
final class EdgeHandlePager {
 private final ModContext mod;
 private Field pageIndex; private Method stableIndex;
 EdgeHandlePager(ModContext mod){this.mod=mod;}
 void install(LoadedApp app){
  try{Class<?> type=Class.forName("com.mobis.splitscreen.right.SlidingView",false,app.classLoader);pageIndex=type.getDeclaredField("fx");pageIndex.setAccessible(true);Class<?> tag=Class.forName("com.mobis.splitscreen.right.F",false,app.classLoader);stableIndex=tag.getDeclaredMethod("getIndex");stableIndex.setAccessible(true);}
  catch(Exception e){mod.log("Widget page indicator unavailable");}catch(LinkageError e){mod.log("Widget page indicator unavailable");}
 }
 int[] currentPage(ViewGroup slider){
  try{if(pageIndex==null||stableIndex==null||slider==null)return null;int count=slider.getChildCount(),current=pageIndex.getInt(null);int[] ids=new int[count];
   for(int i=0;i<count;i++){View child=slider.getChildAt(i);Object tag=child==null?null:child.getTag(0x7f090000);ids[i]=tag!=null&&stableIndex.getDeclaringClass().isInstance(tag)?((Integer)stableIndex.invoke(tag)).intValue():-1;}
   return EdgeHandlePageMapping.resolve(ids,current);
  }catch(Exception e){return null;}catch(LinkageError e){return null;}
 }
}
