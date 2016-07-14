package ltsa.lts.ltl.formula;

import java.util.HashSet;
import java.util.Set;

import ltsa.lts.ltl.visitors.FormulaVisitor;

/**
 * represent not !
 */
public class Not extends Formula {
	Formula next;

	public Not(Formula f) {
		next = f;
	}

	public Formula getNext() {
		return next;
	}
	
	@Override
	public String toString() {
		return "!" + next.toString();
	}

	@Override
	public Formula accept(FormulaVisitor v) {
		return v.visit(this);
	}

	@Override
	public boolean isLiteral() {
		return next.isLiteral();
	}
	@Override
	public Set<Proposition> getPropositions() {
		Set<Proposition> retPropositions=new HashSet<>();
		retPropositions.addAll(next.getPropositions());
		return retPropositions;
	}
}