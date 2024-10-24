package org.example;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GPSListener {

    private static final int PORT = 5000; // Port to listen on
    private static final int MAX_THREADS = 10; // Number of threads for handling clients

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS); // Thread pool for handling clients

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("GPS Listener started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming client connection
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Assign each client to a new thread
                executor.submit(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            System.err.println("Error in GPS Listener: " + e.getMessage());
        }
    }
}