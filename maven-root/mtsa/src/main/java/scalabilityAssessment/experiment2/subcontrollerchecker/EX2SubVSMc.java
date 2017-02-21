package scalabilityAssessment.experiment2.subcontrollerchecker;

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
import scalabilityAssessment.experiment1.wellformedness.EX1Test;
import scalabilityAssessment.modelgenerator.ConfigurationGenerator;
import scalabilityAssessment.modelgenerator.ModelConfiguration;
import scalabilityAssessment.propertygenerator.PropertyGenerator;

public class EX2SubVSMc {
	public static final SimpleDateFormat time_formatter = new SimpleDateFormat(
			"HH:mm:ss.SSS");

	private static Writer outputWriter;

	public static void main(String[] args) throws IOException {

		File outputFile = new File(args[0]);

		outputWriter = new FileWriter(outputFile);

		outputWriter
				.write("PROPERTY  \t TESTNUM \t"+ModelConfiguration.getHeader()+"\t #componentMc \t #componentSub  \t #propMc \t #propSub  \t resMc  \t resSub (ms) \t timeMc (ms) \t timeSub (ms) \t effectivetimeSub \n");
		outputWriter.close();
		AssertDefinition.init();

		long startOfExperiment = System.currentTimeMillis();

		for (int experimentNumber = 1; experimentNumber <= Configuration.numberOfExperiment; experimentNumber++) {
			for (int propertyOfInterest = 0; propertyOfInterest < PropertyGenerator.numFormulae; propertyOfInterest++) {
				ConfigurationGenerator configurationGenerator = new ConfigurationGenerator();

				int configurationNum=0;
				while (configurationGenerator.hasNext()) {
					configurationNum++;
					ModelConfiguration c = configurationGenerator.next();

					ExecutorService executor = Executors
							.newSingleThreadExecutor();
					System.out.println("********************** #Experiment: "
							+ experimentNumber + "\t CONFIGURATION: "+configurationNum+" **********************");

					long testStart = System.currentTimeMillis();

					
					Future<Void> futureResult = executor
							.submit(new EX2test(outputFile, experimentNumber, c, propertyOfInterest));
					try {
						futureResult.get(Configuration.timeoutMinutes, TimeUnit.MINUTES);
					} catch (ExecutionException | TimeoutException | InterruptedException e) {
						e.printStackTrace();
						outputWriter = new FileWriter(outputFile, true);
						outputWriter.write("timeout\n");
						outputWriter.close();
					}
					

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
