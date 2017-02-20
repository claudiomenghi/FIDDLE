package ltsa.lts.automata.lts.state;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

import ltsa.lts.EventStateUtils;
import ltsa.lts.animator.ModelExplorerContext;
import ltsa.lts.automata.automaton.Automata;
import ltsa.lts.automata.lts.LTSConstants;
import ltsa.lts.csp.Declaration;
import ltsa.lts.csp.Relation;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.operations.composition.parallel.StackCheck;
import ltsa.lts.util.Counter;
import ltsa.lts.util.MTSUtils;
import ltsa.lts.util.collections.MyIntHash;
import ltsa.lts.util.collections.MyList;
import ltsa.lts.util.collections.StateMap;

/**
 * contains a Labeled Transition System (LTS).<br>
 * The states of the LTS are stored into the states attribute. <br>
 * The alphabet of the automaton is stored in an appropriate attribute. <br>
 * A boolean variable specified whether the automaton specifies a property
 *
 */
public class LabelledTransitionSystem implements Automata {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The name of the LTS
	 */
	private String name;

	/**
	 * maps each box to the corresponding index
	 */
	private Map<String, Integer> boxIndexes = new HashMap<>();

	/**
	 * maps each box to the corresponding set of String, i.e., the interface of
	 * the automaton
	 */
	private Map<String, Set<String>> mapBoxInterface = new HashMap<>();

	/**
	 * The alphabet of the automaton
	 */
	private String[] alphabet;

	/**
	 * contains for each state (integer position) the list of transitions that
	 * exit that state contained in the LTSTransitionList
	 */
	private LTSTransitionList[] states;

	/**
	 * contains the index of the final states
	 */
	private Set<Integer> finalStateIndexes;

	/**
	 * contains the number of end of sequence state if any
	 */
	private int endseq = LTSConstants.NO_SEQUENCE_FOUND;

	/**
	 * make every state have transitions to ERROR state for actions not already
	 * declared from that state properties can terminate in any state,however,
	 * we set no end state
	 */
	private boolean prop = false;

	private boolean hasDuplicates = false;

	/**
	 * create a new compact state
	 * 
	 * @param name
	 *            the name of the state
	 * @throws NullPointerException
	 *             if the name of the state is null
	 */
	public LabelledTransitionSystem(String name) {
		Preconditions.checkNotNull(name, "The name cannot be null");
		this.name = name;
		this.finalStateIndexes = new HashSet<>();
		this.boxIndexes = new HashMap<>();
	}

	public void removeTransitionsLabeledWithEvents(Set<String> events) {

		events.forEach(event -> {
			int eventIndex = this.getEvent(event);
			for (int i = 0; i < this.states.length; i++)
				this.states[i] = LTSTransitionList.removeEvent(this.states[i], eventIndex);
		});
	}

	public LabelledTransitionSystem(String name, int maxStates) {
		this(name);
		Preconditions.checkArgument(maxStates >= 0, "The maximum number of states mus be >=0");
		this.states = new LTSTransitionList[maxStates];
	}

	public LabelledTransitionSystem(ModelExplorerContext context, String name, StateMap statemap, MyList transitions,
			String[] alphabet, int endSequence) {
		this(name);

		this.alphabet = alphabet;
		this.states = new LTSTransitionList[context.stateCount];

		while (!transitions.empty()) {
			int fromState = transitions.getFrom();
			int toState = transitions.getTo() == null ? -1 : statemap.get(transitions.getTo());

			this.states[fromState] = EventStateUtils.add(states[fromState],
					new LTSTransitionList(transitions.getAction(), toState));

			transitions.next();
		}
		for (byte[] finalState : context.finalStates) {
			this.addFinalStateIndex(statemap.get(finalState));
		}
		endseq = endSequence;
	}

	public LabelledTransitionSystem(int stateNumber, String name, StateMap statemap, MyList transitions,
			String[] alphabet, int endSequence) {
		this(name);

		this.alphabet = alphabet;
		this.states = new LTSTransitionList[stateNumber];
		while (!transitions.empty()) {
			int fromState = transitions.getFrom();
			int toState = transitions.getTo() == null ? -1 : statemap.get(transitions.getTo());

			this.states[fromState] = EventStateUtils.add(states[fromState],
					new LTSTransitionList(transitions.getAction(), toState));

			transitions.next();
		}

		endseq = endSequence;
	}

	public void addFinalStateIndex(Integer index) {
		this.finalStateIndexes.add(index);
	}

	public Set<Integer> getFinalStateIndexes() {
		return this.finalStateIndexes;
	}

	/**
	 * 
	 * @param stateIndex
	 *            the index of one of the states of the automaton or a
	 *            NO_END_STATE value
	 * @throws IllegalArgumentException
	 *             if the value passed as parameter is neither one of the states
	 *             of the automaton nor a NO_END_STATE value
	 */
	public void setEndOfSequence(int stateIndex) {
		Preconditions.checkArgument(states.length > stateIndex || stateIndex == LTSConstants.NO_SEQUENCE_FOUND,
				"The index of the state is not a valid index of a state");
		this.endseq = stateIndex;
	}

	/**
	 * returns the name of the boxes of the LTS
	 * 
	 * @return the name of the boxes of the LTS
	 */
	public Set<String> getBoxes() {
		return Collections.unmodifiableSet(this.mapBoxInterface.keySet());
	}

	/**
	 * adds the specified event to the set of events of the automaton
	 * 
	 * @param event
	 *            the event to be added
	 * @return the index where the event is added the index of the event if it
	 *         is already present
	 */
	public int addEvent(String event) {
		if (alphabet == null) {
			this.alphabet = new String[1];
		} else {
			for (int i = 0; i < alphabet.length; i++) {
				if (alphabet[i].equals(event)) {
					return i;
				}
			}
			alphabet = Arrays.copyOf(alphabet, alphabet.length + 1);
		}
		alphabet[alphabet.length - 1] = event;
		return alphabet.length - 1;
	}

	public int getEndOfSequenceIndex() {
		return endseq;
	}

	public void addBoxIndex(String boxName, Integer boxPosition) {
		this.boxIndexes.put(boxName, boxPosition);
		this.mapBoxInterface.put(boxName, new HashSet<String>());
	}

