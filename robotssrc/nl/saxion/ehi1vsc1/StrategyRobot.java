package nl.saxion.ehi1vsc1;

import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.Base64;
import java.util.ArrayList;

import static java.lang.Math.*;
import static robocode.util.Utils.*;

/**
 * inhoud:
 * moveTo(double coordinateX, double coordinateY)
 * avoidWallStartUp(int maxDistanceToWall)
 * avoidWall(int maxDistanceToWall)
 * randomMovement(double maxRandom)
 * reverse()
 * circle(double radius, double margin, double enemyBearing, double enemyDistance)
 * ram(double enemyBearing, double enemyDistance, double radius)
 * Getter Setters for variables
 */

public class StrategyRobot extends TeamRobot {

    private double oldEnergy;
    private ArrayList<Bullet> bulletListPrediction = new ArrayList<>();
    private double predictionAccuracy = 100;
    private double predictionHit = 0;
    private ArrayList<Bullet> bulletListHeadOn= new ArrayList<>();
    private double headOnAccuracy = 50;
    private double headOnHit = 0;

    //temp identifier
    private static final int RAM = 0;
    private static final int SHOOT = 1;

    private int type = SHOOT;


    private double oldHeading = 0;

    double bulletPower = 3;

    private double enemyBulletPower;
    protected ArrayList<EnemyBullet> bullets = new ArrayList<>();
    private double myOldHeading;
    private double myPredictedX;
    private double myPredictedY;
    private boolean isMovingOutOfTheWay = true;

    private boolean onX = false;
    private boolean onY = false;

    //run(), circle(), ram(), reverse()
    private boolean movingForward;

    //avoidWall()
    private boolean wallTooClose;

    //ram()
    private boolean isRammed;

    //communication
    private OtherRobot targetEnemy;
    private OtherRobot lastScannedEnemy;
    private OtherRobot teamMate;

    /**
     * Run the robot strategy
     * @author Jurgen van de Wardt
     * @author Rick Holtman
     */
    public void run() {
        this.init();

        //Scan the whole field once till we find a target
        setTurnRadarRight(1000);
        execute();
        //If we have no target or we lost it, scan the field again
        while (true) {
            if (getRadarTurnRemaining() == 0) {
                turnRadarLeft(1000);
            }

            if (targetEnemy != null) {
                if(type == RAM){
                    ram(targetEnemy.getBearing(), targetEnemy.getDistance());
                }else if(type == SHOOT){
                    circle(targetEnemy.getBearing(), targetEnemy.getDistance(), 130); //radius <80 or >120 to avoid collision with rambot
                }
            }

            avoidWall();
            execute();
        }
    }

    /**
     * Initialise the robots
     * @author Jurgen van der Wardt
     */
    private void init() {
        /*
        Set the colors and make sure the radar, gun and body turn independent.
         */
        setBodyColor(Color.RED);
        setGunColor(Color.WHITE);
        setRadarColor(Color.BLUE);
        setScanColor(Color.ORANGE);
        setBulletColor(Color.YELLOW);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        this.initMovement();
    }

    /**
     * Initialise the movement variables
     * @author Rick Holtman
     */
    private void initMovement(){
        //needed for movement
        setAhead(30000);

        //needed for correct movement
        movingForward = true;

        //avoidWall(); initial start value
        wallTooClose = true;

        isRammed = false;

    }

    /**
     * Reverses when it hits a robot
     * @author Rick Holtman
     * @param e HitRobotEvent
     */
    @Override
    public void onHitRobot(HitRobotEvent e) {
        //type is the identifier for rambot or shootbot
        if(type == RAM && e.isMyFault() && !isTeammate(e.getName())){
            isRammed = true;
        }
        if(!wallTooClose){
            reverse(); // bounce
        }
    }


    /**
     * Resets the teammate and targetEnemy when a robot died
     * @author Jesse Sterkenburgh
     * @param e RobotDeathEvent
     */
    public void onRobotDeath(RobotDeathEvent e) {
        targetEnemy = null;
        teamMate = null;
    }

