package test.paxos;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import main.paxos.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * AcceptorRunnableTest
 * Unit tests for the Acceptor thread
 */
public class AcceptorRunnableTest {
    AtomicBoolean failure;
    BlockingQueue<String> messages;
    DelayedMessageExecutor sender;
    ExecutorService executor;
    AcceptorRunnable acceptor;
    int id = 99;

    @Before
    public void setFields() {
        failure = new AtomicBoolean();
        messages = new LinkedBlockingQueue<String>();
        sender = mock(DelayedMessageExecutor.class);
        executor = Executors.newFixedThreadPool(1000);
        acceptor = new AcceptorRunnable(messages, sender, failure, id);
    }

    @Test
    public void sendPromiseForAPrepare() throws InterruptedException {
        messages.put(MessageCodes.PREPARE + "56 10");

        executor.execute(acceptor);

        Thread.sleep(250);

        verify(sender).send(MessageCodes.PROMISE + "99 10", 56);
    }

    @Test
    public void test3() {
    }
}