	/**
	 * returns the characters of the automata
	 * 
	 * @return
	 */
	public List<String> getAlphabetEvents() {
		List<String> alphabetCharacters = new ArrayList<>();
		for (int i = 0; i < this.alphabet.length; i++) {
			if (!this.alphabet[i].startsWith("?") && !this.alphabet[i].endsWith("?")) {
				alphabetCharacters.add(this.alphabet[i]);
			}
		}
		return alphabetCharacters;
	}

	/**
	 * returns the maximum number of states the automaton can contain
	 * 
	 * @return the maximum number of states the automaton can contain
	 */
	public int getMaxStates() {
		return this.states.length;
	}

	public void setAlphabet(String[] alphabet) {
		this.alphabet = alphabet;
	}

	public String getName() {
		return this.name;
	}

	public void reachable() {
		MyIntHash otn = EventStateUtils.reachable(states);

		LTSTransitionList[] oldStates = states;
		states = new LTSTransitionList[otn.size()];
		for (int oldi = 0; oldi < oldStates.length; ++oldi) {
			int newi = otn.get(oldi);
			if (newi > -2) {
				states[newi] = EventStateUtils.renumberStates(oldStates[oldi], otn);
			}
		}
		if (endseq > 0)
			endseq = otn.get(endseq);
	}

	// change (a ->(tau->P|tau->Q)) to (a->P | a->Q)
	public void removeNonDetTau() {
		if (!hasTau())
			return;
		while (true) {
			boolean canRemove = false;
			for (int i = 0; i < this.states.length; i++)
				// remove reflexive tau
				// remove reflexive tau
				states[i] = LTSTransitionList.remove(states[i], new LTSTransitionList(Declaration.TAU, i));
			// oStates[i] = EventState.remove(oStates[i],new
			// EventState(Declaration.TAU_MAYBE,i));
			BitSet tauOnly = new BitSet(this.states.length);
			for (int i = 1; i < this.states.length; ++i) {
				if (LTSTransitionList.hasOnlyTauAndAccept(states[i], alphabet)) {
					tauOnly.set(i);
					canRemove = true;
				}
			}
			if (!canRemove)
				return;
			for (int i = 0; i < this.states.length; ++i) {
				if (!tauOnly.get(i))
					states[i] = LTSTransitionList.addNonDetTau(states[i], states, tauOnly);
			}
			int oldSize = this.states.length;
			reachable();
			if (oldSize == this.states.length)
				return;
		}
	}

	public void removeDetCycles(String action) {
		int act = eventNo(action);
		if (act >= alphabet.length)
			return;
		for (int i = 0; i < states.length; ++i) {
			if (!LTSTransitionList.hasNonDetEvent(states[i], act))
				states[i] = LTSTransitionList.remove(states[i], new LTSTransitionList(act, i));
		}
	}

	/**
	 * check if has only single terminal accept state also if no accept states -
	 * treats as safety property so that TRUE generates a null constraint
	 * 
	 * @return
	 */
	public boolean isSafetyOnly() {
		int terminalAcceptStates = 0;
		int acceptStates = 0;
		for (int i = 0; i < this.states.length; i++) {
			if (LTSTransitionList.isAccepting(states[i], alphabet)) {
				++acceptStates;
				if (LTSTransitionList.isTerminal(i, states[i]))
					++terminalAcceptStates;
			}
		}
		return (terminalAcceptStates == 1 && acceptStates == 1) || acceptStates == 0;
	}

	public void makeSafety() {
		int acceptState = -1;
		for (int i = 0; i < this.states.length; i++) {
			if (LTSTransitionList.isAccepting(states[i], alphabet)) {
				acceptState = i;
				break;
			}
		}
		if (acceptState >= 0)
			states[acceptState] = LTSTransitionList.removeAccept(states[acceptState]);
		for (int i = 0; i < this.states.length; i++) {
			LTSTransitionList.replaceWithError(states[i], acceptState);
		}
		reachable();
	}

	/**
	 * removes the state and all the transitions with source/destination that
	 * state
	 * 
	 * @param stateIndex
	 *            the index of the state to be removed
	 * @throws IllegalArgumentException
	 *             if the index does not correspond to an index of the state of
	 *             the LTS
	 */
	public void removeStates(Collection<Integer> stateIndexes) {
		// removing the transitions that reach the states to be removed
		for (int stateIndex = 0; stateIndex < this.states.length; stateIndex++) {

			if (stateIndexes.contains(stateIndex)) {
				this.states[stateIndex] = null;
			} else {
				this.states[stateIndex] = LTSTransitionList.removeTransToState(this.states[stateIndex], stateIndexes);
			}
		}
	}

	/**
	 * remove acceptance from states with only outgoing tau
	 */
	public void removeAcceptTau() {
		for (int i = 1; i < this.states.length; ++i) {
			if (LTSTransitionList.hasOnlyTauAndAccept(states[i], alphabet)) {
				this.states[i] = LTSTransitionList.removeAccept(states[i]);
			}
		}
	}

	public boolean hasERROR() {
		for (int i = 0; i < this.states.length; i++) {
			if (LTSTransitionList.hasState(states[i], Declaration.ERROR)) {
				return true;
			}
		}
		return false;
	}

	public void prefixLabels(String prefix) {
		this.name = prefix + ":" + name;
		// BUGFIX don't prefix tau nor tau?
		int i = 1;
		if (alphabet[Declaration.TAU_MAYBE] != null && alphabet[Declaration.TAU_MAYBE].equals("tau?")) {
			i = 2;
		}
		for (; i < alphabet.length; i++) { // don't prefix tau
			if (!alphabet[i].equals(LTLf2LTS.endSymbol.getValue())
					&& !alphabet[i].equals(LTLf2LTS.initSymbol.getValue())) {
				String old = alphabet[i];
				alphabet[i] = prefix + "." + old;
			}
		}
	}

	public boolean relabelDuplicates() {
		return hasDuplicates;
	}

	

	public int getLabelIndex(String oldLabel) {
		int oldLabelIndex = -1;
		for (int i = 0; i < alphabet.length; i++) {
			if (alphabet[i] == oldLabel) {
				oldLabelIndex = i;
			}
		}
		return oldLabelIndex;
	}

