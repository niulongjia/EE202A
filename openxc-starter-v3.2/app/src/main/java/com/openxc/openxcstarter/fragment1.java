package com.openxc.openxcstarter;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.openxcplatform.openxcstarter.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class fragment1 extends Fragment implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        //GetNearbyGasStation.AsyncResponseGasStation,
        DirectionFinder.AsyncResponseDirectionFinder,
        GasDirectionFinder.AsyncResponseGasDirectionFinder
{

    /*** GOOGLE_PLACE_API_KEY here is only for autocomplete. ***/

    private static final String GOOGLE_PLACE_API_KEY="AIzaSyCVQyng9aT7sk_tTmUVB5ijYyXx3u8rIeM";
    private static final String GOOGLE_AUTOCOMPLETE_URL_BEGIN= "https://maps.googleapis.com/maps/api/place/autocomplete/json?";
    private static final String LOG_TAG = "Google Places Autocomplete";

    public MapView mapView;
    public GoogleMap googleMap;
    public GoogleApiClient googleApiClient;
    public LocationRequest locationRequest;
    // location of the car.
    // Default Drake Stadium UCLA.
    public double latitude_val=34.071985;
    public double longitude_val=-118.44825;

    // location of the nearest gas station (Used for "find gas" button).
    public LatLng destinationLatLng;

    // location of start and end address (Used for "find path" button).
    public double currentLatitude=0.0;
    public double currentLongitude=0.0;
    public boolean mapFirstOpened=true;
    public String startAddrName;
    public String endAddrName;
    public LatLng startAddrLatLng;
    public LatLng endAddrLatLng;

    public Marker marker;
    public Circle circle;
    public Polyline polylineD;
    public boolean isSuccessful=true;
    public String timeString;
    public String distString;
    public Route routes;
    public List<HashMap<String,String>> nearbyPlacesList_unique=new ArrayList<HashMap<String, String>>();
    public List<Marker> markerList=new ArrayList<Marker>();

    public List<Polyline> polylineGList=new ArrayList<Polyline>();
    public Polyline polylineGBest;
    public Route routeGBest;
    public int timeGBest;
    public List<Route> routesGList=new ArrayList<Route>();
    public List<Integer> timeGList = new ArrayList<Integer>();
    //public Route routesG;
    //public int timeG;

    private static final double PROXIMITY_RADIUS=1000.0;

    public Button mfindPathBtn;
    public boolean findPathClicked=false;
    public Button mfindGasStationBtn;
    public Button mcurrentLocation;
    public Button mcurrentLocation2;

    public AutoCompleteTextView autoCompView1;
    public AutoCompleteTextView autoCompView2;
    public Button mclearAutoStartBtn;
    public Button mclearAutoEndBtn;

    public TextView timeTextView;
    public TextView distTextView;


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

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            checkLocationPermission();
        }
        /*** Use mapView inside fragment ***/
        mapView=(MapView)view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        /*** needed to get the map to display immediately ***/
        mapView.onResume();
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapView.getMapAsync(this);


        /***
        These contents including AutoCompleteTextView can be found on:
        https://examples.javacodegeeks.com/android/android-google-places-autocomplete-api-example/
         ***/
        autoCompView1 = (AutoCompleteTextView) view.findViewById(R.id.autoComplete1);
        autoCompView1.setAdapter(new GooglePlacesAutocompleteAdapter(getActivity(), R.layout.list_item));
        autoCompView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                startAddrName = (String) adapterView.getItemAtPosition(position);
                Toast.makeText(getActivity(), "Start Address Name:"+startAddrName, Toast.LENGTH_SHORT).show();
            }
        });

        /***
        These contents including AutoCompleteTextView can be found on:
        https://examples.javacodegeeks.com/android/android-google-places-autocomplete-api-example/
         ***/
        autoCompView2 = (AutoCompleteTextView) view.findViewById(R.id.autoComplete2);
        autoCompView2.setAdapter(new GooglePlacesAutocompleteAdapter(getActivity(), R.layout.list_item));
        autoCompView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                endAddrName = (String) adapterView.getItemAtPosition(position);
                Toast.makeText(getActivity(), "End Address Name:"+endAddrName, Toast.LENGTH_SHORT).show();
            }
        });

        mfindGasStationBtn = (Button)view.findViewById(R.id.findGasStation);
        mfindPathBtn=(Button)view.findViewById(R.id.findPath);
        mcurrentLocation=(Button)view.findViewById(R.id.currentLocation);
        mcurrentLocation2=(Button)view.findViewById(R.id.currentLocation2);

        mclearAutoStartBtn=(Button)view.findViewById(R.id.clearAutoStart);
        mclearAutoEndBtn=(Button)view.findViewById(R.id.clearAutoEnd);

        timeTextView=(TextView)view.findViewById(R.id.textView4);
        distTextView=(TextView)view.findViewById(R.id.textView5);

        mcurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoCompView1.setText(Double.toString(currentLatitude)+","+Double.toString(currentLongitude));
                startAddrName=Double.toString(currentLatitude)+","+Double.toString(currentLongitude);
            }
        });
        mcurrentLocation2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoCompView2.setText(Double.toString(currentLatitude)+","+Double.toString(currentLongitude));
                endAddrName = Double.toString(currentLatitude)+","+Double.toString(currentLongitude);
            }
        });
        /*** This is the function when you press "find path" button ***/
        mfindPathBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                findPathClicked=true;
                /*** Clean previous polylines and markers ***/
                if (polylineD!=null) polylineD.remove();

                /*** polylineGBest is the best polyline selected from polylineGList ***/
                if (polylineGBest!=null) polylineGBest.remove();

                /*** polylineGList stores all the polylines generated from gasDirectionFinder ***/
                if (polylineGList!=null && polylineGList.size()>0)
                {
                    for (int i=0;i<polylineGList.size();i++) polylineGList.get(i).remove();
                    polylineGList.clear();
                }
                routes=new Route();
                routesGList.clear();
                timeGList.clear();

                /*** markerList stores all the red markers ***/
                if (markerList!=null && markerList.size()>0)
                {
                    for (int i=0;i<markerList.size();i++) markerList.get(i).remove();
                    markerList.clear();
                }

                /*** nearbyPlacesList_unique (no duplicates) stores all the nearby gas station info
                     from startAddrName to endAddrName ***/
                if (nearbyPlacesList_unique!=null) nearbyPlacesList_unique.clear();

                if (startAddrName==null || startAddrName.isEmpty()) { Toast.makeText(getActivity(), "Please enter origin address!", Toast.LENGTH_LONG).show();}
                else if (endAddrName==null || endAddrName.isEmpty()) { Toast.makeText(getActivity(), "Please enter destination address!", Toast.LENGTH_LONG).show();}
                else
                {
                    /*** This function is to generate and draw route from startAddrName to endAddrName ***/
                    /*** It will transfer { polylineD, routes, nearbyPlacesList_unique, markerList } back ***/
                    asyncGetDirectionFromStartToEnd();
                }


            }
        });
        /*** This is the function when you press "find gas" button ***/
        mfindGasStationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (findPathClicked==false) { Toast.makeText(getActivity(), "Press the \"FIND PATH\" first before press \"FIND GAS\"!", Toast.LENGTH_LONG).show();}
                else if (startAddrName==null || startAddrName.isEmpty()) { Toast.makeText(getActivity(), "Please enter origin address and press \"FIND PATH\" first!", Toast.LENGTH_LONG).show(); }
                else if (endAddrName==null || endAddrName.isEmpty()) { Toast.makeText(getActivity(), "Please enter destination address and press \"FIND PATH\" first!", Toast.LENGTH_LONG).show(); }
                else
                {
                    findPathClicked=false;
                    if (polylineD!=null) polylineD.remove();
                    if (polylineGBest!=null) polylineGBest.remove();
                    if (polylineGList!=null && polylineGList.size()>0)
                    {
                        for (int i=0;i<polylineGList.size();i++) polylineGList.get(i).remove();
                        polylineGList.clear();
                    }
                    /*** We already got nearbyPlacesList_unique, routes is not necessary ***/
                    routes=new Route();
                    routesGList.clear();
                    timeGList.clear();
                    /*** We will iterate nearbyPlacesList_unique to go through every gas station
                     and find the best path through one of them ***/

                    if (nearbyPlacesList_unique.size()==0)
                        Toast.makeText(getActivity(), "Can not find gas stations nearby!", Toast.LENGTH_LONG).show();
                    for (int i=0;i<nearbyPlacesList_unique.size();i++)
                    {
                        HashMap<String,String> nearbyPlace=nearbyPlacesList_unique.get(i);
                        asyncGetDirectionViaGasStation(nearbyPlace.get("vicinity"));
                    }
                }
            }
        });
        mclearAutoStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoCompView1.setText("");
                startAddrName=null;
            }
        });
        mclearAutoEndBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoCompView2.setText("");
                endAddrName=null;
            }
        });
        /** Inflate the layout for this fragment ***/
        return view;
    }

    /*** transfer data from fragment1 to DirectionFinder ***/
    /*** startAddrName -> endAddrName ***/
    public void asyncGetDirectionFromStartToEnd()
    {


        Object[] DataTransfer = new Object[10];
        /*** This is the start address ***/
        DataTransfer[0] = startAddrName;
        /*** This is the end address ***/
        DataTransfer[1] = endAddrName;
        DataTransfer[2]=googleMap;
        DataTransfer[3]=1; /*** for finding path.no use should be discarded ***/
        DataTransfer[4]=timeTextView;
        DataTransfer[5]=distTextView;
        DataTransfer[6]=PROXIMITY_RADIUS;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            new DirectionFinder(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataTransfer);
        }
        else
        {
            new DirectionFinder(this).execute(DataTransfer);
        }

    }

    /*** transfer data from fragment1 to GasDirectionFinder ***/
    /*** startAddrName -> gasStationAddrName -> endAddrName ***/
    public void asyncGetDirectionViaGasStation(String gasStationAddrName)
    {
        if (startAddrName==null || startAddrName.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter origin address!", Toast.LENGTH_LONG).show();
            return;
        }
        if (endAddrName==null || endAddrName.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter destination address!", Toast.LENGTH_LONG).show();
            return;
        }
        if (gasStationAddrName==null || gasStationAddrName.length()==0)
        {
            Toast.makeText(getActivity(), "Can find gas station nearby /nButGas station information empty!", Toast.LENGTH_LONG).show();
            return;
        }
        Object[] DataTransfer = new Object[10];
        DataTransfer[0]=startAddrName;
        DataTransfer[1]=gasStationAddrName; /*** startAddrName -> gasStationAddrName -> endAddrName ***/
        DataTransfer[2]=googleMap;
        DataTransfer[3]=0; /*** for finding gas station. no use should be discarded ***/
        DataTransfer[4]=timeTextView;
        DataTransfer[5]=distTextView;
        DataTransfer[6]=endAddrName;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            new GasDirectionFinder(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataTransfer);
        }
        else
        {
            new GasDirectionFinder(this).execute(DataTransfer);
        }
    }


    /*** transfer from DirectionFinder back to fragment1 ***/
    @Override
    public void directionFinderTransfer(Object[] transferD)
    {
        polylineD=(Polyline) transferD[0];
        routes=(Route)transferD[1];
        nearbyPlacesList_unique=(List<HashMap<String, String>>) transferD[2];
        markerList.addAll((List<Marker>) transferD[3]);
    }
    /*** transfer from GasDirectionFinder back to fragment1 ***/
    @Override
    public void gasDirectionFinderTransfer(Object[] transferG)
    {
        polylineGList.add((Polyline) transferG[0]);
        routesGList.add((Route)transferG[1]);
        timeGList.add((int) transferG[2]);

        /*** After we received all the data from GasDirectionFinder
             we begin to find the best(currently with min time) one. ***/
        int sz=nearbyPlacesList_unique.size();
        if (  routesGList.size()==sz
                && polylineGList.size()==sz
                && timeGList.size()==sz)
        {
            /*** Iterate through all the time and find the minimum one ***/
            for (int i=0;i<timeGList.size();i++)
            {
                if (i==0)
                {
                    timeGBest=timeGList.get(i);
                    //polylineGBest=polylineGList.get(i);
                    routeGBest=routesGList.get(i);
                }
                else if (timeGList.get(i)<timeGBest)
                {
                    timeGBest=timeGList.get(i);
                    //polylineGBest=polylineGList.get(i);
                    routeGBest=routesGList.get(i);
                }
            }
            PolylineOptions polyOption=new PolylineOptions()
                    .addAll(routeGBest.getPoints())
                    .color(Color.RED)
                    .width(5);
            polylineGBest=googleMap.addPolyline(polyOption);

            timeTextView.setText("Time:"+routeGBest.getDuration().getText());
            distTextView.setText("Dist:"+routeGBest.getDistance().getText());
        }

    }



    @Override
    public void onStart()
    {
        super.onStart();
        //EventBus.getDefault().register(this);
    }

    @Override
    public void onStop()
    {
        //EventBus.getDefault().unregister(this);
        super.onStop();

    }

    /*@Subscribe
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
                    //asyncGetNearbyGasStation();

                    if (destinationLatLng!=null)
                        //mNearbyDataView.setText(Double.toString(destinationLatLng.latitude)+"," +Double.toString(destinationLatLng.longitude));

                    showCircleOnMap();

                    //Log.d("debugMsg2", "finish one listening");
                    listener_cnt=0;
                }
                else listener_cnt++;

            }
        });

    }*/

    /*** http://www.androidtutorialpoint.com/intermediate/android-map-app-showing-current-location-android/ ***/
    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000); //in milliseconds.
        locationRequest.setFastestInterval(1000); //in milliseconds.
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location)
    {
        //Toast.makeText(getActivity(), "Get current location successful!", Toast.LENGTH_LONG).show();
        //Log.d("debugMsg13","latitude:"+location.getLatitude()+" longitude:"+location.getLongitude());
        currentLatitude=location.getLatitude();
        currentLongitude=location.getLongitude();
        if (mapFirstOpened==true)
        {
            LatLng currentLL=new LatLng(currentLatitude,currentLongitude);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLL, 14));
            mapFirstOpened=false;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        this.googleMap=googleMap;

        //showCarMarkerOnMap();

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                googleMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            googleMap.setMyLocationEnabled(true);
        }

        //showCircleOnMap();
    }
    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }
