package ltsa.lts;

import static ltsa.lts.util.MTSUtils.MAYBE_MARK;
import static ltsa.lts.util.MTSUtils.addTauMaybeAlphabet;
import static ltsa.lts.util.MTSUtils.computeHiddenAlphabet;
import static ltsa.lts.util.MTSUtils.getAlphabetWithMaybes;
import static ltsa.lts.util.MTSUtils.getMaybeAction;
import static ltsa.lts.util.MTSUtils.isMTSRepresentation;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import ltsa.dispatcher.TransitionSystemDispatcher;

import com.google.common.base.Preconditions;

public class StateMachine {

	private final String name;
	private final String kludgeName;
	private Map<String, Integer> alphabet = new HashMap<>();
	Vector<String> hidden;
	Relation relabels;
	private Map<String, Integer> explicitStates = new HashMap<>();
	Hashtable constants; // a bit of a kludge, should not be here
	Counter eventLabel = new Counter(0);
	Counter stateLabel = new Counter(0);

	private List<Transition> transitions = new ArrayList<>();
	private boolean isProperty = false;
	private boolean isMinimal = false;
	private boolean isDeterministic = false;
	private boolean isOptimistic = false;

	private boolean isPessimistic = false;
	private boolean isAbstract = false;
	private boolean isClousure = false;
	private boolean exposeNotHide = false;
	private boolean isStarEnv = false;

	Symbol goal;
	private HashMap<Integer, CompactState> sequentialInserts;
	Hashtable<Integer, Integer> preInsertsLast;
	Hashtable<Integer, CompactState> preInsertsMach;
	Hashtable<Integer, Integer> aliases = new Hashtable<>();

	public static LTSOutput output;

	public StateMachine(ProcessSpec spec, Vector<Value> params) {
		name = spec.getname();
		if (params != null) {
			spec.doParams(params);
			kludgeName = name + paramString(params);
		} else
			kludgeName = name;
		make(spec);
	}

	public StateMachine(ProcessSpec spec) {
		// compute machine name
		name = spec.getname();
		kludgeName = name;
		make(spec);
	}

	private void make(ProcessSpec spec) {
		constants = spec.constants;
		alphabet.put("tau", eventLabel.label());
		// compute explicit states
		spec.explicitStates(this);
		// crunch aliases
		spec.crunch(this);
		// relabel states in contiguous range from zero
		renumber();
		// compute transitions
		spec.transition(this);
		// alphabet extensions
		spec.addAlphabet(this);
		// alphabet relabels;
		spec.relabelAlphabet(this);
		// alphabet concealments
		spec.hideAlphabet(this);
		isProperty = spec.isProperty;
		isMinimal = spec.isMinimal;
		isDeterministic = spec.isDeterministic;
		isOptimistic = spec.isOptimistic;
		isPessimistic = spec.isPessimistic;
		isAbstract = spec.isAbstract;
		isClousure = spec.isClousure;
		exposeNotHide = spec.exposeNotHide;
		isStarEnv = spec.isStarEnv;
		goal = spec.goal;
	}

