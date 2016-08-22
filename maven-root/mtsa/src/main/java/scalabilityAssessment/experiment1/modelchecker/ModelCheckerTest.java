package scalabilityAssessment.experiment1.modelchecker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.concurrent.Callable;

import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.Proposition;
import ltsa.lts.ltl.toba.LTL2BA;
import ltsa.lts.parser.Symbol;
import ltsa.ui.EmptyLTSOuput;
import scalabilityAssessment.modelgenerator.RandomLTSGenerator;
import scalabilityAssessment.modelgenerator.Size;
import scalabilityAssessment.propertygenerator.PropertyGenerator;

public class ModelCheckerTest implements Callable<Void> {

	public static final SimpleDateFormat time_formatter = new SimpleDateFormat(
			"HH:mm:ss.SSS");

	Size[] sizes = Size.values();

	private File outputFile;

	int testNumber;

	public ModelCheckerTest(File outputFile, int testNumber) {
		this.outputFile = outputFile;
		this.testNumber = testNumber;
	}

	@Override
	public Void call() {

		long start;
		long end;

		int formulaIndex = 1;

		for (int i = 0; i < sizes.length; i++) {

			try {
				PredicateDefinition.init();
				Writer outputWriter;
				outputWriter = new FileWriter(outputFile, true);

				int numberOfStates = sizes[i].getNumberOfStates();
				System.out
						.println("__________________________________________");
				System.out.println("STATES: " + numberOfStates);

				System.out.println(time_formatter.format(System
						.currentTimeMillis())
						+ "- Generating the environment.....");
				start = System.currentTimeMillis();
				RandomLTSGenerator randomLTSGenerator = new RandomLTSGenerator(
						sizes[i].getNumberOfStates(),
						sizes[i].getNumberOfEvents(),
						sizes[i].getTransitionsPerState());
				LabelledTransitionSystem environment = randomLTSGenerator
						.getRandomLTS("PROVA");
				end = System.currentTimeMillis();
				System.out.println("END- Environment generated in: "
						+ ((end - start) / 1000) + "s");
				String event1 = environment.getAlphabetCharacters().get(0);
				String event2 = environment.getAlphabetCharacters().get(1);

				System.out.println(time_formatter.format(System
						.currentTimeMillis())
						+ "- Generating the controller.....");
				start = System.currentTimeMillis();

				RandomLTSGenerator finalControllerGenerator = new RandomLTSGenerator(
						sizes[i].getControllerStates(),
						sizes[i].getControllerEvents(),
						sizes[i].getTransitionsPerState());
				LabelledTransitionSystem finalController = finalControllerGenerator
						.getRandomLTS("CONTROLLER");
				end = System.currentTimeMillis();
				System.out.println("END- Final controller generated in: "
						+ ((end - start) / 1000) + "s");
				System.out.println("Final controller states: "+finalController.getStates().length+" transitions: "+finalController.getTransitionNumber());

				// check environment + controller VS final property

				PropertyGenerator pg = new PropertyGenerator(
						environment.getAlphabetCharacters(), event1, event2);
				Formula ltlFormula = pg.getFormulae().get(formulaIndex);

				long verificationTime = checkFinalController(environment,
						finalController, ltlFormula);

				outputWriter.write("P" + (formulaIndex + 1) + "\t" + testNumber
						+ "\t" + numberOfStates + "\t" + verificationTime
						+ "\t"
						// + wellFormednessChecking
						// + "\t" + step1ConvertionOfThePostCondition + "\t"
						// + step2Integration + "\t"
						// + (loadingThePrecondition + checkingThePre)
						+ "\n");

				System.out.println("P" + (formulaIndex + 1) + testNumber + "\t"
						+ numberOfStates + "\t" + verificationTime + "\t"
				// + wellFormednessChecking
				// + "\t" + step1ConvertionOfThePostCondition + "\t"
				// + step2Integration + "\t"
				// + (loadingThePrecondition + checkingThePre)
						);
				outputWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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

		//System.out.println(time_formatter.format(System.currentTimeMillis())
		//		+ "-Computing teh composition.....");
		//system.compose(new EmptyLTSOuput());
		//System.out.println("system states: "+system.getComposition().getStates().length+""
		//		+ "transitions "+system.getComposition().getTransitionNumber());
		System.out.println(time_formatter.format(System.currentTimeMillis())
				+ "-Converting the properties into a LTS.....");
		start = System.currentTimeMillis();
		CompositeState formula = new LTL2BA(new EmptyLTSOuput())
				.getCompactState("property", ltlFormula,
						system.getComponentAlphabet());
		end = System.currentTimeMillis();
		long propertyConvertionTime = (end - start);
		System.out.println("END- Properties convertend in: "
				+ propertyConvertionTime);

		String environmentSize = "environment states: "
				+ environment.getStates().length + " environment transitions: "
				+ environment.getTransitionNumber();
		String controllerSize = "controller states: "
				+ finalController.getStates().length
				+ " controller transitions: "
				+ finalController.getTransitionNumber();
		String propertySize = "property states: "
				+ formula.getComposition().getStates().length
				+ " property transitions: "
				+ formula.getComposition().getTransitionNumber();
		System.out.println(time_formatter.format(System.currentTimeMillis())
				+ "-Checking the properties...." + environmentSize + "\t"
				+ controllerSize + "\t" + propertySize);
		start = System.currentTimeMillis();
		system.checkLTL(new EmptyLTSOuput(), formula);
		end = System.currentTimeMillis();
		System.out.println("END- Properties checked into: " + ((end - start))
				+ "s");
		long verification = (end - start);
		return propertyConvertionTime + verification;

	}

}
