import org.jsfml.graphics.*;
import org.jsfml.system.Vector2f;
import org.jsfml.window.Joystick;
import org.jsfml.window.VideoMode;
import org.jsfml.window.event.Event;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Vector;

public class Main
{
    public static double dt_countsPerSwerveRotation = 4096;
    public static int[] driveMultipliers = {1, 1, 1, 1};
    public static boolean[] stopped = {false, false, false, false},
            lastStopped = {false, false, false, false};
    public static double[] lastTarget = {0, 0, 0, 0};

    public static void main(String[] args)
    {
        int joystickNumber = 0;

        RenderWindow window = new RenderWindow(new VideoMode(800, 800), "Swerve Drive Visualization");
        window.setFramerateLimit(120);

        Texture arrowTexture = new Texture();
        try
        {
            arrowTexture.loadFromFile(Path.of("arrow.png"));
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        Vector2f robotCenter = new Vector2f(400, 400);
        Wheel topLeft = new Wheel(arrowTexture, new Vector2f(200, 200), robotCenter);
        Wheel topRight = new Wheel(arrowTexture, new Vector2f(600, 200), robotCenter);
        Wheel bottomLeft = new Wheel(arrowTexture, new Vector2f(200, 600), robotCenter);
        Wheel bottomRight = new Wheel(arrowTexture, new Vector2f(600, 600), robotCenter);

        double leftX, leftY, rotationMagnitude;

        while (window.isOpen())
        {
            for (Event event : window.pollEvents())
            {
                if (event.type == Event.Type.CLOSED)
                {
                    window.close();
                }
            }

            leftX = Joystick.getAxisPosition(joystickNumber, Joystick.Axis.X) / 100;
            leftY = -Joystick.getAxisPosition(joystickNumber, Joystick.Axis.Y) / 100;
            rotationMagnitude = Joystick.getAxisPosition(joystickNumber, Joystick.Axis.U) / 100;

            if (Math.abs(leftX) < 0.1) leftX = 0;
            if (Math.abs(leftY) < 0.1) leftY = 0;
            if (Math.abs(rotationMagnitude) < 0.1) rotationMagnitude = 0;

            /*
            System.out.println("Left X: " + leftX);
            System.out.println("Left Y: " + leftY);
            System.out.println("Right X: " + rightX + '\n');
             */

            Vector2f movement = new Vector2f((float) leftX, (float) leftY);

            /*
            End vector is vector of movement added to vector of rotation
            Vector of rotation has a magnitude equal to the value of the right X-axis
            Angle of the vector of rotation must be perpendicular to the line between the wheel and the center of the robot

            We know magnitude and angle
            magnitude = sqrt(y^2 + x^2)
            angle = arctan(y/x)
             */

            Vector<Vector2f> wheelVectors = new Vector<>();
            wheelVectors.add(Vector2f.add(new Vector2f(driveMultipliers[0] * movement.x, driveMultipliers[0] * movement.y), new Vector2f((float) (driveMultipliers[0] * rotationMagnitude * Math.cos(topLeft.getPerpendicularAngle())), (float) (driveMultipliers[0] * -rotationMagnitude * Math.sin(topLeft.getPerpendicularAngle())))));
            wheelVectors.add(Vector2f.add(new Vector2f(driveMultipliers[1] * movement.x, driveMultipliers[1] * movement.y), new Vector2f((float) (driveMultipliers[1] * rotationMagnitude * Math.cos(topRight.getPerpendicularAngle())), (float) (driveMultipliers[1] * -rotationMagnitude * Math.sin(topRight.getPerpendicularAngle())))));
            wheelVectors.add(Vector2f.add(new Vector2f(driveMultipliers[2] * movement.x, driveMultipliers[2] * movement.y), new Vector2f((float) (driveMultipliers[2] * rotationMagnitude * Math.cos(bottomLeft.getPerpendicularAngle())), (float) (driveMultipliers[2] * -rotationMagnitude * Math.sin(bottomLeft.getPerpendicularAngle())))));
            wheelVectors.add(Vector2f.add(new Vector2f(driveMultipliers[3] * movement.x, driveMultipliers[3] * movement.y), new Vector2f((float) (driveMultipliers[3] * rotationMagnitude * Math.cos(bottomRight.getPerpendicularAngle())), (float) (driveMultipliers[3] * -rotationMagnitude * Math.sin(bottomRight.getPerpendicularAngle())))));

            double largestMagnitude = 1;
            for (Vector2f vector : wheelVectors)
            {
                boolean largest = true;
                for (Vector2f comparisonVector : wheelVectors)
                {
                    if (Math.sqrt(Math.pow(vector.x, 2) + Math.pow(vector.y, 2))
                            < Math.sqrt(Math.pow(comparisonVector.x, 2) + Math.pow(comparisonVector.y, 2)))
                    {
                        largest = false;
                        break;
                    }
                }

                if (largest)
                {
                    largestMagnitude = Math.sqrt(Math.pow(vector.x, 2) + Math.pow(vector.y, 2));
                }
            }

            double multiplier = largestMagnitude > 1 ? 1 / largestMagnitude : 1;

            /*
            We want to set each vector to a value that is a fraction of the largest vector to normalize them
            The largest vector should be set to one, the others some fraction
             */

            for (int i = 0; i < wheelVectors.size(); ++i)
            {
                wheelVectors.setElementAt(new Vector2f((float) (wheelVectors.elementAt(i).x * multiplier), (float) (wheelVectors.elementAt(i).y * multiplier)), i);
            }

            turnToAngle(topLeft, Math.atan2(wheelVectors.get(0).y, wheelVectors.get(0).x) * 180 / Math.PI - 90, 0);
            turnToAngle(topRight, Math.atan2(wheelVectors.get(1).y, wheelVectors.get(1).x) * 180 / Math.PI - 90, 1);
            turnToAngle(bottomLeft, Math.atan2(wheelVectors.get(2).y, wheelVectors.get(2).x) * 180 / Math.PI - 90, 2);
            turnToAngle(bottomRight, Math.atan2(wheelVectors.get(3).y, wheelVectors.get(3).x) * 180 / Math.PI - 90, 3);

            topLeft.update();
            topRight.update();
            bottomLeft.update();
            bottomRight.update();

            window.clear(Color.BLACK);
            window.draw(topLeft);
            window.draw(topRight);
            window.draw(bottomLeft);
            window.draw(bottomRight);
            window.display();
        }
    }

    /*
    Given target angle, rotate wheel from current position counts to target ticks
    -360 <= target <= 360, -INF <= counts <= INF

    Add 90 degrees to the target so everything lines up correctly
    Convert the target to counts
    Put current position on scale from 0 to M, total counts in wheel rotation
    Rotate with PID to (original position + (absolute position - target))

    Can be optimized; it's faster to rotate to 180 degrees of the target and reverse output in some cases
    Opposite angle o is target - M/2; fix when negative by adding to M
    If o - c < target - c, turn to original position + (o - absolute position) and reverse output
    Else if target - absolute position > M/2, rotate the opposite direction but keep output the same
    This prevents situations where the computer doesn't realize that 0 and 359 are right next to each other
     */
    public static void turnToAngle(Wheel wheel, double angle, int index)
    {
        // This gives us the counts of the swivel on a scale from 0 to the total counts per rotation
        int rotationsCompleted = (int) Math.abs(wheel.getSelectedSensorPosition(0) / dt_countsPerSwerveRotation);
        double counts = (Math.abs(wheel.getSelectedSensorPosition(0)) - (rotationsCompleted * dt_countsPerSwerveRotation));// / dt_countsPerSwerveRotation * 360;

        // Converts target to ticks and puts it on scale from -360 to 360
        double target = angle / 360 * dt_countsPerSwerveRotation;

        // Puts negative counts in positive terms
        if (target < 0)
        {
            target = dt_countsPerSwerveRotation + target;
        }

        // Puts negative counts in positive terms
        if (counts < 0)
        {
            counts = dt_countsPerSwerveRotation + counts;
        }

        // We had to put the sensor position through Math.abs before, so this fixes absoluteCounts
        if (wheel.getSelectedSensorPosition(0) < 0)
        {
            counts = dt_countsPerSwerveRotation - counts;
        }

        // We could end it here, but optimizations and field-centric driving help a lot with efficiency

        // This enables field-centric driving. It adds the rotation of the robot to the total counts so it knows where to turn
        //mPigeon.getYawPitchRoll(ypr);
        //absoluteCounts += ypr[0] * dt_countsPerSwerveRotation;

        double oppositeAngle = target - (dt_countsPerSwerveRotation / 2);
        if (oppositeAngle < 0) oppositeAngle += dt_countsPerSwerveRotation;

        HashMap<String, Double> differences = new HashMap<>();
        differences.put("Best Case", Math.max(counts, target) - Math.min(counts, target));
        differences.put("Over Gap", Math.min(counts, target) + (dt_countsPerSwerveRotation - Math.max(counts, target)));
        differences.put("To Opposite Angle", Math.max(counts, oppositeAngle) - Math.min(counts, oppositeAngle));
        differences.put("To Opposite Angle Over Gap", Math.min(counts, oppositeAngle) + (dt_countsPerSwerveRotation - Math.max(counts, oppositeAngle)));

        String smallestDifference = "";
        for (HashMap.Entry<String, Double> pair : differences.entrySet())
        {
            boolean smallest = true;
            for (HashMap.Entry<String, Double> comparePair : differences.entrySet())
            {
                if (pair.getValue() > comparePair.getValue())
                {
                    smallest = false;
                    break;
                }
            }
            if (smallest)
            {
                smallestDifference = pair.getKey();
                break;
            }
        }

        if (counts > target) differences.replace("Best Case", -differences.get("Best Case"));
        if (target > counts) differences.replace("Over Gap", -differences.get("Over Gap"));
        if (counts > oppositeAngle) differences.replace("To Opposite Angle", -differences.get("To Opposite Angle"));
        if (oppositeAngle > counts)
            differences.replace("To Opposite Angle Over Gap", -differences.get("To Opposite Angle Over Gap"));

        if (Math.abs(differences.get(smallestDifference)) < 8)
        {
            wheel.set(0);
            stopped[index] = true;
            if ((!lastStopped[index] || target != lastTarget[index]) && (smallestDifference.equals("To Opposite Angle") || smallestDifference.equals("To Opposite Angle Over Gap")))
            {
                wheel.flip();
                driveMultipliers[index] = -driveMultipliers[index];
            }
        } else
        {
            wheel.set(differences.get(smallestDifference) > 0 ? 1 : -1);
            stopped[index] = false;
        }

        lastStopped[index] = stopped[index];
        lastTarget[index] = target;
    }
}

class Wheel extends Sprite
{
    private Vector2f robotCenter;

    private double output = 0;
    private double counts = 0;

    public Wheel(Texture texture, Vector2f position, Vector2f robotCenter)
    {
        super(texture);
        setOrigin(new Vector2f(
                getPosition().x + (getGlobalBounds().width / 2),
                getPosition().y + (getGlobalBounds().height / 2)));
        setPosition(position);
        this.robotCenter = robotCenter;
    }

    void set(double output)
    {
        this.output = output;
    }

    void update()
    {
        rotate((float) -output);
        counts += output / 360 * 4096;
    }

    double getSelectedSensorPosition(int pidx)
    {
        return counts;
    }

    void flip()
    {
        setScale(getScale().x, getScale().y * -1);
    }

    public double getRadius()
    {
        return Math.sqrt(Math.pow(robotCenter.x - getPosition().x, 2) + Math.pow(robotCenter.y - getPosition().y, 2));
    }

    public double getPerpendicularAngle()
    {
        return Math.atan2(Vector2f.sub(robotCenter, getPosition()).y, Vector2f.sub(robotCenter, getPosition()).x) - (Math.PI / 2);
    }
}
