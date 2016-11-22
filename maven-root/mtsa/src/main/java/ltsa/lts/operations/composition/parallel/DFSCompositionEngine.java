package ltsa.lts.operations.composition.parallel;

import java.util.HashSet;
import java.util.List;

import ltsa.lts.animator.ModelExplorerContext;
import ltsa.lts.animator.StateCodec;
import ltsa.lts.automata.lts.LTSConstants;
import ltsa.lts.util.LTSUtils;
import ltsa.lts.util.Options;
import ltsa.lts.util.collections.MyHashStack;
import ltsa.lts.util.collections.StateMap;

import com.google.common.base.Preconditions;

/**
 * DFS Composition Strategy
 * 
 * @author epavese
 *
 */
public class DFSCompositionEngine implements CompositionEngine {

	private StateMap analysed;
	private StateCodec coder;
	private ModelExplorerContext ctx;
	private boolean deadlockDetected;
	private long maxStateGeneration;
	
	public DFSCompositionEngine(StateCodec coder) {
		Preconditions.checkNotNull(coder, "The coder cannot be null");
		this.coder = coder;
		this.analysed = new MyHashStack(100001);
		this.maxStateGeneration = Options.getMaxStatesGeneration();
	}

	@Override
	public void initialize() {
	}

	@Override
	public void teardown() {
		this.analysed = null;
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
		return this.analysed;
	}

	@Override
	public void add(byte[] state) {
		this.analysed.add(state);
	}

	@Override
	public void add(byte[] state, int depth) {
		this.analysed.add(state, depth);
	}

	@Override
	public byte[] getNextState() {
		return this.analysed.getNextState();
	}

	@Override
	public boolean nextStateIsMarked() {
		return this.analysed.nextStateIsMarked();
	}

	@Override
	public void removeNextState() {
		this.analysed.removeNextState();
	}

	@Override
	public boolean deadlockDetected() {
		return this.deadlockDetected;
	}

	@Override
	public void processNextState() {
		byte[] encodedState=getNextState();
		int[] state = coder.decode(encodedState);
		this.analysed.markNextState(ctx.stateCount++);

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
			CompositionEngineCommon.processTransitions(coder, ctx,
					transitions, analysed, state);
		}
	}

	@Override
	public String getExplorationStatistics() {
		return "";
	}

	@Override
	public void setModelExplorerContext(ModelExplorerContext ctx) {
		this.ctx = ctx;
		this.ctx.finalStates=new HashSet<>();
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
		int tauIndex = 0;
		for (int i = 0; i < ctx.actionName.length; i++) {
			if (ctx.actionName[i].equals("tau")) {
				tauIndex = i;
				break;
			}
		}

		ctx.stateCount++;
		int[] trapState = null;
		byte[] trapStateCode = null;
		while (!analysed.empty()) {
			if (!analysed.nextStateIsMarked()) {
				if (analysed.getNextStateNumber() == -1) {
					analysed.markNextState(ctx.stateCount++);
				}

				if (trapState == null) {
					byte[] nextState = analysed.getNextState();
					trapState = LTSUtils.myclone(coder.decode(nextState));
					for (int i = 0; i < trapState.length; i++) {
						trapState[i] = LTSConstants.TRAPSTATE;
					}

					trapStateCode = coder.encode(trapState);
				}
				ctx.compTrans.add(analysed.getNextStateNumber(), trapStateCode,
						tauIndex);
			}
			analysed.removeNextState();
		}
	}
	
	
}
