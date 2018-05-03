package edu.rosehulman.chenj4.integratedimagerec;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import edu.rosehulman.me435.NavUtils;

public class GolfBallDeliveryActivity extends ImageRecActivity {

    /** Constant used with logging that you'll see later. */
    public static final String TAG = "GolfBallDelivery";

    public State mState;
    public enum State {
        READY_FOR_MISSION, NEAR_BALL_SCRIPT, DRIVING_TOWARD_FAR_BALL, FAR_BALL_SCRIPT, DRIVE_TOWARD_HOME, WAIT_FOR_PICKUP, SEEKING_HOME
    }


    /**
     * An enum used for variables when a ball color needs to be referenced.
     */
    public enum BallColor {
        NONE, BLUE, RED, YELLOW, GREEN, BLACK, WHITE
    }

    /**
     * An array (of size 3) that stores what color is present in each golf ball stand location.
     */
    public BallColor[] mLocationColors = new BallColor[]{BallColor.NONE, BallColor.NONE, BallColor.NONE};

    /**
     * Simple boolean that is updated when the Team button is pressed to switch teams.
     */
    public boolean mOnRedTeam = false;


    // ---------------------- UI References ----------------------
    /**
     * An array (of size 3) that keeps a reference to the 3 balls displayed on the UI.
     */
    private ImageButton[] mBallImageButtons;

    /**
     * References to the buttons on the UI that can change color.
     */
    private Button mTeamChangeButton, mGoOrMissionCompleteButton;

    /**
     * An array constants (of size 7) that keeps a reference to the different ball color images resources.
     */
    // Note, the order is important and must be the same throughout the app.
    private static final int[] BALL_DRAWABLE_RESOURCES = new int[]{R.drawable.none_ball, R.drawable.blue_ball,
            R.drawable.red_ball, R.drawable.yellow_ball, R.drawable.green_ball, R.drawable.black_ball, R.drawable.white_ball};

    /**
     * TextViews that can change values.
     */
    private TextView mCurrentStateTextView, mStateTimeTextView, mGpsInfoTextView, mSensorOrientationTextView,
            mGuessXYTextView, mLeftDutyCycleTextView, mRightDutyCycleTextView, mMatchTimeTextView;

    private TextView mJumboXTextView, mJumboYTextView, mGoOrCompleteJumboButton;



    protected LinearLayout mJumboLinearLayout;

    // ---------------------- End of UI References ----------------------


    // ---------------------- Mission strategy values ----------------------
    /** Constants for the known locations. */
    public static final long NEAR_BALL_GPS_X = 90;
    public static final long FAR_BALL_GPS_X = 240;


    /** Variables that will be either 50 or -50 depending on the balls we get. */
    private double mNearBallGpsY, mFarBallGpsY;

    /**
     * If that ball is present the values will be 1, 2, or 3.
     * If not present the value will be 0.
     * For example if we have the black ball, then mWhiteBallLocation will equal 0.
     */
    public int mNearBallLocation, mFarBallLocation, mWhiteBallLocation;
    // ----------------- End of mission strategy values ----------------------


    // ---------------------------- Timing area ------------------------------
    /**
     * Time when the state began (saved as the number of millisecond since epoch).
     */
    private long mStateStartTime;

    /**
     * Time when the match began, ie when Go! was pressed (saved as the number of millisecond since epoch).
     */
    private long mMatchStartTime;

    /**
     * Constant that holds the maximum length of the match (saved in milliseconds).
     */
    private long MATCH_LENGTH_MS = 300000; // 5 minutes in milliseconds (5 * 60 * 1000)
    // ----------------------- End of timing area --------------------------------


    // ---------------------------- Driving area ---------------------------------
    /**
     * When driving towards a target, using a seek strategy, consider that state a success when the
     * GPS distance to the target is less than (or equal to) this value.
     */
    public static final double ACCEPTED_DISTANCE_AWAY_FT = 10.0; // Within 10 feet is close enough.

