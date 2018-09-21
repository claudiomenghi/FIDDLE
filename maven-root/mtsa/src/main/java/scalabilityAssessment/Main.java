package scalabilityAssessment;

import scalabilityAssessment.modelgenerator.ConfigurationGenerator;

public class Main {

	public static void main(String[] args) {
		
		
		ConfigurationGenerator cg=new ConfigurationGenerator();
		
		while(cg.hasNext()){
			System.out.println(cg.next());
		}
	}

}
