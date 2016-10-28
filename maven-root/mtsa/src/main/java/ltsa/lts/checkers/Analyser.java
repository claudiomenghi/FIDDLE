package ltsa.lts.checkers;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import ltsa.lts.animator.Animator;
import ltsa.lts.animator.ModelExplorerContext;
import ltsa.lts.animator.PartialOrder;
import ltsa.lts.animator.StackChecker;
import ltsa.lts.animator.StateCodec;
import ltsa.lts.automata.automaton.Automata;
import ltsa.lts.automata.automaton.event.LTSEvent;
import ltsa.lts.automata.lts.Alphabet;
import ltsa.lts.automata.lts.LTSConstants;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.gui.EventManager;
import ltsa.lts.ltl.FluentTrace;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.operations.composition.parallel.CompositionEngine;
import ltsa.lts.operations.composition.parallel.CompositionEngineFactory;
import ltsa.lts.operations.composition.parallel.StackCheck;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.util.Counter;
import ltsa.lts.util.LTSUtils;
import ltsa.lts.util.Options;
import ltsa.lts.util.collections.MyHashQueue;
import ltsa.lts.util.collections.MyHashQueueEntry;
import ltsa.lts.util.collections.MyList;

import com.google.common.base.Preconditions;

public class Analyser implements Animator, Automata {

	private ModelExplorerContext explorerContext;

	LinkedList<String> trace;

	int errorMachine;

	// if a number of non deterministic events for a single choice
	// choose one of them at random
	Random rand = new Random();

	// Animator routines
	// ---------------------------------------------------------
	private String[] menuAlpha;

	private Hashtable<Integer, Integer> actionToIndex; // maps action
														// number(key) to index

	private Hashtable<Integer, Integer> indexToAction; // maps index(key) to
														// action number

	// Animator state
	private int[] currentA; // current state

	volatile private List choices; // set of eligible choices

	private boolean errorState = false;

	private Enumeration replay = null; // records replay state

	private String _replayAction = null; // records next replay action

	private MyList compTrans; // list of transitions

	/**
	 * the composite being operated on
	 */
	protected CompositeState cs;

	/**
	 * array of state machines to be composed
	 */
	private LabelledTransitionSystem[] stateMachines;

	/**
	 * interface for text output
	 */
	private LTSOutput output;

	/**
	 * map action name to bitmap of shared machines
	 */
	private Map<String, BitSet> alphabet = new HashMap<>();

	/**
	 * map action name to new number
	 */
	private Map<String, Integer> actionMap = new HashMap<>();

	/**
	 * number of machines which share this action
	 */
	private int[] actionCount;

	/**
	 * map number to name
	 */
	private String[] actionName;

	/**
	 * number of machines to be composed
	 */
	private int machineNumber;

	/**
	 * array of [Nmach] coding bases
	 */
	private int[] mBase;

	/**
	 * composition strategy
	 */
	private CompositionEngine compositionEngine;

	/**
	 * number of states analysed
	 */
	private int stateCount = 0;

	private boolean[] violated; // true if this property already violated

	// animation variables
	private EventManager eman;

	// maximal progress - ie. this action is low priority
	private boolean lowpriority = true; // specified actions are low priority

	private Vector<String> priorLabels = null;

	private BitSet highAction = null; // actions with high priority

	private int acceptEvent = -1; // number of acceptance label @NAME

	private int asteriskEvent = -1; // number of asterisk event

	private BitSet visible; // BitSet of visible actions

	private StateCodec coder;

	private boolean canTerminate = false; // alpha(nonTerm) subset alpha(term)

	public static boolean partialOrderReduction = false;

	public static boolean preserveObsEquiv = true;

	private PartialOrder partial = null;

	public Analyser(CompositeState cs, LTSOutput output, EventManager eman) {
		this(cs, output, eman, false);
	}

