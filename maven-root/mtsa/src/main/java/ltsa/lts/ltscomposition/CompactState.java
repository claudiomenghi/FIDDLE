package ltsa.lts.ltscomposition;

import java.io.PrintStream;
import java.math.BigDecimal;
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

import com.google.common.base.Preconditions;

import ltsa.lts.EventStateUtils;
import ltsa.lts.csp.Declaration;
import ltsa.lts.csp.Relation;
import ltsa.lts.lts.Automata;
import ltsa.lts.lts.EventState;
import ltsa.lts.lts.ProbabilisticEventState;
import ltsa.lts.operations.composition.StackCheck;
import ltsa.lts.util.Counter;
import ltsa.lts.util.MTSUtils;
import ltsa.lts.util.collections.MyIntHash;
import ltsa.lts.util.collections.MyList;
import ltsa.lts.util.collections.MyProbListEntry;
import ltsa.lts.util.collections.StateMap;

public class CompactState implements Automata {

	private String name;
	public int maxStates;
	

	public String[] alphabet;
	

	public EventState[] states; // each state is to a vector of <event,
								// nextstate>
	private String mtsControlProblemAnswer;

	/* AMES: Promoted visibility to public. */
	public int endseq = -9999; // number of end of sequence state if any

	public CompactState(String name) {
		Preconditions.checkNotNull(name, "The name cannot be null");
		this.name=name;
	} // null constructor

	public CompactState(int size, String name, StateMap statemap,
			MyList transitions, String[] alphabet, int endSequence) {
		this.mtsControlProblemAnswer = "";
		this.alphabet = alphabet;
		this.name = name;
		maxStates = size;
		states = new EventState[maxStates];
		while (!transitions.empty()) {
			int fromState = (int) transitions.getFrom();
			int toState = transitions.getTo() == null ? -1 : statemap
					.get(transitions.getTo());
			int bundle = 0;
			BigDecimal prob = BigDecimal.ZERO;
			if (transitions.peek() instanceof MyProbListEntry) {
				bundle = transitions.getBundle();
				prob = transitions.getProb();
				states[fromState] = EventStateUtils.add(states[fromState],
						new ProbabilisticEventState(transitions.getAction(),
								toState, prob, bundle));
			} else {
				states[fromState] = EventStateUtils.add(states[fromState],
						new EventState(transitions.getAction(), toState));
			}
			transitions.next();
		}
		endseq = endSequence;
	}

	public String getName() {
		return this.name;
	}

	public String getMtsControlProblemAnswer() {
		if (this.mtsControlProblemAnswer.equals(""))
			return "NONE";
		else
			return this.mtsControlProblemAnswer;
	}

	public void setMtsControlProblemAnswer(String answer) {
		this.mtsControlProblemAnswer = answer;
	}

