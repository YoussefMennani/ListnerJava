package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

import com.fleetManagementSystem.commons.position.dto.PositionResponse;
import com.fleetManagementSystem.commons.position.model.Position;
import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.bson.Document;
import org.bson.types.ObjectId;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private String imei;

    private ObjectId vehicle;
    KafkaProducer<String, String> producer;

    MongoCollection<Document> positionsCollection;
    MongoCollection<Document> vehiclesCollection;
    public ClientHandler(Socket socket, KafkaProducer<String, String> producer, MongoCollection<Document> positionsCollection,MongoCollection<Document> vehiclesCollection) {
        this.clientSocket = socket;
        this.imei = null;
        this.producer = producer;
        this.positionsCollection = positionsCollection;
        this.vehiclesCollection = vehiclesCollection;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Read IMEI from the client, only once after connection
            String imei = in.readLine();
            if (imei != null) {
                this.imei = imei;
                System.out.println("Received IMEI: " + imei);  // Process IMEI

                Document query = new Document("tracker.imei", imei);
                Document result = vehiclesCollection.find(query).first();
//                if (result != null) {
//                    System.out.println("Found vehicle: " + result.toJson());
//                    System.out.println("id Found vehicle: " + result.toJson());
//                } else {
//                    System.out.println("No vehicle found with the given IMEI.");
//                }

                if (result != null) {
                    // Extract _id
                    this.vehicle = (ObjectId) result.get("_id");
                    System.out.println("Vehicle ID: " + vehicle);
                } else {
                    System.out.println("No vehicle found with the given IMEI.");
                }

            } else {
                System.out.println("IMEI not received; client might have disconnected.");
                return;  // Exit if IMEI is not received
            }

            // Instantiate Gson for JSON parsing
            Gson gson = new Gson();

            // Now handle the position data continuously
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    // Parse the received JSON string into GPSPosition object
                    Position position = gson.fromJson(inputLine, Position.class);
                    //update last position
//                    Position positionData = Position.builder()
//                            .latitude(position.getLatitude())
//                            .longitude(position.getLongitude())
//                            .vehicleID(vehicle.toString())
//                            .imei(position.getImei())
//                            .speed(position.getSpeed())
//                            .timestamp(position.getTimestamp())
//                            .altitude(position.getAltitude())
//                            .heading(position.getHeading())
//                            .metrics(position.getMetrics())
//                            .build();
                    position.setVehicleID(vehicle.toString());
                    // Print the GPS position object
                    System.out.println("Received GPS Position from IMEI "+this.imei+" : " + position);

                    // Convert GPSPosition object to a BSON Document
                    Document document = Document.parse(gson.toJson(position));

                    Date now = new Date(); // Current date and time
                    document.put("createdAt", now);
                    document.put("updatedAt", now);

                    // Insert the document into MongoDB
                    positionsCollection.insertOne(document);

                    System.out.println("Document inserted successfully!");

                    System.out.println(" =============== < send Position to KAFKA TOPIC > ================= ");

                    // create a producer record
                    ProducerRecord<String, String> producerRecord =
                            new ProducerRecord<>("position-updates", inputLine);

                    producer.send(producerRecord);
                    producer.flush();

                    //update last position
                    PositionResponse lastPosition = PositionResponse.builder()
                            .latitude(position.getLatitude())
                            .longitude(position.getLongitude())
                            .imei(position.getImei())
                            .speed(position.getSpeed())
                            .timestamp(position.getTimestamp())
                            .altitude(position.getAltitude())
                            .heading(position.getHeading())
                            .metrics(position.getMetrics())
                            .build();


                    Document documentLastPosition = Document.parse(gson.toJson(lastPosition));
                    Document update = new Document("$set", new Document("lastPosition", documentLastPosition));
                    vehiclesCollection.updateOne(new Document("_id", this.vehicle), update);


                    // Send an acknowledgment back to the client
                    out.println("ACK: Position received");
                } catch (Exception e) {
                    System.err.println("Failed to parse GPS data: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Error in ClientHandler: " + e.getMessage());
        } finally {
            try {
                clientSocket.close(); // Close the socket when done
                System.out.println("Client disconnected.");
                producer.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
