package main.eclient;

import java.net.Socket;
import java.util.concurrent.*;
import java.io.*;

/**
 * EmailClient
 * Maintains 1:1 connection with server
 * Provides sending and recieiving message service
 */
public class EmailClient {
    Future<Socket> socketConnector;
    Socket connection = null;
    PrintWriter writer = null;
    BufferedReader reader = null;
    int emailId;
    BlockingQueue<String> inbox;

    // Connects to server
    // Creates asynchronous thread to get connection
    // Delays until must send/receive request
    public EmailClient(int port, int emailId) {
        socketConnector = new SocketConnecter("localhost", port, emailId)
                .getConnection();
        this.emailId = emailId;

        inbox = new LinkedBlockingQueue<String>();
        initializeInbox();
    }

    // Sends a message to server with a recipient
    public void send(String message, int recipient) {
        synchronized (writer) {
            writer.println(Integer.toString(recipient) + ":" + message);
        }
    }


    // Receives a single message
    // Can be interrupted
    public String receive() throws InterruptedException {
        return stripMessage(inbox.take());
    }

    //Strips message of recipient id
    //Terminates program if incorrect recipient.
    //Means there's something wrong with server
    private String stripMessage(String message) {
        int recipient = Integer.parseInt(
            message.substring(0, message.indexOf(':'))
        );

        if (recipient != emailId) 
            throw new RuntimeException("Email recipient " + Integer.toString(emailId) + " got wrong email: " + message);
        
        return message.substring(message.indexOf(':') + 1);
    }

    private void ensureConnection() {
        if (connection == null) {
            try {
                connection = socketConnector.get();
                reader = new BufferedReader(
                    new InputStreamReader(
                        connection.getInputStream()
                    )
                );
                writer = new PrintWriter(connection.getOutputStream(), true);
            } catch (Exception e) {
                System.err.println("SocketConnecter Error while executing" + e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    // Initializes thread that buffers communication
    // Into inbox
    private void initializeInbox() {
        ensureConnection();
        new Thread() {
             @Override
             public void run() {
                 try {
                     while (true)
                         inbox.put(reader.readLine());
                 } catch (Exception e) {
                     // Shutdown when socket closes
                 }
             }
        }.start(); 
    }
}

/**
 * SocketConnnection
 * Callable that polls for a server connection
 * Useful for connecting to server while allowing
 * user to continue main thread
 */
class SocketConnecter {
    int port;
    String url;
    int emailId;

    private ExecutorService executor;

    public SocketConnecter(String url, int port, int emailId) {
        this.url = url;
        this.port = port;
        this.emailId = emailId;
        executor = Executors.newSingleThreadExecutor();
    }

    // API to retrieve a Future that returns a socket
    public Future<Socket> getConnection() {
        return executor.submit(new SocketConnecterCallable());
    }

    // The callable to be submitted as a Future
    private class SocketConnecterCallable implements Callable<Socket> {
        public Socket call() {
            while (true) {
                try {
                    Socket conn = new Socket(url, port);
                    new PrintWriter(conn.getOutputStream(), true)
                        .println(Integer.toString(emailId));
                    return conn;
                } catch (Exception e) {
                    System.out.println("Connecting to server @" + url + ":" +Integer.toString(port));
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ge) {
                    }
                }
            }
        }
    }

}
