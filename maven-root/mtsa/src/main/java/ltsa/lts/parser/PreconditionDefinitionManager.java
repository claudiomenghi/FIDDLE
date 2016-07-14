package ltsa.lts.parser;

import gov.nasa.ltl.graph.Degeneralize;
import gov.nasa.ltl.graph.Graph;
import gov.nasa.ltl.graph.SCCReduction;
import gov.nasa.ltl.graph.SFSReduction;
import gov.nasa.ltl.graph.Simplify;
import gov.nasa.ltl.graph.SuperSetReduction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.Converter;
import ltsa.lts.ltl.FluentTrace;
import ltsa.lts.ltl.FormulaFactory;
import ltsa.lts.ltl.FormulaSyntax;
import ltsa.lts.ltl.GeneralizedBuchiAutomata;
import ltsa.lts.ltl.LTLAdditionalSymbolTable;
import ltsa.lts.ltl.PreconditionDefinition;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.visitors.FiniteFormulaGeneratorVisitor;
import ltsa.lts.ltscomposition.CompactState;
import ltsa.lts.ltscomposition.CompositeState;
import ltsa.lts.operations.minimization.Minimiser;

import com.google.common.base.Preconditions;

public class PreconditionDefinitionManager {

	private Map<String, PreconditionDefinition> preconditions;

	public static boolean addAsterisk = true;

	public PreconditionDefinitionManager() {
		preconditions = new HashMap<>();
	}

	public void put(Symbol n, FormulaSyntax f, LabelSet ls, Hashtable ip,
			Vector p) {
		if (preconditions == null) {
			preconditions = new HashMap<>();
		}
		if (preconditions.put(n.toString(), new PreconditionDefinition(n, f,
				ls, ip, p)) != null) {
			Diagnostics.fatal("duplicate preconditions definition: " + n, n);
		}

	}

	
	/**
	 * returns a state machine describing the violating behaviors
	 * 
	 * @param output
	 *            the output used to print messages
	 * @param asserted
	 *            the string representing the precondition to be considered
	 * @return a state machine describing the violating behaviors
	 * @throws IllegalArgumentException
	 *             if the string representing the precondition is not a valid
	 *             string
	 */
	public CompositeState compile(LTSOutput output, String asserted) {
		Preconditions
				.checkArgument(
						preconditions.containsKey(asserted),
						"The precondition "
								+ asserted
								+ " is not contained into the set of the preconditions");
		PreconditionDefinition p = preconditions.get(asserted);

		if (p.isCached()) {
			return p.getCached();
		}
		output.outln("Formula !" + p.getName().toString() + " = "
				+ p.getFac().getFormula());
		Vector<String> alpha = p.getAlphaExtension() != null ? p
				.getAlphaExtension().getActions(null) : null;
		if (alpha == null) {
			alpha = new Vector<>();
		}
		if (addAsterisk) {
			alpha.add("*");
		}

		Formula infiniteFormula = p.getFac().getFormula();

		FormulaFactory finiteFormulaFactory = new FormulaFactory(p.getFac()
				.getActionPredicates());

		// translating the formula into its finite path version
		Formula finiteFormula = infiniteFormula
				.accept(new FiniteFormulaGeneratorVisitor(
						LTLAdditionalSymbolTable.getPreSymbol(infiniteFormula,
								"black"), finiteFormulaFactory));
		finiteFormulaFactory.setFormula(finiteFormula);
		output.outln("Infinite LTL precondition:  " + infiniteFormula
				+ "transformed into: ");
		output.outln("Finite LTL precondition:  " + finiteFormula);

		GeneralizedBuchiAutomata gba = new GeneralizedBuchiAutomata(p.getName()
				.toString(), finiteFormulaFactory, alpha);
		gba.translate();
		Graph gbaGraph = gba.makeGBA();
		output.outln("GBA " + gbaGraph.getNodeCount() + " states "
				+ gbaGraph.getEdgeCount() + " transitions");
		gbaGraph = SuperSetReduction.reduce(gbaGraph);
		Graph degeneralizedGraph = Degeneralize.degeneralize(gbaGraph);
		degeneralizedGraph = SCCReduction.reduce(degeneralizedGraph);
		degeneralizedGraph = Simplify.simplify(degeneralizedGraph);
		degeneralizedGraph = SFSReduction.reduce(degeneralizedGraph);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Converter c = new Converter(p.getName().toString(), degeneralizedGraph,
				gba.getLabelFactory());
		output.outln("Buchi automata:");
		c.printFSP(new PrintStream(baos));
		output.out(baos.toString());

		// computing the composition between the system and the fuents
		Vector<CompactState> procs = gba.getLabelFactory().getPropProcs();
		procs.add(c);
		CompositeState cs = new CompositeState(c.getName(), procs);
		cs.hidden = gba.getLabelFactory().getPrefix();

		PredicateDefinition[] fluents = gba.getLabelFactory().getFluents();
		cs.setFluentTracer(new FluentTrace(fluents));
		cs.compose(output, true);
		cs.composition.removeNonDetTau();

		output.outln("After Tau elimination = " + cs.composition.maxStates
				+ " state");
		Minimiser e = new Minimiser(cs.composition, output);
		cs.composition = e.minimise();
		if (cs.composition.isSafetyOnly()) {
			cs.composition.makeSafety();
			cs.determinise(output);
			cs.isProperty = true;
		}
		cs.composition.removeDetCycles("*");

		p.setCached(cs);
		return cs;
	}

	/**
	 * returns the name of the preconditions
	 * 
	 * @return the name of the preconditions
	 */
	public Set<String> names() {
		Set<String> defs = new HashSet<>();

		for (String elem : preconditions.keySet()) {
			if (!elem.startsWith(AssertDefinition.NOT_DEF)) {
				defs.add(elem);
			}
		}

		return defs;
	}
}
