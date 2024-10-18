/* Copyright (c) 2021 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//package org.firstinspires.ftc.robotcontroller.external.samples;
package org.firstinspires.ftc.team23442;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

/*
 * This file contains an example of a Linear "OpMode".
 * An OpMode is a 'program' that runs in either the autonomous or the teleop period of an FTC match.
 * The names of OpModes appear on the menu of the FTC Driver Station.
 * When a selection is made from the menu, the corresponding OpMode is executed.
 *
 * This particular OpMode illustrates driving a 4-motor Omni-Directional (or Holonomic) robot.
 * This code will work with either a Mecanum-Drive or an X-Drive train.
 * Both of these drives are illustrated at https://gm0.org/en/latest/docs/robot-design/drivetrains/holonomic.html
 * Note that a Mecanum drive must display an X roller-pattern when viewed from above.
 *
 * Also note that it is critical to set the correct rotation direction for each motor.  See details below.
 *
 * Teleonomic drives provide the ability for the robot to move in three axes (directions) simultaneously.
 * Each motion axis is controlled by one Joystick axis.
 *
 * 1) Axial:    Driving forward and backward               Left-joystick Forward/Backward
 * 2) Lateral:  Strafing right and left                     Left-joystick Right and Left
 * 3) Yaw:      Rotating Clockwise and counter clockwise    Right-joystick Right and Left
 *
 * This code is written assuming that the right-side motors need to be reversed for the robot to drive forward.
 * When you first test your robot, if it moves backward when you push the left stick forward, then you must flip
 * the direction of all 4 motors (see code below).
 *
 * Use Android Studio to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this OpMode to the Driver Station OpMode list
 */

@TeleOp(name="TeleOpTest")
public class TeleOpTest extends LinearOpMode {

    // Declare OpMode members for each of the 4 motors.
    private ElapsedTime runtime = new ElapsedTime();
    private DcMotor leftFrontDrive = null;
    private DcMotor leftBackDrive = null;
    private DcMotor rightFrontDrive = null;
    private DcMotor rightBackDrive = null;
    private DcMotorEx  armMotor = null; //the arm motor
    private CRServo intake = null; //the active intake servo
    private Servo wrist = null; //the wrist servo

