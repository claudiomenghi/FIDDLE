# Mars Rover

The `Executive` module of Mars Rover is a multi-threaded system with the following
* `Executive`: a component that synchronizes the other components of the system;
<<<<<<< HEAD
* `ExecCondChecker`: a component for monitoring the state conditions, and
=======
* `ExecCondChecker`: a component for monitoring the state conditions. The `ExecCondChecker` verifies whether specific conditions occur by accessing the `condList` variable and signals these condition to the `Executive` by acting over the `exec` variable.
>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
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
<<<<<<< HEAD

* the `Executive` forces an action to be executed only after the controller gets the  `exec` lock:
 * `P1=[](ACTIONEXECUTION->EXEC_LOCKED)` where the fluents `ACTIONEXECUTION` and `EXEC_LOCKED` are defined as
   * `ACTIONEXECUTION=<{executive.action.lock},{executive.action.unlock}>`
   * `EXEC_LOCKED=<{executive.exec.lock},{executive.exec.unlock}>`
* the lock over the `condList` variable is obtained only after obtaining the `exec` lock:
 * `P2=[](COND_LIST_LOCKED->EXEC_LOCKED)` where the fluents `COND_LIST_LOCKED` is defined as
   * `COND_LIST_LOCKED=<{executive.condList.lock},{executive.condList.unlock}>`

## Partial design 1

In the first design the purchase and delivery component is made by two states: the regular state `1` and the black box state `2`. 
The purchase and delivery moves from state `1` to state `2` whenever a `userReq` event is triggered.
It moves from `2` to `1` whenever a `respOk` event is reveived. 
The interface of state `2` contains all the events of the environment with the exception of `userReq` and `respOk`.


![Partial Design 1](PartialDesign1.png "Partial Design 1")

#### Experiment 1
The following Table contains the results obtained without adding post-conditions to the state 2, where `T` means that the procedure returns a positive results while `F` specifies that the procedure failed.
    

By running the *realizability checker* it is possible to conclude that:

| Property | Realizability Checker | 
| ---------|-----------------------|
| P1       |        <p style="color:green">T</p>             |  
| P2       |        <p style="color:green">T</p>             |   
| P3       |       <p style="color:green">T</p>             |    
| P4       |        <p style="color:green">T</p>           |     
| P5       |        <p style="color:green">T</p>            |      
| P6       |       <p style="color:green">T</p>            |      
| P7       |       <p style="color:red">F</p>             |     

##### Realizability checker

* `P1`: it is possible to realize a component that ensures that the system satisfies `P1`. Specifically, the realizability checker returned the following trace:<br/>
`userReq`, `offerRcvd`, `usrNack`,	`reqCancelled`,	`shipInfoReq`,	`costAndTime`,	`shipReq`, `shipInfoReq`, `costAndTime`, `shipReq`, `shipInfoReq`, `costAndTime`.<br/>
This is a trace that can be enforced by the purchase and delivery component that ensures that the system asks shipping info only if the user sent a request.
* `P2`: it is possible to realize a component that ensures that the system satisfies `P2`. Specifically, the realizability checker returned the following trace:<br/> 
`userReq`, `prodInfoReq`, `infoRcvd`, `offerRcvd`, `usrNack`, `reqCancelled`, `shipInfoReq`, `costAndTime`, `prodReq`, `prodInfoReq`, `infoRcvd`, `shipReq`, `shipInfoReq`, `costAndTime`, `prodReq`, `prodInfoReq`, `infoRcvd`, `shipReq`.<br/> 
This trace ensures that after a `userReq` event occurs, the offer is provided to the user (the event `offerRcvd` occurs) only if the furniture service has confirmed the availability of the requested product (the event `inforRcvd` occurs). 
* `P3`: it is possible to realize a component that ensure that the system satisfies `P3`. Specifically, the realizability checker returned the following trace:<br/>
`userReq`, `offerRcvd`, `shipInfoReq`, `costAndTime`, `usrAck`, `respOk`, `userReq`, `offerRcvd`, `usrAck`, `respOk`, `userReq`, `offerRcvd`, `usrAck`.
This trace satisfies `P3` since the event `shipReq` never occurs. However, the developer would probably force the `shipReq` to finally occur. Thus we designed the properties `P3a` and `P3b` as follows
 * `P3a =[]((F_UsrReq&& (<>F_RespOk))->((!((!F_UserAck) W F_ShipReq))&& (<>F_ShipReq)))`<br/>
 Also a partial component that satisfies this requirement is satifiable. Specifically, the realizability checker returned the following trace:<br/>
 `userReq`, `offerRcvd`, `usrAck`, `shipInfoReq`, `costAndTime`, `shipReq`, `respOk`, `userReq`, `offerRcvd`, `usrAck`,	`shipInfoReq`, `prodInfoReq`, `infoRcvd`, `prodReq`, `respOk`, `userReq`, `offerRcvd`, `usrAck`, `costAndTime`, `shipReq`, `respOk`, `userReq`, `offerRcvd`, `usrAck`, `shipInfoReq`, `prodInfoReq`, `infoRcvd`, `prodReq`, `respOk`, `userReq`, `offerRcvd`, `usrAck`, `costAndTime`, `shipReq`, `respOk`, `userReq`, `offerRcvd`, `usrAck`, `shipInfoReq`, `prodInfoReq`, `infoRcvd`, `prodReq`.
 A similar trace can be found by considering the requirement `P3b` as follows:
  * `P3b =[]((F_UsrReq&& (<>F_RespOk))->((!((!F_UserAck) W F_ProdReq))&& (<>F_ProdReq)))`<br/>
   Also a partial component that satisfies this requirement is satifiable. Specifically, the realizability checker returned the following trace:<br/>
