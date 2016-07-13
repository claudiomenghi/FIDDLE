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
import ltsa.lts.ltl.PreconditionDefinition;
import ltsa.lts.ltl.PredicateDefinition;
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
	 * returns the corresponding LTS
	 * 
	 * @param output
	 * @param name
	 * @param refname
	 * @param pvalues
	 * @return
	 */
	public CompactState compilePrecondition(LTSOutput output, Symbol name,
			String refname, Vector pvalues) {
		Preconditions.checkNotNull(name,
				"The name of the precondition to be checked cannot be null");
		Preconditions.checkArgument(this.preconditions.containsKey(name),
				"The name " + name
						+ " is not associated with any valid precondition");

		PreconditionDefinition p = preconditions.get(name.toString());

		if (pvalues != null) {
			if (pvalues.size() != p.getParams().size())
				Diagnostics.fatal("Actual parameters do not match formals: "
						+ name, name);
			Hashtable actualParams = new Hashtable();
			for (int i = 0; i < pvalues.size(); ++i)
				actualParams.put(p.getParams().elementAt(i),
						pvalues.elementAt(i));
			p.getFac().setFormula(
					p.getLTLFormula().expand(p.getFac(), new Hashtable(),
							actualParams));
		} else {
			p.getFac().setFormula(
					p.getLTLFormula().expand(p.getFac(), new Hashtable(),
							p.getInitialParams()));
		}
		CompositeState cs = compile(output, name.toString());

		cs.composition.setName(refname);
		return cs.composition;
	}


	public CompositeState compile( LTSOutput output,
			String asserted) {
		Preconditions.checkArgument(preconditions.containsKey(asserted), "The precondition "+asserted+" is not contained into the set of the preconditions");
		PreconditionDefinition p = preconditions.get(asserted);
	
		if (p.isCached()) {
			return p.getCached();
		}
		output.outln("Formula !" + p.getName().toString() + " = "
				+ p.getFac().getFormula());
		Vector<String> alpha = p.getAlphaExtension() != null ? p.getAlphaExtension()
				.getActions(null) : null;
		if (alpha == null){
			alpha = new Vector<>();
		}
		if (addAsterisk){
			alpha.add("*");
		}
		GeneralizedBuchiAutomata gba = new GeneralizedBuchiAutomata(p.getName()
				.toString(),p.getFac(), alpha);
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
		Vector procs = gba.getLabelFactory().getPropProcs();
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
