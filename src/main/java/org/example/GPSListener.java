package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bson.Document;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GPSListener {

    private static final int PORT = 5000; // Port to listen on
    private static final int MAX_THREADS = 10; // Number of threads for handling clients

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS); // Thread pool for handling clients



        String bootstrapServers = "127.0.0.1:9092";

        // create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);


        try (ServerSocket serverSocket = new ServerSocket(PORT); MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            System.out.println("GPS Listener started on port " + PORT);

            // Access the database
            MongoDatabase geoPositionDB = mongoClient.getDatabase("GeoPositionDB");
            MongoDatabase geoTrackDB = mongoClient.getDatabase("GeoTrackDB");


            // Access the positionsCollection
            MongoCollection<Document> positionsCollection = geoPositionDB.getCollection("positions");
            MongoCollection<Document> vehiclesCollection = geoTrackDB.getCollection("vehicles");


            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming client connection
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Assign each client to a new thread
                executor.submit(new ClientHandler(clientSocket,producer,positionsCollection,vehiclesCollection));
            }

        } catch (IOException e) {
            System.err.println("Error in GPS Listener: " + e.getMessage());
        }
    }
}