    /**
     * Multiplier used during seeking to calculate a PWM value based on the turn amount needed.
     */
    private static final double SEEKING_DUTY_CYCLE_PER_ANGLE_OFF_MULTIPLIER = 3.0;  // units are (PWM value)/degrees

    /**
     * Variable used to cap the slowest PWM duty cycle used while seeking. Pick a value from -255 to 255.
     */
    private static final int LOWEST_DESIRABLE_SEEKING_DUTY_CYCLE = 150;

    /**
     * PWM duty cycle values used with the drive straight dialog that make your robot drive straightest.
     */
    public int mLeftStraightPwmValue = 255, mRightStraightPwmValue = 255;
    // ------------------------ End of Driving area ------------------------------

    private Scripts mScripts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //the topmost Activity should load this line
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mBallImageButtons = new ImageButton[]{(ImageButton) findViewById(R.id.location_1_image_button),
                (ImageButton) findViewById(R.id.location_2_image_button),
                (ImageButton) findViewById(R.id.location_3_image_button)};
        mTeamChangeButton = (Button) findViewById(R.id.team_change_button);
        mCurrentStateTextView = (TextView) findViewById(R.id.current_state_textview);
        mStateTimeTextView = (TextView) findViewById(R.id.state_time_textview);
        mGpsInfoTextView = (TextView) findViewById(R.id.gps_info_textview);
        mSensorOrientationTextView = (TextView) findViewById(R.id.orientation_textview);
        mGuessXYTextView = (TextView) findViewById(R.id.guess_location_textview);
        mLeftDutyCycleTextView = (TextView) findViewById(R.id.left_duty_cycle_textview);
        mRightDutyCycleTextView = (TextView) findViewById(R.id.right_duty_cycle_textview);
        mMatchTimeTextView = (TextView) findViewById(R.id.match_time_textview);
        mGoOrMissionCompleteButton = (Button) findViewById(R.id.go_or_mission_complete_button);
        mGoOrCompleteJumboButton = (Button)findViewById(R.id.go_or_complete_jumbo_button);
        mJumboLinearLayout = findViewById(R.id.jumboLinearLayout);

        mJumboXTextView = findViewById(R.id.jumbo_x);
        mJumboYTextView = findViewById(R.id.jumbo_y);



