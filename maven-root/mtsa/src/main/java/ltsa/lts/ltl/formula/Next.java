package ltsa.lts.ltl.formula;

import java.util.HashSet;
import java.util.Set;

import ltsa.lts.ltl.visitors.FormulaVisitor;

/*
 * represent next X
 */
public class Next extends Formula {
	Formula next;

	public Next(Formula f) {
		next = f;
	}

	public Formula getNext() {
		return next;
	}

	@Override
	public String toString() {
		return "X " + next.toString();
	}

	@Override
	public Formula accept(FormulaVisitor v) {
		return v.visit(this);
	}
	@Override
	public Set<Proposition> getPropositions() {
		Set<Proposition> retPropositions=new HashSet<>();
		retPropositions.addAll(next.getPropositions());
		return retPropositions;
	}
}