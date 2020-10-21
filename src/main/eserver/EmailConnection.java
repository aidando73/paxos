package main.eserver;

import java.net.Socket;
import java.util.concurrent.ConcurrentMap;
import java.io.*;

/**
 * EmailConnection
 */
public class EmailConnection {
    private Socket connection;
    private ConcurrentMap<Integer, EmailConnection> emailRegistry;
    private int connectionId;
    PrintWriter writer;

    private Thread listener;

    public EmailConnection(Socket connection, ConcurrentMap<Integer, EmailConnection> emailRegistry) throws IOException {
        this.connection = connection;
        this.emailRegistry = emailRegistry;
        respond();

        writer = new PrintWriter(connection.getOutputStream(), true);
    }     

    
    public synchronized void send(String message) {
        writer.println(message);
        writer.flush();
        System.out.println("sent " + message);
    }


    public synchronized void join() {
        try {
            connection.close();
            listener.join();
        } catch (Exception io) {
            System.err.println(io.getMessage());
            io.printStackTrace();
        }         
    }

    private void respond() {
        EmailConnection emailConnectionRef = this;
        listener = new Thread() {
            @Override
            public void run() {
                try (
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                            connection.getInputStream()
                        )
                    )
                ) {
                    //First line should always be client id
                    String line = reader.readLine();
                    if (line != null) {
                        connectionId = Integer.parseInt(line);
                        emailRegistry.put(connectionId, emailConnectionRef);
                    }

                    while ((line = reader.readLine()) != null) {
                        int toId = Character.getNumericValue(line.charAt(0));
                        if (emailRegistry.containsKey(toId)) {
                            emailRegistry.get(toId).send(line);
                        }
                    }

                } catch (IOException e) {
                    // System.err.println(e.getMessage());
                    // e.printStackTrace();
                    System.out.println("Connection " + Integer.toString(connectionId) + " closing");
                } catch (NumberFormatException ne) {
                    System.err.println(ne.getMessage());
                    ne.printStackTrace();
                }
            }
        };
        listener.start();
    }
}