    /**
     * Checks for actions to take when scanning a robot,
     * communicates with teammates for targeting and pairing with a teammate
     * @author Jelle Buitenhuis
     * @param e ScannedRobotEvent
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        doCommunication(e);

        //If we have no target and it is not a teammate, set the name as our target. Should be overwritten by communication, this is temporary
        if (targetEnemy == null && !isTeammate(e.getName())) {
            targetEnemy = new OtherRobot(e.getName(), getEnemyX(e.getDistance(), e.getBearing()),getEnemyY(e.getDistance(), e.getBearing()), e.getHeading(), e.getBearing(), e.getVelocity(), e.getDistance(), e.getEnergy());
        }



        //If the scanned bot is our target. Should be overwritten by communication, this is temporary
        if (targetEnemy != null) {
            //Predict where we will go. Needed for bullet dodging
            selfPrediction();

            if (e.getName().equals(targetEnemy.getName())) {
                //Set all the needed variables from the target. This should be put in otherRobot by communication
                double enemyEnergy = e.getEnergy();
                double energyDifference = enemyEnergy - oldEnergy;
                oldEnergy = enemyEnergy;
                //Scan the target, this gives a 100% lock
                setTurnRadarRight(2.0 * normalRelativeAngleDegrees(getHeading() + e.getBearing() - getRadarHeading()));
                //Predict where our target will go
                prediction();
                //Turn our gun towards the place prediction provided
                if(bulletListPrediction.size() <= 10)
                {
                    shootTargetPrediction();
                }
                else if (bulletListHeadOn.size() <= 10)
                {
                    shootTargetHeadon();
                }
                if(bulletListHeadOn.size() >= 10)
                {
                    if(predictionAccuracy >= headOnAccuracy)
                    {
                        shootTargetPrediction();
                    }
                    else
                    {
                        shootTargetHeadon();
                    }
                }
                //Check if a bullet is fired. We pass the scanned target along to the robot.
                checkBullet(e, energyDifference);
            }
        }
    }


    /**
     * Counts the hits made for each shooting decision taken
     * @author Jelle Buitenhuis
     * @param e BulletHitEvent
     */
    public void onBulletHit(BulletHitEvent e)
    {
        for(Bullet b: bulletListPrediction)
        {
            if(e.getBullet().equals(b))
            {
                predictionHit++;
            }
        }
        for(Bullet b : bulletListHeadOn)
        {
            if(e.getBullet().equals(b))
            {
                headOnHit++;
            }
        }
    }