`userReq`, `offerRcvd`, `usrAck`, `shipInfoReq`, `costAndTime`, `prodInfoReq`, `infoRcvd`, `prodReq`, `prodInfoReq`, `respOk`, `userReq`, `offerRcvd`, `usrAck`, `infoRcvd`, `prodReq`, `prodInfoReq`, `respOk`, `userReq`,	`offerRcvd`, `usrAck`, `infoRcvd`,	`prodReq`, `prodInfoReq`, `respOk`.	
* `P4:` it is possible to realize a component that ensures that the system satisfies `P1`. Specifically, the realizability checker returned the following trace:<br/> 
`userReq`, `offerRcvd`, `usrNack`, `reqCancelled`, `shipInfoReq`, `costAndTime`, `prodInfoReq`, `shipCancel`, `shipInfoReq`, `costAndTime`, `shipCancel`, `shipInfoReq`, `costAndTime`, `shipCancel`.<br/>
This trance ensures that a `userAck` event is followed by a `F_ShipCancel`. However, as previously it is possible to force the events `prodCancel` and `shipCancel` to occur. Specifically, the requirements `P4a` and `P4b` are designed as follows:
 * `P4a=[]((F_UsrReq&&<>F_ProdCancel)->(!((!(F_UserNack) W (F_ProdCancel)))))`<br/>
  Also a partial component that satisfies this requirement is satifiable. Specifically, the realizability checker returned the following trace:<br/>
  `userReq`, `offerRcvd`, `usrNack`, `reqCancelled`, `shipInfoReq`,	`costAndTime`, `prodInfoReq`, `infoRcvd`, `prodCancel`, `shipReq`,	`shipInfoReq`, `costAndTime`, `prodInfoReq`, `infoRcvd`, `prodCancel`, `shipReq`, `shipInfoReq`, `costAndTime`, `prodInfoReq`, `infoRcvd`, `prodCancel`.
 * `P4b=[]((F_UsrReq&&<>F_ShipCancelled)->(!((!(F_UserNack) W (F_ShipCancelled)))))`<br/>
  Also a partial component that satisfies this requirement is satifiable. Specifically, the realizability checker returned the following trace:<br/>
  `userReq`, `offerRcvd`, `usrNack`, `reqCancelled`, `shipInfoReq`,	`costAndTime`, `prodInfoReq`, `shipCancel`,	`shipInfoReq`, `costAndTime`, `shipCancel`,	`shipInfoReq`, `costAndTime`, `shipCancel`.	
* `P5:[]( F_UsrReq->(!((!(F_ProdCancelled) W F_ReqCancelled)) && !((!(F_ShipCancelled) W F_ReqCancelled))))`  it is possible to realize a component that ensure that the system satisfies `P5`. Specifically, the realizability checker returned the following trace:<br/>
`userReq`, `offerRcvd`, `usrNack`, `shipInfoReq`, `costAndTime`, `shipCancel`, `shipInfoReq`, `costAndTime`, `prodInfoReq`, `infoRcvd`, `prodCancel`, `reqCancelled`, `prodInfoReq`, `infoRcvd`, `prodCancel`, `prodInfoReq`, `shipCancel`, `shipInfoReq`, `costAndTime`, `infoRcvd`, `prodCancel`, `prodInfoReq`, `shipCancel`, `shipInfoReq`, `costAndTime`, `infoRcvd`,	`prodCancel`, `prodInfoReq`, `shipCancel`.	
Note that the subproperty `!((!(F_ShipCancelled) W F_ReqCancelled))` is satisfied since the fluent `F_ReqCancelled` is never true. Thus, it is possible to decompose `P5` as follows:
  * `P5a=[]( F_UsrReq&&<>F_ReqCancelled->(!((!(F_ProdCancelled) W F_ReqCancelled))))`<br/>
