package test.paxos;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import main.paxos.*;
import static main.paxos.MessageCodes.*;
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
        executor.execute(acceptor);

        messages.put(PREPARE + "56 10");

        Thread.sleep(250);

        verify(sender).send(MessageCodes.PROMISE + "99 10", 56);
    }

    @Test
    public void sendNackForAPrepare() throws InterruptedException {
        executor.execute(acceptor);

        //Send prepare that Acceptor should accept
        messages.put(PREPARE + "56 10");
        //Send prepare that acceptor should now reject
        messages.put(PREPARE + "47 5");

        Thread.sleep(250);

        verify(sender).send(MessageCodes.PROMISE + "99 10", 56);
        verify(sender).send(MessageCodes.PREPARENACK + "99 5", 47);
    }

    @Test
    public void sendAnAcceptIfNoPromise() throws InterruptedException {
        executor.execute(acceptor);

        messages.put(PROPOSAL + "56 10 9000");

        Thread.sleep(250);

        verify(sender).send(MessageCodes.ACCEPT + "99 10", 56);
    }

    @Test
    public void sendAProposalNackIfPromise() throws InterruptedException {
        executor.execute(acceptor);


        messages.put(PREPARE + "56 10 9000");
        messages.put(PROPOSAL + "24 5 2000");

        Thread.sleep(250);

        verify(sender).send(PROMISE + "99 10", 56);
        verify(sender).send(PROPOSALNACK + "99 5", 24);
    }

    @Test(expected = RuntimeException.class)
    public void throwsExceptionIfSentPromise() throws InterruptedException {
        messages.put(PROMISE + "10 10");
        acceptor.run();
    }

    @Test(expected = RuntimeException.class)
    public void throwsExceptionIfSentAccept() throws InterruptedException {
        messages.put(ACCEPT + "10 10");
        acceptor.run();
    }

    @Test
    public void failsIfFailureIsTrue() throws InterruptedException {
        failure.set(true);

        messages.put(PREPARE + "56 10 9000");

        executor.execute(acceptor);

        Thread.sleep(250);

        verify(sender, never()).send(PROMISE + "99 10", 56);

        synchronized(failure) {
            failure.set(false);
            failure.notifyAll();
        }

        Thread.sleep(250);
        messages.put(PREPARE + "56 10 9000");
        Thread.sleep(250);

        verify(sender).send(PROMISE + "99 10", 56);
    }

    @Test
    public void gracefulShutdown() throws InterruptedException {
        Future thread = executor.submit(acceptor);

        Thread.sleep(250);
        
        thread.cancel(true);
        executor.shutdown();

        Thread.sleep(250);

        assertEquals(executor.isTerminated(), true); 
    }
    @Test
    public void gracefulShutdownOnFailure() throws InterruptedException {
        failure.set(true);
        Future thread = executor.submit(acceptor);

        Thread.sleep(250);
        
        thread.cancel(true);
        executor.shutdown();

        Thread.sleep(250);

        assertEquals(executor.isTerminated(), true); 
   }
}