	public CompactState makeCompactState() {
		CompactState c = new CompactState();
		c.name = kludgeName;
		c.maxStates = stateLabel.lastLabel().intValue();
		Integer ii = explicitStates.get("END");
		if (ii != null)
			c.endseq = ii.intValue();
		c.alphabet = new String[alphabet.size()];
		Set<String> elements = alphabet.keySet();
		for (String s : elements) {
			int j = ((Integer) alphabet.get(s)).intValue();
			if (s.equals("@"))
				s = "@" + c.name;
			c.alphabet[j] = s;
		}
		if (!isProperty) {
			c.alphabet = getAlphabetWithMaybes(c.getAlphabet());
		} else {
			c.alphabet = addTauMaybeAlphabet(c.getAlphabet());
		}
		alphabet.clear();
		for (int i = 0; i < c.getAlphabet().length; i++) {
			alphabet.put(c.getAlphabet()[i], i);
		}
		c.states = new EventState[c.maxStates];
		for (Transition t : transitions) {
			String action = "" + t.getEvent();
			if (action.contains(MAYBE_MARK)) {
				action = getMaybeAction(action);
			}
			int ev = alphabet.get(action).intValue();
			EventState evSt = EventStateFactory.createEventState(ev, t);
			c.states[t.getFrom()] = EventStateUtils.add(c.states[t.getFrom()],
					evSt);
		}
		if (sequentialInserts != null)
			c.expandSequential(sequentialInserts);
		if (relabels != null)
			c.relabel(relabels);
		if (hidden != null) {
			computeHiddenAlphabet(hidden);
			if (!exposeNotHide)
				c.conceal(hidden);
			else
				c.expose(hidden);
		}
		if (isProperty) {
			if (c.isNonDeterministic() || c.hasTau())
				Diagnostics
						.fatal("primitive property processes must be deterministic: "
								+ name);
			c.makeProperty();
		}
		check_for_ERROR(c);
		c.reachable();
		if (isProperty && isMTSRepresentation(c)) {
			throw new RuntimeException("Properties must be LTSs");
		}
		if (isOptimistic) {
			c = (CompactState) TransitionSystemDispatcher.getOptimisticModel(c,
					output);
		}
		if (isPessimistic) {
			c = (CompactState) TransitionSystemDispatcher.getPessimistModel(c);
		}
		if (isMinimal) {
			c = TransitionSystemDispatcher.minimise(c, output);
		}
		if (isDeterministic) {
			c = TransitionSystemDispatcher.determinise(c, output);
		}
		if (isClousure) {
			c = TransitionSystemDispatcher.getTauClosure(c, output);
		}
		if (isAbstract) {
			c = TransitionSystemDispatcher.getAbstractModel(c, output);
		}
		if (isStarEnv) {
			c = TransitionSystemDispatcher.getStarEnv(c, output);
		}
		return c;
	}

	// is the first state = ERROR ie P = ERROR?
	void check_for_ERROR(CompactState c) {
		Integer i = explicitStates.get(name);
		if (i.intValue() == Declaration.ERROR) {
			c.states = new EventState[1];
			c.maxStates = 1;
			c.states[0] = EventStateUtils.add(c.states[0], new EventState(
					Declaration.TAU, Declaration.ERROR));
		}
	}

	void addSequential(Integer state, CompactState mach) {
		if (sequentialInserts == null)
			sequentialInserts = new HashMap<>();
		sequentialInserts.put(state, mach);
	}

	void preAddSequential(Integer start, Integer end, CompactState mach) {
		if (preInsertsLast == null)
			preInsertsLast = new Hashtable<>();
		if (preInsertsMach == null)
			preInsertsMach = new Hashtable<>();
		preInsertsLast.put(start, end);
		preInsertsMach.put(start, mach);
	}

	private void insertSequential(int[] map) {
		if (preInsertsMach == null)
			return;
		Enumeration e = preInsertsMach.keys();
		while (e.hasMoreElements()) {
			Integer start = (Integer) e.nextElement();
			CompactState mach = (CompactState) preInsertsMach.get(start);
			Integer end = (Integer) preInsertsLast.get(start);
			Integer newStart = new Integer(map[start.intValue()]);
			mach.offsetSeq(newStart.intValue(),
					end.intValue() >= 0 ? map[end.intValue()] : end.intValue());
			addSequential(newStart, mach);
		}
	}

	private Integer number(Integer alias, Counter newLabel) {
		if (preInsertsMach == null)
			return newLabel.label();
		CompactState mach = (CompactState) preInsertsMach.get(alias);
		if (mach == null)
			return newLabel.label();
		return newLabel.interval(mach.maxStates);
	}

	private void crunch(int index, int[] map) {
		int newi = map[index];
		while (newi >= 0 && newi != map[newi])
			newi = map[newi];
		map[index] = newi;
	}

