# Substitutability checker VS model checker

This experiment tries to assess the differences in terms of performance between the substitutability checker and the model checker.
To main method of the experiment is contained in the class [EX2SubVSMc.java](subcontrollerchecker/EX2SubVSMc.java).
Each test of the experiment is described by the class [EX2test.java](subcontrollerchecker/EX2test.java).


To evaluate the *substitutability checker*, 
1. we generated an LTS model of the environment and a complete model for the component under development.
 e checked the system w.r.t. to a property of interest using a standard model checking procedure. 
2. we created considered sub-component with half of the component states. We defined pre- and post-conditions for the sub-component 
3. we ran the substitutability checker, comparing its performance with model-checking.



## Running the scalability test
To run the scalability test download the file `subVSmc.jar`. You must specify the file where you want the results to be saved. It is also necessary to specify the additional arguments `-Xms6144m -Xmx6144m` to the JVM. For example, the file can be executed as follows:
* `java -jar -Xms6144m -Xmx6144m subVSmc.jar subVSmc.txt`

## Results
The results can be found in the file `subVSmc.xls`.