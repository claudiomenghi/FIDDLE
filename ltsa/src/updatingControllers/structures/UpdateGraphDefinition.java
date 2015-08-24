package updatingControllers.structures;

import lts.Symbol;

import java.util.List;

/**
 * Created by Victor Wjugow on 08/06/15.
 */
public class UpdateGraphDefinition {
	private final String name;
	private Symbol initialProblem;
	private List<Symbol> transitions;

	public UpdateGraphDefinition(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Symbol getInitialProblem() {
		return initialProblem;
	}

	public void setInitialProblem(Symbol initialProblem) {
		this.initialProblem = initialProblem;
	}

	public List<Symbol> getTransitions() {
		return transitions;
	}

	public void setTransitions(List<Symbol> transitions) {
		this.transitions = transitions;
	}
}
