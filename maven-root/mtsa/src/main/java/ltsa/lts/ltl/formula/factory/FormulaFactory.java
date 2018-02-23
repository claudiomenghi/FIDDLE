package ltsa.lts.ltl.formula.factory;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.lts.Alphabet;
import ltsa.lts.ltl.formula.And;
import ltsa.lts.ltl.formula.False;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.Next;
import ltsa.lts.ltl.formula.Not;
import ltsa.lts.ltl.formula.Or;
import ltsa.lts.ltl.formula.Proposition;
import ltsa.lts.ltl.formula.Release;
import ltsa.lts.ltl.formula.True;
import ltsa.lts.ltl.formula.Until;
import ltsa.lts.ltl.visitors.NotVisitor;
import ltsa.lts.ltl.visitors.UntilVisitor;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.parser.actions.ActionLabels;

import com.google.common.base.Preconditions;

/**
 * This factory is used to create LTL formulae. Use the methods makeAlways,
 * makeNot etc to create the desired formula
 *
 */
public class FormulaFactory {

	/**
	 * contains the id of the formula
	 */
	private int id;

	/**
	 * stores sub-formulae to ensure uniqueness
	 */
	private Map<String, Formula> subf;

	/**
	 * stores the set of propositions
	 */
	private SortedSet<Proposition> props;

	/**
	 * The formula to which the factory is associated. Use the method setFormula
	 * to change the current formula associated with the factory
	 */
	private Formula formula;
	private Hashtable<String, Vector<String>> actionPredicates;
	private boolean hasNext = false;
	private static boolean normalLTL = true;

	public FormulaFactory() {
		subf = new HashMap<>();
		props = new TreeSet<>();
		actionPredicates = new Hashtable<>();
	}

	public FormulaFactory(Hashtable<String, Vector<String>> actionPredicates) {
		this();
		Preconditions.checkNotNull(actionPredicates,
				"The actionPredicate cannot be null");

		this.actionPredicates = actionPredicates;
	}

	public Hashtable<String, Vector<String>> getActionPredicates() {
		return actionPredicates;
	}

	boolean nextInFormula() {
		return hasNext;
	}

	/**
	 * sets the formula
	 * 
	 * @param f
	 *            the formula to be considered
	 * @throws NullPointerException
	 *             if the formula is null
	 */
	public void setFormula(Formula f) {
		Preconditions.checkNotNull(f,
				"The formula to be considered cannot be null");

		formula = f;
		f.getSubformulae().forEach(
				currentFormula -> Math.max(this.id, currentFormula.getId()));

		this.props = new TreeSet<>(f.getPropositions());
		for (Proposition p : this.props) {
			this.id = Math.max(this.id, p.getId()+1);
		}
	}

	/**
	 * negates the formula and set the formula
	 * 
	 * @param f
	 *            the formula to be considered
	 * @throws NullPointerException
	 *             if the formula is null
	 */
	public void negateAndSetFormula(Formula f) {
		Preconditions.checkNotNull(f,
				"The formula to be considered cannot be null");
		formula = makeNot(f);
	}

	public Formula getFormula() {
		return formula;
	}

	public Formula makeProposition(Symbol symbol) {
		return unique(new Proposition(symbol));
	}

	public Formula make(Symbol symbol) {
		return unique(new Proposition(symbol));
	}

	public Formula make(Symbol sym, ActionLabels range,
			Hashtable<String, Value> locals, Hashtable<String, Value> globals) {
		range.initContext(locals, globals);
		Formula f = null;
		while (range.hasMoreNames()) {
			String s = range.nextName();
			Symbol newSym = new Symbol(sym, sym + "." + s);
			if (f == null)
				f = make(newSym);
			else
				f = makeOr(f, make(newSym));
		}
		range.clearContext();
		return f;
	}

	public Formula make(Stack<Symbol> expr, Hashtable<String, Value> locals,
			Hashtable<String, Value> globals) {
		if (Expression.evaluate(expr, locals, globals).intValue() > 0)
			return True.make();
		else
			return False.make();
	}

