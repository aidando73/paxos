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
public enum MessageCodes {
    PREPARE,
    PROMISE,
    PROPOSAL,
    ACCEPT,
    NACK
}