	public void relabelAndRemoveOldLabel(String oldLabel, String newLabel) {

		int oldLabelIndex = this.getLabelIndex(oldLabel);

		if (oldLabelIndex == -1) {
			throw new IllegalArgumentException("label " + oldLabel + " not contained in the alphabet of the automaton");
		}

		int newLabelIndex = -1;
		for (int i = 0; i < alphabet.length; i++) {
			if (alphabet[i] == newLabel) {
				newLabelIndex = i;
			}
		}
		if (newLabelIndex == -1) {
			throw new IllegalArgumentException("label " + newLabel + " not contained in the alphabet of the automaton");
		}
		boolean found = false;
		Hashtable<Integer, Integer> oldToNew = new Hashtable<>();

		String[] oldAlphabet = alphabet;
		this.alphabet = new String[this.alphabet.length - 1];
		for (int i = 0; i < oldAlphabet.length; i++) {
			if (oldAlphabet[i].equals(oldLabel)) {
				found = true;
				oldToNew.put(i, newLabelIndex);
			} else {
				if (found) {
					this.alphabet[i - 1] = oldAlphabet[i];
					oldToNew.put(i, i - 1);
				} else {
					this.alphabet[i] = oldAlphabet[i];
					oldToNew.put(i, i);
				}
			}
		}

		for (int i = 0; i < this.states.length; i++) {

			this.states[i] = LTSTransitionList.renumberEvents(this.states[i], oldToNew);
		}

	}

	public void relabelAndKeepOldLabel(String oldLabel, String newLabel) {

		int oldLabelIndex = -1;
		for (int i = 0; i < alphabet.length; i++) {
			if (alphabet[i] == oldLabel) {
				oldLabelIndex = i;
			}
		}
		if (oldLabelIndex == -1) {
			throw new IllegalArgumentException("label " + oldLabel + " not contained in the alphabet of the automaton");
		}

		int newLabelIndex = -1;
		for (int i = 0; i < alphabet.length; i++) {
			if (alphabet[i] == newLabel) {
				newLabelIndex = i;
			}
		}
		if (newLabelIndex == -1) {
			throw new IllegalArgumentException("label " + newLabel + " not contained in the alphabet of the automaton");
		}
		Hashtable<Integer, Integer> oldToNew = new Hashtable<>();

		for (int i = 0; i < this.alphabet.length; i++) {
			oldToNew.put(i, i);
		}
		oldToNew.put(oldLabelIndex, newLabelIndex);

		for (int i = 0; i < this.states.length; i++) {
			this.states[i] = LTSTransitionList.renumberEvents(this.states[i], oldToNew);
		}
	}

	public void relabel(Relation oldtonew) {
		hasDuplicates = false;
		if (oldtonew.isRelation())
			relationalRelabel(oldtonew);
		else
			functionalRelabel(oldtonew);
	}

	// now used only for incremental minimization
	public Vector<String> hide(Vector<String> toShow) {
		Vector<String> toHide = new Vector<String>();
		for (int i = 1; i < alphabet.length; i++) {
			if (!contains(alphabet[i], toShow))
				toHide.addElement(alphabet[i]);
		}
		return toHide;
	}

	/**
	 * hides every event but the ones in toShow
	 * 
	 * @param toShow
	 */
	public void expose(Collection<String> toShow) {
		BitSet visible = new BitSet(alphabet.length);
		for (int i = 1; i < alphabet.length; ++i) {
			if (contains(alphabet[i], toShow)) {
				visible.set(i);
			}
		}
		visible.set(0);
		visible.set(1);
		dohiding(visible);
	}

	public void conceal(Vector<String> toHide) {
		BitSet visible = new BitSet(alphabet.length);
		for (int i = 1; i < alphabet.length; ++i) {
			if (!contains(alphabet[i], toHide))
				visible.set(i);
		}
		visible.set(0);
		dohiding(visible);
	}

	public static boolean contains(String action, Collection<String> v) {
		for (String s : v) {
			if (s.equals(action) || isPrefix(s, action)) {
				return true;
			}
		}
		return false;
	}

	public boolean isProperty() {
		return this.prop;
	}

	public void makeProperty() {
		endseq = -9999;
		prop = true;
		for (int i = 0; i < this.states.length; i++)
			this.states[i] = LTSTransitionList.addTransToError(this.states[i], alphabet.length);
	}

	public void unMakeProperty() {
		endseq = -9999;
		prop = false;
		for (int i = 0; i < this.states.length; i++)
			this.states[i] = LTSTransitionList.removeTransToError(this.states[i]);
	}

	public boolean isNonDeterministic() {
		for (int i = 0; i < this.states.length; i++)
			if (LTSTransitionList.hasNonDet(states[i]))
				return true;
		return false;
	}

	// output LTS in aldebaran format
	public void printAUT(PrintStream out) {

		out.print("des(0," + ntransitions() + "," + this.states.length + ")\n");
		for (int i = 0; i < states.length; i++)
			EventStateUtils.printAUT(states[i], i, alphabet, out);
	}

	public LabelledTransitionSystem myclone() {
		LabelledTransitionSystem m = new LabelledTransitionSystem(this.getName());
		m.endseq = endseq;
		m.prop = prop;
		m.boxIndexes = new HashMap<>(this.boxIndexes);
		m.mapBoxInterface = new HashMap<>(this.mapBoxInterface);

		m.alphabet = new String[alphabet.length];
		System.arraycopy(alphabet, 0, m.alphabet, 0, alphabet.length);
		m.states = new LTSTransitionList[this.states.length];
		for (int i = 0; i < states.length; i++) {
			if (states[i] == null)
				m.states[i] = null;
			else
				m.states[i] = LTSTransitionList.copy(states[i]);
		}
		m.finalStateIndexes = new HashSet<>(this.finalStateIndexes);

		return m;
	}

	public int ntransitions() {
		int count = 0;
		for (int i = 0; i < states.length; i++) {
			count += EventStateUtils.count(states[i]);
		}
		return count;
	}

	public boolean hasTau() {
		for (int i = 0; i < states.length; ++i) {
			if (LTSTransitionList.hasTau(states[i]))
				return true;
		}
		return false;
	}

	/* ------------------------------------------------------------ */

	public boolean isErrorTrace(Vector<?> trace) {
		boolean hasError = false;
		for (int i = 0; i < this.states.length && !hasError; i++)
			if (LTSTransitionList.hasState(states[i], Declaration.ERROR))
				hasError = true;
		if (!hasError)
			return false;
		return isTrace(trace, 0, 0);
	}

	/* --------------------------------------------------------------- */

