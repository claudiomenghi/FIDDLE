/**
 * 
 */
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
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.parser.Symbol;

import com.google.common.base.Preconditions;

/**
 * @author Claudio Menghi
 *
 */
public class PreconditionFiniteFormulaGeneratorVisitor implements FormulaVisitor {

	private FormulaFactory fac;

	private Formula end;

	public PreconditionFiniteFormulaGeneratorVisitor(FormulaFactory fac, Formula end) {
		Preconditions.checkNotNull(fac, "The formula factory cannot be null");
		this.fac = fac;
		this.end = end;
	}

	/**
	 * @see ltsa.lts.ltl.visitors.FormulaVisitor#visit(ltsa.lts.ltl.formula.True)
	 */
	@Override
	public Formula visit(True t) {
		return True.make();
	}

	/**
	 * @see ltsa.lts.ltl.visitors.FormulaVisitor#visit(ltsa.lts.ltl.formula.False)
	 */
	@Override
	public Formula visit(False f) {
		return False.make();
	}

	/**
	 * @see ltsa.lts.ltl.visitors.FormulaVisitor#visit(ltsa.lts.ltl.formula.Proposition)
	 */
	@Override
	public Formula visit(Proposition p) {

		return fac.make(new Symbol(p.getSymbol()));

	}

	/**
	 * @see ltsa.lts.ltl.visitors.FormulaVisitor#visit(ltsa.lts.ltl.formula.Not)
	 */
	@Override
	public Formula visit(Not n) {
		return fac.makeNot(n.getNext().accept(this));
	}

	/**
	 * @see ltsa.lts.ltl.visitors.FormulaVisitor#visit(ltsa.lts.ltl.formula.And)
	 */
	@Override
	public Formula visit(And a) {
		return fac.makeAnd(a.getLeft().accept(this), a.getRight().accept(this));
	}

	/**
	 * @see ltsa.lts.ltl.visitors.FormulaVisitor#visit(ltsa.lts.ltl.formula.Or)
	 */
	@Override
	public Formula visit(Or o) {
		return fac.makeOr(o.getLeft().accept(this), o.getRight().accept(this));
	}

	/**
	 * @see ltsa.lts.ltl.visitors.FormulaVisitor#visit(ltsa.lts.ltl.formula.Until)
	 */
	@Override
	public Formula visit(Until u) {
		return fac.makeUntil(
				u.getLeft().accept(this),
				fac.makeAnd(u.getRight().accept(this),
						fac.makeNot(end.accept(this))));
	}

	/**
	 * @see ltsa.lts.ltl.visitors.FormulaVisitor#visit(ltsa.lts.ltl.formula.Release)
	 */
	@Override
	public Formula visit(Release r) {
		return fac.makeNot(fac.makeUntil(fac.makeNot(r.getLeft()),
				fac.makeNot(r.getRight())).accept(this));

	}

	/**
	 * @see ltsa.lts.ltl.visitors.FormulaVisitor#visit(ltsa.lts.ltl.formula.Next)
	 */
	@Override
	public Formula visit(Next n) {
		return fac.makeNext(fac.makeAnd(n.getNext().accept(this),
				fac.makeNot(end)));
	}
}