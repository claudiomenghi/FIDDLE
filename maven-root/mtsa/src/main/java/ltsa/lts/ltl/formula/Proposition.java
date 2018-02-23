package ltsa.lts.ltl.formula;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;

import ltsa.lts.ltl.visitors.FormulaVisitor;
import ltsa.lts.parser.Symbol;

/**
 * represent a proposition of the formula
 */
public class Proposition extends Formula implements Comparable<Formula>{
	

	private final Symbol symbol;

	/**
	 * 
	 * @param symbol
	 *            the symbol of the proposition
	 * @throws NullPointerException
	 *             if the symbol is null
	 */
	public Proposition(Symbol symbol) {
		Preconditions.checkNotNull(symbol,
				"The symbol of the proposition cannot be null");
		this.symbol = symbol;
	}

	public Symbol getSymbol() {
		return this.symbol;
	}

	@Override
	public String toString() {
		return symbol.toString();
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
		Set<Proposition> retPropositions = new HashSet<>();
		retPropositions.add(this);
		return retPropositions;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Proposition other = (Proposition) obj;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		return true;
	}
	@Override
	public Set<Formula> getSubformulae() {
		Set<Formula> formulae=new HashSet<>();
		formulae.add(this);
		return formulae;
	}
}
