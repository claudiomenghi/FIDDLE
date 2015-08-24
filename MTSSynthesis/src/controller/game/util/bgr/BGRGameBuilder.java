package controller.game.util.bgr;

import java.util.HashSet;
import java.util.Set;

import ontroller.game.bgr.BGRGame;
import ontroller.game.bgr.BGRGoal;
import ac.ic.doc.mtstools.model.MTS;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.FluentImpl;
import ar.dc.uba.model.condition.FluentPropositionalVariable;
import ar.dc.uba.model.condition.Formula;
import ar.dc.uba.model.language.SingleSymbol;
import ar.dc.uba.model.language.Symbol;
import controller.game.model.Assumptions;
import controller.game.model.Guarantees;
import controller.game.util.FluentStateValuation;
import controller.game.util.GRGameBuilder;
import controller.model.gr.GRControllerGoal;

public class BGRGameBuilder<State, Action> extends GRGameBuilder<State, Action> {

	private MTS<State, Action> env;
	private GRControllerGoal<Action> grGoal;
	private Action envYields;
	private Action contYields;


	public BGRGameBuilder(MTS<State, Action> env, GRControllerGoal<Action> goal, Action envYields, Action contYields) {
		this.env = env;
		this.grGoal = goal;
		this.envYields = envYields;
		this.contYields = contYields;
	}

	// TODO MTS to LTS
	public BGRGame<State> buildBGRGame() {

		this.validateActions(this.env, this.grGoal);
		Set<State> initialStates = new HashSet<State>();
		initialStates.add(this.env.getInitialState());
		BGRGame<State> game = new BGRGame<State>(initialStates, this.env.getStates(), this.buildBGRGoal());

		// TODO initialize successors and predecessors.

		return game;
	}

	private BGRGoal<State> buildBGRGoal() {
		Assumptions<State> assumptions = new Assumptions<State>();
		Guarantees<State> guarantees = new Guarantees<State>();
		Set<State> failures = new HashSet<State>();
		Set<State> buchi = new HashSet<State>();

		FluentStateValuation<State> valuation = super.buildGoalComponents(this.env, this.grGoal, assumptions, guarantees, failures);
		//TODO symbol should be parametrised. 
		Symbol envY = new SingleSymbol(this.envYields.toString());
		Symbol contY = new SingleSymbol(this.contYields.toString());
		
		//Builds buchi formula. 
		Fluent buchiFluent = new FluentImpl("", envY, contY, false);
		Formula buchiGoal = new FluentPropositionalVariable(buchiFluent);

		this.buildGoalComponents(this.env, this.grGoal, assumptions, guarantees, failures);
//		this.formulaToStateSet(buchi, this.env.getStates(), buchiGoal, valuation);
		BGRGoal<State> bgrGoal = new BGRGoal<State>(guarantees, assumptions, failures, buchi);
		return bgrGoal;

	}
	
	
	@Override
	public void validateActions(MTS<State, Action> mts, GRControllerGoal<Action> goal) {
		super.validateActions(mts, goal);
		//add validations for buchi. 
		
	}
}
