package ltsa.lts.ltl.toba;

import gov.nasa.ltl.graph.Edge;
import gov.nasa.ltl.graph.Graph;

import java.io.PrintStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;

import ltsa.lts.Diagnostics;
import ltsa.lts.EventStateUtils;
import ltsa.lts.automata.LabelFactory;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;

public class Converter extends LabelledTransitionSystem {

	private BitSet accepting;
	Graph g;
	/**
	 * one if first state is accepting
	 */
	int iacc = 0;

	/**
	 * in this case first state is duplicated with state 1 accepting this alLows
	 * for initialization
	 * 
	 * @param n
	 * @param g
	 * @param lf
	 */
	public Converter(String n, Graph g, LabelFactory lf) {
		super(n);
		this.g = g;
		this.accepting = getAcceptance();
		// disable code for initial accepting state
		// iacc = accepting.get(0) ? 1 : 0;
		this.setAlphabet(lf.makeAlphabet());
		this.makeStates(lf);
	}

	private void makeStates(LabelFactory lf) {
		// add extra node for completion
		this.setStates(new LTSTransitionList[g.getNodeCount() + iacc + 1]);
		HashMap<String, BitSet> trl = lf.getTransLabels();
		addTrueNode(this.getMaxStates() - 1, trl);
		Iterator<gov.nasa.ltl.graph.Node> ii = g.getNodes().iterator();
		while (ii.hasNext()) {
			addNode((gov.nasa.ltl.graph.Node) ii.next(), trl);
		}
		if (iacc == 1) {
			this.getStates()[0] = EventStateUtils.union(this.getStates()[0],
					this.getStates()[1]);
		}
		addAccepting();
		reachable();
	}

	private void addAccepting() {
		for (int id = 0; id < this.getMaxStates() - 1; id++) {
			if (accepting.get(id)) {
				this.getStates()[id + iacc] = EventStateUtils.add(this
						.getStates()[id + iacc], new LTSTransitionList(this
						.getAlphabet().length - 1, id + iacc));
			}
		}
	}

	void addNode(gov.nasa.ltl.graph.Node n, HashMap<String, BitSet> trl) {
		int id = n.getId();
		BitSet all = new BitSet(this.getAlphabet().length - 2);
		Iterator<Edge> ii = n.getOutgoingEdges().iterator();
		while (ii.hasNext()) {
			addEdge((Edge) ii.next(), id, trl, all);
		}
		complete(id, all);
	}

	void addTrueNode(int id, HashMap<String, BitSet> trl) {
		BitSet tr = trl.get("true");
		for (int i = 0; i < tr.size(); ++i) {
			if (tr.get(i)) {
				this.getStates()[id] = EventStateUtils.add(
						this.getStates()[id], new LTSTransitionList(i + 1, id));
			}
		}
	}

	private void complete(int id, BitSet all) {
		for (int i = 0; i < this.getAlphabet().length - 2; ++i) {
			if (!all.get(i)) {
				this.getStates()[id + iacc] = EventStateUtils.add(this
						.getStates()[id + iacc], new LTSTransitionList(i + 1,
						this.getMaxStates() - 1));
			}
		}
	}

	void addEdge(Edge e, int id, HashMap<String, BitSet> trl, BitSet all) {
		String s;
		if (e.getGuard().equals("-"))
			s = "true";
		else
			s = e.getGuard();
		BitSet tr = trl.get(s);
		all.or(tr);
		for (int i = 0; i < tr.size(); ++i) {
			if (tr.get(i)) {
				this.getStates()[id + iacc] = EventStateUtils.add(this
						.getStates()[id + iacc], new LTSTransitionList(i + 1, e
						.getNext().getId() + iacc));
			}
		}
	}

	public void printFSP(PrintStream printstream) {
		if (g.getInit() != null) {
			printstream.print(this.getName() + " = S" + g.getInit().getId());
		} else {
			printstream.print("Empty");
		}
		g.getNodes().stream().forEach(n -> printNodes(n, printstream));

		printstream.println(".");

		if (printstream != System.out) {
			printstream.close();
		}
	}

	protected void printNodes(gov.nasa.ltl.graph.Node n, PrintStream printstream) {
		printstream.println(",");
		printNode(n, printstream);

	}

	protected BitSet getAcceptance() {
		BitSet acc = new BitSet();
		int i = g.getIntAttribute("nsets");
		if (i > 0)
			Diagnostics.fatal("More than one acceptance set");
		for (Iterator<gov.nasa.ltl.graph.Node> iterator1 = g.getNodes()
				.iterator(); iterator1.hasNext();) {
			gov.nasa.ltl.graph.Node node1 = iterator1.next();
			if (node1.getBooleanAttribute("accepting"))
				acc.set(node1.getId());
		}
		return acc;
	}

	void printNode(gov.nasa.ltl.graph.Node n, PrintStream printstream) {
		String s = accepting.get(n.getId()) ? "@" : "";
		printstream.print("S" + n.getId() + s + " =(");
		for (Iterator<Edge> iterator = n.getOutgoingEdges().iterator(); iterator
				.hasNext();) {
			printEdge(iterator.next(), printstream);
			if (iterator.hasNext())
				printstream.print(" |");
		}

		printstream.print(")");
	}

	void printEdge(Edge e, PrintStream printstream) {
		String s = e.getGuard().equals("-") ? "true" : e.getGuard();
		printstream.print(s + " -> S" + e.getNext().getId());
	}
}
