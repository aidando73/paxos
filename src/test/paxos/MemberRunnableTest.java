package test.paxos;

import main.eserver.*;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import main.paxos.*;
import static main.paxos.MessageCodes.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * MemberRunnableTest
 * Unit tests for MemberRunnable
 * Due to lack of latest Junit library, I'm unable
 * to scan system.out for logs, as such logs will be examined manually
 */
public class MemberRunnableTest {

    ExecutorService executor = Executors.newFixedThreadPool(1000);
    MemberRunnable member;
    AtomicBoolean fullShutdown;
    private void initializeMember(int timeToFail, int timeToRestart, int timeToPropose) {
        fullShutdown = new AtomicBoolean();
        executor.execute(new EmailServer(3000));
        member = new MemberRunnable(3000, ResponseTime.IMMEDIATE, timeToFail, timeToRestart, timeToPropose, 5, 20, fullShutdown, true);
        executor.execute(member);
    }

    @Test 
    public void restartsAndFails() throws InterruptedException {
        // 0.5 seconds to fail, 1 seconds to restart
        initializeMember(500, 1000, 80000);
        System.out.println("0.5 seconds to fail, 1 second to restart: ");

        Thread.sleep(4000);
    }
}
