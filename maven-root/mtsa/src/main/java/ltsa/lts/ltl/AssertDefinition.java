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

import ltsa.lts.Diagnostics;
import ltsa.lts.ltscomposition.CompactState;
import ltsa.lts.ltscomposition.CompositeState;
import ltsa.lts.operations.minimization.Minimiser;
import ltsa.lts.parser.LTSOutput;
import ltsa.lts.parser.LabelSet;
import ltsa.lts.parser.Symbol;
import ltsa.dispatcher.TransitionSystemDispatcher;

public class AssertDefinition extends FormulaDefinition {
	public static final String NOT_DEF = "NOT";


	static Hashtable<String, AssertDefinition> definitions;
	static Hashtable<String, AssertDefinition> constraints;
	public static boolean addAsterisk = true;
	
	private boolean isConstraint;
	boolean isProperty;


	private AssertDefinition(Symbol n, FormulaSyntax f, LabelSet ls,
			Hashtable ip, Vector p, boolean isConstraint, boolean isProperty) {
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
			Hashtable ip, Vector p, boolean isConstraint, boolean isProperty) {
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
		System.out.println(definitions);
		Enumeration e = definitions.keys();
		List<String> defs = new ArrayList<>();
		while (e.hasMoreElements()) {
			String elem = (String) e.nextElement();
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

	private static void compileAll(Hashtable definitions, LTSOutput output) {
		if (definitions == null)
			return;
		Enumeration e = definitions.keys();
		while (e.hasMoreElements()) {
			String name = (String) e.nextElement();
			AssertDefinition p = (AssertDefinition) definitions.get(name);
			p.fac = new FormulaFactory();
			p.fac.setFormula(p.ltlFormula.expand(p.fac, new Hashtable(),
					p.initParams));
		}
	}

	public static CompositeState compile(LTSOutput output, String asserted) {
		return compile(definitions, output, asserted, false);
	}

	public static void compileConstraints(LTSOutput output, Hashtable compiled) {
		if (constraints == null) {
			return;
		}
		Enumeration e = constraints.keys();
		while (e.hasMoreElements()) {
			String name = (String) e.nextElement();
			if (!name.startsWith(AssertDefinition.NOT_DEF)) {
				CompactState cm = compileConstraint(output, name);
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
	public static CompactState compileConstraint(LTSOutput output, Symbol name,
			String refname, Vector pvalues) {
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
			Hashtable actualParams = new Hashtable();
			for (int i = 0; i < pvalues.size(); ++i)
				actualParams.put(p.params.elementAt(i), pvalues.elementAt(i));
			p.fac.setFormula(p.ltlFormula.expand(p.fac, new Hashtable(),
					actualParams));
		} else {
			p.fac.setFormula(p.ltlFormula.expand(p.fac, new Hashtable(),
					p.initParams));
		}
		CompositeState cs = compile(constraints, output, name.toString(), true);
		if (cs == null) {
			return null;
		}
		if (!cs.isProperty && !cs.name.startsWith(AssertDefinition.NOT_DEF)) {
			Diagnostics.fatal("LTL constraint must be safety: " + p.getName(),
					p.getName());
		}
		if (!p.isProperty) {
			cs.composition.unMakeProperty();
		}
		cs.composition.setName(refname);
		return cs.composition;
	}

	public static CompactState compileConstraint(LTSOutput output,
			String constraint) {
		CompositeState cs = compile(constraints, output, constraint, true);
		if (cs == null) {
			return null;
		}
		if (!cs.isProperty) {
			AssertDefinition p = constraints.get(constraint);
			if (!cs.name.startsWith(AssertDefinition.NOT_DEF)) {
				Diagnostics.fatal("LTL constraint must be safety: " + p.getName(),
						p.getName());
			}
		}
		if (!cs.isProperty) {
			cs.composition.unMakeProperty();
		}
		return cs.composition;
	}

	private static CompositeState compile(Hashtable definitions,
			LTSOutput output, String asserted, boolean isconstraint) {
		if (definitions == null || asserted == null) {
			return null;
		}
		AssertDefinition p = (AssertDefinition) definitions.get(asserted);
		if (p == null) {
			return null;
		}
		if (p.cached != null) {
			return p.cached;
		}
		output.outln("Formula !" + p.getName().toString() + " = "
				+ p.fac.getFormula());
		Vector alpha = p.alphaExtension != null ? p.alphaExtension
				.getActions(null) : null;
		if (alpha == null)
			alpha = new Vector();
		if (addAsterisk && !isconstraint)
			alpha.add("*");
		GeneralizedBuchiAutomata gba = new GeneralizedBuchiAutomata(
				p.getName().toString(), p.fac, alpha);
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
		Converter c = new Converter(p.getName().toString(), g1,
				gba.getLabelFactory());
		output.outln("Buchi automata:");
		c.printFSP(new PrintStream(baos));
		output.out(baos.toString());
		Vector procs = gba.getLabelFactory().propProcs;
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

		if (p.isConstraint && !p.isProperty) {
			CompactState constrained = cs.getComposition();
			// DIPI: temporal, hay que ver cuando aplicamos el constrained de
			// MTS.
			if (ltsa.lts.util.MTSUtils.isMTSRepresentation(constrained)) {
				cs.setComposition(TransitionSystemDispatcher
						.makeMTSConstraintModel(constrained, output));
			}
		}

		p.cached = cs;
		return cs;
	}

}
