package ltsa.lts.util.collections;

import java.math.BigDecimal;

import ltsa.lts.automata.probabilistic.ProbabilisticTransition;

/**
 * It is a specialized List for the analyzer. 
 * Its stores transitions
 */

class MyTransitionListEntry {
	int fromState;
	byte[] toState;
	int actionNo;
	MyTransitionListEntry next;

	MyTransitionListEntry(int from, byte[] to, int action) {
		fromState = from;
		toState = to;
		actionNo = action;
		this.next = null;
	}
}

public class MyList {
	protected MyTransitionListEntry head = null;
	protected MyTransitionListEntry tail = null;
	protected int count = 0;

	public MyTransitionListEntry peek() {
		return head;
	}

	public void add(MyTransitionListEntry e) {
		if (head == null) {
			head = tail = e;
		} else {
			tail.next = e;
			tail = e;
		}
		++count;
	}

	public void add(int from, byte[] to, int action) {
		MyTransitionListEntry e = new MyTransitionListEntry(from, to, action);
		add(e);
	}

	public void add(int from, byte[] to, int action, int bundle, BigDecimal prob) {
		MyProbListEntry e = new MyProbListEntry(from, to, action, bundle, prob);
		add(e);
	}

	public void next() {
		if (head != null)
			head = head.next;
	}

	public boolean empty() {
		return head == null;
	}

	public int getFrom() {
		return head != null ? head.fromState : -1;
	}

	public byte[] getTo() {
		return head != null ? head.toState : null;
	}

	public int getAction() {
		return head != null ? head.actionNo : -1;
	}

	public int size() {
		return count;
	}

	public int getBundle() {
		if (head == null) {
			return ProbabilisticTransition.BUNDLE_ERROR;
		} else if (head instanceof MyProbListEntry) {
			MyProbListEntry probHead = (MyProbListEntry) head;
			return probHead.bundle;
		} else {
			return ProbabilisticTransition.NO_BUNDLE;
		}
	}

	public BigDecimal getProb() {
		if (head == null) {
			return BigDecimal.ZERO;
		} else if (head instanceof MyProbListEntry) {
			MyProbListEntry probHead = (MyProbListEntry) head;
			return probHead.prob;
		} else {
			return BigDecimal.ONE;
		}
	}
}
