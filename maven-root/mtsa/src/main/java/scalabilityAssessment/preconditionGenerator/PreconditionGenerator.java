package scalabilityAssessment.preconditionGenerator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.spel.ast.Identifier;

import scalabilityAssessment.propertygenerator.PropertyGenerator;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.ltl.visitors.FiniteFormulaGeneratorVisitor;
import ltsa.lts.parser.Symbol;
import ltsa.ui.EmptyLTSOuput;

public class PreconditionGenerator {

	private final PropertyGenerator propertyGenerator;

	private final List<String> alphabet;

	public PreconditionGenerator(List<String> alphabet, String event1,
			String event2) {
		this.propertyGenerator = new PropertyGenerator(alphabet, event1, event2);
		this.alphabet = alphabet;

	}

	public List<Formula> getFormulae() {
		List<Formula> formulae = new ArrayList<>();


		List<String> newAlphabet = new ArrayList<>();
		newAlphabet.add(alphabet.get(0));
		this.makePredicates("end", newAlphabet);
		for (Formula f : propertyGenerator.getFormulae()) {
			FormulaFactory factory = new FormulaFactory();
			factory.setFormula(f);
			formulae.add(f.accept(new FiniteFormulaGeneratorVisitor(factory,
					factory.make(new Symbol("F_end", Symbol.IDENTIFIER)))));
		}
		return formulae;
	}

	private void makePredicates(String end, List<String> alphabet) {

		List<String> newAlphabet = new ArrayList<>();
		newAlphabet.add(alphabet.get(0));
		Symbol eventSymbol = new Symbol(end, Symbol.UPPERIDENT);
		Symbol fluentEventSymbol = new Symbol("F_" + eventSymbol.getValue(),
				Symbol.UPPERIDENT);
		PredicateDefinition.makePredicate(new EmptyLTSOuput(),
				fluentEventSymbol, eventSymbol, newAlphabet);

	}
}
