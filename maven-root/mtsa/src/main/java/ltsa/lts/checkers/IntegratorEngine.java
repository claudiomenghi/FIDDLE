package ltsa.lts.checkers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

import MTSTools.ac.ic.doc.mtstools.model.MTSConstants;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.csp.Declaration;
import ltsa.lts.csp.Relation;
import ltsa.lts.util.Counter;

/**
 * Given a Labeled Transition System, one of its boxes and the post-condition of
 * the box, it substitutes the LTS corresponding to the post-condition of the
 * box to the box it self. <br/>
 * The traces of the obtained automaton contain all the traces that satisfy the
 * post-condition of the box
 *
 */
public class IntegratorEngine {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Given a Labeled Transition System, one of its boxes and the
	 * post-condition of the box, it substitutes the LTS corresponding to the
	 * post-condition of the box to the box it self. <br/>
	 * The traces of the obtained automaton contain all the traces that satisfy
	 * the post-condition of the box
	 * 
	 * @param controllerMachine
	 *            the LTS of the machine that contains the box
	 * @param boxIndex
	 *            the Index of the considered box
	 * @param postConditionMachine
	 *            the postCondition of the box
	 * @throws NullPointerException
	 *             if one of the machine is null
	 * @throws IllegalArgumentException
	 *             if the index of the box is not an index of a box of the
	 *             original machine
	 */
	public LabelledTransitionSystem apply(LabelledTransitionSystem controllerMachine, Integer boxIndex, String boxName,
			LabelledTransitionSystem postConditionMachine) {
		Preconditions.checkNotNull(controllerMachine, "The first machine cannot be null");
		Preconditions.checkNotNull(postConditionMachine, "The machine of the post-condition cannot be null");
		Preconditions.checkArgument(controllerMachine.getBoxIndexes().values().contains(boxIndex),
				"The specified index is not an index of the box of the state machine");

		LabelledTransitionSystem postConditionClone = postConditionMachine.myclone();

		LabelledTransitionSystem newMachine = new LabelledTransitionSystem("");

		logger.debug("Postcondition:  " + postConditionClone.getName());

		newMachine.setAlphabet(sharedAlphabet(controllerMachine, postConditionClone));

		logger.debug("Size of the new machine: " + seqSize(controllerMachine, postConditionClone));

		// the number of states minus the box
		newMachine.setStates(new LTSTransitionList[seqSize(controllerMachine, postConditionClone)]);

		int offset = 0;

		// copies the controller in the new machine

		copyController(newMachine,  newMachine.getStates(), controllerMachine, boxIndex, boxName);

		offset = controllerMachine.getStates().length;

		// copies the post-condition in the new machine
		copyPostcondition(newMachine, offset, newMachine.getStates(), postConditionClone);

		newMachine.setEndOfSequence(postConditionClone.getEndOfSequenceIndex() + offset);

		// merges the two machines
		LTSTransitionList tauTransition = new LTSTransitionList(Declaration.TAU
				, offset);
		newMachine.setState(boxIndex, tauTransition);

		// removes the accepting states
		LTSTransitionList boxList = controllerMachine.getTransitions(boxIndex);

		final int propertyOffset = offset;
		postConditionMachine.getFinalStateIndexes().forEach(index -> {
			newMachine.setState(index + propertyOffset,
					this.getMixTransitionList(boxList, newMachine.getTransitions(index + propertyOffset)));
		});

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
	private LTSTransitionList getMixTransitionList(LTSTransitionList boxList, LTSTransitionList acceptingList) {

		LTSTransitionList currentBoxTransition = boxList;

		LTSTransitionList head = acceptingList;
		while (currentBoxTransition != null) {
			LTSTransitionList currentTransition = acceptingList;
			boolean founded = false;
			while (currentTransition != null && !founded) {
				if (currentTransition.getEvent() == currentBoxTransition.getEvent()) {
					founded = true;

				} else {
					currentTransition = currentTransition.getList();
				}
			}
			if (founded) {
				LTSTransitionList nonDet = new LTSTransitionList(currentBoxTransition.getEvent(),
						currentBoxTransition.getNext());
				nonDet.setNondet(currentBoxTransition.getNondet());
				currentTransition.setNondet(this.concatenateNonDetTransitions(currentTransition.getEvent(),
						currentTransition.getNondet(), nonDet));
			} else {
				LTSTransitionList newHead = new LTSTransitionList(currentBoxTransition.getEvent(),
						currentBoxTransition.getNext());
				newHead.setNondet(currentBoxTransition.getNondet());
				newHead.setList(head);
				head = newHead;
			}
			currentBoxTransition = currentBoxTransition.getList();
		}

		return head;
	}

	private LTSTransitionList concatenateNonDetTransitions(int event, LTSTransitionList list1,
			LTSTransitionList list2) {

		if (list1 == null) {
			return list2;
		}
		if (list2 == null) {
			return list1;
		}
		LTSTransitionList currentTransition = list1;

		while (currentTransition.getNondet() != null) {
			if (currentTransition.getEvent() != event) {
				throw new IllegalArgumentException("The transition in list 1 is not labeled with the correct event");
			}
			currentTransition = currentTransition.getNondet();
		}
		currentTransition = list2;

		while (currentTransition.getNondet() != null) {
			if (currentTransition.getEvent() != event) {
				throw new IllegalArgumentException("The transition in list 1 is not labeled with the correct event");
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

	private void copyPostcondition(LabelledTransitionSystem newMachine, int offset, LTSTransitionList[] dest,
			LabelledTransitionSystem postCondition) {
		for (int i = 0; i < postCondition.getStates().length; i++) {
			dest[i + offset] = LTSTransitionList.offsetSeq(offset, newMachine.getMaxStates() + 1,
					newMachine.getMaxStates() + 1, postCondition.getStates()[i]);
		}
		postCondition.getFinalStateIndexes().forEach(index -> newMachine.addFinalStateIndex(index + offset));
	}

	private void copyController(LabelledTransitionSystem newMachine, LTSTransitionList[] dest,
			LabelledTransitionSystem m, int boxPosition, String boxName) {
		for (int i = 0; i < m.getStates().length; i++) {
			if (i != boxPosition) {
				dest[i] = m.getStates()[i];
			}
		}

		m.getBoxIndexes().entrySet().forEach(e -> {
			if (e.getValue() != boxPosition) {
				newMachine.addBoxIndex(e.getKey(), e.getValue());
			}
		});

		m.getBoxes().stream().filter(Predicate.isEqual(boxName).negate())
				.forEach(box -> newMachine.setBoxInterface(box, m.getBoxInterface(box)));
	}
}
