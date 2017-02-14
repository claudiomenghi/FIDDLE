package ltsa.lts.ltl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.LogFactory;

import ltsa.lts.Diagnostics;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.parser.actions.ActionLabels;
import ltsa.lts.parser.actions.ActionName;
import ltsa.lts.parser.actions.ActionSet;
import ltsa.lts.parser.actions.LabelSet;

import com.google.common.base.Preconditions;

public class PredicateDefinition {

	/** Logger available to subclasses */
	protected static final org.apache.commons.logging.Log logger = LogFactory.getLog(PredicateDefinition.class);

	private final Symbol symbol;
	private ActionLabels trueSet, falseSet;
	private Vector<String> initiatingActions, terminatingActions;
	Stack<Symbol> expr;
	private boolean initial;
	ActionLabels range; // range of fluents

	/**
	 * maps a string to the corresponding predicate
	 */
	public static HashMap<String, PredicateDefinition> definitions;

	private PredicateDefinition(Symbol symbol, ActionLabels range, ActionLabels trueSet, ActionLabels falseSet,
			Stack<Symbol> expr) {
		this.symbol = symbol;
		this.range = range;
		this.trueSet = trueSet;
		this.falseSet = falseSet;
		this.expr = expr;
		this.initial = false;
	}

	public boolean getInitial() {
		return this.initial;
	}

	public PredicateDefinition(Symbol n, Vector<String> trueActions, Vector<String> falseActions) {
		Preconditions.checkNotNull(n, "The symbol cannot be null");
		Preconditions.checkNotNull(trueActions, "The set of true actions cannot be null");
		Preconditions.checkNotNull(falseActions, "The set of false actions cannot be null");
		this.symbol = n;
		this.initiatingActions = trueActions;
		this.terminatingActions = falseActions;
	}

	public PredicateDefinition(String predicateName, Vector<String> initiatingActions,
			Vector<String> terminatingActions, boolean initialValue) {
		Preconditions.checkNotNull(predicateName, "The name cannot be null");

		Preconditions.checkNotNull(initiatingActions, "The set of true actions cannot be null");
		Preconditions.checkNotNull(terminatingActions, "The set of false actions cannot be null");
		this.symbol = new Symbol(Symbol.UPPERIDENT, predicateName);
		this.initiatingActions = initiatingActions;
		this.terminatingActions = terminatingActions;
		initial = initialValue;
	}

	public int query(String s) {
		if (initiatingActions.contains(s))
			return 1;
		if (terminatingActions.contains(s))
			return -1;
		return 0;
	}

	public Vector<String> getInitiatingActions() {
		return this.initiatingActions;
	}

	public void setInitiatingActions(Vector<String> initiatingActions) {
		this.initiatingActions = initiatingActions;

	}

	public Vector<String> getTerminatingActions() {
		return this.terminatingActions;
	}

	public void setTerminatingActions(Vector<String> terminatingActions) {
		this.terminatingActions = terminatingActions;

	}

	public int initial() {
		return initial ? 1 : -1;
	}

	@Override
	public String toString() {
		return symbol.toString();
	}

	public Vector<String> getTrueActions() {
		return initiatingActions;
	}

	public Vector<String> getFalseActions() {
		return terminatingActions;
	}

	public Symbol getSymbol() {
		return symbol;
	}

	public static PredicateDefinition get(String n) {
		if (definitions == null) {
			return null;
		}
		PredicateDefinition p = definitions.get(n);
		if (p == null) {
			return null;
		}
		if (p.range != null) {
			return null;
		}
		return p;
	}

	public static void compileWithEnd(PredicateDefinition p) {
		if (p == null)
			return;
		if (p.range == null) {
			if (!(p.initiatingActions != null && p.terminatingActions != null && p.trueSet == null
					&& p.falseSet == null)) {

				p.terminatingActions.add("end");
				p.initiatingActions = p.trueSet.getActions(null, null);
				p.terminatingActions = p.falseSet.getActions(null, null);
			}
			assertDisjoint(p.initiatingActions, p.terminatingActions, p);
			if (p.expr != null) {
				int ev = Expression.evaluate(p.expr, null, null).intValue();
				p.initial = (ev > 0);
			}
		} else {
			Hashtable<String, Value> locals = new Hashtable<>();
			p.range.initContext(locals, null);
			while (p.range.hasMoreNames()) {
				String s = p.range.nextName();
				Vector<String> trueActions = p.trueSet.getActions(locals, null);
				Vector<String> falseActions = p.falseSet.getActions(locals, null);
				falseActions.add("end");
				boolean init = false;
				assertDisjoint(trueActions, falseActions, p);
				if (p.expr != null) {
					int ev = Expression.evaluate(p.expr, locals, null).intValue();
					init = (ev > 0);
				}
				String newName = p.symbol + "." + s;
				definitions.put(newName, new PredicateDefinition(newName, trueActions, falseActions, init));
			}
			p.range.clearContext();
		}
	}

