package test.eserver;

import main.eserver.EmailConnection;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.net.*;
import java.io.*;

public class EmailConnectionTest {
    @Test
    public void canShutDown() throws IOException, InterruptedException {
        ServerSocket server = new ServerSocket(0);

        sendData("localhost", server.getLocalPort(), "1\n");

        ConcurrentMap<Integer, EmailConnection> emailRegistry = new ConcurrentHashMap<Integer, EmailConnection>();
        
        EmailConnection connection = new EmailConnection(server.accept(), emailRegistry);

        connection.join();
    }

    @Test
    public void assignsItselfToEmailRegistry() throws IOException, InterruptedException {
        ServerSocket server = new ServerSocket(0);

        sendData("localhost", server.getLocalPort(), "1\ntewstinlskjg\ntesting\nnewline\n");

        ConcurrentMap<Integer, EmailConnection> emailRegistry = new ConcurrentHashMap<Integer, EmailConnection>();
        
        EmailConnection connection = new EmailConnection(server.accept(), emailRegistry);

        Thread.sleep(1000);

        assertEquals(connection, emailRegistry.get(1));
    }

    @Test
    public void canSendData() throws IOException, InterruptedException, InterruptedException {
        ServerSocket server = new ServerSocket(0);

        Socket socket = sendData("localhost", server.getLocalPort(), "");

        ConcurrentMap<Integer, EmailConnection> emailRegistry = new ConcurrentHashMap<Integer, EmailConnection>();
        
        EmailConnection connection = new EmailConnection(server.accept(), emailRegistry);
        
        connection.send("message sent");

        assertEquals(getLine(socket), "message sent");
    }

    @Test
    public void sendsEmailToAnotherConnection() throws IOException, InterruptedException {
        ServerSocket server = new ServerSocket(0);

        Socket socket1 = sendData("localhost", server.getLocalPort(), "1\n2:PassedOnMessage\n");
        Socket socket2 = sendData("localhost", server.getLocalPort(), "2\n1:PassedOnMessage\n");

        ConcurrentMap<Integer, EmailConnection> emailRegistry = new ConcurrentHashMap<Integer, EmailConnection>();
        
        EmailConnection connection1 = new EmailConnection(server.accept(), emailRegistry);
        EmailConnection connection2 = new EmailConnection(server.accept(), emailRegistry);

        //Wait for an update to email registries
        while (emailRegistry.size() < 2) {
            Thread.sleep(1000);
        }

        assertEquals(getLine(socket1), "1:PassedOnMessage");
        assertEquals(getLine(socket2), "2:PassedOnMessage");
    }

    //Creates a Socket that sends data to a specific url
    //On a new thread
    //Returns the socket
    public Socket sendData(String host, int port, String data) throws InterruptedException {
        SocketThread thread = new SocketThread(host, port, data);

        thread.start();
        
        return thread.getSocket();
    }

    class SocketThread extends Thread {
        Socket socket;
        Object lock = new Object();

        String host;
        int port;
        String data;

        SocketThread(String host, int port, String data) {
            this.host = host;
            this.port = port;
            this.data = data;
        }

        @Override
        public void run() {
            try {
                synchronized(lock) {
                    socket = new Socket(host, port);
                    PrintWriter writer = new PrintWriter(socket.getOutputStream()); 
                    writer.print(data);
                    writer.flush();

                    lock.notify();
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
        public Socket getSocket() throws InterruptedException {
            synchronized(lock) {
                while (socket == null) {
                    lock.wait();
                }
            }

            return socket;
        }
    };

    private String getLine(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                    socket.getInputStream()
                )
            );
        
        return reader.readLine();
    }
}
