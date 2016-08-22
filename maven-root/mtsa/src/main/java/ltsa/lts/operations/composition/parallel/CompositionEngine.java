package ltsa.lts.operations.composition.parallel;

import java.util.Set;

import ltsa.lts.animator.ModelExplorerContext;
import ltsa.lts.util.collections.StateMap;

public interface CompositionEngine {
	public void initialize();

	public void teardown();

	public StateMap getExploredStates();

	public StackCheck getStackChecker();

	public void add(byte[] state);

	public void add(byte[] state, int depth);

	public byte[] getNextState();

	public void removeNextState();

	public boolean nextStateIsMarked();

	/**
	 * process the next state of the composition
	 */
	public void processNextState();

	public String getExplorationStatistics();

	public void setModelExplorerContext(ModelExplorerContext ctx);

	public ModelExplorerContext getModelExplorerContext();

	public boolean deadlockDetected();

	public void setMaxStateGeneration(long maxStates);

	public long getMaxStateGeneration();

	public void pruneUnfinishedStates();
}
