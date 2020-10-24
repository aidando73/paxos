package test.paxos;

import main.eclient.EmailClient;
import main.paxos.*;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.*;

/**
 * DelayedMessageExecutorTest
 * Unit Tests for DelayedMessageExecutor
 * Utilizes Mockito to ensure EmailClient was called
 */
public class DelayedMessageExecutorTest {

    @Test
    public void canSendImmediateMessage() throws InterruptedException {
        EmailClient mockedClient = mock(EmailClient.class);
        DelayedMessageExecutor sender = new DelayedMessageExecutor(mockedClient, ResponseTime.IMMEDIATE);

        sender.send("test-message", 5);

        //Might take time for thread to get started
        Thread.sleep(250);

        verify(mockedClient).send("test-message", 5);
    }

    @Test
    public void canSendMediumMessage() throws InterruptedException {
        EmailClient mockedClient = mock(EmailClient.class);
        DelayedMessageExecutor sender = new DelayedMessageExecutor(mockedClient, ResponseTime.MEDIUM);

        sender.send("test-message", 5);

        // Email Client should not have been called here
        verify(mockedClient, never()).send("test-message", 5);

        // Wait 1 second with some buffer
        Thread.sleep(1250);

        verify(mockedClient).send("test-message", 5);
    }

    @Test
    public void canSendLateMessage() throws InterruptedException {
        EmailClient mockedClient = mock(EmailClient.class);
        DelayedMessageExecutor sender = new DelayedMessageExecutor(mockedClient, ResponseTime.LATE);

        sender.send("test-message", 5);

        // Email Client should not have been called here
        verify(mockedClient, never()).send("test-message", 5);

        Thread.sleep(2000);

        // Email Client should not have been called here
        verify(mockedClient, never()).send("test-message", 5);

        Thread.sleep(2250);

        verify(mockedClient).send("test-message", 5);
    }

    @Test
    public void canSendNeverMessage() throws InterruptedException {
        EmailClient mockedClient = mock(EmailClient.class);
        DelayedMessageExecutor sender = new DelayedMessageExecutor(mockedClient, ResponseTime.NEVER);

        sender.send("test-message", 5);

        // Email Client should not have been called here
        verify(mockedClient, never()).send("test-message", 5);
        
        Thread.sleep(1000);

        // Email Client should not have been called here
        verify(mockedClient, never()).send("test-message", 5);
    }

    // Basic test for internal Thread safety
    @Test
    public void canSendMultipleImmediates() throws InterruptedException {
        EmailClient mockedClient = mock(EmailClient.class);
        DelayedMessageExecutor sender = new DelayedMessageExecutor(mockedClient, ResponseTime.IMMEDIATE);

        ExecutorService executor = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 100; i++) {
            executor.execute(new Thread() {
                @Override
                public void run() {
                    sender.send("test-message", 5);
                }
            });
        }

        Thread.sleep(1000);

        verify(mockedClient, times(100)).send("test-message", 5);
    }
}
