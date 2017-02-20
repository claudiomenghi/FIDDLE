# Mars Rover

The `Executive` module of Mars Rover is a multi-threaded system with the following
* `Executive`: a component that synchronizes the other components of the system;
* `ExecCondChecker`: a component for monitoring the state conditions. The `ExecCondChecker` verifies whether specific conditions occur by accessing the `condList` variable and signals these condition to the `Executive` by acting over the `exec` variable.
* `ActionExecution`: a component for sending commands to the Rover.

The `ExecCondChecker` is further decomposed into `db-monitor` and `internal`. 

The Mars Rover case study is contained in the file `Mars.lts`. 


#### Structure of the Mars.lts file 
The file is composed by several sections
* `Basic definitions`: contains basic variables and sets;
* `MUTEX definition`: contains the mutex definition of a mutex;
* `Complex variable definition`: contains the definition for variables that, based on what we want to do, checks for race conditions or not, in which case we do not always need the `begin_read`, `end_read`, etc;
* `Mutexes and condition variable instances`: instantiates mutexes and condition variables;
* `ExecCondChecker`: contains the description of the `ExecCondChecker` module;
 * `ExecCondChecker.DBMonitor` contains the `DBMonitor` component of the `ExecCondChecker`;
 * `ExecCondChecker.Internal` contains the `Internal` component of the `ExecCondChecker`;
* `Database`: a database;
* `Actions`: the set of actions that can be executed by the executive.
* `Executive`: contains the designs of the executive module. Specifically, the processes
 * `Executive` contains the final executive module
 * `PartialExecutive1` contains the partial design used in the first experiment
 * `PartialExecutive2` contains the partial design used in the second experiment


#### Environment
The environment where the `Executive` component is deployed is represented by the `||Executive` process.




#### Properties of interest
We considered two properties the Mars Rover must ensure:
* the Executive performs an action only after a new plan is read from the db
  * `P1=[](!ACTION_PERFORMED W (READ_PLAN & !ACTION_PERFORMED))`
* the Executive gets the lock on the condList variable only after getting the exec lock
  * `P2=[](COND_LIST_LOCKED -> EXEC_LOCKED)`

Formulae `P1` and `P2` are defined over the following fluents which are initially false
* `COND_LIST_LOCKED=<{executive.condList.lock},{executive.condList.unlock}>`
* `EXEC_LOCKED=<{executive.exec.lock},{executive.exec.unlock}>`
* `READ_PLAN=<{database.dbChanged.begin_read},{database.dbChanged.end_read[True]}>`
* `ACTION_PERFORMED=<{actionExecution.install},{action.unlock}>`



## Design D1
To analyze design D1, choose as an environment the process `Environment` and as a partial component the design `D1`.

*realizability checker*: load the `Environment` and  the design `D1` as partial component.
 * Property `P1`: to verify `P1` click on *Check > Realizability Checker > P1*. A refinement of `D1` that satisfies the property of interest is realizable.
 * Property `P2`: to verify `P2` click on *Check > Realizability Checker > P2*. A refinement of `D1` that satisfies the property of interest is realizable.

*model checker*: load the `Environment` and  the design `D2` as partial component (make sure that the `D1_PLAN_BOX_PLAN_READ` is commented).
 * Property `P1`: to verify `P1` click on *Check > Model Checker > P1*. A counterexample is returned.
 * Property `P2`: to verify `P2` click on *Check > Model Checker > P2*. A counterexample is returned.


###### Design D1 with post
*model checker*: load the `Environment` and  the design `D1` as partial component (make sure that the `D1_PLAN_BOX_READS_PLAN` is uncommented).
 * Property `P1`: to verify `P1` click on *Check > Model Checker > P1*. The property of interest is satisfied.

*well formedness checker*: load the `Environment` and  the design `D1` as partial component (make sure that the `D1_Executive_BOX_READS_PLAN` is uncommented).
 * Property `P1`: to verify `P1` click on *Check > Well-formedness Checker > PLAN_BOX_NO_PLAN_EXECUTION*. The property of interest is not satisfied.
 
Add the post-condition `D1_Executive_BOX_READS_PLAN`
 * Property `P1`: to verify `P1` click on *Check > Well-formedness Checker > PLAN_BOX_NO_PLAN_EXECUTION*. The property of interest is not satisfied.


## Design D2
To analyze design D2, choose as an environment the process `Environment` and as a partial component the design `D2`.

*realizability checker*: load the `Environment` and  the design `D2` as partial component.
 * Property `P1`: to verify `P1` click on *Check > Realizability Checker > P1*. A refinement of `D2` that satisfies the property of interest is realizable.
 * Property `P2`: to verify `P2` click on *Check > Realizability Checker > P2*. A refinement of `D2` that satisfies the property of interest is realizable.

*model checker*: load the `Environment` and  the design `D2` as partial component (make sure that the `PLAN_READ_IN_PLAN_BOX` is commented).
 * Property `P1`: to verify `P1` click on *Check > Model Checker > P1*. A counterexample is returned.
 * Property `P2`: to verify `P2` click on *Check > Model Checker > P2*. The property of interest is satisfied.

###### Design D2 with post
*model checker*: load the `Environment` and  the design `D2` as partial component (make sure that the `PLAN_READ_IN_PLAN_BOX` is uncommented).
 * Property `P1`: to verify `P1` click on *Check > Model Checker > P1*. The property of interest is satisfied.
 * Property `P2`: to verify `P2` click on *Check > Model Checker > P2*. The property of interest is satisfied.

## Design D3
To analyze design D3, choose as an environment the process `Environment` and as a partial component the design `D3`.

* Property `P1`: to verify `P1` click on *Check > Model Checker > P1*. 
  * The property of interest is satisfied
* Property `P2`: to verify `P2` click on *Check > Model Checker > P2*. 
 * The property of interest is satisfied



