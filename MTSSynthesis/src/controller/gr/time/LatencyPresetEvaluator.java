package controller.gr.time;

import java.util.Set;

public class LatencyPresetEvaluator<P,A,S> extends LatencyEvaluator<P,A,S> {
	protected Set<GenericChooser<S, A, P>> schedulers;
	
	public LatencyPresetEvaluator(Set<GenericChooser<S, A, P>> schedulers) {
		this.schedulers = schedulers;
	}

	@Override
	protected Set<GenericChooser<S, A, P>> getSchedulers() {
		return schedulers;
	}

}
