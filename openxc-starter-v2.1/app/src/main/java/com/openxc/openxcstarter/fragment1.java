package com.openxc.openxcstarter;


import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
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
import java.util.List;

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

    private static final String GOOGLE_PLACE_API_KEY="AIzaSyCVQyng9aT7sk_tTmUVB5ijYyXx3u8rIeM";
    private static final String GOOGLE_AUTOCOMPLETE_URL_BEGIN= "https://maps.googleapis.com/maps/api/place/autocomplete/json?";
    private static final String LOG_TAG = "Google Places Autocomplete";

    public MapView mapView;
    public GoogleMap googleMap;

    // location of the car.
    // Default Drake Stadium UCLA.
    public double latitude_val=34.071985;
    public double longitude_val=-118.44825;

    // location of the nearest gas station (Used for "find gas" button).
    public LatLng destinationLatLng;

    // location of start and end address (Used for "find path" button).
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

    //private static final int TIME_INTERVAL=3000;
    //private static final int FASTEST_TIME_INTERVAL=1500;
    private static final double PROXIMITY_RADIUS=800.0;

    //public TextView mNearbyDataView;
    public Button mfindGasStationBtn;
    public Button mfindPathBtn;
    public AutoCompleteTextView autoCompView1;
    public AutoCompleteTextView autoCompView2;
    public Button clearAutoStartBtn;
    public Button clearAutoEndBtn;

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


        /*
        These contents including AutoCompleteTextView can be found on:
        https://examples.javacodegeeks.com/android/android-google-places-autocomplete-api-example/
         */
        autoCompView1 = (AutoCompleteTextView) view.findViewById(R.id.autoComplete1);
        autoCompView1.setAdapter(new GooglePlacesAutocompleteAdapter(getActivity(), R.layout.list_item));
        autoCompView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                startAddrName = (String) adapterView.getItemAtPosition(position);
                Toast.makeText(getActivity(), "Start Address Name:"+startAddrName, Toast.LENGTH_SHORT).show();
            }
        });

        /*
        These contents including AutoCompleteTextView can be found on:
        https://examples.javacodegeeks.com/android/android-google-places-autocomplete-api-example/
         */
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
        clearAutoStartBtn=(Button)view.findViewById(R.id.clearAutoStart);
        clearAutoEndBtn=(Button)view.findViewById(R.id.clearAutoEnd);

        timeTextView=(TextView)view.findViewById(R.id.textView4);
        distTextView=(TextView)view.findViewById(R.id.textView5);

        mfindPathBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (polylineD!=null) polylineD.remove();
                asyncGetDirectionFromStartToEnd();

            }
        });
        mfindGasStationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (polylineD!=null) polylineD.remove();
                asyncGetDirectionToGasStation();


            }
        });
        clearAutoStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoCompView1.setText("");
                startAddrName=null;
            }
        });
        clearAutoEndBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoCompView2.setText("");
                endAddrName=null;
            }
        });
        // Inflate the layout for this fragment
        return view;
    }


    /*
    These contents including AutoCompleteTextView can be found on:
    https://examples.javacodegeeks.com/android/android-google-places-autocomplete-api-example/
    */
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

    /*
    These contents including AutoCompleteTextView can be found on:
    https://examples.javacodegeeks.com/android/android-google-places-autocomplete-api-example/
     */
    class GooglePlacesAutocompleteAdapter extends ArrayAdapter implements Filterable {

        private ArrayList resultList;
        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId)
        {

            super(context, textViewResourceId);
            //Log.d("debugMsg4","Inside GooglePlacesAutoCompleteAdapter");
        }
        @Override
        public int getCount() {
            //Log.d("debugMsg4","Inside GooglePlacesAutoCompleteAdapter getCount()");
            return resultList.size();
        }
        @Override
        public String getItem(int index) {
            //Log.d("debugMsg4","Inside GooglePlacesAutoCompleteAdapter getItem");
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
                        //Log.d("debugMsg4","before Autocomplete");
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());
                        // Assign the data to the FilterResults
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
        latitude_val=textChangedEvent.latitude_val;
        longitude_val=textChangedEvent.longitude_val;

        // use ui thread to update UI in real time
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                if (listener_cnt>=listener_thres) {
                    LatLng ll = new LatLng(latitude_val, longitude_val);
                    if (googleMap != null)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 14));

                    // clear previous markers!
                    removeMarkerCircle();

                    showCarMarkerOnMap();

                    // Given the car latlng, find nearest gas station.
                    asyncGetNearbyGasStation();

                    if (destinationLatLng!=null)
                        //mNearbyDataView.setText(Double.toString(destinationLatLng.latitude)+"," +Double.toString(destinationLatLng.longitude));

                    showCircleOnMap();

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
        Object[] DataTransfer = new Object[4];
        DataTransfer[0]=googleMap;
        DataTransfer[1]=latitude_val;
        DataTransfer[2]=longitude_val;
        //DataTransfer[3]=mNearbyDataView;
        DataTransfer[3]=PROXIMITY_RADIUS;
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

        if (destinationLatLng==null)
        {
            Toast.makeText(getActivity(), "Can not find gas stations nearby!", Toast.LENGTH_SHORT).show();
            return;
        }
        Object[] DataTransfer = new Object[6];
        DataTransfer[0]=new LatLng(latitude_val,longitude_val);
        DataTransfer[1]=destinationLatLng;
        DataTransfer[2]=googleMap;
        DataTransfer[3]=0; // for finding gas station.
        DataTransfer[4]=timeTextView;
        DataTransfer[5]=distTextView;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {

            new DirectionFinder(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataTransfer);
        }
        else
        {
            new DirectionFinder(this).execute(DataTransfer);
        }
    }

    public void asyncGetDirectionFromStartToEnd()
    {
        if (startAddrName==null || startAddrName.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter origin address!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endAddrName==null || endAddrName.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter destination address!", Toast.LENGTH_SHORT).show();
            return;
        }


        Object[] DataTransfer = new Object[6];
        //This is the start address
        DataTransfer[0] = startAddrName;
        //This is the end address
        DataTransfer[1] = endAddrName;
        DataTransfer[2]=googleMap;
        DataTransfer[3]=1; // for finding path
        DataTransfer[4]=timeTextView;
        DataTransfer[5]=distTextView;
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
    }

    @Override
    public void directionFinderTransfer(Object[] transferD)
    {
        polylineD=(Polyline) transferD[0];
    }
}
