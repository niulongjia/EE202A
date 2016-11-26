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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.MapFragment;

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

    public double speed_val;
    public double fuellevel_val;
    public boolean brakePedalStatus_val;
    public double fuelConsumed_val;
    public double vehicleSpeed_val;
    public double latitude_val=34.052235;
    public double longitude_val=-118.243683;
    public long birthtime_val;

    public SwipeAdapter mSwipeAdapter;
    public ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starter);

        // swipe to show three fragments
        // fragment1 --> google map
        // fragment2 --> real time vehicle information
        // fragment3 --> creaters
        List<Fragment> fragments=new Vector<Fragment>();
        fragments.add(Fragment.instantiate(this,fragment1.class.getName()));
        fragments.add(Fragment.instantiate(this,fragment2.class.getName()));
        fragments.add(Fragment.instantiate(this,fragment3.class.getName()));
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
            mVehicleManager.removeListener(EngineSpeed.class, mSpeedListener);
            mVehicleManager.removeListener(FuelLevel.class,mFuelLevelListener);
            mVehicleManager.removeListener(BrakePedalStatus.class,mBrakeStatusListener);
            mVehicleManager.removeListener(FuelConsumed.class,mFuelConsumeListener);
            mVehicleManager.removeListener(Latitude.class,mLatitudeListener);
            mVehicleManager.removeListener(Longitude.class,mLongitudeListener);
            mVehicleManager.removeListener(VehicleSpeed.class,mVehicleSpeedListener);
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
    // The EngineSpeed measurement represents the speed of the engine.
    // The valid range for this measurement is from 0 to 8000 RotationsPerMinute
    EngineSpeed.Listener mSpeedListener = new EngineSpeed.Listener()
    {
        @Override
        public void receive(Measurement measurement) {
            // When we receive a new EngineSpeed value from the car, we want to
            // update the UI to display the new value. First we cast the generic
            // Measurement back to the type we know it to be, an EngineSpeed.

            EngineSpeed speed = (EngineSpeed) measurement;
            speed_val=speed.getValue().doubleValue();
            birthtime_val = speed.getBirthtime();
            // speed.getAge())


        }
    };

    // The FuelLevel is the current level of fuel in the gas tank.
    FuelLevel.Listener mFuelLevelListener = new FuelLevel.Listener()
    {
        @Override
        public void receive(Measurement measurement) {

            FuelLevel fuelLevel=(FuelLevel) measurement;
            fuellevel_val=fuelLevel.getValue().doubleValue();
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

            // note that we use greenrobot EventBus for transfer data between activities.
            // check out <  https://github.com/greenrobot/EventBus  > for details.
            EventBus.getDefault().post(new TextChangedEvent(
                    speed_val,
                    fuellevel_val,
                    brakePedalStatus_val,
                    fuelConsumed_val,
                    vehicleSpeed_val,
                    latitude_val,
                    longitude_val,
                    birthtime_val
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
        }
    };
    private ServiceConnection mConnection = new ServiceConnection()
    {
        // Called when the connection with the VehicleManager service is
        // established, i.e. bound.
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            // When the VehicleManager starts up, we store a reference to it
            // here in "mVehicleManager" so we can call functions on it
            // elsewhere in our code.
            mVehicleManager = ((VehicleManager.VehicleBinder) service).getService();

            // We want to receive updates whenever the EngineSpeed changes. We
            // have an EngineSpeed.Listener (see above, mSpeedListener) and here
            // we request that the VehicleManager call its receive() method
            // whenever the EngineSpeed changes
            mVehicleManager.addListener(EngineSpeed.class, mSpeedListener);
            mVehicleManager.addListener(FuelLevel.class, mFuelLevelListener);
            mVehicleManager.addListener(FuelConsumed.class,mFuelConsumeListener);
            mVehicleManager.addListener(BrakePedalStatus.class,mBrakeStatusListener);
            mVehicleManager.addListener(Latitude.class,mLatitudeListener);
            mVehicleManager.addListener(Longitude.class,mLongitudeListener);
            mVehicleManager.addListener(VehicleSpeed.class,mVehicleSpeedListener);
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
