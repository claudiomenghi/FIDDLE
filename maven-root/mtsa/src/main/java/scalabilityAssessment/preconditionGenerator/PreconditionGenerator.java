package scalabilityAssessment.preconditionGenerator;

import java.util.List;

import ltsa.lts.ltl.formula.Formula;
import scalabilityAssessment.propertygenerator.PropertyGenerator;

public class PreconditionGenerator {

	private final PropertyGenerator propertyGenerator;

	private final List<String> alphabet;

	public PreconditionGenerator(List<String> alphabet, String event1,
			String event2) {
		this.propertyGenerator = new PropertyGenerator(alphabet, event1, event2);
		this.alphabet = alphabet;

	}

	public List<Formula> getFormulae() {
		
		return propertyGenerator.getFormulae();
	}
}
