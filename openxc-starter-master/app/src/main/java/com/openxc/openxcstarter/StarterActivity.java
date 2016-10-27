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

public class StarterActivity extends Activity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GetNearbyGasStation.AsyncResponseGasStation,
        DirectionFinder.AsyncResponseDirectionFinder
{
    private static final String TAG = "StarterActivity";
    private static final String PLACES_SEARCH_URL =  "https://maps.googleapis.com/maps/api/place/search/json?";
    private static final boolean PRINT_AS_STRING = false;
    private static final int TIME_INTERVAL=3000;
    private static final int FASTEST_TIME_INTERVAL=1500;
    private static final double PROXIMITY_RADIUS=800.0;

    private VehicleManager mVehicleManager;
    private TextView mBirthTimeView;
    private TextView mEngineSpeedView;
    private TextView mFuelLevelView;
    private TextView mBrakeStatusView;
    private TextView mFuelConsumeView;
    private TextView mLatitudeView;
    private TextView mLongitudeView;
    private TextView mVehicleSpeedView;
    public Button mfindGasStationBtn;
    private TextView mNearbyDataView;

    private double latitude_val;
    private double longitude_val;
    public GoogleMap mGoogleMap;
    public Marker marker;
    public Circle circle;
    public Polyline polyline;

    //public Route route;
    public LatLng originLatLng;
    public LatLng destinationLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starter);
        // grab a reference to the engine speed text object in the UI, so we can
        // manipulate its value later from Java code
        mBirthTimeView=(TextView)findViewById(R.id.birthtime);
        mEngineSpeedView = (TextView) findViewById(R.id.engine_speed);
        mFuelLevelView=(TextView) findViewById(R.id.fuel_level);
        mBrakeStatusView = (TextView) findViewById(R.id.brake_status);
        mFuelConsumeView=(TextView)findViewById(R.id.fuel_consume);
        mLatitudeView=(TextView)findViewById(R.id.latitude);
        mLongitudeView=(TextView)findViewById(R.id.longitude);
        mVehicleSpeedView=(TextView)findViewById(R.id.vehicle_speed);
        //mfindGasStationBtn=(Button)findViewById(R.id.findGasStation);
        mNearbyDataView=(TextView)findViewById(R.id.nearbyData);

        MapFragment mapFragment=(MapFragment)getFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

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
            final EngineSpeed speed = (EngineSpeed) measurement;
            // In order to modify the UI, we have to make sure the code is
            // running on the "UI thread" - Google around for this, it's an
            // important concept in Android.
            StarterActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    // Finally, we've got a new value and we're running on the
                    // UI thread - we set the text of the EngineSpeed view to
                    // the latest value
                    mBirthTimeView.setText("Birthtime: "+speed.getBirthtime() + "  ,  "+speed.getAge());
                    mEngineSpeedView.setText("Engine speed (RPM): " + speed.getValue().doubleValue());
                }
            });
        }
    };

    // The FuelLevel is the current level of fuel in the gas tank.
    FuelLevel.Listener mFuelLevelListener = new FuelLevel.Listener()
    {
        @Override
        public void receive(Measurement measurement) {

            //final EngineSpeed speed = (EngineSpeed) measurement;
            final FuelLevel fuelLevel=(FuelLevel) measurement;
            StarterActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    mFuelLevelView.setText("Fuel Level: " + fuelLevel.getValue().doubleValue());
                }
            });
        }
    };
    // The BrakePedalStatus measurement knows if the brake pedal is pressed.
    BrakePedalStatus.Listener mBrakeStatusListener=new BrakePedalStatus.Listener()
    {
        @Override
        public void receive(Measurement measurement)
        {
            final BrakePedalStatus brakePedalStatus=(BrakePedalStatus) measurement;
            StarterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBrakeStatusView.setText("Brake Status: "+brakePedalStatus.getValue().booleanValue());
                }
            });
        }
    };
    // The FuelConsumed is the fuel consumed since the vehicle was started.
    FuelConsumed.Listener mFuelConsumeListener=new FuelConsumed.Listener()
    {
        @Override
        public void receive(Measurement measurement)
        {
            final FuelConsumed fuelConsumed=(FuelConsumed) measurement;
            StarterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFuelConsumeView.setText("Fuel Consume: "+fuelConsumed.getValue().doubleValue());
                }
            });
        }
    };
    // The Latitude is the current latitude of the vehicle in degrees according to GPS.
    Latitude.Listener mLatitudeListener = new Latitude.Listener()
    {
        @Override
        public void receive(Measurement measurement)
        {
            final Latitude latitude=(Latitude)measurement;
            StarterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    latitude_val=latitude.getValue().doubleValue();

                    mLatitudeView.setText("Latitude: "+latitude_val);
                }
            });
        }
    };
    // The Longitude is the current longitude of the vehicle in degrees according to GPS.
    Longitude.Listener mLongitudeListener = new Longitude.Listener()
    {
        @Override
        public void receive(Measurement measurement)
        {
            final Longitude longitude=(Longitude) measurement;
            StarterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    longitude_val=longitude.getValue().doubleValue();
                    mLongitudeView.setText("Longitude: "+longitude_val);
                }
            });
        }
    };

    // The VehicleSpeed is the current forward speed of the vehicle.
    VehicleSpeed.Listener mVehicleSpeedListener = new VehicleSpeed.Listener()
    {
        @Override
        public void receive(Measurement measurement)
        {
            final VehicleSpeed vehicleSpeed=(VehicleSpeed)measurement;
            StarterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mVehicleSpeedView.setText("Vehicle Speed: "+vehicleSpeed.getValue().doubleValue());
                }
            });
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

    // This is used to track real-time positions.
    GoogleApiClient mGoogleApiClient;

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mGoogleMap=googleMap;

        showCarMarkerOnMap();

        LatLng ll=new LatLng(latitude_val,longitude_val);
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll,14 ));

        showCircleOnMap();

        mGoogleApiClient=new GoogleApiClient.Builder(this)
                            .addApi(LocationServices.API)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .build();
        mGoogleApiClient.connect();

        mfindGasStationBtn = (Button)findViewById(R.id.findGasStation);
        mfindGasStationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGoogleMap.clear();
                mNearbyDataView.setText(
                        Double.toString(destinationLatLng.latitude)+","
                        +Double.toString(destinationLatLng.longitude) );
                asyncGetDirectionToGasStation();
            }
        });
    }

    // This is used to track real-time positions.
    public LocationRequest mLocationRequest;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest=LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(TIME_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_TIME_INTERVAL);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location==null) Toast.makeText(this,"Can not get location!",Toast.LENGTH_LONG).show();
        else
        {
            // If using location, the google map will track gps position of the phone
            // However, we force Google Map camera to focus on received gps location from OpenXC, during location change.
            LatLng ll=new LatLng(latitude_val,longitude_val);
            Toast.makeText(this,"Latitude:"+latitude_val+" , Longitude:"+longitude_val,Toast.LENGTH_SHORT).show();

            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(ll));

            // clear previous markers!

            //mGoogleMap.clear();
            removeMarkerCircle();

            showCarMarkerOnMap();


            asyncGetNearbyGasStation();
            //asyncGetDirectionToGasStation();
            showCircleOnMap();
        }
    }
    // remove marker and circle on map.
    public void removeMarkerCircle()
    {
        marker.remove();
        circle.remove();
        //mGoogleMap.clear();
    }
    // show the marker of car.
    public void showCarMarkerOnMap()
    {
        MarkerOptions markerOption=new MarkerOptions()
                .position(new LatLng(latitude_val,longitude_val))
                .title("This is the car!")
                //.snippet("Our Ford Car")
                .icon(BitmapDescriptorFactory.fromResource( R.drawable.blue_circle) );
        marker = mGoogleMap.addMarker(markerOption);
        //marker.showInfoWindow();
        //marker.setDraggable(true);
        return;
    }
    public void showCircleOnMap()
    {
        // Draw a circle around car.
        CircleOptions circleOption = new CircleOptions()
                .center(new LatLng(latitude_val,longitude_val))
                .radius(PROXIMITY_RADIUS)
                .fillColor(0x11FF0000)
                .strokeColor(Color.BLUE)
                .strokeWidth(2);
        circle = mGoogleMap.addCircle(circleOption);
    }
    public void asyncGetNearbyGasStation()
    {
        Object[] DataTransfer = new Object[5];
        DataTransfer[0]=mGoogleMap;
        DataTransfer[1]=latitude_val;
        DataTransfer[2]=longitude_val;
        DataTransfer[3]=mNearbyDataView;
        DataTransfer[4]=PROXIMITY_RADIUS;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new GetNearbyGasStation(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataTransfer);
        }
        else {
            new GetNearbyGasStation(this).execute(DataTransfer);
        }
    }
    public void asyncGetDirectionToGasStation()
    {
        Object[] DataTransfer = new Object[3];
        DataTransfer[0]=new LatLng(latitude_val,longitude_val);
        DataTransfer[1]=destinationLatLng;
        DataTransfer[2]=mGoogleMap;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {

            new DirectionFinder(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataTransfer);
        }
        else
        {
            new DirectionFinder(this).execute(DataTransfer);
        }
    }


    @Override
    public void nearByGasStationTransfer(LatLng nearestGSLatLng)
    {
        destinationLatLng=nearestGSLatLng;

    }
    public LatLng test;
    @Override
    public void directionFinderTransfer(LatLng l)
    {
        test=l;
    }
}
