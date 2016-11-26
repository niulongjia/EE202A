package com.openxc.openxcstarter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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

public class DirectionFinder extends AsyncTask<Object,Integer,String>
{
    private static final String GOOGLE_DIRECTION_URL_BEGIN="https://maps.googleapis.com/maps/api/directions/json?";
    private static final String GOOGLE_DIRECTION_API_KEY="AIzaSyBh_tIpUOiMtKf3N72GwZmiLRfgWuHdto8";
    public LatLng originLatLng;
    private LatLng destinationLatLng;
    private GoogleMap mGoogleMap;

    private String startAddrName;
    private String endAddrName;

    private TextView timeTextView;
    private TextView distTextView;

    private int sign;

    public Polyline polyline;
    public boolean isSuccessful;
    public Object[] transferD=new Object[5];

    // I do these simply because I want transfer data from Asyntask to StarterActivity
    // you may separate this or combined to caller class.
    public interface AsyncResponseDirectionFinder
    {
        void directionFinderTransfer(Object[] transferD);
    }

    public AsyncResponseDirectionFinder delegate = null;
    //public Context mContext;
    public DirectionFinder(AsyncResponseDirectionFinder delegate){
        this.delegate = delegate;
    }

    @Override
    protected String doInBackground(Object[] objects)
    {
        mGoogleMap=(GoogleMap)objects[2];
        sign=(int)objects[3];
        timeTextView=(TextView)objects[4];
        distTextView=(TextView)objects[5];

        String directionUrl=null;
        if (sign==0)
        {
            originLatLng=(LatLng) objects[0];
            destinationLatLng=(LatLng) objects[1];
            directionUrl=getGasDirectionUrl(originLatLng,destinationLatLng);
        }
        else if (sign==1)
        {
            startAddrName=(String)objects[0];
            endAddrName=(String)objects[1];
            directionUrl=getPathDirectionUrl(startAddrName,endAddrName);
        }

        String jsonData=new ReadUrl().readFromUrl(directionUrl);
        return jsonData;
    }
    protected void onProgressUpdate(Integer progress) {}
    @Override
    protected void onPostExecute(String jsonData)
    {
        Route R=new Route();
        try
        {
            R=new DataParser().parseGoogleDirectionApi(jsonData);
        }
        catch (JSONException e) {  e.printStackTrace();  }

        if (R.getId()==1)
        {
            isSuccessful=true;
            showPolyLinesOnMap(R);
            timeTextView.setText("Time:"+R.getDuration().getText());
            distTextView.setText("Dist:"+R.getDistance().getText());
        }
        else
        {
            isSuccessful=false;
            timeTextView.setText("Time:xxx(Try Again!)");
            distTextView.setText("Dist:xxx(Try Again!)");

        }

        transferD[0]=polyline;
        delegate.directionFinderTransfer(transferD);
    }

    private void showPolyLinesOnMap(Route R)
    {
        //polyline.remove();
        if (polyline!=null) Log.d("debugMsg2","before draw poly line"+Integer.toString(polyline.getPoints().size()));
        PolylineOptions polyOption=new PolylineOptions()
                .addAll(R.getPoints())
                .color(Color.BLUE)
                .width(5);
        polyline=mGoogleMap.addPolyline(polyOption);

    }

    // This is used to get the url for direction between car and one gas station.
    // Using google direction api.
    private String getGasDirectionUrl(LatLng originLatLng, LatLng destinationLatLng)
    {

        StringBuilder directionUrl= new StringBuilder(GOOGLE_DIRECTION_URL_BEGIN);
        directionUrl.append("origin="+originLatLng.latitude+","+originLatLng.longitude);
        directionUrl.append("&destination="+destinationLatLng.latitude+","+destinationLatLng.longitude);
        directionUrl.append("&mode=driving");
        directionUrl.append("&key="+GOOGLE_DIRECTION_API_KEY);
        return  (directionUrl.toString());
    }

    // This is used to get the url for direction between start Address and end Address.
    // Using google direction api.
    private String getPathDirectionUrl(String startAddrName, String endAddrName)
    {
        String urlOrigin = null;
        String urlDestination=null;

        try
        {
            urlOrigin = URLEncoder.encode(startAddrName, "utf-8");
            urlDestination = URLEncoder.encode(endAddrName, "utf-8");
        }
        catch (UnsupportedEncodingException e) { e.printStackTrace();}


        StringBuilder directionUrl= new StringBuilder(GOOGLE_DIRECTION_URL_BEGIN);
        directionUrl.append("origin="+urlOrigin);
        directionUrl.append("&destination="+urlDestination);
        directionUrl.append("&mode=driving");
        directionUrl.append("&key="+GOOGLE_DIRECTION_API_KEY);
        return  (directionUrl.toString());
    }
}
