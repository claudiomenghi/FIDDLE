package ltsa.lts.ltl;

import gov.nasa.ltl.graph.Degeneralize;
import gov.nasa.ltl.graph.Graph;
import gov.nasa.ltl.graph.SCCReduction;
import gov.nasa.ltl.graph.SFSReduction;
import gov.nasa.ltl.graph.Simplify;
import gov.nasa.ltl.graph.SuperSetReduction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import ltsa.dispatcher.TransitionSystemDispatcher;
import ltsa.lts.Diagnostics;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.ltl.toba.Converter;
import ltsa.lts.ltl.toba.GeneralizedBuchiAutomata;
import ltsa.lts.operations.minimization.Minimiser;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.parser.actions.LabelSet;

public class AssertDefinition extends FormulaDefinition {
	public static final String NOT_DEF = "NOT";

	public static Hashtable<String, AssertDefinition> definitions;
	public static Hashtable<String, AssertDefinition> constraints;
	public static boolean addAsterisk = true;

	private boolean isConstraint;
	boolean isProperty;

	private AssertDefinition(Symbol n, FormulaSyntax f, LabelSet ls,
			Hashtable<String, Value> ip, Vector<String> p,
			boolean isConstraint, boolean isProperty) {
		super(n, f, ls, ip, p);
		this.isConstraint = isConstraint;
		this.isProperty = isProperty;
	}

	public static AssertDefinition getDefinition(String definitionName) {
		return definitions == null ? null : definitions.get(definitionName);
	}

	public static AssertDefinition getConstraint(String definitionName) {
		return constraints == null ? null : constraints.get(definitionName);
	}

	public static void put(Symbol n, FormulaSyntax f, LabelSet ls,
			Hashtable<String, Value> ip, Vector<String> p,
			boolean isConstraint, boolean isProperty) {
		if (definitions == null)
			definitions = new Hashtable<>();
		if (constraints == null)
			constraints = new Hashtable<>();
		if (!isConstraint) {
			if (definitions.put(n.toString(), new AssertDefinition(n, f, ls,
					ip, p, false, false)) != null) {
				Diagnostics.fatal("duplicate LTL property definition: " + n, n);
			}
		} else {
			if (constraints.put(n.toString(), new AssertDefinition(n, f, ls,
					ip, p, true, isProperty)) != null) {
				Diagnostics
						.fatal("duplicate LTL constraint/property definition: "
								+ n, n);
			}
		}
	}

	public static void init() {
		definitions = null;
		constraints = null;
	}

	public static String[] names() {
		if (definitions == null) {
			return null;
		}
		Enumeration<String> e = definitions.keys();
		List<String> defs = new ArrayList<>();
		while (e.hasMoreElements()) {
			String elem = e.nextElement();
			if (!elem.startsWith(AssertDefinition.NOT_DEF)) {
				defs.add(elem);
			}
		}
		if (defs.isEmpty()) {
			return null;
		}
		String na[] = defs.toArray(new String[0]);
		return na;
	}

	public static void compileAll(LTSOutput output) {
		compileAll(definitions, output);
		compileAll(constraints, output);
	}

	private static void compileAll(
			Hashtable<String, AssertDefinition> definitions, LTSOutput output) {
		if (definitions == null)
			return;
		Enumeration<String> e = definitions.keys();
		while (e.hasMoreElements()) {
			String name = e.nextElement();
			AssertDefinition p = definitions.get(name);
			p.fac = new FormulaFactory();
			p.fac.negateAndSetFormula(p.ltlFormula.expand(p.fac,
					new Hashtable<>(), p.initParams));
		}
	}

	public static CompositeState compile(LTSOutput output, String asserted) {
		return compile(definitions, output, asserted, false);
	}

	public static void compileConstraints(LTSOutput output,
			Hashtable<String, LabelledTransitionSystem> compiled) {
		if (constraints == null) {
			return;
		}
		Enumeration<String> e = constraints.keys();
		while (e.hasMoreElements()) {
			String name = e.nextElement();
			if (!name.startsWith(AssertDefinition.NOT_DEF)) {
				LabelledTransitionSystem cm = compileConstraint(output, name);
				compiled.put(cm.getName(), cm);
			}
		}
	}

