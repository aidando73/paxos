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
    public static final char PREPARE = 'a';
    public static final char PROMISE = 'b';
    public static final char PROPOSAL = 'c';
    public static final char ACCEPT = 'd';
    public static final char PREPARENACK = 'e';
    public static final char PROPOSALNACK = 'f';
}
