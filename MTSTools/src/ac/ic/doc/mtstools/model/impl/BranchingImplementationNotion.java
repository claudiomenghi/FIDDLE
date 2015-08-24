package ac.ic.doc.mtstools.model.impl;

import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.ImplementationNotion;
import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.MTS;
import java.util.Set;

public class BranchingImplementationNotion implements ImplementationNotion {

	/**
	 * @uml.property  name="relationConstructor"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private FixedPointRelationConstructor relationConstructor;

	public BranchingImplementationNotion(Set<?> silentActions) {
		Simulation branchingBisimulation = new SimulationChain()
			.add(new BranchingForwardSimulation(silentActions))
			.add(new BranchingBackwardSimulation(silentActions));

		this.relationConstructor = new FixedPointRelationConstructor(branchingBisimulation);
	}
	

	public <S1, S2, A> boolean isAnImplementation(MTS<S1, A> mts, LTS<S2, A> lts) {
		return this.relationConstructor
			.getLargestRelation(mts, new MTSAdapter<S2,A>(lts))
			.contains(Pair.create(mts.getInitialState(), lts.getInitialState()));
	}

}
