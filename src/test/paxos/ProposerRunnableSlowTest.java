package test.paxos;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import main.paxos.*;
import static main.paxos.MessageCodes.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * ProposerRunnableSlowTest
 * Tests that test the timeout functionality of a proposer
 */
public class ProposerRunnableSlowTest {

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
    public void timeoutAfter15Seconds() throws InterruptedException {
        executor.execute(initalizeProposerRunnable(20, 0));

        Thread.sleep(250);

        for (int i = 19; i >= 0; i--) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 5),i);
        }

        // Wait 15 seconds with some buffer
        Thread.sleep(15250);

        for (int i = 19; i >= 0; i--) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 25),i);
        }
    }

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
    public void timeoutAfter15SecondsFromProposal() throws InterruptedException {
        setProposalState();

        Thread.sleep(16000);

        for (int i = 0; i < 20; i++) {
            if (i != 5)
                verify(sender).send(String.format("%c%d %d",PREPARE, 5, 25),i);
        }
    }
}
