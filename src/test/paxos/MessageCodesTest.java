package test.paxos;

import main.paxos.MessageCodes;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * MessageCodesTest
 * Basic Test to ensure all Message codes are unique
 */
public class MessageCodesTest {

     @Test  
     public void uniqueMessageCodes() {
         assertEquals(MessageCodes.PREPARE == MessageCodes.PROMISE, false);
         assertEquals(MessageCodes.PREPARE == MessageCodes.PROPOSAL, false);
         assertEquals(MessageCodes.PREPARE == MessageCodes.ACCEPT, false);
         assertEquals(MessageCodes.PREPARE == MessageCodes.PREPARENACK, false);
         assertEquals(MessageCodes.PREPARE == MessageCodes.PROPOSALNACK, false);

         assertEquals(MessageCodes.PROMISE == MessageCodes.PROPOSAL, false);
         assertEquals(MessageCodes.PROMISE == MessageCodes.ACCEPT, false);
         assertEquals(MessageCodes.PROMISE == MessageCodes.PREPARENACK, false);
         assertEquals(MessageCodes.PROMISE == MessageCodes.PROPOSALNACK, false);

         assertEquals(MessageCodes.PROPOSAL == MessageCodes.ACCEPT, false);
         assertEquals(MessageCodes.PROPOSAL == MessageCodes.PREPARENACK, false);
         assertEquals(MessageCodes.PROPOSAL == MessageCodes.PROPOSALNACK, false);

         assertEquals(MessageCodes.ACCEPT == MessageCodes.PREPARENACK, false);
         assertEquals(MessageCodes.ACCEPT == MessageCodes.PROPOSALNACK, false);

         assertEquals(MessageCodes.PREPARENACK == MessageCodes.PROPOSALNACK, false);
     }
}
