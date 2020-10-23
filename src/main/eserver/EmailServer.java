package main.eserver;

import java.util.concurrent.*;
import java.net.*;
import java.io.*;

/**
 * EmailServer
 * Listens and accepts email connections
 */
public class EmailServer implements Runnable {
    int port;
    public EmailServer(int port) {
        this.port = port;
    }
    
    public void run() {
        ConcurrentMap<Integer, EmailConnection> emailRegistry = new ConcurrentHashMap<Integer, EmailConnection>();
        
        //Starts a new thread per connection
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                new EmailConnection(server.accept(), emailRegistry);
            }
        } catch (IOException e) {
            System.err.println("Email Server: " + e.getMessage());
            e.printStackTrace();
        }
    }  
}
