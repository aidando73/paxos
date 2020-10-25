# Assignment 3 - Paxos Protocol
###### a1742494 Huy Gia Do Vu

This is Assignment 3, a multi-threaded simulation of the Paxos Protocol as defined by https://lamport.azurewebsites.net/pubs/paxos-simple.pdf. It is recommended to read this document in an markdown reader.

#### Features
- Isolated unit tests using Mockaroo and Junit.
- Paxos Compliant
- Members have dynamic behaviour that you can define easily via a JSON config file
    - Message delay
    - Time to fail
    - Time to restart
    - How many members

### Getting Started
I have submitted this entire folder to web-submission. However, in the case that there are problems. I've also included a zip file called project.zip. If all else fails, you can clone this project on https://github.com/REslim30/paxos.

#### Design
The system consists of 3 main components:

    1. Email Server.
    2. Email Client.
    3. Member.

You can see how these components interact in designs/highLevel.jpg

The email server and email client talk to one another via sockets. An email client can send messages to other email clients by simply attaching their id in the message. The email server redirects messages to the correct recipient.

A Member is where Paxos is actually implemented. They handle all the logic of Paxos, using an Email Client to contact other Members.

#### Message Specification
All messages are sent as: <email>

A <email> consists of:

<recipient-id>:<paxos-message>


### Notes
#### Specific Implementation Notes
- The distiguished learner is the proposer that issued the proposal.
    - Since many nodes might ignore messages, or be unavailable, there needs to be a separate protocol for when a distinguished learner tries to communicate with all other nodes in the system about the chosen value. As this assignment is about Paxos, I've decided that this is out of scope. Once nodes have chosen a value, there is no need to run Paxos any further and as such the algorithm terminates. Of course, in the real world we'd need a way of having all learner nodes learn the value and restarting paxos for the next value.
- All members are simultaneously proposers, and acceptors.
- Non-byzantine, asynchronous message model:
    - Nodes can fail, operate at arbitrary speed, or can restart.
    - Extra: Messages can be duplicated, take arbitrarily long to be delivered and lost.
- Crashes will be simulated **not** actually executed. Truly crashing a thread and restarting the connection is out of scope for this assignment. We will instead simulate the behaviour of a node that does crash by:
    - Ignoring all messages that were sent during failure.
- Proposals/Prepare are broadcast.
- Acceptors can send promisenack or acceptnack messages on:
    - receiving a prepare request with proposal id less than the latest promise's proposal id.
    - receiving a proposal request with proposal id less than the latest promise's proposal id.
- Proposers abandon proposals if receiving an promisenack or acceptnack whilst in the PREPARE and PROPOSAL stages respectively. This is because, if another proposer is issueing a higher proposal number than us. Then it's unlikely that our proposal will be chosen since this other proposer will have their proposals and prepare requests prioritized over ours. This does not affect correctness in anyway as proposers can abadon their request at any time.
- A proposer will time-out if it goes for 15 seconds without receiving a message. At that point it will reset to PREPARE phase.