	private void renumber() { // relabel states
		int map[] = new int[stateLabel.lastLabel().intValue()];
		for (int i = 0; i < map.length; ++i)
			map[i] = i;
		// apply alias
		Enumeration e = aliases.keys();
		while (e.hasMoreElements()) {
			Integer targ = (Integer) e.nextElement();
			Integer alias = (Integer) aliases.get(targ);
			map[targ.intValue()] = alias.intValue();
		}
		// crunch aliases
		for (int i = 0; i < map.length; ++i)
			crunch(i, map);
		// renumber
		Counter newLabel = new Counter(0);
		Hashtable<Integer, Integer> oldnew = new Hashtable<>();
		for (int i = 0; i < map.length; ++i) {
			Integer alias = new Integer(map[i]);
			if (!oldnew.containsKey(alias)) {
				Integer newi = map[i] >= 0 ? number(alias, newLabel)
						: new Integer(-1);
				oldnew.put(alias, newi);
				map[i] = newi.intValue();
			} else {
				Integer newi = (Integer) oldnew.get(alias);
				map[i] = newi.intValue();
			}
		}
		// create offset insert sequential processes
		insertSequential(map);
		// renumber state/local process lookip table
		Set<String> states = explicitStates.keySet();
		for (String s : states) {
			Integer ii = explicitStates.get(s);
			if (ii.intValue() >= 0)
				explicitStates.put(s, new Integer(map[ii.intValue()]));
		}
		stateLabel = newLabel;
	}

	public void print(LTSOutput output) {
		Preconditions.checkNotNull(output, "The output cannot be null");
		// print the process name
		output.outln("PROCESS: " + name);
		// print alphabet
		output.outln("ALPHABET:");
		alphabet.keySet().stream()
				.forEach(s -> output.outln("\t" + alphabet.get(s) + "\t" + s));
		// print states
		output.outln("EXPLICIT STATES:");

		explicitStates
				.keySet()
				.stream()
				.forEach(
						s -> output.outln("\t" + explicitStates.get(s) + "\t"
								+ s));
		// print transitions
		output.outln("TRANSITIONS:");
		transitions.forEach(t -> output.outln("\t" + t));
	}

	static String paramString(Vector<Value> v) {
		int max = v.size() - 1;
		StringBuffer buf = new StringBuffer();
		Enumeration e = v.elements();
		buf.append("(");
		for (int i = 0; i <= max; i++) {
			String s = e.nextElement().toString();
			buf.append(s);
			if (i < max) {
				buf.append(",");
			}
		}
		buf.append(")");
		return buf.toString();
	}

	/**
	 * adds the event to the alphabet of the automaton
	 * 
	 * @param event
	 *            the event to be added (if the event is already in the alphabet
	 *            its index is updated
	 * @throws NullPointerException
	 *             if the event is null
	 */
	public void addEvent(String event) {
		Preconditions.checkNotNull(event,
				"The event to be considered cannot be null");
		alphabet.put(event, eventLabel.label());

	}

	/**
	 * adds a transition to the LTS
	 * 
	 * @param transition
	 *            the transition to be added
	 */
	public void addTransition(Transition transition) {
		Preconditions.checkNotNull(transition,
				"The transition to be considered cannot be null");
		this.transitions.add(transition);
	}

	/**
	 * returns the states of the StateMachine
	 * 
	 * @return the states of the state machine
	 */
	public Set<String> getStates() {
		return this.explicitStates.keySet();
	}

	/**
	 * adds the state and the corresponding id
	 * 
	 * @param state
	 *            the state to be added
	 * @param id
	 *            the id of the state
	 * @throws NullPointerException
	 *             if the state is null
	 */
	@Deprecated
	public void addState(String state, int id) {
		Preconditions.checkNotNull(state,
				"The state to be considered cannot be null");
		this.explicitStates.put(state, id);
	}

	/**
	 * adds the state to the state machine
	 * 
	 * @param state
	 *            the state to be added
	 * @throws NullPointerException
	 *             if the state is null
	 */
	public void addState(String state) {
		Preconditions.checkNotNull(state,
				"The state to be considered cannot be null");

		if (state.equals("ERROR")) {
			this.explicitStates.put("ERROR", new Integer(Declaration.ERROR));
		}

		this.explicitStates.put(state, stateLabel.label());
	}

	/**
	 * returns the index associated with the state
	 * 
	 * @param state
	 *            the state to be considered
	 * @return the id associated with the state
	 * @throws NullPointerException
	 *             if the state is null
	 */
	public Integer getStateIndex(String state) {
		Preconditions.checkNotNull(state,
				"The state to be considered cannot be null");
		return this.explicitStates.get(state);
	}

	/**
	 * returns the alphabet of the automaton
	 * 
	 * @return the alphabet of the automaton
	 */
	public Set<String> getAlphabet() {
		return alphabet.keySet();
	}
}