    /**
     * Turn our gun towards the predicted position and fires
     * @author Jelle Buitenhuis
     */
    private void shootTargetPrediction() {
        try {
            double angleToTarget = normalAbsoluteAngle(Math.atan2(targetEnemy.getPredictedX() - getX(), targetEnemy.getPredictedY() - getY()));
            double targetAngle = angleToTarget - getGunHeadingRadians();
            setTurnGunRightRadians(targetAngle);
            if (getGunHeat() <= 0) {
                Bullet bulletObject = setFireBullet(bulletPower);
                bulletListPrediction.add(bulletObject);
                predictionAccuracy = predictionHit/bulletListPrediction.size();
            }
        }
        catch(NullPointerException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Hits the target head-on
     * @author Jelle Buitenhuis
     */
    private void shootTargetHeadon() {
        try {
            double angleToTarget = normalAbsoluteAngle(Math.atan2(targetEnemy.getX() - getX(), targetEnemy.getY() - getY()));
            double targetAngle = angleToTarget - getGunHeadingRadians();
            setTurnGunRightRadians(targetAngle);
            if (getGunHeat() <= 0) {
                Bullet bulletObject = setFireBullet(bulletPower);
                bulletListHeadOn.add(bulletObject);
                headOnAccuracy = headOnHit/bulletListHeadOn.size();
            }
        }
        catch(NullPointerException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Paints prediction lines on the battlefield
     * @author Jelle Buitenhuis
     * @param g Grahpics2D
     */
    public void onPaint(Graphics2D g) {
        if(targetEnemy != null)
        {
            g.setColor(new Color(255, 0, 16, 100));
            g.drawLine((int) targetEnemy.getX(), (int) targetEnemy.getY(), (int) getX(), (int) getY());
            g.fillRect((int) targetEnemy.getX() - 20, (int) targetEnemy.getY() - 20, 40, 40);
            g.setColor(new Color(1, 0, 255, 100));
            g.drawLine((int) targetEnemy.getPredictedX(), (int) targetEnemy.getPredictedY(), (int) getX(), (int) getY());
            g.fillRect((int) targetEnemy.getPredictedX() - 20, (int) targetEnemy.getPredictedY() - 20, 40, 40);
            g.setColor(new Color(29, 255, 148, 100));
            g.drawLine((int) myPredictedX, (int) myPredictedY, (int) getX(), (int) getY());
            g.fillRect((int) myPredictedX - 20, (int) myPredictedY - 20, 40, 40);
            for(int j = 0; j < bullets.size();j++) {
                if(bullets.get(j).getTicksToWall() < getTime())
                {
                    bullets.remove(j);
                    j -= 1;
                }
                for(int i = 0; i < bullets.get(j).getheadOnBulletX().size(); i++)
                {
                    double predictedBulletX = bullets.get(j).getheadOnBulletX().get(i);
                    double predictedBulletY = bullets.get(j).getheadOnBulletY().get(i);
                    g.setColor(new Color(11, 255, 44, 100));
                    g.fillOval((int) predictedBulletX - 5, (int) predictedBulletY - 5, 10, 10);
                }
                for(int i = 0; i < bullets.get(j).getConsistentBulletX().size(); i++)
                {
                    double predictedBulletX = bullets.get(j).getConsistentBulletX().get(i);
                    double predictedBulletY = bullets.get(j).getConsistentBulletY().get(i);
                    g.setColor(new Color(255, 225, 12, 100));
                    g.fillOval((int) predictedBulletX - 5, (int) predictedBulletY - 5, 10, 10);
                }
            }
        }
    }

    /**
     * Check for the actions to take on the scanned enemy
     * @author Jesse Sterkenburgh
     * @param e ScannedRobotEvent
     */
    private void doCommunication(ScannedRobotEvent e) {
        lastScannedEnemy = new OtherRobot(e.getName(), getEnemyX(e.getDistance(), e.getBearingRadians()), getEnemyY(e.getDistance(), e.getBearingRadians()), e.getHeadingRadians(), e.getBearingRadians(), e.getVelocity(), e.getDistance(), e.getEnergy());
        try {
            lastScannedEnemy.setPredictedX(lastScannedEnemy.getPredictedX());
            lastScannedEnemy.setPredictedY(lastScannedEnemy.getPredictedY());
        }
        catch(NullPointerException e1)
        {
            e1.printStackTrace();
        }
        if (isTeammate(e.getName())) {
            handleTeamMateCommunication(e);
        } else {
            handleTargeting(e);
        }
    }

    /**
     * Handle communication regarding teammate pairing
     * @author Jesse Sterkenburgh
     * @param e Scanned Robot
     */
    private void handleTeamMateCommunication(ScannedRobotEvent e) {
        if (teamMate == null) {
            try {
                //send request
                sendMessage(e.getName(), "REQUEST_TEAM_MATE");

                //send self for pairing
                OtherRobot self = new OtherRobot(getName(), getX(), getY(), getHeading(), 0, getVelocity(), 0, getEnergy());
                sendMessage(e.getName(), "TEAMMATE_" + toString(self));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else if (targetEnemy == null) {
            try {
                sendMessage(teamMate.getName(), "REQUEST_TARGET_ENEMY_" + toString(lastScannedEnemy));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Handle communication regarding targeting
     * @author Jesse Sterkenburgh
     * @param e Scanned Robot
     */
    private void handleTargeting(ScannedRobotEvent e) {
        if (targetEnemy == null) {
            if (teamMate == null) {
                targetEnemy = lastScannedEnemy;
            } else {
                try {
                    sendMessage(teamMate.getName(), "REQUEST_TARGET_ENEMY_" + toString(lastScannedEnemy));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } else if (targetEnemy.getName().equals(e.getName())) {
            targetEnemy = lastScannedEnemy;
        }
    }

    /**
     * Handle all the received messages by teammates and take action accordingly
     * @author Jesse Sterkenburgh
     * @param e MessageEvent
     */
    private void handleReceivedCommunication(MessageEvent e) {
        if (!isTeammate(e.getSender())) {
            return;
        }

        switch (e.getMessage().toString()) {
            case "REQUEST_TEAM_MATE":
                if (teamMate == null) {
                    try {
                        OtherRobot self = new OtherRobot(getName(), getX(), getY(), getHeading(), 0, getVelocity(), 0, getEnergy());
                        sendMessage(e.getSender(), "TEAMMATE_" + toString(self));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                return;
            case "TARGET_ENEMY_NONE":
                targetEnemy = lastScannedEnemy;
                return;
        }

        handleReceivedRobotData(e);
    }

    /**
     * Handle received robot data and update our target or teammate
     * according to the data received by the teammate
     * @author Jesse Sterkenburgh
     * @param e Message Received
     */
    private void handleReceivedRobotData(MessageEvent e) {
        if (e.getMessage().toString().substring(0, 6).equals("ENEMY_")) {
            try {
                targetEnemy = (OtherRobot) fromString(e.getMessage().toString().substring(6));
                targetEnemy.updateBearing(getX(), getY());
            } catch (IOException | ClassNotFoundException e1) {
                e1.printStackTrace();
            }
            return;
        }

        if (e.getMessage().toString().substring(0, 9).equals("TEAMMATE_")) {
            if (teamMate != null) {
                return;
            }
            try {
                teamMate = (OtherRobot) fromString(e.getMessage().toString().substring(9));
                teamMate.updateBearing(getX(), getY());
            } catch (IOException | ClassNotFoundException e1) {
                e1.printStackTrace();
            }
            return;
        }

        if (e.getMessage().toString().substring(0,21).equals("REQUEST_TARGET_ENEMY_")) {
            if (targetEnemy == null) {
                try {
                    sendMessage(e.getSender(), "TARGET_ENEMY_NONE");
                    targetEnemy = (OtherRobot) fromString(e.getMessage().toString().substring(21));
                } catch (IOException | ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
            } else {
                try {
                    sendMessage(e.getSender(), "ENEMY_" + toString(targetEnemy));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return;
        }
    }

    /**
     * Check if the enemy fired a bullet
     * @author Jelle Buitenhuis
     * @param e ScannedRobotEvent
     * @param energyDifference double, difference in energy of the enemy
     */
    private void checkBullet(ScannedRobotEvent e, double energyDifference) {
        if (energyDifference >= -3 && energyDifference <= -0.1) {
            enemyBulletPower = abs(energyDifference);
            double enemyX = getEnemyX(targetEnemy.getDistance(),targetEnemy.getBearing());
            double enemyY = getEnemyY(targetEnemy.getDistance(),targetEnemy.getBearing());
            double bulletHeading = normalAbsoluteAngle(getHeadingRadians() + e.getBearingRadians() + PI);
            double fireTime = getTime();
            //Create a new EnemyBullet with these variables
            EnemyBullet bullet = new EnemyBullet(enemyX, enemyY, enemyBulletPower, fireTime, bulletHeading);
            //Add the bullet to the list of bullets in the air
            bullets.add(bullet);
            bullet.setMyPredictedX(myPredictedX);
            bullet.setMyPredictedY(myPredictedY);
            //Predict where we think that the enemy thinks we will be
            bullet.consistentPrediction(enemyX, enemyY, 20 - (3 * enemyBulletPower), fireTime, bulletHeading);
            //setTurnRight(normalRelativeAngleDegrees(e.getBearing() + 90));
            execute();
        }
    }

    /**
     * Trigger handling of received communication
     * @author Jesse Sterkenburgh
     * @param e MessageEvent
     */
    public void onMessageReceived(MessageEvent e) {
        handleReceivedCommunication(e);
    }

    /**
     * Gets the enemy X
     * @author Jelle Buitenhuis
     * @param distance to enemy
     * @param bearing of the enemy
     * @return enemy X
     */
    private double getEnemyX(double distance, double bearing) {
        return distance * Math.sin(bearing + getHeadingRadians()) + getX();
    }

    /**
     * Gets the enemy Y
     * @author Jelle Buitenhuis
     * @param distance to enemy
     * @param bearing of the enemy
     * @return enemy Y
     */
    private double getEnemyY(double distance, double bearing) {
        return distance * Math.cos(bearing + getHeadingRadians()) + getY();
    }

    /**
     * Predict where the enemy will go
     * @author Jelle Buitenhuis
     */
    private void prediction() {
        try {
            double enemyHeading = targetEnemy.getHeading();
            double enemyVelocity = targetEnemy.getVelocity();
            double headingDifference = enemyHeading - oldHeading;
            oldHeading = enemyHeading;
            double enemyX = targetEnemy.getX();
            double enemyY = targetEnemy.getY();
            double bulletDistance = Point2D.Double.distance(getX(), getY(), targetEnemy.getPredictedX(), targetEnemy.getPredictedY());
            bulletPower = Math.min(bulletPower,3);
            double bulletSpeed = 20 - (3 * bulletPower);
            bulletPower = (600 / bulletDistance);
            double bulletTime = bulletDistance / bulletSpeed;
            double enemyPredictedX = enemyX;
            double enemyPredictedY = enemyY;
            for (int i = 0; i < bulletTime; i++) {
                enemyPredictedX += enemyVelocity * sin(enemyHeading);
                enemyPredictedY += enemyVelocity * cos(enemyHeading);
                enemyHeading += headingDifference;
                enemyPredictedX = Math.min(Math.max(18.0, enemyPredictedX), getBattleFieldWidth() - 18.0);
                enemyPredictedY = Math.min(Math.max(18.0, enemyPredictedY), getBattleFieldHeight() - 18.0);
                targetEnemy.setPredictedX(enemyPredictedX);
                targetEnemy.setPredictedY(enemyPredictedY);
            }

        }
        catch(NullPointerException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Predict enemies prediction behaviour
     * @author Jelle Buitenhuis
     */
    private void selfPrediction() {
        if(targetEnemy != null){
            double enemyX = targetEnemy.getX();
            double enemyY = targetEnemy.getY();
            double myHeading = getHeadingRadians();
            double myHeadingDifference = myHeading - myOldHeading;
            myOldHeading = myHeading;
            double myVelocity = getVelocity();
            double enemyBulletDistance = Point2D.Double.distance(enemyX, enemyY, myPredictedX, myPredictedY);
            double enemyBulletSpeed = 20 - (3 * enemyBulletPower);
            double enemyBulletTime = enemyBulletDistance / enemyBulletSpeed;
            myPredictedX = getX();
            myPredictedY = getY();
            for (int i = 0; i < enemyBulletTime; i++) {
                myPredictedX += myVelocity * sin(myHeading);
                myPredictedY += myVelocity * cos(myHeading);
                myHeading += myHeadingDifference;
                myPredictedX = Math.min(Math.max(18.0, myPredictedX), getBattleFieldWidth() - 18.0);
                myPredictedY = Math.min(Math.max(18.0, myPredictedY), getBattleFieldHeight() - 18.0);
            }
        }
    }

    /**
     * If we are on a bullet line we move. Needs wall avoidance to not hit the wall.
     * @author Rick Holtman
     * @author Jelle Buitenhuis
     */
    private void bulletDodge() {
        //Get all the positions the bullet can take
        getBulletPos();

        if(onX && onY && !isMovingOutOfTheWay && !wallTooClose){
            reverse();
            isMovingOutOfTheWay = true;
        }else if (!wallTooClose){
            resetMovement();
        }

        if(!onX && !onY && isMovingOutOfTheWay){
            isMovingOutOfTheWay = false;
        }
        }

    @Override
    public void onHitWall(HitWallEvent event) {
        super.onHitWall(event);
        reverse();
    }

    /**
     * Get all the positions the bullet can take
     * @author Jelle Buitenhuis
     */
    private void getBulletPos() {
        for (EnemyBullet bullet : bullets) {
            for (int i = 0; i < bullet.getheadOnBulletX().size(); i++) {
                if (round(getX()) <= round(bullet.getheadOnBulletX().get(i)) + 18 && round(getX()) >= round(bullet.getheadOnBulletX().get(i)) - 18) {
                    if (round(getY()) <= round(bullet.getheadOnBulletY().get(i)) + 18 && round(getY()) >= round(bullet.getheadOnBulletY().get(i)) - 18) {
                        onX = true;
                        onY = true;
                        break;
                    }
                } else {
                    onX = false;
                    onY = false;
                }
            }
        }
    }

    /**
     * Moves to given coordinates; nothing else needed for implementation
     * @author Rick Holtman
     * @param coordinateX Target X coordinate
     * @param coordinateY Target Y coordinate
     */
    private void moveTo(double coordinateX, double coordinateY) {
        coordinateX -= getX();
        coordinateY -= getY();
        double angle = robocode.util.Utils.normalRelativeAngle(Math.atan2(coordinateX, coordinateY) - Math.toRadians(getHeading()));
        double turnAngle = Math.atan(Math.tan(angle));

        turnRight(Math.toDegrees(turnAngle));
        ahead(Math.hypot(coordinateX, coordinateY) * (angle == turnAngle ? 1 : -1));
    }

    /**
     * Avoids wall @ distance: maxDistanceToWall
     * @author Rick Holtman
     */
    private void avoidWall(){
        int maxDistanceToWall = 50;
        if (getX() > maxDistanceToWall &&
                getY() > maxDistanceToWall &&
                getBattleFieldWidth() - getX() > maxDistanceToWall &&
                getBattleFieldHeight() - getY() > maxDistanceToWall &&
                wallTooClose) {
            wallTooClose = false;
        }
        if (getX() <= maxDistanceToWall ||
                getY() <= maxDistanceToWall ||
                getBattleFieldWidth() - getX() <= maxDistanceToWall ||
                getBattleFieldHeight() - getY() <= maxDistanceToWall ) {
            if (!wallTooClose){
                reverse();
                wallTooClose = true;
            }
        }
    }

    /**
     * Moves in a circular motion around target @ distance: radius with margin: margin
     * @author Rick Holtman
     * @param bearingR bearing of the target in radians
     * @param distance distance to the target
     */
    private void circle(double bearingR, double distance, double radius){

        bulletDodge();

        double bearing = toDegrees(bearingR);

            double margin = 20;
            /*
             * Needed for circle()
             * Min value = 1 -> moves in an almost perfect circle, very slow adjustment -> robot probably cant keep up
             * Max value = 89 -> Moves in a straigt line to the new radius, super fast -> no circular motion anymore
             * Max value for random movement is the initial value - 1 -> 0 or negative values not allowed, so if adjustment speed = 30 the maximum for randomMovement = 29
             *
             */
            double circleRadiusAdjustmentSpeed = 30;// + randomMovement;

            /*
             * Needed for circle()
             * value 0 ->  perfect circle (preferred)
             * value < 0 -> spiral inward
             * value > 0 -> spiral outward
             * Max value for random movement is 5;
             */
            double circleWithinRadiusRandom = 0;// + randomMovement;

            if(distance < radius - margin){ //too close: spiral outwards with speed circleRadiusAdjustmentSpeed
                if (movingForward){
                    setTurnRight(normalRelativeAngleDegrees(bearing + 90 + circleRadiusAdjustmentSpeed));
                } else {
                    setTurnRight(normalRelativeAngleDegrees(bearing + 90 - circleRadiusAdjustmentSpeed));
                }
            }else if(distance > radius + margin){ //too far: spiral inwards with speed circleRadiusAdjustmentSpeed
                if (movingForward){
                    setTurnRight(normalRelativeAngleDegrees(bearing + 90 - circleRadiusAdjustmentSpeed));
                } else {
                    setTurnRight(normalRelativeAngleDegrees(bearing + 90 + circleRadiusAdjustmentSpeed));
                }
            }else{ //within radius with margin: circle
                setTurnRight(normalRelativeAngleDegrees(bearing + 90 + circleWithinRadiusRandom));
            }
        }




    /**
     * @author Rick Holtman
     * @param bearingR bearing of target in radians
     * @param distance distance to target
     */
    private void ram(double bearingR, double distance){
        resetMovement();
        double bearing = toDegrees(bearingR);
        double radius = 200;
        double margin = 20;

        /*
         * Min value = 0 -> Moves in a straight line to robot, very fast
         * Max value = 89 -> Almost perfect circular movement, extremely slow
         * Max value for random movement is equal to de initial value divided by 2, so if the speed = 30 the maximum for randomMovement = 15
         */
        int ramRamSpeed = 0;

        if(distance > radius - margin){
            isRammed = false;
        }

        if(isRammed){ //spiaral out
            circle(bearingR, distance, radius);
        }else{ //ram
            if (!movingForward){
                reverse();
            }
            setTurnRight(normalRelativeAngleDegrees(bearing + ramRamSpeed));
        }
    }



    /**
     * Reverse the robot movement
     * @author Rick Holtman
     */
    private void reverse() {
        if (movingForward) {
            setBack(30000);
            movingForward = false;
        } else {
            setAhead(30000);
            movingForward = true;
        }
    }

    /**
     * Resets the movement of the robot
     * @author Rick Holtman
     */
    protected void resetMovement(){
        if(movingForward){
            setAhead(30000);
        }else{
            setBack(30000);
        }
    }

    /**
     * Read the object from Base64 string.
     * @author Jesse Sterkenburgh
     * @param s String
     */
    private static Object fromString(String s) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(s);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }

    /**
     * Write the object to a Base64 string.
     * @author Jesse Sterkenburgh
     * @param o Serializable string
     */
    private static String toString(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}