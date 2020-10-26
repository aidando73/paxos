package main.driver;

import main.paxos.*;
import main.eserver.*;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.net.*;

import org.json.*;

/**
 * PaxosDriver
 * Simulates voting scenarios from config.json
 * Look at README.md for the config.json specification
 */
public class PaxosDriver {

    public static void main(String[] args) throws IOException {
        JSONArray arr = new JSONArray(new String(Files.readAllBytes(Paths.get(args[0]))));

        for (Object obj : arr) {
            runScenario((JSONObject)obj);
        }
    }

    private static void runScenario(JSONObject scenario) {
        System.out.println("NEW SCENARIO: " + scenario.getString("name"));
        System.out.println("\t" + scenario.getString("description"));

        simulateVoting((JSONArray)scenario.getJSONArray("members"));
    }

    private static void simulateVoting(JSONArray members) {
        // Start email server
        int port = getAvailablePort();
        EmailServer eserver = new EmailServer(port);

        //Create members
        List<Thread> memberThreads = new ArrayList<Thread>();
        int id = 0;
        int N = members.length();
        for (Object obj : members) {
            Thread member = createMember((JSONObject)obj, id++, N);
            memberThreads.add(member);
        }
    }

    // Create a member thread according to the config.json spec
    private static Thread createMember(JSONObject member, int id, int N) {
        // Get proposal interval
        int timeToPropose = member.isNull("timeToPropose") ?
            -1 : member.getInt("timeToPropose");

        // Get restart interval
        int timeToRestart = member.isNull("timeToRestart") ?
            -1 : member.getInt("timeToRestart");

        // Get fail interval
        int timeToFail = member.isNull("timeToFail") ? 
            -1 : member.getInt("timeToFail");

        // Get ambition
        boolean ambition = member.isNull("ambition") ?
            false : member.getBoolean("ambition");

        // Get response time
        ResponseTime responseTime = member.isNull("responseTime") ? 
            ResponseTime.IMMEDIATE : ResponseTime.valueOf(member.getString("responseTime"));
        
        System.out.println(String.format("Member %d:", id));
        System.out.println("\ttimeToPropose: " + Integer.toString(timeToPropose));
        System.out.println("\ttimeToFail: " + Integer.toString(timeToFail));
        System.out.println("\ttimeToRestart: " + Integer.toString(timeToRestart));
        System.out.println("\tambition: " + Boolean.toString(ambition));
        System.out.println("\tresponseTime: " + ResponseTime.IMMEDIATE.toString());

        return new Thread();
    }

    // Get an available port
    private static int getAvailablePort() {
        int res = -1;
        try {
            ServerSocket s = new ServerSocket(0); 
            res = s.getLocalPort();
            s.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        return res;
    }
}
