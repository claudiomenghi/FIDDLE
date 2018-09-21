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

<<<<<<< HEAD
=======
	public static int numFormulae=3;
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
	private final List<Formula> returnFormulae;

	private final String event1;
	private final String event2;

<<<<<<< HEAD
=======
	
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
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

<<<<<<< HEAD
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
=======
		returnFormulae.add(generateP1(alphabet));
		returnFormulae.add(generateP2(alphabet));
		returnFormulae.add(generateP3(alphabet));
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8

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

<<<<<<< HEAD
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
=======
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
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
		return formulaFactory.makeImplies(formulaFactory.makeEventually(ap1),
				formulaFactory.makeUntil(formulaFactory.makeNot(ap2), ap1));

	}

<<<<<<< HEAD
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
=======
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
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8

	}

	private void makePredicate(String end, List<String> alphabet) {

		Symbol eventSymbol = new Symbol(end, Symbol.UPPERIDENT);
<<<<<<< HEAD
		Symbol fluentEventSymbol = new Symbol("F_" + eventSymbol.getValue(),
				Symbol.UPPERIDENT);
		List<String> alphabet2 = new ArrayList<>();
		alphabet2.add(alphabet.get(0));
		PredicateDefinition.makePredicate(new EmptyLTSOuput(),
				fluentEventSymbol, eventSymbol, new HashSet<>(alphabet2));

=======
		Symbol fluentEventSymbol = new Symbol("F_" + eventSymbol.getValue(), Symbol.UPPERIDENT);
		List<String> alphabet2 = new ArrayList<>();
		alphabet2.add(alphabet.get(0));
		PredicateDefinition.makePredicate(new EmptyLTSOuput(), fluentEventSymbol, eventSymbol,
				new HashSet<>(alphabet2));
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
	}

}
