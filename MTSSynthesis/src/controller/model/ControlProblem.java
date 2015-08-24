package controller.model;


import ac.ic.doc.mtstools.model.LTS;

public interface ControlProblem<S, A> {

	public abstract LTS<S, A> solve();
}