	/*
	 * addAcess extends the alphabet by creating a new copy of the alphabet for
	 * each prefix string in pset. Each transition is replicated acording to the
	 * number of prefixes and renumbered with the new action number.
	 */

	public void addAccess(Vector<?> pset) {
		int n = pset.size();
		if (n == 0)
			return;
		String s = "{";
		LabelledTransitionSystem machs[] = new LabelledTransitionSystem[n];
		Enumeration<?> e = pset.elements();
		int i = 0;
		while (e.hasMoreElements()) {
			String prefix = (String) e.nextElement();
			s = s + prefix;
			machs[i] = myclone();
			machs[i].prefixLabels(prefix);
			i++;
			if (i < n)
				s = s + ",";
		}
		// new name
		name = s + "}::" + name;
		// new alphabet
		int alphaN = alphabet.length - 1;
		alphabet = new String[(alphaN * n) + 1];
		alphabet[0] = "tau";
		for (int j = 0; j < n; j++) {
			for (int k = 1; k < machs[j].alphabet.length; k++) {
				alphabet[alphaN * j + k] = machs[j].alphabet[k];
			}
		}
		// additional transitions
		for (int j = 1; j < n; j++) {
			for (int k = 0; k < this.states.length; k++) {
				LTSTransitionList.offsetEvents(machs[j].states[k], alphaN * j);
				states[k] = EventStateUtils.union(states[k], machs[j].states[k]);
			}
		}
	}

	/* --------------------------------------------------------------- */

	public boolean hasLabel(String label) {
		for (int i = 0; i < alphabet.length; ++i)
			if (label.equals(alphabet[i]))
				return true;
		return false;
	}

	public boolean usesLabel(String label) {
		if (!hasLabel(label))
			return false;
		int en = eventNo(label);
		for (int i = 0; i < states.length; ++i) {
			if (LTSTransitionList.hasEvent(states[i], en))
				return true;
		}
		return false;
	}

	/* --------------------------------------------------------------- */

	public boolean isSequential() {
		return endseq >= 0;
	}

	public boolean isEnd() {
		return this.states.length == 1 && endseq == 0;
	}

	/*----------------------------------------------------------------*/

	/*----------------------------------------------------------------*/

	public void expandSequential(HashMap<?, ?> inserts) {
		int ninserts = inserts.size();
		LabelledTransitionSystem machines[] = new LabelledTransitionSystem[ninserts + 1];
		int insertAt[] = new int[ninserts + 1];
		machines[0] = this;
		int index = 1;
		Iterator<?> e = inserts.keySet().iterator();
		while (e.hasNext()) {
			Integer ii = (Integer) e.next();
			LabelledTransitionSystem m = (LabelledTransitionSystem) inserts.get(ii);
			machines[index] = m;
			insertAt[index] = ii.intValue();
			++index;
		}
		// new alphabet
		alphabet = sharedAlphabet(machines);
		// copy inserted machines
		for (int i = 1; i < machines.length; ++i) {
			int offset = insertAt[i];
			for (int j = 0; j < machines[i].states.length; ++j) {
				states[offset + j] = machines[i].states[j];
			}
		}
	}

	public void offsetSeq(int offset, int finish) {
		for (int i = 0; i < states.length; i++) {
			LTSTransitionList.offsetSeq(offset, endseq, finish, states[i]);
		}
	}

	public int addInitialState() {

		LTSTransitionList[] oldStates = this.states;
		this.states = new LTSTransitionList[oldStates.length + 1];
		this.states[0] = null;
		for (int i = 0; i < oldStates.length; i++) {
			this.states[i + 1] = oldStates[i];
		}
		this.addOffset(1);
		return 0;
	}

	private void addOffset(int offset) {
		for (int i = 0; i < states.length; i++) {
			states[i] = this.offsetSeq(offset, states[i]);
		}
		Set<Integer> oldFinalStateIndexes = this.finalStateIndexes;
		this.finalStateIndexes = new HashSet<>();
		oldFinalStateIndexes.forEach(index -> this.finalStateIndexes.add(index + offset));

		Map<String, Integer> oldBoxIndexes = this.boxIndexes;
		this.boxIndexes = new HashMap<>();
		oldBoxIndexes.entrySet().forEach(e -> this.boxIndexes.put(e.getKey(), e.getValue() + offset));

		this.endseq = this.endseq + offset;

	}

	
	private LTSTransitionList offsetSeq(int offset, LTSTransitionList head) {
		LTSTransitionList listIterator = head;
		while (listIterator != null) {
			LTSTransitionList nonDetListIterator = listIterator;
			while (nonDetListIterator != null) {
				if (nonDetListIterator.getNext() >= 0) {
					nonDetListIterator.setNext(nonDetListIterator.getNext() + offset);
				}
				nonDetListIterator = nonDetListIterator.getNondet();
			}
			listIterator = listIterator.getList();
		}
		return head;
	}

	@Override
	public String[] getAlphabet() {
		return this.alphabet;
	}

	public Vector<String> getAlphabetV() {
		Vector<String> v = new Vector<>(alphabet.length - 1);
		for (int i = 1; i < alphabet.length; ++i) {
			v.add(alphabet[i]);
		}
		return v;
	}

	@Override
	public MyList getTransitions(byte[] fromState) {
		MyList tr = new MyList();
		int state;
		if (fromState == null) {
			state = Declaration.ERROR;
		} else {
			state = decode(fromState);
		}
		if (state < 0 || state >= this.states.length)
			return tr;
		if (states[state] != null)
			for (Enumeration<LTSTransitionList> e = states[state].elements(); e.hasMoreElements();) {
				LTSTransitionList t = e.nextElement();
				// if(this.alphabet.length<=t.getEvent()){
				// throw new
				// InternalError("Automaton: "+this.name+" the action with id
				// "+t.getEvent()+" is not contained into the alphabet of the
				// automaton");
				// }
				tr.add(state, encode(t.getNext()), t.getEvent());
			}
		return tr;
	}

	@Override
	public String getViolatedProperty() {
		return null;
	}

	// returns shortest trace to state (vector of Strings)
	@Override
	public Vector<String> getTraceToState(byte[] from, byte[] to) {
		LTSTransitionList trace = new LTSTransitionList(0, 0);
		LTSTransitionList.search(trace, states, decode(from), decode(to), -123456);
		return LTSTransitionList.getPath(trace.getPath(), alphabet);
	}