	public Analyser(CompositeState cs, LTSOutput output, EventManager eman,
			boolean ignoreAsterix) {
		Preconditions.checkNotNull(cs, "The composite state cannot be null");
		Preconditions.checkNotNull(output, "The output cannot be null");

		this.explorerContext = new ModelExplorerContext();
		this.cs = cs;
		this.output = output;
		this.eman = eman;
		// deal with priority labels if any
		if (cs.priorityLabels != null) {
			lowpriority = cs.priorityIsLow;
			priorLabels = cs.priorityLabels;
			highAction = new BitSet();
			explorerContext.highAction = highAction;
		}
		stateMachines = new LabelledTransitionSystem[cs.getMachines().size()];
		explorerContext.sm = stateMachines;

		violated = new boolean[cs.getMachines().size()];
		explorerContext.violated = violated;
		
		Enumeration<LabelledTransitionSystem> e = cs.getMachines().elements();
		for (int i = 0; e.hasMoreElements(); i++) {
			LabelledTransitionSystem machine=e.nextElement();
			stateMachines[i] = machine.myclone();
		}
		this.machineNumber = stateMachines.length;
		explorerContext.Nmach = machineNumber;

		// print composition name
		printCompositionName(cs, output);

		// print low priority label set
		if (priorLabels != null) {
			if (lowpriority)
				output.out("\t>> ");
			else
				output.out("\t<< ");
			output.outln((new Alphabet(cs.priorityLabels)).toString());
		}
		// print and compute state space size
		printStateMachineSize(output);

		// set up shared alphabet structure
		Set<String> terminating = new HashSet<>();
		Set<String> nonterminating = new HashSet<>();
		Counter newLabel = new Counter(0);
		for (int i = 0; i < stateMachines.length; i++) {
			for (int j = 0; j < stateMachines[i].getAlphabet().length; j++) {
				if (!stateMachines[i].getAlphabet()[j].contains("?")) { // omit
																		// the
					// maybe
					// actions
					// compute sets of labels for term and non-terminating
					// processes
					if (stateMachines[i].getEndOfSequenceIndex() > 0) {
						terminating.add(stateMachines[i].getAlphabet()[j]);
					} else {
						nonterminating.add(stateMachines[i].getAlphabet()[j]);
					}
					// what machines have what labels
					BitSet b = alphabet.get(stateMachines[i].getAlphabet()[j]);
					if (b == null) {
						b = new BitSet();
						b.set(i);
						String s = stateMachines[i].getAlphabet()[j];
						alphabet.put(s, b);
						actionMap.put(s, newLabel.label());
					} else
						b.set(i);
				}
			}
		}

		canTerminate = terminating.containsAll(nonterminating);
		explorerContext.canTerminate = canTerminate;

		actionName = new String[alphabet.size()];
		actionCount = new int[alphabet.size()];
		explorerContext.actionCount = actionCount;
		explorerContext.actionName = actionName;

		Iterator<String> iterator = alphabet.keySet().iterator();
		while (iterator.hasNext()) {
			String s = iterator.next();
			BitSet b = alphabet.get(s);
			int index = actionMap.get(s).intValue();
			actionName[index] = s;
			actionCount[index] = countSet(b);
			if (s.charAt(0) == '@') {
				acceptEvent = index; // assumes only one acceptance label
				explorerContext.acceptEvent = acceptEvent;
			} else {
				if (s.equals("*")) {
					if (!ignoreAsterix) {
						asteriskEvent = index;
						explorerContext.asteriskEvent = index;
					}
				}

			}
			// initialize low priority action bitSet
			if (highAction != null) {
				if (!lowpriority) {
					if (LabelledTransitionSystem.contains(s, priorLabels))
						highAction.set(index);
				} else if (!LabelledTransitionSystem.contains(s, priorLabels))
					highAction.set(index);
			}
		}
		// set priority for tau & accept label
		if (highAction != null) {
			if (lowpriority) {
				highAction.set(0);
				highAction.set(1); // tau? is also high
			} else
				highAction.clear(0);
			if (acceptEvent > 0)
				highAction.clear(acceptEvent); // accept labels are always low
												// priority
		}
		actionCount[0] = 0; // tau

		// renumber all transitions with new action numbers
		for (int i = 0; i < stateMachines.length; i++) {
			for (int j = 0; j < stateMachines[i].getMaxStates(); j++) {
				LTSTransitionList p = stateMachines[i].getStates()[j];
				while (p != null) {
					LTSTransitionList tr = p;
					tr.setMachine(i);
					Preconditions.checkArgument(actionMap
							.containsKey(stateMachines[i].getAlphabet()[tr
									.getEvent()]), "The label "
							+ stateMachines[i].getAlphabet()[tr.getEvent()]
							+ "is not contained in the action map");
					tr.setEvent((actionMap.get(stateMachines[i].getAlphabet()[tr
							.getEvent()])).intValue());
					while (tr.getNondet() != null) {
						tr.getNondet().setEvent(tr.getEvent());
						tr.getNondet().setMachine(tr.getMachine());
						tr = tr.getNondet();
					}
					p = p.getList();
				}
			}
		}
		// compute visible set
		visible = new BitSet(actionName.length);
		explorerContext.visible = visible;
		for (int i = 1; i < actionName.length; ++i) {
			if (cs.getHidden() == null) {
				visible.set(i);
			} else {
				if (cs.exposeNotHide) {
					if (LabelledTransitionSystem.contains(actionName[i],
							cs.getHidden())) {
						visible.set(i);
					}
				} else {
					if (!LabelledTransitionSystem.contains(actionName[i],
							cs.getHidden())) {
						visible.set(i);
					}
				}
			}
		}
		if(actionMap.containsKey(LTLf2LTS.endSymbol.getValue())){
			explorerContext.endEvent=actionMap.get(LTLf2LTS.endSymbol.getValue());
		}
	}

