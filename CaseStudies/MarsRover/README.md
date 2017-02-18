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

The first design assumes  the developer still has to specify a sub-component that reads plans. 
Specifically, states `WaitingForPlan`, `PrepareExecution` and `ExecutePlan` are encapsulated in the box `PlanBox`.


The component `D1` can analyzed considering the component named as `D1_Component` in the file `.lts`



