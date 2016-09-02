package scalabilityAssessment.experiment1.modelchecker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.toba.LTL2BA;
import ltsa.ui.EmptyLTSOuput;
import ltsa.ui.StandardOutput;
import scalabilityAssessment.modelgenerator.ModelConfiguration;
import scalabilityAssessment.modelgenerator.RandomLTSGenerator;
import scalabilityAssessment.propertygenerator.PropertyGenerator;

public class ModelCheckerTest implements Callable<Void> {

	public static final SimpleDateFormat time_formatter = new SimpleDateFormat(
			"HH:mm:ss.SSS");

	private File outputFile;

	int testNumber;
	private final ModelConfiguration c;
	private final int propertyOfInterest;

	public ModelCheckerTest(File outputFile, int testNumber,
			ModelConfiguration c, int propertyOfInterest) {
		this.outputFile = outputFile;
		this.testNumber = testNumber;
		this.c = c;
		this.propertyOfInterest = propertyOfInterest;
	}

	@Override
	public Void call() {

		long start;
		long end;

		try {
			PredicateDefinition.init();
			Writer outputWriter;
			outputWriter = new FileWriter(outputFile, true);

			int numberOfStates = c.getStatesEnviromnet();
			System.out.println("__________________________________________");
			System.out.println("STATES: " + numberOfStates);

			System.out
					.println(time_formatter.format(System.currentTimeMillis())
							+ "- Generating the environment.....");
			start = System.currentTimeMillis();
			RandomLTSGenerator randomLTSGenerator = new RandomLTSGenerator(
					c.getStatesEnviromnet(), c.getEventsEnvironment(),
					c.getTransitionPerStateEnvironment());
			LabelledTransitionSystem environment = randomLTSGenerator
					.getRandomLTS("PROVA");
			end = System.currentTimeMillis();
			System.out.println("END- Environment generated in: "
					+ ((end - start) / 1000) + "s");
			
			List<String> eventsEnvironment=new ArrayList<>(environment.getAlphabetCharacters());
			
			Collections.shuffle(eventsEnvironment);
			String event1 = eventsEnvironment.get(0);
			String event2 = eventsEnvironment.get(1);

			System.out
					.println(time_formatter.format(System.currentTimeMillis())
							+ "- Generating the controller.....");
			start = System.currentTimeMillis();

			RandomLTSGenerator finalControllerGenerator = new RandomLTSGenerator(
					c.getStatesController(), c.getEventsController(),
					c.getTransitionsPerStateController());
			LabelledTransitionSystem finalController = finalControllerGenerator
					.getRandomLTS("CONTROLLER");
			end = System.currentTimeMillis();
			System.out.println("END- Final controller generated in: "
					+ ((end - start) / 1000) + "s");
			System.out.println("Final controller states: "
					+ finalController.getStates().length + " transitions: "
					+ finalController.getTransitionNumber());

			// check environment + controller VS final property

			PropertyGenerator pg = new PropertyGenerator(
					environment.getAlphabetCharacters(), event1, event2);
			Formula ltlFormula = pg.getFormulae().get(propertyOfInterest);

			System.out.println("Formula: "+ltlFormula);
			long verificationTime = checkFinalController(environment,
					finalController, ltlFormula);

			outputWriter.write("P" + propertyOfInterest + "\t" + testNumber + "\t"
					+ c.toString() + "\t"
					+ verificationTime + "\t"
					+ "\n");

			printMessage("P" + propertyOfInterest + "\t" + testNumber + "\t"
					+ c.toString() + "\t"
					+ verificationTime + "\t"
					+ "\n");
			outputWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	private long checkFinalController(LabelledTransitionSystem environment,
			LabelledTransitionSystem finalController, Formula ltlFormula) {

		long start;
		long end;
		Vector<LabelledTransitionSystem> machines = new Vector<>();
		machines.add(environment);
		machines.add(finalController);

		CompositeState system = new CompositeState(machines);

		printMessage(time_formatter.format(System.currentTimeMillis())
				+ "-Converting the properties into a LTS.....");
		start = System.currentTimeMillis();
		CompositeState formula = new LTL2BA(new EmptyLTSOuput())
				.getCompactState("property", ltlFormula,
						system.getComponentAlphabet());
		printMessage("DIMENSION: "
				+ formula.getComposition().getStates().length);

		printMessage("DIMENSION of the formula: states: "
				+ formula.getComposition().getStates().length
				+ " transitions: "
				+ formula.getComposition().getTransitionNumber());

		end = System.currentTimeMillis();
		long propertyConvertionTime = (end - start);
		printMessage("END- Properties convertend in: ", start, end);

		
		
		String environmentSize = "environment states:"
				+ environment.getStates().length+" transitions: "+environment.getTransitionNumber();
		
		String controllerSize = "controller states:"
				+ finalController.getStates().length
				+ "  transitions: "
				+ finalController.getTransitionNumber();
		String propertySize = "property states: "
				+ formula.getComposition().getStates().length + " property transitions: "
				+ formula.getComposition().getTransitionNumber();
		System.out.println(environmentSize+"\t"+controllerSize+"\t"+propertySize);
		printMessage(time_formatter.format(System.currentTimeMillis())
				+ "-Checking the properties...." + environmentSize + "\t"
				+ controllerSize);
		
		start = System.currentTimeMillis();
		
		
		
		system.checkLTL(new EmptyLTSOuput(), formula);
		end = System.currentTimeMillis();
		printMessage("END- Properties checked into: ", start, end);
				
		long verification = (end - start);
		return propertyConvertionTime + verification;

	}
	
	private void printMessage(String message) {
		System.out.println(time_formatter.format(System.currentTimeMillis())
				+ " - " + message);
	}

	private void printMessage(String message, long init, long end) {
		System.out.println(time_formatter.format(System.currentTimeMillis())
				+ " - " + message + " [" + (end - init) + " ms]");

	}

}
