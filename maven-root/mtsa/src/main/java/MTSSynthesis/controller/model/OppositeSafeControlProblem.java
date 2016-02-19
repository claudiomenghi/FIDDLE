package MTSSynthesis.controller.model;

import MTSTools.ac.ic.doc.mtstools.model.LTS;
import MTSSynthesis.controller.game.model.GameSolver;
import MTSSynthesis.controller.game.model.OppositeSafeGameSolver;
import MTSSynthesis.controller.model.gr.GRControllerGoal;

public class OppositeSafeControlProblem<S, A> extends
		StateSpaceCuttingControlProblem<S, A> {

	public OppositeSafeControlProblem(LTS<S, A> env,
			GRControllerGoal<A> grControllerGoal) {
		super(env, grControllerGoal);
	}

	@Override
	protected GameSolver<S, Integer> buildGameSolver() {
		// TODO Auto-generated method stub
		return new OppositeSafeGameSolver<S, A>(environment,
				grControllerGoal.getControllableActions(), buildGuarantees());
	}

}