	/**
	 * return the number of the END state
	 */
	@Override
	public boolean end(byte[] state) {
		return decode(state) == endseq;
	}

	/**
	 * return whether or not state is accepting
	 */
	@Override
	public boolean isAccepting(byte[] state) {
		return isAccepting(decode(state));
	}

	// return the number of the START state
	@Override
	public byte[] start() {
		return encode(0);
	}

	// set the Stack Checker for partial order reduction
	@Override
	public void setStackChecker(StackCheck s) {
	} // null operation

	// returns true if partial order reduction
	@Override
	public boolean isPartialOrder() {
		return false;
	}

	// diable partial order
	@Override
	public void disablePartialOrder() {
	}

	// enable partial order
	@Override
	public void enablePartialOrder() {
	}

	/**
	 * checks whether the state n is accepting
	 * 
	 * @param stateIndex
	 *            the state to be considered
	 * @return true if and only if the state is accepting
	 */
	public boolean isAccepting(int stateIndex) {
		if (stateIndex < 0 || stateIndex >= this.states.length) {
			return false;
		}
		return LTSTransitionList.isAccepting(states[stateIndex], alphabet);
	}

	public Set<Integer> getAccepting() {
		Set<Integer> accepting = new HashSet<>();
		for (int i = 0; i < this.states.length; ++i) {
			if (isAccepting(i)) {
				accepting.add(i);
			}
		}
		return accepting;
	}

	@Override
	public String toString() {
		// return this.getName();
		StringBuilder builder = new StringBuilder();
		builder.append("LTS: " + this.getName() + "\n");

		builder.append("Alphabet: \n");

		if (this.alphabet.length > 1) {
			builder.append(this.alphabet[0]);
			for (int i = 1; i < this.alphabet.length; i++) {
				builder.append("," + this.alphabet[i]);
			}
		}
		builder.append("\n States-Transitions \n");
		for (int i = 0; i < this.states.length; i++) {
			builder.append("state: " + i + " transitions: " + this.states[i] + "\n");
		}
		builder.append("Boxes \n");
		builder.append(boxIndexes);

		builder.append("INTERFACES: " + "\n");
		this.mapBoxInterface.entrySet().stream()
				.forEach(t -> builder.append("\t box: " + t.getKey() + " interface: " + t.getValue() + "\n"));

		builder.append("Final states" + finalStateIndexes + "\n");
		builder.append("END " + this.endseq + "\n");
		builder.append("Prop " + this.prop + "\n");
		builder.append("HasDuplicates " + this.hasDuplicates);
		return builder.toString();
	}

	public BitSet accepting() {
		BitSet b = new BitSet();
		for (int i = 0; i < this.states.length; ++i) {
			if (isAccepting(i)) {
				b.set(i);
			}
		}
		return b;
	}

	public int getEvent(String action) {
		for (int i = 0; i < this.alphabet.length; i++)
			if (this.alphabet[i].equals(action))
				return i;

		throw new IllegalStateException("Invalid action execution");
	}

	public void swapStates(int i, int j) {
		LTSTransitionList swap = this.states[i];
		this.states[i] = this.states[j];
		this.states[j] = swap;

		for (LTSTransitionList transitions : this.states) {
			if (transitions != null) {
				transitions.swapNext(i, j);
			}
		}
	}

	/**
	 * Simulates a run of this nondeterministic automaton on the given word. If
	 * stopOnError is set, and the error state is encountered during simulation,
	 * the ErrorStateReachedException is thrown; otherwise the exception is
	 * never thrown. The set of possible current states as a result of
	 * simulation is returned.
	 * 
	 * @param word
	 *            The sequence of events to simulate
	 * @param wordAlphabet
	 *            The set of symbols which may appear in the word
	 * @param stopOnError
	 *            Whether to stop when the error state is reached
	 * @return The set of states that can be encountered
	 * @throws ErrorStateReachedException
	 */
	public List<Integer> simulate(List<String> word, List<String> wordAlphabet, boolean stopOnError)
			throws NoStateReachedException, ErrorStateReachedException {

		BitSet state = nfaSimulation(word, wordAlphabet, stopOnError);
		List<Integer> v = new Vector<Integer>();
		for (int s = 0; s < state.length(); s++)
			if (state.get(s))
				v.add(s);
		return v;
	}

	public List<Integer> closure(List<Integer> state, List<String> closeActions, boolean stopOnError)
			throws ErrorStateReachedException {

		BitSet stateBS = new BitSet();
		for (int s : state)
			stateBS.set(s);

		Map<String, Integer> alphaMap = alphabetMap();
		Set<Integer> closeEvents = new HashSet<Integer>();
		for (String a : closeActions)
			closeEvents.add(alphaMap.get(a));

		BitSet nextStateBS = nfaClosure(stateBS, closeEvents, stopOnError);

		List<Integer> v = new Vector<Integer>();
		for (int s = 0; s < nextStateBS.length(); s++)
			if (nextStateBS.get(s))
				v.add(s);
		return v;
	}

	public void setName(String name) {
		Preconditions.checkNotNull(name, "The name cannot be null");
		this.name = name;
	}

	/**
	 * sets the interface of the specified box
	 * 
	 * @param boxName
	 *            the name of the box
	 * @param boxInterface
	 *            the interface of the box
	 */
	public void setBoxInterface(String boxName, Set<String> boxInterface) {

		Preconditions.checkNotNull(boxName, "The name of the box cannot be null");
		Preconditions.checkNotNull(boxInterface, "The interface of the box cannot be null");

		Preconditions.checkArgument(this.boxIndexes.keySet().contains(boxName),
				"The box " + boxName + " is not contained in the boxes of the LTS");

		
		//Preconditions.checkArgument(new HashSet<String>(Arrays.asList(this.alphabet)).containsAll(boxInterface),
			//	"The interface of the box "+boxName+" includes some events that do not belong to the LTS");

		
		List<String> alphabetEvents=this.getAlphabetEvents();
		
		for(String event: boxInterface){
			if(!alphabetEvents.contains(event)){
				this.addEvent(event);
			}
		}
		this.mapBoxInterface.put(boxName, new HashSet<String>(boxInterface));
	}

