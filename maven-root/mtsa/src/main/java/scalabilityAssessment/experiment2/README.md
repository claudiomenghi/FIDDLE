# Substitutability checker VS model checker

This experiment tries to assess the differences in terms of performance between the substitutability checker and the model checker.
To main method of the experiment is contained in the class [SubcontrollerChecker](subcontrollerchecker/SubcontrollerChecker.java).
Each test of the experiment is described by the class [SubcontrollerCheckerTest](subcontrollerchecker/SubcontrollerCheckerTest.java).


To evaluate the *substitutability checker*, 
1. we generated an LTS model of the environment and a complete model for the component under development.
 e checked the system w.r.t. to a property of interest using a standard model checking procedure. 
2. we created considered sub-component with half of the component states. We defined pre- and post-conditions for the sub-component 
3. we ran the substitutability checker, comparing its performance with model-checking.

Specifically, 
* *Environment:* We generated environments of different sizes. We considered 10000 states (\#EnvStates)  and performed increases of 20% until 20000 states. We considered the transition density of the Mars Rover example and  increased the number of events proportionally as the number of states increases.
* *Subcomponent* We started with fully specified components with 100 states (\#CompStates) and performed increases of 20% until 200 states. The component is generated using the same transition density  of the *Executive* component.
Also the number of events is generated proportionally. One of the states of the LTS is marked as a black box, with 25\% of the events of the component in its interface. 
To produce the sub-component we extracted it from  the generated component by taking half of the component states and the transitions between them. 
We also added a new state s_f as a final state of the sub-component and connected to it  all the states of the sub-component with a
 successor not included in the sub-component.

## Running the scalability test
To run the scalability test download the file `subVSmc.jar`. You must specify the file where you want the results to be saved. It is also necessary to specify the additional arguments `-Xms6144m -Xmx6144m` to the JVM. For example, the file can be executed as follows:
* `java -jar -Xms6144m -Xmx6144m subVSmc.jar subVSmc.txt`

## Results
The results can be found in the file `subVSmc.xls`.