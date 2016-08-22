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

/**
 * Not visitor pushes negation inside operators to get negative normal form
 */
public class NotVisitor implements FormulaVisitor {
	private FormulaFactory fac;

	public NotVisitor(FormulaFactory f) {
		fac = f;
	}

	@Override
	public Formula visit(True t) {
		return False.make();
	}

	@Override
	public Formula visit(False f) {
		return True.make();
	}

	@Override
	public Formula visit(Proposition p) {
		return fac.makeNot(p);
	}

	@Override
	public Formula visit(Not n) {
		return n.getNext();
	}

	@Override
	public Formula visit(Next n) {
		return fac.makeNext(fac.makeNot(n.getNext()));
	}

	@Override
	public Formula visit(And a) {
		return fac.makeOr(fac.makeNot(a.getLeft()), fac.makeNot(a.getRight()));
	}

	@Override
	public Formula visit(Or o) {
		return fac.makeAnd(fac.makeNot(o.getLeft()), fac.makeNot(o.getRight()));
	}

	@Override
	public Formula visit(Until u) {
		return fac.makeRelease(fac.makeNot(u.getLeft()),
				fac.makeNot(u.getRight()));
	}

	@Override
	public Formula visit(Release r) {
		return fac.makeUntil(fac.makeNot(r.getLeft()),
				fac.makeNot(r.getRight()));
	}
}
