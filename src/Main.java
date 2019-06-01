import org.jsfml.graphics.*;
import org.jsfml.system.Vector2f;
import org.jsfml.window.Joystick;
import org.jsfml.window.VideoMode;
import org.jsfml.window.event.Event;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Vector;

public class Main
{
    public static double dt_countsPerSwerveRotation = 4096;
    public static int[] driveMultipliers = {1, 1, 1, 1};

    public static void main(String[] args)
    {
        int joystickNumber = 0;//1;

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
            for(Event event : window.pollEvents())
            {
                if (event.type == Event.Type.CLOSED)
                {
                    window.close();
                }
            }

            leftX = -Joystick.getAxisPosition(joystickNumber, Joystick.Axis.X) / 100;
            leftY = -Joystick.getAxisPosition(joystickNumber, Joystick.Axis.Y) / 100;
            rotationMagnitude = -Joystick.getAxisPosition(joystickNumber, Joystick.Axis.U) / 100;

            System.out.println(leftX);

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
            wheelVectors.add(Vector2f.add(new Vector2f(driveMultipliers[0] * movement.x, driveMultipliers[0] * movement.y), new Vector2f((float) (rotationMagnitude * Math.cos(topLeft.getPerpendicularAngle())), (float) (rotationMagnitude * Math.sin(topLeft.getPerpendicularAngle())))));
            wheelVectors.add(Vector2f.add(new Vector2f(driveMultipliers[1] * movement.x, driveMultipliers[1] * movement.y), new Vector2f((float) (rotationMagnitude * Math.cos(topRight.getPerpendicularAngle())), (float) (rotationMagnitude * Math.sin(topRight.getPerpendicularAngle())))));
            wheelVectors.add(Vector2f.add(new Vector2f(driveMultipliers[2] * movement.x, driveMultipliers[2] * movement.y), new Vector2f((float) (rotationMagnitude * Math.cos(bottomLeft.getPerpendicularAngle())), (float) (rotationMagnitude * Math.sin(bottomLeft.getPerpendicularAngle())))));
            wheelVectors.add(Vector2f.add(new Vector2f(driveMultipliers[3] * movement.x, driveMultipliers[3] * movement.y), new Vector2f((float) (rotationMagnitude * Math.cos(bottomRight.getPerpendicularAngle())), (float) (rotationMagnitude * Math.sin(bottomRight.getPerpendicularAngle())))));

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

                if(largest)
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

            turnToAngle(topLeft, Math.atan2(wheelVectors.get(0).y, wheelVectors.get(0).x) * 180 / Math.PI, 0);
            turnToAngle(topRight, Math.atan2(wheelVectors.get(1).y, wheelVectors.get(1).x) * 180 / Math.PI, 1);
            turnToAngle(bottomLeft, Math.atan2(wheelVectors.get(2).y, wheelVectors.get(2).x) * 180 / Math.PI, 2);
            turnToAngle(bottomRight, Math.atan2(wheelVectors.get(3).y, wheelVectors.get(3).x) * 180 / Math.PI, 3);

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

            System.out.println();
        }
    }

    public static void turnToAngle(Wheel wheel, double angle, int index)
    {
        // This gives us the counts of the swivel as if rotating it 360 degrees looped its angle back to 0
        int rotationsCompleted = (int) Math.abs(wheel.getSelectedSensorPosition(0) / dt_countsPerSwerveRotation);
        double absoluteCounts = (Math.abs(wheel.getSelectedSensorPosition(0)) - (rotationsCompleted * dt_countsPerSwerveRotation));// / dt_countsPerSwerveRotation * 360;

        double counts = angle / 360 * dt_countsPerSwerveRotation;

        // Puts negative counts in positive terms
        if (absoluteCounts < 0)
        {
            absoluteCounts = dt_countsPerSwerveRotation + absoluteCounts;
        }

        // We had to put the sensor position through Math.abs before, so this fixes absoluteCounts
        if (wheel.getSelectedSensorPosition(0) < 0)
        {
            absoluteCounts = dt_countsPerSwerveRotation - absoluteCounts;
        }

        // This enables field-centric driving. It adds the rotation of the robot to the total counts so it knows where to turn
        //mPigeon.getYawPitchRoll(ypr);
        //absoluteCounts += ypr[0] * dt_countsPerSwerveRotation;

        // If it's faster to turn to the angle opposite of the angle we were given and drive backwards, do that thing
        if (counts - absoluteCounts > dt_countsPerSwerveRotation / 2)
        {
            counts = dt_countsPerSwerveRotation - counts;
            driveMultipliers[index] = -1;
        } else
        {
            driveMultipliers[index] = 1;
        }

        double difference = counts - absoluteCounts;

        System.out.println("Counts: " + counts + " Abs: " + absoluteCounts);

        wheel.set(wheel.getSelectedSensorPosition(0) + difference > 0 ? 1 : -1);
    }
}

class Wheel extends Sprite
{
    private Vector2f robotCenter;

    private double output = 0;
    private double counts = 0;

    private double delay = 0.005;
    private double last = System.currentTimeMillis() / 1000.0;

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
        if (System.currentTimeMillis() / 1000.0 - last > delay)
        {
            rotate((float) output);
            counts += output / 360 * 4096;
            last = System.currentTimeMillis() / 1000.0;
        }
    }

    double getSelectedSensorPosition(int pidx)
    {
        return counts;
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