	public void reachable() {
		MyIntHash otn = EventStateUtils.reachable(states);

		EventState[] oldStates = states;
		maxStates = otn.size();
		states = new EventState[maxStates];
		for (int oldi = 0; oldi < oldStates.length; ++oldi) {
			int newi = otn.get(oldi);
			if (newi > -2) {
				states[newi] = EventStateUtils.renumberStates(oldStates[oldi],
						otn);
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
			for (int i = 0; i < maxStates; i++)
				// remove reflexive tau
				// remove reflexive tau
				states[i] = EventState.remove(states[i], new EventState(
						Declaration.TAU, i));
			// oStates[i] = EventState.remove(oStates[i],new
			// EventState(Declaration.TAU_MAYBE,i));
			BitSet tauOnly = new BitSet(maxStates);
			for (int i = 1; i < maxStates; ++i) {
				if (EventState.hasOnlyTauAndAccept(states[i], alphabet)) {
					tauOnly.set(i);
					canRemove = true;
				}
			}
			if (!canRemove)
				return;
			for (int i = 0; i < maxStates; ++i) {
				if (!tauOnly.get(i))
					states[i] = EventState.addNonDetTau(states[i], states,
							tauOnly);
			}
			int oldSize = maxStates;
			reachable();
			if (oldSize == maxStates)
				return;
		}
	}

	public void removeDetCycles(String action) {
		int act = eventNo(action);
		if (act >= alphabet.length)
			return;
		for (int i = 0; i < states.length; ++i) {
			if (!EventState.hasNonDetEvent(states[i], act))
				states[i] = EventState
						.remove(states[i], new EventState(act, i));
		}
	}

	// check if has only single terminal accept state
	// also if no accept states - treats as safety property so that TRUE
	// generates a null constraint
	public boolean isSafetyOnly() {
		int terminalAcceptStates = 0;
		int acceptStates = 0;
		for (int i = 0; i < maxStates; i++) {
			if (EventState.isAccepting(states[i], alphabet)) {
				++acceptStates;
				if (EventState.isTerminal(i, states[i]))
					++terminalAcceptStates;
			}
		}
		return (terminalAcceptStates == 1 && acceptStates == 1)
				|| acceptStates == 0;
	}

	public void makeSafety() {
		int acceptState = -1;
		for (int i = 0; i < maxStates; i++) {
			if (EventState.isAccepting(states[i], alphabet)) {
				acceptState = i;
				break;
			}
		}
		if (acceptState >= 0)
			states[acceptState] = EventState.removeAccept(states[acceptState]);
		for (int i = 0; i < maxStates; i++) {
			EventState.replaceWithError(states[i], acceptState);
		}
		reachable();
	}

	// remove acceptance from states with only outgoing tau
	public void removeAcceptTau() {
		for (int i = 1; i < maxStates; ++i) {
			if (EventState.hasOnlyTauAndAccept(states[i], alphabet)) {
				states[i] = EventState.removeAccept(states[i]);
			}
		}
	}

	public boolean hasERROR() {
		for (int i = 0; i < maxStates; i++)
			if (EventState.hasState(states[i], Declaration.ERROR))
				return true;
		return false;
	}

	public void prefixLabels(String prefix) {
		name = prefix + ":" + name;
		// BUGFIX don't prefix tau nor tau?
		int i = 1;
		if (alphabet[Declaration.TAU_MAYBE] != null
				&& alphabet[Declaration.TAU_MAYBE].equals("tau?")) {
			i = 2;
		}
		for (; i < alphabet.length; i++) { // don't prefix tau
			String old = alphabet[i];
			alphabet[i] = prefix + "." + old;
		}
	}

	private boolean hasduplicates = false;

	public boolean relabelDuplicates() {
		return hasduplicates;
	}

	public void relabel(Relation oldtonew) {
		hasduplicates = false;
		if (oldtonew.isRelation())
			relational_relabel(oldtonew);
		else
			functional_relabel(oldtonew);
	}

	private void relational_relabel(Relation oldtonew) {
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
						na.setElementAt(
								((String) o)
										+ alphabet[i].substring(prefix_end), i);
					} else { // one - to - many
						Vector<String> v = (Vector<String>) o;
						na.setElementAt(
								v.firstElement()
										+ alphabet[i].substring(prefix_end), i);
						for (int j = 1; j < v.size(); ++j) {
							na.addElement(v.elementAt(j)
									+ alphabet[i].substring(prefix_end));
							otoni.put(new Integer(i), new Integer(new_index));
							++new_index;
						}
					}
				} else {
					na.setElementAt(alphabet[i], i); // not relabelled
				}
			} else {
				na.setElementAt(alphabet[i], i); // not relabelled
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

	private void functional_relabel(Hashtable<?, ?> oldtonew) {
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
		Hashtable<String, String> duplicates = new Hashtable<String, String>();
		for (int i = 1; i < alphabet.length; i++) {
			if (duplicates.put(alphabet[i], alphabet[i]) != null) {
				hasduplicates = true;
				crunchDuplicates();
			}
		}
	}

	private void crunchDuplicates() {
		Hashtable<String, Integer> newAlpha = new Hashtable<String, Integer>();
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
			states[i] = EventState.renumberEvents(states[i], oldtonew);
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

	// hides every event but the ones in toShow
	public void expose(Collection<String> toShow) {
		BitSet visible = new BitSet(alphabet.length);
		for (int i = 1; i < alphabet.length; ++i) {
			if (contains(alphabet[i], toShow))
				visible.set(i);
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

	private void dohiding(BitSet visible) {
		Integer tau = new Integer(Declaration.TAU);
		Integer tauMaybe = new Integer(Declaration.TAU_MAYBE);
		Hashtable<Integer, Integer> oldtonew = new Hashtable<Integer, Integer>();
		Vector<String> newAlphabetVec = new Vector<String>();
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
		for (int i = 0; i < states.length; i++)
			states[i] = EventState.renumberEvents(states[i], oldtonew);
	}

	public static boolean contains(String action, Collection<String> v) {
		for (String s : v) {
			if (s.equals(action) || isPrefix(s, action)) {
				return true;
			}
		}
		return false;
	}

	// make every state have transitions to ERROR state
	// for actions not already declared from that state
	// properties can terminate in any state,however, we set no end state

	private boolean prop = false;

	public boolean isProperty() {
		return prop;
	}

	public void makeProperty() {
		endseq = -9999;
		prop = true;
		for (int i = 0; i < maxStates; i++)
			states[i] = EventState.addTransToError(states[i], alphabet.length);
	}

	public void unMakeProperty() {
		endseq = -9999;
		prop = false;
		for (int i = 0; i < maxStates; i++)
			states[i] = EventState.removeTransToError(states[i]);
	}

	public boolean isNonDeterministic() {
		for (int i = 0; i < maxStates; i++)
			if (EventState.hasNonDet(states[i]))
				return true;
		return false;
	}

	// output LTS in aldebaran format
	public void printAUT(PrintStream out) {
		// modified aldebaran in case of probabilistic systems
		if (states[0] instanceof ProbabilisticEventState)
			out.print("// probabilistic\n");

		out.print("des(0," + ntransitions() + "," + maxStates + ")\n");
		for (int i = 0; i < states.length; i++)
			EventStateUtils.printAUT(states[i], i, alphabet, out);
	}

	public CompactState myclone() {
		CompactState m = new CompactState(this.getName());
		m.endseq = endseq;
		m.prop = prop;
		m.alphabet = new String[alphabet.length];
		System.arraycopy(alphabet, 0, m.alphabet, 0, alphabet.length);
		m.maxStates = maxStates;
		m.states = new EventState[maxStates];
		for (int i = 0; i < states.length; i++) {
			if (states[i] == null)
				m.states[i] = null;
			else
				m.states[i] = EventState.copy(states[i]);
		}

		return m;
	}

	public int ntransitions() {
		int count = 0;
		for (int i = 0; i < states.length; i++)
			count += EventStateUtils.count(states[i]);
		return count;
	}

	public boolean hasTau() {
		for (int i = 0; i < states.length; ++i) {
			if (EventState.hasTau(states[i]))
				return true;
		}
		return false;
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

	static private boolean isPrefix(String prefix, String s) {
		int prefix_end = s.lastIndexOf('.');
		if (prefix_end < 0)
			return false;
		if (prefix.equals(s.substring(0, prefix_end)))
			return true;
		else
			return isPrefix(prefix, s.substring(0, prefix_end));
	}

	/* ------------------------------------------------------------ */

	public boolean isErrorTrace(Vector<?> trace) {
		boolean hasError = false;
		for (int i = 0; i < maxStates && !hasError; i++)
			if (EventState.hasState(states[i], Declaration.ERROR))
				hasError = true;
		if (!hasError)
			return false;
		return isTrace(trace, 0, 0);
	}

	private boolean isTrace(Vector<?> v, int index, int start) {
		if (index < v.size()) {
			String ename = (String) v.elementAt(index);
			int eno = eventNo(ename);
			if (eno < alphabet.length) { // this event is in the alphabet
				if (EventState.hasEvent(states[start], eno)) {
					int n[] = EventState.nextState(states[start], eno);
					for (int i = 0; i < n.length; ++i)
						// try each nondet path
						if (isTrace(v, index + 1, n[i]))
							return true;
					return false;
				} else if (eno != Declaration.TAU
						&& eno != Declaration.TAU_MAYBE) // ignore taus
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
		CompactState machs[] = new CompactState[n];
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
			for (int k = 0; k < maxStates; k++) {
				EventState.offsetEvents(machs[j].states[k], alphaN * j);
				states[k] = EventStateUtils
						.union(states[k], machs[j].states[k]);
			}
		}
	}

	/* --------------------------------------------------------------- */

	private void addtransitions(Relation oni) {
		for (int i = 0; i < states.length; i++) {
			EventState ns = EventState.newTransitions(states[i], oni);
			if (ns != null)
				states[i] = EventStateUtils.union(states[i], ns);
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
			if (EventState.hasEvent(states[i], en))
				return true;
		}
		return false;
	}

	/* --------------------------------------------------------------- */

	public boolean isSequential() {
		return endseq >= 0;
	}

	public boolean isEnd() {
		return maxStates == 1 && endseq == 0;
	}

	/*----------------------------------------------------------------*/

	public static CompactState sequentialCompose(Vector<?> seqs) {
		if (seqs == null){
			return null;
		}
		if (seqs.isEmpty()){
			return null;
		}
		if (seqs.size() == 1){
			return (CompactState) seqs.elementAt(0);
		}
		CompactState machines[] = new CompactState[seqs.size()];
		machines = (CompactState[]) seqs.toArray(machines);
		CompactState newMachine = new CompactState("");
		newMachine.alphabet = sharedAlphabet(machines);
		newMachine.maxStates = seqSize(machines);
		newMachine.states = new EventState[newMachine.maxStates];
		int offset = 0;
		for (int i = 0; i < machines.length; i++) {
			boolean last = (i == (machines.length - 1));
			copyOffset(offset, newMachine.states, machines[i], last);
			if (last)
				newMachine.endseq = machines[i].endseq + offset;
			offset += machines[i].states.length;
		}
		return newMachine;
	}

	/*----------------------------------------------------------------*/

	public void expandSequential(HashMap<?, ?> inserts) {
		int ninserts = inserts.size();
		CompactState machines[] = new CompactState[ninserts + 1];
		int insertAt[] = new int[ninserts + 1];
		machines[0] = this;
		int index = 1;
		Iterator<?> e = inserts.keySet().iterator();
		while (e.hasNext()) {
			Integer ii = (Integer) e.next();
			CompactState m = (CompactState) inserts.get(ii);
			machines[index] = m;
			insertAt[index] = ii.intValue();
			++index;
		}
		// newalphabet
		alphabet = sharedAlphabet(machines);
		// copy inserted machines
		for (int i = 1; i < machines.length; ++i) {
			int offset = insertAt[i];
			for (int j = 0; j < machines[i].states.length; ++j) {
				states[offset + j] = machines[i].states[j];
			}
		}
	}

	/*
	 * compute size of sequential composite
	 */
	private static int seqSize(CompactState[] sm) {
		int length = 0;
		for (int i = 0; i < sm.length; i++)
			length += sm[i].states.length;
		return length;
	}

	private static void copyOffset(int offset, EventState[] dest,
			CompactState m, boolean last) {
		for (int i = 0; i < m.states.length; i++) {
			if (!last)
				dest[i + offset] = EventState.offsetSeq(offset, m.endseq,
						m.maxStates + offset, m.states[i]);
			else
				dest[i + offset] = EventState.offsetSeq(offset, m.endseq,
						m.endseq + offset, m.states[i]);
		}
	}

	public void offsetSeq(int offset, int finish) {
		for (int i = 0; i < states.length; i++) {
			EventState.offsetSeq(offset, endseq, finish, states[i]);
		}
	}

	/*
	 * create shared alphabet for machines & renumber acording to that alphabet
	 */
	private static String[] sharedAlphabet(CompactState[] sm) {
		// set up shared alphabet structure
		Counter newLabel = new Counter(0);
		Hashtable<String, Integer> actionMap = new Hashtable<String, Integer>();
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
			for (int j = 0; j < sm[i].maxStates; j++) {
				EventState p = sm[i].states[j];
				while (p != null) {
					EventState tr = p;
					tr.setEvent(actionMap.get(sm[i].alphabet[tr.getEvent()])
							.intValue());
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

	/** implementation of Automata interface **/

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

	@Override
	public String[] getAlphabet() {
		return alphabet;
	}

	public Vector<String> getAlphabetV() {
		Vector<String> v = new Vector<String>(alphabet.length - 1);
		for (int i = 1; i < alphabet.length; ++i)
			v.add(alphabet[i]);
		return v;
	}

	@Override
	public MyList getTransitions(byte[] fromState) {
		MyList tr = new MyList();
		int state;
		if (fromState == null)
			state = Declaration.ERROR;
		else
			state = decode(fromState);
		if (state < 0 || state >= maxStates)
			return tr;
		if (states[(int) state] != null)
			for (Enumeration<?> e = states[state].elements(); e
					.hasMoreElements();) {
				EventState t = (EventState) e.nextElement();
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
	public Vector<?> getTraceToState(byte[] from, byte[] to) {
		EventState trace = new EventState(0, 0);
		int result = EventState.search(trace, states, decode(from), decode(to),
				-123456);
		return EventState.getPath(trace.getPath(), alphabet);
	}

	// return the number of the END state
	@Override
	public boolean END(byte[] state) {
		return decode(state) == endseq;
	}

	
	// return whether or not state is accepting
	@Override
	public boolean isAccepting(byte[] state) {
		return isAccepting(decode(state));
	}

	// return the number of the START state
	@Override
	public byte[] START() {
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

	/*-------------------------------------------------------------*/
	// is state accepting
	public boolean isAccepting(int n) {
		if (n < 0 || n >= maxStates)
			return false;
		return EventState.isAccepting(states[n], alphabet);
	}

	public BitSet accepting() {
		BitSet b = new BitSet();
		for (int i = 0; i < maxStates; ++i)
			if (isAccepting(i))
				b.set(i);
		return b;
	}

	// >>> AMES: Automaton Simulation
	Map<String, Integer> alphabetMap() {
		Map<String, Integer> alphaMap = new HashMap<String, Integer>();
		for (int i = 0; i < alphabet.length; i++)
			alphaMap.put(alphabet[i], i);
		return alphaMap;
	}

	public int getEvent(String action) {
		for (int i = 0; i < this.alphabet.length; i++)
			if (this.alphabet[i].equals(action))
				return i;

		throw new IllegalStateException("Invalid action execution");
	}

	public void swapStates(int i, int j) {
		EventState swap = this.states[i];
		this.states[i] = this.states[j];
		this.states[j] = swap;

		for (EventState state : this.states)
			if (state != null)
				state.swapNext(i, j);
	}

	/**
	 * An exception indicating that an error state has been encountered.
	 */
	public class ErrorStateReachedException extends Exception {
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
		public List<String> traceToBlocking;

		public NoStateReachedException(List<String> traceToBlocking) {
			this.traceToBlocking = traceToBlocking;
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
	public List<Integer> simulate(List<String> word, List<String> wordAlphabet,
			boolean stopOnError) throws NoStateReachedException,
			ErrorStateReachedException {

		BitSet state = nfaSimulation(word, wordAlphabet, stopOnError);
		List<Integer> v = new Vector<Integer>();
		for (int s = 0; s < state.length(); s++)
			if (state.get(s))
				v.add(s);
		return v;
	}

	public List<Integer> closure(List<Integer> state,
			List<String> closeActions, boolean stopOnError)
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
	BitSet nfaSimulation(List<String> word, List<String> wordAlphabet,
			boolean stopOnError) throws NoStateReachedException,
			ErrorStateReachedException {

		assert !wordAlphabet.contains("tau");

		BitSet state = new BitSet(this.maxStates);

		List<String> prefixTrace = new Vector<String>();

		Map<String, Integer> alphaMap = alphabetMap();
		Set<Integer> wordEvents = new HashSet<Integer>(), skipEvents = new HashSet<Integer>(
				alphaMap.values());

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
					state = nfaClosure(
							nfaNextState(state, alphaMap.get(event),
									stopOnError), skipEvents, stopOnError);

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
	BitSet nfaNextState(BitSet state, int event, boolean stopOnError)
			throws ErrorStateReachedException {

		BitSet nextState = new BitSet(this.maxStates);

		for (int s = 0; s < state.length(); s++) {
			if (state.get(s)) {
				if (s == Declaration.ERROR && stopOnError)
					throw new ErrorStateReachedException();

				int[] next = EventState.nextState(this.states[s], event);
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
	 * more repeated occurrences of the null action (tau). If stopOnError is
	 * set, then the ErrorStateReachedException is thrown upon encountering the
	 * error state.
	 * 
	 * @param state
	 *            The initial set of states
	 * @param stopOnError
	 *            Whether to stop when the error state is reached
	 * @return The set of states reachable through tau
	 * @throws ErrorStateReachedException
	 */
	BitSet nfaTauClosure(BitSet state, boolean stopOnError)
			throws ErrorStateReachedException {
		// Assumption: tau is represented with 0.
		return nfaClosure(state, Collections.singleton(0), stopOnError);
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
	BitSet nfaClosure(BitSet state, Set<Integer> events, boolean stopOnError)
			throws ErrorStateReachedException {
		BitSet closure = new BitSet(this.maxStates);
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
				int[] next = EventState.nextState(this.states[s], action);

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

	public void setName(String name) {
		
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name + " - " + this.getClass();
	}
}