	/**
	 * returns the interface of the box
	 * 
	 * @param boxName
	 *            the name of the box
	 * @return the interface of the box, i.e., the set of events that can occur
	 *         while the system is in the box
	 * @throws NullPointerException
	 *             if the name of the box is null
	 * @throws IllegalArgumentException
	 *             if the box is not contained into the set of the box of the
	 *             LTS
	 */
	public Set<String> getBoxInterface(String boxName) {

		Preconditions.checkNotNull(boxName, "The name of the box cannot be null");
		Preconditions.checkArgument(this.mapBoxInterface.containsKey(boxName),
				"The box " + boxName + " is not a box of the LTS");

		return this.mapBoxInterface.get(boxName);
	}

	/**
	 * Computes the possible set of states that could result from the given
	 * sequence of events occurring from the given set of states. If stopOnError
	 * is set, then the ErrorStateReachedException is thrown upon encountering
	 * the error state.
	 * 
	 * @param word
	 *            The sequence of events to simulate
	 * @param wordAlphabet
	 *            The set of symbols which may appear in the word
	 * @param stopOnError
	 *            Whether to stop when the error state is reached
	 * @return The set of states that can be encountered
	 * @throws ErrorStateReachedException
	 */
	private BitSet nfaSimulation(List<String> word, List<String> wordAlphabet, boolean stopOnError)
			throws NoStateReachedException, ErrorStateReachedException {

		assert !wordAlphabet.contains("tau");

		BitSet state = new BitSet(this.states.length);

		List<String> prefixTrace = new Vector<String>();

		Map<String, Integer> alphaMap = alphabetMap();
		Set<Integer> wordEvents = new HashSet<Integer>(), skipEvents = new HashSet<Integer>(alphaMap.values());

		// Assuption: tau is represented with 0.
		skipEvents.add(0);

		for (String a : wordAlphabet)
			wordEvents.add(alphaMap.get(a));

		// Skip the actions that don't occur in the word alphabet.
		skipEvents.removeAll(wordEvents);

		// start at the initial state
		state.set(0);
		try {
			state = nfaClosure(state, skipEvents, stopOnError);

		} catch (ErrorStateReachedException e) {
			e.traceToError = prefixTrace;
			throw e;
		}

		for (String event : word) {
			assert wordAlphabet.contains(event);
			assert state != null && !state.isEmpty();

			prefixTrace.add(event);

			if (!alphaMap.containsKey(event))
				continue;

			else
				try {
					state = nfaClosure(nfaNextState(state, alphaMap.get(event), stopOnError), skipEvents, stopOnError);

					if (state == null || state.isEmpty())
						throw new NoStateReachedException(prefixTrace);

				} catch (ErrorStateReachedException e) {
					e.traceToError = prefixTrace;
					throw e;
				}
		}
		return state;
	}

	/**
	 * Computes the possible set of states that could result from the given
	 * event occurring once at the given set of states. If stopOnError is set,
	 * then the ErrorStateReachedException is thrown upon encountering the error
	 * state.
	 * 
	 * @param state
	 *            The initial set of states
	 * @param event
	 *            The action to occur
	 * @param stopOnError
	 *            Whether to stop when the error state is reached
	 * @return The set of states that can be encountered
	 * @throws ErrorStateReachedException
	 */
	private BitSet nfaNextState(BitSet state, int event, boolean stopOnError) throws ErrorStateReachedException {

		BitSet nextState = new BitSet(this.states.length);

		for (int s = 0; s < state.length(); s++) {
			if (state.get(s)) {
				if (s == Declaration.ERROR && stopOnError)
					throw new ErrorStateReachedException();

				int[] next = LTSTransitionList.nextState(this.states[s], event);
				if (next != null)
					for (int t : next)
						if (t == Declaration.ERROR && stopOnError)
							throw new ErrorStateReachedException();
						else
							nextState.set(t);
			}
		}
		return nextState;
	}

	/**
	 * Computes the set of states reachable from the given set under zero or
	 * more repeated occurrences of the given events. If stopOnError is set,
	 * then the ErrorStateReachedException is thrown upon encountering the error
	 * state.
	 * 
	 * @param state
	 *            The initial set of states
	 * @param events
	 *            The allowed actions
	 * @param stopOnError
	 *            Whether to stop when the error state is reached
	 * @return The set of states reachable through the events
	 * @throws ErrorStateReachedException
	 */
	private BitSet nfaClosure(BitSet state, Set<Integer> events, boolean stopOnError)
			throws ErrorStateReachedException {
		BitSet closure = new BitSet(this.states.length);
		Stack<Integer> workList = new Stack<Integer>();

		for (int s = 0; s < state.length(); s++)
			if (state.get(s))
				if (s == Declaration.ERROR && stopOnError)
					throw new ErrorStateReachedException();
				else {
					closure.set(s);
					workList.push(s);
				}

		while (!workList.empty()) {
			int s = workList.pop();

			for (int action : events) {
				int[] next = LTSTransitionList.nextState(this.states[s], action);

				if (next != null)
					for (int t : next)
						if (t == Declaration.ERROR && stopOnError)
							throw new ErrorStateReachedException();
						else if (!closure.get(t)) {
							closure.set(t);
							workList.push(t);
						}
			}
		}
		return closure;
	}

	private Map<String, Integer> alphabetMap() {
		Map<String, Integer> alphaMap = new HashMap<>();
		for (int i = 0; i < alphabet.length; i++)
			alphaMap.put(alphabet[i], i);
		return alphaMap;
	}

	/**
	 * create shared alphabet for machines & renumber according to that alphabet
	 */
	private static String[] sharedAlphabet(LabelledTransitionSystem[] sm) {
		// set up shared alphabet structure
		Counter newLabel = new Counter(0);
		Hashtable<String, Integer> actionMap = new Hashtable<>();
		for (int i = 0; i < sm.length; i++) {
			for (int j = 0; j < sm[i].alphabet.length; j++) {
				if (!actionMap.containsKey(sm[i].alphabet[j])) {
					actionMap.put(sm[i].alphabet[j], newLabel.label());
				}
			}
		}
		// copy into alphabet array
		String[] actionName = new String[actionMap.size()];
		Enumeration<String> e = actionMap.keys();
		while (e.hasMoreElements()) {
			String s = e.nextElement();
			int index = actionMap.get(s).intValue();
			actionName[index] = s;
		}
		// renumber all transitions with new action numbers
		for (int i = 0; i < sm.length; i++) {
			for (int j = 0; j < sm[i].getStates().length; j++) {
				LTSTransitionList p = sm[i].states[j];
				while (p != null) {
					LTSTransitionList tr = p;
					tr.setEvent(actionMap.get(sm[i].alphabet[tr.getEvent()]).intValue());
					while (tr.getNondet() != null) {
						tr.getNondet().setEvent(tr.getEvent());
						tr = tr.getNondet();
					}
					p = p.getList();
				}
			}
		}
		return actionName;

	}

