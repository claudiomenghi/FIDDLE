package ltsa.lts;

import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.FormulaSyntax;
import ltsa.updatingControllers.UpdateConstants;
import ltsa.control.ControllerGoalDefinition;
import MTSSynthesis.controller.game.util.GeneralConstants;

public class UpdatingControllersGoalsMaker {

	public static void addOldGoals(Symbol formulaName, ControllerGoalDefinition cgd) {
		// getting elements that I need to build the formula
		Symbol arrow = new Symbol(Symbol.ARROW);
		Symbol always = new Symbol(Symbol.ALWAYS);
		Symbol not = new Symbol(Symbol.PLING);
		ActionName stopOldSpecActionName = new ActionName(new Symbol(123, "StopOldSpec"));
		FormulaSyntax stopOldSpecFormula = FormulaSyntax.make(stopOldSpecActionName);
		FormulaSyntax dontDo = FormulaSyntax.make(null, not, stopOldSpecFormula);
		FormulaSyntax originalFormula = obtainFormula(formulaName);
		// building formula
		FormulaSyntax finalFormula = FormulaSyntax.make(dontDo, arrow, originalFormula);
//		FormulaSyntax finalFormula = FormulaSyntax.make(null, always, implicationFormula);
		// saving formula
		addFormula(cgd, formulaName.toString(), finalFormula, UpdateConstants.OLD_SUFFIX);
		
	}

	public static void addImplyUpdatingGoal(Symbol formulaName, ControllerGoalDefinition cgd) {
		// getting elements that I need to build the formula
		Symbol arrow = new Symbol(Symbol.ARROW);
		Symbol always = new Symbol(Symbol.ALWAYS);
		ActionName startNewSpecActionName = new ActionName(new Symbol(123, "StartNewSpec"));
		FormulaSyntax startNewSpecFormula = FormulaSyntax.make(startNewSpecActionName);
		FormulaSyntax originalFormula = obtainFormula(formulaName);
		// building formula
		FormulaSyntax finalFormula = FormulaSyntax.make(startNewSpecFormula, arrow, originalFormula);
//		FormulaSyntax finalFormula = FormulaSyntax.make(null, always, implicationFormula);
		// saving formula
		addFormula(cgd, formulaName.toString(), finalFormula, UpdateConstants.NEW_SUFFIX);
	}

	public static void addDontDoTwiceGoal(ControllerGoalDefinition cgd, String action, String formulaName) {

		Symbol arrow = new Symbol(Symbol.ARROW);
		Symbol always = new Symbol(Symbol.ALWAYS);
		Symbol next = new Symbol(Symbol.NEXTTIME);
		Symbol not = new Symbol(Symbol.PLING);
		ActionName actionName = new ActionName(new Symbol(123, action));
		FormulaSyntax formula = FormulaSyntax.make(actionName);

		FormulaSyntax dontDo = FormulaSyntax.make(null, not, formula);
		FormulaSyntax nextDontDo = FormulaSyntax.make(null, next, dontDo);
		FormulaSyntax implicationFormula = FormulaSyntax.make(formula, arrow, nextDontDo);
		FormulaSyntax finalFormula = FormulaSyntax.make(null, always, implicationFormula);

		// saving formula
		addFormula(cgd, formulaName, finalFormula, GeneralConstants.EMPTY_STRING);
	}

	/**
	 * @param formulaName
	 * @return
	 */
	private static FormulaSyntax obtainFormula(Symbol formulaName) {
		//TODO: Also consider that instead of a formula we could get a machine / lts
		AssertDefinition def = AssertDefinition.getConstraint(formulaName.getName());
		if (def == null) {
			throw new RuntimeException("ltl_property " + formulaName + " not found");
		}
		return def.getLTLFormula().removeLeftTemporalOperators();
	}

	private static void addFormula(ControllerGoalDefinition cgd, String name, FormulaSyntax formula, String suffix) {
		Symbol finalFormulaName = new Symbol(123, name + suffix);
		if (AssertDefinition.getConstraint(finalFormulaName.toString()) == null) {
			//TODO display a warning about possible duplicate property
			AssertDefinition.put(finalFormulaName, formula, null, null, null, true, false);
		}
		cgd.addSafetyDefinition(finalFormulaName);
	}


}