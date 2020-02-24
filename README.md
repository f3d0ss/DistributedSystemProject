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

### Centralized “tracker” server
This server keeps track of every replica in the network
### Replicas
All the replicas that can change the state
### Clients
All nodes that start Reads and Writes

## Protocol
All communications use TCP channel
### Replica Joining the network
- When a new Replica **R** is created it asks the Tracker to join the network.
- The Tracker adds **R** to the list of Replicas and sends back to **R** the entire list and **R** address to all the other Replicas.
- When **R** receives the list it first need a valid state, so it asks to one (maybe more to avoid packet loss) of the Replicas G to give it the current state (if G isn't malicious the state should be Causal Consistent), if it don't receive an answer in *X* second it retry with a different replica (or even the same).
- After **R** receive a valid state (if more are received it discard all the states after the first) it has officially joined the network.

### Communication between Replicas
- When a Replica receives a Write/Update from a Client it updates its current state and sends his current state with a Vector Clock **V** to all the other Replicas
- When a Replica **R** receives the Write/Update with a Vector Colck **V<sub>Q</sub>** from another Replica Q 
if:
    - **V<sub>R</sub>[Q] + 1 = V<sub>Q</sub>[Q] and V<sub>R</sub>[k] >= V<sub>Q</sub>[k] for all k!=Q** <br>
      Then execute the Write/Update received 
    - *Otherwise* <br> Hold the message in a queue and waits for the intermediate messages

### Replica Exit the network
- When a Replica **R** wants to exit the network it sends an `exit` message to the Traker
- When the Traker receives the `exit` message it remove **R** from the list of Replicas and inform all the other Replicas that **R** has left the network
- Note: if there were clients connected to **R**, after find **R** unreachable, they ask the Traker for a new Replica 


### Client Joining the network
- When a Client **C** start, it asks to the Traker for a Replica address
- The Traker sends back the address of the Replica with less clients connected (could also take into account the locality) and increment Replica load
- When **C** receives the address it opens a socket with the Replica
- **C** starts to perform reads and writes on the connected Replica

### Client Exit the network
- When a Client **C** wants to exit it sends an `exit` message with the IP of the current Replica to the connected Traker
- The Traker decrement the load of the Replica where **C** was connected