	private void printStateMachineSize(LTSOutput output) {
		mBase = new int[machineNumber];
		output.outln("State Space:");
		for (int i = 0; i < stateMachines.length; i++) {
			output.out(" " + stateMachines[i].getMaxStates() + " ");
			if (i < stateMachines.length - 1)
				output.out("*");
			mBase[i] = stateMachines[i].getMaxStates();
		}
		coder = new StateCodec(mBase);
		output.outln("= 2 ** " + coder.bits());
	}

	private void printCompositionName(CompositeState cs, LTSOutput output) {
		output.outln("Composition:");
		output.out(cs.getName() + " = ");
		for (int i = 0; i < stateMachines.length; i++) {
			output.out(stateMachines[i].getName());
			if (i < stateMachines.length - 1) {
				output.out(" || ");
			}
		}
		output.outln("");
	}

	public LabelledTransitionSystem compose() {
		return privateCompose(true);
	}

	public LabelledTransitionSystem composeNoHide() {
		return privateCompose(false);
	}

	/**
	 * it performs the composition of LTS
	 * 
	 * @param dohiding
	 * @return
	 */
	private LabelledTransitionSystem privateCompose(boolean dohiding) {
		output.outln("Composing...");
		long start = System.currentTimeMillis();
		newStateCompose();
		LabelledTransitionSystem c = new LabelledTransitionSystem(
				explorerContext, cs.getName(),
				compositionEngine.getExploredStates(), compTrans, actionName,
				explorerContext.endSequence);
		if (dohiding && cs.getHidden() != null) {
			if (!cs.exposeNotHide) {
				c.conceal(cs.getHidden());
			} else {
				c.expose(cs.getHidden());
			}
		}

		long finish = System.currentTimeMillis();
		outStatistics(explorerContext.stateCount, compTrans.size());
		output.outln("Composed in " + (finish - start) + "ms");
		compositionEngine.teardown();
		compTrans = null;
		explorerContext.compTrans = null;
		return c;
	}

	public boolean analyse(FluentTrace tracer) {
		boolean hasErrors = true;
		output.outln("Analysing...");
		System.gc(); // garbage collect before start
		long start = System.currentTimeMillis();

		int ret = analizeState(true, true, coder.zero(), null);
		long finish = System.currentTimeMillis();
		if (ret == LTSConstants.DEADLOCK) {
			output.outln("Trace to DEADLOCK:");
			tracer.print(output, trace, true);
			
		} else {
			if (ret == LTSConstants.ERROR) {
				output.outln("Trace to property violation in "
						+ stateMachines[errorMachine].getName() + ":");
				tracer.print(output, trace, true);
				cs.satisfied=false;
			} else {
				hasErrors = false;
				output.outln("No deadlocks/errors");
			}
		}
		output.outln("Analysed in: " + (finish - start) + "ms");
		return hasErrors;
	}

	public boolean analyse(boolean checkDeadlocks) {
		boolean hasErrors = true;
		output.outln("Analysing...");
		System.gc(); // garbage collect before start
		long start = System.currentTimeMillis();
		int ret = analizeState(checkDeadlocks, false, coder.zero(), null);
		long finish = System.currentTimeMillis();
		if (ret == LTSConstants.DEADLOCK) {
			output.outln("Trace to DEADLOCK:");
			printPath(trace);
		} else if (ret == LTSConstants.ERROR) {
			output.outln("Trace to property violation in "
					+ stateMachines[errorMachine].getName() + ":");
			printPath(trace);
		} else {
			hasErrors = false;
			output.outln("No deadlocks/errors");
		}
		output.outln("Analysed in: " + (finish - start) + "ms");
		return hasErrors;
	}

