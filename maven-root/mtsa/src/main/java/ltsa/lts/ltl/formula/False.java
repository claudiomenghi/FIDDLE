package ltsa.lts.ltl.formula;

import java.util.HashSet;
import java.util.Set;

import ltsa.lts.ltl.formula.False;
import ltsa.lts.ltl.visitors.FormulaVisitor;

/*
 * represent constant False
 */

 public class False extends Formula {
	private static False f;

	public False() {
	}

	public static False make() {
		if (f == null) {
			f = new False();
			f.setId(0);
		}
		return f;
	}

	@Override
	public String toString() {
		return "false";
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
		return new HashSet<>();
	}
	
	@Override
	public Set<Formula> getSubformulae() {
		Set<Formula> formulae=new HashSet<>();
		formulae.add(this);
		return formulae;
	}
}
