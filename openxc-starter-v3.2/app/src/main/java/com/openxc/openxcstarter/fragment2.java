package com.openxc.openxcstarter;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.openxc.units.Boolean;
import com.openxcplatform.openxcstarter.R;

import com.openxc.measurements.BrakePedalStatus;
import com.openxc.measurements.FuelConsumed;
import com.openxc.measurements.FuelLevel;
import com.openxc.measurements.Latitude;
import com.openxc.measurements.Longitude;
import com.openxc.measurements.VehicleSpeed;
import com.openxcplatform.openxcstarter.R;
import com.openxc.VehicleManager;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.EngineSpeed;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


/**
 * A simple {@link Fragment} subclass.
 */
public class fragment2 extends Fragment
{
    private TextView mBirthTimeView;
    private TextView mEngineSpeedView;
    private TextView mFuelLevelView;
    private TextView mBrakeStatusView;
    private TextView mFuelConsumeView;
    private TextView mLatitudeView;
    private TextView mLongitudeView;
    private TextView mVehicleSpeedView;


    public fragment2() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        View view=inflater.inflate(R.layout.layout_fragment2,container,false);



        mBirthTimeView=(TextView)view.findViewById(R.id.birthtime);
        mEngineSpeedView = (TextView) view.findViewById(R.id.engine_speed);
        mFuelLevelView=(TextView) view.findViewById(R.id.fuel_level);
        mBrakeStatusView = (TextView) view.findViewById(R.id.brake_status);
        mFuelConsumeView=(TextView)view.findViewById(R.id.fuel_consume);
        mLatitudeView=(TextView)view.findViewById(R.id.latitude);
        mLongitudeView=(TextView)view.findViewById(R.id.longitude);
        mVehicleSpeedView=(TextView)view.findViewById(R.id.vehicle_speed);

        // Inflate the layout for this fragment
        return view;
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
    @Subscribe
    public void onEvent(final TextChangedEvent textChangedEvent)
    {
        //Log.d("debugMsg",Double.toString(textChangedEvent.latitude_val));

        // update UI in real time
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                mEngineSpeedView.setText("Engine Speed (RPM):"+Double.toString(textChangedEvent.engineSpeed_val));
                mFuelLevelView.setText("Fuel Level:"+Double.toString(textChangedEvent.fuellevel_val));
                mBrakeStatusView.setText("Brake Status:"+ java.lang.Boolean.toString(textChangedEvent.brakePedalStatus_val));
                mFuelConsumeView.setText("Fuel Consume:"+Double.toString(textChangedEvent.fuelConsumed_val));
                mVehicleSpeedView.setText("Vehicle Speed:"+Double.toString(textChangedEvent.vehicleSpeed_val));
                mLatitudeView.setText("Latitude:"+Double.toString(textChangedEvent.latitude_val));
                mLongitudeView.setText("Longitude:"+Double.toString(textChangedEvent.longitude_val));
                mBirthTimeView.setText("Birthtime:"+Long.toString(textChangedEvent.birthtime_val));
            }
        });

    }
}
