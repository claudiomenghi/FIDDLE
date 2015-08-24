package updatingControllers.structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.MTS;
import ar.dc.uba.model.condition.Fluent;
import ar.dc.uba.model.condition.FluentUtils;
import controller.game.util.FluentStateValuation;

public class MappingStructure {

	private HashMap<ArrayList<Boolean>, Set<Long>> structureOld;
	private HashMap<ArrayList<Boolean>, Set<Long>> structureNew;

	public MappingStructure(MTS<Long, String> updatingEnvironment,
			MTS<Long, String> newEnvironment, List<Fluent> properties) {
		//we need them as List because order matters (these are used as the key of the structures above)
		FluentUtils fluentUtils = FluentUtils.getInstance();
		FluentStateValuation<Long> valuationOld = fluentUtils.buildValuation(updatingEnvironment, properties);
		FluentStateValuation<Long> valuationNew = fluentUtils.buildValuation(newEnvironment, properties);

		structureOld = setStructure(updatingEnvironment, valuationOld,  properties);
		structureNew = setStructure(newEnvironment, valuationNew, properties);
	}

	private HashMap<ArrayList<Boolean>, Set<Long>> setStructure(MTS<Long, String> mts, FluentStateValuation<Long>
		valuation, List<Fluent> properties) {
		
		HashMap<ArrayList<Boolean>, Set<Long>> structure = new HashMap<ArrayList<Boolean>, Set<Long>>();
		for (Long state : mts.getStates()) {

			ArrayList<Boolean> valuationOfState = valuation.getFluentsFromState(state, properties);
			if (structure.containsKey(valuationOfState)) {
				Set<Long> value = structure.get(valuationOfState);
				value.add(state);
				structure.put(valuationOfState, value);
			} else {
				Set<Long> newList = new HashSet<Long>();
				newList.add(state);
				structure.put(valuationOfState, newList);
			}
		}
		return structure;
	}

	public Set<ArrayList<Boolean>> valuationsOld() {
		return structureOld.keySet();
	}

	public Set<Long> getOldStates(ArrayList<Boolean> valuation) {
		return structureOld.get(valuation);
	}
	
	public Set<Long> getNewStates(ArrayList<Boolean> valuation) {
		return structureNew.get(valuation);
	}

	public boolean containsNewValuation(ArrayList<Boolean> oldValuation) {
		return structureNew.containsKey(oldValuation);
	}


}