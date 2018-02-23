package ltsa.lts.animator;

import java.util.BitSet;
import java.util.Set;

import ltsa.lts.automata.lts.LTSConstants;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.util.collections.MyList;

public class ModelExplorerContext {
	/**
	 * number of machines to be composed
	 */
	public int Nmach;
	/**
	 * alpha(nonTerm) subset alpha(term)
	 */
	public boolean canTerminate;								
	public LabelledTransitionSystem[] sm;									// array of state machines to be composed
	public PartialOrder partial;
	public int asteriskEvent = -1; 								// number of asterisk event
	public int endEvent; 
	public int[] actionCount;									// number of machines which share this action;
	public BitSet highAction = null;							// actions with high priority
	public int acceptEvent = -1;								// number of acceptance label @NAME
	public BitSet visible;										// BitSet of visible actions
	public int endSequence = LTSConstants.NO_SEQUENCE_FOUND;
	public int stateCount = 0;									// number of states analysed
	public MyList compTrans;									// list of transitions
	public boolean[] violated;									// true if this property already violated
	public String[] actionName;
	
	public Set<byte[]> finalStates;
}
