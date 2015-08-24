package lts.chart.util;

import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.Formula;
import lts.ltl.FormulaTransformerVisitor;

import java.util.Set;

/**
 * Created by Victor Wjugow on 04/06/15.
 */
public class FormulaUtils {

	/**
	 * Transforms the formula to a Formula as needed by the synthesiser.
	 * The fluents involved in the Formula are added to the set.
	 *
	 * @param formula
	 * @param involvedFluents
	 * @return
	 */
	public static Formula adaptFormulaAndCreateFluents(lts.ltl.Formula formula, Set<Fluent> involvedFluents) {
		// create a visitor for the formula
		FormulaTransformerVisitor formulaTransformerVisitor = new FormulaTransformerVisitor();
		formula.accept(formulaTransformerVisitor);

		// After visiting the formula, the visitor has the transformed formula and the involved fluents
		involvedFluents.addAll(formulaTransformerVisitor.getInvolvedFluents());
		return formulaTransformerVisitor.getTransformedFormula();
	}
}