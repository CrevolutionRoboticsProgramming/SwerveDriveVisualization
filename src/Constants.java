import org.jsfml.system.Vector2f;

public class Constants
{
    private static Constants mInstance = new Constants();

    public static Constants getInstance()
    {
        return mInstance;
    }

    public final int dt_topLeftDrive = 0;
    public final int dt_topRightDrive = 1;
    public final int dt_bottomLeftDrive = 2;
    public final int dt_bottomRightDrive = 3;
    public final int dt_topLeftSwivel = 4;
    public final int dt_topRightSwivel = 5;
    public final int dt_bottomLeftSwivel = 6;
    public final int dt_bottomRightSwivel = 7;

    public final double dt_width = 2.5;
    public final double dt_length = 2.5;
    public final double dt_wheelDiameter = 1.0d / 3.0d;

    public final Vector2f dt_topLeftPosition = new Vector2f(200, 200);//0, 0);
    public final Vector2f dt_topRightPosition = new Vector2f(600, 200);//(float) dt_width, 0);
    public final Vector2f dt_bottomLeftPosition = new Vector2f(200, 600);//0, (float) dt_length);
    public final Vector2f dt_bottomRightPosition = new Vector2f(600, 600);//(float) dt_width, (float) dt_length);

    public final double dt_countsPerSwerveRotation = 4096;

    public final Vector2f robotCenter = new Vector2f(400, 400);//new Vector2f((float) (dt_width / 2), (float) (dt_length / 2));

    public final int talonTimeout = 20;
}
