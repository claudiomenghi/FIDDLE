package scalabilityAssessment.experiment1.wellformedness;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;

import ltsa.control.ControlStackDefinition;
import ltsa.control.ControllerDefinition;
import ltsa.control.ControllerGoalDefinition;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.chart.TriggeredScenarioDefinition;
import ltsa.lts.checkers.wellformedness.WellFormednessLTSModifier;
import ltsa.lts.distribution.DistributionDefinition;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.parser.Def;
import ltsa.lts.parser.actions.LabelSet;
import ltsa.ui.EmptyLTSOuput;
import scalabilityAssessment.modelgenerator.ModelConfiguration;
import scalabilityAssessment.modelgenerator.RandomControllerGenerator;
import scalabilityAssessment.modelgenerator.RandomLTSGenerator;
import scalabilityAssessment.postconditiongenerator.PostConditionGenerator;
import scalabilityAssessment.preconditionGenerator.PreconditionGenerator;

public class WellFormednessCheckerTest implements Callable<Void> {

	public static final SimpleDateFormat time_formatter = new SimpleDateFormat(
			"HH:mm:ss.SSS");

	private File outputFile;

	private int testNumber;

	private final ModelConfiguration c;
	private final int propertyOfInterest;

	public WellFormednessCheckerTest(File outputFile, int testNumber,
			ModelConfiguration size, int propertyOfInterest) {
		this.outputFile = outputFile;
		this.testNumber = testNumber;
		this.c = size;
		this.propertyOfInterest = propertyOfInterest;
	}