	public List<String> getErrorTrace() {
		return trace;
	}

	// Count the number of bits set in a bitSet
	private int countSet(BitSet b) {
		int count = 0;
		for (int i = 0; i < b.size(); i++)
			if (b.get(i))
				count++;
		return count;
	}

	/*
	 * 
	 * state is represented by int[Nmach+1] where Nmach+1 is used to store the
	 * event into this transition
	 */

	private boolean isEND(int[] state) {
		if (!canTerminate)
			return false;
		for (int i = 0; i < machineNumber; i++) {
			if (stateMachines[i].getEndOfSequenceIndex() < 0)
				; // skip
			else if (stateMachines[i].getEndOfSequenceIndex() != state[i])
				return false;
		}
		return true;
	}

	private List<int[]> eligibleTransitions(int[] state) {
		List<int[]> asteriskTransitions = null;
		if (partial != null) {
			if (asteriskEvent > 0
					&& LTSTransitionList
							.hasEvent(stateMachines[machineNumber - 1]
									.getStates()[state[machineNumber - 1]],
									asteriskEvent)) {
				// do nothing
			} else {
				List<int[]> parTrans = partial.transitions(state);
				if (parTrans != null)
					return parTrans;
			}
		}
		int[] ac = LTSUtils.myclone(actionCount);
		LTSTransitionList[] trs = new LTSTransitionList[actionCount.length];
		int nsucc = 0; // count of feasible successor transitions
		int highs = 0; // eligible high priority actions
		// for each machine
		for (int i = 0; i < machineNumber; i++) {
			LTSTransitionList p = stateMachines[i].getStates()[state[i]];
			// for each transition
			while (p != null) {
				LTSTransitionList tr = p;
				tr.setPath(trs[tr.getEvent()]);
				trs[tr.getEvent()] = tr;
				ac[tr.getEvent()]--;
				if (tr.getEvent() != 0 && ac[tr.getEvent()] == 0) {
					nsucc++; // ignoring tau, this transition is possible
					// bugfix 26-mar-04 to handle asterisk + priority
					if (highAction != null && highAction.get(tr.getEvent())
							&& tr.getEvent() != asteriskEvent) {
						++highs;
					}
				}
				p = p.getList();
			}
		}
		if (nsucc == 0 && trs[0] == null) {
			return null; // DEADLOCK - no successor states
		}
		int actionNo = 1;
		List<int[]> transitions = new ArrayList<>(8);
		// we include tau if it is high priority or its low and there are no
		// high priority transitions
		if (trs[0] != null) {
			boolean highTau = (highAction != null && highAction.get(0));
			if (highTau || highs == 0) {
				computeTauTransitions(trs[0], state, transitions);
			}
			if (highTau) {
				++highs;
			}
		}
		while (nsucc > 0) { // do this loop once per successor state
			nsucc--;
			// find number of action
			while (ac[actionNo] > 0)
				actionNo++;
			// now compute the state for this action if not excluded tock
			if (highs > 0 && !highAction.get(actionNo)
					&& actionNo != acceptEvent)
				;// do nothing
			else {
				LTSTransitionList tr = trs[actionNo];
				boolean nonDeterministic = false;
				while (tr != null) { // test for non determinism
					if (tr.getNondet() != null) {
						nonDeterministic = true;
						break;
					}
					tr = tr.getPath();
				}
				tr = trs[actionNo];
				if (!nonDeterministic) {
					int[] next = LTSUtils.myclone(state);
					next[machineNumber] = actionNo;
					while (tr != null) {
						next[tr.getMachine()] = tr.getNext();
						tr = tr.getPath();
					}
					if (actionNo != asteriskEvent)
						transitions.add(next);
					else {
						asteriskTransitions = new ArrayList<>(1);
						asteriskTransitions.add(next);
					}
				} else if (actionNo != asteriskEvent)
					computeNonDetTransitions(tr, state, transitions);
				else
					computeNonDetTransitions(tr, state,
							asteriskTransitions = new ArrayList<>(4));
			}
			++ac[actionNo];
		}
		if (asteriskEvent < 0)
			return transitions;
		else
			return mergeAsterisk(transitions, asteriskTransitions);
	}

