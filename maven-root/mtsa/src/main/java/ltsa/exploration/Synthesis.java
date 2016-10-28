package ltsa.exploration;

import MTSSynthesis.controller.model.gr.GRControllerGoal;
import ltsa.dispatcher.TransitionSystemDispatcher;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.ui.EmptyLTSOuput;

import java.util.*;

public class Synthesis {
	private CompositeState composition;

	// region Constructor
	public Synthesis(LabelledTransitionSystem[] mts, GRControllerGoal<String> goal_original) {
		// Validate goal
		GRControllerGoal<String> goal = goal_original.copy();
		for (String anAction : goal.getControllableActions())
			if (anAction.contains("["))
				throw new UnsupportedOperationException("Goal corrupted");

		// Classify actions
		ArrayList<String> controllableActions = new ArrayList<>();
		for (String anAction : goal.getControllableActions())
			controllableActions.add(anAction);

		ArrayList<String> uncontrollableActions = new ArrayList<>();
		for (int i = 1; i < mts.length; i++)
			for (String anAction : mts[i].getAlphabet())
				if (!anAction.contains("?") && !anAction.contains("tau")
						&& !controllableActions.contains(anAction)
						&& !uncontrollableActions.contains(anAction))
					uncontrollableActions.add(anAction);

		// Order in goal and mts
		Boolean needOrder = uncontrollableActions.size() > 0;
		if (needOrder) {
			controllableActions.add("wait");
			Set<String> goalActions = goal.getControllableActions();
			goalActions.add("wait");
			goal.setControllableActions(goalActions);

			mts[0].setAlphabet(Arrays.copyOf(
					mts[0].getAlphabet(), mts[0]
							.getAlphabet().length + 2));
			mts[0].getAlphabet()[mts[0].getAlphabet().length - 2]="wait";
			mts[0].getAlphabet()[mts[0].getAlphabet().length - 1]="wait?";
			for (int i = 0; i < mts[0].getStates().length; i++)
				if (mts[0].getStates()[i] != null)
					mts[0].getStates()[i].setList(new LTSTransitionList(mts[0]
							.getAlphabet().length - 2, i));
		}

		ArrayList<String> allActions = new ArrayList<>(controllableActions);
		allActions.addAll(uncontrollableActions);

		// Create composition
		Vector<LabelledTransitionSystem> compositionMachines = new Vector<>();
		Collections.addAll(compositionMachines, mts);

		// Alpha stop
		LTSTransitionList[] alphaStopStates = new LTSTransitionList[1];
		LabelledTransitionSystem alphaStop = mts[0].myclone();
		alphaStop.setStates(alphaStopStates);

		// Composition
		this.composition = new CompositeState(compositionMachines);
		this.composition.goal = goal;
		this.composition.makeController = true;
		this.composition.setCompositionType(47);
		this.composition.alphaStop = alphaStop;
		this.composition.priorityIsLow = true;

		// Order in referee
		if (needOrder) {
			LabelledTransitionSystem order = new LabelledTransitionSystem("ORDER");
			order.setAlphabet(Arrays.copyOf(
					allActions.toArray(new String[allActions.size()]),
					allActions.size()));
			order.setStates(new LTSTransitionList[order.getMaxStates()]);

			// Controllable actions
			for (int stateNumber = 0; stateNumber < order.getMaxStates() - 1; stateNumber++) {
				for (int i = 0; i < order.getAlphabet().length; i++)
					if (order.getAlphabet()[i]
							.equals(controllableActions.get(0)))
						order.getStates()[stateNumber] = new LTSTransitionList(i,
								stateNumber + 1);

				for (int i = 1; i < controllableActions.size(); i++)
					for (int j = 0; j < order.getAlphabet().length; j++)
						if (order.getAlphabet()[j]
								.equals(controllableActions.get(i)))
							order.getStates()[stateNumber].setList(new LTSTransitionList(j,
									stateNumber + 1));
			}

			// Uncontrollable actions
			for (int i = 0; i < order.getAlphabet().length; i++)
				if (order.getAlphabet()[i]
						.equals(uncontrollableActions.get(0)))
					order.getStates()[order.getMaxStates() - 1] = new LTSTransitionList(i,
							0);

			for (int i = 1; i < uncontrollableActions.size(); i++)
				for (int j = 0; j < order.getAlphabet().length; j++)
					if (order.getAlphabet()[j]
							.equals(uncontrollableActions.get(i)))
						order.getStates()[order.getMaxStates() - 1]
								.setList(new LTSTransitionList(j, 0));

			this.composition.getMachines().add(order);
		}

		// Sinthesis
		TransitionSystemDispatcher.applyComposition(this.composition,
				new EmptyLTSOuput());
	}

	// endregion

	// region Public methods
	public CompositeState getComposition() {
		return composition;
	}


	public HashSet<String> getControllerAvailableActions() {
		LabelledTransitionSystem machine = this.composition.getComposition();
		HashSet<LTSTransitionList> states = new HashSet<>();
		states.add(machine.getStates()[0]);

		int oldStatesCount = -1;
		int newStatesCount = -2;
		while (oldStatesCount != newStatesCount) {
			oldStatesCount = states.size();
			for (LTSTransitionList aState : states) {
				if (machine.getAlphabet()[aState.getEvent()].contains("["))
					states.add(machine.getStates()[aState.getNext()]);
				LTSTransitionList list = aState.getList();
				while (list != null) {
					if (machine.getAlphabet()[list.getEvent()]
							.contains("["))
						states.add(machine.getStates()[list.getNext()]);
					list = list.getList();
				}
			}
			newStatesCount = states.size();
		}

		HashSet<String> actions = new HashSet<>();
		for (LTSTransitionList anEventState : states)
			for (int anEvent : anEventState.getEvents())
				if (!machine.getAlphabet()[anEvent].contains("["))
					actions.add(machine.getAlphabet()[anEvent]
							.replace("?", ""));
		return actions;
	}
	// endregion

}
