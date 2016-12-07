package com.smartfuel;

import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import com.openxc.measurements.FuelConsumed;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.Odometer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by WANGYH on 11/15/16.
 */

public class SmartFuel {
    private LinkedList<Measurement> recentOdomEvt = new LinkedList<Measurement>();
    private LinkedList<Measurement> recentrecentFuelEvt = new LinkedList<Measurement>();

    private JSONArray h_data;
    private ArrayList<Double> h_speed;
    private ArrayList<Double> h_fuel_eff;

    double SPLIT_CHUNK = 0.1;

    public SmartFuel() {
        try{
            String fileName = "openxc_model/tmp_output.json";
            String path = Environment.getExternalStorageDirectory()+"/"+fileName;
            File file = new File(path);
            InputStream is = new FileInputStream(file);
            String jsonTxt = IOUtils.toString(is);
            h_data = new JSONArray(jsonTxt);
            Log.i("SmartFuel.Init", "load model success");
            h_speed = new ArrayList<Double>();
            h_fuel_eff = new ArrayList<Double>();
            for (int i = 0; i < h_data.length(); i++) {
                JSONObject jsonData = h_data.getJSONObject(i);
                String speed_s = jsonData.getString("speed");
                String fuel_efficiency_s = jsonData.getString("fuel_efficiency");
                h_speed.add(Double.valueOf(speed_s));
                h_fuel_eff.add(Double.valueOf(fuel_efficiency_s));
            }
            Log.i("SmartFuel.Init", "process model success");
        } catch (Exception e) {
            Log.e("SmartFuel.Init", "exception", e);
        }
    }

    public void onOpenXCEvent(Measurement measurement) {
        if (measurement.getGenericName() == "fuel_consumed_since_restart") {
            recentrecentFuelEvt.add(measurement);
        }
        if (measurement.getGenericName() == "odometer") {
            recentOdomEvt.add(measurement);
        }
    }

    public double getMilesPerGallon() {
        if (recentrecentFuelEvt.isEmpty() || recentOdomEvt.isEmpty()) {
            return -1;
        }

        double startOdom = ((Odometer)recentOdomEvt.getFirst()).getValue().doubleValue();
        double endOdom = ((Odometer)recentOdomEvt.getLast()).getValue().doubleValue();

        double startFuel = ((FuelConsumed)recentrecentFuelEvt.getFirst()).getValue().doubleValue();
        double endFuel = ((FuelConsumed)recentrecentFuelEvt.getLast()).getValue().doubleValue();

        if (startOdom < endOdom && startFuel < endFuel) {
            double gallons = (endFuel - startFuel) / 3.785411784;
            double miles = (endOdom - startOdom) / 1.60934;
            return miles / gallons;
        } else {
            return -2;
        }
    }

    private Pair<Double, Double> getChunkFuelEffEstimation(double speed) {
        /*** total_num change ***/
        int total_num = 30;
        // find closest index
        int index;
        for (index = 0; index < h_data.length(); index++) {
            if (h_speed.get(index) > speed) break;
        }
        if (index == h_data.length() || index > 0 &&
                Math.abs(h_speed.get(index-1) - speed) < Math.abs(h_speed.get(index) - speed)) {
            index--;
        }
        int l = index;
        int h = index;
        while (h - l + 1 < total_num) {
            if (h == h_data.length() - 1) l--;
            else if (l == 0) h++;
            else if (Math.abs(h_speed.get(l-1) - speed) < Math.abs(h_speed.get(h+1) - speed)) l--;
            else h++;
        }
        double mean = 0;
        for (int i=l; i<=h; i++) {
            mean += h_fuel_eff.get(i);
        }
        mean /= total_num;
        double variance = 0;
        for (int i=l; i<=h; i++) {
            variance += (h_fuel_eff.get(i) - mean) * (h_fuel_eff.get(i) - mean);
        }
        variance /= total_num;
        return new Pair<> (mean, variance);
    }

    public Pair<Double, Double> getFuelEstimation(String jsonData) {
        double total_fuel_est = 0;
        double total_fuel_variance = 0;

        try{
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray jsonRoutes = jsonObject.getJSONArray("routes");
            JSONObject jsonRoute = jsonRoutes.getJSONObject(0);
            JSONArray jsonLegs = jsonRoute.getJSONArray("legs");

            for(int i=0; i<jsonLegs.length(); i++) { // in case there're waypoints (many legs)
                JSONObject jsonLeg = jsonLegs.getJSONObject(i);
                JSONArray jsonSteps = jsonLeg.getJSONArray("steps");
                for (int j=0; j<jsonSteps.length(); j++) { // may steps in a leg
                    JSONObject jsonStep = jsonSteps.getJSONObject(j);
                    String distance_s = jsonStep.getJSONObject("distance").getString("value");
                    String duration_s = jsonStep.getJSONObject("duration").getString("value");
                    double dist_m = Double.valueOf(distance_s);
                    double dist_km = dist_m / 1000;
                    double time_s = Double.valueOf(duration_s);
                    double time_h = time_s / 3600;
                    double speed_km_h = dist_km / time_h;

                    Pair<Double, Double> est = getChunkFuelEffEstimation(speed_km_h);
                    // for SPLIT_CHUNK, variance is est.second * SPLIT_CHUNK * SPLIT_CHUNK
                    // there're (dist_km / SPLIT_CHUNK) chunks
                    double fuel_est =  est.first * dist_km;
                    double fuel_est_variance = est.second * dist_km * SPLIT_CHUNK;
                    total_fuel_est += fuel_est;
                    total_fuel_variance += fuel_est_variance;
                    Log.d("SmartFuel.Cal", "Leg: "+
                            " dist: "+String.valueOf(dist_km)+
                            " speed: "+String.valueOf(speed_km_h)+
                            " fuel_eff: "+String.valueOf(est.first)+
                            " fuel: "+String.valueOf(est.first * dist_km * dist_km));
                }
            }
        } catch (Exception e) {
            Log.e("SmartFuel.Cal", "exception", e);
        }

        Log.d("SmartFuel.Cal", "Result: mean: "+String.valueOf(total_fuel_est)+" variance: "+String.valueOf(total_fuel_variance));

        return new Pair<>(total_fuel_est, total_fuel_variance);
    }
}
