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
    private ProposerRunnable proposer;
    private ExecutorService executor;

    @Before
    public void setFields() {
        messages = new LinkedBlockingQueue<String>();
        sender = mock(DelayedMessageExecutor.class);
        failure = new AtomicBoolean();
        executor = Executors.newFixedThreadPool(1000);
    }

    private void initalizeProposerRunnable(int N, int timeToPropose) {
        proposer = new ProposerRunnable(N, id, timeToPropose, messages, sender, failure);
    }

    @Test
    public void broadCastPrepareRequestAfterDelay() {
        
    }
}
