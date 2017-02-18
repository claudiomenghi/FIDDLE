package scalabilityAssessment.modelgenerator;

import java.util.Iterator;

public class ConfigurationGenerator implements Iterator<ModelConfiguration> {

	private final int initStatesEnvironment = 10;
	private final int incrementStatesEnvironment = 10;
	private final int finalStatesEnvironment = 10000;

//	private final int initStatesController = 50;
//	private final int incrementalStatesController = 50;
//	private final int finalStatesController = 300;

	private final int initStatesController = 200;
	private final int incrementalStatesController = 200;
	private final int finalStatesController = 1000;
	
	private final int environmentTransitionRatio = 15;
	private final int controllerTransitionRatio = 22;

	private int currentStatesController = initStatesController;
	private int currentStatesEnvironment = initStatesEnvironment;

	private boolean first = true;

	@Override
	public boolean hasNext() {
		if (currentStatesEnvironment == finalStatesEnvironment && currentStatesController == finalStatesController) {
			return false;
		}
		return true;
	}

	@Override
	public ModelConfiguration next() {
		if (currentStatesEnvironment > finalStatesEnvironment) {
			throw new IllegalArgumentException("There is no next");
		}
		if (first) {
			this.first = false;
		} else {
			if (this.currentStatesController < finalStatesController) {
				this.currentStatesController = this.currentStatesController + this.incrementalStatesController;
			} else {
				this.currentStatesEnvironment = this.currentStatesEnvironment * this.incrementStatesEnvironment;
				this.currentStatesController = this.initStatesController;

			}
		}
		int transitionsEnvironment = this.currentStatesEnvironment
				* this.environmentTransitionRatio;
		// int eventsEnvironment = (int) Math
		// .round(110.0 * this.currentStatesEnvironment / 10000);
		int eventsEnvironment = 10+this.initStatesEnvironment*2;
		int transitionsController = this.currentStatesController * this.controllerTransitionRatio;
		int eventsController = 10+this.initStatesController*2;
				

		int eventsControllerInterface = (int) Math.round(25.0 / 100 * eventsController);
		return new ModelConfiguration(this.currentStatesEnvironment, transitionsEnvironment, eventsEnvironment,
				currentStatesController, transitionsController, eventsController, eventsControllerInterface);
	}
}