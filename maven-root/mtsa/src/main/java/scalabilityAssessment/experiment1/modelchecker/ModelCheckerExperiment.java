package scalabilityAssessment.experiment1.modelchecker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import scalabilityAssessment.experiment1.Configuration;
import scalabilityAssessment.modelgenerator.Size;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.parser.Symbol;

public class ModelCheckerExperiment {

	public static final Symbol endSymbol = new Symbol("end", Symbol.UPPERIDENT);

	public static final Symbol endFluent = new Symbol("F_"
			+ LTLf2LTS.endSymbol.toString(), Symbol.UPPERIDENT);

	public static final SimpleDateFormat time_formatter = new SimpleDateFormat(
			"HH:mm:ss.SSS");

	private static File outputFile;

	private static Writer outputWriter;

	

	public static void main(String[] args) throws IOException {


		outputFile = new File(args[0] + File.separator + "results.txt");

		outputWriter = new FileWriter(outputFile);

		outputWriter
				.write("TESTNUMBER \t NUMBEROFSTATES \t VERIFICATIONTIME(ms) \t WELLFORMEDNESSTIME(ms) \t POSTPROCESSINGTIME(ms) \t INTEGRATIONTIME(ms) \t MODELCHECKINGTIME(ms)\n");
		outputWriter.close();
		AssertDefinition.init();

		long startOfExperiment = System.currentTimeMillis();

		for(int i=0; i<Size.values().length; i++){
			System.out.println(Size.values()[i].toString());
		}
		System.in.read();
		for (int testNumber = 1; testNumber <= Configuration.numberOfTest; testNumber++) {

			ExecutorService executor = Executors.newSingleThreadExecutor();
			System.out.println("********************** TEST " + testNumber
					+ " **********************");

			long testStart = System.currentTimeMillis();

			Future<?> future = executor
					.submit(new ModelCheckerTest(outputFile, testNumber));

			try {
				future.get(Configuration.timeoutMinutes, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				future.cancel(true); // <-- interrupt the job
				outputWriter = new FileWriter(outputFile,true);
				outputWriter.write("timeout\n");
				outputWriter.close();
				executor.shutdownNow();
			}
			
			long testEnd = System.currentTimeMillis();
			System.out
					.println("***************** END: Test performed in: "
							+ (testEnd - testStart) / 1000 / 60
							+ "m *****************");
		}
		long endOfExperiment = System.currentTimeMillis();

		System.out.println("END: Experiment performed in: "
				+ (endOfExperiment - startOfExperiment) / 1000 / 60 + "m");

		outputWriter.close();
	}

}