	private void computeTauTransitions(LTSTransitionList first, int[] state,
			List<int[]> v) {
		LTSTransitionList down = first;
		while (down != null) {
			LTSTransitionList across = down;
			while (across != null) {
				int[] next = LTSUtils.myclone(state);
				next[across.getMachine()] = across.getNext();
				next[machineNumber] = 0; // tau
				v.add(next);
				across = across.getNondet();
			}
			down = down.getPath();
		}
	}

	private void computeNonDetTransitions(LTSTransitionList first, int[] state,
			List<int[]> v) {
		LTSTransitionList tr = first;
		while (tr != null) {
			int[] next = LTSUtils.myclone(state);
			next[tr.getMachine()] = tr.getNext();
			if (first.getPath() != null)
				computeNonDetTransitions(first.getPath(), next, v);
			else {
				next[machineNumber] = first.getEvent();
				v.add(next);
			}
			tr = tr.getNondet();
		}
	}

	List<int[]> mergeAsterisk(List<int[]> transitions,
			List<int[]> asteriskTransitions) {
		if (transitions == null || asteriskTransitions == null)
			return transitions;
		if (transitions.size() == 0) {
			return null;
		}
		int[] asteriskTransition;
		if (asteriskTransitions.size() == 1) {
			asteriskTransition = (int[]) asteriskTransitions.get(0);
			Iterator<int[]> e = transitions.iterator();
			while (e.hasNext()) {
				int[] next = e.next();
				if (!visible.get(next[machineNumber])) {
					// fragile, assumes property is last machine!!
					next[machineNumber - 1] = asteriskTransition[machineNumber - 1];
				}
			}
			return transitions;
		} else {
			Iterator<int[]> a = asteriskTransitions.iterator();
			List<int[]> newTransitions = new ArrayList<>();
			while (a.hasNext()) {
				asteriskTransition = a.next();
				Iterator<int[]> e = transitions.iterator();
				while (e.hasNext()) {
					int[] next = (int[]) e.next();
					if (!visible.get(next[machineNumber])) {
						// fragile, assumes property is the last machine!
						next[machineNumber - 1] = asteriskTransition[machineNumber - 1];
					}
					newTransitions.add(LTSUtils.myclone(next));
				}
			}
			return newTransitions;
		}
	}

	private void outStatistics(int states, int transitions) {
		Runtime r = Runtime.getRuntime();
		output.outln("-- States: " + states + " Transitions: " + transitions
				+ " Memory used: " + (r.totalMemory() - r.freeMemory()) / 1000
				+ "K");
	}

	/**
	 * performs the composition of LTS
	 * 
	 * @return the composition of LTS
	 */
	private int newStateCompose() {
		System.gc(); // garbage collect before start

		// composes the state machines
		this.compositionEngine = CompositionEngineFactory
				.createCompositionEngine(Options.getCompositionStrategyClass(),
						coder);
		this.compositionEngine.initialize();
		this.compositionEngine.setModelExplorerContext(explorerContext);

		if (partialOrderReduction) {
			this.partial = new PartialOrder(alphabet, actionName,
					stateMachines, new StackChecker(coder,
							compositionEngine.getStackChecker()),
					cs.getHidden(), cs.exposeNotHide, preserveObsEquiv,
					highAction);
			this.explorerContext.partial = partial;
		}

		this.compTrans = new MyList();
		this.explorerContext.compTrans = compTrans;
		this.explorerContext.stateCount = 0;
		this.compositionEngine.add(coder.zero(), 0);

		boolean deadlockReported = false;
		while (!compositionEngine.getExploredStates().empty()) {
			if (compositionEngine.nextStateIsMarked()) {
				compositionEngine.removeNextState();
			} else {
				compositionEngine.processNextState();
				if (!deadlockReported && compositionEngine.deadlockDetected()) {
					output.outln("  potential DEADLOCK");
					deadlockReported = true;
				}

				if (explorerContext.stateCount % 10000 == 0) {
					output.out(compositionEngine.getExplorationStatistics());
					outStatistics(explorerContext.stateCount, compTrans.size());
				}

				if (compositionEngine.getMaxStateGeneration() != LTSConstants.NO_MAX_STATE_GENERATION
						&& explorerContext.stateCount > compositionEngine
								.getMaxStateGeneration()) {
					return LTSConstants.REACHED_THRESHOLD;
				}
			}
		}
		return LTSConstants.SUCCESS;
	}

