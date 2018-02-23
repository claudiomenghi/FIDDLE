package ltsa.lts.ltl.visitors;

import java.util.List;

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
 * Untils visitor computes the untils indexes
 */
public class UntilVisitor implements FormulaVisitor {

	private List<Formula> ll;

	public UntilVisitor(List<Formula> l) {
		ll = l;
	}

	@Override
	public Formula visit(True t) {
		return t;
	}

	@Override
	public Formula visit(False f) {
		return f;
	}

	@Override
	public Formula visit(Proposition p) {
		return p;
	}

	@Override
	public Formula visit(Not n) {
		n.getNext().accept(this);
		return n;
	}

	@Override
	public Formula visit(Next n) {
		n.getNext().accept(this);
		return n;
	}

	@Override
	public Formula visit(And a) {
		a.getLeft().accept(this);
		a.getRight().accept(this);
		return a;
	}

	@Override
	public Formula visit(Or o) {
		o.getLeft().accept(this);
		o.getRight().accept(this);
		return o;
	}

	@Override
	public Formula visit(Until u) {
		if (!u.visited()) {
			u.setVisited();
			ll.add(u);
			u.setUI(ll.size() - 1);
			u.getRight().setRofUI(ll.size() - 1);
			u.getLeft().accept(this);
			u.getRight().accept(this);
		}
		return u;
	}

	@Override
	public Formula visit(Release r) {
		r.getLeft().accept(this);
		r.getRight().accept(this);
		return r;
	}
}