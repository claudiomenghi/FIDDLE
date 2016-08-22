package scalabilityAssessment.experiment1.wellformedness;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
import ltsa.lts.ltl.formula.Proposition;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.ltl.toba.LTL2BA;
import ltsa.lts.parser.Def;
import ltsa.lts.parser.PostconditionDefinitionManager;
import ltsa.lts.parser.PreconditionDefinitionManager;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.actions.LabelSet;
import ltsa.ui.EmptyLTSOuput;
import scalabilityAssessment.modelgenerator.RandomControllerGenerator;
import scalabilityAssessment.modelgenerator.RandomLTSGenerator;
import scalabilityAssessment.modelgenerator.Size;
import scalabilityAssessment.postconditiongenerator.PostConditionGenerator;
import scalabilityAssessment.preconditionGenerator.PreconditionGenerator;
import scalabilityAssessment.propertygenerator.PropertyGenerator;

public class WellFormednessCheckerTest implements Callable<Void> {

	public static final SimpleDateFormat time_formatter = new SimpleDateFormat(
			"HH:mm:ss.SSS");

	private Size[] sizes = Size.values();

	private File outputFile;

	private int testNumber;

	public WellFormednessCheckerTest(File outputFile, int testNumber) {
		this.outputFile = outputFile;
		this.testNumber = testNumber;
	}

