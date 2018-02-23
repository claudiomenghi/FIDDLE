package ltsa.lts.operations.composition.sequential;

import java.util.HashMap;
import java.util.Vector;
import java.util.function.Function;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.util.Counter;

/**
 * sequentially composes the compact states
 *
 */
public class SequentialMergeEngine implements
		Function<Vector<LabelledTransitionSystem>, LabelledTransitionSystem> {

	@Override
	public LabelledTransitionSystem apply(Vector<LabelledTransitionSystem> seqs) {
		if (seqs == null) {
			return null;
		}
		if (seqs.isEmpty()) {
			return null;
		}
		if (seqs.size() == 1) {
			return seqs.elementAt(0);
		}
		LabelledTransitionSystem[] machines = new LabelledTransitionSystem[seqs.size()];
		machines = seqs.toArray(machines);
		LabelledTransitionSystem newMachine = new LabelledTransitionSystem("");
		newMachine.setAlphabet(sharedAlphabet(machines));
		newMachine.setStates(new LTSTransitionList[seqSize(machines)]);
		int offset = 0;
		for (int i = 0; i < machines.length; i++) {
			boolean last = (i == (machines.length - 1));
			copyOffset(offset, newMachine.getStates(), machines[i], last);
			if (last)
				newMachine.setEndOfSequence(machines[i].getEndOfSequenceIndex() + offset);
			offset += machines[i].getStates().length;
		}
		return newMachine;
	}

	/**
	 * create shared alphabet for machines & renumber according to that alphabet
	 */
	private String[] sharedAlphabet(LabelledTransitionSystem[] sm) {
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
	private int seqSize(LabelledTransitionSystem[] sm) {
		int length = 0;
		for (int i = 0; i < sm.length; i++)
			length += sm[i].getStates().length;
		return length;
	}

	private void copyOffset(int offset, LTSTransitionList[] dest, LabelledTransitionSystem m,
			boolean last) {
		for (int i = 0; i < m.getStates().length; i++) {
			if (!last){
				dest[i + offset] = LTSTransitionList.offsetSeq(offset, m.getEndOfSequenceIndex(),
						m.getMaxStates() + offset, m.getStates()[i]);
			}
			else{
				dest[i + offset] = LTSTransitionList.offsetSeq(offset, m.getEndOfSequenceIndex(),
						m.getEndOfSequenceIndex() + offset, m.getStates()[i]);
			}
		}
	}
}
