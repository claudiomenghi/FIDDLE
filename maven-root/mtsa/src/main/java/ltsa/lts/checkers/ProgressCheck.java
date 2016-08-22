package ltsa.lts.checkers;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import ltsa.lts.animator.StateCodec;
import ltsa.lts.automata.automaton.Automata;
import ltsa.lts.automata.lts.Alphabet;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.util.collections.MyHashProg;
import ltsa.lts.util.collections.MyHashProgEntry;
import ltsa.lts.util.collections.MyList;
import ltsa.lts.util.collections.MyStack;

import com.google.common.base.Preconditions;

public class ProgressCheck {

	public static boolean strongFairFlag = true;

	// essentially Tarjan's algorithm in non-recursive form

	private int sccId;
	private int numberExploredTransitions; // count transitions explored

	private Automata mach;
	private Stack<byte[]> stack;
	int id = 0;
	int ncomp = 0; // number of connected components
	LTSOutput output;
	int violation = 0;
	boolean hasERROR = false;
	static final int Maxviolation = 10;
	String tnames;
	int accept = -1;
	boolean progress;

	ltsa.lts.ltl.FluentTrace tracer = null;

	public ProgressCheck(Automata m, LTSOutput o) {
		Preconditions.checkNotNull(m, "The automaton cannot be null");
		Preconditions.checkNotNull(o, "The output cannot be null");
		mach = m;
		output = o;
	}

	public ProgressCheck(Automata m, LTSOutput o, ltsa.lts.ltl.FluentTrace t) {
		this(m, o);
		tracer = t;
	}

	public void doProgressCheck() {
		progress = true;
		output.outln("Progress Check...");
		long start = System.currentTimeMillis();
		ProgressTest.initTests(mach.getAlphabet());
		stack = new Stack<>();
		findCC(); // compute components
		// output.outln("#connected components = "+ncomp);
		long finish = System.currentTimeMillis();
		if (hasERROR)
			output.outln("Safety property violation detected - check safety.");
		else if (violation == 0)
			output.outln("No progress violations detected.");
		else if (violation > Maxviolation)
			output.outln("More than " + Maxviolation + " violations");
		output.outln("Progress Check in: " + (finish - start) + "ms");
	}

	public void doLTLCheck() {
		progress = false;
		output.outln("LTL Property Check...");
		long start = System.currentTimeMillis();
		accept = acceptLabel(mach.getAlphabet());
		if (accept < 0) {
			output.outln("No labeled acceptance states.");
			return;
		}
		stack = new Stack<>();
		findCC(); // compute components
		long finish = System.currentTimeMillis();
		if (hasERROR)
			output.outln("Safety property violation detected - check safety.");
		else {
			if (violation == 0) {
				output.outln("No LTL Property violations detected.");
			} else {
				if (violation > Maxviolation) {
					output.outln("More than " + Maxviolation + " violations");
				}
			}
		}
		output.outln("LTL Property Check in: " + (finish - start) + "ms");
	}

	public int numberComponents() {
		return ncomp;
	}

	private void findCC() { // non-recursive version
		MyHashProg hh = new MyHashProg();
		MyStack stk = new MyStack();
		mach.setStackChecker(hh);
		sccId = 0;
		numberExploredTransitions = 0; // count transitions explored
		MyHashProgEntry currentState;
		byte[] zero = mach.start();
		stk.push(zero);
		hh.add(zero, null);
		while (!stk.empty()) {
			// return actions
			currentState = hh.get(stk.peek());
			while (currentState.getIsReturn() || currentState.isProcessed) {
				if (currentState.getIsReturn() && !currentState.isProcessed) {
					currentState.isProcessed = true;
					if (currentState.parent != null) {
						currentState.parent.low = Math.min(
								currentState.parent.low, currentState.low);
					}
					if (currentState.low == currentState.dfn) {
						if (component(hh, stack, currentState.key)) {
							return;
						}
					}
				}
				stk.pop();
				if (stk.empty()) {
					outStatistics(sccId, numberExploredTransitions);
					return;
				}
				currentState = hh.get(stk.peek());
			}
			// call actions
			currentState.low = currentState.dfn = ++sccId;
			if (sccId % 10000 == 0) {
				outStatistics(sccId, numberExploredTransitions);
			}
			stack.push(currentState.key);
			// outhse(currentState);
			currentState.setIsReturn(true);
			// next time its a return
			MyList transitions = mach.getTransitions(currentState.key);
			while (!transitions.empty()) {
				++numberExploredTransitions;
				if (transitions.getTo() == null) {
					hasERROR = true;
					return;
				} else if (accept < 0 || transitions.getAction() != accept)
				// ignore accept label transitions
				{
					MyHashProgEntry child = hh.get(transitions.getTo());
					if (child == null) {
						hh.add(transitions.getTo(), currentState);
						stk.push(transitions.getTo());
					} else if (child.dfn == 0) {
						child.parent = currentState;
						stk.push(transitions.getTo());
					} else if (child.dfn < currentState.dfn) {
						currentState.low = Math
								.min(child.dfn, currentState.low);
					}
				}
				transitions.next();
			}
		}
		outStatistics(sccId, numberExploredTransitions);
	}

