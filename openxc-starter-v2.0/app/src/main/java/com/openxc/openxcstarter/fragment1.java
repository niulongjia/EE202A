package com.openxc.openxcstarter;


import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.openxcplatform.openxcstarter.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.w3c.dom.Text;

/**
 * A simple {@link Fragment} subclass.
 */
public class fragment1 extends Fragment implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GetNearbyGasStation.AsyncResponseGasStation,
        DirectionFinder.AsyncResponseDirectionFinder
{



    public MapView mapView;
    public GoogleMap googleMap;

    // location of the car.
    public double latitude_val=34.052235;
    public double longitude_val=-118.243683;

    // location of the nearest gas station.
    public LatLng destinationLatLng;

    public Marker marker;
    public Circle circle;
    public Polyline polylineD;

    //private static final int TIME_INTERVAL=3000;
    //private static final int FASTEST_TIME_INTERVAL=1500;
    private static final double PROXIMITY_RADIUS=800.0;

    public TextView mNearbyDataView;
    public Button mfindGasStationBtn;

    public int listener_cnt=0;
    public int listener_thres=3;

    public CameraPosition cp;
    public fragment1() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.layout_fragment1,container,false);

        mNearbyDataView=(TextView)view.findViewById(R.id.nearbyData);

        // Use mapView inside fragment
        mapView=(MapView)view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        // needed to get the map to display immediately
        mapView.onResume();
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapView.getMapAsync(this);




        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        EventBus.getDefault().register(this);
    }
    /*
    @Override
    public void onPause()
    {
        cp=googleMap.getCameraPosition();
        super.onPause();
    }
    @Override
    public void onResume()
    {
        if (cp != null) {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
            cp = null;
        }
        super.onResume();
    }
    */

    @Override
    public void onStop()
    {
        EventBus.getDefault().unregister(this);
        super.onStop();

    }
    @Subscribe
    public void onEvent(final TextChangedEvent textChangedEvent)
    {
        latitude_val=textChangedEvent.latitude_val;
        longitude_val=textChangedEvent.longitude_val;

        // use ui thread to update UI in real time
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                if (listener_cnt>=listener_thres) {
                    LatLng ll = new LatLng(latitude_val, longitude_val);
                    //Log.d("debugMsg", Double.toString(latitude_val));
                    //Log.d("debugMsg", Double.toString(longitude_val));
                    if (googleMap != null)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 14));

                    // clear previous markers!
                    removeMarkerCircle();

                    showCarMarkerOnMap();

                    // Given the car latlng, find nearest gas station.
                    asyncGetNearbyGasStation();

                    if (destinationLatLng!=null)
                        mNearbyDataView.setText(Double.toString(destinationLatLng.latitude)+"," +Double.toString(destinationLatLng.longitude));

                    showCircleOnMap();

                    //Log.d("debugMsg2", "finish one listening");
                    listener_cnt=0;
                }
                else listener_cnt++;

            }
        });

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        this.googleMap=googleMap;

        showCarMarkerOnMap();

        LatLng ll=new LatLng(latitude_val,longitude_val);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll,14 ));

        showCircleOnMap();


        mfindGasStationBtn = (Button)getActivity().findViewById(R.id.findGasStation);
        mfindGasStationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (polylineD!=null) polylineD.remove();
                mNearbyDataView.setText(Double.toString(destinationLatLng.latitude)+"," +Double.toString(destinationLatLng.longitude));

                // given the car latlng, and destination latlng, find route using google direction api.
                asyncGetDirectionToGasStation();

            }
        });


    }
    // show the marker of car.
    public void showCarMarkerOnMap()
    {
        MarkerOptions markerOption=new MarkerOptions()
                .position(new LatLng(latitude_val,longitude_val))
                .title("This is the car!")
                //.snippet("Our Ford Car")
                .icon(BitmapDescriptorFactory.fromResource( R.drawable.blue_circle) );
        marker = googleMap.addMarker(markerOption);
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
        circle = googleMap.addCircle(circleOption);
    }

    // remove marker and circle on map.
    public void removeMarkerCircle()
    {
        marker.remove();
        circle.remove();
        //mGoogleMap.clear();
    }

    // These are used to transfer data from asyncTask back to fragment1.
    public void asyncGetNearbyGasStation()
    {
        Log.d("debugMsg2","inside asynTask");
        Object[] DataTransfer = new Object[5];
        DataTransfer[0]=googleMap;
        DataTransfer[1]=latitude_val;
        DataTransfer[2]=longitude_val;
        DataTransfer[3]=mNearbyDataView;
        DataTransfer[4]=PROXIMITY_RADIUS;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            //getNearbyGasStation.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataTransfer);
            new GetNearbyGasStation(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataTransfer);
        }
        else {
            //getNearbyGasStation.execute(DataTransfer);
            new GetNearbyGasStation(this).execute(DataTransfer);
        }
    }
    public void asyncGetDirectionToGasStation()
    {
        Object[] DataTransfer = new Object[3];
        DataTransfer[0]=new LatLng(latitude_val,longitude_val);
        DataTransfer[1]=destinationLatLng;
        DataTransfer[2]=googleMap;
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
    public void nearByGasStationTransfer(Object[] transfer)
    {
        destinationLatLng=(LatLng)transfer[0];
        //polyline=(Polyline)transfer[1];
    }

    @Override
    public void directionFinderTransfer(Object[] transferD)
    {
        polylineD=(Polyline) transferD[0];
    }
}
