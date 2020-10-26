package test.eclient;

import main.eclient.EmailClient;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.net.*;
import java.io.*;

/**
 * EmailClientTest
 * Unit tests for email client
 */
public class EmailClientTest {


    @Test
    public void canHandshake() throws IOException {
        ServerSocket s = new ServerSocket(0);

        EmailClient client = new EmailClient(s.getLocalPort(), 1);

        Socket conn = s.accept();
        BufferedReader r = getReader(conn);
        
        assertEquals(r.readLine(), "1");
    }

    @Test
    public void canSendToServer() throws IOException {
        ServerSocket s = new ServerSocket(0);

        EmailClient client = new EmailClient(s.getLocalPort(), 1);

        Socket conn = s.accept();
        BufferedReader r = getReader(conn);

        client.send(",./;'l'l[][]+_)(*(*^&^%$#$%!@87687613';sdfkcx.;pdfa']|\\",298);
        
        assertEquals(r.readLine(), "1");
        assertEquals(r.readLine(), "298:,./;'l'l[][]+_)(*(*^&^%$#$%!@87687613';sdfkcx.;pdfa']|\\");
    }

    @Test
    public void canReceiveFromServer() throws IOException, InterruptedException {
        ServerSocket s = new ServerSocket(0);

        EmailClient client = new EmailClient(s.getLocalPort(), 4800);

        Socket conn = s.accept();
        BufferedReader r = getReader(conn);
        PrintWriter w = getWriter(conn);

        w.println("4800:test-message");

        //Poll for response
        String response;
        while ( (response  = client.receive()) == null) {
            Thread.sleep(500);
            System.out.println("Polling for server response");
        }
        
        assertEquals(response, "test-message");
    }

    //Basic test to ensure the send method is thread safe
    //Creates a thousand threads to use send concurrently
    @Test
    public void sendIsThreadSafe() throws IOException, InterruptedException {
        ServerSocket s = new ServerSocket(0);

        EmailClient client = new EmailClient(s.getLocalPort(), 4800);

        Socket conn = s.accept();
        BufferedReader r = getReader(conn);

        ExecutorService executor = Executors.newFixedThreadPool(1000);
        for (int i = 0; i < 1000; i++) {
            executor.execute(new Thread() {
                @Override
                public void run() {
                    client.send("test-message", 5);
                }
            });
        }

        //Wait until all tasks completed
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        System.out.println("all tasks completed");

        assertEquals(r.readLine(), "4800");
        for (int i = 0; i < 1000; i++) {
            assertEquals(r.readLine(), "5:test-message");
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

