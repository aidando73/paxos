package main.driver;

import java.nio.file.*;
import java.io.IOException;

/**
 * PaxosDriver
 */
public class PaxosDriver {

    public static void main(String[] args) throws IOException {
        System.out.println(new String(Files.readAllBytes(Paths.get(args[0]))));
    }
}