        // When you start using the real hardware you don't need test buttons.
        boolean hideFakeGpsButtons = false;
        if (hideFakeGpsButtons) {
            TableLayout fakeGpsButtonTable = (TableLayout) findViewById(R.id.fake_gps_button_table);
            fakeGpsButtonTable.setVisibility(View.GONE);
        }
        mScripts = new Scripts(this);
        setLocationToColor(1, BallColor.RED);
        setLocationToColor(2, BallColor.WHITE);
        setLocationToColor(3, BallColor.BLUE);
        setState(State.READY_FOR_MISSION);

    }

    public void setState(State newState) {
        if (mState == State.READY_FOR_MISSION && newState!= State.NEAR_BALL_SCRIPT){
            return;
        }
        mStateStartTime = System.currentTimeMillis();
        mCurrentStateTextView.setText(newState.name());
        speak(newState.name().replace("_", " ").toLowerCase());
        switch (newState){

            case READY_FOR_MISSION:
                mGoOrMissionCompleteButton.setBackgroundResource(R.drawable.green_button);
                mGoOrMissionCompleteButton.setText("Go");
                mGoOrCompleteJumboButton.setBackgroundResource(R.drawable.green_button);
                mGoOrCompleteJumboButton.setText("Go");
                sendWheelSpeed(0,0);
                break;
            case NEAR_BALL_SCRIPT:
                mGpsInfoTextView.setText("---");
                mGuessXYTextView.setText("---");
                mScripts.nearBallScript();
                mViewFlipper.setDisplayedChild(2);
                break;
            case DRIVING_TOWARD_FAR_BALL:
                break;
            case FAR_BALL_SCRIPT:
                mScripts.farBallScript();
                break;
            case DRIVE_TOWARD_HOME:
                break;
            case WAIT_FOR_PICKUP:
                sendWheelSpeed(0,0);
                break;
            case SEEKING_HOME:
                break;
        }
        mState = newState;
    }


    /**
     * Use this helper method to set the color of a ball.
     * The location value here is 1 based.  Send 1, 2, or 3
     * Side effect: Updates the UI with the appropriate ball color resource image.
     */
    public void setLocationToColor(int location, BallColor ballColor) {
        mBallImageButtons[location - 1].setImageResource(BALL_DRAWABLE_RESOURCES[ballColor.ordinal()]);
        mLocationColors[location - 1] = ballColor;
    }

    /**
     * Used to get the state time in milliseconds.
     */
    private long getStateTimeMs() {
        return System.currentTimeMillis() - mStateStartTime;
    }

    /**
     * Used to get the match time in milliseconds.
     */
    private long getMatchTimeMs() {
        return System.currentTimeMillis() - mMatchStartTime;
    }


    // --------------------------- Methods added ---------------------------

    @Override
    public void loop() {
        super.loop();
        Log.d(TAG, "this is loop within out subclass of Robot Activity");
        mStateTimeTextView.setText(""+getStateTimeMs() / 1000);
        mGuessXYTextView.setText("(" + (int)mGuessX + ", " + (int)mGuessY + ")");

//        mJumboYTextView.setText(""+(int)mCurrentGpsX);
//        mJumboYTextView.setText("" + (int)mCurrentGpsY);

        mJumboXTextView.setText(""+ (int)mGuessX);
        mJumboYTextView.setText(""+(int)mGuessY);

        if (mCurrentGpsHeading != NO_HEADING){
            mJumboLinearLayout.setBackgroundColor(Color.GREEN);
        }else{
            mJumboLinearLayout.setBackgroundColor(Color.GRAY);
        }

        long timeRemainingSeconds = MATCH_LENGTH_MS / 1000;
        if (mState != State.READY_FOR_MISSION){
            timeRemainingSeconds = (MATCH_LENGTH_MS - getMatchTimeMs())/ 1000;
            if (getMatchTimeMs() > MATCH_LENGTH_MS){
                setState(State.READY_FOR_MISSION);
            }
        }
        mMatchTimeTextView.setText(getString(R.string.time_format,timeRemainingSeconds / 60, timeRemainingSeconds % 60));

        switch (mState){

            case READY_FOR_MISSION:
                break;
            case NEAR_BALL_SCRIPT:
                break;
            case DRIVING_TOWARD_FAR_BALL:
                //Todo Drive towards farball_gpsx, mFarball_gps_Y
                seekTargetAt(FAR_BALL_GPS_X,mFarBallGpsY);
                break;
            case FAR_BALL_SCRIPT:
                break;
            case DRIVE_TOWARD_HOME:
                //Todo Drive towards 0,0
                seekTargetAt(0,0);
                break;
            case WAIT_FOR_PICKUP:
                if (getStateTimeMs() > 8000){
                    setState(State.SEEKING_HOME);
                }
                break;
            case SEEKING_HOME:
                seekTargetAt(0,0);
                //Todo Drive towards 0,0
                if (getStateTimeMs() > 8000){
                    setState(State.WAIT_FOR_PICKUP);
                }
                break;
        }
    }

    private void seekTargetAt(double x, double y) {
        int leftDutyCycle = mLeftStraightPwmValue;
        int rightDutyCycle = mRightStraightPwmValue;
        double targetHeading = NavUtils.getTargetHeading(mGuessX, mGuessY, x, y);
        double leftTurnAmount = NavUtils.getLeftTurnHeadingDelta(mCurrentSensorHeading, targetHeading);
        double rightTurnAmount = NavUtils.getRightTurnHeadingDelta(mCurrentSensorHeading, targetHeading);
        if (leftTurnAmount < rightTurnAmount) {
            leftDutyCycle = mLeftStraightPwmValue - (int)(leftTurnAmount * SEEKING_DUTY_CYCLE_PER_ANGLE_OFF_MULTIPLIER);
            leftDutyCycle = Math.max(leftDutyCycle, LOWEST_DESIRABLE_SEEKING_DUTY_CYCLE);
        } else {
            rightDutyCycle = mRightStraightPwmValue - (int)(rightTurnAmount * SEEKING_DUTY_CYCLE_PER_ANGLE_OFF_MULTIPLIER);
            rightDutyCycle = Math.max(rightDutyCycle, LOWEST_DESIRABLE_SEEKING_DUTY_CYCLE);
        }
        sendWheelSpeed(leftDutyCycle, rightDutyCycle);
    }


    // --------------------------- Drive command ---------------------------

    @Override
    public void sendWheelSpeed(int leftDutyCycle, int rightDutyCycle) {
        super.sendWheelSpeed(leftDutyCycle, rightDutyCycle);
        mLeftDutyCycleTextView.setText("Left\n" + leftDutyCycle);
        mRightDutyCycleTextView.setText("Rgiht\n" + rightDutyCycle);
    }


    // --------------------------- Sensor listeners ---------------------------

    @Override
    public void onLocationChanged(double x, double y, double heading, Location location) {
        super.onLocationChanged(x, y, heading, location);
        String gpsInfo = getString(R.string.xy_format, mCurrentGpsX, mCurrentGpsY);
        if (mCurrentGpsHeading != NO_HEADING){
            gpsInfo += " " + getString(R.string.degrees_format, mCurrentGpsHeading);

        }else{
            gpsInfo += "?";
        }

        if (mCurrentGpsHeading != NO_HEADING){
            mJumboLinearLayout.setBackgroundColor(Color.GREEN);
        }else{
            mJumboLinearLayout.setBackgroundColor(Color.GRAY);
        }

        gpsInfo += "  " + mGpsCounter;
        mGpsInfoTextView.setText(gpsInfo);

        if (mState == State.DRIVING_TOWARD_FAR_BALL) {
            double distanceFromTarget = NavUtils.getDistance(mCurrentGpsX, mCurrentGpsY,
                    FAR_BALL_GPS_X, mFarBallGpsY);
            if (distanceFromTarget < ACCEPTED_DISTANCE_AWAY_FT) {
                setState(State.FAR_BALL_SCRIPT);
            }
        }
        if (mState == State.DRIVE_TOWARD_HOME) {
            // Shorter to write since the RobotActivity already calculates the distance to 0, 0.
            if (mCurrentGpsDistance < ACCEPTED_DISTANCE_AWAY_FT) {
                setState(State.WAIT_FOR_PICKUP);
            }
        }


    }

    @Override
    public void onSensorChanged(double fieldHeading, float[] orientationValues) {
        super.onSensorChanged(fieldHeading, orientationValues);
        mSensorOrientationTextView.setText(getString(R.string.degrees_format, mCurrentSensorHeading));
    }

    // --------------------------- Button Handlers ----------------------------

    /**
     * Helper method that is called by all three golf ball clicks.
     */
    private void handleBallClickForLocation(final int location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GolfBallDeliveryActivity.this);
        builder.setTitle("What was the real color?").setItems(R.array.ball_colors,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        GolfBallDeliveryActivity.this.setLocationToColor(location, BallColor.values()[which]);
                    }
                });
        builder.create().show();
    }

    /**
     * Click to the far left image button (Location 1).
     */
    public void handleBallAtLocation1Click(View view) {
        handleBallClickForLocation(1);
    }

    /**
     * Click to the center image button (Location 2).
     */
    public void handleBallAtLocation2Click(View view) {
        handleBallClickForLocation(2);
    }

    /**
     * Click to the far right image button (Location 3).
     */
    public void handleBallAtLocation3Click(View view) {
        handleBallClickForLocation(3);
    }

    /**
     * Sets the mOnRedTeam boolean value as appropriate
     * Side effects: Clears the balls
     * @param view
     */
    public void handleTeamChange(View view) {
        setLocationToColor(1, BallColor.NONE);
        setLocationToColor(2, BallColor.NONE);
        setLocationToColor(3, BallColor.NONE);
        if (mOnRedTeam) {
            mOnRedTeam = false;
            mTeamChangeButton.setBackgroundResource(R.drawable.blue_button);
            mTeamChangeButton.setText("Team Blue");
        } else {
            mOnRedTeam = true;
            mTeamChangeButton.setBackgroundResource(R.drawable.red_button);
            mTeamChangeButton.setText("Team Red");
        }
        // setTeamToRed(mOnRedTeam); // This call is optional. It will reset your GPS and sensor heading values.
    }

    /**
     * Sends a message to Arduino to perform a ball color test.
     */
    public void handlePerformBallTest(View view) {
//        Toast.makeText(this, "TODO: Implement handlePerformBallTest", Toast.LENGTH_SHORT).show();
//        speak("TO DO Perform a ball test");
        //this is what we really do
        //sendCommand("CUSTOM baltest");


        //for testing
        onCommandReceived("1R");
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onCommandReceived("2W");
            }
        }, 100);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onCommandReceived("3B");
            }
        }, 2000);
    }


    @Override
    protected void onCommandReceived(String receivedCommand) {
        super.onCommandReceived(receivedCommand);
        if(receivedCommand.equalsIgnoreCase("1R")){
            setLocationToColor(1, BallColor.RED);
        }else if(receivedCommand.equalsIgnoreCase("2W")) {
            setLocationToColor(2, BallColor.WHITE);
        }else if(receivedCommand.equalsIgnoreCase("3B")){
            setLocationToColor(3, BallColor.BLUE);
        }
    }

    AlertDialog alert;
    /**
     * Clicks to the red arrow image button that should show a dialog window.
     */
    public void handleDrivingStraight(View view) {
        Toast.makeText(this, "handleDrivingStraight", Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(GolfBallDeliveryActivity.this);
        builder.setTitle("Driving Straight Calibration");
        View dialoglayout = getLayoutInflater().inflate(R.layout.driving_straight_dialog, (ViewGroup) getCurrentFocus());
        builder.setView(dialoglayout);
        final NumberPicker rightDutyCyclePicker = (NumberPicker) dialoglayout.findViewById(R.id.right_pwm_number_picker);
        rightDutyCyclePicker.setMaxValue(255);
        rightDutyCyclePicker.setMinValue(0);
        rightDutyCyclePicker.setValue(mRightStraightPwmValue);
        rightDutyCyclePicker.setWrapSelectorWheel(false);
        final NumberPicker leftDutyCyclePicker = (NumberPicker) dialoglayout.findViewById(R.id.left_pwm_number_picker);
        leftDutyCyclePicker.setMaxValue(255);
        leftDutyCyclePicker.setMinValue(0);
        leftDutyCyclePicker.setValue(mLeftStraightPwmValue);
        leftDutyCyclePicker.setWrapSelectorWheel(false);
        Button doneButton = (Button) dialoglayout.findViewById(R.id.done_button);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLeftStraightPwmValue = leftDutyCyclePicker.getValue();
                mRightStraightPwmValue = rightDutyCyclePicker.getValue();
                alert.dismiss();
            }
        });
        final Button testStraightButton = (Button) dialoglayout.findViewById(R.id.test_straight_button);
        testStraightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLeftStraightPwmValue = leftDutyCyclePicker.getValue();
                mRightStraightPwmValue = rightDutyCyclePicker.getValue();
