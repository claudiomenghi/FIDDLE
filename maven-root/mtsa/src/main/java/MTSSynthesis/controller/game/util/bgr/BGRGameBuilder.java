package MTSSynthesis.controller.game.util.bgr;

import java.util.HashSet;
import java.util.Set;

import MTSSynthesis.ontroller.game.bgr.BGRGame;
import MTSSynthesis.ontroller.game.bgr.BGRGoal;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSSynthesis.ar.dc.uba.model.condition.Fluent;
import MTSSynthesis.ar.dc.uba.model.condition.FluentImpl;
import MTSSynthesis.ar.dc.uba.model.condition.FluentPropositionalVariable;
import MTSSynthesis.ar.dc.uba.model.condition.Formula;
import MTSSynthesis.ar.dc.uba.model.language.SingleSymbol;
import MTSSynthesis.ar.dc.uba.model.language.Symbol;
import MTSSynthesis.controller.game.model.Assumptions;
import MTSSynthesis.controller.game.model.Guarantees;
import MTSSynthesis.controller.game.util.FluentStateValuation;
import MTSSynthesis.controller.game.util.GRGameBuilder;
import MTSSynthesis.controller.model.gr.GRControllerGoal;

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