	@Override
	public Void call() {

		long start;
		long end;

		int propertyOfInterest = 1;
		int postConditionOfInterest = 0;

		for (int i = 0; i < sizes.length; i++) {

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

				int numberOfStates = sizes[i].getNumberOfStates();
				System.out
						.println("__________________________________________");
				System.out.println("STATES: " + numberOfStates);

				printMessage("Generating the environment.....");
				start = System.currentTimeMillis();
				LabelledTransitionSystem environment = new RandomLTSGenerator(
						sizes[i].getNumberOfStates(),
						sizes[i].getNumberOfEvents(),
						sizes[i].getTransitionsPerState())
						.getRandomLTS("ENVIRONMENT");
				end = System.currentTimeMillis();
				printMessage("END- Environment generated in:", start, end);
				String event1 = environment.getAlphabetCharacters().get(0);
				String event2 = environment.getAlphabetCharacters().get(1);
				printMessage("Generating the partial controller.....");
				start = System.currentTimeMillis();
				LabelledTransitionSystem partialController = new RandomControllerGenerator()
						.getComponent(sizes[i]);

				List<String> alphabet = new ArrayList<>();
				alphabet.addAll(environment.getAlphabetCharacters());
				alphabet.addAll(partialController.getAlphabetCharacters());

				end = System.currentTimeMillis();
				printMessage("END- Partial controller generated in: ", start,
						end);

				String boxOfInterest = partialController.getBoxIndexes()
						.keySet().iterator().next();

				PostConditionGenerator postConditionGen = new PostConditionGenerator(
						new ArrayList<>(
								partialController.mapBoxInterface
										.get(boxOfInterest)), event1, event2);
				Formula postConditionFormula = postConditionGen.getFormulae()
						.get(postConditionOfInterest);

				printMessage("Converting the post condition in automaton....");
				start = System.currentTimeMillis();
				Map<String, LabelledTransitionSystem> mapBoxPostCondition = new HashMap<>();
				System.out.println("post-condition size.. "+partialController.mapBoxInterface
				.get("box").size());
				LabelledTransitionSystem postConditionLTS = new LTLf2LTS()
						.toLTS(postConditionFormula,
								new EmptyLTSOuput(),
								new ArrayList<>(
										partialController.mapBoxInterface
												.get("box")), "post");
				end = System.currentTimeMillis();
				long step1ConvertionOfThePostCondition = end - start;
				printMessage("END- Post condition converted in: ", start, end);

				mapBoxPostCondition.put(boxOfInterest, postConditionLTS);

				start = System.currentTimeMillis();
				partialController = new WellFormednessLTSModifier(
						new EmptyLTSOuput()).modify(partialController,
						mapBoxPostCondition, true, boxOfInterest);
				end = System.currentTimeMillis();
				long step2Integration = end - start;
				printMessage("Integrating post in partial controller: ", start,
						end);

				printMessage("Transforming the pre....");
				start = System.currentTimeMillis();

				PropertyGenerator pg = new PropertyGenerator(
						environment.getAlphabetCharacters(), event1, event2);
				Formula ltlFormula = pg.getFormulae().get(propertyOfInterest);

				// long verificationTime = checkFinalController(environment,
				// partialController, ltlFormula);

				checkWellFormedness(start, outputWriter, numberOfStates,
						environment, event1, event2, partialController,
						alphabet, step1ConvertionOfThePostCondition,
						step2Integration, 1);

				outputWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;

	}

	private void checkWellFormedness(long start, Writer outputWriter,
			int numberOfStates, LabelledTransitionSystem environment,
			String event1, String event2,
			LabelledTransitionSystem partialController, List<String> alphabet,
			long step1ConvertionOfThePostCondition, long step2Integration,
			int propertyOfInterest) throws IOException {
		long end;
		Formula fltlPrecondition = new PreconditionGenerator(alphabet, event1,
				event2).getFormulae().get(propertyOfInterest);
		System.out.println(fltlPrecondition);
		CompositeState precondition = new LTLf2LTS()
				.toProperty(fltlPrecondition, new EmptyLTSOuput(), alphabet,
						"precondition");

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
				+ environment.getStates().length;
		String controllerSize = "partial controller states:"
				+ partialController.getStates().length
				+ " partial controller transitions: "
				+ partialController.getTransitionNumber();
		printMessage("Well-formedness checker....\t" + environmentSize + "\t"
				+ controllerSize);

		start = System.currentTimeMillis();
		system.checkLTL(new EmptyLTSOuput(), precondition);
		end = System.currentTimeMillis();
		long checkingThePre = end - start;
		printMessage("END- Well-formedness checker ", start, end);

		long wellFormednessChecking = step1ConvertionOfThePostCondition
				+ step2Integration + loadingThePrecondition + checkingThePre;

		outputWriter.write(testNumber + "\t" + numberOfStates + "\t"
				+ wellFormednessChecking + "\t"
				+ step1ConvertionOfThePostCondition + "\t" + step2Integration
				+ "\t" + (loadingThePrecondition + checkingThePre) + "\n");

		System.out.println(testNumber + "\t" + numberOfStates + "\t"
				+ wellFormednessChecking + "\t"
				+ step1ConvertionOfThePostCondition + "\t" + step2Integration
				+ "\t" + (loadingThePrecondition + checkingThePre));
	}

	private long checkFinalController(LabelledTransitionSystem environment,
			LabelledTransitionSystem finalController, Formula ltlFormula) {
		

		long start;
		long end;
		Vector<LabelledTransitionSystem> machines = new Vector<>();
		machines.add(environment);
		machines.add(finalController);

		CompositeState system = new CompositeState(machines);

		System.out.println(time_formatter.format(System.currentTimeMillis())
				+ "-Converting the properties into a LTS.....");
		start = System.currentTimeMillis();
		CompositeState formula = new LTL2BA(new EmptyLTSOuput())
				.getCompactState("property", ltlFormula,
						system.getComponentAlphabet());
		System.out.println("DIMENSION: "
				+ formula.getComposition().getStates().length);

		System.out.println("DIMENSION of the formula: states: "
				+ formula.getComposition().getStates().length
				+ " transitions: "
				+ formula.getComposition().getTransitionNumber());
		
		end = System.currentTimeMillis();
		long propertyConvertionTime = (end - start);
		System.out.println("END- Properties convertend in: "
				+ propertyConvertionTime);

		String environmentSize = "environment states: "
				+ environment.getStates().length+" environment transitions: "+environment.getTransitionNumber();
		String controllerSize = "controller states: "
				+ finalController.getStates().length+" controller transitions: "+finalController.getTransitionNumber();
		System.out.println(time_formatter.format(System.currentTimeMillis())
				+ "-Checking the properties...." + environmentSize + "\t"
				+ controllerSize);
		start = System.currentTimeMillis();
		system.checkLTL(new EmptyLTSOuput(), formula);
		end = System.currentTimeMillis();
		System.out.println("END- Properties checked into: " + ((end - start))
				+ "s");
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
