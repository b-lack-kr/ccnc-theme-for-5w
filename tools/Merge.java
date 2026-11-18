import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import java.io.File;
public class Merge {
 public static void main(String[] a) throws Exception {
  Opcodes op=Opcodes.forApi(19);
  DexFile old=DexFileFactory.loadDexFile(new File(a[0]),op), replacement=DexFileFactory.loadDexFile(new File(a[1]),op);
  DexPool pool=new DexPool(op);
  for(ClassDef c:old.getClasses()) if(!c.getType().startsWith("Ldev/ev6/nowplaying/OsdModePolicy") && !c.getType().startsWith("Ldev/ev6/nowplaying/PrototypeViewport") && !c.getType().startsWith("Ldev/ev6/nowplaying/BottomMediaView") && !c.getType().startsWith("Ldev/ev6/nowplaying/BtCommandGate") && !c.getType().startsWith("Ldev/ev6/nowplaying/StockBtControlBridge") && !c.getType().equals("Ldev/ev6/nowplaying/NowPlayingController;") && !c.getType().startsWith("Ldev/ev6/nowplaying/BottomBarLayout") && !c.getType().startsWith("Ldev/ev6/nowplaying/BottomBarSurface") && !c.getType().equals("Ldev/ev6/nowplaying/NavScrollState;") && !c.getType().startsWith("Ldev/ev6/nowplaying/NavScrollMonitor")) pool.internClass(c);
  for(ClassDef c:replacement.getClasses()) pool.internClass(c);
  pool.writeTo(new FileDataStore(new File(a[2])));
 }
}
