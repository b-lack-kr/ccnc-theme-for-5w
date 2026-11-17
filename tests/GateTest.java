import dev.ev6.nowplaying.BtCommandGate;
import dev.ev6.nowplaying.StockBtControlBridge;
public final class GateTest {
 public static void main(String[] args) {
  final int[] calls={0};
  BtCommandGate g=new BtCommandGate(new BtCommandGate.ModeReader(){public int currentMode(){throw new AssertionError("mode should not be queried");}},new BtCommandGate.Target(){public boolean invoke(StockBtControlBridge.Command c){calls[0]++;return true;}});
  if(!g.dispatch(StockBtControlBridge.Command.PREVIOUS)||!g.dispatch(StockBtControlBridge.Command.NEXT)||g.dispatch(null)||calls[0]!=2) throw new AssertionError();
  System.out.println("PASS: previous/next forwarded without source checks; null rejected");
 }
}