/*
   // show the marker of car.
    public void showCarMarkerOnMap(LatLng ll)
    {
        MarkerOptions markerOption=new MarkerOptions()
                .position(ll)
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
    */

    // remove marker and circle on map.
    public void removeMarkerCircle()
    {
        marker.remove();
        circle.remove();
    }

    /*** http://www.androidtutorialpoint.com/intermediate/android-map-app-showing-current-location-android/ ***/
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }
    /*** http://www.androidtutorialpoint.com/intermediate/android-map-app-showing-current-location-android/ ***/
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (googleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        googleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(getActivity(), "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    /***
     These contents including AutoCompleteTextView can be found on:
     https://examples.javacodegeeks.com/android/android-google-places-autocomplete-api-example/
     ***/
    public static ArrayList autocomplete(String input)
    {
        ArrayList resultList = null;
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(GOOGLE_AUTOCOMPLETE_URL_BEGIN);
            sb.append("key=" + GOOGLE_PLACE_API_KEY);
            sb.append("&components=country:US");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));


            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            //Log.e(LOG_TAG, "Error processing Places API URL",e);
            return resultList;
        } catch (IOException e) {
            //Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {

            // Create a JSON object hierarchy from the results

            JSONObject jsonObj = new JSONObject(jsonResults.toString());

            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            //Log.e(LOG_TAG, "Cannot process JSON results", e);
        }
        return resultList;
    }

    /***
     These contents including AutoCompleteTextView can be found on:
     https://examples.javacodegeeks.com/android/android-google-places-autocomplete-api-example/
     ***/
    class GooglePlacesAutocompleteAdapter extends ArrayAdapter implements Filterable {

        private ArrayList resultList;
        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId)
        {

            super(context, textViewResourceId);
        }
        @Override
        public int getCount() {
            return resultList.size();
        }
        @Override
        public String getItem(int index) {
            return (String)resultList.get(index);
        }
        @Override
        public Filter getFilter() {
            Filter filter = new Filter()
            {
                @Override
                protected FilterResults performFiltering(CharSequence constraint)
                {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        /** Retrieve the autocomplete results.***/
                        resultList = autocomplete(constraint.toString());
                        /*** Assign the data to the FilterResults ***/
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }

    }

}
