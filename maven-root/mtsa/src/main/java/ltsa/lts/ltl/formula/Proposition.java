package ltsa.lts.ltl.formula;

import java.util.HashSet;
import java.util.Set;

import ltsa.lts.ltl.visitors.FormulaVisitor;
import ltsa.lts.parser.Symbol;

/*
 * represent proposition
 */
public class Proposition extends Formula {
	private final Symbol sym;

	public Proposition(Symbol s) {
		sym = s;
	}

	public Symbol getSymbol() {
		return this.sym;
	}

	@Override
	public String toString() {
		return sym.toString();
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
		retPropositions.add(this);
		return retPropositions;
	}
}
