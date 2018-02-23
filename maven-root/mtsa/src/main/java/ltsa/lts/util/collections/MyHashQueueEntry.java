package ltsa.lts.util.collections;


public class MyHashQueueEntry extends StateMapEntry {
	int action;
	int level = 0;
	MyHashQueueEntry next; // for linking buckets in hash table
	MyHashQueueEntry link; // for queue linked list
	MyHashQueueEntry parent; // pointer to node above in BFS

	MyHashQueueEntry(byte[] l) {
		stateNumber= -1;
		key = l;
		// action= 0;
		action = -1;
		next = null;
		link = null;
		marked = false;
	}

	MyHashQueueEntry(byte[] l, int a, MyHashQueueEntry p) {
		stateNumber= -1;
		key = l;
		action = a;
		next = null;
		link = null;
		parent = p;
		marked = false;
	}

}