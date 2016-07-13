package ltsa.lts.util.collections;

public abstract class StateMapEntry {
    public byte[] key;
    public int stateNumber;
    public boolean marked;
    public int depth;
}
