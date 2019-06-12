import org.jsfml.graphics.*;
import org.jsfml.system.Vector2f;
import org.jsfml.window.Joystick;
import org.jsfml.window.VideoMode;
import org.jsfml.window.event.Event;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class Main
{
    private static Constants mConstants = Constants.getInstance();
    
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
                turnToAngle(mSwerveModules.get(i), Math.atan2(swerveMovementVectors.get(i).y, swerveMovementVectors.get(i).x) * 180 / Math.PI - 90);
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

    private static void turnToAngle(SwerveModule module, double angle)
    {
        double counts = module.getSwivelTalon().getSelectedSensorPosition(0);

        // This enables field-centric driving. It adds the rotation of the robot to the total counts so it knows where to turn
        //mPigeon.getYawPitchRoll(ypr);
        //counts += (ypr[0] - ((int) (ypr[0] / 360) * 360)) / 360 * mConstants.dt_countsPerSwerveRotation;
        // TO DO: What's the resolution of a pigeon? Find it and replace 360

        // This gives us the counts of the swivel on a scale from 0 to the total counts per rotation
        int rotationsCompleted = (int) (Math.abs(counts) / mConstants.dt_countsPerSwerveRotation);
        counts = Math.abs(counts) - (rotationsCompleted * mConstants.dt_countsPerSwerveRotation);

        // Converts target to ticks and puts it on scale from -360 to 360
        double target = angle / 360 * mConstants.dt_countsPerSwerveRotation;

        // Puts negative counts in positive terms
        if (target < 0) target += mConstants.dt_countsPerSwerveRotation;

        // Puts negative counts in positive terms
        if (counts < 0) counts += mConstants.dt_countsPerSwerveRotation;

        // We had to put the sensor position through Math.abs before, so this fixes absoluteCounts
        if (module.getSwivelTalon().getSelectedSensorPosition(0) < 0)
            counts = mConstants.dt_countsPerSwerveRotation - counts;

        double oppositeAngle = target - (mConstants.dt_countsPerSwerveRotation / 2);
        if (oppositeAngle < 0) oppositeAngle += mConstants.dt_countsPerSwerveRotation;

        HashMap<Case, Double> differences = new HashMap<>();
        differences.put(Case.BEST_CASE, Math.max(counts, target) - Math.min(counts, target));
        differences.put(Case.OVER_GAP, Math.min(counts, target) + (mConstants.dt_countsPerSwerveRotation - Math.max(counts, target)));
        differences.put(Case.TO_OPPOSITE_ANGLE, Math.max(counts, oppositeAngle) - Math.min(counts, oppositeAngle));
        differences.put(Case.TO_OPPOSITE_ANGLE_OVER_GAP, Math.min(counts, oppositeAngle) + (mConstants.dt_countsPerSwerveRotation - Math.max(counts, oppositeAngle)));

        Case smallestDifference = Case.BEST_CASE;
        for (HashMap.Entry<Case, Double> pair : differences.entrySet())
        {
            boolean smallest = true;
            for (HashMap.Entry<Case, Double> comparePair : differences.entrySet())
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

        if (counts > target)
            differences.replace(Case.BEST_CASE, -differences.get(Case.BEST_CASE));
        if (target > counts)
            differences.replace(Case.OVER_GAP, -differences.get(Case.OVER_GAP));
        if (counts > oppositeAngle)
            differences.replace(Case.TO_OPPOSITE_ANGLE, -differences.get(Case.TO_OPPOSITE_ANGLE));
        if (oppositeAngle > counts)
            differences.replace(Case.TO_OPPOSITE_ANGLE_OVER_GAP, -differences.get(Case.TO_OPPOSITE_ANGLE_OVER_GAP));

        if (Math.abs(differences.get(smallestDifference)) < 8)
        {
            module.getSwivelTalon().set(0);
            module.setStopped(true);
            if (!module.getLastStopped() && (smallestDifference == Case.TO_OPPOSITE_ANGLE || smallestDifference == Case.TO_OPPOSITE_ANGLE_OVER_GAP))
            {
                module.getSwivelTalon().setInverted(!module.getSwivelTalon().getInverted());
                module.getDriveTalon().setInverted(!module.getDriveTalon().getInverted());

                module.getSwivelTalon().setSelectedSensorPosition((int) (module.getSwivelTalon().getSelectedSensorPosition(0) - (mConstants.dt_countsPerSwerveRotation / 2)));
            }
        } else
        {
            module.getSwivelTalon().set(ControlMode.Position, module.getSwivelTalon().getSelectedSensorPosition(0) + differences.get(smallestDifference));
            module.setStopped(false);
        }

        module.setLastStopped(module.isStopped());
        module.setLastTarget(target);
    }

    static class WPI_TalonSRX
    {
        public double output = 0;
        public double counts = 0;

        private boolean isInverted = false;

        void set(ControlMode mode, double output)
        {
            if (mode == ControlMode.PercentOutput)
            {
                this.output = output;
                if (isInverted)
                    this.output = -this.output;
            } else if (mode == ControlMode.Position)
            {
                if (output - counts != 0)
                    this.output = output - counts > 0 ? 1 : -1;
                else
                    this.output = 0;
            }
        }

        void set(double output)
        {
            set(ControlMode.PercentOutput, output);
        }

        void setSelectedSensorPosition(int pos)
        {
            counts = pos;
        }

        double getSelectedSensorPosition(int pidx)
        {
            return counts;
        }

        public boolean getInverted()
        {
            return isInverted;
        }

        public void setInverted(boolean inverted)
        {
            isInverted = inverted;
        }
    }

    static class SwerveModule extends Sprite
    {
        private WPI_TalonSRX mDriveTalon, mSwivelTalon;
        private boolean mStopped = false;
        private boolean mLastStopped = false;
        private double mLastTarget = 0.0;
        private double mPerpendicularAngle = 0.0;

        public SwerveModule(Texture texture, Vector2f position)
        {
            super(texture);
            setOrigin(new Vector2f(
                    getPosition().x + (getGlobalBounds().width / 2),
                    getPosition().y + (getGlobalBounds().height / 2)));
            setPosition(position);
            mPerpendicularAngle = Math.atan2(Vector2f.sub(mConstants.robotCenter, getPosition()).y, Vector2f.sub(mConstants.robotCenter, getPosition()).x) - (Math.PI / 2);

            mDriveTalon = new WPI_TalonSRX();
            mSwivelTalon = new WPI_TalonSRX();
        }

        public WPI_TalonSRX getDriveTalon()
        {
            return mDriveTalon;
        }

        public WPI_TalonSRX getSwivelTalon()
        {
            return mSwivelTalon;
        }

        public double getPerpendicularAngle()
        {
            return mPerpendicularAngle;
        }

        public boolean isStopped()
        {
            return mStopped;
        }

        public void setStopped(boolean stopped)
        {
            mStopped = stopped;
        }

        public boolean getLastStopped()
        {
            return mLastStopped;
        }

        public void setLastStopped(boolean lastStopped)
        {
            mLastStopped = lastStopped;
        }

        public double getLastTarget()
        {
            return mLastTarget;
        }

        public void setLastTarget(double lastTarget)
        {
            mLastTarget = lastTarget;
        }


        void update()
        {
            rotate((float) -mSwivelTalon.output);
            mSwivelTalon.counts += mSwivelTalon.output / 360 * 4096;

            setScale(1, (float) (Math.abs(mDriveTalon.output) > 0.2 ? mDriveTalon.output : 0.2));
        }
    }

    public enum ControlMode
    {
        PercentOutput,
        Position
    }

    public enum Case
    {
        BEST_CASE,
        OVER_GAP,
        TO_OPPOSITE_ANGLE,
        TO_OPPOSITE_ANGLE_OVER_GAP,
    }
}