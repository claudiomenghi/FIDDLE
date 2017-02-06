package ltsa.lts.ltl;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ltsa.lts.Diagnostics;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.parser.actions.ActionLabels;

/**
 * abstract syntax tree for unexpanded (i.e. includes forall ) LTL formlae.
 *
 */
public class FormulaSyntax {
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	FormulaSyntax left, right;
	Symbol operator;
	Symbol proposition;
	ActionLabels range;
	ActionLabels action;
	Vector<Stack<Symbol>> parameters; // overloaded with Stack for "when expr"

	private FormulaSyntax(FormulaSyntax lt, Symbol op, FormulaSyntax rt, Symbol prop, ActionLabels r, ActionLabels a,
			Vector<Stack<Symbol>> v) {
		left = lt;
		right = rt;
		operator = op;
		proposition = prop;
		range = r;
		action = a;
		parameters = v;
	}

	public static FormulaSyntax make(FormulaSyntax lt, Symbol op, FormulaSyntax rt) {
		return new FormulaSyntax(lt, op, rt, null, null, null, null);
	}

	public static FormulaSyntax make(Symbol prop) {
		return new FormulaSyntax(null, null, null, prop, null, null, null);
	}

	public static FormulaSyntax make(Symbol prop, ActionLabels r) {
		return new FormulaSyntax(null, null, null, prop, r, null, null);
	}

	public static FormulaSyntax make(Symbol prop, Vector<Stack<Symbol>> v) {
		return new FormulaSyntax(null, null, null, prop, null, null, v);
	}

	public static FormulaSyntax makeE(Symbol op, Stack<Stack<Symbol>> v) {
		return new FormulaSyntax(null, op, null, null, null, null, v);
	}

	public static FormulaSyntax make(ActionLabels a) {
		return new FormulaSyntax(null, null, null, null, null, a, null);
	}

	public static FormulaSyntax make(Symbol op, ActionLabels r, FormulaSyntax rt) {
		return new FormulaSyntax(null, op, rt, null, r, null, null);
	}

	public Formula expand(FormulaFactory fac, Hashtable<String, Value> locals, Hashtable<String, Value> globals) {
		if (proposition != null) {
			if (range == null) {
				if (PredicateDefinition.definitions != null
						&& PredicateDefinition.definitions.containsKey(proposition.toString()))
					return fac.make(proposition);
				else {
				
					AssertDefinition p = AssertDefinition.definitions.get(proposition.toString());
					if (p == null){
						Diagnostics.fatal("LTL fluent or assertion not defined: " + proposition, proposition);
					}
					if (parameters == null)
						return p.getLTLFormula().expand(fac, locals, p.initParams);
					else {
						if (parameters.size() != p.params.size())
							Diagnostics.fatal("Actual parameters do not match formals: " + proposition, proposition);

						Hashtable<String, Value> actual_params = new Hashtable<>();
						Vector<Value> values = paramValues(parameters, locals, globals);
						for (int i = 0; i < parameters.size(); ++i) {
							actual_params.put(p.params.elementAt(i), values.elementAt(i));
						}
						return p.getLTLFormula().expand(fac, locals, actual_params);
					}
				}
			} else {
				return fac.make(proposition, range, locals, globals);
			}
		} else if (action != null) {
			return fac.make(action, locals, globals);
		} else if (operator != null && range == null) {
			if (left == null) {
				return fac.make(null, operator, right.expand(fac, locals, globals));
			} else {
				return fac.make(left.expand(fac, locals, globals), operator, right.expand(fac, locals, globals));
			}
		} else if (range != null && right != null) {
			range.initContext(locals, globals);
			Formula f = null;
			while (range.hasMoreNames()) {
				range.nextName();
				if (f == null)
					f = right.expand(fac, locals, globals);
				else {
					if (operator.kind == Symbol.AND)
						f = fac.makeAnd(f, right.expand(fac, locals, globals));
					else
						f = fac.makeOr(f, right.expand(fac, locals, globals));
				}
			}
			range.clearContext();
			return f;
		}
		return null;
	}

	private Vector<Value> paramValues(Vector<Stack<Symbol>> paramExprs, Hashtable<String, Value> locals,
			Hashtable<String, Value> globals) {
		if (paramExprs == null)
			return null;
		Enumeration<Stack<Symbol>> e = paramExprs.elements();
		Vector<Value> v = new Vector<>();
		while (e.hasMoreElements()) {
			Stack<Symbol> stk = e.nextElement();
			v.addElement(Expression.getValue(stk, locals, globals));
		}
		return v;
	}

	public boolean isPropositionalLogic() {
		if (this.proposition != null) {
			return true;
		}
		if (this.operator != null) {
			int opKind = this.operator.kind;

			if (opKind == Symbol.PLING) {
				// unary operator. The parameter (the formula) is on the right
				// side.
				return this.right.isPropositionalLogic();
			} else if (opKind == Symbol.AND || opKind == Symbol.OR || opKind == Symbol.EQUIVALENT
					|| opKind == Symbol.ARROW) {
				return this.right.isPropositionalLogic() && this.left.isPropositionalLogic();
			} else {
				return false;
			}
		} else if (this.action != null) {
			return true;
		} else {
			Diagnostics.fatal("Malformed Formula");
			return false;
		}

	}

	/**
	 * Removes the first ALWAYS symbol if there is one.
	 * 
	 * @return
	 */
	public FormulaSyntax removeLeftTemporalOperators() {
		// ([]A && []B && []C)
		FormulaSyntax answer = this;
		if (this.operator != null) {
			if (this.operator.kind == Symbol.ALWAYS) {
				answer = this.right;
			} else if (this.operator.kind == Symbol.AND) {
				answer = new FormulaSyntax(this.left.removeLeftTemporalOperators(), this.operator,
						this.right.removeLeftTemporalOperators(), this.proposition, this.range, this.action,
						this.parameters);
			}
		}
		return answer;
	}
}