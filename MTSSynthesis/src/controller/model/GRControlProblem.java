package controller.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.FluentUtils;
import ar.dc.uba.model.condition.Formula;
import controller.game.model.Guarantee;
import controller.game.model.Guarantees;
import controller.game.util.FluentStateValuation;
import controller.model.gr.GRControllerGoal;

public abstract class GRControlProblem<S, A, M> implements ControlProblem<S, A> {

	protected GRControllerGoal<A> grControllerGoal;
	protected LTS<S,A> environment;	
	protected Set<A> controllable;
	protected boolean problemSolved;
	private LTS<S,A> solution;
	
	public GRControlProblem(LTS<S, A> environment,
			GRControllerGoal<A> grControllerGoal){
		this.environment = environment;
		this.grControllerGoal = grControllerGoal;
		this.problemSolved = false;
	}
	
	@Override
	public LTS<S, A> solve() {
		if(!problemSolved){
			solution = primitiveSolve();
		}
		return solution;
	}
	
	protected abstract LTS<S, A> primitiveSolve();
	
	protected Set<Set<S>> buildGuarantees() {
		FluentStateValuation<S> valuation = FluentUtils.getInstance()
				.buildValuation(new MTSAdapter<S, A>(environment), grControllerGoal.getFluents());

		Guarantees<S> guarantees = new Guarantees<S>();
		formulasToGuarantees(guarantees, environment.getStates(), grControllerGoal.getGuarantees(),
				valuation);
		for (Fluent f : grControllerGoal.getFluents())
			for (S s : valuation.getStates())
				System.out.println("s" + s + " f " + f.getName() + " = "
						+ valuation.isTrue(s, f));
		Set<Set<S>> returnSet = new HashSet<Set<S>>();
		for (Guarantee<S> g : guarantees) {
			returnSet.add(g.getStateSet());
		}
		return returnSet; // this contains the marked states, only fluents not a
							// goal formula (even propositional), need to output
							// formula itself into prism and evaluate there
	}	
	
	private void formulasToGuarantees(Guarantees<S> guarantees,
			Set<S> states, List<Formula> formulas,
			FluentStateValuation<S> valuation) {

		for (Formula formula : formulas) {
			Guarantee<S> guarantee = new Guarantee<S>();
			for (S state : states) {
				valuation.setActualState(state);
				if (formula.evaluate(valuation)) {
					guarantee.addState(state);
				}
			}
			if (guarantee.isEmpty()) {
				Logger.getAnonymousLogger().warning(
						"There is no state satisfying formula:" + formula);
			}
			guarantees.addGuarantee(guarantee);
		}

		if (guarantees.isEmpty()) {
			Guarantee<S> trueAssume = new Guarantee<S>();
			trueAssume.addStates(states);
			guarantees.addGuarantee(trueAssume);
		}
	}	
	
}
