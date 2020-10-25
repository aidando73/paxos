package main.paxos;

import main.eclient.EmailClient;
import java.util.concurrent.*;

/**
 * DelayedMessageExecutor
 * Sends message after a pre-defined delay
 * asynchronously with a fixed thread pool
 *
 * Note: Delay is guaranteed MINIMUM time to send
 * not exact time: In reality it could be longer
 *
 * Delayed values are as follows
 * IMMEDIATE: 0s
 * MEDIUM: 1s
 * LATE: 4s
 * NEVER: never
 */
public class DelayedMessageExecutor {

    EmailClient client;
    ResponseTime delay;

    ExecutorService executor = Executors.newFixedThreadPool(5);

    public DelayedMessageExecutor(EmailClient client, ResponseTime delay) {
        this.client = client;
        this.delay = delay;
    }

    // Submits task to threadpool.
    // Sends after some delay.
    public void send(String message, int recipient) {
        executor.execute(new Thread() {
            @Override
            public void run() {
                //Wait depending on delay value
                try {
                    switch (delay) {
                        case IMMEDIATE:
                            //Wait no time
                            break;

                        case MEDIUM:
                            Thread.sleep(1000);
                            break;

                        case LATE:
                            Thread.sleep(4000);
                            break;

                        case NEVER:
                            return;
                        
                        default:
                            throw new RuntimeException("Unknown response time: " + delay.toString());
                    }
                    
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }

                client.send(message, recipient);
            }
        });
    }
}

