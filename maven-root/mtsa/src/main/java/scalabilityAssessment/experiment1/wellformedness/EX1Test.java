package scalabilityAssessment.experiment1.wellformedness;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;

import ltsa.control.ControlStackDefinition;
import ltsa.control.ControllerDefinition;
import ltsa.control.ControllerGoalDefinition;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.checkers.modelchecker.ModelChecker;
import ltsa.lts.checkers.wellformedness.WellFormednessLTSModifier;
import ltsa.lts.distribution.DistributionDefinition;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.ltl.toba.LTL2BA;
import ltsa.lts.parser.Def;
import ltsa.lts.parser.actions.LabelSet;
import ltsa.ui.EmptyLTSOuput;
import scalabilityAssessment.MessageHandler;
import scalabilityAssessment.modelgenerator.ModelConfiguration;
import scalabilityAssessment.modelgenerator.RandomComponentGenerator;
import scalabilityAssessment.modelgenerator.RandomLTSGenerator;
import scalabilityAssessment.postconditiongenerator.PostConditionGenerator;
import scalabilityAssessment.preconditionGenerator.PreconditionGenerator;
import scalabilityAssessment.propertygenerator.PropertyGenerator;

public class EX1Test implements Callable<Void> {

	public static final SimpleDateFormat time_formatter = new SimpleDateFormat("HH:mm:ss.SSS");

	private File outputFile;

	private int testNumber;

	private final ModelConfiguration c;
	private final int propertyOfInterest;
	
	private CompositeState precondition;
	private boolean result;
	
	long wellFormednessChecking;
	long checkingThePre;

