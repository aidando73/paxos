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
    Thread memberThread;

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
        memberThread = new Thread(member);
        memberThread.start();
    }

    @After
    public void shutdown() throws InterruptedException {
        fullShutdown.set(true);
        memberThread.interrupt();
        memberThread.join();
    }

    @Test 
    public void restartsAndFails() throws InterruptedException {
        // 0.5 seconds to fail, 1 seconds to restart
        initializeMember(500, 1000, 80000);
        System.out.println("0.5 seconds to fail, 1 second to restart: ");

        Thread.sleep(4000);
        fullShutdown.set(true);
    }

    @Test 
    public void sendsBroadcastPrepareToEveryone() throws InterruptedException {
        initializeMember(-1, -1, 0);

        Thread.sleep(250);
        
        for (EmailClient ec : eClients) {
            assertEquals(ec.receive(),PREPARE + "5 5");
        }
    }
    
    // @Test
    // public void receivesPrepareRequest() throws InterruptedException {
    //     initializeMember(-1, -1, 8000);

    //     // First send a prepare for proposal 10
    //     eClients.get(0).send(PREPARE + "0 10",5);
    //     Thread.sleep(250);
    //     // Then send a prepare for proposal 0
    //     eClients.get(1).send(PREPARE + "1 0",5);
    //     Thread.sleep(250);
    //     // Also send a proposal for proposal 1
    //     eClients.get(2).send(PROPOSAL + "2 1 666", 5);

    //     // We should receive a preparenack and proposalnack respectively
    //     Thread.sleep(250);

    //     assertEquals(eClients.get(0).receive(), PROMISE + "5 10");
    //     assertEquals(eClients.get(1).receive(), PREPARENACK + "5 0");
    //     assertEquals(eClients.get(2).receive(), PROPOSALNACK + "5 1");
    // }

    // @Test
    // public void acceptsProposals() throws InterruptedException {
    //     initializeMember(-1, -1, 8000);
        
    //     // Send a proposal for proposalId 0
    //     eClients.get(0).send(PROPOSAL + "0 0 500", 5);
    //     Thread.sleep(250);
    //     // Send a prepare for a higher proposal
    //     eClients.get(1).send(PREPARE + "1 1 666", 5);

    //     Thread.sleep(250);

    //     // Should have received an accept request and an augmented promise
    //     assertEquals(eClients.get(0).receive(), ACCEPT + "5 0");
    //     assertEquals(eClients.get(1).receive(), PROMISE + "5 1 0 500");
    // }

    @Test
    public void gracefulShutdown() throws InterruptedException {
        initializeMember(-1, -1, 0);
        Thread.sleep(250);
        fullShutdown.set(true);
        memberThread.interrupt();
    }


    // @Test
    // public void sendProposalsIfPromisedAndDoneIfAccepted() throws InterruptedException {
    //     initializeMember(-1, -1, 0);

    //     // Send an promise for the prepare
    //     for (int i = 0; i < 19; i++) {
    //         // Get id of eclient
    //         int id = i;
    //         if (i >= 5)
    //             id++;

    //         eClients.get(i).send(
    //             String.format(
    //                 "%c%d %d %d %d",
    //                 PROMISE,
    //                 id,
    //                 5,
    //                 8,
    //                 666
    //             )
    //         , 5);
    //     }

    //     Thread.sleep(500);

    //     // We should receive a Proposal id = 5 and value 666
    //     for (int i = 0; i < 19; i++) {
    //         // Get id of eclient
    //         int id = i;
    //         if (i >= 5)
    //             id++;
    //         // Dump the prepare request
    //         eClients.get(i).receive();
    //         // Assert the proposal
    //         assertEquals(eClients.get(i).receive(), PROPOSAL + "5 5 666");

    //         // Send back an accept request
    //         eClients.get(i).send(
    //             String.format(
    //                 "%c%d %d",
    //                 ACCEPT,
    //                 id,
    //                 5
    //             )
    //         , 5);
    //     }
        
    //     memberThread.join();
    //     System.out.println(memberThread.getState().toString());
    //     assertEquals(fullShutdown.get(), true);
    // }
}
