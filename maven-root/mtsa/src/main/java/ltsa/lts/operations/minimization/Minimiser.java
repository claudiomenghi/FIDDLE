package ltsa.lts.operations.minimization;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Map;

import ltsa.lts.EventStateUtils;
import ltsa.lts.automata.lts.LTSConstants;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.MarkedCompactState;
import ltsa.lts.csp.Declaration;
import ltsa.lts.operations.determinization.Determinizer;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.util.Counter;

import com.google.common.base.Preconditions;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Map;

public class Minimiser {

	final static int TAU = 0;

	BitSet[] E; // array of |states| x |states| bits
	BitSet[] A; // array of |states| x |actions| bits
	LTSTransitionList[] T; // tau adjacency lists - stores reflexive transitive closure
	LabelledTransitionSystem machine;
	LTSOutput output;

	public Minimiser(LabelledTransitionSystem c, LTSOutput output) {
		machine = c;
		this.output = output;
	}

	// initialise T with transitive closure of tau in machine
	private void initTau() {
		T = new LTSTransitionList[machine.getStates().length];
		for (int i = 0; i < T.length; i++) {
			T[i] = LTSTransitionList.reachableTau(machine.getStates(), i);
		}
	}

	// G=>G' using T
	private LabelledTransitionSystem machTau(LabelledTransitionSystem m) {
		// do T* a pass

		// agrega a cada estado en el path los estados alcanzables
		for (int i = 0; i < m.getStates().length; i++)
			m.getStates()[i] = LTSTransitionList.tauAdd(m.getStates()[i], T);
		for (int i = 0; i < m.getStates().length; i++) {
			// agrega los estados alcanzables como vecinos del estado
			m.getStates()[i] = EventStateUtils.union(m.getStates()[i], T[i]);
			m.getStates()[i] = LTSTransitionList.actionAdd(m.getStates()[i], m.getStates());
		}
		for (int i = 0; i < m.getStates().length; i++)
			m.getStates()[i] = EventStateUtils.add(m.getStates()[i], new LTSTransitionList(Declaration.TAU, i));
		output.out(".");
		return m;
	}

	public LabelledTransitionSystem removeTau(LabelledTransitionSystem m) {
		for (int i = 0; i < m.getStates().length; i++)
			m.getStates()[i] = LTSTransitionList.removeTau(m.getStates()[i]);
		return m;
	}

	// first step in initialisation is set up E
	private void initialise() {
		// initialise A such that A[i,a] is true if transition a from state i
		A = new BitSet[machine.getMaxStates()];
		for (int i = 0; i < A.length; i++) {
			A[i] = new BitSet(machine.getAlphabet().length);
			LTSTransitionList.setActions(machine.getStates()[i], A[i]);
		}
		E = new BitSet[machine.getMaxStates()];
		for (int i = 0; i < E.length; i++)
			E[i] = new BitSet(E.length);
		// set E[i,j] if A[i] = A[j] ie same set of transitions
		for (int i = 0; i < E.length; i++) {
			E[i].set(i);
			for (int j = 0; j < i; j++)
				if (A[i].equals(A[j])) {
					E[i].set(j);
					E[j].set(i);
				}
		}
		output.out(".");
	}

	private void dominimise() {
		boolean more = true;
		;
		while (more) {
			output.out(".");
			more = false;
			for (int i = 0; i < E.length; i++) {
				Thread.yield();
				for (int j = 0; j < i; j++)
					if (E[i].get(j)) {
						boolean b = is_equivalent(i, j) && is_equivalent(j, i);
						if (!b) {
							more = true;
							E[i].clear(j);
							E[j].clear(i);
						}
					}
			}
		}
	}

	public LabelledTransitionSystem minimiseTauClousure() {
		LabelledTransitionSystem minimise = this.minimise();
		this.removeTau(minimise);
		return minimise;
	}

	/*
	 * minimise using observational equivalence
	 */
	public LabelledTransitionSystem minimise() {
		
		System.out.println("DDDDD 1");
		//// Added to make minimisation of CompositeState and LabelledTransitionSystem
		//// be consistent.
		if (CompositeState.reduceFlag) {
			output.outln("Tau reduction ON");
			machine.removeNonDetTau();
		}
		output.out(machine.getName() + " minimising");
		long start = System.currentTimeMillis();
		LabelledTransitionSystem saved = machine.myclone();
		/* distinguish end state from STOP with self transition using special label */
		if (machine.getEndOfSequenceIndex() >= 0) {
			int es = machine.getEndOfSequenceIndex();
			machine.getStates()[es] = EventStateUtils.add(machine.getStates()[es],
					new LTSTransitionList(machine.getAlphabet().length, es));
		}
		if (machine.hasTau()) {
			initTau();
			machine = machTau(machine);
			T = null; // release storage
		}
		initialise();
		dominimise();
		/*
		 * makeNewMachine() uses machine. If first overwrite machine with saved you
		 * loose minimization machine = saved; LabelledTransitionSystem c =
		 * makeNewMachine();
		 */
		LabelledTransitionSystem c = makeNewMachine();
		machine = saved;
		long finish = System.currentTimeMillis();
		output.outln("");
		output.outln("Minimised getStates(): " + c.getMaxStates() + " in " + (finish - start) + "ms");
		return c;
	}

