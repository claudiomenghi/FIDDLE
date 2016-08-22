package ltsa.lts.csp;

import java.util.HashSet;
import java.util.Vector;

import ltsa.lts.automata.automaton.StateMachine;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.actions.ActionLabels;

public class BoxStateDefn extends StateDefn {

	private ActionLabels actionInterface;

	public BoxStateDefn(Symbol name) {
		super(name);
	}

	public void setInterface(ActionLabels actionInterface) {
		this.actionInterface = actionInterface;
	}

	public ActionLabels getInterface() {
		return this.actionInterface;
	}

	@Override
	public void transition(StateMachine machine) {

		super.transition(machine);
		this.addLoop(machine);

	}

	private void addLoop(StateMachine machine) {

		int boxIndex = machine.getStateIndex(this.getName().getValue());
		machine.addBoxIndex(this.getName().getValue(), boxIndex);

		Vector<String> actions = actionInterface.getActions(null, null);
		machine.mapBoxInterface.put(this.getName().getValue(),
				new HashSet<String>(actions));
	}
}