	private void printPath(LinkedList<String> v) {
		v.stream().forEach(t -> output.outln("\t" + t));
	}

	private int analizeState(boolean checkDeadlocks, boolean callFromTracer,
			byte[] fromState, byte[] target) {
		stateCount = 0;
		explorerContext.stateCount = 0;

		int nTrans = 0; // number of transitions
		MyHashQueue queue = new MyHashQueue(100001);

		if (partialOrderReduction) {
			partial = new PartialOrder(alphabet, actionName, stateMachines,
					new StackChecker(coder, queue), cs.getHidden(),
					cs.exposeNotHide, false, highAction);
			explorerContext.partial = partial;
		}
		queue.addPut(fromState, 0, null);

		MyHashQueueEntry qe = null;
		while (!queue.empty()) {
			qe = queue.peek();
			fromState = qe.key;
			int[] state = coder.decode(fromState);
			stateCount++;
			explorerContext.stateCount++;
			if (stateCount % 10000 == 0) {
				output.out("Depth " + queue.depth(qe) + " ");
				outStatistics(stateCount, nTrans);
			}

			// determine eligible transitions
			List<int[]> transitions = eligibleTransitions(state);
			queue.pop();
			if (transitions == null && (checkDeadlocks || callFromTracer)) {
				if (!isEND(state)) {
					output.out("Depth " + queue.depth(qe) + " ");
					outStatistics(stateCount, nTrans);
					trace = queue.getPath(qe, actionName);
					return LTSConstants.DEADLOCK;
				}
			} else {
				if (transitions != null) {
					Iterator<int[]> transitionsIterator = transitions
							.iterator();
					while (transitionsIterator.hasNext()) {
						int[] next = transitionsIterator.next();
						byte[] code = coder.encode(next);
						nTrans++;
						if (code == null || StateCodec.equals(code, target)) {
							output.out("Depth " + queue.depth(qe) + " ");
							outStatistics(stateCount, nTrans);
							if (code == null) {
								int i = 0;
								while (next[i] >= 0)
									i++;
								errorMachine = i;
							}
							trace = queue.getPath(qe, actionName);
							trace.add(actionName[next[machineNumber]]); // last
																		// action
							// to
							// ERROR
							if (!checkDeadlocks || callFromTracer) {
								if (code == null) {
									return LTSConstants.ERROR;
								} else {
									return LTSConstants.FOUND;
								}
							}
						} else if (!queue.containsKey(code)) {
							queue.addPut(code, next[machineNumber], qe);
						}
					}
				}
			}
		}
		output.out("Depth " + queue.depth(qe) + " ");
		outStatistics(stateCount, nTrans);

		return LTSConstants.SUCCESS;
	}

	/**
	 * Implement Automata Interface and returns the alphabet
	 * 
	 * @return the alphabet
	 */
	@Override
	public String[] getAlphabet() {
		return actionName;
	}

	/**
	 * returns the transitions from a particular state
	 * 
	 * @return the transitions from a particular state
	 */
	@Override
	public MyList getTransitions(byte[] state) {
		List<int[]> ex = eligibleTransitions(coder.decode(state));
		MyList trs = new MyList();
		if (ex == null)
			return trs;
		Iterator<int[]> e = ex.iterator();
		while (e.hasNext()) {
			int[] next = (int[]) e.next();
			byte[] code = coder.encode(next);
			if (code == null) {
				int i = 0;
				while (next[i] >= 0)
					i++;
				errorMachine = i;
			}
			trs.add(0, code, next[machineNumber]);
		}
		return trs;
	}

	// assumes property is Machine Nmach-1
	@Override
	public boolean isAccepting(byte[] state) {
		if (acceptEvent < 0)
			return false;
		int[] ds = coder.decode(state);
		return LTSTransitionList
				.hasEvent(
						stateMachines[machineNumber - 1].getStates()[ds[machineNumber - 1]],
						acceptEvent);
	}

	@Override
	public String getViolatedProperty() {
		return stateMachines[errorMachine].getName();
	}

