package ltsa.lts.animator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;

public class PartialOrder {

	private LabelledTransitionSystem machines[]; // array of machines to be composed
	private int[][] actionSharedBy; // [action number] list of machines that
									// share action
									// ordered by machine number
	private StackChecker checker; // provides on stack check
	private int candidates[][]; // 0=not a candidate, 1= all independent
								// actions, 2= max shared by two etc.
	private int partners[][]; // records partner for candidate = 2 states -1
								// otherwise
	private int Nactions;

	private String[] names; // alphabet of composite
	private BitSet visible; // visible actions
	private boolean preserveOE; // if set preserve Observational Equivalence
	private BitSet high;

	/**
	 * 
	 * @param alphabet
	 *            map action name to bitmap of shared machines
	 * @param actionName
	 *            map number to name;
	 * @param sm
	 * @param ck
	 *            array of state machines to be composed
	 * @param hidden
	 *            vector of prefixes
	 * @param exposeNotHide
	 * @param OE
	 *            preserve Observational Equivalence
	 * @param high
	 *            null or set of high priority actions
	 */
	public PartialOrder(Map<String, BitSet> alphabet, String[] actionName,
			LabelledTransitionSystem[] sm, StackChecker ck, Vector<String> hidden,
			boolean exposeNotHide, boolean OE, BitSet high) {
		machines = sm;
		names = actionName;
		Nactions = actionName.length;
		checker = ck;
		preserveOE = OE;
		this.high = high;
		// compute actionSharedby for non tau action
		actionSharedBy = new int[Nactions][];
		for (int i = 1; i < actionName.length; ++i) {
			BitSet b = alphabet.get(actionName[i]);
			actionSharedBy[i] = bitsToArray(b);
		}
		// compute visible set
		visible = new BitSet(Nactions);
		for (int i = 1; i < actionName.length; ++i) {
			if (hidden == null) {
				visible.set(i);
			} else if (exposeNotHide) {
				if (LabelledTransitionSystem.contains(actionName[i], hidden))
					visible.set(i);
			} else {
				if (!LabelledTransitionSystem.contains(actionName[i], hidden))
					visible.set(i);
			}
		}
		// compute candidates & partners
		initPartners();
		candidates = computeCandidates();
	}

	/**
	 * given the current state returns the set of eligible transitions
	 * 
	 * @param state
	 *            the state to be considered
	 * @return the set of eligible transitions
	 */
	public List<int[]> transitions(int[] state) {
		// find machine with all independent actions i.e. candidate == 1
		for (int mach = 0; mach < machines.length; ++mach) {
			if (candidates[mach][state[mach]] == 1) {
				List<int[]> trs = new ArrayList<>(8);
				boolean res = getMachTransitions(trs, mach, state, null);
				if (res) {
					return trs;
				}
			}
		}
		// find pair of machines with independent actions
		for (int mach = 0; mach < machines.length; ++mach) {
			if (candidates[mach][state[mach]] == 2) {
				int partner = partners[mach][state[mach]];
				if (mach == partners[partner][state[partner]]) { // his partner
																	// is me so
																	// we match
					List<int[]> trs = getPairTransitions(mach, partner, state);
					if (trs != null) {
						return trs;
					}
				}
			}
		}
		return null;
	}

	private boolean addTransitions(List<int[]> trs, int[] state, int event,
			int first) {
		int[] saved = null;
		int mach = actionSharedBy[event][first];
		LTSTransitionList p = machines[mach].getStates()[state[mach]];
		if (p != null)
			saved = myclone(state, event);
		p = LTSTransitionList.firstCompState(p, event, state);
		if (first < actionSharedBy[event].length - 1) {
			if (!addTransitions(trs, state, event, first + 1))
				return false;
		} else {
			if (checker.onStack(state))
				return false;
			trs.add(state);
		}
		while (p != null) {
			int next[] = myclone(saved, event);
			p = LTSTransitionList.moreCompState(p, next);
			if (first < actionSharedBy[event].length - 1) {
				if (!addTransitions(trs, next, event, first + 1))
					return false;
			} else {
				if (checker.onStack(next))
					return false;
				trs.add(next);
			}
		}
		return true;
	}

	private List<int[]> getPairTransitions(int mach, int partner, int[] state) { // candidate==2
		List<int[]> trs = new ArrayList<>(8);
		boolean res = true;
		if (!preserveOE) {
			// get unshared transitions for mach
			BitSet tmp = getUnshared(mach, state);
			if (tmp != null)
				res = getMachTransitions(trs, mach, state, tmp);
			if (!res)
				return null;
			// get unshared transitions partner
			tmp = getUnshared(partner, state);
			if (tmp != null)
				res = getMachTransitions(trs, partner, state, tmp);
			if (!res)
				return null;
		}
		// get shared transitions with partner
		BitSet machB = new BitSet(Nactions);
		LTSTransitionList.hasEvents(machines[mach].getStates()[state[mach]], machB);
		BitSet partnerB = new BitSet(Nactions);
		LTSTransitionList
				.hasEvents(machines[partner].getStates()[state[partner]], partnerB);
		machB.and(partnerB);
		if (preserveOE && countSet(machB) != 1)
			return null;
		machB.clear(0); // tau is not shared even if present in both
		int[] actions = bitsToArray(machB);
		for (int i = 0; i < actions.length; ++i) {
			res = addTransitions(trs, myclone(state, actions[i]), actions[i], 0);
			if (!res)
				return null;
		}
		return trs;
	}

