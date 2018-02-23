package ltsa.lts.util.collections;

/*
 * Simple low overhead Stack data type used by analyser
 */

class StackEntries {
	final static int N = 1024;
	byte[] val[] = new byte[N][];
	boolean[] marks = new boolean[N];
	int index;
	StackEntries next;

	StackEntries(StackEntries se) {
		index = 0;
		next = se;
	}

	boolean empty() {
		return index == 0;
	}

	boolean full() {
		return index == N;
	}

	void push(byte[] o) {
		val[index] = o;
		marks[index] = false;
		++index;
	}

	byte[] pop() {
		--index;
		return val[index];
	}

	byte[] peek() {
		return val[index - 1];
	}

	void mark() {
		marks[index - 1] = true;
	}

	boolean marked() {
		return marks[index - 1];
	}

}

public class MyStack {

	protected StackEntries head = null;
	public int depth = 0;

	public boolean empty() {
		return head == null;
	}

	public void push(byte[] o) {
		if (head == null) {
			head = new StackEntries(null);
		} else if (head.full()) {
			head = new StackEntries(head);
		}
		head.push(o);
		++depth;
	}

	public byte[] pop() {
		byte[] t = head.pop();
		--depth;
		if (head.empty())
			head = head.next;
		return t;
	}

	public byte[] peek() {
		return head.peek();
	}

	public void mark() {
		head.mark();
	}

	public boolean marked() {
		return head.marked();
	}

	public int getDepth() {
		return depth;
	}

}