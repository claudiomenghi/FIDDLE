package scalabilityAssessment.preconditionGenerator;

import java.util.List;

import ltsa.lts.ltl.formula.Formula;
import scalabilityAssessment.propertygenerator.PropertyGenerator;

public class PreconditionGenerator {

	private final PropertyGenerator propertyGenerator;

<<<<<<< HEAD
<<<<<<< HEAD
	private final List<String> alphabet;

	public PreconditionGenerator(List<String> alphabet, String event1,
			String event2) {
		this.propertyGenerator = new PropertyGenerator(alphabet, event1, event2);
		this.alphabet = alphabet;
=======
	public PreconditionGenerator(List<String> alphabet, String event1, String event2) {
		this.propertyGenerator = new PropertyGenerator(alphabet, event1, event2);
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
	public PreconditionGenerator(List<String> alphabet, String event1, String event2) {
		this.propertyGenerator = new PropertyGenerator(alphabet, event1, event2);
>>>>>>> dev

	}

	public List<Formula> getFormulae() {
<<<<<<< HEAD
<<<<<<< HEAD
		
=======

>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======

>>>>>>> dev
		return propertyGenerator.getFormulae();
	}
}
