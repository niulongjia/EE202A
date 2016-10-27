package com.openxc.openxcstarter;

import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebStorage;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.openxcplatform.openxcstarter.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InterfaceAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED;
import static com.openxcplatform.openxcstarter.R.drawable.red_circle;

/**
 * Created by niulongjia on 2016/10/25.
 */

public class GetNearbyGasStation extends AsyncTask<Object,String,String>{
    //private static final int PROXIMITY_RADIUS=800;


    private static final String GOOGLE_PLACE_API_KEY="AIzaSyCVQyng9aT7sk_tTmUVB5ijYyXx3u8rIeM";
    private static final String GOOGLE_PLACE_URL_BEGIN= "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";


    private GoogleMap mgoogleMap;
    private double latitude_val;
    private double longitude_val;
    public LatLng nearestGSLatLng;
    private TextView textView;
    private double PROXIMITY_RADIUS;

    // I do these simply because I want transfer data from Asyntask to StarterActivity
    // you may separate this or combined to caller class.
    public interface AsyncResponseGasStation
    {
        void nearByGasStationTransfer(LatLng nearestGSLatLng);
    }

    public AsyncResponseGasStation delegate = null;

    public GetNearbyGasStation(AsyncResponseGasStation delegate){
        this.delegate = delegate;
    }

    // objects stores googleMap, url string
    // This method takes in url string to retrieve JSON data.
    @Override
    protected String doInBackground(Object[] objects)
    {
        mgoogleMap=(GoogleMap) objects[0];
        latitude_val=(double) objects[1];
        longitude_val=(double) objects[2];
        //urlString=(String) objects[1];
        textView=(TextView)objects[3];
        PROXIMITY_RADIUS=(Double)objects[4];

        String urlString=getGasStationUrl(latitude_val,longitude_val);
        String jsonData=new ReadUrl().readFromUrl(urlString);
        return jsonData;
    }
    // This method updates google map UI
    @Override
    protected void onPostExecute(String jsonData)
    {
        //textView.setText(jsonData);
        List<HashMap<String, String>> nearbyPlacesList =  new DataParser().parseGooglePlaceApi(jsonData);
        textView.setText("There are "+ Integer.toString(nearbyPlacesList.size())+" choices");
        ShowNearbyPlaces(nearbyPlacesList);

        Log.d("GooglePlacesReadTask", "onPostExecute Exit");
        //This is used to transfer data from AsyncTask to StarterActivity.
        delegate.nearByGasStationTransfer(nearestGSLatLng);
    }
    private void ShowNearbyPlaces(List<HashMap<String, String>> nearbyPlacesList)
    {
        HashMap<String, String> nearbyPlace=nearbyPlacesList.get(0);
        double lat = Double.parseDouble(nearbyPlace.get("lat"));
        double lng = Double.parseDouble(nearbyPlace.get("lng"));
        nearestGSLatLng = new LatLng(lat,lng);

        for (int i = 0; i < nearbyPlacesList.size(); i++) {
            Log.d("onPostExecute","Entered into showing locations");

            nearbyPlace = nearbyPlacesList.get(i);

            showNearbyMarkerOnMap(nearbyPlace);

            //do not move map camera for nearby gas stations.

            //mgoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat,lng)));
            //mgoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }
    }

    // This is used to get the url for the nearest gas station.
    // Using google place api.
    public String getGasStationUrl(double latitude, double longitude) {

        StringBuilder gasStationUrl = new StringBuilder(GOOGLE_PLACE_URL_BEGIN);
        gasStationUrl.append("location=" + latitude + "," + longitude);
        gasStationUrl.append("&radius=" + PROXIMITY_RADIUS);
        // Can not write as "&type=gas station", word separation will cause trouble !
        // For full details of type, see < https://developers.google.com/places/supported_types >
        gasStationUrl.append("&type=gas_station");
        gasStationUrl.append("&sensor=true");
        gasStationUrl.append("&key=" + GOOGLE_PLACE_API_KEY);
        Log.d("debugMsg", gasStationUrl.toString());
        return (gasStationUrl.toString());
    }


    public void showNearbyMarkerOnMap(HashMap<String, String> nearbyPlace)
    {
        double lat = Double.parseDouble(nearbyPlace.get("lat"));
        double lng = Double.parseDouble(nearbyPlace.get("lng"));

        String placeName = nearbyPlace.get("place_name");
        String vicinity = nearbyPlace.get("vicinity");

        Marker marker = mgoogleMap.addMarker
                (
                        new MarkerOptions()
                                .position(new LatLng(lat,lng))
                                .title(placeName + " : " + vicinity)
                                //.snippet(placeName + " : " + vicinity)
                                .icon(BitmapDescriptorFactory.defaultMarker(HUE_RED))
                );
        marker.showInfoWindow();
        marker.setDraggable(true);
        marker.setTag("Testing!");
    }

}
