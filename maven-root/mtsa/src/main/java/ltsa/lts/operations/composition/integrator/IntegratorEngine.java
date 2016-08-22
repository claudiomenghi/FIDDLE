package ltsa.lts.operations.composition.integrator;

import java.util.HashMap;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.operations.composition.TriFunction;
import ltsa.lts.util.Counter;

import com.google.common.base.Preconditions;

public class IntegratorEngine
		implements
		TriFunction<Integer, LabelledTransitionSystem, LabelledTransitionSystem, LabelledTransitionSystem> {

	@Override
	public LabelledTransitionSystem apply(Integer boxIndex,
			LabelledTransitionSystem controllerMachine,
			LabelledTransitionSystem postConditionMachine) {
		Preconditions.checkNotNull(controllerMachine,
				"The first machine cannot be null");
		Preconditions.checkNotNull(postConditionMachine,
				"The second machine cannot be null");

		LabelledTransitionSystem secondMachineclone = postConditionMachine
				.myclone();

		LabelledTransitionSystem newMachine = new LabelledTransitionSystem("");
		newMachine.setAlphabet(sharedAlphabet(controllerMachine,
				secondMachineclone));

		newMachine.setBoxIndexes(new HashMap<String, Integer>(controllerMachine
				.getBoxIndexes()));
		newMachine.getBoxIndexes().remove(postConditionMachine.getName());

		newMachine.mapBoxInterface = new HashMap<>(
				controllerMachine.mapBoxInterface);
		newMachine.mapBoxInterface.remove(postConditionMachine);
		// the number of states minus the box
		newMachine.setStates(new LTSTransitionList[seqSize(controllerMachine,
				secondMachineclone)]);

		int offset = 0;
		copyFirstMachine(newMachine, offset, newMachine.getStates(), controllerMachine,
				false, boxIndex);

		offset = controllerMachine.getStates().length;
		LTSTransitionList tauTransition = new LTSTransitionList(0, offset);
		newMachine.setState(boxIndex, tauTransition);

		copySecondMachine(newMachine, offset, newMachine.getStates(), secondMachineclone,
				true);
		newMachine.setEndOfSequence(secondMachineclone.getEndOfSequenceIndex()
				+ offset);

		// removes the accepting states
		LTSTransitionList boxList = controllerMachine.getTransitions(boxIndex);


		for (Integer index : postConditionMachine.getAccepting()) {
			newMachine.setState(index + offset,
			// LTSTransitionList
			// .removeAccept(this.getMixTransitionList(boxList,
			// newMachine.getTransitions(index + offset)))
					this.getMixTransitionList(boxList,
							newMachine.getTransitions(index + offset)));

		}

		return newMachine;
	}

	/**
	 * given the transitions that exits the box and the transitions of the
	 * accepting states merges the two transition sets
	 * 
	 * @param boxList
	 * @param acceptingList
	 * @return
	 */
	private LTSTransitionList getMixTransitionList(LTSTransitionList boxList,
			LTSTransitionList acceptingList) {

		LTSTransitionList currentBoxTransition = boxList;

		LTSTransitionList head = acceptingList;
		while (currentBoxTransition != null) {
			LTSTransitionList currentTransition = acceptingList;
			boolean founded = false;
			while (currentTransition != null && !founded) {
				if (currentTransition.getEvent() == currentBoxTransition
						.getEvent()) {
					founded = true;

				} else {
					currentTransition = currentTransition.getList();
				}
			}
			if (founded) {
				LTSTransitionList nonDet = new LTSTransitionList(
						currentBoxTransition.getEvent(),
						currentBoxTransition.getNext());
				nonDet.setNondet(currentBoxTransition.getNondet());
				currentTransition.setNondet(this.concatenateNonDetTransitions(
						currentTransition.getEvent(),
						currentTransition.getNondet(), nonDet));
			} else {
				LTSTransitionList newHead = new LTSTransitionList(
						currentBoxTransition.getEvent(),
						currentBoxTransition.getNext());
				newHead.setNondet(currentBoxTransition.getNondet());
				newHead.setList(head);
				head = newHead;
			}
			currentBoxTransition = currentBoxTransition.getList();
		}

		return head;
	}

	private LTSTransitionList concatenateNonDetTransitions(int event,
			LTSTransitionList list1, LTSTransitionList list2) {

		if (list1 == null) {
			return list2;
		}
		if (list2 == null) {
			return list1;
		}
		LTSTransitionList currentTransition = list1;

		while (currentTransition.getNondet() != null) {
			if (currentTransition.getEvent() != event) {
				throw new IllegalArgumentException(
						"The transition in list 1 is not labeled with the correct event");
			}
			currentTransition = currentTransition.getNondet();
		}
		currentTransition = list2;

		while (currentTransition.getNondet() != null) {
			if (currentTransition.getEvent() != event) {
				throw new IllegalArgumentException(
						"The transition in list 1 is not labeled with the correct event");
			}
			currentTransition = currentTransition.getNondet();
		}

		currentTransition = list1;

		while (currentTransition.getNondet() != null) {
			currentTransition = currentTransition.getNondet();
		}
		currentTransition.setNondet(list2);
		return list1;
	}

	/**
	 * create shared alphabet for machines & renumber according to that alphabet
	 */
	private String[] sharedAlphabet(LabelledTransitionSystem... sm) {
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

	/**
	 * compute size of sequential composite
	 */
	private int seqSize(LabelledTransitionSystem... sm) {
		int length = 0;
		for (int i = 0; i < sm.length; i++)
			length += sm[i].getStates().length;
		return length;
	}

	private void copySecondMachine(LabelledTransitionSystem newMachine, int offset, LTSTransitionList[] dest,
			LabelledTransitionSystem m, boolean last) {
		for (int i = 0; i < m.getStates().length; i++) {
			if (!last) {
				dest[i + offset] = LTSTransitionList.offsetSeq(offset,
						m.getEndOfSequenceIndex(), m.getMaxStates() + offset,
						m.getStates()[i]);
			} else {
				dest[i + offset] = LTSTransitionList.offsetSeq(offset,
						m.getEndOfSequenceIndex(), m.getEndOfSequenceIndex()
								+ offset, m.getStates()[i]);

			}
		}
		m.getFinalStateIndexes().forEach(e -> newMachine.addFinalStateIndex(e+offset));
	}

	private void copyFirstMachine(LabelledTransitionSystem newMachine, int offset, LTSTransitionList[] dest,
			LabelledTransitionSystem m, boolean last, int boxPosition) {
		for (int i = 0; i < m.getStates().length; i++) {

			if (i != boxPosition) {
				dest[i + offset] = offsetSeq(offset, m.getEndOfSequenceIndex(),
						m.getEndOfSequenceIndex() + offset, m.getStates()[i]);
				
				
			}
		}
		
		m.getFinalStateIndexes().forEach(e -> newMachine.addFinalStateIndex(e+offset));

	}

	public LTSTransitionList offsetSeq(int off, int seq, int max,
			LTSTransitionList head) {
		LTSTransitionList p = head;
		while (p != null) {
			LTSTransitionList q = p;
			while (q != null) {
				if (q.getNext() >= 0) {
					if (q.getNext() == seq)
						q.setNext(max);
					else
						q.setNext(q.getNext() + off);
				}
				q = q.getNondet();
			}
			p = p.getList();
		}

		return head;
	}

}