	private boolean component(MyHashProg hstk, Stack<byte[]> stack,
			byte[] currentState) {
		// mach.disablePartialOrder();
		ncomp++;
		boolean hasAccept = false;
		Stack<byte[]> trace = new Stack<>();
		BitSet hasActions = new BitSet(mach.getAlphabet().length);
		// output.out("{");
		byte[] m;
		byte[] accstate = currentState;
		// fill this out!
		do {
			trace.push(stack.pop());
			m = trace.peek();
			if (progress) {
				// events included in this component
				MyList transitions = mach.getTransitions(m);
				while (!transitions.empty()) {
					int act = transitions.getAction();
					hasActions.set(act);
					transitions.next();
				}
			} else {
				if (!hasAccept) {
					hasAccept = mach.isAccepting(m);
					if (hasAccept) {
						accstate = m;
					}
				}
			}
		} while (!StateCodec.equals(m, currentState));

		if (progress) {
			if (missing(hasActions) && terminalComponent(hstk, trace)) {
				outStatistics(sccId, numberExploredTransitions);
				printCycle(trace, hasActions, currentState);
				return true;
			} // else
				// output.outln("}");
		} else {
			if (hasAccept) {
				if (!strongFairFlag) {
					if (nontrivial(trace)) {
						outStatistics(sccId, numberExploredTransitions);
						printCounterExample(trace, accstate);
						return true;
					}
				} else if (terminalComponent(hstk, trace)) {
					outStatistics(sccId, numberExploredTransitions);
					printCounterExample(null, accstate);
					return true;
				}
			}
		}
		// set dfn to large value
		for (Enumeration<byte[]> e = trace.elements(); e.hasMoreElements();) {
			byte[] i = e.nextElement();
			MyHashProgEntry hse = hstk.get(i);
			hse.dfn = Integer.MAX_VALUE;
		}
		// mach.enablePartialOrder();
		return false;
	}

	private boolean missing(BitSet actions) {
		int alphalen = mach.getAlphabet().length;
		if (ProgressTest.noTests()) { // check for all actions
			for (int i = 1; i < alphalen; ++i) {
				if (!actions.get(i))
					return true;
			}
		} else { // check for each progress test
			tnames = null;
			Enumeration<ProgressTest> e = ProgressTest.tests.elements();
			while (e.hasMoreElements()) {
				ProgressTest p = (ProgressTest) e.nextElement();
				if (p.cset == null) {
					if (contains_none_of(alphalen, actions, p.pset)) {
						if (tnames == null)
							tnames = p.name;
						else
							tnames = tnames + " " + p.name;
					}
				} else {
					if (!contains_none_of(alphalen, actions, p.pset)
							&& contains_none_of(alphalen, actions, p.cset)) {
						if (tnames == null)
							tnames = p.name;
						else
							tnames = tnames + " " + p.name;
					}
				}
			}
			if (tnames != null)
				return true;
		}
		return false;
	}

	private boolean contains_none_of(int length, BitSet actions, BitSet target) {
		for (int i = 1; i < length; ++i)
			if (actions.get(i) && target.get(i))
				return false;
		return true;
	}

	private boolean terminalComponent(MyHashProg hstk, Vector<byte[]> component) {
		BitSet tc = new BitSet(10001);
		for (Enumeration<byte[]> e = component.elements(); e.hasMoreElements();) {
			byte[] i = ((byte[]) e.nextElement());
			MyHashProgEntry hse = hstk.get(i);
			tc.set(hse.dfn); // use depth first search number
		}

		for (Enumeration<byte[]> e = component.elements(); e.hasMoreElements();) {
			byte[] i = ((byte[]) e.nextElement());
			MyList tr = mach.getTransitions(i);
			while (!tr.empty()) {
				if (tr.getTo() == null) {
					hasERROR = true;
					return false;
				}
				MyHashProgEntry hse = hstk.get(tr.getTo());
				if (hse == null)
					return false;
				if (hse.dfn == 0)
					return false;
				if (hse.dfn == Integer.MAX_VALUE)
					return false;
				if (!tc.get(hse.dfn))
					return false;
				tr.next();
			}
		}
		return true;
	}

	private boolean inComponent(Vector<byte[]> component, byte[] state) {
		for (Enumeration<byte[]> e = component.elements(); e.hasMoreElements();) {
			byte[] i = e.nextElement();
			if (StateCodec.equals(i, state))
				return true;
		}
		return false;
	}

