package com.openxc.openxcstarter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.sqrt;

/**
 * Created by niulongjia on 2016/10/26.
 */

public class GasDirectionFinder extends AsyncTask<Object,Integer,String>
{
    private static final String GOOGLE_DIRECTION_URL_BEGIN="https://maps.googleapis.com/maps/api/directions/json?";
    private static final String GOOGLE_DIRECTION_API_KEY="AIzaSyBh_tIpUOiMtKf3N72GwZmiLRfgWuHdto8";
    public LatLng originLatLng;
    private LatLng destinationLatLng;
    private String startAddrName;
    private String gasStationAddrName;
    private String endAddrName;
    private GoogleMap mGoogleMap;
    private LatLng nearestLatLng;

    private TextView timeTextView;
    private TextView distTextView;
    private HashMap<String,String> nearbyPlace;

    private int sign;
    private Route routes=new Route();
    public Polyline polyline;
    public int time; // in seconds.
    public String toGasJsonData;
    //public boolean isSuccessful;
    public Object[] transferG=new Object[10];

    /*** I do these simply because I want transfer data from Asyntask to StarterActivity
         you may separate this or combined to caller class. ***/
    public interface AsyncResponseGasDirectionFinder
    {
        void gasDirectionFinderTransfer(Object[] transferD);
    }

    public AsyncResponseGasDirectionFinder delegate = null;

    public GasDirectionFinder(AsyncResponseGasDirectionFinder delegate){
        this.delegate = delegate;
    }

    @Override
    protected String doInBackground(Object[] objects)
    {
        startAddrName = (String) objects[0];
        gasStationAddrName = (String) objects[1];
        mGoogleMap=(GoogleMap)objects[2];
        sign=(int)objects[3];
        timeTextView=(TextView)objects[4];
        distTextView=(TextView)objects[5];
        endAddrName=(String) objects[6];

        /*** startAddrName -> gasStationAddrName -> endAddrName ***/
        String viaGasDirectionUrl=getViaGasDirectionUrl(startAddrName, gasStationAddrName, endAddrName);
        String viaGasJsonData=new ReadUrl().readFromUrl(viaGasDirectionUrl);

        /*** startAddrName -> gasStationAddrName ***/
        String toGasDirectionUrl=getToGasDirectionUrl(startAddrName,gasStationAddrName);
        toGasJsonData=new ReadUrl().readFromUrl(toGasDirectionUrl);

        return viaGasJsonData;
    }
    @Override
    protected void onPostExecute(String jsonData)
    {
        routes=new Route();
        try
        {
            routes=new DataParser().parseGoogleDirectionApi(jsonData);
        }
        catch (JSONException e) {  e.printStackTrace();  }
        if (routes.getId()==1)
        {
            //isSuccessful=true;
            showPolyLinesOnMap(routes);
            /*** do not update timeTextView and distTextView now
                update them after comparing all the time in gragment1 ***/
            time=routes.getDuration().getValue();
        }
        else
        {
            //isSuccessful=false;
            Log.d("debugMsg0","start:"+startAddrName+" gas:"+gasStationAddrName+" end:"+endAddrName);
            timeTextView.setText("Time:xxx(Try Again!)");
            distTextView.setText("Dist:xxx(Try Again!)");
            time=-1;
        }

        transferG[0]=polyline;
        transferG[1]=routes;
        transferG[2]=time;
        transferG[3]=toGasJsonData;
        delegate.gasDirectionFinderTransfer(transferG);
    }



    /*** get direction url for startAddrName -> gasStationAddrName -> endAddrName ***/
    private String getViaGasDirectionUrl(String startAddrName, String gasStationAddrName, String endAddrName)
    {
        String urlOrigin = null;
        String urlDestination=null;
        String urlGasStation=null;

        try
        {
            urlOrigin = URLEncoder.encode(startAddrName, "utf-8");
            urlDestination = URLEncoder.encode(endAddrName, "utf-8");
            urlGasStation = URLEncoder.encode(gasStationAddrName, "utf-8");
        }
        catch (UnsupportedEncodingException e) { e.printStackTrace();}


        StringBuilder directionUrl= new StringBuilder(GOOGLE_DIRECTION_URL_BEGIN);
        directionUrl.append("origin="+urlOrigin);
        directionUrl.append("&destination="+urlDestination);
        directionUrl.append("&mode=driving");
        /*** adding waypoints parameter ***/
        directionUrl.append("&waypoints="+urlGasStation);
        directionUrl.append("&key="+GOOGLE_DIRECTION_API_KEY);
        return  (directionUrl.toString());
    }
    /*** This is used to get the url for direction between start Address and end Address.
     Using google direction api. ***/
    private String getToGasDirectionUrl(String startAddrName, String endAddrName)
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
    public double calculateDist(LatLng lineBegin, LatLng lineEnd)
    {
        double x1=lineBegin.latitude;
        double y1=lineBegin.longitude;
        double x2=lineEnd.latitude;
        double y2=lineEnd.longitude;
        float[] results = new float[1];
        Location.distanceBetween(x1,y1,x2,y2,results);
        return results[0];
    }

    private void showPolyLinesOnMap(Route R)
    {
        if (polyline!=null) Log.d("debugMsg2","before draw poly line"+Integer.toString(polyline.getPoints().size()));
        PolylineOptions polyOption=new PolylineOptions()
                .addAll(R.getPoints())
                .color(Color.BLUE)
                .width(5);
        polyline=mGoogleMap.addPolyline(polyOption);
        /*** we do not want to show all the lines, only show best route via gas station in fragment1 ***/
        polyline.setVisible(false);
        Log.d("debugMsg2","after draw poly line"+Integer.toString(polyline.getPoints().size()));

    }
}