	private BitSet getUnshared(int mach, int state[]) {
		BitSet b = new BitSet(Nactions);
		Enumeration<LTSTransitionList> e = machines[mach].getStates()[state[mach]]
				.elements();
		while (e.hasMoreElements()) {
			LTSTransitionList es = (LTSTransitionList) e.nextElement();
			if (es.getEvent() == 0) {
				b.set(es.getEvent());
			} else {
				if (actionSharedBy[es.getEvent()].length == 1)
					b.set(es.getEvent());
			}
		}
		if (b.length() == 0)
			return null;
		return b;
	}

	private boolean getMachTransitions(List<int[]> trs, int mach, int[] state,
			BitSet single) {
		Enumeration<LTSTransitionList> e = machines[mach].getStates()[state[mach]]
				.elements();
		while (e.hasMoreElements()) {
			LTSTransitionList es = e.nextElement();
			if (single == null || single.get(es.getEvent())) {
				int[] next = myclone(state, es.getEvent());
				next[mach] = es.getNext();
				if (checker.onStack(next))
					return false;
				trs.add(next);
			}
		}
		return true;
	}

	// move BitSet in to an array list
	private int[] bitsToArray(BitSet b) {
		int len = countSet(b);
		if (len == 0)
			return null;
		int[] a = new int[len];
		int j = 0;
		int max = b.length();
		for (int i = 0; i < max; ++i) {
			if (b.get(i)) {
				a[j] = i;
				++j;
			}
		}
		return a;
	}

	// Count the number of bits set in a bitSet
	private int countSet(BitSet b) {
		int count = 0;
		int len = b.length();
		for (int i = 0; i < len; i++)
			if (b.get(i))
				count++;
		return count;
	}

	private int[] myclone(int[] x, int event) {
		int[] tmp = new int[x.length];
		for (int i = 0; i < x.length - 1; i++)
			tmp[i] = x[i];
		tmp[x.length - 1] = event;
		return tmp;
	}

	private void initPartners() {
		partners = new int[machines.length][];
		for (int i = 0; i < machines.length; ++i) {
			partners[i] = new int[machines[i].getStates().length];
			for (int j = 0; j < machines[i].getStates().length; ++j)
				partners[i][j] = -1;
		}
	}

	private int[][] computeCandidates() {
		int[][] cd = new int[machines.length][];
		for (int i = 0; i < machines.length; ++i) {
			cd[i] = new int[machines[i].getStates().length];
			for (int j = 0; j < machines[i].getStates().length; ++j) {
				int[] actions = LTSTransitionList.localEnabled(machines[i].getStates()[j]);
				cd[i][j] = candidateNumber(i, j, actions);
			}
		}
		return cd;
	}

	/*
	 * the state of a process has a candidate number of: 0 if it is not a
	 * candidate for an ample set 1 if all the enabled actions are not shared
	 * with other processes 2 if all the enabled actions are shared with one
	 * other process this "partner" is recorded in partners.
	 */
	private int candidateNumber(int mach, int state, int[] actions) {
		if (actions == null)
			return 0;
		if (preserveOE && LTSTransitionList.hasNonDet(machines[mach].getStates()[state]))
			return 0;
		int cn = 0;
		int singles = 0;
		int partner = -1;
		for (int i = 0; i < actions.length; ++i) {
			int shared = 0;
			int a = actions[i];
			if (visible.get(a))
				return 0; // eliminate if contains visible action
			if (high != null && !high.get(a))
				return 0; // eliminate if contains low priority action (if
							// priority)
			if (a == 0)
				shared = 1; // tau
			else
				shared = actionSharedBy[a].length;
			if (shared == 1)
				++singles;
			if (shared > cn)
				cn = shared;
			if (cn > 2)
				return 0;
			if (shared == 2) {
				if (partner < 0) {
					partner = getPartner(mach, a);
				} else {
					if (partner != getPartner(mach, a))
						return 0;
				}
			}
		}
		if (preserveOE && (singles > 1 || (cn == 2 && singles > 0)))
			return 0;
		if (cn == 2)
			partners[mach][state] = partner;
		return cn;
	}

	private int getPartner(int mach, int event) {
		if (actionSharedBy[event][0] == mach)
			return actionSharedBy[event][1];
		else
			return actionSharedBy[event][0];
	}
}