	private boolean nontrivial(Vector<byte[]> component) {
		if (component.size() > 1)
			return true;
		byte[] i = component.elementAt(0);
		MyList transitions = mach.getTransitions(i);
		while (!transitions.empty()) {
			int act = transitions.getAction();
			if ((act != accept || accept < 0)
					&& StateCodec.equals(i, transitions.getTo()))
				return true; // non accept labelled self transition
			transitions.next();
		}
		return false;
	}

	private void printSet(BitSet actions, boolean missing) {
		Vector<String> v = new Vector<>();
		String[] alphabet = mach.getAlphabet();
		for (int i = 1; i < alphabet.length; ++i) {
			if ((missing && !actions.get(i)) || (!missing && actions.get(i))) {
				v.addElement(alphabet[i]);
			}
		}
		output.outln("\t" + (new Alphabet(v)).toString());
	}

	Vector<String> errorTrace;
	Vector<String> cycleTrace;

	public Vector<String> getErrorTrace() {
		if (errorTrace == null)
			return null;
		if (cycleTrace != null) {
			errorTrace.addAll(cycleTrace);
			errorTrace.addAll(cycleTrace); // add another cycle for replay
		}
		return errorTrace;
	}

	private void printCycle(Stack<byte[]> trace, BitSet actions, byte[] root) {
		++violation;
		if (violation > Maxviolation)
			return;
		errorTrace = getRootTrace(root);
		if (errorTrace == null)
			return;
		cycleTrace = getCycleTrace(null, root);
		if (ProgressTest.noTests()) {
			output.outln("Progress violation for actions: ");
			printSet(actions, true);
		} else {
			output.outln("Progress violation: " + tnames);
		}
		output.outln("Trace to terminal set of states:");
		printTrace(errorTrace);
		output.outln("Cycle in terminal set:");
		printTrace(cycleTrace);
		output.outln("Actions in terminal set:");
		printSet(actions, false);
	}

	private void printCounterExample(Stack trace, byte[] root) {
		++violation;
		if (violation > Maxviolation)
			return;
		errorTrace = getRootTrace(root);
		if (errorTrace == null)
			return;
		cycleTrace = getCycleTrace(trace, root);
		output.outln("Violation of LTL property: "
				+ mach.getAlphabet()[accept]);
		output.outln("Trace to terminal set of states:");
		// printTrace(errorTrace);
		tracer.print(output, errorTrace, true);
		output.outln("Cycle in terminal set:");
		// printTrace(cycleTrace);
		tracer.print(output, cycleTrace, false);
	}

	Vector<String> getRootTrace(byte[] root) {
		output.outln("Finding trace to cycle...");
		Vector<String> trace = mach.getTraceToState(mach.start(), root);
		if (trace == null)
			hasERROR = true;
		return trace;
	}

	Vector<String> getCycleTrace(Vector<byte[]> component, byte[] root) {
		output.outln("Finding trace in cycle...");
		// if (component!=null) output.outln("Component size
		// "+component.size());
		Vector<String> trace = null;
		MyList transitions = mach.getTransitions(root);
		byte[] cycle = null;
		int act = 0;
		while (!transitions.empty()) {
			act = transitions.getAction();
			// output.outln("finding next "+(mach.getAlphabet())[act]);
			if (act == accept && accept > 0 || stateLabel(act)) {
				transitions.next();
			} else {
				cycle = transitions.getTo();
				if (component == null)
					break;
				if (inComponent(component, cycle))
					break;
				transitions.next();
			}
		}
		if (cycle != null) {
			trace = mach.getTraceToState(cycle, root);
			trace.add(0, mach.getAlphabet()[act]);
		}
		return trace;
	}

	private void printTrace(Vector<String> trace) {
		if (trace == null)
			return;
		Enumeration<String> e = trace.elements();
		while (e.hasMoreElements())
			output.outln("\t" + (String) e.nextElement());
	}

	private boolean stateLabel(int act) {
		String s = mach.getAlphabet()[act];
		return s.charAt(0) == '_';
	}

	private void outStatistics(int states, int transitions) {
		Runtime r = Runtime.getRuntime();
		output.outln("-- States: " + states + " Transitions: " + transitions
				+ " Memory used: " + (r.totalMemory() - r.freeMemory()) / 1000
				+ "K");
	}

	private boolean isAccept(String label) {
		if (label.charAt(0) == '@')
			return true;
		int begin = 0;
		int end = label.indexOf('.');
		while (end > 0) {
			if (label.substring(begin, end).charAt(0) == '@')
				return true;
			begin = end + 1;
			end = label.indexOf('.', end + 1);
		}
		return label.substring(begin).charAt(0) == '@';
	}

	private int acceptLabel(String[] alphabet) {
		
		for (int i = 1; i < alphabet.length; i++) {
			if (isAccept(alphabet[i])) {
				return i;
			}
		}
		return -1;
	}

}