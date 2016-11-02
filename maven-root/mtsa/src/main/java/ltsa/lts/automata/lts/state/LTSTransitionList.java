package ltsa.lts.automata.lts.state;

import java.util.BitSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

import ltsa.lts.EventStateUtils;
import ltsa.lts.csp.Declaration;
import ltsa.lts.csp.Relation;
import ltsa.lts.output.LTSOutput;

import org.apache.commons.lang.ArrayUtils;

import com.google.common.base.Preconditions;

/**
 * records transitions in the CompactState class
 * 
 *
 */
public class LTSTransitionList {

	/**
	 * contains the position of the event in the alphabet
	 */
	private int event;
	private int next;
	private int machine;

	/**
	 * used to keep list in event order, TAU first
	 */
	private LTSTransitionList list;

	/**
	 * used for additional non-deterministic transitions
	 */
	private LTSTransitionList nondet;

	/**
	 * used by analyser & by minimiser
	 */
	private LTSTransitionList path;

	/**
	 * 
	 * @param event
	 *            the event
	 * @param next
	 *            the next state
	 * 
	 */
	public LTSTransitionList(int event, int next) {
		Preconditions.checkArgument(next >= -1,
				"The id of the state must be grather than or equal to -1");
		this.event = event;
		this.next = next;
	}

	public LTSTransitionList(int event, int next, int machine) {
		this(event, next);
		this.machine = machine;
	}

	public void setEvent(int event) {
		this.event = event;
	}

	public void setNext(int next) {
		Preconditions.checkArgument(next >= -1, "The id " + next
				+ " of the state must be grather than or equal to -1");

		this.next = next;
	}

	public void setMachine(int machine) {
		this.machine = machine;
	}

	public void setNondet(LTSTransitionList nondet) {
		this.nondet = nondet;
	}

	public void setPath(LTSTransitionList path) {
		this.path = path;
	}

	public Enumeration<LTSTransitionList> elements() {
		return new EventStateEnumerator(this);
	}

	public int getEvent() {
		return event;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("(" + this.getEvent() + ", [" + this.getNext());

		LTSTransitionList transition = this.getNondet();
		while (transition != null) {
			if (this.getEvent() != transition.getEvent()) {
				throw new InternalError(
						"The transitions in the no det must have the same valu");
			}
			if (transition.getList() != null) {
				throw new InternalError("The list of the non det transition: ("
						+ this.getEvent() + "," + this.getNext()
						+ ") must be null");
			}
			builder.append(", " + transition.getNext());
			transition = transition.getNondet();
		}

		builder.append("])");
		if (this.getList() != null) {
			String ret = this.getList().toString();
			builder.append(ret != null ? ret : "");
		}

		return builder.toString();
	}

	public int[] getEvents() {
		int[] events;
		if (this.list == null) {
			events = new int[1];
			events[0] = this.event;
		} else {
			int[] head = new int[1];
			head[0] = this.event;

			int[] tail = this.list.getEvents();

			events = ArrayUtils.addAll(head, tail);
		}

		return events;
	}

	public int getNext() {
		return next;
	}

	public int getNext(int event) {
		if (this.event == event) {
			return this.next;
		} else {
			if (this.list == null) {
				throw new IllegalStateException("Invalid action execution");
			}

			return this.list.getNext(event);
		}
	}

	public void updateEventAndNext(int oldEvent, int newEvent, int newNext) {
		if (this.event == oldEvent) {
			this.event = newEvent;
			this.next = newNext;
		} else {
			if (this.list != null)
				this.list.updateEventAndNext(oldEvent, newEvent, newNext);
		}
	}

	public void swapNext(int next1, int next2) {
		if (this.next == next1) {
			this.next = next2;
		} else {
			if (this.next == next2)
				this.next = next1;
		}

		if (this.list != null)
			this.list.swapNext(next1, next2);
	}

