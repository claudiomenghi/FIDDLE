package scalabilityAssessment.experiment2.subcontrollerchecker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;

import ltsa.control.ControlStackDefinition;
import ltsa.control.ControllerDefinition;
import ltsa.control.ControllerGoalDefinition;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.chart.TriggeredScenarioDefinition;
import ltsa.lts.checkers.substitutability.SubstitutabilityChecker;
import ltsa.lts.distribution.DistributionDefinition;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.parser.Def;
import ltsa.lts.parser.actions.LabelSet;
import ltsa.ui.EmptyLTSOuput;
import scalabilityAssessment.MessageHandler;
import scalabilityAssessment.modelgenerator.ModelConfiguration;
import scalabilityAssessment.modelgenerator.RandomLTSGenerator;
import scalabilityAssessment.modelgenerator.SubcontrollerGenerator;
import scalabilityAssessment.postconditiongenerator.PostConditionGenerator;
import scalabilityAssessment.preconditionGenerator.PreconditionGenerator;

public class SubcontrollerCheckerTest implements Callable<Void> {

	private final File outputFile;
	private final int testNumber;
	private final ModelConfiguration c;
	private final int propertyOfInterest;

	public SubcontrollerCheckerTest(File outputFile, int testNumber,
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

			MessageHandler.printMessage("Generating the environment.....");
			start = System.currentTimeMillis();
			LabelledTransitionSystem environment = new RandomLTSGenerator(
					c.getStatesEnviromnet(), c.getEventsEnvironment(),
					c.getTransitionPerStateEnvironment())
					.getRandomLTS("ENVIRONMENT");
			end = System.currentTimeMillis();
			MessageHandler.printMessage("END- Environment generated in:",
					start, end);
			List<String> eventsEnvironment = new ArrayList<>(
					environment.getAlphabetEvents());

			Collections.shuffle(eventsEnvironment);
			String event1 = eventsEnvironment.get(0);
			String event2 = eventsEnvironment.get(1);
			MessageHandler.printMessage("Generating the controller.....");
			start = System.currentTimeMillis();
			LabelledTransitionSystem partialController = new RandomLTSGenerator(
					c.getStatesController(), c.getEventsController(),
					c.getTransitionsPerStateController())
					.getRandomLTS("CONTROLLER");
			end = System.currentTimeMillis();
			MessageHandler.printMessage("END- Controller generated in:", start,
					end);

			MessageHandler.printMessage("Generating the sub-controller.....");
			start = System.currentTimeMillis();
			LabelledTransitionSystem subController = new SubcontrollerGenerator(
					c.getStatesController() / 2)
					.subController(partialController);
			end = System.currentTimeMillis();

			MessageHandler.printMessage("END- Sub-Controller generated in:",
					start, end);
			List<String> alphabet = new ArrayList<>();

			alphabet.addAll(environment.getAlphabetEvents());
			alphabet.addAll(partialController.getAlphabetEvents());

			PostConditionGenerator postConditionGen = new PostConditionGenerator(
					subController.getAlphabetEvents(), event1, event2);

			Formula postConditionFormula = postConditionGen.getFormulae().get(
					postConditionOfInterest);
			Formula fltlPrecondition = new PreconditionGenerator(alphabet,
					event1, event2).getFormulae().get(propertyOfInterest);
			System.out.println(fltlPrecondition);

			long init = System.currentTimeMillis();

			CompositeState environmentState=new CompositeState("ENVIRONMENT");
			environmentState.addMachine(environment);
			SubstitutabilityChecker subchecker = new SubstitutabilityChecker(new EmptyLTSOuput(),
					environmentState, subController, 
					fltlPrecondition,"PRECONDITION", postConditionFormula,  "POSTCONDITION");
			subchecker.check();
			end = System.currentTimeMillis();

			outputWriter.write("P" + propertyOfInterest + "\t" + testNumber
					+ "\t" + c.toString() + "\t" + (end - init) + "\n");

			System.out.println("P" + propertyOfInterest + "\t" + testNumber
					+ "\t" + c.toString() + "\t" + (end - init) + "\n");

			outputWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