//                Toast.makeText(GolfBallDeliveryActivity.this, "TODO: Implement the drive straight test", Toast.LENGTH_SHORT).show();
                mScripts.testStraightScript();
            }
        });
        alert = builder.create();
        alert.show();
    }

    /**
     * Test GPS point when going to the Far ball (assumes Blue Team heading to red ball).
     */
    public void handleFakeGpsF0(View view) {
//        Toast.makeText(this, "TODO: Implement handleFakeGpsF0", Toast.LENGTH_SHORT).show();
        onLocationChanged(165, 50, NO_HEADING,null);
    }

    public void handleFakeGpsF1(View view) {
//        Toast.makeText(this, "TODO: Implement handleFakeGpsF1", Toast.LENGTH_SHORT).show();
        onLocationChanged(209, 50, NO_HEADING,null);
    }

    public void handleFakeGpsF2(View view) {
//        Toast.makeText(this, "TODO: Implement handleFakeGpsF2", Toast.LENGTH_SHORT).show();
        onLocationChanged(231, 50, 135,null);
    }

    public void handleFakeGpsF3(View view) {
//        Toast.makeText(this, "TODO: Implement handleFakeGpsF3", Toast.LENGTH_SHORT).show();
        onLocationChanged(240, 41, 35,null);
    }

    public void handleFakeGpsH0(View view) {
//        Toast.makeText(this, "TODO: Implement handleFakeGpsH0", Toast.LENGTH_SHORT).show();
        onLocationChanged(165, 0, -179.9,null);
    }

    public void handleFakeGpsH1(View view) {
//        Toast.makeText(this, "TODO: Implement handleFakeGpsH1", Toast.LENGTH_SHORT).show();
        onLocationChanged(11, 0, -179.9,null);

    }

    public void handleFakeGpsH2(View view) {
//        Toast.makeText(this, "TODO: Implement handleFakeGpsH2", Toast.LENGTH_SHORT).show();
        onLocationChanged(9, 0, -170,null);
    }

    public void handleFakeGpsH3(View view) {
//        Toast.makeText(this, "TODO: Implement handleFakeGpsH3", Toast.LENGTH_SHORT).show();
        onLocationChanged(0, -9, -170,null);
    }

    public void handleSetOrigin(View view) {
//        Toast.makeText(this, "TODO: Implement handleSetOrigin", Toast.LENGTH_SHORT).show();
        mFieldGps.setCurrentLocationAsOrigin();
    }

    public void handleSetXAxis(View view) {
//        Toast.makeText(this, "TODO: Implement handleSetXAxis", Toast.LENGTH_SHORT).show();
        mFieldGps.setCurrentLocationAsLocationOnXAxis();
    }

    public void handleZeroHeading(View view) {
//        Toast.makeText(this, "TODO: Implement handleZeroHeading", Toast.LENGTH_SHORT).show();
        mFieldOrientation.setCurrentFieldHeading(0);
    }

    public void handleGoOrMissionComplete(View view) {
//        Toast.makeText(this, "TODO: Implement handleGoOrMissionComplete", Toast.LENGTH_SHORT).show();
        if (mState == State.READY_FOR_MISSION){
            mMatchStartTime = System.currentTimeMillis();//Match starts
            UpdateMissionStrategyVariables();
            mGoOrMissionCompleteButton.setBackgroundResource(R.drawable.red_button);
            mGoOrMissionCompleteButton.setText("Mission Complete!");
            mGoOrCompleteJumboButton.setBackgroundResource(R.drawable.red_button);
            mGoOrCompleteJumboButton.setText("Stop!");
            setState(State.NEAR_BALL_SCRIPT);
        }else{
            setState(State.READY_FOR_MISSION);
        }
    }

    private void UpdateMissionStrategyVariables() {
        mNearBallGpsY = -50;
        mFarBallGpsY = 50;
        mNearBallLocation = 1;
        mWhiteBallLocation = 0;
        mFarBallLocation = 3;

        for (int i = 0; i < 3 ; i++){
            BallColor currentLocationColor = mLocationColors[i];
            if (currentLocationColor == BallColor.WHITE){
                mWhiteBallLocation = i +1;
            }
        }
        if (mOnRedTeam){
            Log.d(TAG, "I'm on the red team");
        }else{
            Log.d(TAG, "I'm on the blue team");
        }

        Log.d(TAG, "Near ball location:" + mNearBallLocation +" drop off at" + mNearBallGpsY);
        Log.d(TAG, "Far ball location:" + mFarBallLocation +" drop off at" + mFarBallGpsY);
        Log.d(TAG, "White ball location:" + mWhiteBallLocation);
    }
}