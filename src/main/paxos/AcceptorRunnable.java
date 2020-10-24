package main.paxos;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * AcceptorRunnable
 * Runnable that responds to prepare and proposals
 * coming from a producer/consumer style queue
 */

public class AcceptorRunnable implements Runnable {
    int id;

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
            try {
                String message = messages.take();
                // Separate the type from message
                MessageCodes type = (MessageCodes)Character.getNumericValue(message.charAt(0));
                message = message.substring(1);

                switch (type) {
                    case PREPARE:
                        handlePrepare(message);                  
                        break;

                    
                    default:
                        throw new RuntimeException("Unknown Code for Acceptor: " + type);
                }
                
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void handlePrepare(String message) {
         String[] messageArr = message.split(" ");     
         int from = Integer.parseInt(messageArr[0]);
         int n = Integer.parseInt(messageArr[1]);

         if (n > maxPromise) {
            sender.send(String.format("%d %d", id, n), from);
        }
    }
}