A component that satisfies this requirement is realizable. Specifically, the realizability checker returned the following trace:<br/>
`userReq`, `offerRcvd`, `usrNack`, `shipInfoReq`, `costAndTime`, `prodInfoReq`, `infoRcvd`, `prodCancel`, `reqCancelled`, `shipReq`, `shipInfoReq`, `costAndTime`, `shipReq`, `shipInfoReq`, `costAndTime`, `shipReq`.		
 * `P5b=[]( F_UsrReq&&<>F_ReqCancelled->(!(!(F_ShipCancelled) W F_ReqCancelled)))`<br/>
 A component that satisfies this requirement is realizable. Specifically, the realizability checker returned the following trace:
<br/>
`userReq`, `offerRcvd`, `usrNack`, `shipInfoReq`, `costAndTime`, `shipCancel`, `reqCancelled`, `shipInfoReq`, `costAndTime`, `prodInfoReq`, `shipCancel`, `shipInfoReq`, `costAndTime`, `shipCancel`, `shipInfoReq`, `costAndTime`, `shipCancel`. 
* `P6=[]( (F_UserAck-> <>F_RespOk) && (F_UserNack-> <>F_ReqCancelled) )` it is possible to realize a component that ensure that the system satisfies `P6`. Specifically, the realizability checker returned the following trace:<br/>
`userReq`, `offerRcvd`, `usrNack`, `reqCancelled`, `shipInfoReq`, `costAndTime`, `shipReq`, `shipInfoReq`, `costAndTime`, `shipReq`, `shipInfoReq`, `costAndTime`.	
Note that the requirement does not force the fluents `F_UserAck` and `F_UserNack` occur. Thus we rewritten `P6` as:
 * `P6a=[]( (F_UserAck-> <>F_RespOk) && <>F_UserAck )`<br/>
    A component that realizes `P6a` could exist.
 * `P6b=[]( (F_UserNack-> <>F_ReqCancelled) &&<>F_UserNack)`<br/>
    A component that realizes `P6b` could exist.
* `P7= ([]((F_ReqCancelled-><>F_UsrReq)&&<>F_ReqCancelled))`<br/> 
A component that satisfies `P7` is not realizable. Specifically, the box `2` can be left only if a `respOk` event occurs and no `userReq` event can occur while the purchase and delivery component is in `2`. Thus, after a `reqCanc` it is not possible to trigger the event `userReq`.
This implies that the design of the partial controller must be modified.

## Partial design 2
In the second design the purchase and delivery component is made by five states: the regulars state `1` and `3` and the boxes `2`, `4` and `5`. 
The purchase and delivery moves from state `1` to state `2` whenever a `userReq` event is triggered.
It moves from `2` to `3` whenever a `offerRcvd` event is reveived. 
It moves from `3` to `4` and from `4` to `1` whenever a `userAck` and a `respOk` event is reveived. 
It moves from `3` to `5` and from `5` to `1` whenever a `userNack` and a `reqCanc` event is reveived. 

![Partial Design 2](PartialDesign2.png "Partial Design 2")

#### Experiment 2
The following Table contains the results obtained without adding post-conditions to the boxes, where `T` means that the procedure returns a positive results while `F` specifies that the procedure failed.
    

By running the *realizability checker* it is possible to conclude that a component that ensures the satisfaction of all the properties of interest could be realizable. Specifically, also property `P7` can be satisfied by the current partial component.

| Property | Realizability Checker | 
| ---------|-----------------------|
| P1       |        <p style="color:green">T</p>             |  
| P2       |        <p style="color:green">T</p>             |   
| P3a       |       <p style="color:green">T</p>             |    
| P3b       |       <p style="color:green">T</p>             |    
| P4a       |        <p style="color:green">T</p>           |     
| P4b       |        <p style="color:green">T</p>           |     
| P5a       |        <p style="color:green">T</p>            |    
| P5b       |        <p style="color:green">T</p>            |    
| P6a       |       <p style="color:green">T</p>            |     
| P6b       |       <p style="color:green">T</p>            |     
| P7       |       <p style="color:green">T</p>             |     


##### Model checker
The following Table contains the results obtained without adding post-conditions to the boxes, where `T` means that the procedure returns a positive results while `F` specifies that the procedure failed.


By running the *model checker* it is possible to conclude that the partial design satisfies the following properties. 


| Property | Model checker | 
| ---------|-----------------------|
| P1       |        <p style="color:green">T</p>             |  
| P2       |        <p style="color:red">F</p>             |   
| P3a       |       <p style="color:red">F</p>             |    
| P3b       |       <p style="color:red">F</p>             |    
| P4a       |        <p style="color:green">T</p>           |     
| P4b       |        <p style="color:green">T</p>           |     
| P5a       |        <p style="color:red">F</p>            |    
| P5b       |        <p style="color:red">F</p>            |    
| P6a       |       <p style="color:green">T</p>            |     
| P6b       |       <p style="color:green">T</p>            |     
| P7       |       <p style="color:green">T</p>             | 



