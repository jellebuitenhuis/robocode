package nl.saxion.ehi1vsc1;

import java.util.ArrayList;

import static java.lang.Math.*;

public class EnemyBullet {

    private double ticksToWall;
    private ArrayList<Double> headOnBulletX = new ArrayList<>();
    private ArrayList<Double> headOnBulletY = new ArrayList<>();
    private ArrayList<Double> consistentBulletX = new ArrayList<>();
    private ArrayList<Double> consistentBulletY = new ArrayList<>();
    private double myPredictedX;
    private double myPredictedY;

    public EnemyBullet(double startX, double startY, double bulletPower, double fireTime, double heading)
    {
        headOnPrediction(startX, startY, 20 - (3 * bulletPower), fireTime, heading);
    }

    /**
     * Assume the enemy shoots straight at us
     * @param startX double The X from which the bullet was fired
     * @param startY double The Y from which the bullet was fired
     * @param bulletSpeed double The speed of the bullet
     * @param fireTime double The time the bullet was fired
     * @param heading double The heading if the enemy looks straight at us
     */
    public void headOnPrediction(double startX, double startY, double bulletSpeed, double fireTime, double heading)
    {
        double predictedBulletX = startX;
        double predictedBulletY = startY;
        for (int i = 0; i < 100; i++) {
            predictedBulletX += bulletSpeed * sin(heading);
            predictedBulletY += bulletSpeed * cos(heading);
            headOnBulletX.add(predictedBulletX);
            headOnBulletY.add(predictedBulletY);
            if(predictedBulletX <= 0 || predictedBulletY <= 0 || predictedBulletX >= 800 || predictedBulletY >= 600)
            {
                ticksToWall = i+fireTime;
                break;
            }
        }
    }

    /**
     * Use our prediction algorithm to calculate where the enemy shoots
     * @param startX double The X from which the bullet was fired
     * @param startY double The Y from which the bullet was fired
     * @param bulletSpeed double The speed of the bullet
     * @param fireTime double The time the bullet was fired
     * @param heading double The heading if the enemy looks straight at us
     */
    public void consistentPrediction(double startX, double startY, double bulletSpeed, double fireTime, double heading) {
        double heading2 = atan2(myPredictedX-startX,myPredictedY-startY);
        if(heading2 < 0)
        {
            heading2 = 2*PI+heading2;
        }
        heading = heading2;
        double predictedBulletX = startX;
        double predictedBulletY = startY;
        for (int i = 0; i < 100; i++) {
            predictedBulletX += bulletSpeed * sin(heading);
            predictedBulletY += bulletSpeed * cos(heading);
            consistentBulletX.add(predictedBulletX);
            consistentBulletY.add(predictedBulletY);
            if(predictedBulletX <= 0 || predictedBulletY <= 0 || predictedBulletX >= 800 || predictedBulletY >= 600)
            {
                if(i+fireTime < ticksToWall)
                {
                    ticksToWall = i+fireTime;
                }
                break;
            }
        }
    }

    /**
     * Get the time it takes for the bullet to hit a wall
     * @return ticksToWall double The amount of time it takes for the bullet ot hit the wall
     */
    public double getTicksToWall()
    {
        return ticksToWall;
    }

    /**
     * Get all X positions of the bullet for headOnPrediction
     * @return headOnBulletX double The predicted X coordinates of the bullet
     */
    public ArrayList<Double> getheadOnBulletX()
    {
        return headOnBulletX;
    }

    /**
     * Get all Y positions of the bullet for headOnPrediction
     * @return headOnBulletY double The predicted Y coordinates of the bullet
     */
    public ArrayList<Double> getheadOnBulletY()
    {
        return headOnBulletY;
    }

    /**
     * Get all X positions of the bullet for consistentPrediction
     * @return consistentBulletX double The predicted X coordinates of the bullet
     */
    public ArrayList<Double> getConsistentBulletX()
    {
        return consistentBulletX;
    }

    /**
     * Get all Y positions of the bullet for consistentPrediction
     * @return consistentBulletY double The predicted Y coordinates of the bullet
     */
    public ArrayList<Double> getConsistentBulletY()
    {
        return consistentBulletY;
    }

    /**
     * Set my predicted X, used by consistentPrediction
     * @param myPredictedX double The predicted X coordinates of the calling robot
     */
    public void setMyPredictedX(Double myPredictedX)
    {
        this.myPredictedX = myPredictedX;
    }

    /**
     * Set my predicted Y, used by consistentPrediction
     * @param myPredictedY double The predicted Y coordinates of the calling robot
     */
    public void setMyPredictedY(Double myPredictedY)
    {
        this.myPredictedY = myPredictedY;
    }

}
