package controller.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ac.ic.doc.mtstools.model.LTS;
import ac.ic.doc.mtstools.model.impl.LTSImpl;
import ac.ic.doc.mtstools.model.impl.LTSSimulationSemantics;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import controller.game.gr.GRRankSystem;
import controller.game.gr.perfect.PerfectInfoGRGameSolver;
import controller.game.gr.perfect.StateSpaceCuttingPerfectInfoOppositeGrControlProblem;
import controller.game.util.GRGameBuilder;
import controller.model.gr.GRControllerGoal;
import controller.model.gr.GRGame;

public class ReportingPerfectInfoStateSpaceCuttingGRControlProblem<S, A>
		extends StateSpaceCuttingPerfectInfoOppositeGrControlProblem<S, A> {

	class CompositeReport {
		public String testName;
		public int guaranteesCount;
		public int assumptionsCount;
		public int failuresCount;
		public boolean simulatesController;
		public boolean simulatedByController;

		public boolean isEquivalent() {
			return simulatesController && simulatedByController;
		}

		public List<CutterReport> cutterReports;

		public CompositeReport(String testName, GRControllerGoal<A> goal,
				List<CutterReport> cutterReports, LTS<S, A> mut, LTS<S, A> exp) {
			this.testName = testName;
			this.cutterReports = cutterReports;
			this.guaranteesCount = goal.getGuarantees().size();
			this.assumptionsCount = goal.getAssumptions().size();
			this.failuresCount = goal.getFaults().size();

			removeUnusedActionsFromLTS(mut);
			removeUnusedActionsFromLTS(exp);

			MTSAdapter<S, A> mutAdapter = new MTSAdapter<S, A>(mut);
			MTSAdapter<S, A> expAdapter = new MTSAdapter<S, A>(exp);
			LTSSimulationSemantics simulationSemantics = new LTSSimulationSemantics();
			simulatesController = simulationSemantics.isARefinement(mutAdapter,
					expAdapter);
			simulatedByController = simulationSemantics.isARefinement(
					expAdapter, mutAdapter);
		}

		protected void removeUnusedActionsFromLTS(LTS<S, A> machine) {
			Set<A> actionsToRemove = new HashSet<A>();
			// remove unused actions in order to pass equivalence check for the
			// result
			for (A action : machine.getActions()) {
				boolean hasAction = false;
				for (S s : machine.getStates()) {
					Set<S> image = machine.getTransitions(s).getImage(action);
					if (image != null && image != Collections.EMPTY_SET) {
						if (image.size() > 0) {
							hasAction = true;
							break;
						}
					}
				}
				if (!hasAction)
					actionsToRemove.add(action);
			}
			for (A actionToRemove : actionsToRemove)
				machine.removeAction(actionToRemove);
		}

		public String boolToString(boolean value) {
			return (value ? "Y" : "N");
		}

		public String toString() {
			return toString(false, "\t", "|", "\n");
		}

		public String getHeader() {
			return getHeader("\t", "", "\n");
		}

		public String getHeader(String columnSeparator, String sectionSeparator, String rowSeparator) {
			String cutsHeader = "";
			for (CutterReport report : cutterReports)
				cutsHeader += columnSeparator + sectionSeparator
						+ report.getHeader(columnSeparator, "");
			return "test name" + columnSeparator + "c'=c" + columnSeparator
					/*+ "c'<c" + columnSeparator + "c'>c" + columnSeparator*/
					+ "#As" + columnSeparator + "#G" + columnSeparator + "#F"
					+ cutsHeader + rowSeparator;
		}

		public String toString(boolean printHeader, String columnSeparator
				, String sectionSeparator, String rowSeparator) {
			String returnString = "";
			if (printHeader)
				returnString = getHeader(columnSeparator, sectionSeparator, rowSeparator);
			String cutsString = "";
			for (CutterReport report : cutterReports)
				cutsString += columnSeparator
						+ report.toString(false, columnSeparator, "");
			returnString = returnString + testName + columnSeparator
					+ boolToString(isEquivalent()) + columnSeparator
					/*+ boolToString(simulatesController) + columnSeparator
					+ boolToString(simulatedByController) + columnSeparator*/
					+ assumptionsCount + columnSeparator + guaranteesCount
					+ columnSeparator + failuresCount + cutsString
					+ rowSeparator;
			return returnString;
		}
	}

	class CutterReport {
		public Date initialTime;
		public Date finishingTime;
		public String cutterName;
		public int initialStatesCount;
		public int finishingStatesCount;

		public CutterReport(String cutterName, int initialStatesCount) {
			this(cutterName, new Date(), new Date(), initialStatesCount,
					initialStatesCount);
		}

		public CutterReport(String cutterName, Date initialTime,
				int initialStatesCount) {
			this(cutterName, initialTime, initialTime, initialStatesCount,
					initialStatesCount);
		}

		public CutterReport(String cutterName, Date initialTime,
				Date finishingTime, int initialStatesCount,
				int finishingStatesCount) {
			this.cutterName = cutterName;
			this.initialTime = initialTime;
			this.initialStatesCount = initialStatesCount;
			this.finishingStatesCount = finishingStatesCount;
		}

		public int getStatesCountDelta() {
			return finishingStatesCount - initialStatesCount;
		}

		public long getTimeDelta() {
			return finishingTime.getTime() - initialTime.getTime();
		}

		public String toString() {
			return toString(false, "\t", "\n");
		}

		public String getHeader() {
			return getHeader("\t", "\n");
		}

		public String getHeader(String columnSeparator, String rowSeparator) {
			return "cut name" + columnSeparator + /*"#S_0" + columnSeparator
					+ "#S_f" + columnSeparator +*/ "#S_d" + columnSeparator
					+ "T_d"
					+ rowSeparator;
		}

		public String toString(boolean printHeader, String columnSeparator,
				String rowSeparator) {
			String returnString = "";
			if (printHeader)
				returnString = getHeader(columnSeparator, rowSeparator);
			returnString = returnString + cutterName + columnSeparator
					/*+ initialStatesCount + columnSeparator
					+ finishingStatesCount + columnSeparator*/
					+ getStatesCountDelta() + columnSeparator
					+ getTimeDelta() + rowSeparator;
			return returnString;
		}
	}

	protected CompositeReport generalReport;

	protected LTS<S, A> expectedResult;

	public String getReport(boolean hasHeader, String columnSeparator,
			String sectionSeparator, String rowSeparator) {
		if (generalReport == null)
			return "";
		return generalReport.toString(hasHeader, columnSeparator, sectionSeparator, rowSeparator);
	}

	protected String outputFileName;
	protected String testName;
	protected LTS<S, A> envForController;
	
	public ReportingPerfectInfoStateSpaceCuttingGRControlProblem(
			String testName, 
			LTS<S, A> originalEnvironment,
			LTS<S, A> originalEnvironmentForController,
			GRControllerGoal<A> grControllerGoal, String outputFileName,
			LTS<S, A> expectedResult) {
		super(originalEnvironment, grControllerGoal);

		this.testName		= testName;
		this.outputFileName = outputFileName;
		this.envForController = originalEnvironmentForController;
		this.expectedResult = expectedResult;
	}

	@Override
	protected LTS<S, A> primitiveSolve() {
		List<CutterReport> reports = new ArrayList<CutterReport>();
		//ORIGINAL GR
		CutterReport originalSolverReport = new CutterReport("GR",
				environment.getStates().size());
		reports.add(originalSolverReport);		
		
		GRGame<S> game = new GRGameBuilder<S, A>().buildGRGameFrom(new MTSAdapter<S,A>(envForController), grControllerGoal);
		GRRankSystem<S> system = new GRRankSystem<S>(game.getStates(), game.getGoal().getGuarantees(), game.getGoal().getAssumptions(), game.getGoal().getFailures());		
		PerfectInfoGRGameSolver<S> originalGameSolver = new PerfectInfoGRGameSolver<S>(game, system);		

		originalGameSolver.solveGame();
		
		originalSolverReport.finishingTime = new Date();
		originalSolverReport.finishingStatesCount = originalGameSolver.getWinningStates().size();				
		
		//WHOLE PROBLEM
		CutterReport grReport = new CutterReport("GR+CUT", environment
				.getStates().size());
		reports.add(grReport);
		
		//OPPOSITE SAFE
		CutterReport oppositeSafeReport = new CutterReport("Opp.Safe",
				environment.getStates().size());
		reports.add(oppositeSafeReport);

		OppositeSafeControlProblem<S, A> safeControlProblem = new OppositeSafeControlProblem<S, A>(environment, grControllerGoal);
		Set<S> winningNoG =  safeControlProblem.getWinningStates();

		oppositeSafeReport.finishingTime = new Date();
		oppositeSafeReport.finishingStatesCount = safeControlProblem.getWinningStates().size();

		//OPPOSITE GR
		CutterReport oppositeGRReport = new CutterReport("Opp.GR", environment
				.getStates().size());
		reports.add(oppositeGRReport);

		PerfectInfoOppositeGRControlProblem<S, A> oppositeGRControlProblem = new PerfectInfoOppositeGRControlProblem<S, A>(environment, grControllerGoal);
		Set<S> winningAssumptions =  oppositeGRControlProblem.getWinningStates();			

		oppositeGRReport.finishingTime = new Date();
		oppositeGRReport.finishingStatesCount = oppositeGRControlProblem.getWinningStates().size();

		SetView<S> losingStates = Sets.difference(winningNoG,
				winningAssumptions);
		removelosingStates(losingStates);
		gameSolver.solveGame();

		grReport.finishingTime = new Date();
		grReport.finishingStatesCount = gameSolver.getWinningStates().size();

		LTS<S, A> result = buildStrategy();		
		
		generalReport = new CompositeReport(testName, grControllerGoal,
				reports, result, expectedResult);

		try {
			LTSImpl<S, A> resultImpl = (LTSImpl<S, A>) result;
			File file = new File(outputFileName + ".lts");
			FileOutputStream fout = new FileOutputStream(file);
			// now convert the FileOutputStream into a PrintStream
			PrintStream myOutput = new PrintStream(fout);
			String resultString = resultImpl.toString();
			myOutput.print(resultString);
			myOutput.close();
			fout.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		PrintWriter writer;
		try {
			writer = new PrintWriter(outputFileName, "UTF-8");
			writer.print(generalReport.toString(true, "&", "|", "\\\n"));
			writer.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return result;

	}
}