	/**
	 * remove the transition from the list
	 * 
	 * @param transitionList
	 *            the list of transition from with the specified transition must
	 *            be removed
	 * @param transitionToBeRemoved
	 *            the transition to be removed
	 * @return the original list where the transition is removed
	 * @throws NullPointerException
	 *             if one of the transition to be removed is null
	 * @throws IllegalArgumentException
	 *             if the transition to be removed is a list instead of a
	 *             transition
	 */
	public static LTSTransitionList remove(LTSTransitionList transitionList,
			LTSTransitionList transitionToBeRemoved) {
		Preconditions.checkNotNull(transitionToBeRemoved,
				"The transition to be removed cannot be null");
		

		// remove from head
		if (transitionList == null) {
			return transitionList;
		}
		if (transitionList.event == transitionToBeRemoved.event
				&& transitionList.next == transitionToBeRemoved.next) {
			if (transitionList.nondet == null)
				return transitionList.list;
			else {
				transitionList.nondet.list = transitionList.list;
				return transitionList.nondet;
			}
		}
		LTSTransitionList p = transitionList;
		LTSTransitionList plag = transitionList;
		while (p != null) {
			LTSTransitionList q = p;
			LTSTransitionList qlag = p;
			while (q != null) {
				if (q.event == transitionToBeRemoved.event
						&& q.next == transitionToBeRemoved.next) {
					if (p == q) { // remove from head of nondet
						if (p.nondet == null) {
							plag.list = p.list;
							return transitionList;
						} else {
							p.nondet.list = p.list;
							plag.list = p.nondet;
							return transitionList;
						}
					} else {
						qlag.nondet = q.nondet;
						return transitionList;
					}
				}
				qlag = q;
				q = q.nondet;
			}
			plag = p;
			p = p.list;
		}
		return transitionList;
	}

	public static LTSTransitionList removeEvent(LTSTransitionList anEventState,
			int anEvent) {
		if (anEventState == null)
			return null;

		LTSTransitionList cleanEvent = anEventState;
		if (cleanEvent.event == anEvent)
			cleanEvent = cleanEvent.list;
		else
			cleanEvent.list = LTSTransitionList.removeEvent(cleanEvent.list,
					anEvent);
		return cleanEvent;
	}

	public static LTSTransitionList copy(LTSTransitionList anEventState) {
		LTSTransitionList aCopy = new LTSTransitionList(anEventState.event,
				anEventState.next);
		aCopy.machine = anEventState.machine;

		if (anEventState.list == null)
			aCopy.list = null;
		else
			aCopy.list = LTSTransitionList.copy(anEventState.list);

		if (anEventState.nondet == null)
			aCopy.nondet = null;
		else
			aCopy.nondet = LTSTransitionList.copy(anEventState.nondet);

		if (anEventState.path == null)
			aCopy.path = null;
		else
			aCopy.path = LTSTransitionList.copy(anEventState.path);

		return aCopy;
	}

