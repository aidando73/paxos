package test.paxos;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import main.paxos.*;
import static main.paxos.MessageCodes.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


/**
 * ProposerRunnabletest
 * Unit tests for proposer test
 */
public class ProposerRunnableTest {
    private BlockingQueue<String> messages;
    private DelayedMessageExecutor sender;
    private final int id = 5;
    private AtomicBoolean failure;
    private ExecutorService executor;
    private AtomicBoolean shutdown;

    @Before
    public void setFields() {
        messages = new LinkedBlockingQueue<String>();
        sender = mock(DelayedMessageExecutor.class);
        failure = new AtomicBoolean(false);
        executor = Executors.newFixedThreadPool(1000);
        shutdown = new AtomicBoolean(false);
    }

    private ProposerRunnable initalizeProposerRunnable(int N, int timeToPropose) {
        return new ProposerRunnable(N, id, timeToPropose, messages, sender, failure, true, shutdown, Thread.currentThread());
    }

    @Test
    public void broadCastPrepareRequest() throws InterruptedException {
        executor.execute(initalizeProposerRunnable(20, 0));

        Thread.sleep(250);

        for (int i = 19; i >= 0; i--) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 5),i);
        }
    }

    @Test
    public void broadCastPrepareRequestAfterDelay() throws InterruptedException {
        executor.execute(initalizeProposerRunnable(20, 500));

        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender, never()).send(String.format("%c%d %d",PREPARE, 5, 5),i);
        }

        Thread.sleep(550);


        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 5),i);
        }
    }

    @Test
    public void doNothingIfFailure() throws InterruptedException {
        failure.set(true);
        executor.execute(initalizeProposerRunnable(20, 0));

        Thread.sleep(250);

        messages.put(PREPARENACK + "2 5");

        synchronized(failure) {
            failure.set(false);
            failure.notifyAll();
        }

        Thread.sleep(250);

        // Prepare broadcast should only ever be called once since we failed
        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender, times(1)).send(String.format("%c%d %d",PREPARE, 5, 5),i);
        }
    }

    @Test
    public void ignoresProposalNackAndAcceptDuringPREPARE() throws InterruptedException {
        executor.execute(initalizeProposerRunnable(20, 0));

        Thread.sleep(250);

        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 5),i);
        }

        messages.put(ACCEPT + "0 0");
        messages.put(PROPOSALNACK + "0 0");

        Thread.sleep(250);

        verifyNoMoreInteractions(sender);
    }

    @Test
    public void ignoresOldPromises() throws InterruptedException {
        executor.execute(initalizeProposerRunnable(20, 0));

        Thread.sleep(250);

        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 5),i);
        }

        messages.put(PROMISE + "0 4");

        Thread.sleep(250);

        verifyNoMoreInteractions(sender);
    }

    @Test
    public void rebroadcastIfPrepareNack() throws InterruptedException {
        executor.execute(initalizeProposerRunnable(20, 0));

        Thread.sleep(250);

        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 5),i);
        }

        messages.put(PREPARENACK + "0 4");

        Thread.sleep(250);

        // Check if there was as rebroadcase with 1xN + id new proposal Id
        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 25),i);
        }
    }

    @Test
    public void sendProposalsIfEnoughPromises() throws InterruptedException {
        executor.execute(initalizeProposerRunnable(20, 0));

        Thread.sleep(250);

        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 5),i);
        }

        // Send exactly 10 proposals with random accepted values
        for (int i = 0; i < 11; i++) {
            if (i != 5)
                messages.put(String.format("%c%d %d %d %d", PROMISE, i, 5, i, 500));
        }

        Thread.sleep(250);
        // Check that no proposals have been sent yet
        verifyNoMoreInteractions(sender);

        // Now send the last vote
        // With an accepted value that should be adopted by propooser
        messages.put(String.format("%c%d %d %d %d", PROMISE, 11, 5, 200, 200));

        Thread.sleep(250);

        // Verify that a proposal was broadcast
        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d %d", PROPOSAL, 5, 5, 200),i);
        }
    }

    // Helper method to set proposer into the PROPOSAL stage for testing
    private void setProposalState() throws InterruptedException {
        executor.execute(initalizeProposerRunnable(20, 0));

        Thread.sleep(250);

        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 5),i);
        }

        // Send exactly a majority promises
        for (int i = 0; i < 14; i++) {
            if (i != 5)
                messages.put(String.format("%c%d %d %d %d", PROMISE, i, 5, i, 500));
        }

        Thread.sleep(250);

        // Verify that a proposal was broadcast
        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d %d", PROPOSAL, 5, 5, 500),i);
        }
    }

    @Test
    public void doNothingWhenPromiseOrPromiseNackOrWrongProposalId() throws InterruptedException {
        setProposalState();

        messages.put(String.format("%c%d %d", PROMISE, 4, 5));
        messages.put(String.format("%c%d %d", PREPARENACK, 4, 5));
        messages.put(String.format("%c%d %d", ACCEPT, 4, 3));

        Thread.sleep(250);
        verifyNoMoreInteractions(sender);
    }

    @Test
    public void resetToPrepareIfAcceptNack() throws InterruptedException {
        setProposalState();

        messages.put(String.format("%c%d %d", PROPOSALNACK, 4, 5));
        
        Thread.sleep(250);
        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 25),i);
        }
    }

    @Test(expected = InterruptedException.class)
    public void movesToStateDONEIfmajorityAccepts() throws InterruptedException {
        setProposalState();

        // Send exactly 10 accept messages
        for (int i = 0; i < 11; i++) {
            if (i != 5)
                messages.put(String.format("%c%d %d", ACCEPT, i, 5));
        }

        Thread.sleep(250);
        
        verifyNoMoreInteractions(sender);
        assertEquals(shutdown.get(), false);

        Thread.sleep(250);

        // Send the 11th message
        messages.put(String.format("%c%d %d", ACCEPT, 11, 5));

        // Shutdown executor
        executor.shutdown();

        executor.awaitTermination(1, TimeUnit.DAYS);

        Thread.sleep(250);
        assertEquals(shutdown.get(), true);
        assertEquals(executor.isTerminated(), true);
    }


    // @Test
    // public void gracefulShutdown() throws InterruptedException {
    //     Future thread = executor.submit(initalizeProposerRunnable(20, 0));

    //     Thread.sleep(250);
        
    //     thread.cancel(true);
    //     executor.shutdown();

    //     assertEquals(executor.isTerminated(), true); 
    // }

    // @Test()
    // public void gracefulShutdownOnFailure() throws InterruptedException {
    //     failure.set(true);
    //     Future thread = executor.submit(initalizeProposerRunnable(20, 0));

    //     Thread.sleep(250);
        
    //     thread.cancel(true);
    //     executor.shutdown();

    //     Thread.sleep(250);

    //     assertEquals(executor.isTerminated(), true); 
    // }

}
