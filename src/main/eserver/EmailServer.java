package main.eserver;

import java.util.concurrent.*;
import java.net.*;
import java.io.*;

/**
 * EmailServer
 */
public class EmailServer {

    //Starts the EmailServer
    public EmailServer(int port) {
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