	@Override
	public Void call() {

		long start;
		long end;

		int num = 0;

		System.out.println("Number of configurations environment" + num);

		int postConditionOfInterest = 0;

		try {
			Def.init();
			PredicateDefinition.init();
			AssertDefinition.init();
			TriggeredScenarioDefinition.init();
			ControllerDefinition.init();
			LabelSet.constants = new Hashtable<>();
			ControllerGoalDefinition.init();
			ControlStackDefinition.initDefinitionList();
			DistributionDefinition.init();

			Writer outputWriter = new FileWriter(outputFile, true);

			int numberOfStates = c.getStatesEnviromnet();
			System.out.println("__________________________________________");
			System.out.println("STATES: " + numberOfStates);

			printMessage("Generating the environment.....");
			start = System.currentTimeMillis();
			LabelledTransitionSystem environment = new RandomLTSGenerator(
					c.getStatesEnviromnet(), c.getEventsEnvironment(),
					c.getTransitionPerStateEnvironment())
					.getRandomLTS("ENVIRONMENT");
			end = System.currentTimeMillis();
			printMessage("END- Environment generated in:", start, end);
			List<String> eventsEnvironment = new ArrayList<>(
					environment.getAlphabetEvents());

			Collections.shuffle(eventsEnvironment);

			printMessage("Generating the partial controller.....");
			start = System.currentTimeMillis();
			LabelledTransitionSystem partialController = new RandomControllerGenerator()
					.getComponent(c);

			List<String> alphabet = new ArrayList<>();
			alphabet.addAll(environment.getAlphabetEvents());
			alphabet.addAll(partialController.getAlphabetEvents());

			end = System.currentTimeMillis();
			printMessage("END- Partial controller generated in: ", start, end);

			String boxOfInterest = partialController.getBoxIndexes().keySet()
					.iterator().next();

			List<String> controllerAlphabet = new ArrayList<>(
					partialController.getBoxInterface(boxOfInterest));
			Collections.shuffle(controllerAlphabet);
			String event1 = controllerAlphabet.get(0);
			String event2 = controllerAlphabet.get(1);

			PostConditionGenerator postConditionGen = new PostConditionGenerator(
					new ArrayList<>(
							partialController.getBoxInterface(boxOfInterest)),
					event1, event2);
			Formula postConditionFormula = postConditionGen.getFormulae().get(
					postConditionOfInterest);

			printMessage("Converting the post condition in automaton....");
			start = System.currentTimeMillis();
			Map<String, LabelledTransitionSystem> mapBoxPostCondition = new HashMap<>();
			System.out.println("post-condition size.. "
					+ partialController.getBoxInterface("box").size());

			LabelledTransitionSystem postConditionLTS = new LTLf2LTS().toLTS(
					postConditionFormula, new EmptyLTSOuput(), new HashSet<>(
							partialController.getBoxInterface("box")), "post");
			end = System.currentTimeMillis();
			long step1ConvertionOfThePostCondition = end - start;
			printMessage("END- Post condition converted in: ", start, end);

			mapBoxPostCondition.put(boxOfInterest, postConditionLTS);

			start = System.currentTimeMillis();
			partialController = new WellFormednessLTSModifier(
					new EmptyLTSOuput()).modify(partialController, boxOfInterest);
			end = System.currentTimeMillis();
			long step2Integration = end - start;
			printMessage("Integrating post in partial controller: ", start, end);

			printMessage("Transforming the pre....");
			start = System.currentTimeMillis();

			/*
			 * LabelledTransitionSystem partialController = new
			 * RandomLTSGenerator( c.getStatesController(),
			 * c.getEventsController(), c.getTransitionsPerStateController())
			 * .getRandomLTS("CONTROLLER");
			 */
			checkWellFormedness(start, outputWriter, numberOfStates,
					environment, event1, event2, partialController, alphabet,
					step1ConvertionOfThePostCondition, step2Integration,
					propertyOfInterest, c);

			outputWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void checkWellFormedness(long start, Writer outputWriter,
			int numberOfStates, LabelledTransitionSystem environment,
			String event1, String event2,
			LabelledTransitionSystem partialController, List<String> alphabet,
			long step1ConvertionOfThePostCondition, long step2Integration,
			int propertyOfInterest, ModelConfiguration c) throws IOException {
		long end;
		List<String> newAlphaPre = new ArrayList<>(alphabet);
		newAlphaPre.remove(event1);
		newAlphaPre.remove(event2);
		Formula fltlPrecondition = new PreconditionGenerator(newAlphaPre,
				event1, event2).getFormulae().get(propertyOfInterest);
		System.out.println(fltlPrecondition);
		// TODO change
		CompositeState precondition = new LTLf2LTS().toProperty(
				fltlPrecondition, new EmptyLTSOuput(), 
				new HashSet<String>(newAlphaPre),
				"precondition");
		// CompositeState precondition = new LTL2BA(new EmptyLTSOuput())
		// .getCompactState("property", fltlPrecondition, new Vector<>(
		// newAlphaPre));

		// CompositeState precondition =new
		// LTLf2LTS().toPropertyWithInit(fltlPrecondition, new EmptyLTSOuput(),
		// newAlphaPre, "AAA");
		System.out.println("DIMENSION of the precondition: states: "
				+ precondition.getComposition().getStates().length
				+ " transitions: "
				+ precondition.getComposition().getTransitionNumber());
		end = System.currentTimeMillis();
		long loadingThePrecondition = end - start;
		printMessage("END- Transforming the pre ", start, end);

		Vector<LabelledTransitionSystem> machines = new Vector<>();
		machines.add(partialController);
		machines.add(environment);
		CompositeState system = new CompositeState(machines);

		String environmentSize = "environment states:"
				+ environment.getStates().length + " transitions: "
				+ environment.getTransitionNumber();

		String controllerSize = "partial controller states:"
				+ partialController.getStates().length + "  transitions: "
				+ partialController.getTransitionNumber();
		String propertySize = "property states: "
				+ precondition.getComposition().getStates().length
				+ " property transitions: "
				+ precondition.getComposition().getTransitionNumber();

		printMessage("Well-formedness checker....\t" + environmentSize + "\t"
				+ controllerSize + "\t" + propertySize);

		start = System.currentTimeMillis();
		system.checkLTL(new EmptyLTSOuput(), precondition);
		end = System.currentTimeMillis();
		long checkingThePre = end - start;
		printMessage("END- Well-formedness checker ", start, end);

		long wellFormednessChecking = step1ConvertionOfThePostCondition
				+ step2Integration + loadingThePrecondition + checkingThePre;

		outputWriter.write("P" + propertyOfInterest + "\t" + testNumber + "\t"
				+ c.toString() + "\t" + environment.getStates().length + "\t"
				+ environment.getTransitionNumber() + "\t"
				+ partialController.getStates().length + "\t"
				+ partialController.getTransitionNumber() + "\t"
				+ precondition.getComposition().getStates().length + "\t"
				+ precondition.getComposition().getTransitionNumber() + "\t"
				+ step1ConvertionOfThePostCondition + "\t" + step2Integration
				+ "\t" + (loadingThePrecondition + checkingThePre) + "\t"
				+ wellFormednessChecking + "\n");

		System.out.println("P" + propertyOfInterest + "\t" + testNumber + "\t"
				+ c.toString() + "\t" + wellFormednessChecking + "\t"
				+ step1ConvertionOfThePostCondition + "\t" + step2Integration
				+ "\t" + (loadingThePrecondition + checkingThePre));
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
