package scalabilityAssessment.modelgenerator;

import java.util.Iterator;

public class ConfigurationGenerator implements Iterator<ModelConfiguration> {

<<<<<<< HEAD
<<<<<<< HEAD
	private final int initStatesEnvironment = 10000;
	private final int incrementStatesEnvironment = 2000;
	private final int finalStatesEnvironment = 20000;
=======
	private final int[] statesEnvironment = { 10, 100, 1000};
//	private final int[] statesEnvironment = { 10000};
	
	// private final int initStatesEnvironment = 10;
	// private final int incrementStatesEnvironment = 10;
	// private final int finalStatesEnvironment = 1000;
>>>>>>> dev

	// private final int initStatesController = 50;
	// private final int incrementalStatesController = 50;
	// private final int finalStatesController = 300;

	private final int[] statesController = { 10, 50, 100, 250, 500, 750, 1000 };
	// private final int initStatesController = 200;
	// private final int incrementalStatesController = 200;
	// private final int finalStatesController = 1000;

	// private final int environmentTransitionRatio = 15;
	// private final int controllerTransitionRatio = 22;

	private final int eventsEnvironment=50;
	private final int environmentTransitionRatio = 10;
	private final int controllerTransitionRatio = 10;

	private int indexCurrentStateEnvironment = 0;
	private int indexCurrentStateController = 0;
	private int currentStatesEnvironment = statesEnvironment[0];
	private int currentStatesController = statesController[0];

	boolean first = true;

	@Override
	public boolean hasNext() {
<<<<<<< HEAD
		if (currentStatesEnvironment == finalStatesEnvironment && currentStatesController==finalStatesController) {
=======
//	private final int[] statesEnvironment = { 10, 100, 1000};
	private final int[] statesEnvironment = { 10000};
	
	// private final int initStatesEnvironment = 10;
	// private final int incrementStatesEnvironment = 10;
	// private final int finalStatesEnvironment = 1000;

	// private final int initStatesController = 50;
	// private final int incrementalStatesController = 50;
	// private final int finalStatesController = 300;

	private final int[] statesController = { 10, 50, 100, 250, 500, 750, 1000 };
	// private final int initStatesController = 200;
	// private final int incrementalStatesController = 200;
	// private final int finalStatesController = 1000;

	// private final int environmentTransitionRatio = 15;
	// private final int controllerTransitionRatio = 22;

	private final int eventsEnvironment=50;
	private final int environmentTransitionRatio = 10;
	private final int controllerTransitionRatio = 10;

	private int indexCurrentStateEnvironment = 0;
	private int indexCurrentStateController = 0;
	private int currentStatesEnvironment = statesEnvironment[0];
	private int currentStatesController = statesController[0];

	boolean first = true;

	@Override
	public boolean hasNext() {
		if (indexCurrentStateEnvironment == statesEnvironment.length - 1
				&& indexCurrentStateController == statesController.length - 1) {
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
		if (indexCurrentStateEnvironment == statesEnvironment.length - 1
				&& indexCurrentStateController == statesController.length - 1) {
>>>>>>> dev
			return false;
		}
		return true;
	}

	@Override
	public ModelConfiguration next() {
<<<<<<< HEAD
<<<<<<< HEAD
		if (currentStatesEnvironment > finalStatesEnvironment) {
			throw new IllegalArgumentException("There is no next");
		}
=======
>>>>>>> dev
		if (first) {
			first = false;
		} else {
			if (indexCurrentStateEnvironment > statesEnvironment.length) {
				throw new IllegalArgumentException("There is no next");
			}
			if (this.indexCurrentStateController < statesController.length-1) {
				this.indexCurrentStateController++;
				this.currentStatesController = this.statesController[this.indexCurrentStateController];
				this.currentStatesEnvironment = this.statesEnvironment[this.indexCurrentStateEnvironment];

				
			} else {
				this.indexCurrentStateEnvironment++;
				this.indexCurrentStateController = 0;
				this.currentStatesController=this.statesController[0];
				this.currentStatesEnvironment = this.statesEnvironment[this.indexCurrentStateEnvironment];
				
			}
		}

		int transitionsEnvironment = this.currentStatesEnvironment * this.environmentTransitionRatio;
		// int eventsEnvironment = (int) Math
		// .round(110.0 * this.currentStatesEnvironment / 10000);
		int transitionsController = this.currentStatesController * this.controllerTransitionRatio;
		int eventsComponent = eventsEnvironment;

		int eventsComponentInterface = (int) Math.round(eventsComponent / 100.0 * 25);
		return new ModelConfiguration(this.currentStatesEnvironment, transitionsEnvironment, eventsEnvironment,
				currentStatesController, transitionsController, eventsComponent, eventsComponentInterface);
	}
<<<<<<< HEAD

=======
		if (first) {
			first = false;
		} else {
			if (indexCurrentStateEnvironment > statesEnvironment.length) {
				throw new IllegalArgumentException("There is no next");
			}
			if (this.indexCurrentStateController < statesController.length-1) {
				this.indexCurrentStateController++;
				this.currentStatesController = this.statesController[this.indexCurrentStateController];
				this.currentStatesEnvironment = this.statesEnvironment[this.indexCurrentStateEnvironment];

				
			} else {
				this.indexCurrentStateEnvironment++;
				this.indexCurrentStateController = 0;
				this.currentStatesController=this.statesController[0];
				this.currentStatesEnvironment = this.statesEnvironment[this.indexCurrentStateEnvironment];
				
			}
		}

		int transitionsEnvironment = this.currentStatesEnvironment * this.environmentTransitionRatio;
		// int eventsEnvironment = (int) Math
		// .round(110.0 * this.currentStatesEnvironment / 10000);
		int transitionsController = this.currentStatesController * this.controllerTransitionRatio;
		int eventsComponent = eventsEnvironment;

		int eventsComponentInterface = (int) Math.round(eventsComponent / 100.0 * 25);
		return new ModelConfiguration(this.currentStatesEnvironment, transitionsEnvironment, eventsEnvironment,
				currentStatesController, transitionsController, eventsComponent, eventsComponentInterface);
	}
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
=======
>>>>>>> dev
}