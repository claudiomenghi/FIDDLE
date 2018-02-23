package ltsa.lts.util.collections;

public class MyHashProgEntry {
	public byte[] key;
	public int dfn; // depth first search number
	public int low; // low link number
	private boolean isReturn; // boolean
	public boolean isProcessed; // boolean
	public MyHashProgEntry next; // for linking buckets in hash table
	public MyHashProgEntry parent; // pointer to node above in BFS

	MyHashProgEntry(byte[] l) {
		key = l;
		dfn = 0;
		low = 0;
		isReturn = false;
		isProcessed = false;
		next = null;
		parent = null;
	}

	MyHashProgEntry(byte[] l, MyHashProgEntry p) {
		key = l;
		dfn = 0;
		low = 0;
		isReturn = false;
		isProcessed = false;
		next = null;
		parent = p;
	}
	
	public boolean getIsReturn(){
		return this.isReturn;
	}

	
	public void setIsReturn(boolean isReturn){
		this.isReturn=isReturn;
	}
}