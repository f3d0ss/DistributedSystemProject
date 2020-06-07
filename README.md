# Distributed System Project

Implement a replicated key-value store that offers causal consistency.

## Requirements
- Implement causal consistency with limited (coordination) overhead.
- New replicas can be added or removed at runtime. 
- The store can be implemented as a real distributed application (for example, in Java) together with some client code to use / test the implementation, or it can be simulated using OmNet++. In the first case you are allowed to use only basic communication facilities (i.e., sockets and RMI, in case of Java). 
## Assumption
- Processes are reliable.
- Channels are point-to-point (no broadcast) and you may assume the same fault model of the Internet (congestions or partition).
- Clients are "sticky": they always interact with the same replica.

## Components

### Centralized "Tracker" Server
The main server, it coordinates the replicas and redirects the clients to an available replica.
### Replicas
The data storages, they contain all the data written by users and exchange updates (vector clocks) with other replicas.
### Clients
The users of the system, they connect to a Replicas through the Tracker and can then perform read and write operations.

## Abstract

We had to implement causal consistency, so the easy choice was to implement a vector clock, but vector clock is not usually implemented for dynamic system like this, where replicas join and leave over time.<br>
An easy solution would have been to add indexes to the vector clock as replicas join the network, and keep these indexes even when the replicas leave, but one of the requirements was to use `limited (coordination) overhead`. 
Also consider the case where a new Replica C joins the network and obtains the state from Replica B and, meanwhile, Replica A, who doesn't know yet about C, sends an update to B. Normally in this case C would be cut off from any new update from B and A because it would wait the update from B before apply subsequent updates.<br>
So we came up with a "fix" to the protocol adding a TrackerIndex.<br>
The Tracker keeps track of all the Replicas in the network. When a Replica joins or leaves the network it sends to every other Replica in the network an update with the current TrackerIndex which is incremented for each update sent.
The TrackerIndex is used to understand which replica has the most recent list of indexes when replicas have different indexes in the vector clock.
This makes possible to discard the indexes of a Replica that left the network whilst keeping a limited overhead (especially if the protocol has to run for a long time with a lot of replicas joining and leaving) and to fix the problem with the join. Having this TrackerIndex, it is sufficient that when a Replica B receives an update from a Replica A that "know less", B replies warning A that it could be missing some Replicas, in this way A can store the update until it receives the update with the new address from the Tracker.


## Protocol
All communications use TCP channel
### Replica Joins the network
- When a new Replica **R** is created it asks the Tracker to join the network.
- The Tracker adds **R** to the list of Replicas and sends back to **R** the entire list and the new TrackerIndex and sends **R** address to all the other Replicas.
- When **R** receives the list, it first needs a valid state, so it asks one of the Replicas G to send it, if it doesn't receive an answer, it retries a different replica.
- After **R** receives a valid state it has officially joined the network.

### Communication between Replicas
- When a Replica receives a Write from a Client it updates its current state and sends the Update with its Vector Clock **V** to all the other Replicas
- When a Replica **R** receives the Update with a Vector Clock **V<sub>Q</sub>** from another Replica **Q** 
if:
    - The Update TrackerIndex is greater than **R**'s trackerIndex, if k is present in **V<sub>Q</sub>** and not in **V<sub>R</sub>** **V<sub>R</sub>**[k] = 0 and if k is present in **V<sub>R</sub>** and not in **V<sub>Q</sub>** k is simply ignored
    - The Update TrackerIndex is lower than **R**'s trackerIndex, if k is present in **V<sub>Q</sub>** and not in **V<sub>R</sub>** k is simply ignored and if k is present in **V<sub>R</sub>** and not in **V<sub>Q</sub>** **V<sub>Q</sub>**[k] = 0
    
- Then if: 
    - **V<sub>R</sub>[Q] + 1 = V<sub>Q</sub>[Q] and V<sub>R</sub>[k] >= V<sub>Q</sub>[k] for all k!=Q** <br>
      Then executes the Write/Update received 
    - *Otherwise* <br> Holds the message in a queue and waits for the intermediate messages
    
- Finally if The Update TrackerIndex is lower than **R**'s trackerIndex **R** replies with a WAIT message with its trackerIndex (to let **Q** know that it may have an outdated list of replicas), otherwise it replies with an ACK

Note: if the Update TrackerIndex is greater than **R**'s trackerIndex it means that **Q** has a more up-to-date list of replicas, so:
if **R** doesn't have an index that **Q** has, it means that **R** didn't received the update of a join of a new replica from the tracker yet, so it can simply consider that index equal to 0;
if **R** has an index k that **Q** doesn't have it means that **R** doesn't know yet that replica k left the network, but thanks to the fact that a replica will leave the network only after it is sure that all other replicas has received all its update, it can simply ignore that index because it certainly is at the maximum value;

### Replica Exits the network
- When a Replica **R** wants to exit the network it sends an `exit` message to the Tracker
- When the Tracker receives the `exit` message it removes **R** from the list of Replicas and informs all the other Replicas that **R** has left the network sending a REMOVE message with the updated TrackerIndex
- Note: if there were clients connected to **R**, after they find that **R** is unreachable, they ask the Tracker for a new Replica 

### Client Joins the network
- When a Client **C** starts, it asks the Tracker for a Replica address
- The Tracker sends back the address of the Replica with less clients connected and increment the Replica load
- **C** starts to perform reads and writes on the connected Replica

### Client Exits the network
- When a Client **C** wants to exit, it sends an `exit` message with the IP of the current Replica to the connected Tracker
- The Tracker decrements the load of the Replica where **C** was connected
