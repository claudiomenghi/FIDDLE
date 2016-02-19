package MTSSynthesis.controller.model;


import MTSTools.ac.ic.doc.mtstools.model.LTS;

public interface ControlProblem<S, A> {

	public abstract LTS<S, A> solve();
}