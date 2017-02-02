# Purchase&Delivery

In the purchase and delivery example, we imagine that the developer has to design a system that manages book orders.
The users purchese books online which must be delivered.
The purchase and delivering component must use a furniture-sale and a shipping service to provide the desired functionality to the user.

We consider two partial design for the purchase and delivering component. 
These designs are contained in the file `purchaseAndDelivery.lts`, where the two purchase and delivering components are indicated as `purchaseAndDelivery1` and `purchaseAndDelivery2`, respectively.

Before discussing the two designs, we describe the environment in which they operate and the properties the components aim to ensure.

#### Environment
The environment where the components are deployed is composed by three components:
* *Furniture sale*: aims at providing information about books and provide the books for being delivered
* *Shipping service*: provides information about cost and time of the shipping and allows to ship a book
* *User*: performs requests to the purchase and delivery system

The purchase and delivering component must synchronize the *Furniture sale*, the *Shipping service* and the *User*.
#### Properties of interest

* the system must check the presence of some book or ask shipping info only if the user sent a request:
  * `P1=[]( ! ( (!F_UsrReq) U (F_ShipInfoReq || F_ProdInfoReq) ) )`;
* an offer is provided to the user only if the furniture service has confirmed the availability of the requested product:
  * `P2=[]( ! ( (!F_InfoRcvd) U F_OfferRcvd)) `;
* furniture and shipping are activated only if the user has decided to purchase (i.e., a `userAck` always precedes a `shipReq` and a `userAck` always precedes a `prodReq`):
  * `P3=( ([]( F_UserAck W F_ShipReq))   && ([](F_UserAck W F_ProdReq))  )`;
* the system allows canceling an order only if the cancellation procedure was initiated by a user (i.e., a `prodCancel` or `shipCancel` event is always precedeed by a `userNack`):
  * `P4=[](F_UserNack W (F_ProdCancel || F_ShipCancel))`;
* a request is marked as canceled when both the product ordering and the shipping services have canceled the request (i.e., `F_ProdCancelled` and `F_ShipCancelled` always occur before `F_ReqCancelled`):
  * `P5=[]( (F_ProdCancelled W F_ReqCancelled) && (F_ShipCancelled W F_ReqCancelled))`;
* a request succeeds only when both the product ordering and the shipping service have handled their requests correctly (i.e., the event `respOk` can occur only after the `prodReq` and the `shipReq` events occurred. These events indicate that a product request and a shipping request have been received and handled correctly by the furniture sale and the shipping services):
  * `P6=[]( (F_RespOk W prodReq) && (F_RespOk W shipReq) )`;
* a request succeeds if the user accepts the offer or fails in the opposite case:
  * `P7=[]( (F_UserAck-> <>F_RespOk) && (F_UserNack-> <>F_ReqCancelled) )`.

## Partial design 1


#### Experiment 1: Partial component with no post-conditions
The following Table contains the results obtained without adding post-conditions to the state 2, where `OK` means that the procedure returns a positive results while `KO` specifies that the procedure failed.

| Property | Realizability Checker | Model Checker |
| ---------|-----------------------|---------------|
| P1       |        <p style="color:red">KO</p>             |     <p style="color:red">KO</p>          |
| P2       |        <p style="color:red">KO</p>             |     <p style="color:red">KO</p>          |
| P3       |       <p style="color:red">KO</p>             |     <p style="color:red">KO</p>          |
| P4       |        <p style="color:red">KO</p>           |      <p style="color:red">KO</p>         |
| P5       |        <p style="color:red">KO</p>            |      <p style="color:red">KO</p>         |
| P6       |       <p style="color:green">OK</p>            |      <p style="color:red">KO</p>         |
| P7       |       <p style="color:green">OK</p>             |          <p style="color:red">KO</p>     |

## Partial design 2

