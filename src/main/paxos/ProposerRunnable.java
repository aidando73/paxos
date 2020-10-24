package main.paxos;

/**
 * ProposerRunnable
 * Runnable that performs 4 tasks:
 * 1: Broadcasts prepare request
 * 2: Receives and counts promises
 * 3: Broadcasts proposal requests
 * 4: Receives and counts acceptances
 *
 * ProposerRunnable does each task synchronously
 * moving to the next step only if it received majority votes from the previous step
 * You can see a FSM of this Runnable in designs/ProposerFSM.png
 */
public class ProposerRunnable implements Runnable {

    
}
