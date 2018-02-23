package ltsa.lts.operations.determinization;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import ltsa.lts.EventStateUtils;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.csp.Declaration;
import ltsa.lts.output.LTSOutput;

/*
 * the class computes a Deterministic Finite State Automata
 * from a deterministic  finite state automata
 * reference "Introduction to Automata Theory, Languages and Computation"
 * John e. Hopcroft & Jeffrey D. Ullman p 21-23
 *
 * non-deterministic transitions to ERROR state are disgarded
 * (but not with Dimitra's mod)
 * this treats ERROR in the same way as STOP
 */

public class Determinizer {

	final static int TAU = 0;

	LabelledTransitionSystem machine;
	LTSOutput output;

	Vector<LTSTransitionList> newStates; // list of newStates, indexed
											// transition lists
	// (EventState)
	Vector<BitSet> stateSets; // list of sets of oldStates
	Hashtable<BitSet, Integer> map; // maps sets of oldstates (BitSet) -> new
									// state (Integer)

	int nextState; // next new state number
	int currentState; // current state being computed

	public Determinizer(LabelledTransitionSystem c, LTSOutput output) {
		machine = c;
		this.output = output;
	}

	public LabelledTransitionSystem determine() {
		output.outln("make DFA(" + machine.getName() + ")");
		newStates = new Vector<>(machine.getMaxStates() * 2);
		stateSets = new Vector<>(machine.getMaxStates() * 2);
		map = new Hashtable<>(machine.getMaxStates() * 2);
		nextState = 0;
		currentState = 0;
		BitSet st = new BitSet();
		st.set(0); // start state is set with state 0
		addState(st);
		while (currentState < nextState) {
			compute(currentState);
			++currentState;
		}
		return makeNewMachine();
	}

	protected void compute(int n) {
		BitSet state = stateSets.elementAt(n);
		LTSTransitionList tr = null; // the set of all transitions from this
										// state set
		LTSTransitionList newtr = null; // the new transitions from the new
										// state
		for (int i = 0; i < state.size(); ++i) {
			if (state.get(i))
				tr = EventStateUtils.union(tr, machine.getStates()[i]);
		}
		LTSTransitionList action = tr;
		while (action != null) { // for each action
			boolean errorState = false;
			BitSet newState = new BitSet();
			/*
			 * if (action.next!=Declaration.ERROR) newState.set(action.next);
			 * else errorState = true; EventState nd = action.nondet; while
			 * (nd!=null) { if(nd.next!=Declaration.ERROR) {
			 * newState.set(nd.next); errorState = false; } nd=nd.nondet; }
			 */
			// change for Dimitra
			if (action.getNext() != Declaration.ERROR)
				newState.set(action.getNext());
			else
				errorState = true;
			LTSTransitionList nd = action.getNondet();
			while (nd != null) {
				if (nd.getNext() != Declaration.ERROR) {
					newState.set(nd.getNext());
					// errorState = false;
				} else
					errorState = true;
				nd = nd.getNondet();
			}
			int newStateId;
			if (errorState)
				newStateId = Declaration.ERROR;
			else
				newStateId = addState(newState);
			newtr = EventStateUtils.add(newtr,
					new LTSTransitionList(action.getEvent(), newStateId));
			action = action.getList();
		}
		newStates.addElement(newtr);
	}

	protected int addState(BitSet bs) {
		Integer ii = (Integer) map.get(bs);
		if (ii != null)
			return ii.intValue();
		map.put(bs, new Integer(nextState));
		stateSets.addElement(bs);
		++nextState;
		return nextState - 1;
	}

	protected LabelledTransitionSystem makeNewMachine() {
		LabelledTransitionSystem m = new LabelledTransitionSystem(machine.getName(), nextState);
		String[] newAlphabet = new String[machine.getAlphabet().length];
		for (int i = 0; i < machine.getAlphabet().length; i++) {
			newAlphabet[i] = machine.getAlphabet()[i];
		}

		m.setAlphabet(newAlphabet);
		m.setStates(new LTSTransitionList[m.getMaxStates()]);
		for (int i = 0; i < m.getMaxStates(); i++) {
			m.getStates()[i] = newStates.elementAt(i);
		}
		// compute new end state if any
		if (machine.getEndOfSequenceIndex() >= 0) {
			BitSet es = new BitSet();
			es.set(machine.getEndOfSequenceIndex());
			Integer ii = map.get(es);
			if (ii != null)
				m.setEndOfSequence(ii.intValue());
		}
		output.outln("DFA(" + machine.getName() + ") has " + m.getMaxStates()
				+ " states.");
		return m;
	}

}
