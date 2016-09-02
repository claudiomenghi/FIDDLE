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

		alphabetNew = new ArrayList<>(alphabet);
		alphabetNew.remove(event2);
		this.makePredicate(event2, alphabetNew);

		// EXPERIMENT 1
		// Formula p1 = generateP1(alphabet);
		// returnFormulae.add(p1);

		// Formula p2 = generateP2(alphabet);
		// returnFormulae.add(p2);

		// Formula p3 = generateP3(alphabet);
		// returnFormulae.add(p3);

		// THREATS TO VALIDITY
		Formula p4 = generateP4(alphabet);
		returnFormulae.add(p4);

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

	// [](!Q) | <>(Q & <>P))
	private Formula generateP1(List<String> alphabet) {
		FormulaFactory formulaFactory = new FormulaFactory();
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1,
				Symbol.UPPERIDENT));

		Formula ap2 = formulaFactory.make(new Symbol("F_" + event2,
				Symbol.UPPERIDENT));
		return formulaFactory.makeOr(formulaFactory.makeAlways(formulaFactory
				.makeNot(ap1)), formulaFactory.makeEventually(formulaFactory
				.makeAnd(ap1, formulaFactory.makeEventually(ap2))));

		// formulaFactory.makeEventually(ap1);

	}

	// p is false before r <>R -> (!P U R)
	private Formula generateP2(List<String> alphabet) {
		FormulaFactory formulaFactory = new FormulaFactory();
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1,
				Symbol.UPPERIDENT));
		Formula ap2 = formulaFactory.make(new Symbol("F_" + event2,
				Symbol.UPPERIDENT));
		return formulaFactory.makeImplies(formulaFactory.makeEventually(ap1),
				formulaFactory.makeUntil(formulaFactory.makeNot(ap2), ap1));

	}

	private Formula generateP3(List<String> alphabet) {
		FormulaFactory formulaFactory = new FormulaFactory();
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1,
				Symbol.UPPERIDENT));
		Formula ap2 = formulaFactory.make(new Symbol("F_" + event2,
				Symbol.UPPERIDENT));
		return formulaFactory.makeAlways(formulaFactory.makeImplies(ap1, ap2));

	}

	private Formula generateP4(List<String> alphabet) {
		FormulaFactory formulaFactory = new FormulaFactory();
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1,
				Symbol.UPPERIDENT));
		Formula ap2 = formulaFactory.make(new Symbol("F_" + event2,
				Symbol.UPPERIDENT));
		return formulaFactory.makeEventually(formulaFactory.makeImplies(ap1,
				formulaFactory.makeAlways(ap2)));

	}

	private void makePredicate(String end, List<String> alphabet) {

		Symbol eventSymbol = new Symbol(end, Symbol.UPPERIDENT);
		Symbol fluentEventSymbol = new Symbol("F_" + eventSymbol.getValue(),
				Symbol.UPPERIDENT);
		List<String> alphabet2 = new ArrayList<>();
		alphabet2.add(alphabet.get(0));
		PredicateDefinition.makePredicate(new EmptyLTSOuput(),
				fluentEventSymbol, eventSymbol, alphabet2);

	}

}