	/*
	 * generate minimized trace equivalent deterministic automata
	 */
	public LabelledTransitionSystem trace_minimise() {
		boolean must_minimize = false;
		// convert to trace equivalent NFA without tau
		if (machine.hasTau()) {
			must_minimize = true;
			output.out("Eliminating tau");
			initTau();
			machine = machTau(machine);
			machine = removeTau(machine);
			T = null; // release storage
		}
		// convert NFA to DFA
		if (must_minimize || machine.isNonDeterministic()) {
			must_minimize = true;
			Determinizer d = new Determinizer(machine, output);
			machine = d.determine();
		}
		// now minimise
		if (must_minimize)
			return minimise();
		else
			return machine;
	}

	private boolean is_equivalent(int i, int j) {
		LTSTransitionList p = machine.getStates()[i];
		while (p != null) {
			LTSTransitionList tr = p;
			while (tr != null) {
				if (!findSuccessor(j, tr))
					return false;
				tr = tr.getNondet();
			}
			p = p.getList();
		}
		return true;
	}

	private boolean findSuccessor(int j, LTSTransitionList tr) {
		LTSTransitionList p = machine.getStates()[j]; // find event
		while (p.getEvent() != tr.getEvent())
			p = p.getList();
		while (p != null) {
			if (tr.getNext() < 0) {
				if (p.getNext() < 0)
					return true;
			} else {
				if (p.getNext() >= 0) {
					// pregunto en la matriz de las maybes si tiene transiciones al reves
					if (E[tr.getNext()].get(p.getNext()))
						return true;
				}
			}
			p = p.getNondet();
		}
		return false;
	}

	private LabelledTransitionSystem makeNewMachine() {
		Hashtable oldtonew = new Hashtable();
		Hashtable newtoold = new Hashtable();
		Counter newSt = new Counter(0);
		for (int i = 0; i < E.length; i++) {
			Integer oldIndex = new Integer(i);
			Integer newIndex = (Integer) oldtonew.get(oldIndex);
			if (newIndex == null) {
				oldtonew.put(oldIndex, newIndex = newSt.label());
				newtoold.put(newIndex, oldIndex);
			}
			for (int j = 0; j < E.length; j++) {
				if (E[i].get(j))
					oldtonew.put(new Integer(j), newIndex);
			}
		}
		LabelledTransitionSystem m = new LabelledTransitionSystem(machine.getName());
		int maxStates = newtoold.size();
		m.setAlphabet(machine.getAlphabet());
		m.setStates(new LTSTransitionList[maxStates]);
		if (machine.getEndOfSequenceIndex() < 0)
			m.setEndOfSequence(machine.getEndOfSequenceIndex());
		else {
			m.setEndOfSequence(((Integer) oldtonew.get(new Integer(machine.getEndOfSequenceIndex()))).intValue());
			/* remove marking transition */
			m.getStates()[m.getEndOfSequenceIndex()] = LTSTransitionList.remove(
					m.getStates()[m.getEndOfSequenceIndex()],
					new LTSTransitionList(m.getAlphabet().length, m.getEndOfSequenceIndex()));
		}

		for (int i = 0; i < machine.getMaxStates(); i++) {
			int newi = ((Integer) oldtonew.get(new Integer(i))).intValue();
			LTSTransitionList tmp = EventStateUtils.renumberStates(machine.getStates()[i], oldtonew);
			m.getStates()[newi] = EventStateUtils.union(m.getStates()[newi], tmp);
		}

		for (int i = 0; i < m.getMaxStates(); i++) // remove reflexive tau
			m.getStates()[i] = LTSTransitionList.remove(m.getStates()[i], new LTSTransitionList(Declaration.TAU, i));
		LabelledTransitionSystem response = handleMarkedCompactState(m, machine, oldtonew);
		return response;
	}

	private LabelledTransitionSystem handleMarkedCompactState(LabelledTransitionSystem m,
			LabelledTransitionSystem machine, Map<Integer, Integer> oldToNew) {
		LabelledTransitionSystem response = m;
		if (machine instanceof MarkedCompactState) {
			int[] markedStates = ((MarkedCompactState) machine).getMarkedStates();
			for (int i = 0; i < markedStates.length; ++i) {
				markedStates[i] = oldToNew.get(markedStates[i]);
			}
			response = new MarkedCompactState(m, markedStates);
		}
		return response;
	}

	public void print(LTSOutput output) {
		privPrint(output, E);
	}

	private void privPrint(LTSOutput output, BitSet[] E) {
		if (E.length > 20)
			return;
		char[] buf = new char[E.length * 2];
		for (int i = 0; i < E.length * 2; i++)
			buf[i] = ' ';
		output.outln("E:");
		output.out("       ");
		for (int i = 0; i < E.length; i++)
			output.out(" " + i);
		output.outln("");
		for (int i = 0; i < E.length; i++) {
			output.out("State " + i + " ");
			for (int j = 0; j < E.length; j++)
				if (E[i].get(j))
					buf[j * 2] = '1';
				else
					buf[j * 2] = ' ';
			output.outln(new String(buf));
		}
	}

}