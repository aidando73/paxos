package main.paxos;

import java.util.concurrent.*;

/**
 * Timer
 * Interrupts a thread after a certain duration
 */
public class Timer {

     ExecutorService executor = Executors.newSingleThreadExecutor();  
     
     //Interrupts a thread after timeout msec
     public void setTimeout(Thread interruptee, int timeout) {
        executor.execute(new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeout);
                } catch (Exception e) {
                    System.err.println("In timer thread: " + e.getMessage());
                    e.printStackTrace();
                }

                interruptee.interrupt();
            }
        });
    }
}
