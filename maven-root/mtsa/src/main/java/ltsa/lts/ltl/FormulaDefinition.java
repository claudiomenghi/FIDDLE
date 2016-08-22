package ltsa.lts.ltl;

import java.util.Hashtable;
import java.util.Vector;

import com.google.common.base.Preconditions;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.parser.actions.LabelSet;

public class FormulaDefinition {

	private Symbol symbol;
	FormulaSyntax ltlFormula;
	CompositeState cached;
	LabelSet alphaExtension;
	

	Hashtable<String, Value> initParams; // initial parameter values name,value
	Vector<String> params; // list of parameter names
	FormulaFactory fac;

	/**
	 * 
	 * @param name
	 * @param ltlFormula
	 * @param ls
	 * @param initialParams
	 * @param finalParams
	 */
	public FormulaDefinition(Symbol name, FormulaSyntax ltlFormula,
			LabelSet ls, Hashtable<String, Value> initialParams, Vector<String> finalParams) {
		Preconditions.checkNotNull(name,
				"The name of the formula cannot be null");
		Preconditions.checkNotNull(ltlFormula,
				"The syntax of the formula cannot be null");
		Preconditions.checkNotNull(initialParams, "The ip table cannot be null");
		Preconditions.checkNotNull(finalParams, "The p table cannot be null");

		this.symbol = name;
		this.ltlFormula = ltlFormula;
		this.cached = null;
		this.alphaExtension = ls;
		this.initParams = initialParams;
		this.params = finalParams;
		this.fac = new FormulaFactory();
	}

	public boolean isCached() {
		return cached != null;
	}

	public void setCached(CompositeState process) {
		Preconditions.checkNotNull(process, "The process cannot be null");
		
		this.cached = process;
	}

	public CompositeState getCached() {
		return this.cached;
	}

	public FormulaFactory getFac() {
		return fac;
	}

	public Vector<String> getParams() {
		return params;
	}

	public Hashtable<String, Value> getInitialParams() {
		return initParams;
	}

	public FormulaSyntax getLTLFormula() {
		return ltlFormula;
	}

	/**
	 * 
	 * @param original
	 *            if true returns the negation of the formula if false returns
	 *            the formula
	 * 
	 * @return the negation of the formula if original is true. the formula if
	 *         original is false
	 */
	public Formula getFormula(boolean original) {
		if (this.fac != null) {
			if (original) {
				return this.fac.makeNot(this.fac.getFormula());
			} else {
				return this.fac.getFormula();
			}
		} else {
			return null;
		}
	}
	
	public LabelSet getAlphaExtension() {
		return alphaExtension;
	}

	/**
	 * @return the name
	 */
	public Symbol getSymbol() {
		return this.symbol;
	}
}