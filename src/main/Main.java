package main;

import org.jsfml.graphics.Color;
import org.jsfml.graphics.RenderWindow;
import org.jsfml.graphics.Texture;
import org.jsfml.system.Vector2f;
import org.jsfml.window.Joystick;
import org.jsfml.window.VideoMode;
import org.jsfml.window.event.Event;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class Main
{
    private static Constants mConstants = Constants.getInstance();

    public static void main(String[] args)
    {
        int joystickNumber = 2;

        RenderWindow window = new RenderWindow(new VideoMode(800, 800), "Swerve Drive Visualization");
        window.setFramerateLimit(120);

        Texture arrowTexture = new Texture();
        try
        {
            arrowTexture.loadFromFile(Path.of("src", "resources", "arrow.png"));
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        SwerveModule mTopLeftModule = new SwerveModule(arrowTexture, mConstants.dt_topLeftPosition);
        SwerveModule mTopRightModule = new SwerveModule(arrowTexture, mConstants.dt_topRightPosition);
        SwerveModule mBottomLeftModule = new SwerveModule(arrowTexture, mConstants.dt_bottomLeftPosition);
        SwerveModule mBottomRightModule = new SwerveModule(arrowTexture, mConstants.dt_bottomRightPosition);

        ArrayList<SwerveModule> mSwerveModules = new ArrayList<>();
        mSwerveModules.add(mTopLeftModule);
        mSwerveModules.add(mTopRightModule);
        mSwerveModules.add(mBottomLeftModule);
        mSwerveModules.add(mBottomRightModule);

        double leftX, leftY, rotationMagnitude;

        ArrayList<Vector2f> swerveMovementVectors = new ArrayList<>();

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

            Vector2f movement = new Vector2f((float) leftX, (float) leftY);

            swerveMovementVectors.clear();

                /*
                In these lines we:
                1. Change the signs of the vector representing movement to accommodate its orientation (if it made backwards forwards to save time rotating)
                2. Create a new vector with:
                    a. The x-value as the x-component of the rotation vector
                    b. The y-value as the y-component of the rotation vector
                        i. This is where we use the perpendicular angle of the module; that's what rotates the magnitude and makes it a vector
                3. Add the vectors to get the total vector representing our final movement
                 */
            for (SwerveModule module : mSwerveModules)
            {
                swerveMovementVectors.add(Vector2f.add(new Vector2f(movement.x, movement.y),
                        new Vector2f((float) (rotationMagnitude * Math.cos(module.getPerpendicularAngle())), (float) (-rotationMagnitude * Math.sin(module.getPerpendicularAngle())))));
            }

            // If the largest magnitude is greater than one (which we can't use as a magnitude), set the multiplier to reduce
            // the magnitude of all the vectors by the fraction it takes to reduce the largest magnitude to one
            double largestMagnitude = 0;
            for (Vector2f vector : swerveMovementVectors)
            {
                if (Math.sqrt(Math.pow(vector.x, 2) + Math.pow(vector.y, 2)) > largestMagnitude)
                    largestMagnitude = Math.sqrt(Math.pow(vector.x, 2) + Math.pow(vector.y, 2));
            }

            if (largestMagnitude > 1.0)
            {
                double multiplier = 1 / largestMagnitude;
                for (int i = 0; i < swerveMovementVectors.size(); ++i)
                    swerveMovementVectors.set(i, new Vector2f((float) (swerveMovementVectors.get(i).x * multiplier), (float) (swerveMovementVectors.get(i).y * multiplier)));
            }

            // Converts the angle to degrees for easier understanding. All the other angles were in radians because the Math trig functions use rads
            for (int i = 0; i < mSwerveModules.size(); ++i)
            {
                mSwerveModules.get(i).turnToAngle(Math.atan2(swerveMovementVectors.get(i).y, swerveMovementVectors.get(i).x) * 180 / Math.PI - 90);
                mSwerveModules.get(i).getDriveTalon().set(Math.sqrt(Math.pow(swerveMovementVectors.get(i).x, 2) + Math.pow(swerveMovementVectors.get(i).y, 2)));
            }

            window.clear(Color.BLACK);
            for (SwerveModule module : mSwerveModules)
            {
                module.update();
                window.draw(module);
            }
            window.display();
        }
    }
}
