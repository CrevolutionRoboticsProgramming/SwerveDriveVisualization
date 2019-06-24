# SwerveDriveVisualization

Displays four arrows that represent the wheels of a swerve drive. The magnitude of each arrow is equivalent to the output of its motor. This program works by simulating the encoders mounted on each swivel motor and informing the motor how to turn through its position in relation to its calculated target. While it does not run a position PID loop, it does use its position to calculate which direction it has to rotate.

To use, open the project in a Java editor and add jsfml.jar (from pdinklag's [JSFML](https://github.com/pdinklag/JSFML/releases "JSFML")) as a library. Plug in a controller that acts as an XInput device (Logitech and Xbox 360 controllers work) and run the program.
