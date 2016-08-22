package ltsa.lts;

import java.io.PrintStream;
import java.util.Hashtable;

import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.probabilistic.ProbabilisticEventState;
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
				if (q instanceof ProbabilisticEventState) {
					newhead = EventStateUtils.add(
							newhead,
							new ProbabilisticEventState(q.getEvent(), next,
									((ProbabilisticEventState) q)
											.getProbability(),
									((ProbabilisticEventState) q).getBundle()));
					ProbabilisticEventState probSt = (ProbabilisticEventState) ((ProbabilisticEventState) q)
							.getProbTr();
					while (probSt != null) {
						next = probSt.getNext() < 0 ? Declaration.ERROR
								: ((Integer) oldtonew.get(new Integer(probSt
										.getNext()))).intValue();
						newhead = EventStateUtils.add(
								newhead,
								new ProbabilisticEventState(probSt.getEvent(),
										next, probSt.getProbability(), probSt
												.getBundle()));
						probSt = (ProbabilisticEventState) probSt.getProbTr();
					}
				} else {
					newhead = EventStateUtils.add(newhead,
							new LTSTransitionList(q.getEvent(), next));
				}
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
				if (q instanceof ProbabilisticEventState) {
					newhead = EventStateUtils.add(
							newhead,
							new ProbabilisticEventState(q.getEvent(), next,
									((ProbabilisticEventState) q)
											.getProbability(),
									((ProbabilisticEventState) q).getBundle()));
					ProbabilisticEventState probSt = (ProbabilisticEventState) ((ProbabilisticEventState) q)
							.getProbTr();
					while (probSt != null) {
						next = probSt.getNext() < 0 ? Declaration.ERROR
								: oldtonew.get(probSt.getNext());
						newhead = EventStateUtils.add(
								newhead,
								new ProbabilisticEventState(probSt.getEvent(),
										next,
										((ProbabilisticEventState) probSt)
												.getProbability(),
										((ProbabilisticEventState) probSt)
												.getBundle()));
						probSt = (ProbabilisticEventState) probSt.getProbTr();
					}
				} else {
					newhead = EventStateUtils.add(newhead,
							new LTSTransitionList(q.getEvent(), next));
				}
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

						if (q instanceof ProbabilisticEventState) {
							ProbabilisticEventState probSt = (ProbabilisticEventState) ((ProbabilisticEventState) q)
									.getProbTr();
							while (probSt != null) {
								if (probSt.getNext() >= 0
										&& !visited.containsKey(probSt
												.getNext()))
									stack = LTSTransitionList.push(stack,
											probSt);
								probSt = (ProbabilisticEventState) probSt
										.getProbTr();
							}
						}

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
				if (q instanceof ProbabilisticEventState) {
					ProbabilisticEventState probQ = (ProbabilisticEventState) q;
					evSt = new ProbabilisticEventState(probQ.getEvent(),
							probQ.getNext(), probQ.getProbability(),
							probQ.getBundle());
					res = EventStateUtils.add(res, evSt);

					ProbabilisticEventState probSt = (ProbabilisticEventState) probQ
							.getProbTr();
					while (probSt != null) {
						evSt = new ProbabilisticEventState(probSt.getEvent(),
								probSt.getNext(), probSt.getProbability(),
								probSt.getBundle());
						res = EventStateUtils.add(res, evSt);
						probSt = (ProbabilisticEventState) probSt.getProbTr();
					}
				} else {
					evSt = new LTSTransitionList(q.getEvent(), q.getNext());
					res = EventStateUtils.add(res, evSt);
				}

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
			if (transitionToBeAdded instanceof ProbabilisticEventState) {
				// p must also be ProbabilisticEventState
				ProbabilisticEventState newP, newTr;
				try {
					newP = (ProbabilisticEventState) p;
					newTr = (ProbabilisticEventState) transitionToBeAdded;
					// check if it is for existing bundle
					while (newP != null && newP.getEvent() == newTr.getEvent()
							&& newTr.getBundle() != newP.getBundle()) {
						newP = (ProbabilisticEventState) newP.getNondet();
					}

					if (newP != null) {
						// it is a known bundle
						newTr.setProbTr(newP.getProbTr());
						newP.setProbTr(newTr);
					} else {
						// is a new bundle, a nondet transition on event
						newTr.setNondet(p.getNondet());
						p.setNondet(newTr);
					}
				} catch (ClassCastException e) {
					Diagnostics.fatal("Probabilistic transitions expected", e);
				}
			} else {
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
			}
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
				if (q instanceof ProbabilisticEventState) {
					ProbabilisticEventState probQ = (ProbabilisticEventState) q;
					do {
						res = EventStateUtils.add(
								res,
								new ProbabilisticEventState(probQ.getNext(),
										probQ.getEvent(), probQ
												.getProbability(), probQ
												.getBundle()));
						probQ = (ProbabilisticEventState) probQ.getProbTr();
					} while (probQ != null);
				} else {
					res = EventStateUtils.add(res,
							new LTSTransitionList(q.getNext(), q.getEvent())); // swap
																				// event
																				// &
																				// next
				}
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
				if (q instanceof ProbabilisticEventState) {
					ProbabilisticEventState probQ = (ProbabilisticEventState) q;
					probQ = (ProbabilisticEventState) probQ.getProbTr();
					while (probQ != null) {
						n = probQ.getNext();
						probQ.setNext(probQ.getEvent());
						probQ.setEvent(n);
						probQ = (ProbabilisticEventState) probQ.getProbTr();
					}
				}
				q = q.getNondet();
			}
			p = p.getList();
		}
		return res;
	}

	public static void printAUT(LTSTransitionList head, int from,
			String[] alpha, PrintStream output) {
		LTSTransitionList p = head;
		ProbabilisticEventState probP;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {

				if (q instanceof ProbabilisticEventState) {
					probP = (ProbabilisticEventState) q;
					while (probP != null) {
						output.print("(" + from + "," + alpha[probP.getEvent()]
								+ "{" + probP.getBundle() + ":"
								+ probP.getProbability() + "},"
								+ probP.getNext() + ")\n");
						probP = (ProbabilisticEventState) probP.getProbTr();
					}

				} else {
					output.print("(" + from + "," + alpha[q.getEvent()] + ","
							+ q.getNext() + ")\n");
				}
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
				if (q instanceof ProbabilisticEventState) {
					ProbabilisticEventState probP = (ProbabilisticEventState) q;
					probP = (ProbabilisticEventState) probP.getProbTr();
					while (probP != null) {
						n++;
						probP = (ProbabilisticEventState) probP.getProbTr();
					}
				}
				q = q.getNondet();
			}
			p = p.getList();
		}
		return n;
	}

}
