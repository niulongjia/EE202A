package com.openxc.openxcstarter;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by navneet on 23/7/16.
 */
public class DataParser
{

    // Currently we parse to get only one Route, no alternatives.
    public Route parseGoogleDirectionApi(String jsonData) throws  JSONException
    {
        Route route = new Route();
        route.setId(0);

        if (jsonData == null)
            return route;

        //List<Route> routes = new ArrayList<Route>();
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray jsonRoutes = jsonObject.getJSONArray("routes");
        /*** If we set "alternative=true" in URL, then jsonRoutes.length() may be larger than one
             Otherwise, there will only be one route. So we do not need loop ***/
        //for (int i = 0; i < jsonRoutes.length(); i++) {

            if (jsonRoutes.length()==0) return route;

            JSONObject jsonRoute = jsonRoutes.getJSONObject(0);
            JSONObject overview_polylineJson = jsonRoute.getJSONObject("overview_polyline");
            JSONArray jsonLegs = jsonRoute.getJSONArray("legs");

            String distanceText=""; int distanceVal=0;
            String durationText=""; int durationVal=0;
            String startAddrName=""; String endAddrName="";
            double startLat=0.0; double startLng=0.0;
            double endLat=0.0; double endLng=0.0;

        /*** After adding waypoints parameter, there may be multiple jsonLegs ***/
            for (int i=0;i<jsonLegs.length();i++)
            {
                JSONObject jsonLeg = jsonLegs.getJSONObject(i);

                JSONObject jsonDistance = jsonLeg.getJSONObject("distance");
                JSONObject jsonDuration = jsonLeg.getJSONObject("duration");
                JSONObject jsonEndLocation = jsonLeg.getJSONObject("end_location");
                JSONObject jsonStartLocation = jsonLeg.getJSONObject("start_location");
                 distanceVal += jsonDistance.getInt("value");
                 durationVal += jsonDuration.getInt("value");
                if (i==0)
                {
                    distanceText += jsonDistance.getString("text");
                    durationText += jsonDuration.getString("text");
                    startAddrName = jsonLeg.getString("start_address");
                    startLat = jsonStartLocation.getDouble("lat"); startLng = jsonStartLocation.getDouble("lng");
                }
                else
                {
                    distanceText += " + " + jsonDistance.getString("text");
                    durationText += " + " + jsonDuration.getString("text");
                    if (i==jsonLegs.length()-1)
                    {
                        endAddrName = jsonLeg.getString("end_address");
                        endLat=jsonEndLocation.getDouble("lat"); endLng = jsonEndLocation.getDouble("lng");
                    }
                }

            }

            route.setDistance( new Distance(distanceText,distanceVal) );
            route.setDuration( new Duration(durationText,durationVal) );
            route.setEndAddress( endAddrName );
            route.setStartAddress(startAddrName );
            route.setStartLatLng( new LatLng(startLat,startLng) );
            route.setEndLatLng( new LatLng(endLat,endLng) );
            route.setPoints( decodePoly(overview_polylineJson.getString("points")) );
            route.setId(1);
            //routes.add(route);
        //}
        return route;
    }
    /***
     Method to decode polyline points
     Courtesy : jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     ***/
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
    public List<HashMap<String, String>> parseGooglePlaceApi(String jsonData)
    {
        JSONArray jsonArray = null;
        JSONObject jsonObject;

        try {
            jsonObject = new JSONObject((String) jsonData);
            jsonArray = jsonObject.getJSONArray("results");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return getPlaces(jsonArray);
    }

    private List<HashMap<String, String>> getPlaces(JSONArray jsonArray) {
        int placesCount = jsonArray.length();
        List<HashMap<String, String>> placesList = new ArrayList<>();
        HashMap<String, String> placeMap = null;

        for (int i = 0; i < placesCount; i++) {
            try {
                placeMap = getPlace((JSONObject) jsonArray.get(i));
                placesList.add(placeMap);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return placesList;
    }

    private HashMap<String, String> getPlace(JSONObject googlePlaceJson) {
        HashMap<String, String> googlePlaceMap = new HashMap<String, String>();
        String placeId ="";
        String placeName = "-NA-";
        String vicinity = "-NA-";
        String latitude = "";
        String longitude = "";
        String reference = "";


        try {
            if (!googlePlaceJson.isNull("name")) {
                placeName = googlePlaceJson.getString("name");
            }
            if (!googlePlaceJson.isNull("vicinity")) {
                vicinity = googlePlaceJson.getString("vicinity");
            }
            latitude = googlePlaceJson.getJSONObject("geometry").getJSONObject("location").getString("lat");
            longitude = googlePlaceJson.getJSONObject("geometry").getJSONObject("location").getString("lng");
            reference = googlePlaceJson.getString("reference");
            placeId = googlePlaceJson.getString("place_id");

            googlePlaceMap.put("place_name", placeName);
            googlePlaceMap.put("vicinity", vicinity);
            googlePlaceMap.put("lat", latitude);
            googlePlaceMap.put("lng", longitude);
            googlePlaceMap.put("reference", reference);
            googlePlaceMap.put("place_id",placeId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return googlePlaceMap;
    }


}

