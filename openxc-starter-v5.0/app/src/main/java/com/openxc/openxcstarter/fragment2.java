package com.openxc.openxcstarter;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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
import com.smartfuel.SmartFuel;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.w3c.dom.Text;


/**
 * A simple {@link Fragment} subclass.
 */
public class fragment2 extends Fragment
{
    public SmartFuel smartFuel;
    public String jsonData;
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

    public double mean;
    public double var;
    public double toDestPossibility=-1.0;
    public double toGasPossibility=-1.0;


    private TextView mFuelAmountView;

    private TextView mMeanView;
    private TextView mVarrView;
    private TextView mPossibilityArrivingView;
    private TextView mToDestWarning;
    private TextView mToGasWarning;
    public fragment2() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        View view=inflater.inflate(R.layout.layout_fragment2,container,false);


        mFuelAmountView = (TextView) view.findViewById(R.id.fuelAmountLeft);
        mMeanView=(TextView)view.findViewById(R.id.mean);
        mVarrView=(TextView)view.findViewById(R.id.var);
        mPossibilityArrivingView=(TextView)view.findViewById(R.id.possibilityArriving);
        mToDestWarning = (TextView)view.findViewById(R.id.toDestinationWarning);
        mToGasWarning = (TextView) view.findViewById(R.id.toGasStationWarning);
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
    public void onEventPressFindPath(final PressFindPathEvent pressFindPathEvent)
    {
        if (pressFindPathEvent.pressed==true)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mToGasWarning.setText("");
                }
            });
        }
    }
    @Subscribe
    public void onEventToGasPossibility(final ToGasPossibilityEvent toGasPossibilityEvent)
    {
        toGasPossibility=toGasPossibilityEvent.toGasPossibility;
        Log.d("debugMsg0","toGasPoss:"+toGasPossibility+" toGasMean:"+toGasPossibilityEvent.mean+" toGasVar:"+toGasPossibilityEvent.var);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toGasPossibility>=0 && toGasPossibility<0.5)
                {
                    mToGasWarning.setText("WARNING! Can not make it to the gas station!");
                    mToGasWarning.setTextColor(Color.RED);
                }
                else
                {
                    mToGasWarning.setText("You can safely drive to the gas station!");
                    mToGasWarning.setTextColor(Color.GREEN);
                }
            }
        });
    }
    @Subscribe
    public void onEventToDestPossibility(final ToDestPossibilityEvent toDestPossibilityEvent)
    {

        toDestPossibility=toDestPossibilityEvent.toDestPossibility;
        mean=toDestPossibilityEvent.mean;
        var=toDestPossibilityEvent.var;
        Log.d("debugMsg0","toDestPoss:"+toDestPossibility+" toDestMean:"+mean+" toDestVar:"+var);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMeanView.setText("FuelConsumed Mean (to dest):"+Double.toString(mean));
                mVarrView.setText("FuelConsumed Var (to dest):"+Double.toString(var));
                String arrivPoss=Double.toString(100*toDestPossibility);

                if (toDestPossibility>=0) mPossibilityArrivingView.setText("To Dest Possibility:"+ arrivPoss+"%");
                if (toDestPossibility>=0 && toDestPossibility<0.5)
                {
                    mToDestWarning.setText("WARNING! Can not make it to the destination!");
                    mToDestWarning.setTextColor(Color.RED);
                }
                else
                {
                    mToDestWarning.setText("You can safely drive to the destination!");
                    mToDestWarning.setTextColor(Color.GREEN);
                }
            }
        });
    }
    @Subscribe
    public void onEvent(final TextChangedEvent textChangedEvent)
    {
        //Log.d("debugMsg",Double.toString(textChangedEvent.latitude_val));

            smartFuel=textChangedEvent.smartFuel;
           steeringWheelAngle_val = textChangedEvent.steeringWheelAngle_val;
           torqueAtTransmission_val = textChangedEvent.torqueAtTransmission_val;
           engineSpeed_val = textChangedEvent.engineSpeed_val;
           vehicleSpeed_val = textChangedEvent.vehicleSpeed_val;
           acceleratorPedalPosition_val = textChangedEvent.acceleratorPedalPosition_val;
           brakePedalStatus_val = textChangedEvent.brakePedalStatus_val;
           odometer_val = textChangedEvent.odometer_val;
           fuelConsumed_val = textChangedEvent.fuelConsumed_val;
            fuelLevel_val = textChangedEvent.fuelLevel_val;
            totalFuel_val=textChangedEvent.totalFuel_val;
            latitude_val = textChangedEvent.latitude_val;
           longitude_val = textChangedEvent.longitude_val;


        // update UI in real time
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run()
            {

                mFuelAmountView.setText("FuelAmountLeft (L):"+Double.toString(totalFuel_val*fuelLevel_val/100.0));
            }
        });

    }
}