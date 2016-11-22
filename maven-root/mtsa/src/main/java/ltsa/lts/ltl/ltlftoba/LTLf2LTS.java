package ltsa.lts.ltl.ltlftoba;

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
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.FluentTrace;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.True;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.ltl.toba.Converter;
import ltsa.lts.ltl.toba.GeneralizedBuchiAutomata;
import ltsa.lts.ltl.visitors.FiniteFormulaGeneratorVisitor;
import ltsa.lts.operations.minimization.Minimiser;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.Symbol;

/**
 * Transforms the LTL formula into the corresponding FSA
 *
 */
public class LTLf2LTS {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());
	
	public static final Symbol endSymbol = new Symbol("end", Symbol.UPPERIDENT);

	public static final Symbol endFluent = new Symbol("F_"
			+ LTLf2LTS.endSymbol.toString(), Symbol.UPPERIDENT);

	public static final Symbol initSymbol = new Symbol("init",
			Symbol.UPPERIDENT);

	public static final Symbol initFluent = new Symbol("F_"
			+ LTLf2LTS.initSymbol.toString(), Symbol.UPPERIDENT);

	private Map<String, Vector<String>> fluentTerminatingActions = new HashMap<>();

	public CompositeState toCompositeState(Formula formula, LTSOutput output,
			Set<String> alphabet, String name) {

		output.outln("Running the LTLf2BA");
		logger.debug("Running the LTLf2BA");
		// updates the fluents considering the additional endSymbol
		// it is necessary to conform the fluents with the new end symbol
		this.updateFluents();

		FormulaFactory formulaFactory = new FormulaFactory();
		formulaFactory.setFormula(formula);

		PredicateDefinition.makePredicate(output, endFluent,
				LTLf2LTS.endSymbol, alphabet);

		Formula end = formulaFactory.make(endFluent);

		FiniteFormulaGeneratorVisitor visitor = new FiniteFormulaGeneratorVisitor(
				formulaFactory, end);

		Formula formulaUpdatedByf = formula.accept(visitor);

		Formula epsilon = this.getEpsilon(output, formula, formulaFactory, end);

		Formula finalFormula = formulaFactory.makeAnd(formulaUpdatedByf,
				epsilon);

		output.outln("Finite formula: " + finalFormula);
		logger.debug("Finite formula: " + finalFormula);
		formulaFactory.setFormula(finalFormula);

		CompositeState s = this.computeCompositeState(output, formulaFactory,
				finalFormula, new Vector<>(alphabet));
		s.setName(name);
		
		this.updateRemoveEndFluents();
		
		return s;
	}

	public CompositeState toProperty(Formula formula, LTSOutput output,
			Set<String> alphabet, String name) {

	
		this.init(formula, output, alphabet, name);

		FormulaFactory formulaFactory = new FormulaFactory();
		formulaFactory.setFormula(formula);

		Formula end = formulaFactory.make(LTLf2LTS.endFluent);
		PredicateDefinition.makePredicate(output, LTLf2LTS.endFluent,
				LTLf2LTS.endSymbol, alphabet);

		Formula init = formulaFactory.make(LTLf2LTS.initFluent);
		PredicateDefinition.makePredicate(output, LTLf2LTS.initFluent,
				LTLf2LTS.initSymbol, alphabet);

		formulaFactory.setFormula(formula);
		PredicateDefinition.makePredicate(output, LTLf2LTS.endFluent,
				LTLf2LTS.endSymbol, alphabet);

		Formula newPostConditionFormula = formula;

		output.outln("FORMULA: " + newPostConditionFormula + " considered");

		formulaFactory.setFormula(newPostConditionFormula);

		FiniteFormulaGeneratorVisitor visitor = new FiniteFormulaGeneratorVisitor(
				formulaFactory, end);

		Formula formulaUpdatedByf = newPostConditionFormula.accept(visitor);

		Formula epsilon = this.getEpsilon(output, formula, formulaFactory, end);

		Formula finalFormula = formulaFactory.makeAnd(formulaUpdatedByf,
				epsilon);

		output.outln("Finite formula: " + finalFormula);
		formulaFactory.setFormula(finalFormula);
		System.out.println("Finite formula: " + finalFormula);

		return this.toAutomata(formulaFactory, finalFormula, output, alphabet,
				name);
	}

	public CompositeState toPropertyWithInit(Formula formula, LTSOutput output,
			Set<String> alphabet, String name) {

		this.init(formula, output, alphabet, name);

		FormulaFactory formulaFactory = new FormulaFactory();
		formulaFactory.setFormula(formula);

		Formula end = formulaFactory.make(LTLf2LTS.endFluent);
		PredicateDefinition.makePredicate(output, LTLf2LTS.endFluent,
				LTLf2LTS.endSymbol, alphabet);

		Formula init = formulaFactory.make(LTLf2LTS.initFluent);
		PredicateDefinition.makePredicate(output, LTLf2LTS.initFluent,
				LTLf2LTS.initSymbol, alphabet);
		// init
		Formula newPostConditionFormula = formulaFactory.makeNot(formulaFactory
				.makeImplies(init, formulaFactory.makeNext(formula)));

	
		output.outln("FORMULA: " + newPostConditionFormula + " considered");

		formulaFactory.setFormula(newPostConditionFormula);

		FiniteFormulaGeneratorVisitor visitor = new FiniteFormulaGeneratorVisitor(
				formulaFactory, end);

		Formula formulaUpdatedByf = newPostConditionFormula.accept(visitor);

		Formula epsilon = this.getEpsilon(output, formula, formulaFactory, end);

		Formula finalFormula = formulaFactory.makeAnd(formulaUpdatedByf,
				epsilon);

		System.out.println("FORMULA CONVERTED: "+finalFormula);
		return this.toAutomata(formulaFactory, finalFormula, output, alphabet,
				name);
	}

	private CompositeState toAutomata(FormulaFactory formulaFactory,
			Formula formula, LTSOutput output, Set<String> alphabet,
			String name) {

		Vector<String> alpha = new Vector<>(alphabet);
		alpha.add("*");
		GeneralizedBuchiAutomata gba = new GeneralizedBuchiAutomata(name,
				formulaFactory, alpha);
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
		Converter c = new Converter(name, g1, gba.getLabelFactory());
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

	private void init(Formula formula, LTSOutput output, Set<String> alphabet,
			String name) {
		output.outln("Running the LTLf2BA");
		// updates the fluents considering the additional endSymbol
		// it is necessary to conform the fluents with the new end symbol
		this.updateFluents();

	}

	public LabelledTransitionSystem toLTS(Formula formula, LTSOutput output,
			Set<String> alphabet, String name) {

		output.outln("Running the LTLf2BA");
		// updates the fluents considering the additional endSymbol
		// it is necessary to conform the fluents with the new end symbol
		this.updateFluents();

		FormulaFactory formulaFactory = new FormulaFactory();
		formulaFactory.setFormula(formula);

		PredicateDefinition.makePredicate(output, endFluent,
				LTLf2LTS.endSymbol, alphabet);

		Formula end = formulaFactory.make(endFluent);

		FiniteFormulaGeneratorVisitor visitor = new FiniteFormulaGeneratorVisitor(
				formulaFactory, end);

		Formula formulaUpdatedByf = formula.accept(visitor);

		Formula epsilon = this.getEpsilon(output, formula, formulaFactory, end);

		Formula finalFormula = formulaFactory.makeAnd(formulaUpdatedByf,
				epsilon);

		output.outln("Finite formula: " + finalFormula);
		formulaFactory.setFormula(finalFormula);

		LabelledTransitionSystem s = this.computeAutomaton(output,
				formulaFactory, finalFormula, new Vector<>(alphabet));

		Vector<String> toHide = new Vector<>();
		toHide.add(endSymbol.getValue());
		s.conceal(toHide);
		//s = new NoAcceptingRemover().apply(s);
		s.setName(name);

		Minimiser m = new Minimiser(s, output);
		s = m.minimise();
		this.updateRemoveEndFluents();
		return s;
	}

	public LabelledTransitionSystem toLTSForPostChecking(Formula formula,
			LTSOutput output, Set<String> alphabet, String name) {

		output.outln("Running the LTLf2BA");
		// updates the fluents considering the additional endSymbol
		// it is necessary to conform the fluents with the new end symbol
		this.updateFluents();

		FormulaFactory formulaFactory = new FormulaFactory();
		formulaFactory.setFormula(formula);
		Formula toBeTransformed = formulaFactory.makeNot(formula);
		formulaFactory.setFormula(toBeTransformed);

		PredicateDefinition.makePredicate(output, endFluent,
				LTLf2LTS.endSymbol, alphabet);

		Formula end = formulaFactory.make(endFluent);

		FiniteFormulaGeneratorVisitor visitor = new FiniteFormulaGeneratorVisitor(
				formulaFactory, end);

		Formula formulaUpdatedByf = toBeTransformed.accept(visitor);

		Formula epsilon = this.getEpsilon(output, toBeTransformed,
				formulaFactory, end);

		Formula finalFormula = formulaFactory.makeAnd(formulaUpdatedByf,
				epsilon);

		output.outln("Finite formula: " + finalFormula);
		formulaFactory.setFormula(finalFormula);

		LabelledTransitionSystem s = this.computeAutomaton(output,
				formulaFactory, finalFormula, new Vector<>(alphabet));
		Vector<String> toHide = new Vector<>();
		toHide.add(endSymbol.getValue());
		s.conceal(toHide);

		System.out.println("s "+s.getStates().length+" transitions: "+s.getTransitionNumber());
		
		//s = new NoAcceptingRemover().apply(s);
		s.setName(name);
		System.out.println("2s "+s.getStates().length+" transitions: "+s.getTransitionNumber());
		
		Minimiser m = new Minimiser(s, output);
		s = m.minimise();
		System.out.println("s "+s.getStates().length+" transitions: "+s.getTransitionNumber());
		this.updateRemoveEndFluents();
		return s;
	}

	private void updateRemoveEndFluents() {
		Set<String> fluents = PredicateDefinition.definitions.keySet();
		fluents.stream().forEach(
				fluent -> {
					if (PredicateDefinition.get(fluent) != null) {

						PredicateDefinition fluentObj = PredicateDefinition
								.get(fluent);
						if (fluentObj.getTerminatingActions() != null) {
							this.fluentTerminatingActions.put(
									fluent,
									new Vector<>(fluentObj
											.getTerminatingActions()));

							fluentObj.getTerminatingActions().remove(
									endSymbol.getValue());

						}
					}
				});
		PredicateDefinition.remove(endSymbol);
		PredicateDefinition.remove(endFluent);
	}

	private void updateFluents() {

		Set<String> fluents = new HashSet<String>(
				PredicateDefinition.definitions.keySet());

		fluents.stream().forEach(
				fluent -> {

					if (PredicateDefinition.get(fluent) != null) {
						PredicateDefinition fluentObj = PredicateDefinition
								.get(fluent);
						if (fluentObj.getTerminatingActions() != null) {
							this.fluentTerminatingActions.put(
									fluent,
									new Vector<>(fluentObj
											.getTerminatingActions()));

							fluentObj.getTerminatingActions().add(
									endSymbol.getValue());

						}
					}
				});
	}

	private Formula getEpsilon(LTSOutput output, Formula formula,
			FormulaFactory formulaFactory, Formula end) {

		Formula andPart = True.make();

		for (PredicateDefinition predicate : PredicateDefinition.definitions
				.values()) {
			andPart = formulaFactory.makeAnd(andPart,
					formulaFactory.make(predicate.getSymbol()));
		}

		Formula globallyEndImpliesNextEnd = formulaFactory
				.makeAlways(formulaFactory.makeImplies(end,
						formulaFactory.makeNext(end)));

		Formula finallyEnd = formulaFactory.makeEventually(end);
		return formulaFactory.makeAnd(finallyEnd, globallyEndImpliesNextEnd);

	}

	private CompositeState computeCompositeState(LTSOutput output,
			FormulaFactory finiteFormulaFactory, Formula formula,
			Vector<String> alpha) {

		logger.debug("Converting the formula: "+formula);
		GeneralizedBuchiAutomata gba = new GeneralizedBuchiAutomata(
				formula.toString(), finiteFormulaFactory, alpha);
		gba.translate();

		Graph g = gba.makeGBA();
		output.outln("GBA " + g.getNodeCount() + " states " + g.getEdgeCount()
				+ " transitions");
		g = SuperSetReduction.reduce(g);
		Graph g1 = Degeneralize.degeneralize(g);
		g1 = SCCReduction.reduce(g1);
		g1 = Simplify.simplify(g1);
		g1 = SFSReduction.reduce(g1);
		
		
		FSAConverter c = new FSAConverter(formula.toString(),
				g1, gba.getLabelFactory());

		output.outln("Buchi automata:");

		ByteArrayOutputStream outputArray = new ByteArrayOutputStream();

		c.printFSP(new PrintStream(outputArray));
		output.out(outputArray.toString());

		// computing the composition between the system and the fluents
		CompositeState cs = this.composeWithFluents(output, gba, c);

		cs.getComposition().removeNonDetTau();

		Minimiser e = new Minimiser(cs.getComposition(), output);
		cs.setComposition(e.minimise());
		cs.getComposition().removeDetCycles("*");

		logger.debug("Post-condition automaton size: "+cs.getComposition().size());
		logger.debug("Removing states from which an accepting state can not be reached: ");		
		LabelledTransitionSystem composition=new NoAcceptingRemover().apply(cs.getComposition());
		cs.setComposition(composition);
		
		logger.debug("Post-condition automaton size: "+composition.size());
		return cs;
	}

	private LabelledTransitionSystem computeAutomaton(LTSOutput output,
			FormulaFactory finiteFormulaFactory, Formula formula,
			Vector<String> alpha) {
		return this.computeCompositeState(output, finiteFormulaFactory,
				formula, alpha).getComposition();
	}

	/**
	 * computes the composition between the GBA and the fluents
	 * 
	 * @param output
	 * @param gba
	 * @param c
	 * @return
	 */
	private CompositeState composeWithFluents(LTSOutput output,
			GeneralizedBuchiAutomata gba, FSAConverter c) {
		Vector<LabelledTransitionSystem> procs = gba.getLabelFactory()
				.getPropProcs();
		procs.add(c);

		CompositeState cs = new CompositeState(c.getName(), procs);
		cs.setHidden(gba.getLabelFactory().getPrefix());

		PredicateDefinition[] fluents = gba.getLabelFactory().getFluents();

		// composition
		cs.setFluentTracer(new FluentTrace(fluents));

		cs.compose(output, true);
		return cs;
	}

	private Graph gbaToBa(Graph gbaGraph) {
		Graph degeneralizedGraph = Degeneralize.degeneralize(gbaGraph);

		degeneralizedGraph = SCCReduction.reduce(degeneralizedGraph);
		degeneralizedGraph = Simplify.simplify(degeneralizedGraph);
		degeneralizedGraph = SFSReduction.reduce(degeneralizedGraph);
		return degeneralizedGraph;
	}
}
