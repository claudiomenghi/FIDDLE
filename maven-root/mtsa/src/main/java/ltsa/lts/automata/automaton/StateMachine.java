package ltsa.lts.automata.automaton;

import static ltsa.lts.util.MTSUtils.MAYBE_MARK;
import static ltsa.lts.util.MTSUtils.addTauMaybeAlphabet;
import static ltsa.lts.util.MTSUtils.computeHiddenAlphabet;
import static ltsa.lts.util.MTSUtils.getAlphabetWithMaybes;
import static ltsa.lts.util.MTSUtils.getMaybeAction;
import static ltsa.lts.util.MTSUtils.isMTSRepresentation;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import ltsa.dispatcher.TransitionSystemDispatcher;
import ltsa.lts.Diagnostics;
import ltsa.lts.EventStateUtils;
import ltsa.lts.automata.automaton.transition.Transition;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.factory.EventStateFactory;
import ltsa.lts.checkers.wellformedness.WellFormednessLTSModifier;
import ltsa.lts.csp.Declaration;
import ltsa.lts.csp.ProcessSpec;
import ltsa.lts.csp.Relation;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.operations.composition.integrator.IntegratorEngine;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.PostconditionDefinitionManager;
import ltsa.lts.parser.PreconditionDefinitionManager;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.util.Counter;
import ltsa.ui.EmptyLTSOuput;
import MTSTools.ac.ic.doc.mtstools.model.MTSConstants;

import com.google.common.base.Preconditions;

public class StateMachine {

	private Map<String, Integer> boxIndexes = new HashMap<>();

	public Map<String, Set<String>> mapBoxInterface = new HashMap<>();

	private Set<String> finalStates = new HashSet<>();

	private PostconditionDefinitionManager postManager;

	private PreconditionDefinitionManager preconditionManager;

	private final String name;
	private final String kludgeName;
	private Map<String, Integer> alphabet = new HashMap<>();
	Vector<String> hidden;
	Relation relabels;

	private Map<String, Integer> explicitStates = new HashMap<>();
	Hashtable<String, Value> constants; // a bit of a kludge, should not be here

	private Counter eventLabel = new Counter(0);
	private Counter stateLabel = new Counter(0);

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

	private boolean isReplacement = false;

	private HashMap<Integer, LabelledTransitionSystem> sequentialInserts;
	Hashtable<Integer, Integer> preInsertsLast;
	Hashtable<Integer, LabelledTransitionSystem> preInsertsMach;
	Hashtable<Integer, Integer> aliases = new Hashtable<>();

	public static LTSOutput output;

	public StateMachine(String name) {
		this.name = name;
		this.kludgeName = name;
	}

	public StateMachine(ProcessSpec spec) {
		// compute machine name
		name = spec.getName();
		kludgeName = name;
		make(spec);
		this.postManager = spec.getPostManager();
	}

	public StateMachine(ProcessSpec spec, Vector<Value> params) {
		name = spec.getName();
		if (params != null) {
			spec.doParams(params);
			kludgeName = name + paramString(params);
		} else
			kludgeName = name;
		make(spec);
		this.postManager = spec.getPostManager();
	}

	public boolean isReplacement() {
		return isReplacement;
	}

	public void setReplacement(boolean isReplacement) {
		this.isReplacement = isReplacement;
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
		this.isProperty = spec.isProperty;
		this.isMinimal = spec.isMinimal;
		this.isDeterministic = spec.isDeterministic;
		this.isOptimistic = spec.isOptimistic;
		this.isPessimistic = spec.isPessimistic;
		this.isAbstract = spec.isAbstract;
		this.isClousure = spec.isClousure;
		this.exposeNotHide = spec.exposeNotHide;
		this.isStarEnv = spec.isStarEnv;
	}

	public Set<String> getFinalStates() {
		return this.finalStates;
	}

	public void addFinalState(String stateName) {
		this.finalStates.add(stateName);
	}