    @Override
    public void runOpMode() {

        // Initialize the hardware variables. Note that the strings used here must correspond
        // to the names assigned during the robot configuration step on the DS or RC devices.
        leftFrontDrive  = hardwareMap.get(DcMotor.class, "left_front_drive");
        leftBackDrive  = hardwareMap.get(DcMotor.class, "left_back_drive");
        rightFrontDrive = hardwareMap.get(DcMotor.class, "right_front_drive");
        rightBackDrive = hardwareMap.get(DcMotor.class, "right_back_drive");
        armMotor   = hardwareMap.get(DcMotorEx.class, "left_arm"); //the arm motor

            /* This constant is the number of encoder ticks for each degree of rotation of the arm.
    To find this, we first need to consider the total gear reduction powering our arm.
    First, we have an external 20t:100t (5:1) reduction created by two spur gears.
    But we also have an internal gear reduction in our motor.
    The motor we use for this arm is a 117RPM Yellow Jacket. Which has an internal gear
    reduction of ~50.9:1. (more precisely it is 250047/4913:1)
    We can multiply these two ratios together to get our final reduction of ~254.47:1.
    The motor's encoder counts 28 times per rotation. So in total you should see about 7125.16
    counts per rotation of the arm. We divide that by 360 to get the counts per degree. */
        final double ARM_TICKS_PER_DEGREE =
                28 // number of encoder ticks per rotation of the bare motor
                        * 250047.0 / 4913.0 // This is the exact gear ratio of the 50.9:1 Yellow Jacket gearbox
                        * 100.0 / 20.0 // This is the external gear reduction, a 20T pinion gear that drives a 100T hub-mount gear
                        * 1/360.0; // we want ticks per degree, not per rotation    /* These constants hold the position that the arm is commanded to run to.

        /*
         These are relative to where the arm was located when you start the OpMode. So make sure the
        arm is reset to collapsed inside the robot before you start the program.

                In these variables you'll see a number in degrees, multiplied by the ticks per degree of the arm.
        This results in the number of encoder ticks the arm needs to move in order to achieve the ideal
        set position of the arm. For example, the ARM_SCORE_SAMPLE_IN_LOW is set to
        160 * ARM_TICKS_PER_DEGREE. This asks the arm to move 160° from the starting position.
        If you'd like it to move further, increase that number. If you'd like it to not move
        as far from the starting position, decrease it. */

        final double ARM_COLLAPSED_INTO_ROBOT  = 90;
        final double ARM_COLLECT               = 250 * ARM_TICKS_PER_DEGREE;
        final double ARM_CLEAR_BARRIER         = 230 * ARM_TICKS_PER_DEGREE;
        final double ARM_SCORE_SPECIMEN        = 160 * ARM_TICKS_PER_DEGREE;
        final double ARM_SCORE_SAMPLE_IN_LOW   = 160 * ARM_TICKS_PER_DEGREE;
        final double ARM_ATTACH_HANGING_HOOK   = 120 * ARM_TICKS_PER_DEGREE;
        final double ARM_WINCH_ROBOT           = 15  * ARM_TICKS_PER_DEGREE;

        /* Variables to store the speed the intake servo should be set at to intake, and deposit game elements. */
        final double INTAKE_COLLECT    = -1.0;
        final double INTAKE_OFF        =  0.0;
        final double INTAKE_DEPOSIT    =  0.5;

        /* Variables to store the positions that the wrist should be set to when folding in, or folding out. */
        final double WRIST_FOLDED_IN   = 0.8333;
        final double WRIST_FOLDED_OUT  = 0.5;

        /* A number in degrees that the triggers can adjust the arm position by */
        final double FUDGE_FACTOR = 15 * ARM_TICKS_PER_DEGREE;

        /* Variables that are used to set the arm to a specific position */
        double armPosition = (int)ARM_COLLAPSED_INTO_ROBOT;
        double armPositionFudgeFactor;

        /* Setting zeroPowerBehavior to BRAKE enables a "brake mode". This causes the motor to slow down
        much faster when it is coasting. This creates a much more controllable drivetrain. As the robot
        stops much quicker. */
        armMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        /*This sets the maximum current that the control hub will apply to the arm before throwing a flag */
        ((DcMotorEx) armMotor).setCurrentAlert(5, CurrentUnit.AMPS);

        /* Before starting the armMotor. We'll make sure the TargetPosition is set to 0.
        Then we'll set the RunMode to RUN_TO_POSITION. And we'll ask it to stop and reset encoder.
        If you do not have the encoder plugged into this motor, it will not run in this code. */
        armMotor.setTargetPosition(0);
        armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        armMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        /* Define and initialize servos.*/
        intake = hardwareMap.get(CRServo.class, "intake");
        wrist  = hardwareMap.get(Servo.class, "wrist");

        /* Make sure that the intake is off, and the wrist is folded in. */
        intake.setPower(INTAKE_OFF);
        wrist.setPosition(WRIST_FOLDED_IN);

        /* Send telemetry message to signify robot waiting */
        telemetry.addLine("Robot Ready.");
        telemetry.update();


        // ########################################################################################
        // !!!            IMPORTANT Drive Information. Test your motor directions.            !!!!!
        // ########################################################################################
        // Most robots need the motors on one side to be reversed to drive forward.
        // The motor reversals shown here are for a "direct drive" robot (the wheels turn the same direction as the motor shaft)
        // If your robot has additional gear reductions or uses a right-angled drive, it's important to ensure
        // that your motors are turning in the correct direction.  So, start out with the reversals here, BUT
        // when you first test your robot, push the left joystick forward and observe the direction the wheels turn.
        // Reverse the direction (flip FORWARD <-> REVERSE ) of any wheel that runs backward
        // Keep testing until ALL the wheels move the robot forward when you push the left joystick forward.
        leftFrontDrive.setDirection(DcMotor.Direction.REVERSE);
        leftBackDrive.setDirection(DcMotor.Direction.FORWARD);
        rightFrontDrive.setDirection(DcMotor.Direction.FORWARD);
        rightBackDrive.setDirection(DcMotor.Direction.FORWARD);

        // Wait for the game to start (driver presses START)
        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();
        runtime.reset();

        // run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {
            double max;

            // POV Mode uses left joystick to go forward & strafe, and right joystick to rotate.
            double axial   = -gamepad1.left_stick_y;  // Note: pushing stick forward gives negative value
            double lateral =  gamepad1.left_stick_x;
            double yaw     =  gamepad1.right_stick_x;

            // Combine the joystick requests for each axis-motion to determine each wheel's power.
            // Set up a variable for each drive wheel to save the power level for telemetry.
            double leftFrontPower  = axial + lateral + yaw;
            double rightFrontPower = axial - lateral - yaw;
            double leftBackPower   = axial - lateral + yaw;
            double rightBackPower  = axial + lateral - yaw;

            // Normalize the values so no wheel power exceeds 100%
            // This ensures that the robot maintains the desired motion.
            max = Math.max(Math.abs(leftFrontPower), Math.abs(rightFrontPower));
            max = Math.max(max, Math.abs(leftBackPower));
            max = Math.max(max, Math.abs(rightBackPower));

            if (max > 1.0) {
                leftFrontPower  /= max;
                rightFrontPower /= max;
                leftBackPower   /= max;
                rightBackPower  /= max;
            }

            // This is test code:
            //
            // Uncomment the following code to test your motor directions.
            // Each button should make the corresponding motor run FORWARD.
            //   1) First get all the motors to take to correct positions on the robot
            //      by adjusting your Robot Configuration if necessary.
            //   2) Then make sure they run in the correct direction by modifying the
            //      the setDirection() calls above.
            // Once the correct motors move in the correct direction re-comment this code.

            /*
            leftFrontPower  = gamepad1.x ? 1.0 : 0.0;  // X gamepad
            leftBackPower   = gamepad1.a ? 1.0 : 0.0;  // A gamepad
            rightFrontPower = gamepad1.y ? 1.0 : 0.0;  // Y gamepad
            rightBackPower  = gamepad1.b ? 1.0 : 0.0;  // B gamepad
            */

            // Send calculated power to wheels
            leftFrontDrive.setPower(leftFrontPower);
            rightFrontDrive.setPower(rightFrontPower);
            leftBackDrive.setPower(leftBackPower);
            rightBackDrive.setPower(rightBackPower);

            // Show the elapsed game time and wheel power.
            telemetry.addData("Status", "Run Time: " + runtime.toString());
            telemetry.addData("Front left/Right", "%4.2f, %4.2f", leftFrontPower, rightFrontPower);
            telemetry.addData("Back  left/Right", "%4.2f, %4.2f", leftBackPower, rightBackPower);
            telemetry.update();


            /* Here we handle the three buttons that have direct control of the intake speed.
            These control the continuous rotation servo that pulls elements into the robot,
            If the user presses A, it sets the intake power to the final variable that
            holds the speed we want to collect at.
            If the user presses X, it sets the servo to Off.
            And if the user presses B it reveres the servo to spit out the element.*/

            /* TECH TIP: If Else statements:
            We're using an else if statement on "gamepad1.x" and "gamepad1.b" just in case
            multiple buttons are pressed at the same time. If the driver presses both "a" and "x"
            at the same time. "a" will win over and the intake will turn on. If we just had
            three if statements, then it will set the intake servo's power to multiple speeds in
            one cycle. Which can cause strange behavior. */

            if (gamepad1.a) {
                intake.setPower(INTAKE_COLLECT);

            }
            else if (gamepad1.x) {
                intake.setPower(INTAKE_OFF);
            }
            else if (gamepad1.b) {
                intake.setPower(INTAKE_DEPOSIT);
            }

            if(gamepad1.right_bumper){
                wrist.setPosition(WRIST_FOLDED_OUT);
            }else{
                wrist.setPosition(WRIST_FOLDED_IN);
            }

            /* Here we implement a set of if else statements to set our arm to different scoring positions.
            We check to see if a specific button is pressed, and then move the arm (and sometimes
            intake and wrist) to match. For example, if we click the right bumper we want the robot
            to start collecting. So it moves the armPosition to the ARM_COLLECT position,
            it folds out the wrist to make sure it is in the correct orientation to intake, and it
            turns the intake on to the COLLECT mode.*/
            if(gamepad1.right_bumper){
                /* This is the intaking/collecting arm position */
                armPosition = ARM_COLLECT;
                wrist.setPosition(WRIST_FOLDED_OUT);
                intake.setPower(INTAKE_COLLECT);
            }

            else if (gamepad1.left_bumper){
                    /* This is about 20° up from the collecting position to clear the barrier
                    Note here that we don't set the wrist position or the intake power when we
                    select this "mode", this means that the intake and wrist will continue what
                    they were doing before we clicked left bumper. */
                armPosition = ARM_CLEAR_BARRIER;
            }

            else if (gamepad1.y){
                /* This is the correct height to score the sample in the LOW BASKET */
                armPosition = ARM_SCORE_SAMPLE_IN_LOW;
            }

            else if (gamepad1.dpad_left) {
                    /* This turns off the intake, folds in the wrist, and moves the arm
                    back to folded inside the robot. This is also the starting configuration */
                armPosition = ARM_COLLAPSED_INTO_ROBOT;
                intake.setPower(INTAKE_OFF);
                //wrist.setPosition(WRIST_FOLDED_IN);
            }

            else if (gamepad1.dpad_right){
                /* This is the correct height to score SPECIMEN on the HIGH CHAMBER */
                armPosition = ARM_SCORE_SPECIMEN;
                //wrist.setPosition(WRIST_FOLDED_IN);
            }

            else if (gamepad1.dpad_up){
                /* This sets the arm to vertical to hook onto the LOW RUNG for hanging */
                armPosition = ARM_ATTACH_HANGING_HOOK;
                intake.setPower(INTAKE_OFF);
                //wrist.setPosition(WRIST_FOLDED_IN);
            }

            else if (gamepad1.dpad_down){
                /* this moves the arm down to lift the robot up once it has been hooked */
                armPosition = ARM_WINCH_ROBOT;
                intake.setPower(INTAKE_OFF);
               // wrist.setPosition(WRIST_FOLDED_IN);

            }



            /* Here we create a "fudge factor" for the arm position.
            This allows you to adjust (or "fudge") the arm position slightly with the gamepad triggers.
            We want the left trigger to move the arm up, and right trigger to move the arm down.
            So we add the right trigger's variable to the inverse of the left trigger. If you pull
            both triggers an equal amount, they cancel and leave the arm at zero. But if one is larger
            than the other, it "wins out". This variable is then multiplied by our FUDGE_FACTOR.
            The FUDGE_FACTOR is the number of degrees that we can adjust the arm by with this function. */
            armPositionFudgeFactor = FUDGE_FACTOR * (gamepad1.right_trigger + (-gamepad1.left_trigger));


            //telemetry.addData("armPosition", armPosition);
            //telemetry.addData("armPositionFudgeFactor", armPositionFudgeFactor);

            /* Here we set the target position of our arm to match the variable that was selected
            by the driver.
            We also set the target velocity (speed) the motor runs at, and use setMode to run it.*/
            armMotor.setTargetPosition((int) (armPosition + armPositionFudgeFactor));

            ((DcMotorEx) armMotor).setVelocity(85);
            armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                   /* TECH TIP: Encoders, integers, and doubles
            Encoders report when the motor has moved a specified angle. They send out pulses which
            only occur at specific intervals (see our ARM_TICKS_PER_DEGREE). This means that the
            position our arm is currently at can be expressed as a whole number of encoder "ticks".
            The encoder will never report a partial number of ticks. So we can store the position in
            an integer (or int).
            A lot of the variables we use in FTC are doubles. These can capture fractions of whole
            numbers. Which is great when we want our arm to move to 122.5°, or we want to set our
            servo power to 0.5.

            setTargetPosition is expecting a number of encoder ticks to drive to. Since encoder
            ticks are always whole numbers, it expects an int. But we want to think about our
            arm position in degrees. And we'd like to be able to set it to fractions of a degree.
            So we make our arm positions Doubles. This allows us to precisely multiply together
            armPosition and our armPositionFudgeFactor. But once we're done multiplying these
            variables. We can decide which exact encoder tick we want our motor to go to. We do
            this by "typecasting" our double, into an int. This takes our fractional double and
            rounds it to the nearest whole number.
            */

            /* Check to see if our arm is over the current limit, and report via telemetry. */
            if (((DcMotorEx) armMotor).isOverCurrent()){
                telemetry.addLine("MOTOR EXCEEDED CURRENT LIMIT!");
            }


            /* send telemetry to the driver of the arm's current position and target position */
            telemetry.addData("armTarget: ", armMotor.getTargetPosition());
            telemetry.addData("arm Encoder: ", armMotor.getCurrentPosition());
            telemetry.update();
        }
    }}
