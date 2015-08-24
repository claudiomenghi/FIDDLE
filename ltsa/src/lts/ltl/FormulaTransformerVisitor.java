package lts.ltl;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.Validate;

import ar.dc.uba.model.condition.AndFormula;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.FluentImpl;
import ar.dc.uba.model.condition.FluentPropositionalVariable;
import ar.dc.uba.model.condition.Formula;
import ar.dc.uba.model.condition.NotFormula;
import ar.dc.uba.model.condition.OrFormula;
import ar.dc.uba.model.language.SingleSymbol;
import ar.dc.uba.model.language.Symbol;

/**
 * Visits a formula and stores the resulting 
 * transformed Formula and the Fluents occurring in 
 * the Formula
 * @author gsibay
 *
 */
public class FormulaTransformerVisitor implements FormulaVisitor {

	private ar.dc.uba.model.condition.Formula transformedFormula;
	private Set<Fluent> involvedFluents = new HashSet<Fluent>();
	
	public Set<Fluent> getInvolvedFluents() {
		return this.involvedFluents;
	}
	
	public ar.dc.uba.model.condition.Formula getTransformedFormula() {
		return this.transformedFormula;
	}
	
	/* (non-Javadoc)
	 * @see lts.ltl.FormulaVisitor#visit(lts.ltl.True)
	 */
	@Override
	public lts.ltl.Formula visit(True t) {
		this.transformedFormula = ar.dc.uba.model.condition.Formula.TRUE_FORMULA;
		return null;
	}

	/* (non-Javadoc)
	 * @see lts.ltl.FormulaVisitor#visit(lts.ltl.False)
	 */
	@Override
	public lts.ltl.Formula visit(False f) {
		this.transformedFormula = ar.dc.uba.model.condition.Formula.FALSE_FORMULA;
		return null;
	}
	
	/* (non-Javadoc)
	 * @see lts.ltl.FormulaVisitor#visit(lts.ltl.Proposition)
	 */
	@Override
	public lts.ltl.Formula visit(Proposition p) {
		Fluent fluent = this.createFluent(p);
		this.involvedFluents.add(fluent);
		
		this.transformedFormula = new FluentPropositionalVariable(fluent);
		return null;
	}

	/* (non-Javadoc)
	 * @see lts.ltl.FormulaVisitor#visit(lts.ltl.Not)
	 */
	@Override
	public lts.ltl.Formula visit(Not n) {
		// transform the n formula
		n.getNext().accept(this);
		// negate the formula and set it to the transformer
		this.transformedFormula = new NotFormula(this.getTransformedFormula());
		return null;
	}

	/* (non-Javadoc)
	 * @see lts.ltl.FormulaVisitor#visit(lts.ltl.And)
	 */
	@Override
	public lts.ltl.Formula visit(And a) {
		a.getLeft().accept(this);
		Formula left = this.getTransformedFormula();
		
		a.getRight().accept(this);
		Formula right = this.getTransformedFormula();
		
		this.transformedFormula = new AndFormula(left, right);
		return null;
	}

	/* (non-Javadoc)
	 * @see lts.ltl.FormulaVisitor#visit(lts.ltl.Or)
	 */
	@Override
	public lts.ltl.Formula visit(Or o) {
		o.getLeft().accept(this);
		Formula left = this.getTransformedFormula();
		
		o.getRight().accept(this);
		Formula right = this.getTransformedFormula();
		
		this.transformedFormula = new OrFormula(left, right);
		return null;
	}

	/* (non-Javadoc)
	 * @see lts.ltl.FormulaVisitor#visit(lts.ltl.Until)
	 */
	@Override
	public lts.ltl.Formula visit(Until u) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see lts.ltl.FormulaVisitor#visit(lts.ltl.Release)
	 */
	@Override
	public lts.ltl.Formula visit(Release r) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see lts.ltl.FormulaVisitor#visit(lts.ltl.Next)
	 */
	@Override
	public lts.ltl.Formula visit(Next n) {
		throw new UnsupportedOperationException();		
	}

	private Fluent createFluent(Proposition proposition) {
		String name = proposition.toString();
		PredicateDefinition predicateDefinition = PredicateDefinition.get(name);
		Validate.notNull(predicateDefinition, "Undefined predicate: " + name);
		PredicateDefinition.compile(predicateDefinition);
		
		// if initial is 1 then is true. If it's -1 it's false.
		boolean initialValue = (predicateDefinition.initial() == 1) ? true : false;
		
		Set<Symbol> initiatingActions = this.transformFluentActions(predicateDefinition.getInitiatingActions());
		Set<Symbol> terminatingActions = this.transformFluentActions(predicateDefinition.getTerminatingActions());
		
		return new FluentImpl(name, initiatingActions, terminatingActions, initialValue);
	}
	
	private Set<Symbol> transformFluentActions(Vector fluentActions) {
		Set<Symbol> symbols = new HashSet<Symbol>();
		for (Object fluentAction : fluentActions) {
			// The fluentAction must have a toString method representing the action
			symbols.add(new SingleSymbol(fluentAction.toString()));
		}
		return symbols;
	}
	
}
