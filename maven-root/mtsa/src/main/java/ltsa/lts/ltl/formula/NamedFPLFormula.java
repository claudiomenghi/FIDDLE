package ltsa.lts.ltl.formula;

import ltsa.lts.Diagnostics;
import ltsa.lts.ltl.FormulaSyntax;
/**
 * This class represents a named Fluent Propositional Logic Formula.
 * 
 * @author dipi
 *
 */
public class NamedFPLFormula extends NamedFormula {

	public NamedFPLFormula(String name, FormulaSyntax formula) {
		super(name, formula);
		if(!formula.isPropositionalLogic()) {
			Diagnostics.fatal ("Assume must be a Fluent Propositional Logic formula");
		}
	}
}
