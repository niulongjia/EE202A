package com.openxc.openxcstarter;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by niulongjia on 2016/10/26.
 */

public class DirectionFinder extends AsyncTask<Object,String,String>
{
    private static final String GOOGLE_DIRECTION_URL_BEGIN="https://maps.googleapis.com/maps/api/directions/json?";
    private static final String GOOGLE_DIRECTION_API_KEY="AIzaSyBh_tIpUOiMtKf3N72GwZmiLRfgWuHdto8";
    public LatLng originLatLng;
    private LatLng destinationLatLng;
    private GoogleMap mGoogleMap;
    public Polyline polyline;
    public Object[] transferD=new Object[5];

    /****************The following is needed to transfer data back to mainActivity******************/
    // I do these simply because I want transfer data from Asyntask to StarterActivity
    // you may separate this or combined to caller class.
    public interface AsyncResponseDirectionFinder
    {
        void directionFinderTransfer(Object[] transferD);
    }

    public AsyncResponseDirectionFinder delegate = null;

    public DirectionFinder(AsyncResponseDirectionFinder delegate){
        this.delegate = delegate;
    }
    /****************The above is needed to transfer data back to mainActivity******************/

    @Override
    protected String doInBackground(Object[] objects)
    {
        originLatLng=(LatLng) objects[0];
        destinationLatLng=(LatLng) objects[1];
        mGoogleMap=(GoogleMap)objects[2];

        String directionUrl=getDirectionUrl(originLatLng,destinationLatLng);
        String jsonData=new ReadUrl().readFromUrl(directionUrl);
        return jsonData;
    }
    // This is used to get the url for direction between car and one gas station using google direction api.
    private String getDirectionUrl(LatLng originLatLng, LatLng destinationLatLng)
    {

        StringBuilder directionUrl= new StringBuilder(GOOGLE_DIRECTION_URL_BEGIN);
        directionUrl.append("origin="+originLatLng.latitude+","+originLatLng.longitude);
        directionUrl.append("&destination="+destinationLatLng.latitude+","+destinationLatLng.longitude);
        directionUrl.append("&mode=driving");
        directionUrl.append("&key="+GOOGLE_DIRECTION_API_KEY);
        return  (directionUrl.toString());
    }
    @Override
    protected void onPostExecute(String jsonData)
    {
        Route R=new Route();
        try {  R=new DataParser().parseGoogleDirectionApi(jsonData);  }
        catch (JSONException e) {  e.printStackTrace();  }

        showPolyLinesOnMap(R);

        //This is used to transfer data from AsyncTask to StarterActivity.
        transferD[0]=polyline;
        delegate.directionFinderTransfer(transferD);
    }

    private void showPolyLinesOnMap(Route R)
    {
        PolylineOptions polyOption=new PolylineOptions()
                .addAll(R.getPoints())
                .color(Color.BLUE)
                .width(5);
        polyline=mGoogleMap.addPolyline(polyOption);

    }


}