	/**
	 * 
	 * @param preconditions
	 *            true if the compact state should be used for checking
	 *            preconditions
	 * @return
	 */
	public LabelledTransitionSystem makeCompactState() {
		LabelledTransitionSystem c = new LabelledTransitionSystem(kludgeName,
				this.stateLabel.lastLabel().intValue());
		Integer endIndex = explicitStates.get("END");
		if (endIndex != null) {
			c.setEndOfSequence(endIndex.intValue());
		}

		String[] newalphabet = new String[alphabet.size()];
		Set<String> elements = alphabet.keySet();
		for (String s : elements) {
			int j = alphabet.get(s).intValue();
			if ("@".equals(s))
				s = "@" + c.getName();
			newalphabet[j] = s;
		}
		c.setAlphabet(newalphabet);

		if (!isProperty) {
			c.setAlphabet(getAlphabetWithMaybes(c.getAlphabet()));
		} else {
			c.setAlphabet(addTauMaybeAlphabet(c.getAlphabet()));
		}
		alphabet.clear();
		for (int i = 0; i < c.getAlphabet().length; i++) {
			alphabet.put(c.getAlphabet()[i], i);
		}
		c.setStates(new LTSTransitionList[c.getMaxStates()]);

		for (Transition t : this.transitions) {
			String action = "" + t.getEvent();
			if (action.contains(MAYBE_MARK)) {
				action = getMaybeAction(action);
			}
			int ev = getEventIndex(action);
			LTSTransitionList evSt = EventStateFactory.createEventState(ev, t);
			c.getStates()[t.getFrom()] = EventStateUtils.add(
					c.getStates()[t.getFrom()], evSt);
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
		checkForERROR(c);
		// c.reachable();
		if (isProperty && isMTSRepresentation(c)) {
			throw new RuntimeException("Properties must be LTSs");
		}
		if (isOptimistic) {
			c = TransitionSystemDispatcher.getOptimisticModel(c, output);
		}
		if (isPessimistic) {
			c = TransitionSystemDispatcher.getPessimistModel(c);
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
		c.mapBoxInterface = this.mapBoxInterface;

		for (String boxName : c.mapBoxInterface.keySet()) {
			c.addBoxIndex(boxName, this.getStateIndex(boxName));
		}

		for (String finalState : this.finalStates) {
			c.addFinalStateIndex(this.getStateIndex(finalState));
		}

		c.mapBoxInterface = this.mapBoxInterface;

		if (this.postManager != null
				&& this.postManager.getMapBoxPostcondition() != null) {
			Map<String, String> mapBoxPost = this.postManager
					.getMapBoxPostcondition().get(name);
			Map<String, LabelledTransitionSystem> boxPostLTS = new HashMap<>();

			if (mapBoxPost != null) {
				for (Entry<String, String> mapEntry : mapBoxPost.entrySet()) {
					String box = mapEntry.getKey();
					LabelledTransitionSystem cs = this.postManager.compile(
							// this.output,
							new EmptyLTSOuput(), mapEntry.getValue(),
							new ArrayList<>(this.mapBoxInterface.get(box)),
							mapEntry.getValue());

					boxPostLTS.put(box, cs);
				}
			}
			c = new WellFormednessLTSModifier(new EmptyLTSOuput()).modify(c,
					boxPostLTS, LTSCompiler.forPreconditionChecking,
					LTSCompiler.boxOfInterest);
		}

		return c;
	}

	private Integer getEventIndex(String event) {
		Preconditions.checkArgument(this.alphabet.keySet().contains(event),
				"Event: " + event
						+ " not contained in the alphabet of the machine "
						+ this.name);
		return this.alphabet.get(event);
	}

	// is the first state = ERROR ie P = ERROR?
	void checkForERROR(LabelledTransitionSystem c) {
		Preconditions.checkArgument(this.explicitStates.containsKey(name),
				"compact state " + name
						+ " not contained into the explicit states");
		Integer i = explicitStates.get(name);
		if (i.intValue() == Declaration.ERROR) {
			c.setStates(new LTSTransitionList[1]);
			c.getStates()[0] = EventStateUtils.add(c.getStates()[0],
					new LTSTransitionList(Declaration.TAU, Declaration.ERROR));
		}
	}

	public void addSequential(Integer state, LabelledTransitionSystem mach) {
		if (sequentialInserts == null) {
			sequentialInserts = new HashMap<>();
		}
		sequentialInserts.put(state, mach);
	}

	public void preAddSequential(Integer start, Integer end,
			LabelledTransitionSystem mach) {
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
		Enumeration<Integer> e = preInsertsMach.keys();
		while (e.hasMoreElements()) {
			Integer start = e.nextElement();
			LabelledTransitionSystem mach = preInsertsMach.get(start);
			Integer end = preInsertsLast.get(start);
			Integer newStart = new Integer(map[start.intValue()]);
			mach.offsetSeq(newStart.intValue(),
					end.intValue() >= 0 ? map[end.intValue()] : end.intValue());
			addSequential(newStart, mach);
		}
	}

	private Integer number(Integer alias, Counter newLabel) {
		if (preInsertsMach == null)
			return newLabel.label();
		LabelledTransitionSystem mach = preInsertsMach.get(alias);
		if (mach == null)
			return newLabel.label();
		return newLabel.interval(mach.getMaxStates());
	}

	private void crunch(int index, int[] map) {
		int newi = map[index];
		while (newi >= 0 && newi != map[newi])
			newi = map[newi];
		map[index] = newi;
	}

	private void renumber() { // relabel states
		int map[] = new int[this.stateLabel.lastLabel().intValue()];
		for (int i = 0; i < map.length; ++i)
			map[i] = i;
		// apply alias
		Enumeration<Integer> e = aliases.keys();
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
				Integer newi = oldnew.get(alias);
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
		this.stateLabel = newLabel;
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

	/**
	 * returns a String representation of the state machine
	 * 
	 * @return a String representation of the state machine
	 */
	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		// print the process name
		builder.append("PROCESS: " + name + "\n");
		// print alphabet
		builder.append("ALPHABET:" + "\n");
		alphabet.keySet()
				.stream()
				.forEach(
						s -> builder.append("\t" + alphabet.get(s) + "\t" + s
								+ "\n"));
		// print states
		builder.append("EXPLICIT STATES:" + "\n");

		explicitStates
				.keySet()
				.stream()
				.forEach(
						s -> builder.append("\t" + explicitStates.get(s) + "\t"
								+ s + "\n"));
		// print transitions
		builder.append("TRANSITIONS:" + "\n");
		transitions.forEach(t -> builder.append("\t" + t + "\n"));

		builder.append("INTERFACES: " + "\n");
		this.mapBoxInterface
				.entrySet()
				.stream()
				.forEach(
						t -> builder.append("\t box: " + t.getKey()
								+ " interface: " + t.getValue() + "\n"));
		return builder.toString();
	}

	public static String paramString(Vector<Value> v) {
		int max = v.size() - 1;
		StringBuffer buf = new StringBuffer();
		Enumeration<Value> e = v.elements();
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

		this.explicitStates.put(state, this.stateLabel.label());
	}

	/**
	 * returns the index associated with the state
	 * 
	 * @param state
	 *            the state to be considered
	 * @return the id associated with the state
	 * @throws NullPointerException
	 *             if the state is null
	 * @throws IllegalArgumentException
	 *             if the state is not contained into the state of the machine
	 */
	public Integer getStateIndex(String state) {
		Preconditions.checkNotNull(state,
				"The state to be considered cannot be null");
		Preconditions.checkArgument(this.explicitStates.containsKey(state),
				"The state " + state
						+ " is not contained into the state of the machine");
		return this.explicitStates.get(state);
	}

	public Hashtable<String, Value> getConstants() {
		return constants;
	}

	public Relation getRelabels() {
		return relabels;
	}

	public void setRelabels(Relation relabels) {
		this.relabels = relabels;
	}

	public void addBoxIndex(String boxName, int index) {
		this.boxIndexes.put(boxName, index);
	}

	public int getBoxNumber() {
		return this.boxIndexes.size();
	}

	/**
	 * returns the alphabet of the automaton
	 * 
	 * @return the alphabet of the automaton
	 */
	public Set<String> getAlphabet() {
		return alphabet.keySet();
	}

	public Vector<String> getHidden() {
		return hidden;
	}

	public void setHidden(Vector<String> hidden) {
		this.hidden = hidden;
	}

	public Hashtable<Integer, Integer> getAliases() {
		return aliases;
	}

	public void setAliases(Hashtable<Integer, Integer> aliases) {
		this.aliases = aliases;
	}

	public Counter getStateLabel() {
		return stateLabel;
	}
}