	public Formula make(ActionLabels act, Hashtable<String, Value> locals,
			Hashtable<String, Value> globals) {
		if (actionPredicates == null) {
			actionPredicates = new Hashtable<>();
		}
		Vector<String> av = act.getActions(locals, globals);
		String name = (new Alphabet(av)).toString();
		if (!actionPredicates.containsKey(name)) {
			actionPredicates.put(name, av);
		}
		return unique(new Proposition(new Symbol(Symbol.UPPERIDENT, name)));
	}

	public Formula makeTick() {
		if (actionPredicates == null) {
			actionPredicates = new Hashtable<>();
		}
		Vector<String> av = new Vector<>(1);
		av.add("tick");
		String name = (new Alphabet(av)).toString();
		if (!actionPredicates.containsKey(name)) {
			actionPredicates.put(name, av);
		}
		return unique(new Proposition(new Symbol(Symbol.UPPERIDENT, name)));
	}

	/**
	 * returns the propositions of the formula created using the factory
	 * 
	 * @return the propositions of the formula created using the factory
	 */
	public SortedSet<Proposition> getPropositions() {
		return props;
	}

	public Formula make(Formula left, Symbol op, Formula right) {
		switch (op.kind) {
		case Symbol.PLING:
			return makeNot(right);
		case Symbol.NEXTTIME:
			if (normalLTL)
				return makeNext(right);
			else
				return makeNext(makeWeakUntil(makeNot(makeTick()),
						makeAnd(makeTick(), right)));
		case Symbol.EVENTUALLY:
			if (normalLTL)
				return makeEventually(right);
			else
				return makeEventually(makeAnd(makeTick(), right));
		case Symbol.ALWAYS:
			if (normalLTL)
				return makeAlways(right);
			else
				return makeAlways(makeImplies(makeTick(), right));
		case Symbol.AND:
			return makeAnd(left, right);
		case Symbol.OR:
			return makeOr(left, right);
		case Symbol.ARROW:
			return makeImplies(left, right);
		case Symbol.UNTIL:
			if (normalLTL)
				return makeUntil(left, right);
			else
				return makeUntil(makeImplies(makeTick(), left),
						makeAnd(makeTick(), right));
		case Symbol.WEAKUNTIL:
			if (normalLTL)
				return makeWeakUntil(left, right);
			else
				return makeWeakUntil(makeImplies(makeTick(), left),
						makeAnd(makeTick(), right));
		case Symbol.EQUIVALENT:
			return makeEquivalent(left, right);
		default:
			Diagnostics.fatal("Unexpected operator in LTL expression: " + op,
					op);
		}
		return null;
	}

	public Formula makeAnd(Formula left, Formula right) {
		if (left == right) {
			return left;
		}
		if (left == False.make() || right == False.make())
			return False.make(); // P/\false
		if (left == True.make())
			return right;
		if (right == True.make())
			return left;
		if (left == makeNot(right))
			return False.make(); // contradiction
		if ((left instanceof Next) && (right instanceof Next)) // X a && X b -->
																// X(a && b)
			return makeNext(makeAnd(((Next) left).getNext(),
					((Next) right).getNext()));
		if (left.compareTo(right) < 0)
			return unique(new And(left, right));
		else
			return unique(new And(right, left));
	}

	/**
	 * given a set of Formulae generates the and combination of the formulae
	 * 
	 * @param formulae
	 *            the formulae to be and combined
	 * @return a formula which is the and combination of the formulae passed as
	 *         parameters
	 * @throws NullPointerException
	 *             if one of the formulae is null
	 */
	public Formula makeAnd(Formula... formulae) {
		Formula ret = formulae[0];
		for (int i = 1; i < formulae.length; i++) {
			Preconditions.checkNotNull(formulae[i],
					"You cannot generate an and formula from a null formula");
			ret = this.makeAnd(ret, formulae[i]);
		}

		return ret;
	}

