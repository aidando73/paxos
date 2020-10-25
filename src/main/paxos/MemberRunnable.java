package main.paxos;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import main.eclient.EmailClient;
import static main.paxos.MessageCodes.*;

/**
 * MemberRunnable
 * Implements Paxos Protocol with N other members
 * Spawns an acceptor thread and proposer thread
 * This class mainly handles simulating failure
 */
public class MemberRunnable implements Runnable {
    //Time for member to crash
    private int timeToFail;
    //Time for member to restart after crash
    private int timeToRestart;
    private AtomicBoolean fullShutdown;
    private int id;

    private EmailClient eClient;

    // Acceptor and Proposer management variables
    private Thread proposer;
    private Thread acceptor;
    private BlockingQueue<String> proposerMessages;
    private BlockingQueue<String> acceptorMessages;
    private AtomicBoolean failure;
    private AtomicBoolean shutdown;

    public MemberRunnable(
        int port,
        ResponseTime responseTime,
        int timeToFail,
        int timeToRestart,
        int timeToPropose,
        int id,
        int N,
        AtomicBoolean fullShutdown,
        boolean ambition
    ) {
        // Store variables 
        this.timeToFail = timeToFail;
        this.timeToRestart = timeToRestart;
        this.id = id;

        // Create proposer and acceptor threads
        eClient = new EmailClient(port, id);
        DelayedMessageExecutor sender = new DelayedMessageExecutor(eClient, responseTime);
        failure = new AtomicBoolean(false);
        shutdown = new AtomicBoolean(false);
        this.fullShutdown = fullShutdown;
        proposerMessages = new LinkedBlockingQueue<String>();
        acceptorMessages = new LinkedBlockingQueue<String>();

        proposer = new Thread(new ProposerRunnable(N, id, timeToPropose, proposerMessages, sender, failure, ambition, shutdown));
        acceptor = new Thread(new AcceptorRunnable(acceptorMessages, sender, failure, id));

        // If ResponseTime is Never, then it's as if we fail all the time
        if (responseTime == ResponseTime.NEVER)
            timeToFail = 0;
    }

    public void run() {
        // If timeToFail is == to zero then don't bother running
        if (timeToFail == 0)
            return;

        //Start both threads
        proposer.start();
        acceptor.start();

        // Timer objects for timeouts
        Timer timer = new Timer();
        timer.setTimeout(Thread.currentThread(), timeToFail);
        while (!fullShutdown.get() && !shutdown.get()) {
            // Calculate failure
            // If current thread has been interrupted. 
            // Then our timer has gone off
            // Toggle failure
            if (Thread.interrupted()) {
                synchronized (failure) {
                    failure.set(!failure.get());
                    // If we're in state of failure, set a timer for restart
                    if (failure.get() ==  true) {
                        timer.setTimeout(Thread.currentThread(), timeToRestart);
                        System.out.println(String.format("Member %d is now unavailable", id));

                    // If we're in state of liveness, set a timer for failure
                    } else {
                        timer.setTimeout(Thread.currentThread(), timeToFail);
                        System.out.println(String.format("Member %d is now back", id));
                    }
                }
            }


            try {
                multiplexMessages();
            } catch (InterruptedException e) {
                // The Interrupted flag is cleared here so we reset it
                Thread.currentThread().interrupt();
            }
        }
    }

    public void multiplexMessages() throws InterruptedException {
        String message = eClient.receive();      
        char type = message.charAt(0);

        switch (type) {
            case PREPARE:
            case PROPOSAL:
                acceptorMessages.put(message);
                break;

            case PROMISE:
            case ACCEPT:
            case PREPARENACK:
            case PROPOSALNACK:
                proposerMessages.put(message);
                break;

            default:
                throw new RuntimeException("Unknown Type at MemberRunnable: " + type);
        }

    }
    
}
