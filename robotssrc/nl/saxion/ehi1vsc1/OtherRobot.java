package nl.saxion.ehi1vsc1;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class OtherRobot implements Serializable {

    private String name;
    private double heading;
    private double bearing;
    private double velocity;
    private double distance;
    private double energy;
    private double x;
    private double y;
    private double predictedX;
    private double predictedY;

    /**
     *
     * @param name String Name of the robot
     * @param x double Current X of the robot
     * @param y double Current Y of the robot
     * @param heading double Current heading of the robot
     * @param bearing double Current bearing of the robot
     * @param velocity double Current velocity of the robot
     * @param distance double Current distance of the robot to the creator robot
     * @param energy double Current energy of the robot
     */
    public OtherRobot(String name, double x, double y, double heading, double bearing, double velocity, double distance, double energy)
    {
        this.name = name;
        this.heading = heading;
        this.bearing = bearing;
        this.velocity = velocity;
        this.distance = distance;
        this.energy = energy;
        this.x = x;
        this.y = y;
    }

    /**
     * Set heading
     * @param heading double The heading of the robot
     */
    public void setHeading(double heading) {
        this.heading = heading;
    }

    /**
     * Set bearing
     * @param bearing double The bearing of the robot
     */
    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    /**
     * Set distance
     * @param distance double The distance of the robot to the creator
     */
    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * Set velocity
     * @param velocity double The velocity of the robot
     */
    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    /**
     * Set X
     * @param x double The X coordinate of the robot
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Set Y
     * @param y double The Y coordinate of the robot
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Set predictedX
     * @param predictedX double The predicted X coordinate of the robot
     */
    public void setPredictedX(double predictedX) {
        this.predictedX = predictedX;
    }
    /**
     * Set predictedY
     * @param predictedY double The predicted Y coordinate of the robot
     */
    public void setPredictedY(double predictedY) {
        this.predictedY = predictedY;
    }

    /**
     * Set energy
     * @param energy double The energy of the robot
     */
    public void setEnergy(double energy) {
        this.energy = energy;
    }

    /**
     * Get energy
     * @return energy double The energy of the robot
     */
    public double getEnergy() {
        return energy;
    }

    /**
     * Get predictedX
     * @return predictedX double The predicted X coordinate of the robot
     */
    public double getPredictedX() {
        return predictedX;
    }

    /**
     * Get predictedY
     * @return predictedY double The predicted Y coordinate of the robot
     */
    public double getPredictedY() {
        return predictedY;
    }

    /**
     * Get X
     * @return x double The X coordinate of the robot
     */
    public double getX() {
        return x;
    }

    /**
     * Get Y
     * @return y double The Y coordinate of the robot
     */
    public double getY() {
        return y;
    }

    /**
     * Get bearing
     * @return bearing double The robots bearing
     */
    public double getBearing() {
        return bearing;
    }

    /**
     * Get distance
     * @return distance double The robots distance to the creator
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Get heading
     * @return heading double The robots heading
     */
    public double getHeading() {
        return heading;
    }

    /**
     * Get velocity
     * @return double velocity The robots velocity
     */
    public double getVelocity() {
        return velocity;
    }

    /**
     * Get name
     * @return String name The robots name
     */
    public String getName() {
        return name;
    }

    /**
     * Update the bearing relevant to the robot who calls this function
     * @param scannerX double The X of the caller
     * @param scannerY double The Y of the caller
     */
    public void updateBearing(double scannerX, double scannerY) {
        double xDifference = scannerX - this.x;
        double yDifference = scannerY - this.y;
        double distance = Point2D.distance(scannerX, scannerY, this.x, this.y);
        double angle = Math.toDegrees(Math.asin(xDifference / distance));
        double bearing = 0;

        if (xDifference > 0 && yDifference > 0) { // both pos: lower-Left
            bearing = angle;
        } else if (xDifference < 0 && yDifference > 0) { // x neg, y pos: lower-right
            bearing = 360 + angle; // arcsin is negative here, actuall 360 - ang
        } else if (xDifference > 0 && yDifference < 0) { // x pos, y neg: upper-left
            bearing = 180 - angle;
        } else if (xDifference < 0 && yDifference < 0) { // both neg: upper-right
            bearing = 180 - angle; // arcsin is negative here, actually 180 + ang
        }

        this.bearing = bearing;
    }
}