	public Formula makeOr(Formula left, Formula right) {
		if (left == right)
			return left; // P\/P
		if (left == True.make() || right == True.make())
			return True.make(); // P\/true
		if (left == False.make())
			return right;
		if (right == False.make())
			return left;
		if (left == makeNot(right))
			return True.make(); // tautology
		if (left.compareTo(right) < 0)
			return unique(new Or(left, right));
		else
			return unique(new Or(right, left));
	}

	public Formula makeUntil(Formula left, Formula right) {
		if (right == False.make()) {
			return False.make(); // P U false = false
		}
		if ((left instanceof Next) && (right instanceof Next)) { // X a U X b
																	// -->
																	// X(a U b)
			return makeNext(makeUntil(((Next) left).getNext(),
					((Next) right).getNext()));
		}
		return unique(new Until(left, right));
	}

	public Formula makeWeakUntil(Formula left, Formula right) {
		return makeRelease(right, makeOr(left, right));
	}

	public Formula makeRelease(Formula left, Formula right) {
		return unique(new Release(left, right));
	}

	public Formula makeImplies(Formula left, Formula right) {
		return makeOr(makeNot(left), right);
	}

	public Formula makeEquivalent(Formula left, Formula right) {
		return makeAnd(makeImplies(left, right), makeImplies(right, left));
	}

	public Formula makeEventually(Formula right) {
		return makeUntil(True.make(), right);
	}

	public Formula makeAlways(Formula right) {
		return makeNot(makeEventually(makeNot(right)));

	}

	public Formula makeNot(Formula right) {
		return right.accept(new NotVisitor(this));
	}

	public Formula makeNot(Proposition p) {
		return unique(new Not(p));
	}

	public Formula makeNext(Formula right) {
		hasNext = true;
		return unique(new Next(right));
	}

	/**
	 * returns the number of until in the formula. The until sub-formulae are
	 * added to the until parameter.
	 * 
	 * @param f
	 *            the formula to be considered
	 * @param untils
	 *            the list where the until formulae are added
	 * @return the number of until present in the formula
	 */
	public int processUntils(Formula f, List<Formula> untils) {
		f.accept(new UntilVisitor(untils));
		return untils.size();
	}

	public boolean specialCaseV(Formula f, Set<Formula> s) {
		Formula ff = makeRelease(False.make(), f);
		return s.contains(ff);
	}

	public boolean syntaxImplied(Formula f, SortedSet<Formula> one,
			SortedSet<Formula> two) {
		if (f == null)
			return true;
		if (f instanceof True)
			return true;
		if (one.contains(f))
			return true;
		if (f.isLiteral())
			return false;
		Formula a = f.getSub1();
		Formula b = f.getSub2();
		Formula c = ((f instanceof Until) || (f instanceof Release)) ? f : null;
		boolean bf = syntaxImplied(b, one, two);
		boolean af = syntaxImplied(a, one, two);
		boolean cf;
		if (c != null) {
			if (two != null)
				cf = two.contains(c);
			else
				cf = false;
		} else
			cf = true;
		if ((f instanceof Until) || (f instanceof Or))
			return bf || af && cf;
		if (f instanceof Release)
			return af && bf || af && cf;
		if (f instanceof And)
			return af && bf;
		if (f instanceof Next) {
			if (a != null) {
				if (two != null)
					return two.contains(a);
				else
					return false;
			} else {
				return true;
			}
		}
		return false;
	}

	private int newId() {
		return ++id;
	}

	private Formula unique(Formula f) {
		String s = f.toString();
		if (subf.containsKey(s)) {
			return subf.get(s);
		} else {
			f.setId(newId());
			subf.put(s, f);
			if (f instanceof Proposition) {
				props.add((Proposition) f);
			}
			return f;
		}
	}

	public static void setNormalLTL(boolean value) {
		normalLTL = value;
	}
}
