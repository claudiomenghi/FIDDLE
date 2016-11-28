package ltsa.lts.ltl.formula;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;

import ltsa.lts.ltl.visitors.FormulaVisitor;

/*
 * represent release R
 */

public class Release extends Formula {
	private final Formula left;
	private final Formula right;

	public Release(Formula l, Formula r) {
		Preconditions.checkNotNull(l, "The left formula cannot be null");
		Preconditions.checkNotNull(r, "The right formula cannot be null");
		left = l;
		right = r;
	}

	public Formula getLeft() {
		return left;
	}

	public Formula getRight() {
		return right;
	}

	@Override
	public String toString() {
		return "(" + left.toString() + " R " + right.toString() + ")";
	}

	@Override
	public Formula accept(FormulaVisitor v) {
		return v.visit(this);
	}

	@Override
	public Set<Proposition> getPropositions() {
		Set<Proposition> retPropositions = new HashSet<>();
		retPropositions.addAll(left.getPropositions());
		retPropositions.addAll(right.getPropositions());
		return retPropositions;
	}
	@Override
	public Set<Formula> getSubformulae() {
		Set<Formula> formulae=new HashSet<>();
		formulae.add(left);
		formulae.add(right);
		return formulae;
	}
}