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

### Joining the network
- When a new Replica **R** is created it asks the Tracker to join the network.
- The Tracker adds **R** to the list of Replicas and sends back to **R** the entire list and **R** address to all the other Replicas (for every message if it doesn't receive an ACK the Tracker resend it).
- When **R** receives the list it first need a valid state, so it asks to one (maybe more to avoid packet loss) of the Replicas G to give it the current state (if G isn't malicious the state should be Causal Consistent), if it don't receive an answer in *X* second it retry with a different replica (or even the same).
- After **R** receive a valid state (if more are received it discard all the states after the first) it has officially joined the network.

### Communication between replicas
- When a Replica receives a Write/Update from a Client it updates its current state and sends his current state with a Vector Clock **V** to all the other Replicas and if it doesn't receive an acknowledge in *X* second it retransmits the message
- When a Replica **R** receives the Write/Update with a Vector Colck **V<sub>Q</sub>** from another Replica Q, it sends an acknowledge and 
if:
    - **V<sub>R</sub>[Q] + 1 = V<sub>Q</sub>[Q] and V<sub>R</sub>[k] >= V<sub>Q</sub>[k] for all k!=Q** <br>
      Then execute the Write/Update received 
    - *Otherwise* <br> Hold the message in a queue and waits for the intermediate messages

### Replica want to exit
<!-- TODO -->