	public EX1Test(File outputFile, int testNumber, ModelConfiguration size, int propertyOfInterest) {
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
			LabelledTransitionSystem environment = new RandomLTSGenerator(c.getStatesEnviromnet(),
					c.getEventsEnvironment(), c.getTransitionPerStateEnvironment()).getRandomLTS("ENVIRONMENT");
			end = System.currentTimeMillis();
			printMessage("END- Environment generated in:", start, end);
			List<String> eventsEnvironment = new ArrayList<>(environment.getAlphabetEvents());

			Collections.shuffle(eventsEnvironment);

			printMessage("Generating the partial component.....");
			start = System.currentTimeMillis();
			RandomComponentGenerator cg = new RandomComponentGenerator();
			LabelledTransitionSystem partialComponent = cg.getComponent(c);

			LabelledTransitionSystem finalComponent = cg.getFinalComponent();
			end = System.currentTimeMillis();
			printMessage("END- Partial component generated in: ", start, end);

			List<String> alphabet = new ArrayList<>();
			alphabet.addAll(environment.getAlphabetEvents());
			alphabet.addAll(partialComponent.getAlphabetEvents());
			Collections.shuffle(eventsEnvironment);
			String event1 = eventsEnvironment.get(0);
			String event2 = eventsEnvironment.get(1);

			// RUNNING THE MODEL CHECKER
			Vector<LabelledTransitionSystem> machines = new Vector<>();
			machines.add(environment);
			CompositeState envCompState = new CompositeState(machines);

			Vector<LabelledTransitionSystem> componentMachines = new Vector<>();
			componentMachines.add(finalComponent);
			CompositeState compCompState = new CompositeState(componentMachines);

			PropertyGenerator pg = new PropertyGenerator(finalComponent.getAlphabetEvents(), event1, event2);

			CompositeState formulaLTS = new LTL2BA(new EmptyLTSOuput()).getCompactState(
					Integer.toString(propertyOfInterest), pg.getFormulae().get(propertyOfInterest),
					new Vector<>(finalComponent.getAlphabetEvents()));
			ModelChecker mc = new ModelChecker(new EmptyLTSOuput(), envCompState, compCompState, formulaLTS);

			int compsizeMc = finalComponent.size();
			int propsizeMc = formulaLTS.getComposition().size();

			MessageHandler.printMessage("Performing the model checking.....");
			long mcstart = System.currentTimeMillis();
			mc.check();
			boolean mcresult = mc.getResult();
			long mcend = System.currentTimeMillis();
			MessageHandler.printMessage("END- Model checking with result: " + mcresult + " performed in:", mcstart,
					mcend);

			long timeMc=mcend-mcstart;
			// RUNNING THE WELL FORMEDNESS CHECKER
			String boxOfInterest = partialComponent.getBoxIndexes().keySet().iterator().next();

			PostConditionGenerator postConditionGen = new PostConditionGenerator(
					new ArrayList<>(partialComponent.getBoxInterface(boxOfInterest)), event1, event2);
			Formula postConditionFormula = postConditionGen.getFormulae().get(postConditionOfInterest);

			printMessage("Converting the post condition in automaton....");
			start = System.currentTimeMillis();
			System.out.println("post-condition size.. " + partialComponent.getBoxInterface("box").size());

			LabelledTransitionSystem postConditionLTS = new LTLf2LTS().postconditionToLTS(postConditionFormula,
					new EmptyLTSOuput(), new HashSet<>(partialComponent.getBoxInterface("box")), "post")
					.getComposition();
			end = System.currentTimeMillis();
			long step1ConvertionOfThePostCondition = end - start;
			printMessage("END- Post condition converted in: ", start, end);

			start = System.currentTimeMillis();
			partialComponent = new WellFormednessLTSModifier(new EmptyLTSOuput()).modify(partialComponent,
					boxOfInterest, postConditionLTS);
			end = System.currentTimeMillis();
			long step2Integration = end - start;
			printMessage("Integrating post in partial controller: ", start, end);

			printMessage("Transforming the pre....");
			start = System.currentTimeMillis();

			checkWellFormedness(start, outputWriter, numberOfStates, environment, event1, event2, partialComponent,
					alphabet, step1ConvertionOfThePostCondition, step2Integration, propertyOfInterest, c);

			
			
			outputWriter.write("P" + propertyOfInterest + "\t" + testNumber + "\t" + c.toString() + "\t"
					+ compsizeMc + "\t" + + partialComponent.size() + "\t" +
					+ propsizeMc +"\t"+ precondition.getComposition().size() +"\t" 
					+ Boolean.toString(mc.getResult()) + "\t"+  result +"\t"
					+ timeMc+ "\t"+wellFormednessChecking +"\t" +checkingThePre+"\n");
					
					
			System.out.println("P" + propertyOfInterest + "\t" + testNumber + "\t" + c.toString() + "\t"
					+ compsizeMc + "\t" + + partialComponent.size() + "\t" +
					+ propsizeMc +"\t"+ precondition.getComposition().size() +"\t" 
					+ Boolean.toString(mc.getResult()) + "\t"+  result +"\t"
					+ timeMc+ "\t"+wellFormednessChecking +"\t" +checkingThePre+"\n");
			
			outputWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void checkWellFormedness(long start, Writer outputWriter, int numberOfStates,
			LabelledTransitionSystem environment, String event1, String event2,
			LabelledTransitionSystem partialController, List<String> alphabet, long step1ConvertionOfThePostCondition,
			long step2Integration, int propertyOfInterest, ModelConfiguration c) throws IOException {
		long end;
		List<String> newAlphaPre = new ArrayList<>(alphabet);
		newAlphaPre.remove(event1);
		newAlphaPre.remove(event2);
		Formula fltlPrecondition = new PreconditionGenerator(newAlphaPre, event1, event2).getFormulae()
				.get(propertyOfInterest);
		precondition = new LTLf2LTS().toProperty(fltlPrecondition, new EmptyLTSOuput(),
				new HashSet<String>(newAlphaPre), "precondition");

		
		end = System.currentTimeMillis();
		long loadingThePrecondition = end - start;
		printMessage("END- Transforming the pre ", start, end);

		Vector<LabelledTransitionSystem> machines = new Vector<>();
		machines.add(partialController);
		machines.add(environment);
		CompositeState system = new CompositeState(machines);

		String environmentSize = "environment states:" + environment.getStates().length + " transitions: "
				+ environment.getTransitionNumber();

		String controllerSize = "partial controller states:" + partialController.getStates().length + "  transitions: "
				+ partialController.getTransitionNumber();
		String propertySize = "property states: " + precondition.getComposition().getStates().length
				+ " property transitions: " + precondition.getComposition().getTransitionNumber();

		printMessage("Well-formedness checker....\t" + environmentSize + "\t" + controllerSize + "\t" + propertySize);

		start = System.currentTimeMillis();
		result=system.checkLTL(new EmptyLTSOuput(), precondition);
		end = System.currentTimeMillis();
		checkingThePre = end - start;
		printMessage("END- Well-formedness checker ", start, end);

		 wellFormednessChecking = step1ConvertionOfThePostCondition + step2Integration + loadingThePrecondition
				+ checkingThePre;

		
		
	}

	private void printMessage(String message) {
		System.out.println(time_formatter.format(System.currentTimeMillis()) + " - " + message);
	}

	private void printMessage(String message, long init, long end) {
		System.out.println(
				time_formatter.format(System.currentTimeMillis()) + " - " + message + " [" + (end - init) + " ms]");

	}
}
