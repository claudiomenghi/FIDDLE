package ltsa.lts.ltl.toba;

import gov.nasa.ltl.graph.Edge;
import gov.nasa.ltl.graph.Graph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.LabelFactory;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.output.LTSOutput;

import com.google.common.base.Preconditions;

public class GeneralizedBuchiAutomata {

	private List<Node> nodes;
	private Formula formula;
	private FormulaFactory formulaFactory;
	private int maxId = -1;
	private Node[] equivClasses;
	private State[] states;
	private int naccept;
	private LabelFactory labelFac;

	/**
	 * 
	 * @param name the name of the automata
	 * @param formulaFactory the factory used to create the formula
	 * @param alphaExt
	 */
	public GeneralizedBuchiAutomata(String name, FormulaFactory formulaFactory,
			Vector<String> alphaExt) {
		Preconditions.checkNotNull(name, "The name cannot be null");
		Preconditions.checkNotNull(formulaFactory, "The formula factory cannot be null");
		Preconditions.checkNotNull(formulaFactory.getFormula(),
				"The formula contained in the formula factory cannot be null");

		this.formulaFactory = formulaFactory;
		this.formula = formulaFactory.getFormula();
		this.nodes = new ArrayList<>();
		this.labelFac = new LabelFactory(name, formulaFactory, alphaExt);
	}

	public void translate() {
		Node.setAut(this);
		Node.setFactory(formulaFactory);
		Transition.setLabelFactory(labelFac);
		this.naccept = this.formulaFactory.processUntils(this.formula, new ArrayList<>());
		Node first = new Node(this.formula);
		this.nodes = first.expand(this.nodes);
		this.states = makeStates();
	}

	public LabelFactory getLabelFactory() {
		return this.labelFac;
	}

	public void printNodes(LTSOutput out) {
		// print states
		for (int ii = 0; ii < states.length; ii++)
			if (states[ii] != null && ii == states[ii].getId()) {
				states[ii].print(out, naccept);
			}
	}

	public int indexEquivalence(Node n) {
		int i;
		for (i = 0; i < maxId; i++) {
			if (equivClasses[i] == null)
				break;
			if (equivClasses[i].next.equals(n.next))
				return equivClasses[i].id;
		}
		if (i == maxId)
			Diagnostics
					.fatal("size of equivalence classes array was incorrect");
		equivClasses[i] = n;
		return equivClasses[i].id;
	}

	public State[] makeStates() {
		State[] astate = new State[maxId];
		equivClasses = new Node[maxId];
		Iterator<Node> i = nodes.iterator();
		while (i.hasNext()) {
			Node node =  i.next();
			node.equivId = indexEquivalence(node);
			node.makeTransitions(astate);
		}
		return astate;
	}

	int newId() {
		return ++maxId;
	}

	public Graph makeGBA() {
		Graph graph = new Graph();
		graph.setStringAttribute("type", "gba");
		graph.setStringAttribute("ac", "edges");
		if (states == null)
			return graph;
		int i = maxId;
		gov.nasa.ltl.graph.Node anode[] = new gov.nasa.ltl.graph.Node[i];
		for (int j = 0; j < i; j++)
			if (states[j] != null && j == states[j].getId()) {
				anode[j] = new gov.nasa.ltl.graph.Node(graph);
				anode[j].setStringAttribute("label", "S" + states[j].getId());
			}

		for (int k = 0; k < i; k++)
			if (states[k] != null && k == states[k].getId())
				states[k].Gmake(anode, anode[k], naccept);

		if (naccept == 0)
			graph.setIntAttribute("nsets", 1);
		else
			graph.setIntAttribute("nsets", naccept);
		return graph;
	}

}

class State implements Comparable<State> {
	private List<Transition> transitions;
	private int stateId;

	State(List<Transition> t, int id) {
		transitions = t;
		stateId = id;
	}

	State() {
		this(new LinkedList<>(), -1);
	}

	State(int id) {
		this(new LinkedList<>(), id);
	}

	void setId(int id) {
		stateId = id;
	}

	int getId() {
		return stateId;
	}

	@Override
	public int compareTo(State obj) {
		return this != obj ? 1 : 0;
	}

	public void add(Transition t) {
		transitions.add(t);
	}

	void print(LTSOutput out, int nacc) {
		out.outln("STATE " + stateId);
		Iterator<Transition> i = transitions.iterator();
		while (i.hasNext()){
			(i.next()).print(out, nacc);
		}
	}

	void Gmake(gov.nasa.ltl.graph.Node anode[], gov.nasa.ltl.graph.Node node,
			int nacc) {
		ListIterator<Transition> listiterator = transitions.listIterator(0);
		Transition transition;
		for (; listiterator.hasNext(); transition.Gmake(anode, node, nacc))
			transition = listiterator.next();

	}

}

class Transition {
	SortedSet propositions;
	int pointsTo;
	BitSet accepting;
	boolean safe_acc;

	static LabelFactory lf;

	static void setLabelFactory(LabelFactory f) {
		lf = f;
	}

	Transition(SortedSet p, int i, BitSet acc, boolean sa) {
		propositions = p;
		pointsTo = i;
		accepting = new BitSet();
		accepting.or(acc);
		safe_acc = sa;
	}

	int goesTo() {
		return pointsTo;
	}

	BitSet computeAccepting(int nacc) {
		BitSet b = new BitSet(nacc);
		for (int i = 0; i < nacc; ++i)
			if (!accepting.get(i))
				b.set(i);
		return b;
	}

	void print(LTSOutput out, int nacc) {
		if (propositions.isEmpty())
			out.out("LABEL True");
		else
			Node.printFormulaSet(out, "LABEL", propositions);
		out.out(" T0 " + goesTo());
		if (nacc > 0)
			out.outln(" Acc " + computeAccepting(nacc));
		else if (safe_acc)
			out.outln(" Acc {0}");
		else
			out.outln("");
	}

	void Gmake(gov.nasa.ltl.graph.Node anode[], gov.nasa.ltl.graph.Node node,
			int nacc) {
		String s = "-";
		String s1 = "-";
		if (!propositions.isEmpty()) {
			s = lf.makeLabel(propositions);
		}
		Edge edge = new Edge(node, anode[pointsTo], s, s1);
		if (nacc == 0) {
			// if(safe_acc)
			edge.setBooleanAttribute("acc0", true);
		} else {
			for (int i = 0; i < nacc; i++){
				if (!accepting.get(i)){
					edge.setBooleanAttribute("acc" + i, true);
				}
			}

		}
	}
}
