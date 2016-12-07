package com.openxc.openxcstarter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import com.openxc.measurements.AcceleratorPedalPosition;
import com.openxc.measurements.BrakePedalStatus;
import com.openxc.measurements.FuelConsumed;
import com.openxc.measurements.FuelLevel;
import com.openxc.measurements.Latitude;
import com.openxc.measurements.Longitude;
import com.openxc.measurements.Odometer;
import com.openxc.measurements.SteeringWheelAngle;
import com.openxc.measurements.TorqueAtTransmission;
import com.openxc.measurements.TransmissionGearPosition;
import com.openxc.measurements.VehicleSpeed;
import com.openxcplatform.openxcstarter.R;
import com.openxc.VehicleManager;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.EngineSpeed;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.MapFragment;
import com.smartfuel.SmartFuel;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequestFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import org.greenrobot.eventbus.EventBus;

public class StarterActivity extends FragmentActivity
{
    private static final String TAG = "StarterActivity";
    private static final String PLACES_SEARCH_URL =  "https://maps.googleapis.com/maps/api/place/search/json?";
    private static final boolean PRINT_AS_STRING = false;
    private static final int TIME_INTERVAL=3000;
    private static final int FASTEST_TIME_INTERVAL=1500;
    private static final double PROXIMITY_RADIUS=800.0;

    private VehicleManager mVehicleManager;
    // VehicleManager mVehicleManagerAll;

    private SmartFuel smartFuel = new SmartFuel();

    public double steeringWheelAngle_val;
    public double torqueAtTransmission_val;
    public double engineSpeed_val;
    public double vehicleSpeed_val;
    public double acceleratorPedalPosition_val;
    public boolean brakePedalStatus_val;
    public double odometer_val;
    public double fuelConsumed_val;
    /*** this is the percentage of gasoline left ***/
    public double fuelLevel_val;
    /*** This is the size of gasoline tank, we don't know so make up one ***/
    public double totalFuel_val=100;
    public double latitude_val=34.052235;
    public double longitude_val=-118.243683;