* `P1`: the partial design ensures the satisfaction of property `P1`. Indeed, the partial design 2 forces the system to start with a `usrReq` event, that occurs befor state `2` in which a `shipInfoReq` and a `prodInfoReq` can occur.
* `P2`: the partial design `2` violates the property `P2`. Specifically, the model checker returned the following counterexample:<br/>
`userReq`, `tau`, `offerRcvd`.<br/>
In this trace the event `offerRcvd` is not preceeded by the event `infoRcvd`.
* `P3a`: the partial design `2` violates the property `P3a`. Specifically, the model checker returned the following counterexample:<br/>
`userReq`, `tau`, `offerRcvd`, `usrNack`, `tau`, `reqCancelled`, `userReq`, `tau`, `offerRcvd`, `usrNack`, `tau`, `reqCancelled` `usrReq`, `tau`, `offerRcvd`, `usrNack`<br/>
In this trace the event `userReq` is never followed by a `shipReq`.
* `P3b`: the partial design `2` violates the property `P3b`. Specifically, the model checker returned the following counterexample:<br/>
`userReq`, `tau`, `offerRcvd`, `usrNack`, `tau`, `reqCancelled`, `userReq`, `tau`, `offerRcvd`,	`usrNack`, `tau`, `reqCancelled`, `userReq`, `tau`, `offerRcvd`, `usrNack`.	<br/>
In this trace the event `userReq` is never followed by a `prodReq`.
* `P5a`:  the partial design `2` violates the property `P5a`. Specifically, the model checker returned the following counterexample:<br/>
`userReq`, `tau`, `offerRcvd`, `usrAck`, `tau`, `respOk`, `userReq`, `tau`, `offerRcvd`, `usrAck`, `tau`, `respOk`, `userReq`, `tau`, `offerRcvd`, `usrAck`.	<br/>
In this trace the event `userReq` is not followed by a `reqCancelled`.
* `P5b`: the partial design `2` violates the property `P5b`. Specifically, the model checker returned the following counterexample:<br/>
`userReq`, `tau`, `offerRcvd`, `usrNack`, `tau`, `reqCancelled`, `userReq`, `tau`, `shipInfoReq`, `costAndTime`, `offerRcvd`, `usrAck`, `tau`, `respOk`, `userReq`, `tau`, `offerRcvd`, `usrAck`, `tau`, `respOk`, `userReq`, `tau`, `offerRcvd`, `userAck`. <br/>
In this trace the event `reqCancelled` follows the event `userReq` without being preceeded by a `shipCancelled`. 

## Partial design 2 with post-conditions
 
The following Table contains the results obtained by adding post-conditions to the boxes, where `T` means that the procedure returns a positive results while `F` specifies that the procedure failed.


By running the *model checker* it is possible to conclude that the partial design satisfies the following properties. 


| Property | Model checker | 
| ---------|-----------------------|
| P1       |        <p style="color:green">T</p>             |  
| P2       |        <p style="color:green">T</p>             |   
| P3a       |       <p style="color:green">T</p>             |    
| P3b       |       <p style="color:green">T</p>             |    
| P4a       |        <p style="color:green">T</p>           |     
| P4b       |        <p style="color:green">T</p>           |     
| P5a       |        <p style="color:green">T</p>            |    
| P5b       |        <p style="color:green">F</p>            |    
| P6a       |       <p style="color:green">T</p>            |     
| P6b       |       <p style="color:green">T</p>            |     
| P7       |       <p style="color:green">T</p>             | 


* `P1`: the property was already satisfied also without post-conditions;
* `P2`: the post-condition `<>(F_InfoRcvd)` on box `2` ensures the satisfaction of `P2`.
* `P3a`:  the post-condition `<>(F_ShipReq)` on box `4` ensures the satisfaction of `P3a`. 
* `P3b`: the post-condition `<>(F_ProdReq)` on box `4` ensures the satisfaction of `P3b`.
* `P4a`: the property was already satisfied also without post-conditions;
* `P4b`: the property was already satisfied also without post-conditions;
* `P5a`:  the post-condition `<>(F_ProdCancelled)` is added on box `5`.
* `P5b`:  the post-condition `<>(F_ShipCancelled)` is added on box `5`.
* `P6a`: the property was already satisfied also without post-conditions;
* `P6b`: the property was already satisfied also without post-conditions;
* `P7`: the property was already satisfied also without post-conditions;


TODO: show a refinement (without boxes) that satisfies the properties of interest 
=======
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
 e* Property `P1`: to verify `P1` click on *Check > Well-formedness Checker > PLAN_BOX_NO_PLAN_EXECUTION*. The property of interest is not satisfied.


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



>>>>>>> c0c727445a15ab11c8e5c067e8f5e17b13e3dfa8
