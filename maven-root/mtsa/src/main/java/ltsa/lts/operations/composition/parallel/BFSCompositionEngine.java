package ltsa.lts.operations.composition.parallel;

import java.util.HashSet;
import java.util.List;

import ltsa.lts.animator.ModelExplorerContext;
import ltsa.lts.animator.StateCodec;
import ltsa.lts.util.Options;
import ltsa.lts.util.collections.MyHashQueue;
import ltsa.lts.util.collections.StateMap;

/**
 * BFS Composition Strategy
 * 
 * @author epavese
 *
 */
public class BFSCompositionEngine implements CompositionEngine {
	private StateMap analysed;
	private StateCodec coder;
	private ModelExplorerContext ctx;
	private boolean deadlockDetected;
	private long maxStateGeneration;

	public BFSCompositionEngine(StateCodec coder) {
		this.coder = coder;
		analysed = new MyHashQueue(100001);
		maxStateGeneration = Options.getMaxStatesGeneration();
		ctx.finalStates = new HashSet<>();
	}

	@Override
	public void initialize() {
	}

	@Override
	public void teardown() {
		analysed = null;
	}

	@Override
	public StackCheck getStackChecker() {
		if (analysed instanceof StackCheck)
			return (StackCheck) analysed;
		else
			return null;
	}

	@Override
	public StateMap getExploredStates() {
		return analysed;
	}

	@Override
	public void add(byte[] state) {
		analysed.add(state);
	}

	@Override
	public void add(byte[] state, int depth) {
		analysed.add(state, depth);
	}

	@Override
	public byte[] getNextState() {
		return analysed.getNextState();
	}

	@Override
	public boolean nextStateIsMarked() {
		return analysed.nextStateIsMarked();
	}

	@Override
	public void removeNextState() {
		analysed.removeNextState();
	}

	@Override
	public boolean deadlockDetected() {
		return deadlockDetected;
	}

	@Override
	public void processNextState() {
		byte[] encodedState = getNextState();
		int[] state = coder.decode(encodedState);
		analysed.markNextState(ctx.stateCount++);

		// determine eligible transitions
		List<int[]> transitions = ModelExplorer.eligibleTransitions(ctx, state);
		if (transitions == null) {
			if (!ModelExplorer.isEND(ctx, state)) {
				deadlockDetected = true;
			} else { // this is the end state
				if (ctx.endSequence < 0)
					ctx.endSequence = ctx.stateCount - 1;
				else {
					analysed.markNextState(ctx.endSequence);
					--ctx.stateCount;
				}
			}
		} else {
			if (ModelExplorer.isFinal(ctx, state)) {
				ctx.finalStates.add(encodedState);
			}
			CompositionEngineCommon.processTransitions(coder, ctx, transitions,
					analysed, state);
		}
	}

	@Override
	public String getExplorationStatistics() {
		return "";
	}

	@Override
	public void setModelExplorerContext(ModelExplorerContext ctx) {
		this.ctx = ctx;
		this.ctx.finalStates = new HashSet<>();
	}

	@Override
	public ModelExplorerContext getModelExplorerContext() {
		return ctx;
	}

	@Override
	public void setMaxStateGeneration(long maxStates) {
		maxStateGeneration = maxStates;
	}

	@Override
	public long getMaxStateGeneration() {
		return maxStateGeneration;
	}

	@Override
	public void pruneUnfinishedStates() {

	}
}
