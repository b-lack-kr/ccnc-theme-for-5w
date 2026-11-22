package dev.ev6.ccnc_media;
public class LayoutTest {
 static void check(boolean condition){if(!condition)throw new AssertionError();}
 public static void main(String[] a) {
  GuidanceProxy.set(false);
  BottomBarLayout.setCompact(true);
  BottomBarLayout.Result compact=BottomBarLayout.resolve(1920,720,240,false,null);
  check(compact.row.left==1401 && compact.row.right==1916 && compact.row.right-compact.row.left==515);
  BottomBarLayout.setCompact(false);
  BottomBarLayout.Result normal=BottomBarLayout.resolve(1920,720,240,false,null);
  check(normal.row.left==1276 && normal.row.right==1916 && normal.row.top==635 && normal.row.bottom==720);
  check(normal.artwork.left==1276 && normal.row.right-normal.row.left-125==515);
  BarPlacement.Box reserved=BottomBarLayout.provisionalGuidanceButton(false);
  GuidanceProxy.set(null); BottomBarLayout.Result unknown=BottomBarLayout.resolve(1920,720,240,false,reserved); check(unknown.row.right<=reserved.left);
  GuidanceProxy.set(true); check(!GuidanceProxy.active());
  GuidanceProxy.setExpanded(false); check(GuidanceProxy.active()); BottomBarLayout.Result guide=BottomBarLayout.resolve(1920,720,240,false,reserved); check(guide.row.left==1276 && guide.row.right==1916 && guide.artwork.left==1276);
  BarPlacement.Box splitReserved=BottomBarLayout.provisionalGuidanceButton(true);
  BottomBarLayout.Result split=BottomBarLayout.resolve(1280,720,240,true,splitReserved);check(split.row.left==636&&split.row.right==1276);
  check(BottomBarLayout.resolve(1024,720,240,false,null)==null);
  System.out.println("PASS: idle, unknown, guidance, split and unsupported geometry");
 }
}
