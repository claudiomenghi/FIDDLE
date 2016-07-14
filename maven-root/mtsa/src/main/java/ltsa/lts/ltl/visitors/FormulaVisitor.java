package ltsa.lts.ltl.visitors;

import ltsa.lts.ltl.formula.And;
import ltsa.lts.ltl.formula.False;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.Next;
import ltsa.lts.ltl.formula.Not;
import ltsa.lts.ltl.formula.Or;
import ltsa.lts.ltl.formula.Proposition;
import ltsa.lts.ltl.formula.Release;
import ltsa.lts.ltl.formula.True;
import ltsa.lts.ltl.formula.Until;

/**
 * Visitor interface to Formula
 * @author gsibay
 *
 */
public interface FormulaVisitor {

	public Formula visit(True t);

	public Formula visit(False f);

	public Formula visit(Proposition p);

	public Formula visit(Not n);

	public Formula visit(And a);

	public Formula visit(Or o);

	public Formula visit(Until u);

	public Formula visit(Release r);

	public Formula visit(Next n);
}
