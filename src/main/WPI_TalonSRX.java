package main;

enum ControlMode
{
    PercentOutput,
    Position
}

public class WPI_TalonSRX
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