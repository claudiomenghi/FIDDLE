package ac.ic.doc.mtstools.model.impl;

import java.util.Set;

import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.MTS;

public interface Simulation<A>  {

	public <S1,S2> boolean simulate(MTS<S1, A> mts1, S1 s1, MTS<S2, A> mts2, S2 s2, Set<Pair<S1,S2>> relation); 
	
}
