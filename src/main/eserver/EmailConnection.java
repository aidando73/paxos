package main.eserver;

import java.net.Socket;
import java.util.concurrent.ConcurrentMap;
import java.io.*;

/**
 * EmailConnection
 * Listens to single email connection
 * receives then sends emails off to recipients
 * according to an id
 */
public class EmailConnection {
    private Socket connection;
    PrintWriter writer;

    private Thread listener;

    //Initializes fields
    public EmailConnection(Socket connection, ConcurrentMap<Integer, EmailConnection> emailRegistry) throws IOException {
        this.connection = connection;
        respond(emailRegistry);

        writer = new PrintWriter(connection.getOutputStream(), true);
    }     

    // Sends a message to client
    public synchronized void send(String message) {
        writer.println(message);
        writer.flush();
    }

    // Halts responder thread gracefully
    public synchronized void join() {
        try {
            connection.close();
            listener.join();
        } catch (Exception io) {
            System.err.println(io.getMessage());
            io.printStackTrace();
        }         
    }

    //Initialize responder thread
    private void respond(ConcurrentMap<Integer, EmailConnection> emailRegistry) {
        EmailConnection emailConnectionRef = this;
        listener = new Thread(new ResponderRunnable(emailConnectionRef, connection, emailRegistry));
        listener.start();
    }
}

// Listens to single connection with Email Client
// Sends of messages to different recipients using emailRegistry
class ResponderRunnable implements Runnable {
    EmailConnection emailConnectionRef;
    Socket connection;
    ConcurrentMap<Integer, EmailConnection> emailRegistry;

    public ResponderRunnable(EmailConnection emailConnectionRef, Socket connection, ConcurrentMap<Integer, EmailConnection> emailRegistry) {
         this.emailConnectionRef = emailConnectionRef;
         this.connection = connection;
         this.emailRegistry = emailRegistry;
    }

    public void run() {
        int connectionId = -1;
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    connection.getInputStream()
                    )
                )
            ) {
            //First line should always be client id
            String line = reader.readLine();
            if (line != null) 
                connectionId = Integer.parseInt(line);
                emailRegistry.put(connectionId, emailConnectionRef);

            // Listen to email client and send to recipients
            respond(reader);

        } catch (IOException e) {
            // System.err.println(e.getMessage());
            // e.printStackTrace();
            System.out.println("Email connection " + Integer.toString(connectionId) + " closing");
        } catch (NumberFormatException ne) {
            System.err.println(ne.getMessage());
            ne.printStackTrace();
        }     
    }

    //Listen to email client and redirect email to recepieint
    void respond(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            try {
                int toId = Integer.parseInt(line.substring(0, line.indexOf(':')));
                if (emailRegistry.containsKey(toId)) {
                    emailRegistry.get(toId).send(line);
                } else {
                    System.out.println("recipient " + Integer.toString(toId) + " doesn't exist yet. Cannot send: " + line);
                }
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Invalid format for email. Must be: <recipient_id>:<message>");
            }
        }
    }
}

