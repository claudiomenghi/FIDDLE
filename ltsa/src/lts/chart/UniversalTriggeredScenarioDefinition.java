package lts.chart;

import lts.Symbol;
import lts.chart.util.TriggeredScenarioDefinitionToTriggeredScenario;
import lts.chart.util.TriggeredScenarioTransformationException;
import ar.dc.uba.model.lsc.TriggeredScenario;
import ar.dc.uba.model.lsc.LocationNamingStrategyImpl;

/**
 * @author gsibay
 *
 */
public class UniversalTriggeredScenarioDefinition extends TriggeredScenarioDefinition {

    public static void put(TriggeredScenarioDefinition chart) {
    	throw new UnsupportedOperationException("Use the superclass to hold the definitions");
    }

	public UniversalTriggeredScenarioDefinition(String name) {
		super(name);
	}
    
	public UniversalTriggeredScenarioDefinition(Symbol n) {
		super(n);
	}

	@Override
	public TriggeredScenario adapt(LocationNamingStrategyImpl locationNamingStrategyImpl) throws TriggeredScenarioTransformationException {
		return TriggeredScenarioDefinitionToTriggeredScenario.getInstance().transformUniversalTriggeredScenario(this, new LocationNamingStrategyImpl());
	}

}
