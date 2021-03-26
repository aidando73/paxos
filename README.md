# Assignment 3 - Paxos Protocol
###### a1742494 Huy Gia Do Vu

This is Assignment 3, a multi-threaded simulation of the Paxos Protocol as defined by [Paxos Made Simple](https://lamport.azurewebsites.net/pubs/paxos-simple.pdf).

#### Features
- Unit tests using Mockaroo and Junit.
- Paxos Compliant
- Members have dynamic behaviour that you can define easily via a JSON config file
    You can define however many Member you want. For each member you can define
        - Four profiles of response times: immediate, medium, late, never.
        - Time to fail
        - Time to restart
        - ambition (whether or not to propose for themselves initially)

### Getting Started
I have submitted this assignment a zip file called project.zip. In case of any issues with websub, you can clone this project on https://github.com/REslim30/paxos. I've included in the project.zip file the .git folder as proof that this work is mine.

There is only one command you need to know:

    make run

This will simulate scenarios defined in **config.json**. I encourage you to toy around with different settings. config.json is defined as follows:
```
[
    <scenario>,
    <scenario>,
    <scenario>,
    ...
]

<scenario> =
{
    "name": "any string",
    "description": "any string"
    "members": [
        <member>,
        <member>,
        <member>
    ]
}

<member> = 
{
    "timeToPropose": <int>,
    "timeToFail": <int>,
    "timeToRestart": <int>,
    "ambition": <boolean>,
    "responseTime": "[IMMEDIATE|MEDIUM|LATE|NEVER]"
}

Where:
    timeToPropose    ->    Interval in which member proposers (ms)
    timeToFail       ->    Interval in which member becomes unavailable (ms)
    timeToRestart    ->    Interval in which member becomes available after becoming unavailable (ms)
    responseTime     ->    responsiveness of member. Only accepts "IMMEDIATE", "MEDIUM", "LATE", "NEVER"
    ambition         ->    If true, will initially propose for themself. Otherwise will propose randomly.

Note: all member entries are optional. If not present, they will take on the following default failues
    timeToPropose = -1         (Never proposes)
    timeToFail = -1            (Never fails)
    timeToRestart= -1          (Never restart)
    responseTime = "IMMEDIATE"
    ambition = false
```

Currently, **config.json** includes the two scenarios defined in the assignment specification.

## Design
The system consists of 3 main components:

    1. Email Server.
    2. Email Client.
    3. Member.

You can see how these components interact in designs/highLevel.jpg

The email server and email client talk to one another via sockets. An email client can send messages to other email clients by simply attaching their id in the message. The email server redirects messages to the correct recipient.

A Member is where Paxos is actually implemented. They handle all the logic of Paxos, using an Email Client to contact other Members.

### Terminology
Paxos has 4 different types of messages corresponding to 4 particular phases. I've unofficially given them names:
1. Prepare

    Member tries to request a promise that receiver will not accept any request with proposal-id higher than the prepare request proposal-id

2. Promise

    In response to a prepare request, member promises sender that they will not accept any request with proposal-id higher than the prepare request proposal-id.    

3. Proposal

    In response to receiving a majority of promises among acceptors. Member tries to send a proposal in hopes for a majority of accepts.

4. Accept

    In response to a proposal, Member accepts proposal only if they have not promised otherwise.

We are done when a proposer has received majority votes.

In order to increase efficiency, I've introduced two extra messages that can be sent:

1. Preparenack
    
    If a reciever cannot promise such a prepare request.

2. Proposalnack

    If a reciever cannot accept such a proposal request.

### Message Specification
> Note: understanding of the messages are only required to understand the code.

```
All messages are sent as: {email}

{email} = {recipient-id}:{paxos-message}

There are six types of {paxos-messasge}:
    {prepare-message} = PREPARECHAR{from-id} {proposal-id}
    {promise-message} = PROMISECHAR{from-id} {proposal-id} [{max-accepted-proposal-id} {max-accepted-proposal-value}]
    {proposal-message} = PROPOSALCHAR{from-id} {proposal-id} {proposal-value}
    {accept-message} = ACCEPTCHAR{from-id} {proposal-id}
    {preparenack-message} = PREPARENACKCHAR{from-id} {proposal-id}
    {proposalnack-message} = PROPOSALNACKCHAR{from-id} {proposal-id}
```

### Tests
All tests are included in the `src/test` folder. The following commands run them:

    make test_eserver            ->      tests for email server
    make test_eclient            ->      tests for email client
    make test_paxos              ->      tests for paxos implementation

### Notes
- The distiguished learner is the proposer that issued the proposal.
- Since many nodes might ignore messages, or be unavailable, there needs to be a separate protocol for when a distinguished learner tries to communicate with all other nodes in the system about the chosen value. As this assignment is about Paxos, I've decided that this is out of scope. Once nodes have chosen a value, there is no need to run Paxos any further and as such the algorithm terminates. Of course, in the real world we'd need a way of having all learner nodes learn the value and restarting paxos for the next value.
- All members are simultaneously proposers, and acceptors.
- Non-byzantine, asynchronous message model:
    - Nodes can fail, operate at arbitrary speed, or can restart.
    - Messages can be duplicated, take arbitrarily long to be delivered and lost.
- Crashes will be simulated **not** actually executed. Truly crashing a thread and restarting the connection is out of scope for this assignment. We will instead simulate the behaviour of a node that does crash by:
    - Ignoring all messages that were sent during failure.
    - Stop making prepare/proposal requests during failure.
- Proposals/Prepare are broadcast.
- Acceptors can send promisenack or acceptnack messages on:
    - receiving a prepare request with proposal id less than the latest promise's proposal id.
    - receiving a proposal request with proposal id less than the latest promise's proposal id.
- Proposers abandon proposals if receiving an promisenack or acceptnack whilst in the PREPARE and PROPOSAL stages respectively. This is because, if another proposer is issueing a higher proposal number than us. Then it's unlikely that our proposal will be chosen since this other proposer will have their proposals and prepare requests prioritized over ours. This does not affect correctness in anyway as proposers can abadon their request at any time.
- A proposer will time-out if it goes for 15 seconds without receiving a message. At that point it will reset to PREPARE phase.
- Initially to reduce the likelihood of livelock I set each prepare request to be resent at random intervals (between 0 and 1 second). However I found that this made the algorithm too efficient for the purposes of marking. Members were reaching consensus in only 2 or 3 prepare calls. Thus I've turned it off to demonstrate that I've implemented paxos.
