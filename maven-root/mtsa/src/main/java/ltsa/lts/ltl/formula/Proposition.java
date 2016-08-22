package ltsa.lts.ltl.formula;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;

import ltsa.lts.ltl.visitors.FormulaVisitor;
import ltsa.lts.parser.Symbol;

/**
 * represent a proposition of the formula
 */
public class Proposition extends Formula {
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
}
