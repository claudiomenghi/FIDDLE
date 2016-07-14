package ltsa.lts.ltl.formula;

import java.util.HashSet;
import java.util.Set;

import ltsa.lts.ltl.visitors.FormulaVisitor;

/*
 * represent and /\ &
 */

public class And extends Formula {
	Formula left, right;

	public And(Formula l, Formula r) {
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
		return "(" + left.toString() + " & " + right.toString() + ")";
	}

	@Override
	public Formula accept(FormulaVisitor v) {
		return v.visit(this);
	}

	@Override
	public Set<Proposition> getPropositions() {
		Set<Proposition> retPropositions=new HashSet<>();
		retPropositions.addAll(left.getPropositions());
		retPropositions.addAll(right.getPropositions());
		return retPropositions;
	}
}
