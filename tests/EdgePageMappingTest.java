package dev.ev6.edge;
public final class EdgePageMappingTest {
 private static void check(int[] ids,int current,int expected,int count){int[] r=EdgeHandlePageMapping.resolve(ids,current);if(r==null||r[0]!=expected||r[1]!=count)throw new AssertionError();}
 public static void main(String[] args){check(new int[]{0,1,2},0,0,3);check(new int[]{2,0,1},0,2,3);check(new int[]{2,0,1},1,0,3);check(new int[]{5,8,5,2},0,1,3);if(EdgeHandlePageMapping.resolve(new int[]{-1},0)!=null)throw new AssertionError();System.out.println("PASS: rotating widget order and duplicated pages");}
}
