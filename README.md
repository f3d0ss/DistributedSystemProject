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

### Centralized "tracker" server
This server keeps track of every replica in the network
### Replicas
All the replicas that can change the state
### Clients
All nodes that start Reads and Writes

## Abstract

We had to implement causal consistency, so the easy choice was implementing a vector clock, but vector clock is not usually implemented for dynamic system like this, where replica join e leave over time.<br>
The easy solution would have been add indexes to the vector clock as replicas join the network and leave them in even if they leave, but in the requirement we have `with limited (coordination) overhead`. And what if a new Replica C joined the network and got the state from B and Replica A who doesn't yet know about C send an update to B? Normally in this case C would be cut off from any new update from B and A because it would wait the update from B before apply subsequent update.<br>
So we came up with a "fix" to protocol adding a TrackerIndex.<br>
We have a Tracker that keep track of all the Replicas in the network, and when a Replica join or leave the network it send to all the Replicas in the network the update with the current TrackerIndex that is incremented for each update sent.
This TrackerIndex is used if two replica has different indexes in the vector clock to understand which replica has the most recent list of indexes making possible to discard indexes of Replica that left the network limiting the overhead (especially if the protocol has to run for a long time with a lot of replica joining and leaving) and it making possible to fix the problem with the join. Having this TrackerIndex it is sufficient that when a Replica B receives an update from a Replica A that "know less" B reply warning A that it could be missing some Replica, in this way A can store the update until it receive the update with the new address from the Tracker.


## Protocol
All communications use TCP channel
### Replica Joining the network
- When a new Replica **R** is created it asks the Tracker to join the network.
- The Tracker adds **R** to the list of Replicas and sends back to **R** the entire list and the new TrackerIndex and sends **R** address to all the other Replicas.
- When **R** receives the list it first need a valid state, so it asks to one of the Replicas G to give it the current state , if it doesn't receive an answer it retry with a different replica.
- After **R** receive a valid state it has officially joined the network.

### Communication between Replicas
- When a Replica receives a Write from a Client it updates its current state and sends the Update with its Vector Clock **V** to all the other Replicas
- When a Replica **R** receives the Update with a Vector Clock **V<sub>Q</sub>** from another Replica Q 
if:
    - The Update TrackerIndex is greater than **R**'s trackerIndex if k is present in **V<sub>Q</sub>** and not in **V<sub>R</sub>** **V<sub>R</sub>**[k] = 0 and if k is present in **V<sub>R</sub>** and not in **V<sub>Q</sub>** k is simply ignored
    - The Update TrackerIndex is lower than **R**'s trackerIndex if k is present in **V<sub>Q</sub>** and not in **V<sub>R</sub>** k is simply ignored and if k is present in **V<sub>R</sub>** and not in **V<sub>Q</sub>** **V<sub>Q</sub>**[k] = 0
    
- Then if: 
    - **V<sub>R</sub>[Q] + 1 = V<sub>Q</sub>[Q] and V<sub>R</sub>[k] >= V<sub>Q</sub>[k] for all k!=Q** <br>
      Then execute the Write/Update received 
    - *Otherwise* <br> Hold the message in a queue and waits for the intermediate messages
    
- Finally if The Update TrackerIndex is lower than **R**'s trackerIndex **R** reply with a WAIT message with its trackerIndex (to let **Q** knows that it may have an outdated list of replicas), otherwise it reply with an ACK

Note: if the Update TrackerIndex is greater than **R**'s trackerIndex it means that **Q** has a more updated list of replicas, so:
if **R** doesn't have an index that **Q** has it means that **R** didn't received the update of a join of a new replica from the tracker yet, so it can simply consider that index = to 0;
if **R** has an index k that **Q** doesn't have it means that **R** doesn't know yet that replica k left the network, but thanks to the fact that a replica will leave the network only after it is sure that all other replicas has received all its update, it can simply ignore that index because it certainly is at the maximum value;

### Replica Exit the network
- When a Replica **R** wants to exit the network it sends an `exit` message to the Tracker
- When the Tracker receives the `exit` message it remove **R** from the list of Replicas and inform all the other Replicas that **R** has left the network sending a REMOVE message with the updated TrackerIndex
- Note: if there were clients connected to **R**, after find **R** unreachable, they ask the Tracker for a new Replica 

### Client Joining the network
- When a Client **C** start, it asks to the Tracker for a Replica address
- The Tracker sends back the address of the Replica with less clients connected (could also take into account the locality) and increment Replica load
- **C** starts to perform reads and writes on the connected Replica

### Client Exit the network
- When a Client **C** wants to exit it sends an `exit` message with the IP of the current Replica to the connected Tracker
- The Tracker decrement the load of the Replica where **C** was connected