	// returns shortest trace to state (vector of Strings)
	@Override
	public Vector getTraceToState(byte[] from, byte[] to) {
		if (StateCodec.equals(from, to))
			return new Vector();
		int ret = analizeState(true, true, from, to);
		if (ret == LTSConstants.FOUND) {
			Vector v = new Vector();
			v.addAll(trace);
			return v;
		}
		return null;
	}

	// return the number of the END state
	@Override
	public boolean end(byte[] state) {
		return isEND(coder.decode(state));
	}

	// return the number of the START state
	@Override
	public byte[] start() {
		return coder.zero();
	}

	// set the Stack Checker for partial order reduction
	@Override
	public void setStackChecker(StackCheck s) {
		if (partialOrderReduction) {
			partial = new PartialOrder(alphabet, actionName, stateMachines,
					new StackChecker(coder, s), cs.getHidden(),
					cs.exposeNotHide, false, highAction);
			explorerContext.partial = partial;
		}
	}

	// returns true if partial order reduction
	@Override
	public boolean isPartialOrder() {
		return partialOrderReduction;
	}

	PartialOrder savedPartial = null;

	// disable partial order
	@Override
	public void disablePartialOrder() {
		savedPartial = partial;
		partial = null;
		explorerContext.partial = null;
	}

	// enable partial order
	@Override
	public void enablePartialOrder() {
		partial = savedPartial;
		explorerContext.partial = partial;
	}

	private void getMenuHash() {
		actionToIndex = new Hashtable<>();
		indexToAction = new Hashtable<>();
		for (int i = 1; i < menuAlpha.length; i++) {
			Integer index = new Integer(i);
			Integer actionNo = actionMap.get(menuAlpha[i]);
			actionToIndex.put(actionNo, index);
			indexToAction.put(index, actionNo);
		}
	}

	private void getMenu(Vector a) {
		if (a != null) {
			Vector validAction = new Vector();
			Enumeration e = a.elements();
			while (e.hasMoreElements()) {
				String s = (String) e.nextElement();
				if (alphabet.containsKey(s))
					validAction.addElement(s);
			}
			menuAlpha = new String[validAction.size() + 1];
			menuAlpha[0] = "tau";
			for (int i = 1; i < menuAlpha.length; i++)
				menuAlpha[i] = (String) validAction.elementAt(i - 1);
		} else {
			menuAlpha = actionName;
		}
		getMenuHash();
		return;
	}

	// create bitmap of eligible actions in menu from choices
	private BitSet menuActions() {
		BitSet b = new BitSet(menuAlpha.length);
		if (choices != null) {
			Iterator e = choices.iterator();
			while (e.hasNext()) {
				int[] next = (int[]) e.next();
				Integer actionNo = new Integer(next[machineNumber]);
				Integer index = (Integer) actionToIndex.get(actionNo);
				if (index != null)
					b.set(index.intValue());
			}
		}
		return b;
	}

	// create bitmap of all eligible actions from choices
	private BitSet allActions() {
		BitSet b = new BitSet(actionCount.length);
		if (choices != null) {
			Iterator e = choices.iterator();
			while (e.hasNext()) {
				int[] next = (int[]) e.next();
				b.set(next[machineNumber]);
			}
		}
		return b;
	}

	@Override
	public BitSet initialise(Vector menu) {
		// set state to 0
		choices = eligibleTransitions(currentA = coder.decode(coder.zero()));
		if (eman != null) {
			// initialise animation to 0
			eman.post(new LTSEvent(LTSEvent.NEWSTATE, currentA));
		}
		getMenu(menu);
		// initialize possible replay trace
		if (cs.getErrorTrace() != null) {
			replay = cs.getErrorTrace().elements();
			if (replay.hasMoreElements())
				_replayAction = (String) replay.nextElement();
		}
		return menuActions();
	}

	@Override
	public BitSet singleStep() {
		if (errorState)
			return null;
		if (nonMenuChoice()) {
			currentA = step(randomNonMenuChoice());
			if (errorState)
				return null;
			choices = eligibleTransitions(currentA);
		}
		return menuActions();
	}

	@Override
	public BitSet menuStep(int choice) {
		if (errorState)
			return null;
		theChoice = ((Integer) indexToAction.get(new Integer(choice)))
				.intValue();
		currentA = step(theChoice);
		if (errorState)
			return null;
		choices = eligibleTransitions(currentA);
		return menuActions();
	}

