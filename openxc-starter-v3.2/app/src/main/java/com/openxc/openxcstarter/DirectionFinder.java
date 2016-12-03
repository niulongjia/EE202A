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
import com.google.android.gms.maps.model.Marker;
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

public class DirectionFinder extends AsyncTask<Object,Integer,String> implements
        GetNearbyGasStation.AsyncResponseGasStation
{
    private static final String GOOGLE_DIRECTION_URL_BEGIN="https://maps.googleapis.com/maps/api/directions/json?";
    private static final String GOOGLE_DIRECTION_API_KEY="AIzaSyBh_tIpUOiMtKf3N72GwZmiLRfgWuHdto8";
    public LatLng originLatLng;
    private LatLng destinationLatLng;
    private GoogleMap mGoogleMap;
    private LatLng nearestLatLng;
    private List<HashMap<String, String>> nearbyPlacesList=new ArrayList<HashMap<String, String>>();
    private List<Marker> markerList = new ArrayList<Marker>();
    private Set<String> nearbyPlacesList_placeId=new HashSet<String>();
    private List<HashMap<String, String>> nearbyPlacesList_unique=new ArrayList<HashMap<String, String>>();

    private String startAddrName;
    private String endAddrName;

    private TextView timeTextView;
    private TextView distTextView;
    private double PROXIMITY_RADIUS;
    private int sign;
    private Route routes=new Route();
    public Polyline polyline;
    public boolean isSuccessful;
    public Object[] transferD=new Object[10];



    /*** I do these simply because I want transfer data from Asyntask to StarterActivity
         you may separate this or combined to caller class ***/
    public interface AsyncResponseDirectionFinder
    {
        void directionFinderTransfer(Object[] transferD);
    }

    public AsyncResponseDirectionFinder delegate = null;

    public DirectionFinder(AsyncResponseDirectionFinder delegate){
        this.delegate = delegate;
    }

    @Override
    protected String doInBackground(Object[] objects)
    {
/*        nearbyPlacesList_unique = new ArrayList<HashMap<String, String>>();
        nearbyPlacesList=new ArrayList<HashMap<String, String>>();
        nearbyPlacesList_placeId=new HashSet<String>();*/

        if (nearbyPlacesList_unique!=null) nearbyPlacesList_unique.clear();
        if (nearbyPlacesList!=null) nearbyPlacesList.clear();
        if (nearbyPlacesList_placeId!=null) nearbyPlacesList_placeId.clear();

        startAddrName=(String)objects[0];
        endAddrName=(String)objects[1];
        mGoogleMap=(GoogleMap)objects[2];
        sign=(int)objects[3];
        timeTextView=(TextView)objects[4];
        distTextView=(TextView)objects[5];
        PROXIMITY_RADIUS=(double)objects[6];

        String directionUrl=null;
        directionUrl=getPathDirectionUrl(startAddrName,endAddrName);


        String jsonData=new ReadUrl().readFromUrl(directionUrl);
        return jsonData;
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

        /*** If we successfully find the routes between startAddrName and endAddrName ***/
        if (routes.getId()==1)
        {
            isSuccessful=true;
            /*** draw the routes startAddrName -> endAddrName ***/
            showPolyLinesOnMap(routes);
            timeTextView.setText("Time:"+routes.getDuration().getText());
            distTextView.setText("Dist:"+routes.getDistance().getText());
            /*** transfer points on routes to GetNearbyGasStation ***/
            asyncGetNearbyGasStation();


        }
        /*** If we fail to find routes ***/
        else
        {
            isSuccessful=false;
            timeTextView.setText("Time:xxx(Try Again!)");
            distTextView.setText("Dist:xxx(Try Again!)");

        }

    }


    public void asyncGetNearbyGasStation()
    {
        /*** Scan nearby gas stations every 5 consecutive points on routes ***/
        int step=5;
        List<LatLng> routesPts=routes.getPoints();
        double total=0;

        for (int i=0;i<routesPts.size()+step;i=i+step)
        {
            LatLng lineBegin;
            LatLng lineEnd;
            if ( i>=routesPts.size() )
            {
                lineBegin = routesPts.get(routesPts.size()-1);
            }
            else
            {
                lineBegin = routesPts.get(i);
            }
            if ( i+step>=routesPts.size() )
            {
                lineEnd = routesPts.get(routesPts.size()-1);
            }
            else
            {
                lineEnd = routesPts.get(i+step);
            }
            double dist = calculateDist(lineBegin,lineEnd);
            total += dist;

            /*** Every 5 consecutive points distance ranges from 100~500 ***/
            Log.d("debugMsg10","i:"+i+" dist:"+dist);


            Object[] DataTransfer = new Object[4];
            DataTransfer[0]=mGoogleMap;
            DataTransfer[1]=lineBegin;
            DataTransfer[2]=PROXIMITY_RADIUS;
            DataTransfer[3]=i;
            //DataTransfer[4]=j;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new GetNearbyGasStation(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataTransfer);
            }
            else {
                new GetNearbyGasStation(this).execute(DataTransfer);
            }
        }

        Log.d("debugMsg10","total:"+total);

    }
    /*** transfer data from GetNearbyGasStation back to DirectionFinder ***/
    @Override
    public void nearByGasStationTransfer(Object[] transfer)
    {

        nearestLatLng = (LatLng)transfer[0];
        nearbyPlacesList = (List<HashMap<String, String>>) transfer[1];
        markerList = (List<Marker>)transfer[2];

        /*** nearbyPlacesList has a lot of gas stations overlapped
            we remove the duplicates and produce nearbyPlacesList_unique ***/
        if (nearbyPlacesList!= null || nearbyPlacesList.size()>0)
        {
            for (int i=0;i<nearbyPlacesList.size();i++)
            {
                HashMap<String,String> nearbyPlace=nearbyPlacesList.get(i);
                String place_id = nearbyPlace.get("place_id");
                if (nearbyPlacesList_placeId.contains(place_id)==true) continue;
                nearbyPlacesList_placeId.add(place_id);
                nearbyPlacesList_unique.add(nearbyPlace);
            }
            /*** transfer data from DirectionFinder to fragment1 ***/
            transferD[0]=polyline;
            transferD[1]=routes;
            transferD[2]=nearbyPlacesList_unique;
            transferD[3]=markerList;
            delegate.directionFinderTransfer(transferD);
        }

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
        PolylineOptions polyOption=new PolylineOptions()
                .addAll(R.getPoints())
                .color(Color.BLUE)
                .width(5)
                ;
        polyline=mGoogleMap.addPolyline(polyOption);

    }

    /*** This is used to get the url for direction between start Address and end Address.
     Using google direction api. ***/
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
