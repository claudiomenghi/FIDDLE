package ltsa.lts.operations.composition.parallel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ltsa.lts.animator.ModelExplorerContext;
import ltsa.lts.animator.StateCodec;
import ltsa.lts.automata.lts.LTSConstants;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.util.LTSUtils;
import ltsa.lts.util.Options;
import ltsa.lts.util.collections.RandomHashStateMap;
import ltsa.lts.util.collections.StateMap;
import ltsa.lts.util.collections.StateMapEntry;

public class RandomCompositionEngine implements CompositionEngine {
	private StateMap analysed;
	private StateCodec coder;
	private long maxStateGeneration;
	private ModelExplorerContext ctx;
	private boolean deadlockDetected;
	LTSOutput output;
	
	
	
	public RandomCompositionEngine(StateCodec coder) {
		this.coder= coder;
		if (Options.useGeneratedSeed()) {
			analysed= new RandomHashStateMap(100001, Options.getRandomSeed());
		} else {
			analysed= new RandomHashStateMap(100001);
		}
		maxStateGeneration= Options.getMaxStatesGeneration();
		ctx.finalStates=new HashSet<>();
	}
	
	@Override
	public void initialize() {
		output.outln("Initializing RANDOM composition engine, seed= " +
					 ((RandomHashStateMap) analysed).getSeed());
	}
	
	@Override
	public StackCheck getStackChecker() {
		if (analysed instanceof StackCheck)
			return (StackCheck) analysed;
		else
			return null;
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
	public boolean deadlockDetected() {
		return deadlockDetected;
	}

	@Override
	public String getExplorationStatistics() {
		return "";
	}

	@Override
	public StateMap getExploredStates() {
		return analysed;
	}

	@Override
	public long getMaxStateGeneration() {
		return maxStateGeneration;
	}

	@Override
	public ModelExplorerContext getModelExplorerContext() {
		return ctx;
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
	public void processNextState() {
		byte[] encodedState=getNextState();
		int[] state = coder.decode(encodedState);
		analysed.markNextState(ctx.stateCount++);
		if (ModelExplorer.isFinal(ctx, state)) {
			ctx.finalStates.add(encodedState);
		} 
		int depth= ((RandomHashStateMap) analysed).getNextStateDepth();

		// determine eligible transitions
		List transitions = ModelExplorer.eligibleTransitions(ctx, state);
		if (transitions == null) {
			if (!ModelExplorer.isEND(ctx, state)) {
				// deadlockDetected = true;
				// HACK try to keep exploring
				((RandomHashStateMap) analysed).unmarkNextState();
				StateMapEntry poppedState= (StateMapEntry) ((RandomHashStateMap) analysed).popNextState();
				analysed.add(poppedState.key);
				// CAN LEAD TO LOOPING!
				--ctx.stateCount;
				
			} else { // this is the end state
				if (ctx.endSequence < 0)
					ctx.endSequence = ctx.stateCount - 1;
				else {
					analysed.markNextState(ctx.endSequence);
					--ctx.stateCount;
				}
			}
		} else {
			Iterator e = transitions.iterator();
			while (e.hasNext()) {
				int[] next = (int[]) e.next();
				byte[] code = coder.encode(next);
				ctx.compTrans.add(ctx.stateCount-1, code, next[ctx.Nmach]);
				if (code == null) {
					int i = 0;
					while (next[i] >= 0)
						i++;
					if (!ctx.violated[i]) {
						// TODO move away and log
						// output.outln("  property " + ctx.sm[i].name + " violation.");
					}
					ctx.violated[i] = true;
				} else if (!analysed.contains(code)) {
					analysed.add(code, depth+1);
				}
			}
		}
	}

	// @Override
	public void pruneUnfinishedStates() {
		// TODO can be improved.
		int tauIndex= 0;
		for (int i= 0; i < ctx.actionName.length; i++) {
			if (ctx.actionName[i].equals("tau")) {
				tauIndex= i;
				break;
			}
		}

		ctx.stateCount++;
		int[] trapState= null;
		byte[] trapStateCode= null;
		while (!analysed.empty()) {
			if (!analysed.nextStateIsMarked()) {
				if (analysed.getNextStateNumber() == -1) {
					analysed.markNextState(ctx.stateCount++);
				}

				if (trapState == null) {
					byte[] nextState= analysed.getNextState();
					trapState= LTSUtils.myclone(coder.decode(nextState));
					for (int i= 0; i < trapState.length; i++) {
						trapState[i]= LTSConstants.TRAPSTATE;
					}
					
					trapStateCode= coder.encode(trapState);
				}
				
				ctx.compTrans.add(analysed.getNextStateNumber(), trapStateCode, tauIndex);
			}
			analysed.removeNextState();
		}
	}

	@Override
	public void removeNextState() {
		analysed.removeNextState();
	}

	@Override
	public void setMaxStateGeneration(long maxStates) {
		maxStateGeneration= maxStates;
	}

	@Override
	public void setModelExplorerContext(ModelExplorerContext ctx) {
		this.ctx= ctx;
		this.ctx.finalStates=new HashSet<>();
	}

	@Override
	public void teardown() {
		analysed= null;
	}
	
	
}