	// choice of event to run until nothing but menu event
	int theChoice = 0;

	// action chosen - only valid after a step
	@Override
	public int actionChosen() {
		return theChoice;
	}

	// action name chosen - only valid after a step
	@Override
	public String actionNameChosen() {
		return actionName[theChoice];
	}

	@Override
	public boolean nonMenuChoice() {
		if (errorState)
			return false;
		BitSet b = allActions();
		for (int i = 0; i < b.size(); i++) {
			if (b.get(i) && !actionToIndex.containsKey(new Integer(i))) {
				theChoice = i;
				return true;
			}
		}
		return false;
	}

	private int randomNonMenuChoice() {
		BitSet b = allActions();
		List<Integer> nmc = new ArrayList<>(8);
		for (int i = 0; i < b.size(); i++) {
			Integer II = new Integer(i);
			if (b.get(i) && !actionToIndex.containsKey(II)) {
				nmc.add(II);
			}
		}
		int i = (Math.abs(rand.nextInt())) % nmc.size();
		theChoice = ((Integer) nmc.get(i)).intValue();
		return theChoice;
	}

	// returns true if next element in the trace is eligible
	@Override
	public boolean traceChoice() {
		if (errorState)
			return false;
		if (replay == null)
			return false;
		if (_replayAction != null) {
			int i = ((Integer) actionMap.get(_replayAction)).intValue();
			BitSet b = allActions();
			if (b.get(i)) {
				theChoice = i;
				return true;
			}
		}
		return false;
	}

	// is there an error trace
	@Override
	public boolean hasErrorTrace() {
		return (cs.getErrorTrace() != null);
	}

	// execute next step in trace
	@Override
	public BitSet traceStep() {
		if (errorState)
			return null;
		if (traceChoice()) {
			currentA = step(theChoice);
			if (errorState)
				return null;
			choices = eligibleTransitions(currentA);
			if (replay.hasMoreElements()) {
				_replayAction = (String) replay.nextElement();
			} else
				_replayAction = null;
		}
		return menuActions();
	}

	@Override
	public boolean isError() {
		return errorState;
	}

	/**
	 * return true if END state has been reached
	 */
	@Override
	public boolean isEnd() {
		return isEND(currentA);
	}

	private int[] thestep(int action) { // take step from current
		if (errorState)
			return currentA;
		if (choices == null) {
			output.outln("DEADLOCK");
			errorState = true;
			return currentA;
		}
		// now compute the state for this action
		Iterator e = choices.iterator();
		while (e.hasNext()) {
			int[] next = (int[]) e.next();
			if (next[machineNumber] == action) {
				next = nonDetSelect(next);
				// output.outln(" "+actionName[action]);
				errorState = (coder.encode(next) == null);
				if (errorState) {/* output.outln("ERROR STATE"); */
					return next;
				}
				return currentA = next;
			}
		}
		return currentA;
	}

	private int[] step(int action) { // take step from current and post it
		int[] tmp = thestep(action);
		if (eman != null) {
			eman.post(new LTSEvent(LTSEvent.NEWSTATE, tmp, actionName[action]));
		}
		return tmp;
	}

	int[] nonDetSelect(int[] x) {
		int start = choices.indexOf(x);
		int last = start + 1;
		while (last < choices.size()
				&& x[machineNumber] == ((int[]) choices.get(last))[machineNumber])
			last++;
		if (start + 1 == last)
			return x;
		// otherwise do random choice
		int i = start + (Math.abs(rand.nextInt())) % (last - start);
		return (int[]) choices.get(i);
	}

	@Override
	public String[] getMenuNames() {
		return menuAlpha;
	}

	@Override
	public String[] getAllNames() {
		return actionName;
	}

	@Override
	public boolean getPriority() {
		return lowpriority;
	}

	@Override
	public BitSet getPriorityActions() {
		if (priorLabels == null)
			return null;
		BitSet b = new BitSet();
		for (int i = 1; i < actionName.length; i++) {
			Integer ix = ((Integer) actionToIndex.get(new Integer(i)));
			if (ix != null
					&& ((lowpriority && !highAction.get(i)) || (!lowpriority && highAction
							.get(i))))
				b.set(ix.intValue());
		}
		return b;
	}

	@Override
	public void message(String msg) {
		output.outln(msg);
	}

}
