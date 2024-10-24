package org.example;
import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Read IMEI from the client, only once after connection
            String imei = in.readLine();
            if (imei != null) {
                System.out.println("Received IMEI: " + imei);  // Process IMEI
            }

            // Instantiate Gson for JSON parsing
            Gson gson = new Gson();

            // Now handle the position data continuously
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // Parse the received JSON string into GPSPosition object
                GPSPosition position = gson.fromJson(inputLine, GPSPosition.class);

                // Print the GPS position object
                System.out.println("Received GPS Position: " + position);

                // Send an acknowledgment back to the client
                out.println("ACK: Position received");
                out.flush();  // Ensure the acknowledgment is sent immediately
            }

        } catch (IOException e) {
            System.err.println("Error in ClientHandler: " + e.getMessage());
        } finally {
            try {
                clientSocket.close(); // Close the socket when done
                System.out.println("Client disconnected.");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
