# Assignment 3 - Paxos Protocol
###### a1742494 Huy Gia Do Vu

This is Assignment 3, a multi-threaded simulation of the Paxos Protocol as defined by https://lamport.azurewebsites.net/pubs/paxos-simple.pdf. 

#### Features
- A JSON frontend
- Comprehensive unit tests

### Getting Started

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


