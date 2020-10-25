package main.paxos;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.*;

/**
 * ProposerRunnable
 * Runnable that first tries to issue a prepare request and then a proposal
 * Waits timeToPropose ms before issueing a prepare request
 *
 * This class was implemented in a event driven style using a Mealy Moore finite state machine
 * You can view this FSM in designs/ProposerFSM.jpg
 * This class directly implements that FSM.
 */
public class ProposerRunnable implements Runnable {
    // Values used to calculate unique proposalId
    private long proposalId = -1;
    private long proposalNum = 0;
    private final int N;
    private final int id;
    private int proposalValue = -1;

    BlockingQueue<String> messages;
    Set<Integer> promiseSet = new HashSet<Integer>();
    Set<Integer> acceptSet = new HashSet<Integer>();
    DelayedMessageExecutor sender;

    //Timing variables
    private final int timeToPropose;
    private final AtomicBoolean failure;

    // Mealy Moore FSM state variables
    private static final int PREPARE = 0;
    private static final int PROPOSAL = 1;
    private static final int DONE = 2;

    private int state;

    public ProposerRunnable(int N, int id, int timeToPropose, BlockingQueue<String> messages, DelayedMessageExecutor sender, AtomicBoolean failure) {
        this.N = N;
        this.id = id;
        this.messages = messages;
        this.timeToPropose = timeToPropose;
        this.sender = sender;
        this.failure = failure;
    }
    
    // Reads message queue with a timeout
    // Where 'events' come from in terms of our FSM
    public void run() {
        //Always begin in the prepare state
        next(PREPARE);
        while (true) {
            // Get next message
            String message = null;
            try {
                message = messages.poll(15, TimeUnit.SECONDS);              
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }

            // If no messages were received in 15 seconds
            // Trigger a timeout
            if (message == null) {
                timeout();
                continue;
            }

            char type = message.charAt(0);
            message = message.substring(1);

            switch (type) {
                case MessageCodes.PROMISE:
                    receivePromise(message);
                    break;

                case MessageCodes.PREPARENACK:
                    receivePromisenack();
                    break;

                case MessageCodes.ACCEPT:
                    receiveAccept(message);
                    break;

                case MessageCodes.PROPOSALNACK:
                    receiveAcceptnack();
                    break;
                
                default:
                    throw new RuntimeException("Unknown message type in ProposerRunnable.run(): " + type);
            }
        }
    }

    // Handling actions on entry to a state
    private void next(int newState) {
        state = newState;
        switch (state) {
            case PREPARE:
                try {
                    Thread.sleep(timeToPropose);
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
                setUniqueProposal();
                promiseSet.clear();
                broadCastPrepare();
                return;

            case PROPOSAL:
                broadCastProposal();
                acceptSet.clear();
                return;

            case DONE:
                System.out.println("VALUE HAS BEEN CHOSEN: " + Integer.toString(proposalValue));
                System.exit(0);
                return;

            default:
                throw new RuntimeException("Unknown Proposer new state: " + Integer.toString(state));      
        }
    }

    private int maxAcceptedId = -1;
    private void receivePromise(String message) {
        String[] messageArr = message.split(" ");
        int fromId = Integer.parseInt(messageArr[0]);
        int fromProposalId = Integer.parseInt(messageArr[1]);

        switch (state) {
            case PREPARE:
                // If promise is from an old prepare, do nothing
                if (fromProposalId != proposalId)
                    return;

                // Add promise to set
                promiseSet.add(fromId);
                // If accepted value was set
                if (messageArr.length == 4) {
                    int fromAcceptedId = Integer.parseInt(messageArr[2]);
                    if (fromAcceptedId > maxAcceptedId) {
                        maxAcceptedId = fromAcceptedId;
                        proposalValue = Integer.parseInt(messageArr[3]);
                    }
                }

                // If we've accumulated majority promises, move to state PROPOSAL
                if (promiseSet.size() >= (N/2 + 1))
                    next(PROPOSAL);
                return;

            case PROPOSAL:
                //do nothing
                return;

            default:
                throw new RuntimeException("Unknown Proposer new state: " + Integer.toString(state));
        }
    }

    private void receivePromisenack() {
        switch (state) {
            case PREPARE:
                next(PREPARE);
                return;

            case PROPOSAL:
                // do nothing
                return;

            default:
                break;
        }
    }

    private void receiveAccept(String message) {
        String[] messageArr = message.split(" ");
        int fromId = Integer.parseInt(messageArr[0]);
        int fromProposalId = Integer.parseInt(messageArr[1]);

        switch (state) {
            case PREPARE:
                // do nothing
                return;

            case PROPOSAL:
                // If accept message is from different proposal
                // Do nothing
                if (fromProposalId != proposalId)
                    return;

                acceptSet.add(fromId);

                if (acceptSet.size() >= (N/2 + 1))
                    next(DONE);
                return;

            default:
                throw new RuntimeException("Unknown Proposer new state: " + Integer.toString(state));      
        }  
    }

    private void receiveAcceptnack() {
        switch (state) {
            case PREPARE:
                // do nothing
                return;

            case PROPOSAL:
                next(PREPARE);
                break;

            default:
                throw new RuntimeException("Unknown Proposer new state: " + Integer.toString(state));
        }
    }

    
    private void timeout() {
        switch (state) {
            case PREPARE:
                next(PREPARE);
                return;

            case PROPOSAL:
                next(PREPARE);
                break;

            default:
                throw new RuntimeException("Unknown state for timeout: " + Integer.toString(state));
        }
    }

    // *** HELPERS *** (non-FSM helper methods)
    
    //Sets a globally unique proposal id by:
    //proposalId = id + N*proposalNum
    //as long as other proposers follow the same scheme and
    //id is unique and all ids are in range [0,N-1] 
    //this will be globally unique
    private void setUniqueProposal() {
        proposalId = id + N*proposalNum;
        proposalNum++;
    }

    //Broadcasts a prepare request to all mebers
    private void broadCastPrepare() {
         for (int recipient = 0; recipient < N; recipient++) {
             if (recipient != id)
                 sender.send(String.format("%c%d %d", MessageCodes.PREPARE, id, proposalId), recipient);
         }  
    }

    //Broadcasts a proposal request to all members
    private void broadCastProposal() {
         for (int recipient = 0; recipient < N; recipient++) {
             if (recipient != id)
                 sender.send(String.format("%c%d %d %d", MessageCodes.PROPOSAL, id, proposalId, proposalValue), recipient);
         }  
    }
}
