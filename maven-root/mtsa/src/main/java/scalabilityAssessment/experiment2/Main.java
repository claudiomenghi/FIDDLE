package scalabilityAssessment.experiment2;

import scalabilityAssessment.experiment1.Configuration;
import scalabilityAssessment.modelgenerator.ModelConfiguration;
import scalabilityAssessment.modelgenerator.ConfigurationGenerator;

public class Main {

	public static void main(String[] args) {
		ConfigurationGenerator size=new ConfigurationGenerator();
		
		while(size.hasNext()){
			ModelConfiguration conf=size.next();
			System.out.println(conf);
		}

	}

}