    public SwipeAdapter mSwipeAdapter;
    public ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starter);

        // swipe to show three fragments
        // fragment1 --> google map
        // fragment2 --> prediction results based on smartfuel model
        // fragment3 --> vehicle information
        // fragment4 --> creaters
        List<Fragment> fragments=new Vector<Fragment>();
        fragments.add(Fragment.instantiate(this,fragment1.class.getName()));
        fragments.add(Fragment.instantiate(this,fragment2.class.getName()));
        fragments.add(Fragment.instantiate(this,fragment3.class.getName()));
        fragments.add(Fragment.instantiate(this,fragment4.class.getName()));
        mSwipeAdapter=new SwipeAdapter(getSupportFragmentManager(),fragments);
        viewPager=(ViewPager)findViewById(R.id.view_pager);
        viewPager.setAdapter(mSwipeAdapter);

    }


    @Override
    public void onPause() {
        super.onPause();
        // When the activity goes into the background or exits, we want to make
        // sure to unbind from the service to avoid leaking memory
        if(mVehicleManager != null) {
            Log.i(TAG, "Unbinding from Vehicle Manager");
            // Remember to remove your listeners, in typical Android fashion.
            mVehicleManager.removeListener(SteeringWheelAngle.class,mSteeringAngleListener);
            mVehicleManager.removeListener(TorqueAtTransmission.class,mTorqueListener);
            mVehicleManager.removeListener(EngineSpeed.class, mEngineSpeedListener);
            mVehicleManager.removeListener(VehicleSpeed.class,mVehicleSpeedListener);
            mVehicleManager.removeListener(AcceleratorPedalPosition.class,mAccelPedalPosListener);
            mVehicleManager.removeListener(BrakePedalStatus.class,mBrakeStatusListener);
            //mVehicleManager.removeListener(TransmissionGearPosition.class,mTransGearPosListener);
            mVehicleManager.removeListener(Odometer.class,mOdometerListener);
            mVehicleManager.removeListener(FuelConsumed.class,mFuelConsumeListener);
            mVehicleManager.removeListener(FuelLevel.class,mFuelLevelListener);
            mVehicleManager.removeListener(Latitude.class,mLatitudeListener);
            mVehicleManager.removeListener(Longitude.class,mLongitudeListener);
            unbindService(mConnection);
            mVehicleManager = null;
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        // When the activity starts up or returns from the background,
        // re-connect to the VehicleManager so we can receive updates.
        if(mVehicleManager == null) {
            Intent intent = new Intent(this, VehicleManager.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /* This is an OpenXC measurement listener object - the type is recognized
     * by the VehicleManager as something that can receive measurement updates.
     * Later in the file, we'll ask the VehicleManager to call the receive()
     * function here whenever a new EngineSpeed value arrives.
     */

    SteeringWheelAngle.Listener mSteeringAngleListener = new SteeringWheelAngle.Listener() {
        @Override
        public void receive(Measurement measurement) {
            SteeringWheelAngle steeringWheelAngle = (SteeringWheelAngle) measurement;
            steeringWheelAngle_val=steeringWheelAngle.getValue().doubleValue();
            smartFuel.onOpenXCEvent(measurement);
        }
    };
    TorqueAtTransmission.Listener mTorqueListener= new TorqueAtTransmission.Listener() {
        @Override
        public void receive(Measurement measurement) {
            TorqueAtTransmission torqueAtTransmission=(TorqueAtTransmission)measurement;
            torqueAtTransmission_val=torqueAtTransmission.getValue().doubleValue();
            smartFuel.onOpenXCEvent(measurement);
        }
    };

    // The EngineSpeed measurement represents the speed of the engine.
    // The valid range for this measurement is from 0 to 8000 RotationsPerMinute
    EngineSpeed.Listener mEngineSpeedListener = new EngineSpeed.Listener()
    {
        @Override
        public void receive(Measurement measurement) {

            EngineSpeed engineSpeed = (EngineSpeed) measurement;
            engineSpeed_val=engineSpeed.getValue().doubleValue();
            smartFuel.onOpenXCEvent(measurement);
        }
    };
    // The VehicleSpeed is the current forward speed of the vehicle.
    VehicleSpeed.Listener mVehicleSpeedListener = new VehicleSpeed.Listener()
    {
        @Override
        public void receive(Measurement measurement)
        {
            VehicleSpeed vehicleSpeed=(VehicleSpeed)measurement;
            vehicleSpeed_val=vehicleSpeed.getValue().doubleValue();
            smartFuel.onOpenXCEvent(measurement);
        }
    };
    AcceleratorPedalPosition.Listener mAccelPedalPosListener = new AcceleratorPedalPosition.Listener() {
        @Override
        public void receive(Measurement measurement) {
            AcceleratorPedalPosition acceleratorPedalPosition = (AcceleratorPedalPosition)measurement;
            acceleratorPedalPosition_val=acceleratorPedalPosition.getValue().doubleValue();
            smartFuel.onOpenXCEvent(measurement);
        }
    };
    // The BrakePedalStatus measurement knows if the brake pedal is pressed.
    BrakePedalStatus.Listener mBrakeStatusListener=new BrakePedalStatus.Listener()
    {
        @Override
        public void receive(Measurement measurement)
        {

            BrakePedalStatus brakePedalStatus=(BrakePedalStatus) measurement;
            brakePedalStatus_val=brakePedalStatus.getValue().booleanValue();
            smartFuel.onOpenXCEvent(measurement);
        }
    };
/*    public int transmissionGearPosition_val;
    TransmissionGearPosition.Listener mTransGearPosListener = new TransmissionGearPosition.Listener() {
        @Override
        public void receive(Measurement measurement) {
            TransmissionGearPosition transmissionGearPosition=(TransmissionGearPosition)measurement;
            transmissionGearPosition_val=transmissionGearPosition.getValue().enumValue();
            sf.onOpenXCEvent(measurement);
        }
    };*/

    Odometer.Listener mOdometerListener = new Odometer.Listener() {
        @Override
        public void receive(Measurement measurement) {
            Odometer odometer=(Odometer)measurement;
            odometer_val=odometer.getValue().doubleValue();
            smartFuel.onOpenXCEvent(measurement);
        }
    };
    // The FuelConsumed is the fuel consumed since the vehicle was started.
    FuelConsumed.Listener mFuelConsumeListener=new FuelConsumed.Listener()
    {
        @Override
        public void receive(Measurement measurement)
        {

            FuelConsumed fuelConsumed=(FuelConsumed) measurement;
            fuelConsumed_val=fuelConsumed.getValue().doubleValue();
            smartFuel.onOpenXCEvent(measurement);
        }
    };
    FuelLevel.Listener mFuelLevelListener = new FuelLevel.Listener() {
        @Override
        public void receive(Measurement measurement) {
            FuelLevel fuelLevel = (FuelLevel) measurement;
            fuelLevel_val = fuelLevel.getValue().doubleValue();
            smartFuel.onOpenXCEvent(measurement);
        }
    };
    // The Latitude is the current latitude of the vehicle in degrees according to GPS.
    Latitude.Listener mLatitudeListener = new Latitude.Listener()
    {
        @Override
        public void receive(Measurement measurement)
        {

            Latitude latitude=(Latitude)measurement;
            latitude_val=latitude.getValue().doubleValue();
            smartFuel.onOpenXCEvent(measurement);
            /*** note that we use greenrobot EventBus for transfer data between activities.
             *  check out   https://github.com/greenrobot/EventBus   for details.***/
            EventBus.getDefault().post(new TextChangedEvent(
                    smartFuel,
                    steeringWheelAngle_val,
                    torqueAtTransmission_val,
                    engineSpeed_val,
                    vehicleSpeed_val,
                    acceleratorPedalPosition_val,
                    brakePedalStatus_val,
                    odometer_val,
                    fuelConsumed_val,
                    fuelLevel_val,
                    totalFuel_val,
                    latitude_val,
                    longitude_val
            ));
        }
    };
    // The Longitude is the current longitude of the vehicle in degrees according to GPS.
    Longitude.Listener mLongitudeListener = new Longitude.Listener()
    {
        @Override
        public void receive(Measurement measurement)
        {
            Longitude longitude=(Longitude) measurement;
            longitude_val=longitude.getValue().doubleValue();
            smartFuel.onOpenXCEvent(measurement);
        }
    };

    private ServiceConnection mConnection = new ServiceConnection()
    {
        // Called when the connection with the VehicleManager service is
        // established, i.e. bound.
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");

            mVehicleManager = ((VehicleManager.VehicleBinder) service).getService();

            mVehicleManager.addListener(SteeringWheelAngle.class,mSteeringAngleListener);
            mVehicleManager.addListener(TorqueAtTransmission.class,mTorqueListener);
            mVehicleManager.addListener(EngineSpeed.class, mEngineSpeedListener);
            mVehicleManager.addListener(VehicleSpeed.class,mVehicleSpeedListener);
            mVehicleManager.addListener(AcceleratorPedalPosition.class,mAccelPedalPosListener);
            mVehicleManager.addListener(BrakePedalStatus.class,mBrakeStatusListener);
            //mVehicleManager.addListener(TransmissionGearPosition.class,mTransGearPosListener);
            mVehicleManager.addListener(Odometer.class,mOdometerListener);
            mVehicleManager.addListener(FuelConsumed.class,mFuelConsumeListener);
            mVehicleManager.addListener(FuelLevel.class,mFuelLevelListener);
            mVehicleManager.addListener(Latitude.class,mLatitudeListener);
            mVehicleManager.addListener(Longitude.class,mLongitudeListener);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleManager Service  disconnected unexpectedly");
            mVehicleManager = null;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.starter, menu);
        return true;
    }


}
