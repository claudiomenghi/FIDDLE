package scalabilityAssessment.modelgenerator;

import java.util.Iterator;

public class ConfigurationGenerator implements Iterator<ModelConfiguration> {

	private final int[] statesEnvironment={10, 100, 1000, 10000};
//	private final int initStatesEnvironment = 10;
//	private final int incrementStatesEnvironment = 10;
//	private final int finalStatesEnvironment = 1000;

//	private final int initStatesController = 50;
//	private final int incrementalStatesController = 50;
//	private final int finalStatesController = 300;

	private final int[] statesController={10, 50, 100, 250, 500, 750, 1000};
//	private final int initStatesController = 200;
//	private final int incrementalStatesController = 200;
//	private final int finalStatesController = 1000;
	
//	private final int environmentTransitionRatio = 15;
//	private final int controllerTransitionRatio = 22;

	private final int environmentTransitionRatio = 10;
	private final int controllerTransitionRatio = 10;


	private int indexCurrentStateEnvironment=0;
	private int indexCurrentStateController=0;
	private int currentStatesEnvironment = statesEnvironment[0];
	private int currentStatesController = statesController[0];
	
	private boolean first = true;

	@Override
	public boolean hasNext() {
		if (indexCurrentStateEnvironment == statesEnvironment.length-1 && indexCurrentStateController == statesController.length-1) {
			return false;
		}
		return true;
	}

	@Override
	public ModelConfiguration next() {
		if (indexCurrentStateEnvironment > statesEnvironment.length) {
			throw new IllegalArgumentException("There is no next");
		}
			if (this.indexCurrentStateController < statesController.length) {
				this.currentStatesController = this.statesController[this.indexCurrentStateController];
				this.indexCurrentStateController++;
			} else {
				this.indexCurrentStateEnvironment++;
				this.currentStatesEnvironment = this.statesEnvironment[this.indexCurrentStateEnvironment] ;
				this.indexCurrentStateController=0;
			}
		
		int transitionsEnvironment = this.currentStatesEnvironment
				* this.environmentTransitionRatio;
		// int eventsEnvironment = (int) Math
		// .round(110.0 * this.currentStatesEnvironment / 10000);
		int eventsEnvironment = 10+this.statesController[2]*2;
		int transitionsController = this.currentStatesController * this.controllerTransitionRatio;
		int eventsController = eventsEnvironment;
				

		int eventsControllerInterface = (int) Math.round(eventsController/100*25);
		return new ModelConfiguration(this.currentStatesEnvironment, transitionsEnvironment, eventsEnvironment,
				currentStatesController, transitionsController, eventsController, eventsControllerInterface);
	}
}