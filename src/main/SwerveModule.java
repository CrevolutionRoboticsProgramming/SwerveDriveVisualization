package main;

import org.jsfml.graphics.Sprite;
import org.jsfml.graphics.Texture;
import org.jsfml.system.Vector2f;

import java.util.HashMap;

enum Case
{
    BEST_CASE,
    OVER_GAP,
    TO_OPPOSITE_ANGLE,
    TO_OPPOSITE_ANGLE_OVER_GAP,
}

public class SwerveModule extends Sprite
{
    private Constants mConstants = Constants.getInstance();
    private WPI_TalonSRX mDriveTalon, mSwivelTalon;
    private boolean mStopped = false;
    private boolean mLastStopped = false;
    private double mLastTarget = 0.0;
    private double mPerpendicularAngle;

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

    // We put both the current counts and the target counts on an absolute scale because
    //      it's easier to visualize in my opinion. We could do the same process with
    //      a continuous model, but the same problems with the transition over 0 degrees
    //      with a mismatched current position and target (like 4095 and 5) and calculating
    //      when to move to the opposite angle and reverse still arise.
    public void turnToAngle(double angle)
    {
        double counts = getSwivelTalon().getSelectedSensorPosition(0);

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
        if (getSwivelTalon().getSelectedSensorPosition(0) < 0)
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
            getSwivelTalon().set(0);
            setStopped(true);
            if ((target != getLastTarget() || !getLastStopped()) && (smallestDifference == Case.TO_OPPOSITE_ANGLE || smallestDifference == Case.TO_OPPOSITE_ANGLE_OVER_GAP))
            {
                getSwivelTalon().setInverted(!getSwivelTalon().getInverted());
                getDriveTalon().setInverted(!getDriveTalon().getInverted());

                getSwivelTalon().setSelectedSensorPosition((int) (getSwivelTalon().getSelectedSensorPosition(0) - (mConstants.dt_countsPerSwerveRotation / 2)));
            }
        } else
        {
            getSwivelTalon().set(ControlMode.Position, getSwivelTalon().getSelectedSensorPosition(0) + differences.get(smallestDifference));
            setStopped(false);
        }

        setLastStopped(isStopped());
        setLastTarget(target);
    }

    void update()
    {
        rotate((float) -mSwivelTalon.output);
        mSwivelTalon.counts += mSwivelTalon.output / 360 * 4096;

        setScale(1, (float) (Math.abs(mDriveTalon.output) > 0.2 ? mDriveTalon.output : 0.2));
    }
}