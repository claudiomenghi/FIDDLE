package scalabilityAssessment.experiment1.wellformedness;

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

import ltsa.lts.ltl.AssertDefinition;
import scalabilityAssessment.experiment1.Configuration;
import scalabilityAssessment.modelgenerator.ModelConfiguration;
import scalabilityAssessment.modelgenerator.ConfigurationGenerator;

public class ExWellFormednessChecker {

	public static final SimpleDateFormat time_formatter = new SimpleDateFormat(
			"HH:mm:ss.SSS");

	private static Writer outputWriter;

	public static void main(String[] args) throws IOException {

		File outputFile = new File(args[0]);

		outputWriter = new FileWriter(outputFile);

		outputWriter
				.write("TESTNUMBER \t NUMBEROFSTATES \t WELLFORMEDNESSTIME(ms) \t POSTPROCESSINGTIME(ms) \t INTEGRATIONTIME(ms) \t MODELCHECKINGTIME(ms)\n");
		outputWriter.close();
		AssertDefinition.init();

		long startOfExperiment = System.currentTimeMillis();

		for (int testNumber = 1; testNumber <= Configuration.numberOfTest; testNumber++) {
			for (int propertyOfInterest = 0; propertyOfInterest < Configuration.numberOfFormulae; propertyOfInterest++) {
				ConfigurationGenerator configurationGenerator = new ConfigurationGenerator();

				while (configurationGenerator.hasNext()) {
					ModelConfiguration c = configurationGenerator.next();

					ExecutorService executor = Executors
							.newSingleThreadExecutor();
					System.out.println("********************** TEST "
							+ testNumber + " **********************");

					long testStart = System.currentTimeMillis();

					Future<?> future = executor
							.submit(new WellFormednessCheckerTest(outputFile,
									testNumber, c, propertyOfInterest));

					try {
						future.get(Configuration.timeoutMinutes,
								TimeUnit.MINUTES);
					} catch (InterruptedException e) {
						e.printStackTrace();
						future.cancel(true);
					} catch (ExecutionException e) {
						e.printStackTrace();
						future.cancel(true);
					} catch (TimeoutException e) {
						future.cancel(true); // <-- interrupt the job
						outputWriter = new FileWriter(outputFile, true);
						outputWriter.write("timeout\n");
						outputWriter.close();
						
					}
					executor.shutdown();

					long testEnd = System.currentTimeMillis();
					System.out
							.println("***************** END: Test performed in: "
									+ (testEnd - testStart)
									/ 1000
									/ 60
									+ "m *****************");
				}
			}
		}
		long endOfExperiment = System.currentTimeMillis();

		System.out.println("END: Experiment performed in: "
				+ (endOfExperiment - startOfExperiment) / 1000 / 60 + "m");

		outputWriter.close();

	}
}
