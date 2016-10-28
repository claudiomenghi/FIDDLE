package ltsa.lts;

import java.io.PrintStream;
import java.util.Hashtable;

import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.csp.Declaration;
import ltsa.lts.util.collections.MyIntHash;

/**
 * Carried away EventState class specific methods from EventState to account for
 * subclasses
 * 
 * @author epavese
 *
 */

public class EventStateUtils {
	public static LTSTransitionList renumberStates(LTSTransitionList head,
			Hashtable oldtonew) {
		LTSTransitionList p = head;
		LTSTransitionList newhead = null;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				int next = q.getNext() < 0 ? Declaration.ERROR
						: ((Integer) oldtonew.get(new Integer(q.getNext())))
								.intValue();

				newhead = EventStateUtils.add(newhead,
						new LTSTransitionList(q.getEvent(), next));

				q = q.getNondet();
			}
			p = p.getList();
		}
		return newhead;
	}

	public static LTSTransitionList renumberStates(LTSTransitionList head,
			MyIntHash oldtonew) {
		LTSTransitionList p = head;
		LTSTransitionList newhead = null;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				int next = q.getNext() < 0 ? Declaration.ERROR : oldtonew.get(q
						.getNext());

				newhead = EventStateUtils.add(newhead,
						new LTSTransitionList(q.getEvent(), next));
				q = q.getNondet();
			}
			p = p.getList();
		}
		return newhead;
	}

	/*----------------------------------------------------------------*/
	/*
	 * depth first Search to return set of reachable states
	 * /*----------------------------------------------------------------
	 */

	public static MyIntHash reachable(LTSTransitionList[] states) {
		int ns = 0; // newstate
		MyIntHash visited = new MyIntHash(states.length);
		LTSTransitionList stack = null;
		stack = LTSTransitionList.push(stack, new LTSTransitionList(0, 0));
		while (stack != null) {
			int v = stack.getNext();
			stack = LTSTransitionList.pop(stack);
			if (!visited.containsKey(v)) {
				visited.put(v, ns++);
				LTSTransitionList p = states[v];
				while (p != null) {
					LTSTransitionList q = p;
					while (q != null) {
						if (q.getNext() >= 0
								&& !visited.containsKey(q.getNext()))
							stack = LTSTransitionList.push(stack, q);

						q = q.getNondet();
					}
					p = p.getList();
				}
			}
		}
		return visited;
	}

	// to = to U from
	// Agrega al to, todos los eventState a los que llega el from
	public static LTSTransitionList union(LTSTransitionList to,
			LTSTransitionList from) {
		LTSTransitionList res = to;
		LTSTransitionList p = from;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				LTSTransitionList evSt;

				evSt = new LTSTransitionList(q.getEvent(), q.getNext());
				res = EventStateUtils.add(res, evSt);

				q = q.getNondet();
			}
			p = p.getList();
		}
		return res;
	}

	/**
	 * requires that the lists are ordered by events. Branches may spawn on a
	 * given EventState: -- nondet branches for transitions under the same event
	 * -- prob branches for transitions on the same prob bundle (only //
	 * ProbabilisticEventState)
	 * 
	 * @param head
	 * @param transitionToBeAdded
	 * @return
	 */
	// the following is not very OO but efficient
	// duplicates are discarded
	public static LTSTransitionList add(LTSTransitionList head,
			LTSTransitionList transitionToBeAdded) {

		if (head == null) {
			return transitionToBeAdded;
		}
		if (transitionToBeAdded == null) {
			return head;
		}
		if (transitionToBeAdded.getEvent() < head.getEvent()) {
			transitionToBeAdded.setList(head);
			return transitionToBeAdded;
		}

		LTSTransitionList p = head;
		while (p.getList() != null
				&& p.getEvent() != transitionToBeAdded.getEvent()
				&& transitionToBeAdded.getEvent() >= p.getList().getEvent())
			p = p.getList();

		if (p.getEvent() == transitionToBeAdded.getEvent()) {
			// check if probabilistic

			// add to nondet
			LTSTransitionList q = p;
			if (q.getNext() == transitionToBeAdded.getNext()) {
				return head;
			}
			while (q.getNondet() != null) {
				q = q.getNondet();
				if (q.getNext() == transitionToBeAdded.getNext()) {
					return head;
				}
			}
			q.setNondet(transitionToBeAdded);

		} else { // unknown event, add after p
			transitionToBeAdded.setList(p.getList());
			p.setList(transitionToBeAdded);
		}

		return head;
	}

	// normally, EventState lists are sorted by event with
	// the nondet list containing lists of different next states
	// for the same event
	// transpose creates a new list sorted by next
	public static LTSTransitionList transpose(LTSTransitionList from) {
		LTSTransitionList res = null;
		LTSTransitionList p = from;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {

				res = EventStateUtils.add(res,
						new LTSTransitionList(q.getNext(), q.getEvent())); // swap
																			// event
																			// &
																			// next

				q = q.getNondet();
			}
			p = p.getList();
		}
		// now walk through the list a swap event & next back again
		p = res;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				int n = q.getNext();
				q.setNext(q.getEvent());
				q.setEvent(n);

				q = q.getNondet();
			}
			p = p.getList();
		}
		return res;
	}

	public static void printAUT(LTSTransitionList head, int from,
			String[] alpha, PrintStream output) {
		LTSTransitionList p = head;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {

				output.print("(" + from + "," + alpha[q.getEvent()] + ","
						+ q.getNext() + ")\n");

				q = q.getNondet();
			}
			p = p.getList();
		}
	}

	public static int count(LTSTransitionList head) {
		LTSTransitionList p = head;
		int n = 0;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				n++;
				
				q = q.getNondet();
			}
			p = p.getList();
		}
		return n;
	}

}
