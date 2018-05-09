package edu.rosehulman.chenj4.integratedimagerec;

import android.widget.Toast;

/**
 * Created by chenj4 on 4/24/2018.
 */

public class Scripts {
    private android.os.Handler mCommandHandler = new android.os.Handler();
    private GolfBallDeliveryActivity mActivity;
    private int ARM_REMOVAL_TIME = 3000;
    public Scripts(GolfBallDeliveryActivity activity){
        mActivity = activity;
    }

    public void testStraightScript(){
        mActivity.sendWheelSpeed(mActivity.mLeftStraightPwmValue, mActivity.mRightStraightPwmValue);
        Toast.makeText(mActivity, "Begin driving",Toast.LENGTH_SHORT).show();
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.sendWheelSpeed(0,0);
                Toast.makeText(mActivity, "Stop driving",Toast.LENGTH_SHORT).show();
            }
        },8000);
    }

    public void nearBallScript(){
        mActivity.sendWheelSpeed(0,0);
        KickPosition(mActivity.mNearBallLocation,false,true);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.setState(GolfBallDeliveryActivity.State.DRIVING_TOWARD_FAR_BALL);
            }
        },15000);
    }

    public void farBallScript() {
        mActivity.sendWheelSpeed(0,0);
        KickPosition(mActivity.WKindex,mActivity.isBlack,true);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                KickPosition(mActivity.mFarBallLocation,false,true);
            }
        },15000);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.setState(GolfBallDeliveryActivity.State.DRIVE_TOWARD_HOME);
            }
        },30000);
    }

    private void KickPosition(int position, boolean isBlack, boolean moveForward){
        if (!isBlack){
            if (position == 1){
                mActivity.sendCommand("ATTACH 111111");
                mActivity.sendCommand("GRIPPER 62");
                mActivity.sendCommand("POSITION 11 65 -3 -79 72");
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION 47 78 -73 -163 158");
                    }
                },3000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("GRIPPER 0");
                    }
                },5000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION 47 78 -73 -90 158");
                    }
                },6500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION 13 160 -7 -76 158");
                    }
                },8000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("GRIPPER 62");
                    }
                },10000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("ATTACH 11111");
                        mActivity.sendCommand("POSITION 11 65 -3 -79 72");
                    }
                },12000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION 16 0 90 0 159");
                    }
                },14000);


            }else if (position == 2){
                mActivity.sendCommand("ATTACH 111111");
                mActivity.sendCommand("GRIPPER 62");
                mActivity.sendCommand("POSITION 11 65 -3 -79 72");
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION 20 87 -73 -171 158");
                    }
                },3000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("GRIPPER 0");
                    }
                },5000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION 20 87 -73 -90 158");
                    }
                },6500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION 13 160 -7 -76 158");
                    }
                },8000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("GRIPPER 62");
                    }
                },10000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("ATTACH 11111");
                        mActivity.sendCommand("POSITION 11 65 -3 -79 72");
                    }
                },12000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION 16 0 90 0 159");
                    }
                },14000);


            }else if (position == 3){
                mActivity.sendCommand("ATTACH 111111");
                mActivity.sendCommand("GRIPPER 62");
                mActivity.sendCommand("POSITION 11 65 -3 -79 72");
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION -8 89 -79 -164 158");
                    }
                },3000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("GRIPPER 0");
                    }
                },5000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION -8 89 -79 -90 158");
                    }
                },6500);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION 13 160 -7 -76 158");
                    }
                },8000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("GRIPPER 62");
                    }
                },10000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("ATTACH 11111");
                        mActivity.sendCommand("POSITION 11 65 -3 -79 72");
                    }
                },12000);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.sendCommand("POSITION 16 0 90 0 159");
                    }
                },14000);
            }
        }

    }

}
