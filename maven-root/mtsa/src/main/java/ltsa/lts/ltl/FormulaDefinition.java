package ltsa.lts.ltl;

import java.util.Hashtable;
import java.util.Vector;

import com.google.common.base.Preconditions;

import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltscomposition.CompositeState;
import ltsa.lts.parser.LabelSet;
import ltsa.lts.parser.Symbol;

public class FormulaDefinition {

	private Symbol name;
	FormulaSyntax ltlFormula;
	CompositeState cached;
	LabelSet alphaExtension;
	

	Hashtable initParams; // initial parameter values name,value
	Vector params; // list of parameter names
	FormulaFactory fac;

	/**
	 * 
	 * @param name
	 * @param ltlFormula
	 * @param ls
	 * @param ip
	 * @param p
	 */
	public FormulaDefinition(Symbol name, FormulaSyntax ltlFormula,
			LabelSet ls, Hashtable ip, Vector p) {
		Preconditions.checkNotNull(name,
				"The name of the formula cannot be null");
		Preconditions.checkNotNull(ltlFormula,
				"The syntax of the formula cannot be null");
		Preconditions.checkNotNull(ip, "The ip table cannot be null");
		Preconditions.checkNotNull(p, "The p table cannot be null");

		this.name = name;
		this.ltlFormula = ltlFormula;
		cached = null;
		alphaExtension = ls;
		initParams = ip;
		params = p;
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

	public Vector getParams() {
		return params;
	}

	public Hashtable getInitialParams() {
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
	public Symbol getName() {
		return name;
	}
}