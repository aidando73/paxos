package test.paxos;

import main.eserver.*;
import main.eclient.*;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import main.paxos.*;
import static main.paxos.MessageCodes.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;
import java.net.*;
import java.io.*;

/**
 * MemberRunnableTest
 * Unit tests for MemberRunnable
 * Due to lack of latest Junit library, I'm unable
 * to scan system.out for logs, as such logs will be examined manually
 *
 * Can be considered high level integration tests
 */
public class MemberRunnableTest {

    ExecutorService executor = Executors.newFixedThreadPool(1000);
    MemberRunnable member;
    AtomicBoolean fullShutdown;
    int port = -1;
    List<EmailClient> eClients;

    @Before
    public void setUpServerAndPort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        port = s.getLocalPort();
        s.close();
        // Initialize email server
        executor.execute(new EmailServer(port));
            
        // Initialize rest of email clients
        eClients = new ArrayList<EmailClient>();
        for (int i = 0; i < 20; i++) {
            if (i != 5)
                eClients.add(new EmailClient(port, i));
        }
    }

    private void initializeMember(int timeToFail, int timeToRestart, int timeToPropose) {
        fullShutdown = new AtomicBoolean();

        // Create member that:
        // has Id of 5
        // with 20 total members
        member = new MemberRunnable(port, ResponseTime.IMMEDIATE, timeToFail, timeToRestart, timeToPropose, 5, 20, fullShutdown, true);
        executor.execute(member);
    }

    @Test 
    public void restartsAndFails() throws InterruptedException {
        // 0.5 seconds to fail, 1 seconds to restart
        initializeMember(500, 1000, 80000);
        System.out.println("0.5 seconds to fail, 1 second to restart: ");

        Thread.sleep(4000);
    }

    @Test 
    public void sendsBroadcastPrepareToEveryone() throws InterruptedException {
        initializeMember(-1, -1, 0);

        Thread.sleep(250);
        
        for (EmailClient ec : eClients) {
            assertEquals(ec.receive(),PREPARE + "5 5");
        }
    }
    
    @Test
    public void receivesPrepareRequest() throws InterruptedException {
        initializeMember(-1, -1, 8000);

        Thread.sleep(250);

        // First send a prepare for proposal 10
        eClients.get(0).send(PREPARE + "0 10",5);
        // Then send a prepare for proposal 0
        eClients.get(1).send(PREPARE + "1 0",5);
        // Also send a proposal for proposal 1
        eClients.get(2).send(PROPOSAL + "2 1 666", 5);

        // We should receive a preparenack and proposalnack respectively
        Thread.sleep(250);

        assertEquals(eClients.get(1).receive(), PREPARENACK + "5 0");
        assertEquals(eClients.get(2).receive(), PROPOSALNACK + "5 1");
    }
}