	/**
	 * returns the corresponding LTS
	 * 
	 * @param output
	 * @param name
	 * @param refname
	 * @param pvalues
	 * @return
	 */
	public static LabelledTransitionSystem compileConstraint(LTSOutput output, Symbol name,
			String refname, Vector<Value> pvalues) {
		if (constraints == null) {
			return null;
		}
		AssertDefinition p = constraints.get(name.toString());
		if (p == null) {
			return null;
		}
		p.cached = null;
		p.fac = new FormulaFactory();
		if (pvalues != null) {
			if (pvalues.size() != p.params.size())
				Diagnostics.fatal("Actual parameters do not match formals: "
						+ name, name);
			Hashtable<String, Value> actualParams = new Hashtable<>();
			for (int i = 0; i < pvalues.size(); ++i)
				actualParams.put(p.params.elementAt(i), pvalues.elementAt(i));
			p.fac.negateAndSetFormula(p.ltlFormula.expand(p.fac,
					new Hashtable<>(), actualParams));
		} else {
			p.fac.negateAndSetFormula(p.ltlFormula.expand(p.fac,
					new Hashtable<>(), p.initParams));
		}
		CompositeState cs = compile(constraints, output, name.toString(), true);
		if (cs == null) {
			return null;
		}
		if (!cs.isProperty
				&& !cs.getName().startsWith(AssertDefinition.NOT_DEF)) {
			Diagnostics.fatal(
					"LTL constraint must be safety: " + p.getSymbol(),
					p.getSymbol());
		}
		if (!p.isProperty) {
			cs.getComposition().unMakeProperty();
		}
		cs.getComposition().setName(refname);
		return cs.getComposition();
	}

	public static LabelledTransitionSystem compileConstraint(LTSOutput output,
			String constraint) {
		CompositeState cs = compile(constraints, output, constraint, true);
		if (cs == null) {
			return null;
		}
		if (!cs.isProperty) {
			AssertDefinition p = constraints.get(constraint);
			if (!cs.getName().startsWith(AssertDefinition.NOT_DEF)) {
				Diagnostics.fatal(
						"LTL constraint must be safety: " + p.getSymbol(),
						p.getSymbol());
			}
		}
		if (!cs.isProperty) {
			cs.getComposition().unMakeProperty();
		}
		return cs.getComposition();
	}

	private static CompositeState compile(
			Hashtable<String, AssertDefinition> definitions, LTSOutput output,
			String asserted, boolean isconstraint) {
		
		if (definitions == null || asserted == null) {
			return null;
		}
		AssertDefinition assertDefinition = definitions.get(asserted);
		if (assertDefinition == null) {
			return null;
		}
		if (assertDefinition.cached != null) {
			return assertDefinition.cached;
		}
		output.outln("Formula !" + assertDefinition.getSymbol().toString()
				+ " = " + assertDefinition.fac.getFormula());
		Vector<String> alpha = assertDefinition.alphaExtension != null ? assertDefinition.alphaExtension
				.getActions(null) : null;
		if (alpha == null)
			alpha = new Vector<>();
		if (addAsterisk && !isconstraint)
			alpha.add("*");

		GeneralizedBuchiAutomata gba = new GeneralizedBuchiAutomata(
				assertDefinition.getSymbol().toString(), assertDefinition.fac,
				alpha);
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
		Converter c = new Converter(assertDefinition.getSymbol().toString(),
				g1, gba.getLabelFactory());
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

		if (assertDefinition.isConstraint && !assertDefinition.isProperty) {
			LabelledTransitionSystem constrained = cs.getComposition();
			// DIPI: temporal, hay que ver cuando aplicamos el constrained de
			// MTS.
			if (ltsa.lts.util.MTSUtils.isMTSRepresentation(constrained)) {
				cs.setComposition(TransitionSystemDispatcher
						.makeMTSConstraintModel(constrained, output));
			}
		}

		assertDefinition.cached = cs;
		return cs;
	}

}
