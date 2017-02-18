package scalabilityAssessment.modelgenerator;

public class ModelConfiguration {

	private final int statesEnviromnet;
	private final int transitionsEnvironment;
	private final int eventsEnvironment;
	private final int statesController;
	private final int transitionsController;
	private final int eventsController;
	private final int eventsControllerInterface;
	private final int transitionPerStateEnvironment;
	private final int transitionsPerStateController;

	public ModelConfiguration(int statesEnviromnet, int transitionsEnvironment,
			int eventsEnvironment, int statesController,
			int transitionsController, int eventsController, int eventsControllerInterface) {
		this.statesEnviromnet=statesEnviromnet;
		this.transitionsEnvironment=transitionsEnvironment;
		this.eventsEnvironment=eventsEnvironment;
		this.statesController=statesController;
		this.transitionsController=transitionsController;
		this.eventsController=eventsController;
		this.eventsControllerInterface=eventsControllerInterface;
		this.transitionsPerStateController=this.transitionsController/this.statesController;
		this.transitionPerStateEnvironment=this.transitionsEnvironment/this.statesEnviromnet;

	}

	public int getStatesEnviromnet() {
		return statesEnviromnet;
	}

	public int getTransitionsEnvironment() {
		return transitionsEnvironment;
	}

	public int getTransitionPerStateEnvironment(){
		return this.transitionPerStateEnvironment;
	}
	public int getEventsEnvironment() {
		return eventsEnvironment;
	}

	public int getTransitionsPerStateController(){
		return transitionsPerStateController;
	}
	public int getTransitionsController() {
		return transitionsController;
	}

	public int getStatesController() {
		return statesController;
	}

	public int getEventsController() {
		return eventsController;
	}

	public int getEventsControllerInterface() {
		return eventsControllerInterface;
	}
	
	public static String getHeader(){
		return "#StatesEnvironment \t #TransitionsEnvironment \t #eventsEnvironment \t #statesController \t #transitionsController \t #eventsController \t #eventsInterface";
	}
	public String toString() {
		return this.statesEnviromnet+"\t"+this.transitionsEnvironment+"\t"+this.eventsEnvironment+"\t"+this.statesController+"\t"+this.transitionsController+"\t"+this.eventsController+"\t"+this.eventsControllerInterface;
	}
}
