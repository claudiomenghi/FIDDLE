package scalabilityAssessment.experiment2.subcontrollerchecker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
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
import ltsa.lts.checkers.substitutability.SubstitutabilityChecker;
import ltsa.lts.distribution.DistributionDefinition;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.toba.LTL2BA;
import ltsa.lts.parser.Def;
import ltsa.lts.parser.actions.LabelSet;
import ltsa.ui.EmptyLTSOuput;
import scalabilityAssessment.MessageHandler;
import scalabilityAssessment.modelgenerator.ModelConfiguration;
import scalabilityAssessment.modelgenerator.RandomLTSGenerator;
import scalabilityAssessment.modelgenerator.SubcontrollerGenerator;
import scalabilityAssessment.postconditiongenerator.PostConditionGenerator;
import scalabilityAssessment.preconditionGenerator.PreconditionGenerator;
import scalabilityAssessment.propertygenerator.PropertyGenerator;

public class EX2test implements Callable<Void> {

	private final File outputFile;
	private final int testNumber;
	private final ModelConfiguration c;
	private final int propertyOfInterest;

	public EX2test(File outputFile, int testNumber, ModelConfiguration size, int propertyOfInterest) {
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

			MessageHandler.printMessage("Generating the environment.....");
			start = System.currentTimeMillis();
			LabelledTransitionSystem environment = new RandomLTSGenerator(c.getStatesEnviromnet(),
					c.getEventsEnvironment(), c.getTransitionPerStateEnvironment()).getRandomLTS("ENVIRONMENT");
			end = System.currentTimeMillis();
			MessageHandler.printMessage("END- Environment generated in:", start, end);
			List<String> eventsEnvironment = new ArrayList<>(environment.getAlphabetEvents());

			Collections.shuffle(eventsEnvironment);
			String event1 = eventsEnvironment.get(0);
			String event2 = eventsEnvironment.get(1);
			MessageHandler.printMessage("Generating the final component.....");
			
			
			start = System.currentTimeMillis();
			LabelledTransitionSystem finalComponent = new RandomLTSGenerator(c.getStatesController(),
					c.getEventsController(), c.getTransitionsPerStateController()).getRandomLTS("CONTROLLER");
			end = System.currentTimeMillis();
			MessageHandler.printMessage("END- Controller generated in:", start, end);

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

			int compsizeMc=finalComponent.size();
			int propsizeMc=formulaLTS.getComposition().size();
			
			MessageHandler.printMessage("Performing the model checking.....");
			long mcstart = System.currentTimeMillis();
			mc.check();
			boolean mcresult=mc.getResult();
			long mcend = System.currentTimeMillis();
			MessageHandler.printMessage("END- Model checking with result: "+mcresult+" performed in:", mcstart, mcend);

			
			MessageHandler.printMessage("Generating the sub-controller.....");
			start = System.currentTimeMillis();
			LabelledTransitionSystem subController = new SubcontrollerGenerator(c.getStatesController() / 2)
					.subController(finalComponent);
			end = System.currentTimeMillis();

			MessageHandler.printMessage("END- Sub-Controller generated in:", start, end);
			List<String> alphabet = new ArrayList<>();

			alphabet.addAll(environment.getAlphabetEvents());
			alphabet.addAll(finalComponent.getAlphabetEvents());

			PostConditionGenerator postConditionGen = new PostConditionGenerator(subController.getAlphabetEvents(),
					event1, event2);

			Formula postConditionFormula = postConditionGen.getFormulae().get(postConditionOfInterest);
			Formula fltlPrecondition = new PreconditionGenerator(alphabet, event1, event2).getFormulae()
					.get(propertyOfInterest);
			System.out.println(fltlPrecondition);


			CompositeState environmentState = new CompositeState("ENVIRONMENT");
			environmentState.addMachine(environment);
			SubstitutabilityChecker subchecker = new SubstitutabilityChecker(new EmptyLTSOuput(), environmentState,
					subController, fltlPrecondition, "PRECONDITION", postConditionFormula, "POSTCONDITION");


			long subinit = System.currentTimeMillis();
			subchecker.check();
			boolean subresult=subchecker.getResult();
			long  subend = System.currentTimeMillis();
			
			int compsizeSub=subchecker.getResultComponentSize();
			int propsizeSub=subchecker.getPostCondition().getComposition().size();

			outputWriter.write(
					"P" + propertyOfInterest + "\t" + testNumber + "\t" + c.toString() + "\t" +compsizeMc+ "\t" +compsizeSub+"\t" + propsizeMc+"\t" +propsizeSub+  "\t"  + mcresult+ "\t"+ subresult+ "\t"+ (mcend-mcstart)+"\t" + (subend - subinit) +"\t" +subchecker.getEffectiveCheckingTime()+ "\n");

			System.out.println(
					"P" + propertyOfInterest + "\t" + testNumber + "\t" + c.toString() + "\t" +compsizeMc+ "\t" +compsizeSub+"\t" +propsizeMc +  "\t"   + propsizeSub+"\t" + mcresult+ "\t"+subresult+ "\t"+(mcend-mcstart)+"\t" +  (subend - subinit) +"\t" +subchecker.getEffectiveCheckingTime()+ "\n");

			outputWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
