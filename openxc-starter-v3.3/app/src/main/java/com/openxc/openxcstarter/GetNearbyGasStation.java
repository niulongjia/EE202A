package com.openxc.openxcstarter;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebStorage;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
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
import java.util.ArrayList;
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


    private GoogleMap mGoogleMap;
    private Circle circle;
    private Route routes;
    private LatLng pointOnLine;

    private int i;
    private int j;
    public LatLng nearestGSLatLng;
    //private TextView textView;
    private double PROXIMITY_RADIUS;
    private List<Marker> markerList=new ArrayList<Marker>();
    public Object[] transfer=new Object[10];

    /*** I do these simply because I want transfer data from Asyntask to StarterActivity
         you may separate this or combined to caller class. ***/
    public interface AsyncResponseGasStation
    {
        void nearByGasStationTransfer(Object[] transfer);
    }

    public AsyncResponseGasStation delegate = null;

    public GetNearbyGasStation(AsyncResponseGasStation delegate){
        this.delegate = delegate;
    }


    // This method takes in url string to retrieve JSON data.
    @Override
    protected String doInBackground(Object[] objects)
    {
        mGoogleMap=(GoogleMap) objects[0];
        pointOnLine=(LatLng) objects[1];
        PROXIMITY_RADIUS=(Double)objects[2];
        i=(int) objects[3];

        String urlString=getGasStationUrl(pointOnLine);
        String jsonData=new ReadUrl().readFromUrl(urlString);
        return jsonData;
    }
    // This method updates google map UI
    @Override
    protected void onPostExecute(String jsonData)
    {
        List<HashMap<String, String>> nearbyPlacesList =  new DataParser().parseGooglePlaceApi(jsonData);

        if (nearbyPlacesList==null || nearbyPlacesList.size()==0) return;
        //showCircleOnMap(pointOnLine);
        markerList.clear();
        ShowNearbyPlaces(nearbyPlacesList);

        /*** This is used to transfer data from GetNearbyGasStation to DirectionFinder ***/
        transfer[0] = nearestGSLatLng;
        transfer[1] = nearbyPlacesList;
        transfer[2] = markerList;
        delegate.nearByGasStationTransfer(transfer);
    }
    /*** show nearby gas stations on map ***/
    private void ShowNearbyPlaces(List<HashMap<String, String>> nearbyPlacesList)
    {

        for (int i = 0; i < nearbyPlacesList.size(); i++) {

            HashMap<String, String> nearbyPlace = nearbyPlacesList.get(i);

            double lat = Double.parseDouble(nearbyPlace.get("lat"));
            double lng = Double.parseDouble(nearbyPlace.get("lng"));

            String placeName = nearbyPlace.get("place_name");
            String vicinity = nearbyPlace.get("vicinity");

            Marker marker = mGoogleMap.addMarker
                    (
                            new MarkerOptions()
                                    .position(new LatLng(lat,lng))
                                    .title(placeName + " : " + vicinity)
                                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.red_circle))
//                                    .icon(BitmapDescriptorFactory.defaultMarker(HUE_RED))
                    );
            marker.showInfoWindow();
            marker.setDraggable(true);
            marker.setTag("Gas Station!");

            //marker.setVisible(false);
            markerList.add(marker);

            //do not move map camera for nearby gas stations.
            //mgoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat,lng)));
            //mgoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }
    }

    // This is used to get the url for the nearest gas station.
    // Using google place api.
    public String getGasStationUrl(LatLng latLng) {

        StringBuilder gasStationUrl = new StringBuilder(GOOGLE_PLACE_URL_BEGIN);
        gasStationUrl.append("location=" + latLng.latitude + "," + latLng.longitude);
        gasStationUrl.append("&radius=" + PROXIMITY_RADIUS);
        /*** Can not write as "&type=gas station", word separation will cause trouble !
            For full details of type, see "https://developers.google.com/places/supported_types" ***/
        gasStationUrl.append("&type=gas_station");
        gasStationUrl.append("&language=en");
        gasStationUrl.append("&opennow=true");
        gasStationUrl.append("&sensor=true");
        gasStationUrl.append("&key=" + GOOGLE_PLACE_API_KEY);
        return (gasStationUrl.toString());
    }

    public void showCircleOnMap(LatLng center)
    {
        // Draw a circle around car.
        CircleOptions circleOption = new CircleOptions()
                .center(center)
                .radius(PROXIMITY_RADIUS)
                .fillColor(0x11FF0000)
                .strokeColor(Color.BLUE)
                .strokeWidth(2);
        circle = mGoogleMap.addCircle(circleOption);
    }

}
