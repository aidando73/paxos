package test.eserver;

import main.eserver.EmailServer;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.net.*;
import java.io.*;

/**
 * EmailServerTest
 * Tests for email server
 */
public class EmailServerTest {

    @Test
    public void canConnectOneClient() throws IOException, InterruptedException {
        int port = getAvailablePort();
        new Thread(new EmailServer(port)).start();

        Socket connection = getConnection(port);
        BufferedReader reader = getReader(connection);
        PrintWriter writer = getWriter(connection);

        writer.println("1");
        writer.println("1:echo");
        assertEquals("1:echo", reader.readLine());
    }

    @Test
    public void canConnectThreeClients() throws IOException, InterruptedException {
        int port = getAvailablePort();
        new Thread(new EmailServer(port)).start();

        Socket c1 = getConnection(port);
        BufferedReader r1 = getReader(c1);
        PrintWriter w1 = getWriter(c1);

        w1.println("1");

        Socket c2 = getConnection(port);
        BufferedReader r2 = getReader(c2);
        PrintWriter w2 = getWriter(c2);

        w2.println("2");

        Socket c3 = getConnection(port);
        BufferedReader r3 = getReader(c3);
        PrintWriter w3 = getWriter(c3);

        w3.println("3789");

        while (!r1.ready()) {
            w2.println("1:new-recipient");
            Thread.sleep(250);
        }

        while (!r2.ready()) {
            w1.println("2:reply-back");
            Thread.sleep(250);
        }

        while (!r3.ready()) {
            w1.println("3789:from1,./l'l'[]=-)()(*&(*^%^!@%$#^!%@$';");
            Thread.sleep(250);
        }

        assertEquals(r1.readLine(), "1:new-recipient");
        assertEquals(r2.readLine(), "2:reply-back");
        assertEquals(r3.readLine(), "3789:from1,./l'l'[]=-)()(*&(*^%^!@%$#^!%@$';");
    }

    private int getAvailablePort() throws IOException {
        int res = -1;
        ServerSocket s = new ServerSocket(0); 
        res = s.getLocalPort();
        s.close();
        return res;
    }

    private Socket getConnection(int port) throws IOException, InterruptedException {
        while (true) {
            try {
                return new Socket("localhost", port);
            } catch (ConnectException e) {
                System.out.println("connecting to port " + Integer.toString(port));
                Thread.sleep(1000);
            }
        }
    }

    private BufferedReader getReader(Socket connection) throws IOException {
        return new BufferedReader(
            new InputStreamReader(
                connection.getInputStream()
            )
        );
    }

    private PrintWriter getWriter(Socket connection) throws IOException {
        return new PrintWriter(
            connection.getOutputStream(),
            true
        );
    }
}
