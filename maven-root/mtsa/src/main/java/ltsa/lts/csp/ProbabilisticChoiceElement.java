package ltsa.lts.csp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ltsa.lts.lts.ProbabilisticTransition;
import ltsa.lts.lts.StateMachine;
import ltsa.lts.parser.ActionLabels;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;

/* ----------------------------------------------------------------------- */
public class ProbabilisticChoiceElement extends ChoiceElement {
	Map<BigDecimal, List<StateExpr>> probabilisticChoices = new HashMap<>(); // Map<BigDecimal,
																				// StateExpr>
	int bundle = ProbabilisticTransition.NO_BUNDLE;

	public ProbabilisticChoiceElement(ChoiceElement elem) {
		this.action = elem.action;
		this.setGuard(elem.getGuard());
		this.stateExpr = elem.stateExpr;
	}

	public void addProbabilisticChoice(BigDecimal prob, int bundle,
			StateExpr stateEx) {
		this.bundle = bundle;

		List<StateExpr> probTrans = probabilisticChoices.get(prob);
		if (probTrans == null) {
			probTrans = new ArrayList<StateExpr>();
			probabilisticChoices.put(prob, probTrans);
		}
		probTrans.add(stateEx);
	}

	private void add(int from, Hashtable locals, StateMachine m,
			ActionLabels action) {
		action.initContext(locals, m.getConstants());
		while (action.hasMoreNames()) {
			String s = action.nextName();
			Symbol e = new Symbol(Symbol.IDENTIFIER, s);
			if (!m.getAlphabet().contains(s))
				m.addEvent(s);
			endProbabilisticTransition(from, e, locals, m);
		}
		action.clearContext();
	}

	private void endProbabilisticTransition(int from, Symbol e,
			Hashtable locals, StateMachine m) {
		for (Object o1 : probabilisticChoices.entrySet()) {
			Entry entry = (Entry) o1;
			Collection stExCol = (Collection) entry.getValue();
			for (Object o2 : stExCol) {
				StateExpr stEx = (StateExpr) o2;
				// stEx.endTransition(from, e, locals, m);
				stEx.endProbabilisticTransition(from, e, locals, m,
						(BigDecimal) entry.getKey(), bundle);
			}
		}
	}

	private void add(int from, Hashtable locals, StateMachine m, String s) {
		Symbol e = new Symbol(Symbol.IDENTIFIER, s);
		if (!m.getAlphabet().contains(s))
			m.addEvent(s);
		// stateExpr.endTransition(from, e, locals, m);
		for (Object obj : probabilisticChoices.entrySet()) {
			Entry entry = (Entry) obj;
			StateExpr stEx = (StateExpr) entry.getValue();
			stEx.endTransition(from, e, locals, m);
		}
	}

	@Override
	public void addTransition(int from, Hashtable locals, StateMachine m) {
		if (this.getGuard() == null
				|| Expression.evaluate(this.getGuard(), locals, m.getConstants()).intValue() != 0) {
			if (action != null) {
				add(from, locals, m, action);
			}
		}
	}

	@Override
	public ChoiceElement myclone() {
		ChoiceElement ce = new ChoiceElement();
		ce.setGuard(this.getGuard());
		if (action != null)
			ce.action = action.myclone();
		if (stateExpr != null)
			ce.stateExpr = stateExpr.myclone();
		return ce;
	}
}