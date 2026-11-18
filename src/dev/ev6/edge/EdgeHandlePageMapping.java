package dev.ev6.edge;
import java.util.Arrays;
/** Maps rotating view positions to the stable order assigned by the stock panel. */
final class EdgeHandlePageMapping {
 static int[] resolve(int[] pageIds,int current) {
  if(pageIds==null||current<0||current>=pageIds.length||pageIds[current]<0)return null;
  int[] sorted=pageIds.clone();Arrays.sort(sorted);int count=0,selected=-1,last=-1;
  for(int id:sorted){if(id<0||id==last)continue;if(id==pageIds[current])selected=count;count++;last=id;}
  return selected<0?null:new int[]{selected,count};
 }
}