	public static void compile(PredicateDefinition p) {
		if (p == null)
			return;
		if (p.range == null) {
			if (!(p.initiatingActions != null && p.terminatingActions != null && p.trueSet == null
					&& p.falseSet == null)) {

				p.initiatingActions = p.trueSet.getActions(null, null);
				p.terminatingActions = p.falseSet.getActions(null, null);
			}
			assertDisjoint(p.initiatingActions, p.terminatingActions, p);
			if (p.expr != null) {
				int ev = Expression.evaluate(p.expr, null, null).intValue();
				p.initial = (ev > 0);
			}
		} else {
			Hashtable<String, Value> locals = new Hashtable<>();
			p.range.initContext(locals, null);
			while (p.range.hasMoreNames()) {
				String s = p.range.nextName();
				Vector<String> trueActions = p.trueSet.getActions(locals, null);
				Vector<String> falseActions = p.falseSet.getActions(locals, null);
				boolean init = false;
				assertDisjoint(trueActions, falseActions, p);
				if (p.expr != null) {
					int ev = Expression.evaluate(p.expr, locals, null).intValue();
					init = (ev > 0);
				}
				String newName = p.symbol + "." + s;
				definitions.put(newName, new PredicateDefinition(newName, trueActions, falseActions, init));
			}
			p.range.clearContext();
		}
	}

	private static void assertDisjoint(Vector<String> PA, Vector<String> NA, PredicateDefinition p) {
		Set<String> s = new TreeSet<>(PA);
		s.retainAll(NA);
		if (!s.isEmpty()) {
			Diagnostics.fatal("Predicate " + p.symbol + " True & False sets must be disjoint", p.symbol);
		}
	}

	public static void put(Symbol n, ActionLabels rng, ActionLabels ts, ActionLabels fs, Stack<Symbol> es) {
		if (definitions == null) {
			definitions = new HashMap<>();
		}
		if (!definitions.containsKey(n.toString())) {
			if (definitions.put(n.toString(), new PredicateDefinition(n, rng, ts, fs, es)) != null) {

				Diagnostics.fatal("duplicate LTL predicate definition: " + n, n);
			}
		}
	}

	public static void remove(Symbol n) {
		definitions.remove(n.toString());
	}

	public static boolean contains(Symbol n) {
		if (definitions == null)
			return false;
		return definitions.containsKey(n.toString());
	}

	public static void init() {
		definitions = null;
	}

	public static void compileAll() {
		if (definitions == null)
			return;
		List<PredicateDefinition> v = new ArrayList<>();
		v.addAll(definitions.values());

		v.forEach(p -> compile(p));
	}

	public static void compileAllWithEnd() {
		if (definitions == null)
			return;
		List<PredicateDefinition> v = new ArrayList<>();
		v.addAll(definitions.values());

		v.forEach(p -> compileWithEnd(p));
	}

	public PredicateDefinition clone() {
		PredicateDefinition def = new PredicateDefinition(this.symbol, new Vector<>(this.initiatingActions),
				new Vector<>(this.terminatingActions));

		def.expr = this.expr;
		def.initial = this.initial;
		def.initiatingActions = this.initiatingActions;
		def.terminatingActions = this.terminatingActions;
		return def;
	}

	static public void makePredicate(LTSOutput output, Symbol fluentName, Symbol predicateSymbol,
			Set<String> alphabetCharacters) {

		Preconditions.checkNotNull(predicateSymbol, "The predicate symbol cannot be null");
		Preconditions.checkNotNull(alphabetCharacters, "The set of the terminating actions cannot be null");

		ActionName initialFluentActions = new ActionName(predicateSymbol);

		Set<String> alp = new HashSet<>(alphabetCharacters);
		alp.remove(predicateSymbol.getValue());

		final Vector<ActionLabels> v = new Vector<>();

		alp.forEach(a -> v.add(new ActionName(new Symbol(a, Symbol.IDENTIFIER))));

		ActionLabels endFluentActions = new ActionSet(new LabelSet(v));

		Stack<Symbol> symbols = new Stack<>();
		symbols.add(new Symbol("False"));
		
		if (PredicateDefinition.contains(fluentName)) {
			PredicateDefinition.remove(fluentName);
		}
		PredicateDefinition.put(fluentName, null, initialFluentActions, endFluentActions, null);
		compile(definitions.get(fluentName.toString()));
	}

}
