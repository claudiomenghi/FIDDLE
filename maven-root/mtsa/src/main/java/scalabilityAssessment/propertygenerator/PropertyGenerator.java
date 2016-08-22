package scalabilityAssessment.propertygenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.parser.Symbol;
import ltsa.ui.EmptyLTSOuput;

public class PropertyGenerator {

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

		Formula finalAndFormula = generateP1(alphabet);
		returnFormulae.add(finalAndFormula);

		Formula p2 = generateP2(alphabet);
		returnFormulae.add(p2);

		/*
		 * Formula p3 = generateP3(formulaFactory); finalAndFormula =
		 * formulaFactory.makeAnd(finalAndFormula, p3);
		 * returnFormulae.add(newFormula);
		 * 
		 * newFormula = generateP4(formulaFactory); finalAndFormula =
		 * formulaFactory.makeAnd(finalAndFormula, newFormula);
		 * returnFormulae.add(newFormula);
		 * 
		 * newFormula = generateP5(formulaFactory); finalAndFormula =
		 * formulaFactory.makeAnd(finalAndFormula, newFormula);
		 * returnFormulae.add(newFormula);
		 */

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
		Formula ap = formulaFactory.make(new Symbol("F_" + event1,
				Symbol.UPPERIDENT));
		return formulaFactory.makeEventually(ap);

	}

	// p is false before r <>R -> (!P U R)
	private Formula generateP2(List<String> alphabet) {
		FormulaFactory formulaFactory = new FormulaFactory();
		Formula ap1 = formulaFactory.make(new Symbol("F_" +event1,
				Symbol.UPPERIDENT));
		Formula ap2 = formulaFactory.make(new Symbol("F_" +event1,
				Symbol.UPPERIDENT));
		return 
				formulaFactory.makeImplies(
				formulaFactory.makeEventually(ap1),
				formulaFactory.makeUntil(formulaFactory.makeNot(ap2), ap1));
		
	}

	private void makePredicate(String end, List<String> alphabet) {

		Symbol eventSymbol = new Symbol(end, Symbol.UPPERIDENT);
		Symbol fluentEventSymbol = new Symbol("F_" + eventSymbol.getValue(),
				Symbol.UPPERIDENT);
		List<String> alphabet2=new ArrayList<>();
		alphabet2.add(alphabet.get(0));
		PredicateDefinition.makePredicate(new EmptyLTSOuput(),
				fluentEventSymbol, eventSymbol,alphabet2);

	}

}
