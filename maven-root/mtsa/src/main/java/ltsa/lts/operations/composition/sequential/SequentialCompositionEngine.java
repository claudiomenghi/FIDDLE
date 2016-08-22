package ltsa.lts.operations.composition.sequential;

import java.util.HashMap;

import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.operations.composition.TriFunction;
import ltsa.lts.util.Counter;

import com.google.common.base.Preconditions;

public class SequentialCompositionEngine
		implements
		TriFunction<String, LabelledTransitionSystem, LabelledTransitionSystem, LabelledTransitionSystem> {

	/**
	 * connect the accepting states of the first machine to the initial state of
	 * the second machine. Remove the self-loops labeled with end from the first
	 * machine
	 * 
	 * @param event
	 *            the event that labels the transitions that connect the first
	 *            and the second machine
	 * @param firstMachine
	 *            the first machine to be composed sequentially
	 * @param secondMachine
	 *            the second machine to be composed sequentially
	 * @return the parallel composition between the first and the second machine
	 * @throws NullPointerException
	 *             if one of the two machines is null
	 */
	@Override
	public LabelledTransitionSystem apply(String event,
			LabelledTransitionSystem firstMachine,
			LabelledTransitionSystem secondMachine) {
		Preconditions.checkNotNull(firstMachine,
				"The first machine cannot be null");
		Preconditions.checkNotNull(secondMachine,
				"The second machine cannot be null");

		LabelledTransitionSystem firstMachineclone = firstMachine.myclone();
		LabelledTransitionSystem secondMachineclone = secondMachine.myclone();

		LabelledTransitionSystem newMachine = new LabelledTransitionSystem("");
		newMachine.setAlphabet(alphabetUnion(firstMachineclone,
				secondMachineclone));

		firstMachineclone.getBoxIndexes().entrySet()
				.forEach(e -> newMachine.addBoxIndex(e.getKey(), e.getValue()));
		

		// the number of states minus the box
		newMachine.setStates(new LTSTransitionList[firstMachine
				.getNumberOfStates() + secondMachineclone.getNumberOfStates()]);

		int offset = 0;
		copyMachine(offset, firstMachineclone, newMachine);
		firstMachine.getFinalStateIndexes().forEach(newMachine::addFinalStateIndex);
		
		final int offsetSecondMachine = firstMachine.getStates().length;
		copyMachine(offsetSecondMachine, secondMachineclone, newMachine);
		secondMachine.getFinalStateIndexes().forEach(e -> newMachine.addFinalStateIndex(e+offsetSecondMachine));

		secondMachineclone.getBoxIndexes().entrySet()
		.forEach(e -> newMachine.addBoxIndex(e.getKey(), e.getValue()+offsetSecondMachine));

		int eventIndex=newMachine.addEvent(event);

		for (Integer index : firstMachineclone.getAccepting()) {
			newMachine.addTransition(index, eventIndex, offsetSecondMachine);
		}

		newMachine.setName(firstMachine.getName()+"."+secondMachine.getName());
		return newMachine;
	}

	/**
	 * create shared alphabet (union of the alphabets) for machines & renumber
	 * according to that alphabet
	 */
	private String[] alphabetUnion(LabelledTransitionSystem... sm) {
		// set up shared alphabet structure
		Counter newLabel = new Counter(0);
		HashMap<String, Integer> actionMap = new HashMap<>();
		for (int i = 0; i < sm.length; i++) {
			for (int j = 0; j < sm[i].getAlphabet().length; j++) {
				if (!actionMap.containsKey(sm[i].getAlphabet()[j])) {
					actionMap.put(sm[i].getAlphabet()[j], newLabel.label());
				}
			}
		}
		// copy into alphabet array
		String[] actionName = new String[actionMap.size()];
		for (String s : actionMap.keySet()) {
			int index = actionMap.get(s).intValue();
			actionName[index] = s;
		}
		// renumber all transitions with new action numbers
		for (int i = 0; i < sm.length; i++) {
			for (int j = 0; j < sm[i].getMaxStates(); j++) {
				LTSTransitionList p = sm[i].getStates()[j];
				while (p != null) {
					LTSTransitionList tr = p;
					tr.setEvent(actionMap.get(
							sm[i].getAlphabet()[tr.getEvent()]).intValue());
					while (tr.getNondet() != null) {
						tr.getNondet().setEvent(tr.getEvent());
						tr = tr.getNondet();
					}
					p = p.getList();
				}
			}
		}
		return actionName;
	}

	private void copyMachine(int offset,
			LabelledTransitionSystem machineToBeCopied,
			LabelledTransitionSystem destinationMachine) {
		for (int i = 0; i < machineToBeCopied.getStates().length; i++) {
			destinationMachine.setState(
					i + offset,
					applyOffset(offset,
							machineToBeCopied.getStates()[i]));
		}

	}

	public LTSTransitionList applyOffset(int off,
			LTSTransitionList head) {
		LTSTransitionList p = head;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				if (q.getNext() >= 0) {
					q.setNext(q.getNext() + off);
				}
				q = q.getNondet();
			}
			p = p.getList();
		}
		return head;
	}
}
