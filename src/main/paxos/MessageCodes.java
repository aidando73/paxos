package main.paxos;

/**
 * MessageCodes
 * Defines the characters to identify 5 types of messages:
 * Prepare
 * Promise
 * Proposal
 * Accept
 * Nack
 */
public class MessageCodes {
    public static char PREPARE = 'a';
    public static char PROMISE = 'b';
    public static char PROPOSAL = 'c';
    public static char ACCEPT = 'd';
    public static char NACK = 'e';
}
