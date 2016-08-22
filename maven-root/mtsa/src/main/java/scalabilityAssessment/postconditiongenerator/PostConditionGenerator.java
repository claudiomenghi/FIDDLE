package scalabilityAssessment.postconditiongenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.ltl.visitors.FiniteFormulaGeneratorVisitor;
import ltsa.lts.parser.Symbol;
import ltsa.ui.EmptyLTSOuput;

public class PostConditionGenerator {

	private final List<Formula> returnFormulae;
	private final String event1;
	private final String event2;

	public PostConditionGenerator(List<String> alphabet, String event1, String event2) {
		this.makePredicate("end", alphabet);

		List<String> alphabetNew=new ArrayList<>(alphabet);
		alphabetNew.remove(event1);
		this.makePredicate(event1, alphabetNew);
		returnFormulae = new ArrayList<>();
		this.event1=event1;
		this.event2=event2;
		FormulaFactory formulaFactory = new FormulaFactory();

		Formula f1=generateP1(formulaFactory);
		f1=f1.accept(new FiniteFormulaGeneratorVisitor(formulaFactory, formulaFactory
				.make(new Symbol("F_end", Symbol.IDENTIFIER))));
		
		returnFormulae.add(f1);
		// returnFormulae.add(generateP2(formulaFactory));
		// returnFormulae.add(generateP3(formulaFactory));
		// returnFormulae.add(generateP4(formulaFactory));
		// returnFormulae.add(generateP5(formulaFactory));

	}

	
	
	private void makePredicate(String end, List<String> alphabet) {

		Symbol eventSymbol = new Symbol(end, Symbol.UPPERIDENT);
		Symbol fluentEventSymbol = new Symbol("F_" + eventSymbol.getValue(),
				Symbol.UPPERIDENT);
		PredicateDefinition.makePredicate(new EmptyLTSOuput(),
				fluentEventSymbol, eventSymbol, alphabet);

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

		Formula ap = formulaFactory.make(new Symbol("F_" +event1,
				Symbol.UPPERIDENT));
		return formulaFactory.makeEventually(ap);
	}

}