	private byte[] encode(int state) {
		byte[] code = new byte[4];
		for (int i = 0; i < 4; ++i) {
			code[i] |= (byte) state;
			state = state >>> 8;
		}
		return code;
	}

	private int decode(byte[] code) {
		int x = 0;
		for (int i = 3; i >= 0; --i) {
			x |= (int) (code[i]) & 0xFF;
			if (i > 0)
				x = x << 8;
		}
		return x;

	}

	private void relationalRelabel(Relation oldtonew) {
		Vector<String> na = new Vector<>();
		Relation otoni = new Relation(); // index map old to additional
		na.setSize(alphabet.length);
		int new_index = alphabet.length;
		na.setElementAt(alphabet[0], 0);
		for (int i = 1; i < alphabet.length; i++) {
			int prefix_end = -1;
			Object o = oldtonew.get(alphabet[i]);
			if (o != null) {
				if (o instanceof String) {
					na.setElementAt((String) o, i);
				} else { // one - to - many
					@SuppressWarnings("unchecked")
					Vector<String> v = (Vector<String>) o;
					na.setElementAt(v.firstElement(), i);
					for (int j = 1; j < v.size(); ++j) {
						na.addElement(v.elementAt(j));
						otoni.put(new Integer(i), new Integer(new_index));
						++new_index;
					}
				}
			} else if ((prefix_end = maximalPrefix(alphabet[i], oldtonew)) >= 0) { // is
																					// it
																					// prefix?
				String old_prefix = alphabet[i].substring(0, prefix_end);
				o = oldtonew.get(old_prefix);
				if (o != null) {
					if (o instanceof String) {
						na.setElementAt(((String) o) + alphabet[i].substring(prefix_end), i);
					} else { // one - to - many
						@SuppressWarnings("unchecked")
						Vector<String> v = (Vector<String>) o;
						na.setElementAt(v.firstElement() + alphabet[i].substring(prefix_end), i);
						for (int j = 1; j < v.size(); ++j) {
							na.addElement(v.elementAt(j) + alphabet[i].substring(prefix_end));
							otoni.put(new Integer(i), new Integer(new_index));
							++new_index;
						}
					}
				} else {
					na.setElementAt(alphabet[i], i); // not relabeled
				}
			} else {
				na.setElementAt(alphabet[i], i); // not relabeled
			}
		}
		// install new alphabet
		String aa[] = new String[na.size()];
		na.copyInto(aa);
		alphabet = aa;
		// add transitions
		addtransitions(otoni);
		checkDuplicates();
	}

	private void functionalRelabel(Hashtable<?, ?> oldtonew) {
		for (int i = 1; i < alphabet.length; i++) { // don't relabel tau
			String newlabel = (String) oldtonew.get(alphabet[i]);
			if (newlabel != null)
				alphabet[i] = newlabel;
			else
				alphabet[i] = prefixLabelReplace(i, oldtonew);
		}
		checkDuplicates();
	}

	private void checkDuplicates() {
		Hashtable<String, String> duplicates = new Hashtable<>();
		for (int i = 1; i < alphabet.length; i++) {
			if (duplicates.put(alphabet[i], alphabet[i]) != null) {
				hasDuplicates = true;
				crunchDuplicates();
			}
		}
	}

	private void crunchDuplicates() {
		Hashtable<String, Integer> newAlpha = new Hashtable<>();
		Hashtable<Integer, Integer> oldtonew = new Hashtable<Integer, Integer>();
		int index = 0;
		for (int i = 0; i < alphabet.length; i++) {
			if (newAlpha.containsKey(alphabet[i])) {
				oldtonew.put(new Integer(i), newAlpha.get(alphabet[i]));
			} else {
				newAlpha.put(alphabet[i], new Integer(index));
				oldtonew.put(new Integer(i), new Integer(index));
				index++;
			}
		}
		alphabet = new String[newAlpha.size()];
		Enumeration<String> e = newAlpha.keys();
		while (e.hasMoreElements()) {
			String s = e.nextElement();
			int i = newAlpha.get(s).intValue();
			alphabet[i] = s;
		}
		// renumber transitions
		for (int i = 0; i < states.length; i++)
			states[i] = LTSTransitionList.renumberEvents(states[i], oldtonew);
	}

	/* ------------------------------------------------------------ */
	private String prefixLabelReplace(int i, Hashtable<?, ?> oldtonew) {
		int prefix_end = maximalPrefix(alphabet[i], oldtonew);
		if (prefix_end < 0)
			return alphabet[i];
		String old_prefix = alphabet[i].substring(0, prefix_end);
		String new_prefix = (String) oldtonew.get(old_prefix);
		if (new_prefix == null)
			return alphabet[i];
		return new_prefix + alphabet[i].substring(prefix_end);
	}

	private int maximalPrefix(String s, Hashtable<?, ?> oldtonew) {
		int prefix_end = s.lastIndexOf('.');
		if (prefix_end < 0)
			return prefix_end;
		if (oldtonew.containsKey(s.substring(0, prefix_end)))
			return prefix_end;
		else
			return maximalPrefix(s.substring(0, prefix_end), oldtonew);
	}

	private boolean isTrace(Vector<?> v, int index, int start) {
		if (index < v.size()) {
			String ename = (String) v.elementAt(index);
			int eno = eventNo(ename);
			if (eno < alphabet.length) { // this event is in the alphabet
				if (LTSTransitionList.hasEvent(states[start], eno)) {
					int n[] = LTSTransitionList.nextState(states[start], eno);
					for (int i = 0; i < n.length; ++i)
						// try each nondet path
						if (isTrace(v, index + 1, n[i]))
							return true;
					return false;
				} else if (eno != Declaration.TAU && eno != Declaration.TAU_MAYBE) // ignore
																					// taus
					return false;
			}
			return isTrace(v, index + 1, start);
		} else
			return (start == Declaration.ERROR);
	}

