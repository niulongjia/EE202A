package com.openxc.openxcstarter;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.openxcplatform.openxcstarter.R;
import com.smartfuel.SmartFuel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * A simple {@link Fragment} subclass.
 */
public class fragment3 extends Fragment
{
    public double steeringWheelAngle_val;
    public double torqueAtTransmission_val;
    public double engineSpeed_val;
    public double vehicleSpeed_val;
    public double acceleratorPedalPosition_val;
    public boolean brakePedalStatus_val;
    public double odometer_val;
    public double fuelConsumed_val;
    public double fuelLevel_val;
    public double latitude_val=34.052235;
    public double longitude_val=-118.243683;
    public double totalFuel_val;


    private TextView mSteerWheelAngleView;
    private TextView mTorqueAtTransView;
    private TextView mEngineSpeedView;
    private TextView mVehicleSpeedView;
    private TextView mAccelPedalPosView;
    private TextView mBrakeStatusView;
    private TextView mOdometerView;
    private TextView mFuelConsumeView;
    private TextView mFuelLevelView;
    private TextView mLatitudeView;
    private TextView mLongitudeView;
    private TextView mTotalFuelView;

    public fragment3()
    {
        // Required empty public constructor
    }
    @Override
    public void onStart()
    {
        super.onStart();
        EventBus.getDefault().register(this);
    }
    @Override
    public void onStop()
    {
        EventBus.getDefault().unregister(this);
        super.onStop();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.layout_fragment3,container,false);

        mSteerWheelAngleView = (TextView) view.findViewById(R.id.steerWheelAngle);
        mTorqueAtTransView = (TextView) view.findViewById(R.id.torqueAtTransmission);
        mEngineSpeedView = (TextView) view.findViewById(R.id.engine_speed);
        mVehicleSpeedView=(TextView)view.findViewById(R.id.vehicle_speed);
        mAccelPedalPosView = (TextView) view.findViewById(R.id.accelPedalPos);
        mBrakeStatusView = (TextView) view.findViewById(R.id.brake_status);
        mOdometerView = (TextView) view.findViewById(R.id.odometer);
        mFuelConsumeView=(TextView)view.findViewById(R.id.fuel_consume);
        mFuelLevelView = (TextView) view.findViewById(R.id.fuel_level);
        mLatitudeView = (TextView)view.findViewById(R.id.latitude);
        mLongitudeView = (TextView)view.findViewById(R.id.longitude);
        mTotalFuelView = (TextView)view.findViewById(R.id.total_fuel);
        return view;
    }

    @Subscribe
    public void onEvent(final TextChangedEvent textChangedEvent)
    {
        //Log.d("debugMsg",Double.toString(textChangedEvent.latitude_val));

        steeringWheelAngle_val = textChangedEvent.steeringWheelAngle_val;
        torqueAtTransmission_val = textChangedEvent.torqueAtTransmission_val;
        engineSpeed_val = textChangedEvent.engineSpeed_val;
        vehicleSpeed_val = textChangedEvent.vehicleSpeed_val;
        acceleratorPedalPosition_val = textChangedEvent.acceleratorPedalPosition_val;
        brakePedalStatus_val = textChangedEvent.brakePedalStatus_val;
        odometer_val = textChangedEvent.odometer_val;
        fuelConsumed_val = textChangedEvent.fuelConsumed_val;
        fuelLevel_val = textChangedEvent.fuelLevel_val;
        latitude_val = textChangedEvent.latitude_val;
        longitude_val = textChangedEvent.longitude_val;

        totalFuel_val=textChangedEvent.totalFuel_val;

        // update UI in real time
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run()
            {

                mSteerWheelAngleView.setText("SteerWheelAngle (degrees):"+Double.toString(steeringWheelAngle_val));

                mTorqueAtTransView.setText("TorqueAtTrans (Nm):"+Double.toString(torqueAtTransmission_val));

                mEngineSpeedView.setText("EngSpeed (RPM):"+Double.toString(engineSpeed_val));

                mVehicleSpeedView.setText("VehSpeed (km/h):"+Double.toString(vehicleSpeed_val));

                mAccelPedalPosView.setText("AccelPedalPos (%):"+Double.toString(acceleratorPedalPosition_val));

                mBrakeStatusView.setText("BrakeStatus (bool):"+ java.lang.Boolean.toString(brakePedalStatus_val));

                mOdometerView.setText("Odometer (km):"+Double.toString(odometer_val));

                mFuelConsumeView.setText("FuelConsumed (L):"+Double.toString(fuelConsumed_val));

                mFuelLevelView.setText("FuelLevel (%):"+Double.toString(fuelLevel_val));

                mLatitudeView.setText("Latitude (degrees):"+Double.toString(latitude_val));

                mLongitudeView.setText("Longitude (degrees)"+Double.toString(longitude_val));

                mTotalFuelView.setText("GasolineTankSize (L):"+Double.toString(totalFuel_val));

            }
        });

    }
}
