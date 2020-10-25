package test.eclient;

import org.junit.*;
import static org.junit.Assert.*;

import main.eclient.*;
import main.eserver.*;

import java.net.*;
import java.util.concurrent.*;
import java.io.*;

/**
 * EmailIntegrationTest
 * Integration tests for entire email system
 */
public class EmailIntegrationTest {

    @Test
    public void emailClientCanEcho() throws IOException, InterruptedException {
        int port = getAvailablePort();
        new Thread(new EmailServer(port)).start();
        EmailClient client = new EmailClient(port, 5);
        client.send("echo message", 5);

        assertEquals(receiveMessage(client), "echo message");
    }

    @Test
    public void emailClientsCanTalk() throws IOException, InterruptedException {
        int port = getAvailablePort();
        new Thread(new EmailServer(port)).start();

        EmailClient c1 = new EmailClient(port, 100);
        EmailClient c2 = new EmailClient(port, 200);
        EmailClient c3 = new EmailClient(port, 300);

        Thread.sleep(1000);

        c1.send("to c2", 200);
        c2.send("to c3", 300);
        c3.send("to c1", 100);

        assertEquals(receiveMessage(c1), "to c1");
        assertEquals(receiveMessage(c2), "to c2");
        assertEquals(receiveMessage(c3), "to c3");
    }

    // Polls for a response from inbox
    private String receiveMessage(EmailClient client) throws InterruptedException {
        String response;
        while ((response = client.receive()) == null) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
        return response;
    }

    private int getAvailablePort() throws IOException {
        int res = -1;
        ServerSocket s = new ServerSocket(0); 
        res = s.getLocalPort();
        s.close();
        return res;
    }
}
