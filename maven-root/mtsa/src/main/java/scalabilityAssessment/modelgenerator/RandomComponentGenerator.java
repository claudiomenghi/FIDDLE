package scalabilityAssessment.modelgenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;

public class RandomComponentGenerator {

	private LabelledTransitionSystem finalComponent;
	
	public LabelledTransitionSystem getComponent(ModelConfiguration size) {

		LabelledTransitionSystem ltsCopy = new RandomLTSGenerator(size.getStatesController(),
				size.getEventsController(), size.getTransitionsPerStateController()).getRandomLTS("CONTROLLER");

		setFinalComponent(ltsCopy.clone());
		int boxIndex = new Random().nextInt(ltsCopy.getStates().length);

		List<String> events = new ArrayList<>(ltsCopy.getAlphabetEvents());
		List<String> interfaceBox = events.subList(0, size.getEventsControllerInterface());
		ltsCopy.addBoxIndex("box", boxIndex);
		ltsCopy.setBoxInterface("box", new HashSet<>(interfaceBox));

		return ltsCopy;
	}

	public LabelledTransitionSystem getFinalComponent() {
		return finalComponent;
	}

	public void setFinalComponent(LabelledTransitionSystem finalComponent) {
		this.finalComponent = finalComponent;
	}

}
