package ltsa.lts.ltl;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;

import com.google.common.base.Preconditions;

import ltsa.lts.Diagnostics;
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
import ltsa.lts.ltl.visitors.FormulaVisitor;
import ltsa.lts.lts.Alphabet;
import ltsa.lts.parser.ActionLabels;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;

/*
 * factory for LTL formlae
 */

public class FormulaFactory {

	NotVisitor nv;
	int id;
	Map<String, Formula> subf; // stores subformula to ensure uniqueness
	SortedSet<Proposition> props; // stores the set of propositions
	Formula formula;
	private Hashtable<String, Vector<String>> actionPredicates;
	private boolean hasNext = false;
	static public boolean normalLTL = true;

	public FormulaFactory() {
		nv = new NotVisitor(this);
		subf = new HashMap();
		props = new TreeSet();
		id = 1;
		actionPredicates = null;
	}
	
	public FormulaFactory(Hashtable<String, Vector<String>> actionPredicates) {
		nv = new NotVisitor(this);
		subf = new HashMap();
		props = new TreeSet();
		id = 1;
		this.actionPredicates = actionPredicates;
	}

	
	public Hashtable<String, Vector<String>> getActionPredicates(){
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
		this.props = new TreeSet<>(f.getPropositions());
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

	public Formula make(Symbol sym) {
		return unique(new Proposition(sym));
	}

	public Formula make(Symbol sym, ActionLabels range, Hashtable locals,
			Hashtable globals) {
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

	public Formula make(Stack expr, Hashtable locals, Hashtable globals) {
		if (Expression.evaluate(expr, locals, globals).intValue() > 0)
			return True.make();
		else
			return False.make();
	}

	public Formula make(ActionLabels act, Hashtable locals, Hashtable globals) {
		if (actionPredicates == null){
			actionPredicates = new Hashtable<>();
		}
		Vector<String> av = act.getActions(locals, globals);
		String name = (new Alphabet(av)).toString();
		if (!actionPredicates.containsKey(name)){
			actionPredicates.put(name, av);
		}
		return unique(new Proposition(new Symbol(Symbol.UPPERIDENT, name)));
	}

	public Formula makeTick() {
		if (actionPredicates == null){
			actionPredicates = new Hashtable();
		}
		Vector av = new Vector(1);
		av.add("tick");
		String name = (new Alphabet(av)).toString();
		if (!actionPredicates.containsKey(name)){
			actionPredicates.put(name, av);
		}
		return unique(new Proposition(new Symbol(Symbol.UPPERIDENT, name)));
	}

	public SortedSet<Proposition> getProps() {
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
		if (left == right)
			return left; // P/\P
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
		if (right == False.make())
			return False.make(); // P U false = false
		if ((left instanceof Next) && (right instanceof Next)) // X a U X b -->
																// X(a U b)
			return makeNext(makeUntil(((Next) left).getNext(),
					((Next) right).getNext()));
		return unique(new Until(left, right));
	}

	Formula makeWeakUntil(Formula left, Formula right) {
		return makeRelease(right, makeOr(left, right));
	}

	Formula makeRelease(Formula left, Formula right) {
		return unique(new Release(left, right));
	}

	Formula makeImplies(Formula left, Formula right) {
		return makeOr(makeNot(left), right);
	}

	Formula makeEquivalent(Formula left, Formula right) {
		return makeAnd(makeImplies(left, right), makeImplies(right, left));
	}

	Formula makeEventually(Formula right) {
		return makeUntil(True.make(), right);
	}

	Formula makeAlways(Formula right) {
		return makeRelease(False.make(), right);
	}

	public Formula makeNot(Formula right) {
		return right.accept(nv);
	}

	public Formula makeNot(Proposition p) {
		return unique(new Not(p));
	}

	public Formula makeNext(Formula right) {
		hasNext = true;
		return unique(new Next(right));
	}

	int processUntils(Formula f, List untils) {
		f.accept(new UntilVisitor(this, untils));
		return untils.size();
	}

	boolean specialCaseV(Formula f, Set s) {
		Formula ff = makeRelease(False.make(), f);
		return s.contains(ff);
	}

	boolean syntaxImplied(Formula f, SortedSet one, SortedSet two) {
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
		if (subf.containsKey(s)){
			return subf.get(s);
		}
		else {
			f.setId(newId());
			subf.put(s, f);
			if (f instanceof Proposition){
				props.add((Proposition) f);
			}
			return f;
		}
	}
}

/*
 * Not visitor pushes negation inside operators to get negative normal form
 */

class NotVisitor implements FormulaVisitor {
	private FormulaFactory fac;

	NotVisitor(FormulaFactory f) {
		fac = f;
	}

	@Override
	public Formula visit(True t) {
		return False.make();
	}

	@Override
	public Formula visit(False f) {
		return True.make();
	}

	@Override
	public Formula visit(Proposition p) {
		return fac.makeNot(p);
	}

	@Override
	public Formula visit(Not n) {
		return n.getNext();
	}

	@Override
	public Formula visit(Next n) {
		return fac.makeNext(fac.makeNot(n.getNext()));
	}

	@Override
	public Formula visit(And a) {
		return fac.makeOr(fac.makeNot(a.getLeft()), fac.makeNot(a.getRight()));
	}

	@Override
	public Formula visit(Or o) {
		return fac.makeAnd(fac.makeNot(o.getLeft()), fac.makeNot(o.getRight()));
	}

	@Override
	public Formula visit(Until u) {
		return fac.makeRelease(fac.makeNot(u.getLeft()),
				fac.makeNot(u.getRight()));
	}

	@Override
	public Formula visit(Release r) {
		return fac.makeUntil(fac.makeNot(r.getLeft()),
				fac.makeNot(r.getRight()));
	}
}

/*
 * Untils visitor computes the untils indexes
 */

class UntilVisitor implements FormulaVisitor {
	private FormulaFactory fac;
	private List ll;

	UntilVisitor(FormulaFactory f, List l) {
		fac = f;
		ll = l;
	}

	@Override
	public Formula visit(True t) {
		return t;
	}

	@Override
	public Formula visit(False f) {
		return f;
	}

	@Override
	public Formula visit(Proposition p) {
		return p;
	}

	@Override
	public Formula visit(Not n) {
		n.getNext().accept(this);
		return n;
	}

	@Override
	public Formula visit(Next n) {
		n.getNext().accept(this);
		return n;
	}

	@Override
	public Formula visit(And a) {
		a.getLeft().accept(this);
		a.getRight().accept(this);
		return a;
	}

	@Override
	public Formula visit(Or o) {
		o.getLeft().accept(this);
		o.getRight().accept(this);
		return o;
	}

	@Override
	public Formula visit(Until u) {
		if (!u.visited()) {
			u.setVisited();
			ll.add(u);
			u.setUI(ll.size() - 1);
			u.getRight().setRofUI(ll.size() - 1);
			u.getLeft().accept(this);
			u.getRight().accept(this);
		}
		return u;
	}

	@Override
	public Formula visit(Release r) {
		r.getLeft().accept(this);
		r.getRight().accept(this);
		return r;
	}
}
