package ltsa.lts.ltl.formula;

import java.util.HashSet;
import java.util.Set;

import ltsa.lts.ltl.visitors.FormulaVisitor;

public class True extends Formula {
	private static True t;

	public True() {
	}

	public static True make() {
		if (t == null) {
			t = new True();
			t.setId(1);
		}
		return t;
	}

	@Override
	public String toString() {
		return "true";
	}

	@Override
	public Formula accept(FormulaVisitor v) {
		return v.visit(this);
	}

	@Override
	public boolean isLiteral() {
		return true;
	}
	@Override
	public Set<Proposition> getPropositions() {
		Set<Proposition> retPropositions=new HashSet<>();
		return retPropositions;
	}
}