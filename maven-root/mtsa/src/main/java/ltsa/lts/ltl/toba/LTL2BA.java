package ltsa.lts.ltl.toba;

import gov.nasa.ltl.graph.Degeneralize;
import gov.nasa.ltl.graph.Graph;
import gov.nasa.ltl.graph.SCCReduction;
import gov.nasa.ltl.graph.SFSReduction;
import gov.nasa.ltl.graph.Simplify;
import gov.nasa.ltl.graph.SuperSetReduction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Vector;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.FluentTrace;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.operations.minimization.Minimiser;
import ltsa.lts.output.LTSOutput;

public class LTL2BA {

	LTSOutput output;

	public LTL2BA(LTSOutput output) {
		this.output = output;
	}

	public CompositeState getCompactState(String formulaName, Formula formula, Vector<String> alphabet) {

		FormulaFactory factory = new FormulaFactory();
		factory.setFormula(formula);
		GeneralizedBuchiAutomata gba = new GeneralizedBuchiAutomata(
				formulaName, factory, alphabet);
		gba.translate();
		Graph g = gba.makeGBA();
		output.outln("GBA " + g.getNodeCount() + " states " + g.getEdgeCount()
				+ " transitions");
		g = SuperSetReduction.reduce(g);
		Graph g1 = Degeneralize.degeneralize(g);
		g1 = SCCReduction.reduce(g1);
		g1 = Simplify.simplify(g1);
		g1 = SFSReduction.reduce(g1);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Converter c = new Converter(formulaName, g1, gba.getLabelFactory());
		output.outln("Buchi automata:");
		c.printFSP(new PrintStream(baos));
		output.out(baos.toString());
		Vector<LabelledTransitionSystem> procs = gba.getLabelFactory().propProcs;
		procs.add(c);
		CompositeState cs = new CompositeState(c.getName(), procs);
		cs.setHidden(gba.getLabelFactory().getPrefix());

		PredicateDefinition[] fluents = gba.getLabelFactory().getFluents();
		cs.setFluentTracer(new FluentTrace(fluents));
		cs.compose(output, true);
		cs.getComposition().removeNonDetTau();
		output.outln("After Tau elimination = "
				+ cs.getComposition().getMaxStates() + " state");
		Minimiser e = new Minimiser(cs.getComposition(), output);
		cs.setComposition(e.minimise());
		if (cs.getComposition().isSafetyOnly()) {
			cs.getComposition().makeSafety();
			cs.determinise(output);
			cs.isProperty = true;
		}
		cs.getComposition().removeDetCycles("*");

		return cs;
	}

}
