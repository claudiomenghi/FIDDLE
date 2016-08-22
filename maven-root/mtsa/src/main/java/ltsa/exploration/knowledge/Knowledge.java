package ltsa.exploration.knowledge;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;

import java.util.*;

public class Knowledge {
	private LabelledTransitionSystem[] components;
	private int[] currentStates;
	private ArrayList<ArrayList<StateEquivalence>> stateEquivalence;

	// region Constructor
	public Knowledge(LabelledTransitionSystem[] components) {
		this.components = new LabelledTransitionSystem[components.length];
		this.currentStates = new int[components.length];
		this.stateEquivalence = new ArrayList<>(components.length);

		for (int i = 0; i < components.length; i++) {
			this.components[i] = components[i];

			this.components[i].setStates(Arrays.copyOf(
					components[i].getStates(),
					components[i].getStates().length + 1));
			this.components[i].getStates()[this.components[i].getStates().length - 1] = LTSTransitionList
					.copy(this.components[i].getStates()[0]);
			this.currentStates[i] = this.components[i].getStates().length - 1;
			this.stateEquivalence.add(new ArrayList<StateEquivalence>(1));
			this.stateEquivalence.get(i).add(
					new StateEquivalence(0,
							this.components[i].getStates().length - 1));
		}
	}

	// endregion

	// Getters
	public LabelledTransitionSystem[] getCmponents() {
		return this.components;
	}

	public int[] getCurrentStates() {
		return this.currentStates;
	}

	// endregion

	// region Public methods
	public void updateKnowledgeFromCurrentStateActions(Integer componentNumber,
			HashSet<String> actions) {
		// List of available possible actions
		HashSet<String> currentStateActions = new HashSet<>(0);
		for (String action : actions)
			currentStateActions.add(action + "?");

		int[] currentStateEvents = this.components[componentNumber].getStates()[this.currentStates[componentNumber]]
				.getEvents();
		for (int anEvent : currentStateEvents) {
			String anAction = this.components[componentNumber].getAlphabet()[anEvent];
			if (anAction.contains("?")
					&& !currentStateActions.contains(anAction))
				this.removeEventFromCurrentState(componentNumber, anEvent);
		}
	}

	public void execute(Integer componentNumber, String nextAction,
			Integer nextViewState, LTSTransitionList modelNewEventState) {
		if (nextAction.equals("WAIT"))
			return;

		Integer fromState = this.currentStates[componentNumber];

		String nextActionPossible = nextAction + "?";
		int nextActionEvent = this.components[componentNumber]
				.getEvent(nextAction);
		int nextActionEventPossible = this.components[componentNumber]
				.getEvent(nextActionPossible);

		this.changeState(componentNumber, modelNewEventState, nextViewState);
		this.conifirmAction(componentNumber, nextActionEvent, nextActionEvent,
				fromState, this.currentStates[componentNumber]);
		this.conifirmAction(componentNumber, nextActionEventPossible,
				nextActionEvent, fromState, this.currentStates[componentNumber]);
	}

	public void reset() {
		this.currentStates[0] = this.getFirstStateNumber(0);
	}

	public LabelledTransitionSystem[] cloneForSynthesisFromStart() {
		LabelledTransitionSystem[] clone = new LabelledTransitionSystem[components.length];
		for (int i = 0; i < clone.length; i++) {
			clone[i] = this.components[i].myclone();
			clone[i].setStates(Arrays.copyOf(clone[i].getStates(),
					clone[i].getMaxStates()));
		}

		for (int i = 0; i < this.components.length; i++)
			clone[i].swapStates(0, this.getFirstStateNumber(i));

		return clone;
	}

	public LabelledTransitionSystem[] cloneForSynthesisFromCurrentState() {
		LabelledTransitionSystem[] clone = new LabelledTransitionSystem[components.length];
		for (int i = 0; i < clone.length; i++) {
			clone[i] = this.components[i].myclone();
			clone[i].setStates(Arrays.copyOf(clone[i].getStates(),
					clone[i].getMaxStates()));
		}

		for (int i = 0; i < this.components.length; i++)
			clone[i].swapStates(0, this.currentStates[i]);

		return clone;
	}

	public Integer getFirstStateNumber(Integer componentNumber) {
		for (int i = 0; i < components[componentNumber].getStates().length; i++)
			if (this.stateEquivalence.get(componentNumber).get(i)
					.getViewStateNumber() == 0)
				return this.stateEquivalence.get(componentNumber).get(i)
						.getKnowledgeStateNumber();

		throw new UnsupportedOperationException("Wrong state equivalence");
	}

	// endregion

	// region Private methods
	private void removeEventFromCurrentState(int compunentNumber, int event) {
		this.components[compunentNumber].getStates()[this.currentStates[compunentNumber]] = LTSTransitionList
				.removeEvent(
						this.components[compunentNumber].getStates()[this.currentStates[compunentNumber]],
						event);
	}

	private void changeState(Integer componentNumber,
			LTSTransitionList modelNewEventState, Integer nextViewState) {
		Integer nextKnowledgeState = null;
		for (int i = 0; i < this.stateEquivalence.get(componentNumber).size(); i++)
			if (this.stateEquivalence.get(componentNumber).get(i)
					.getViewStateNumber().equals(nextViewState))
				nextKnowledgeState = this.stateEquivalence.get(componentNumber)
						.get(i).getKnowledgeStateNumber();

		if (nextKnowledgeState != null)
			this.currentStates[componentNumber] = nextKnowledgeState;
		else {
			this.currentStates[componentNumber] = this.components[componentNumber]
					.getStates().length;
			this.stateEquivalence.get(componentNumber).add(
					new StateEquivalence(nextViewState,
							this.currentStates[componentNumber]));
			this.addState(componentNumber, this.currentStates[componentNumber],
					modelNewEventState);
		}
	}

	private void addState(Integer componentNumber, int nextState,
			LTSTransitionList modelNewEventState) {
		this.components[componentNumber].setStates(Arrays.copyOf(
				this.components[componentNumber].getStates(), nextState + 1));
		this.components[componentNumber].getStates()[nextState] = modelNewEventState;
	}

	private void conifirmAction(Integer componentNumber, int oldEvent,
			int newEvent, int fromState, int toState) {
		this.components[componentNumber].getStates()[fromState]
				.updateEventAndNext(oldEvent, newEvent, toState);
	}
	// endregion
}