	private int eventNo(String ename) {
		int i = 0;
		while (i < alphabet.length && !ename.equals(alphabet[i]))
			i++;
		return i;
	}

	private void dohiding(BitSet visible) {
		Integer tau = new Integer(Declaration.TAU);
		Integer tauMaybe = new Integer(Declaration.TAU_MAYBE);
		Hashtable<Integer, Integer> oldtonew = new Hashtable<>();
		Vector<String> newAlphabetVec = new Vector<>();
		int index = 0;
		for (int i = 0; i < alphabet.length; i++) {
			if (!visible.get(i)) {
				// pone taus!
				if (!MTSUtils.isMaybe(alphabet[i])) {
					oldtonew.put(new Integer(i), tau);
				} else {
					// newAlphabetVec.addElement("tau?");
					oldtonew.put(new Integer(i), tauMaybe);
					// index++;
				}
			} else {
				newAlphabetVec.addElement(alphabet[i]);
				oldtonew.put(new Integer(i), new Integer(index));
				index++;
			}
		}
		alphabet = new String[newAlphabetVec.size()];
		newAlphabetVec.copyInto(alphabet);
		// renumber transitions
		for (int i = 0; i < states.length; i++) {
			states[i] = LTSTransitionList.renumberEvents(states[i], oldtonew);
		}
	}

	private void addtransitions(Relation oni) {
		for (int i = 0; i < states.length; i++) {
			LTSTransitionList ns = LTSTransitionList.newTransitions(states[i], oni);
			if (ns != null)
				states[i] = EventStateUtils.union(states[i], ns);
		}
	}

	public LabelledTransitionSystem clone() {
		LabelledTransitionSystem returnLTS = new LabelledTransitionSystem(this.name);
		returnLTS.boxIndexes = new HashMap<>(this.boxIndexes);
		returnLTS.mapBoxInterface = new HashMap<String, Set<String>>(this.mapBoxInterface);
		returnLTS.alphabet = new String[this.alphabet.length];
		for (int i = 0; i < this.alphabet.length; i++) {
			returnLTS.alphabet[i] = this.alphabet[i];
		}
		returnLTS.states = new LTSTransitionList[this.states.length];
		for (int i = 0; i < this.states.length; i++) {
			returnLTS.states[i] = LTSTransitionList.copy(this.states[i]);
		}
		returnLTS.finalStateIndexes = new HashSet<Integer>(this.finalStateIndexes);
		return returnLTS;
	}

	static private boolean isPrefix(String prefix, String s) {
		int prefix_end = s.lastIndexOf('.');
		if (prefix_end < 0)
			return false;
		if (prefix.equals(s.substring(0, prefix_end)))
			return true;
		else
			return isPrefix(prefix, s.substring(0, prefix_end));
	}

	/**
	 * An exception indicating that an error state has been encountered.
	 */
	public class ErrorStateReachedException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 8387277816071488972L;

		public List<String> traceToError;

		private ErrorStateReachedException() {
			this.traceToError = Collections.emptyList();
		}

		public ErrorStateReachedException(List<String> traceToError) {
			this.traceToError = traceToError;
		}
	}

	/**
	 * An exception indicating that an no states can be encountered.
	 */
	public class NoStateReachedException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5913329330925988799L;
		public List<String> traceToBlocking;

		public NoStateReachedException(List<String> traceToBlocking) {
			this.traceToBlocking = traceToBlocking;
		}
	}

	public LTSTransitionList[] getStates() {
		return states;
	}

	public int getTransitionNumber() {
		int transitionNumber = 0;
		for (int stateIndex = 0; stateIndex < this.states.length; stateIndex++) {
			if (this.states[stateIndex] != null) {
				Enumeration<LTSTransitionList> transitions = this.states[stateIndex].elements();

				while (transitions.hasMoreElements()) {
					transitionNumber++;
					transitions.nextElement();
				}
			}
		}

		return transitionNumber;
	}

	/**
	 * returns the number of states of the Labelled Transition System
	 * 
	 * @return the number of states of the Labelled Transition System
	 */
	public int getNumberOfStates() {
		return this.states.length;
	}

	/**
	 * |T|/(|S|(|S|-1))
	 */
	public double getGraphDensity() {
		double transitionNumber =

				this.getTransitionNumber();
		double stateNumber = this.getStates().length;

		return transitionNumber / (stateNumber * (stateNumber - 1));
	}

	public int size() {
		return this.getStates().length + this.getTransitionNumber();
	}

	/**
	 * returns the transitions that exit the state with the specified index
	 * 
	 * @param stateIndex
	 *            the transitions that exit the state with the specified index
	 * @return the transitions that exit the state with the specified index
	 */
	public LTSTransitionList getTransitions(int stateIndex) {
		return this.states[stateIndex];
	}

	public void setState(int stateIndex, LTSTransitionList transitionList) {
		this.states[stateIndex] = transitionList;
	}

	public void setStates(LTSTransitionList[] states) {
		this.states = states;
	}

	public void removeOutgoingTransitionsWithLabel(int stateIndex, String label) {
		Preconditions.checkArgument(this.alphabetMap().containsKey(label),
				"The label: " + label + " is not contained in the alphabet of the automaton");
		if (this.states[stateIndex] != null) {
			this.states[stateIndex] = LTSTransitionList.removeEvent(this.states[stateIndex],
					this.alphabetMap().get(label));
		}
	}

	public void removeTransition(int stateSource, int event, int stateDestination) {
		this.states[stateSource] = LTSTransitionList.remove(this.states[stateSource],
				new LTSTransitionList(event, stateDestination));
	}

	public Map<String, Integer> getBoxIndexes() {
		return boxIndexes;
	}

	public void setBoxIndexes(Map<String, Integer> boxIndexes) {
		this.boxIndexes = boxIndexes;
	}

	/**
	 * creates a new state
	 * 
	 * @return the index of the new state
	 */
	public int addNewState() {
		if (this.states == null) {
			this.states = new LTSTransitionList[1];
		} else {
			this.states = Arrays.copyOf(this.states, this.states.length + 1);
		}
		return this.states.length - 1;
	}

	public void addTransition(int source, int event, int destination) {
		LTSTransitionList transition = new LTSTransitionList(event, destination);
		this.states[source] = EventStateUtils.add(this.states[source], transition);
	}
}