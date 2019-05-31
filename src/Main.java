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
    public static void main(String[] args)
    {
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

        double leftX, leftY, rightX;

        while (window.isOpen())
        {
            for(Event event : window.pollEvents())
            {
                if (event.type == Event.Type.CLOSED)
                {
                    window.close();
                }
            }

            leftX = -Joystick.getAxisPosition(0, Joystick.Axis.X) / 100;
            leftY = -Joystick.getAxisPosition(0, Joystick.Axis.Y) / 100;
            rightX = -Joystick.getAxisPosition(0, Joystick.Axis.U) / 100;

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
            wheelVectors.add(Vector2f.add(movement, new Vector2f((float) (rightX * Math.cos(topLeft.getPerpendicularAngle())), (float) (rightX * Math.sin(topLeft.getPerpendicularAngle())))));
            wheelVectors.add(Vector2f.add(movement, new Vector2f((float) (rightX * Math.cos(topRight.getPerpendicularAngle())), (float) (rightX * Math.sin(topRight.getPerpendicularAngle())))));
            wheelVectors.add(Vector2f.add(movement, new Vector2f((float) (rightX * Math.cos(bottomLeft.getPerpendicularAngle())), (float) (rightX * Math.sin(bottomLeft.getPerpendicularAngle())))));
            wheelVectors.add(Vector2f.add(movement, new Vector2f((float) (rightX * Math.cos(bottomRight.getPerpendicularAngle())), (float) (rightX * Math.sin(bottomRight.getPerpendicularAngle())))));

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

            double multiplier;
            if(largestMagnitude > 1)
            {
                multiplier = 1 / largestMagnitude;
            } else
            {
                multiplier = 1;
            }

            /*
            We want to set each vector to a value that is a fraction of the largest vector to normalize them
            The largest vector should be set to one, the others some fraction
             */

            for (int i = 0; i < wheelVectors.size(); ++i)
            {
               wheelVectors.setElementAt(new Vector2f((float) (wheelVectors.elementAt(i).x * multiplier), (float) (wheelVectors.elementAt(i).y * multiplier)), i);
            }

            topLeft.applyRotation(wheelVectors.elementAt(0));
            topRight.applyRotation(wheelVectors.elementAt(1));
            bottomLeft.applyRotation(wheelVectors.elementAt(2));
            bottomRight.applyRotation(wheelVectors.elementAt(3));

            window.clear(Color.BLACK);
            window.draw(topLeft);
            window.draw(topRight);
            window.draw(bottomLeft);
            window.draw(bottomRight);
            window.display();
        }
    }
}

class Wheel extends Sprite
{
    private Vector2f robotCenter;

    public Wheel(Texture texture, Vector2f position, Vector2f robotCenter)
    {
        super(texture);
        setOrigin(new Vector2f(
                getPosition().x + (getGlobalBounds().width / 2),
                getPosition().y + (getGlobalBounds().height / 2)));
        setPosition(position);
        this.robotCenter = robotCenter;
    }

    public void applyRotation(Vector2f total)
    {
        setRotation((float) (Math.atan2(total.y, total.x) * 180 / Math.PI - 90));

        setScale(1, (float) Math.sqrt(Math.pow(total.x, 2) + Math.pow(total.y, 2)));

        if (getScale().y < 0.2)
        {
            setScale(1, 0.2f);
        }
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