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
    private int timeToPropose;
    //Time for member to restart after crash
    private int timeToRestart;
    private int id;
    private int N;

    private EmailClient eClient;
    private DelayedMessageExecutor sender;

    // Acceptor and Proposer management variables
    private Thread proposer;
    private Thread acceptor;
    private BlockingQueue<String> proposerMessages;
    private BlockingQueue<String> acceptorMessages;
    private AtomicBoolean failure;
    private AtomicBoolean fullShutdown;
    private boolean ambition;

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
        this.N = N;

        // Create proposer and acceptor threads
        eClient = new EmailClient(port, id);
        sender = new DelayedMessageExecutor(eClient, responseTime);
        failure = new AtomicBoolean(false);
        this.fullShutdown = fullShutdown;
        proposerMessages = new LinkedBlockingQueue<String>();
        acceptorMessages = new LinkedBlockingQueue<String>();
        acceptor = new Thread(new AcceptorRunnable(acceptorMessages, sender, failure, id));
        this.ambition = ambition;
        this.timeToPropose = timeToPropose;


        // If ResponseTime is Never, then it's as if we fail all the time
        if (responseTime == ResponseTime.NEVER)
            timeToFail = 0;

    }

    public void run() {
        // If timeToFail is == to zero then don't bother running
        if (timeToFail == 0)
            return;

        //Start both threads
        proposer = new Thread(new ProposerRunnable(N, id, timeToPropose, proposerMessages, sender, failure, ambition, fullShutdown, Thread.currentThread()));
        proposer.start();
        acceptor.start();

        // Timer objects for timeouts
        Timer timer = new Timer();
        // Only set a timer if timeToFail is positive
        if (timeToFail > 0)
            timer.setTimeout(Thread.currentThread(), timeToFail);

        while (!fullShutdown.get()) {
            handleFailure(timer);

            try {
                multiplexMessages();
            } catch (InterruptedException e) {
                // The Interrupted flag is cleared here so we reset it
                Thread.currentThread().interrupt();
            }
        }

        gracefulShutdown();
    }

    // Calculates whether we're in a state of failure or not
    private void handleFailure(Timer timer) {
        // Calculate failure
        // If current thread has been interrupted. 
        // Then our timer has gone off
        // Toggle failure
        if (Thread.interrupted()) {
            synchronized (failure) {
                failure.set(!failure.get());
                // If we're in state of failure, set a timer for restart
                // Only if timeToRestart is non-negative
                if (failure.get() ==  true) {
                    if (timeToRestart > 0) {
                        timer.setTimeout(Thread.currentThread(), timeToRestart);
                        System.out.println(String.format("Member %d is now unavailable", id));
                    }
                // If we're in state of liveness, set a timer for failure
                } else {
                    // Other threads have been waiting so we need to notify
                    failure.notifyAll();
                    timer.setTimeout(Thread.currentThread(), timeToFail);
                    System.out.println(String.format("Member %d is now back", id));
                }
            }
        }
        
    }

    private void multiplexMessages() throws InterruptedException {
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

    // Handles graceful shutdown of threads
    private void gracefulShutdown() {
        acceptor.interrupt();     
        proposer.interrupt();     
        while (acceptor.getState() != Thread.State.TERMINATED && proposer.getState() != Thread.State.TERMINATED) {
            try {
                acceptor.join();
                proposer.join();
            } catch (Exception e) {
            }
        }
        System.out.println(String.format("Member %d Shutting down", id));
    }
}
