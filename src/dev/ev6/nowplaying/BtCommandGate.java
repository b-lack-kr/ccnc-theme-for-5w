package dev.ev6.nowplaying;

/* JADX INFO: loaded from: classes.dex */
public final class BtCommandGate {
    private final ModeReader modes;
    private final Target target;

    public interface ModeReader {
        int currentMode();
    }

    public enum Result {
        INVALID_COMMAND,
        MODE_UNAVAILABLE,
        NOT_BLUETOOTH,
        BRIDGE_UNAVAILABLE,
        DISPATCHED
    }

    public interface Target {
        boolean invoke(StockBtControlBridge.Command command);
    }

    public BtCommandGate(ModeReader modeReader, Target target) {
        if (modeReader == null || target == null) {
            throw new IllegalArgumentException();
        }
        this.modes = modeReader;
        this.target = target;
    }

    public Result dispatchResult(StockBtControlBridge.Command command) {
        if (command == null) {
            return Result.INVALID_COMMAND;
        }
        try {
            return this.target.invoke(command) ? Result.DISPATCHED : Result.BRIDGE_UNAVAILABLE;
        } catch (LinkageError e) {
            return Result.BRIDGE_UNAVAILABLE;
        } catch (RuntimeException e) {
            return Result.BRIDGE_UNAVAILABLE;
        }
    }

    public boolean dispatch(StockBtControlBridge.Command command) {
        return dispatchResult(command) == Result.DISPATCHED;
    }
}
