package scalabilityAssessment.modelgenerator;

public enum Size implements ModelSize {

	//SMALL(1000), MEDIUM(10000),
	//LARGE(50000), 
	EXTRALARGE(100000);
	//SMALL(10000), MEDIUM(20000), LARGE(30000), EXTRALARGE(100000);

	private static final int transitionDensity = 5;
	private static final double eventDensity = 0.01;
	private static final int coefficientStatesController = 10;
	private static final double controllerInterfaceCoefficient = 0.5;
	
	private static final int coefficientEventsEnvironment = 5;

	private final int numberOfEvents;
	private final int numberOfTransitions;
	private final int size;
	private final int transitionsPerState;
	private final int controllerStaes;
	private final int controllerTransitions;
	private final int controllerEvents;
	private final int controllerEventInterface;

	Size(int size) {
		this.size = size;
		this.numberOfEvents = 50 + (int) (Math.log10(this.size) * coefficientEventsEnvironment);;
		this.numberOfTransitions = (int) (this.size * transitionDensity);
		this.transitionsPerState = transitionDensity;
		this.controllerStaes = 10 + (int) (Math.log10(this.size) * coefficientStatesController);
		this.controllerTransitions = (int) (this.controllerStaes * transitionDensity);
		this.controllerEvents = Math.max(10,
				(int) (eventDensity * this.controllerStaes));
		this.controllerEventInterface = (int) (this.controllerEvents * controllerInterfaceCoefficient);
	}

	public int getControllerStates() {
		return this.controllerStaes;
	}

	@Override
	public int getNumberOfStates() {
		return this.size;
	}

	@Override
	public int getNumberOfTransitions() {
		return this.numberOfTransitions;
	}

	@Override
	public int getNumberOfEvents() {
		return this.numberOfEvents;
	}

	public int getTransitionsPerState() {
		return transitionsPerState;
	}

	public String toString() {
		return "NumberOfStates: " + this.size + "\t NumberOfTransitions: "
				+ this.numberOfTransitions + "\t TransitionsPerStates: "
				+ transitionsPerState + "\t NumberOfEvents: "
				+ this.numberOfEvents + "\t ControllerStates: "
				+ this.controllerStaes + "\t ControllerTransitions: "
				+ this.controllerTransitions + "\t ControllerEvents: "
				+ this.controllerEvents + "\t ControllerEventsInterface: "
				+ this.controllerEventInterface;
	}

	public int getControllerTransitions() {
		return controllerTransitions;
	}

	public int getControllerEvents() {
		return controllerEvents;
	}

	public int getControllerEventInterface() {
		return controllerEventInterface;
	}
}

interface ModelSize {
	public int getNumberOfStates();

	public int getNumberOfTransitions();

	public int getNumberOfEvents();

	public int getTransitionsPerState();

}