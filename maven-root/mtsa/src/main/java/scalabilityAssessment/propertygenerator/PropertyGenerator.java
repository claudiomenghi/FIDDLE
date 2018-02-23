package scalabilityAssessment.propertygenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.parser.Symbol;
import ltsa.ui.EmptyLTSOuput;

public class PropertyGenerator {

	public static int numFormulae=3;
	private final List<Formula> returnFormulae;

	private final String event1;
	private final String event2;

	
	public PropertyGenerator(List<String> alphabet, String event1, String event2) {
		this.event1 = event1;
		this.event2 = event2;
		this.returnFormulae = new ArrayList<>();
		List<String> alphabetNew = new ArrayList<>(alphabet);
		alphabetNew.remove(event1);
		this.makePredicate(event1, alphabetNew);

		alphabetNew = new ArrayList<>(alphabet);
		alphabetNew.remove(event2);
		this.makePredicate(event2, alphabetNew);

		returnFormulae.add(generateP1(alphabet));
		returnFormulae.add(generateP2(alphabet));
		returnFormulae.add(generateP3(alphabet));

	}

	/**
	 * returns the formulae generated. The formulae are defined over the
	 * alphabet 1,2
	 * 
	 * @return the formulae generated. The formulae are defined over the
	 *         alphabet 1,2
	 */
	public List<Formula> getFormulae() {
		return Collections.unmodifiableList(returnFormulae);
	}

	private Formula generateP1(List<String> alphabet) {
		FormulaFactory formulaFactory = new FormulaFactory();
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1, Symbol.UPPERIDENT));

		Formula ap2 = formulaFactory.make(new Symbol("F_" + event2, Symbol.UPPERIDENT));
		return formulaFactory.makeAlways(formulaFactory.makeImplies(ap1, ap2));

	}

	/**
	 * p is false before r <>R -> (!P U R)
	 * 
	 * @param alphabet
	 * @return
	 */
	private Formula generateP2(List<String> alphabet) {
		FormulaFactory formulaFactory = new FormulaFactory();
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1, Symbol.UPPERIDENT));
		Formula ap2 = formulaFactory.make(new Symbol("F_" + event2, Symbol.UPPERIDENT));
		return formulaFactory.makeImplies(formulaFactory.makeEventually(ap1),
				formulaFactory.makeUntil(formulaFactory.makeNot(ap2), ap1));

	}

	/**
	 * [](Q -> [](!P))
	 * 
	 * @param alphabet
	 * @return
	 */
	private Formula generateP3(List<String> alphabet) {
		FormulaFactory formulaFactory = new FormulaFactory();
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1, Symbol.UPPERIDENT));
		Formula ap2 = formulaFactory.make(new Symbol("F_" + event2, Symbol.UPPERIDENT));
		return formulaFactory
				.makeAlways(formulaFactory.makeImplies(ap1, formulaFactory.makeAlways(formulaFactory.makeNot(ap2))));

	}

	private void makePredicate(String end, List<String> alphabet) {

		Symbol eventSymbol = new Symbol(end, Symbol.UPPERIDENT);
		Symbol fluentEventSymbol = new Symbol("F_" + eventSymbol.getValue(), Symbol.UPPERIDENT);
		List<String> alphabet2 = new ArrayList<>();
		alphabet2.add(alphabet.get(0));
		PredicateDefinition.makePredicate(new EmptyLTSOuput(), fluentEventSymbol, eventSymbol,
				new HashSet<>(alphabet2));
	}

}
