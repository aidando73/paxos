package main.paxos;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static main.paxos.MessageCodes.*;


/**
 * AcceptorRunnable
 * Runnable that responds to prepare and proposals
 * coming from a producer/consumer style queue
 */

public class AcceptorRunnable implements Runnable {
    private final int id;

    private long maxPromise = -1;
    private long maxAccept = -1;
    private int maxAcceptValue = -1;

    private final BlockingQueue<String> messages;
    private final AtomicBoolean failure;
    private final DelayedMessageExecutor sender;

    public AcceptorRunnable(BlockingQueue<String> messages, DelayedMessageExecutor sender, AtomicBoolean failure, int id) {
        this.messages = messages;
        this.failure = failure;
        this.sender = sender;
        this.id = id;
    }
    
    public void run() {
        while (true) {
            // Pretend to fail if failure == true and clear all messages
            // While we're still using AtomicBoolean we must own the monitor
            // In order to wait
            while (failure.get()) {
                synchronized (failure) {
                    try {
                        failure.wait();
                    } catch (Exception e) {
                    }
                }
                messages.clear();
            }

            String message = "";
            try {
                message = messages.take();

            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }

            // Separate the type from message
            char type = message.charAt(0);
            message = message.substring(1);
            // Handle PREPARE and PROPOSAL
            switch (type) {
                case PREPARE:
                    handlePrepare(message);                  
                    break;

                case PROPOSAL:
                    handleProposal(message);
                    break;
                
                default:
                    throw new RuntimeException("Unknown Code for Acceptor: " + type);
            }
                
        }
    }

    private void handlePrepare(String message) {
        String[] messageArr = message.split(" ");     
        int from = Integer.parseInt(messageArr[0]);
        int n = Integer.parseInt(messageArr[1]);

        if (n > maxPromise) {
            // If haven't accepted any proposals
            if (maxAccept == -1) {
                message = String.format("%c%d %d", PROMISE, id, n);

            // If have accepted proposals
            } else {
                message = String.format("%c%d %d %d %d", PROMISE, id, n, maxAccept, maxAcceptValue);
            }
            maxPromise = n;
        } else {
            message = String.format("%c%d %d", PREPARENACK, id, n);
        }
        sender.send(message, from);
    }

    private void handleProposal(String message) {
        String[] messageArr = message.split(" ");     
        int from = Integer.parseInt(messageArr[0]);
        int n = Integer.parseInt(messageArr[1]);
        int v = Integer.parseInt(messageArr[2]);

        //Send accept if we haven't promised
        if (maxPromise < n) {
            sender.send(String.format("%c%d %d", ACCEPT, id, n), from);
            // Set new accepted max if required
            if (maxAccept < n) {
                maxAccept = n;
                maxAcceptValue = v;
            }
        //Send a PROPOSALNACK if we did promise
        } else {
            sender.send(String.format("%c%d %d", PROPOSALNACK, id, n), from);
        }
    }
}
