package org.example;

public class GPSPosition {
    private double[] position; // Latitude and Longitude
    private double vitesse;     // Speed
    private long timestamps;    // Timestamp in milliseconds

    // Constructor
    public GPSPosition(double[] position, double vitesse, long timestamps) {
        this.position = position;
        this.vitesse = vitesse;
        this.timestamps = timestamps;
    }

    // Getters and Setters
    public double[] getPosition() {
        return position;
    }

    public void setPosition(double[] position) {
        this.position = position;
    }

    public double getVitesse() {
        return vitesse;
    }

    public void setVitesse(double vitesse) {
        this.vitesse = vitesse;
    }

    public long getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(long timestamps) {
        this.timestamps = timestamps;
    }

    @Override
    public String toString() {
        return "GPSPosition{" +
                "position=[" + position[0] + ", " + position[1] + "], " +
                "vitesse=" + vitesse +
                ", timestamps=" + timestamps +
                '}';
    }
}
