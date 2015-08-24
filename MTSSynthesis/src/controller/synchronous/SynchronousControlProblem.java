package controller.synchronous;

import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.MTS;
import controller.game.gr.StrategyState;
import controller.game.util.bgr.BGRGameBuilder;
import controller.model.gr.GRControllerGoal;

public class SynchronousControlProblem<State, Action> {

	public SynchronousControlProblem(MTS<State, Action> env, GRControllerGoal<Action> goal, Action envYields, Action contYields) {
		BGRGameBuilder<State, Action> builder = new BGRGameBuilder<State, Action>(env, goal, envYields, contYields);
		
	}

	public LTS<StrategyState<State, Integer>, Action> solve() {
		return null;
	}

}
