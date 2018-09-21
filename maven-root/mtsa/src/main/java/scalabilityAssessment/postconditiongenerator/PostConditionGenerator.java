package scalabilityAssessment.postconditiongenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

<<<<<<< HEAD
<<<<<<< HEAD
=======
import com.google.common.base.Preconditions;

>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
import com.google.common.base.Preconditions;

>>>>>>> dev
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.parser.Symbol;
import ltsa.ui.EmptyLTSOuput;

public class PostConditionGenerator {

	private final List<Formula> returnFormulae;
	private final String event1;
	private final String event2;

<<<<<<< HEAD
<<<<<<< HEAD
	public PostConditionGenerator(List<String> alphabet, String event1,
			String event2) {
=======
	public PostConditionGenerator(List<String> alphabet, String event1, String event2) {
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
	public PostConditionGenerator(List<String> alphabet, String event1, String event2) {
>>>>>>> dev
		this.event1 = event1;
		this.event2 = event2;

		this.makePredicate("end", alphabet);

		List<String> alphabetNew = new ArrayList<>(alphabet);
		alphabetNew.remove(event1);
		this.makePredicate(event1, alphabetNew);
		alphabetNew = new ArrayList<>(alphabet);
		alphabetNew.remove(event2);
		this.makePredicate(event2, alphabetNew);

		returnFormulae = new ArrayList<>();
		FormulaFactory formulaFactory = new FormulaFactory();

		// EXPERIMENT 1
<<<<<<< HEAD
<<<<<<< HEAD
		//Formula f1 = generateP1(formulaFactory);
		//returnFormulae.add(f1);
=======
		// Formula f1 = generateP1(formulaFactory);
		// returnFormulae.add(f1);
>>>>>>> dev

		// Formula f2 = generateP2(formulaFactory);
		// returnFormulae.add(f2);

		// Formula f3 = generateP3(formulaFactory);
		// returnFormulae.add(f3);
		// THREATS TO VALIDITY
		returnFormulae.add(generateP1(formulaFactory));
		returnFormulae.add(generateP2(alphabet));
		returnFormulae.add(generateP3(alphabet));
	}

	private void makePredicate(String end, List<String> alphabet) {
		Preconditions.checkNotNull(end, "The end string cannot be null");
		Preconditions.checkNotNull(alphabet, "The alphabet cannot be null");

		Symbol eventSymbol = new Symbol(end, Symbol.UPPERIDENT);
<<<<<<< HEAD
		Symbol fluentEventSymbol = new Symbol("F_" + eventSymbol.getValue(),
				Symbol.UPPERIDENT);
		PredicateDefinition.makePredicate(new EmptyLTSOuput(),
				fluentEventSymbol, eventSymbol, 
				new HashSet<String>(alphabet)
				);
=======
		// Formula f1 = generateP1(formulaFactory);
		// returnFormulae.add(f1);

		// Formula f2 = generateP2(formulaFactory);
		// returnFormulae.add(f2);

		// Formula f3 = generateP3(formulaFactory);
		// returnFormulae.add(f3);
		// THREATS TO VALIDITY
		returnFormulae.add(generateP1(formulaFactory));
		returnFormulae.add(generateP2(alphabet));
		returnFormulae.add(generateP3(alphabet));
	}

	private void makePredicate(String end, List<String> alphabet) {
		Preconditions.checkNotNull(end, "The end string cannot be null");
		Preconditions.checkNotNull(alphabet, "The alphabet cannot be null");

		Symbol eventSymbol = new Symbol(end, Symbol.UPPERIDENT);
		Symbol fluentEventSymbol = new Symbol("F_" + eventSymbol.getValue(), Symbol.UPPERIDENT);
		PredicateDefinition.makePredicate(new EmptyLTSOuput(), fluentEventSymbol, eventSymbol,
				new HashSet<String>(alphabet));
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
		Symbol fluentEventSymbol = new Symbol("F_" + eventSymbol.getValue(), Symbol.UPPERIDENT);
		PredicateDefinition.makePredicate(new EmptyLTSOuput(), fluentEventSymbol, eventSymbol,
				new HashSet<String>(alphabet));
>>>>>>> dev

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

	private Formula generateP1(FormulaFactory formulaFactory) {

<<<<<<< HEAD
<<<<<<< HEAD
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1,
				Symbol.UPPERIDENT));

		Formula ap2 = formulaFactory.make(new Symbol("F_" + event2,
				Symbol.UPPERIDENT));
		return formulaFactory.makeEventually(formulaFactory.makeAnd(ap1,
				formulaFactory.makeEventually(ap2)));
=======
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1, Symbol.UPPERIDENT));
>>>>>>> dev

		return formulaFactory.makeAlways(formulaFactory.makeNot(ap1));
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

<<<<<<< HEAD
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1,
				Symbol.UPPERIDENT));
		return formulaFactory.makeAlways(formulaFactory.makeNot(ap1));
	}
	
	private Formula generateP4(FormulaFactory formulaFactory) {

		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1,
				Symbol.UPPERIDENT));
		Formula ap2 = formulaFactory.make(new Symbol("F_" + event2,
				Symbol.UPPERIDENT));
		return formulaFactory.makeAlways(formulaFactory.makeAnd(ap1, ap2));
=======
		Formula ap1 = formulaFactory.make(new Symbol("F_" + event1, Symbol.UPPERIDENT));

		return formulaFactory.makeAlways(formulaFactory.makeNot(ap1));
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

>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
>>>>>>> dev
	}
}
