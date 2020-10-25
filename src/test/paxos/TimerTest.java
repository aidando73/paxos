package test.paxos;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import main.paxos.*;
import static main.paxos.MessageCodes.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


/**
 * TimerTest
 */
public class TimerTest {

     Timer timer;  

     @Before
     public void initializeTimer() {
         timer = new Timer();
     }

     @Test(expected = InterruptedException.class)
     public void interruptsAfterTimer() throws InterruptedException{
         timer.setTimeout(Thread.currentThread(), 1000);

         Thread.sleep(4000);
     }
}
