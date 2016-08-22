package ltsa.lts.automata.lts.state;

import java.util.Iterator;
import java.util.Set;

import ltsa.ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import MTSTools.ac.ic.doc.mtstools.model.MTS;

/**
 * Created by Victor Wjugow on 28/05/15. This class has an extra property, that
 * marks special states of a CompactState
 */
public class MarkedCompactState extends LabelledTransitionSystem {

	private int[] markedStates;

	public MarkedCompactState(LabelledTransitionSystem cs, int[] markedStates) {
		super(cs.getName(), cs.getMaxStates());
		super.setAlphabet(
				cs.getAlphabet());
		super.setEndOfSequence(cs.getEndOfSequenceIndex());
		this.setStates(cs.getStates());
		this.markedStates = markedStates;
	}

	public MarkedCompactState(MTS<Long, String> mts, Set<Long> terminalSet,
			String name) {
		super(name);
		LabelledTransitionSystem cs = MTSToAutomataConverter.getInstance().convert(mts,
				name);
		super.setAlphabet(
				cs.getAlphabet());
		super.setEndOfSequence(cs.getEndOfSequenceIndex());
		super.setName(cs.getName());
		this.setStates(cs.getStates());
		this.markedStates = new int[terminalSet.size()];
		int i = 0;
		for (Iterator<Long> it = terminalSet.iterator(); it.hasNext(); ++i) {
			this.markedStates[i] = (int) it.next().longValue();
		}
	}

	public int[] getMarkedStates() {
		return markedStates;
	}

	public void setMarkedStates(int[] markedStates) {
		this.markedStates = markedStates;
	}
}