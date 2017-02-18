package scalabilityAssessment.experiment1.wellformedness;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ltsa.lts.ltl.AssertDefinition;
import scalabilityAssessment.experiment1.Configuration;
import scalabilityAssessment.modelgenerator.ConfigurationGenerator;
import scalabilityAssessment.modelgenerator.ModelConfiguration;
import scalabilityAssessment.propertygenerator.PropertyGenerator;

public class EX1WellVSMc {

	public static final SimpleDateFormat time_formatter = new SimpleDateFormat("HH:mm:ss.SSS");

	private static Writer outputWriter;

	public static void main(String[] args) throws IOException {

		File outputFile = new File(args[0]);

		outputWriter = new FileWriter(outputFile);

		outputWriter.write("PROPERTY  \t TESTNUM \t" + ModelConfiguration.getHeader()
				+ "\t #componentMc \t #componentWell  \t #propMc \t #propWell  \t resMc  \t resWell (ms) \t timeMc (ms) \t timeWell (ms) \t effectivetimeWell \n");

		outputWriter.close();
		AssertDefinition.init();

		long startOfExperiment = System.currentTimeMillis();

		for (int testNumber = 1; testNumber <= Configuration.numberOfExperiment; testNumber++) {
			for (int propertyOfInterest = 0; propertyOfInterest < PropertyGenerator.numFormulae; propertyOfInterest++) {
				ConfigurationGenerator configurationGenerator = new ConfigurationGenerator();

				while (configurationGenerator.hasNext()) {
					ModelConfiguration c = configurationGenerator.next();

					ExecutorService executor = Executors.newSingleThreadExecutor();
					System.out.println("********************** TEST " + testNumber + " **********************");

					long testStart = System.currentTimeMillis();

					try {
						executor.invokeAll(Arrays.asList(new EX1Test(outputFile, testNumber, c, propertyOfInterest)),
								Configuration.timeoutMinutes, TimeUnit.MINUTES); 
					} catch (InterruptedException e) {
						e.printStackTrace();
						outputWriter = new FileWriter(outputFile, true);
						outputWriter.write("timeout\n");
						outputWriter.close();
					}
					executor.shutdown();

					long testEnd = System.currentTimeMillis();
					System.out.println("***************** END: Test performed in: " + (testEnd - testStart) / 1000 / 60
							+ "m *****************");
				}
			}
		}
		long endOfExperiment = System.currentTimeMillis();

		System.out.println("END: Experiment performed in: " + (endOfExperiment - startOfExperiment) / 1000 / 60 + "m");

		outputWriter.close();

	}
}