	public static boolean hasState(LTSTransitionList head, int next) {
		LTSTransitionList p = head;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				if (q.next == next)
					return true;
				q = q.nondet;
			}
			p = p.list;
		}
		return false;
	}

	public static void replaceWithError(LTSTransitionList head, int sinkState) {
		LTSTransitionList p = head;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				if (q.next == sinkState)
					q.next = Declaration.ERROR;
				q = q.nondet;
			}
			p = p.list;
		}
	}

	public static LTSTransitionList offsetSeq(int off, int seq, int max,
			LTSTransitionList head) {
		LTSTransitionList p = head;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				if (q.next >= 0) {
					if (q.next == seq)
						q.next = max;
					else
						q.next += off;
				}
				q = q.nondet;
			}
			p = p.list;
		}
		return head;
	}

	public static LTSTransitionList replaceNext(LTSTransitionList anEventState,
			int oldNext, int newNext) {
		if (anEventState == null)
			return null;

		if (anEventState.next == oldNext)
			anEventState.next = newNext;

		anEventState.list = LTSTransitionList.replaceNext(anEventState.list,
				oldNext, newNext);

		return anEventState;
	}

	/**
	 * returns a new transition list in which the old transition list is
	 * concatenated with the new transition with the specified d
	 * 
	 * @param transitionList
	 *            the transition list that exits a state
	 * @param anEvent
	 *            the event
	 * @param aState
	 *            the destination of the new transition
	 * @return a new transition list in which the old transition list is
	 *         concatenated with the new transition with the specified
	 *         destination state and event
	 */
	public static LTSTransitionList addTransition(
			LTSTransitionList transitionList, int anEvent, int aState) {
		if (transitionList == null) {
			return new LTSTransitionList(anEvent, aState);
		} else {
			LTSTransitionList updatedEvent = new LTSTransitionList(anEvent,
					aState);
			updatedEvent.nondet = transitionList.nondet;
			updatedEvent.list = transitionList;
			return updatedEvent;
		}
	}

	public static int toState(LTSTransitionList head, int next) {
		LTSTransitionList p = head;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				if (q.next == next)
					return q.event;
				q = q.nondet;
			}
			p = p.list;
		}
		return -1;
	}

	public static int countStates(LTSTransitionList head, int next) {
		LTSTransitionList p = head;
		int result = 0;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				if (q.next == next)
					result++;
				q = q.nondet;
			}
			p = p.list;
		}
		return result;
	}

	public static boolean hasEvent(LTSTransitionList head, int event) {
		LTSTransitionList p = head;
		while (p != null) {
			if (p.event == event)
				return true;
			p = p.list;
		}
		return false;
	}

	public static boolean isAccepting(LTSTransitionList head, String[] alphabet) {
		LTSTransitionList p = head;
		while (p != null) {
			if (alphabet[p.event].charAt(0) == '@') {
				return true;
			}
			p = p.list;
		}
		return false;
	}

	public static boolean isTerminal(int state, LTSTransitionList head) {
		LTSTransitionList p = head;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				if (q.next != state)
					return false;
				q = q.nondet;
			}
			p = p.list;
		}
		return true;
	}

	public static LTSTransitionList firstCompState(LTSTransitionList head,
			int event, int[] state) {
		LTSTransitionList p = head;
		while (p != null) {
			if (p.event == event) {
				state[p.machine] = p.next;
				return p.nondet;
			}
			p = p.list;
		}
		return null;
	}

	public static LTSTransitionList moreCompState(LTSTransitionList head,
			int[] state) {
		state[head.machine] = head.next;
		return head.nondet;
	}

	public static boolean hasTau(LTSTransitionList head) {
		if (head == null)
			return false;
		return (head.event == Declaration.TAU);
	}

	public static boolean hasOnlyTau(LTSTransitionList head) {
		if (head == null)
			return false;
		return ((head.event == Declaration.TAU) && head.list == null);
	}

	public static boolean hasOnlyTauAndAccept(LTSTransitionList head,
			String[] alphabet) {
		if (head == null)
			return false;
		if (head.event != Declaration.TAU)
			return false;
		if (head.list == null)
			return true;
		if (alphabet[head.list.event].charAt(0) != '@')
			return false;
		return (head.list.list == null);
	}

	// precondition is "hasOnlyTauAndAccept"
	public static LTSTransitionList removeAccept(LTSTransitionList head) {
		head.list = null;
		return head;
	}

	public static LTSTransitionList addNonDetTau(LTSTransitionList head,
			LTSTransitionList states[], BitSet tauOnly) {
		LTSTransitionList p = head;
		LTSTransitionList toAdd = null;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				if (q.next > 0 && tauOnly.get(q.next)) {
					int nextS[] = nextState(states[q.next], Declaration.TAU);
					q.next = nextS[0]; // replace transition to next
					for (int i = 1; i < nextS.length; ++i) {
						toAdd = EventStateUtils.add(toAdd,
								new LTSTransitionList(q.event, nextS[i]));
					}
				}
				q = q.nondet;
			}
			p = p.list;
		}
		if (toAdd == null)
			return head;
		else
			return EventStateUtils.union(head, toAdd);
	}

	public static boolean hasNonDet(LTSTransitionList head) {
		LTSTransitionList p = head;
		while (p != null) {
			if (p.nondet != null)
				return true;
			p = p.list;
		}
		return false;
	}

	public static boolean hasNonDetEvent(LTSTransitionList head, int event) {
		LTSTransitionList p = head;
		while (p != null) {
			if (p.event == event && p.nondet != null)
				return true;
			p = p.list;
		}
		return false;
	}

	public static int[] localEnabled(LTSTransitionList head) {
		LTSTransitionList p = head;
		int n = 0;
		while (p != null) {
			++n;
			p = p.list;
		}
		if (n == 0)
			return null;
		int[] a = new int[n];
		p = head;
		n = 0;
		while (p != null) {
			a[n++] = p.event;
			p = p.list;
		}
		return a;
	}

	public static void hasEvents(LTSTransitionList head, BitSet actions) {
		LTSTransitionList p = head;
		while (p != null) {
			actions.set(p.event);
			p = p.list;
		}
	}

	public static int[] nextState(LTSTransitionList head, int event) {
		LTSTransitionList p = head;
		while (p != null) {
			if (p.event == event) {
				LTSTransitionList q = p;
				int size = 0;
				while (q != null) {
					q = q.nondet;
					++size;
				}
				q = p;
				int n[] = new int[size];
				for (int i = 0; i < n.length; ++i) {
					n[i] = q.next;
					q = q.nondet;
				}
				return n;
			}
			p = p.list;
		}
		return null;
	}

	public static LTSTransitionList renumberEvents(LTSTransitionList head,
			Hashtable<Integer, Integer> oldtonew) {
		LTSTransitionList p = head;
		LTSTransitionList newhead = null;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				int event = oldtonew.get(q.event);
				LTSTransitionList child = new LTSTransitionList(event, q.next);
				newhead = EventStateUtils.add(newhead, child);
				q = q.nondet;
			}
			p = p.list;
		}
		return newhead;
	}

	public int getMachine() {
		return machine;
	}

	public LTSTransitionList getNondet() {
		return nondet;
	}

	public LTSTransitionList getPath() {
		return path;
	}

	public static LTSTransitionList newTransitions(LTSTransitionList head,
			Relation oldtonew) {
		LTSTransitionList p = head;
		LTSTransitionList newhead = null;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				Object o = oldtonew.get(new Integer(q.event));
				if (o != null) {
					if (o instanceof Integer) {
						newhead = EventStateUtils.add(newhead,
								new LTSTransitionList(((Integer) o).intValue(),
										q.next));
					} else {
						@SuppressWarnings("unchecked")
						Vector<Integer> v = (Vector<Integer>) o;
						for (Enumeration<Integer> e = v.elements(); e
								.hasMoreElements();) {
							newhead = EventStateUtils
									.add(newhead,
											new LTSTransitionList(((Integer) e
													.nextElement()).intValue(),
													q.next));
						}
					}
				}
				q = q.nondet;
			}
			p = p.list;
		}
		return newhead;
	}

	public static LTSTransitionList offsetEvents(LTSTransitionList head,
			int offset) {
		LTSTransitionList p = head;
		LTSTransitionList newhead = null;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				q.event = q.event == 0 ? 0 : q.event + offset;
				q = q.nondet;
			}
			p = p.list;
		}
		return newhead;
	}

	public static LTSTransitionList addTransToError(LTSTransitionList head,
			int last) {
		LTSTransitionList p = head;
		LTSTransitionList newhead = null;
		// Skip TAU y TAU?
		// p.event==Declaration.TAU_MAYBE is a problem when it's not an MTS!
		if (p != null && (p.event == Declaration.TAU /*
													 * || p.event==Declaration.
													 * TAU_MAYBE
													 */))
			p = p.list; // skip tau
		int index = 2;
		while (p != null) {
			if (index < p.event) {
				for (int i = index; i < p.event; i++)
					newhead = EventStateUtils.add(newhead,
							new LTSTransitionList(i, Declaration.ERROR));

			}
			index = p.event + 1;
			LTSTransitionList q = p;
			while (q != null) {
				// Avoid adding maybes.
				newhead = EventStateUtils.add(newhead, new LTSTransitionList(
						q.event, q.next));
				q = q.nondet;
			}
			p = p.list;
		}
		for (int i = index; i < last; i++)
			newhead = EventStateUtils.add(newhead, new LTSTransitionList(i,
					Declaration.ERROR));
		return newhead;
	}

	// precondition - no non-deterministic transitions
	public static LTSTransitionList removeTransToError(LTSTransitionList head) {
		LTSTransitionList p = head;
		LTSTransitionList newHead = null;
		while (p != null) {
			if (p.next != Declaration.ERROR)
				newHead = EventStateUtils.add(newHead, new LTSTransitionList(
						p.event, p.next));
			p = p.list;
		}
		return newHead;
	}

	private static LTSTransitionList removeNonDetTransToState(
			LTSTransitionList head, Collection<Integer> destinationStates) {
		if (head == null) {
			return null;
		}
		LTSTransitionList p = head;
		while (p != null && destinationStates.contains(p.next)) {
			p = p.getNondet();
		}
		if (p == null) {
			return null;
		}
		LTSTransitionList toBeReturned = p;
		while (p.nondet != null) {
			if (destinationStates.contains(p.nondet.getNext())) {
				p.setNondet(p.nondet.getNondet());
				// probably here
			} else {
				p = p.getNondet();
			}
		}

		return toBeReturned;
	}

	public static LTSTransitionList removeTransToState(LTSTransitionList head,
			Collection<Integer> destinationStates) {
		if (head == null) {
			return null;
		}
		LTSTransitionList pBeforeRemoving = head;
		LTSTransitionList p = removeNonDetTransToState(pBeforeRemoving,
				destinationStates);
		while (p == null && pBeforeRemoving != null) {
			pBeforeRemoving = pBeforeRemoving.getList();
			p = removeNonDetTransToState(pBeforeRemoving, destinationStates);
		}
		if (pBeforeRemoving == null) {
			return null;
		}

		LTSTransitionList toBeReturned = p;
		toBeReturned.setList(pBeforeRemoving.getList());

		pBeforeRemoving = toBeReturned;

		while (pBeforeRemoving != null && pBeforeRemoving.getList() != null) {
			LTSTransitionList succBeforeRemoving = pBeforeRemoving.getList();
			LTSTransitionList succAfterRemoving = removeNonDetTransToState(
					succBeforeRemoving, destinationStates);
			if (succAfterRemoving != null) {
				pBeforeRemoving.setList(succAfterRemoving);
				succAfterRemoving.setList(succBeforeRemoving.getList());
				pBeforeRemoving = succAfterRemoving;
			} else {
				pBeforeRemoving = succBeforeRemoving.getList();
			}
		}
		return toBeReturned;
	}

	/**
	 * remove tau actions
	 * 
	 * @param head
	 * @return
	 */
	public static LTSTransitionList removeTau(LTSTransitionList head) {
		if (head == null)
			return head;
		if (head.event != Declaration.TAU)
			return head;
		return head.list;
	}

	// agrega al path los estados alcanzables y las acciones por las que se
	// mueve
	// add states reachable by next from events
	public static LTSTransitionList tauAdd(LTSTransitionList head,
			LTSTransitionList[] T) {
		LTSTransitionList p = head;
		LTSTransitionList added = null;
		if (p != null && p.event == Declaration.TAU)
			p = p.list; // skip tau
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				if (q.next != Declaration.ERROR) {
					LTSTransitionList t = T[q.next];
					while (t != null) {
						added = push(added, new LTSTransitionList(p.event,
								t.next));
						t = t.nondet;
					}
				}
				q = q.nondet;
			}
			p = p.list;
		}
		while (added != null) {
			head = EventStateUtils.add(head, added);
			added = pop(added);
		}
		return head;
	}

	public static void setActions(LTSTransitionList head, BitSet b) {
		LTSTransitionList p = head;
		while (p != null) {
			b.set(p.event);
			p = p.list;
		}
	}

	// add actions reachable by tau
	public static LTSTransitionList actionAdd(LTSTransitionList head,
			LTSTransitionList[] states) {
		if (head == null || head.event != Declaration.TAU)
			return head; // no tau
		LTSTransitionList tau = head;
		while (tau != null) {
			if (tau.next != Declaration.ERROR)
				head = EventStateUtils.union(head, states[tau.next]);
			tau = tau.nondet;
		}
		return head;
	}

	// only applicable to a transposed list
	// returns set of event names to next state
	public static String[] eventsToNext(LTSTransitionList from,
			String[] alphabet) {
		LTSTransitionList q = from;
		int size = 0;
		while (q != null) {
			q = q.nondet;
			++size;
		}
		q = from;
		String s[] = new String[size];
		for (int i = 0; i < s.length; ++i) {
			s[i] = alphabet[q.event];
			q = q.nondet;
		}
		return s;
	}

	// only applicable to a transposed list
	// returns set of event names to next state
	// omit accepting label
	public static String[] eventsToNextNoAccept(LTSTransitionList from,
			String[] alphabet) {
		LTSTransitionList q = from;
		int size = 0;
		while (q != null) {
			if (alphabet[q.event].charAt(0) != '@')
				++size;
			q = q.nondet;
		}
		q = from;
		String s[] = new String[size];
		for (int i = 0; i < s.length; ++i) {
			if (alphabet[q.event].charAt(0) != '@')
				s[i] = alphabet[q.event];
			else
				--i;
			q = q.nondet;
		}
		return s;
	}

	/* -------------------------------------------------------------- */
	// Stack using path
	/* -------------------------------------------------------------- */

	public static LTSTransitionList push(LTSTransitionList head,
			LTSTransitionList es) {
		if (head == null)
			es.path = es;
		else
			es.path = head;
		return head = es;
	}

	public static boolean inStack(LTSTransitionList es) {
		return (es.path != null);
	}

	public static LTSTransitionList pop(LTSTransitionList head) {
		if (head == null)
			return head;
		LTSTransitionList es = head;
		head = es.path;
		es.path = null;
		if (head == es)
			return null;
		else
			return head;
	}

	/*-------------------------------------------------------------*/
	// compute all states reachable from state k
	/*-------------------------------------------------------------*/
	// lo hace devolviendo transiciones TAU a los estados alcanzables
	public static LTSTransitionList reachableTau(LTSTransitionList[] states,
			int k) {
		LTSTransitionList head = states[k];
		if (head == null || head.event != Declaration.TAU)
			return null;
		BitSet visited = new BitSet(states.length);
		visited.set(k);
		LTSTransitionList stack = null;
		while (head != null) {
			stack = push(stack, head);
			head = head.nondet;
		}
		// armo una pila con todos los estados que se llegan desde el estado k
		while (stack != null) {
			int j = stack.next;
			head = EventStateUtils.add(head, new LTSTransitionList(
					Declaration.TAU, j));
			stack = pop(stack);
			if (j != Declaration.ERROR) {
				visited.set(j);
				LTSTransitionList t = states[j];
				if (t != null && t.event == Declaration.TAU)
					while (t != null) {
						if (!inStack(t)) {
							if (t.next < 0 || !visited.get(t.next))
								stack = push(stack, t);
						}
						t = t.nondet;
					}
			}
		}
		return head;
	}

	/* -------------------------------------------------------------- */
	// Queue using path
	/* -------------------------------------------------------------- */

	private static LTSTransitionList addtail(LTSTransitionList tail,
			LTSTransitionList es) {
		es.path = null;
		if (tail != null)
			tail.path = es;
		return es;
	}

	private static LTSTransitionList removehead(LTSTransitionList head) {
		if (head == null)
			return head;
		LTSTransitionList es = head;
		head = es.path;
		return head;
	}

	/*-------------------------------------------------------------*/
	// breadth first search of states from 0, return trace to deadlock/error
	/*-------------------------------------------------------------*/

	public static int search(LTSTransitionList trace,
			LTSTransitionList[] states, int fromState, int findState,
			int ignoreState) {
		return search(trace, states, fromState, findState, ignoreState, true);
	}

	public static int search(LTSTransitionList trace,
			LTSTransitionList[] states, int fromState, int findState,
			int ignoreState, boolean checkDeadlocks) {
		LTSTransitionList zero = new LTSTransitionList(0, fromState);
		LTSTransitionList head = zero;
		LTSTransitionList tail = zero;
		int res = Declaration.SUCCESS;
		// int id = 0;
		LTSTransitionList val[] = new LTSTransitionList[states.length + 1]; // shift
																			// by
																			// 1
																			// so
		// ERROR is 0
		while (head != null) {
			int k = head.next;
			val[k + 1] = head; // the event that got us here
			if (k < 0 || k == findState) {
				if (!checkDeadlocks) {
					res = Declaration.ERROR;
					break;// ERROR
				} else {
					head = removehead(head);
				}
			} else {
				LTSTransitionList t = states[k];
				if (checkDeadlocks && t == null && k != ignoreState) {
					res = Declaration.STOP;
					break;
				}
				; // DEADLOCK
				while (t != null) {
					LTSTransitionList q = t;
					while (q != null) {
						if (val[q.next + 1] == null) { // not visited or in
														// queue
							q.machine = k; // backward pointer to source state
							tail = addtail(tail, q);
							val[q.next + 1] = zero;
						}
						q = q.nondet;
					}
					t = t.list;
				}
				head = removehead(head);
			}
		}
		if (head == null)
			return res;
		LTSTransitionList stack = null;
		LTSTransitionList ts = head;
		while (ts.next != fromState) {
			stack = push(stack, ts);
			ts = val[ts.machine + 1];
		}
		trace.path = stack;
		return res;
	}

	/*-------------------------------------------------------------*/
	// print a path of EventStates
	/*-------------------------------------------------------------*/
	public static void printPath(LTSTransitionList head, String[] alpha,
			LTSOutput output) {
		LTSTransitionList q = head;
		while (q != null) {
			output.outln("\t" + alpha[q.event]);
			q = pop(q);
		}
	}

	public static Vector<String> getPath(LTSTransitionList head, String[] alpha) {
		LTSTransitionList q = head;
		Vector<String> v = new Vector<>();
		while (q != null) {
			v.addElement(alpha[q.event]);
			q = pop(q);
		}
		return v;
	}

	public void setList(LTSTransitionList list) {
		this.list = list;
	}

	public LTSTransitionList getList() {
		return this.list;
	}
}

final class EventStateEnumerator implements Enumeration<LTSTransitionList> {
	LTSTransitionList es;
	LTSTransitionList list;

	EventStateEnumerator(LTSTransitionList es) {
		this.es = es;
		if (es != null)
			list = es.getList();
	}

	@Override
	public boolean hasMoreElements() {
		return es != null;
	}

	@Override
	public LTSTransitionList nextElement() {
		if (es != null) {
			LTSTransitionList temp = es;

			if (es.getNondet() != null)
				es = es.getNondet();
			else {
				es = list;
				if (es != null)
					list = list.getList();
			}
			return temp;
		}
		throw new NoSuchElementException("EventStateEnumerator");
	}
}