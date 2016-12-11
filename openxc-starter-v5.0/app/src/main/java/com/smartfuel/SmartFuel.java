package com.smartfuel;

import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import com.openxc.measurements.AcceleratorPedalPosition;
import com.openxc.measurements.BrakePedalStatus;
import com.openxc.measurements.EngineSpeed;
import com.openxc.measurements.FuelConsumed;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.Odometer;
import com.openxc.measurements.SteeringWheelAngle;
import com.openxc.measurements.TorqueAtTransmission;
import com.openxc.measurements.VehicleSpeed;

import org.dmg.pmml.Array;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;

import org.jpmml.evaluator.TargetField;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by WANGYH on 11/15/16.
 */

public class SmartFuel {
    private LinkedList<Map<String, Object>> odometer_recent_evt = new LinkedList<>();
    private LinkedList<Map<String, Object>> fuel_recent_evt = new LinkedList<>();
    private LinkedList<Map<String, Object>> steer_recent_evt = new LinkedList<>();
    private LinkedList<Map<String, Object>> accelerator_recent_evt = new LinkedList<>();
    private LinkedList<Map<String, Object>> brake_recent_evt = new LinkedList<>();
    private LinkedList<Map<String, Object>> speed_recent_evt = new LinkedList<>();
    private LinkedList<Map<String, Object>> engine_recent_evt = new LinkedList<>();
    private LinkedList<Map<String, Object>> torque_recent_evt = new LinkedList<>();

    private boolean brake_status = true;
    private boolean parking_status = true;
    private double last_fuel_value = 0;
    private double last_speed_value = 0;

    private double SPLIT_CHUNK = 0.1;
    private double SPLIT_GAP = 0.01;

    private int score_preserve_distance = 2;
    private double score_default = 5;
    int score_preserve_samples = (int)(score_preserve_distance / SPLIT_GAP);
    private double score_sum = score_default * score_preserve_samples;
    private LinkedList<Double> eco_driving_scores = new LinkedList<>();
    public double eco_driving_score = score_default;

    private JSONArray h_data;
    private ArrayList<Double> h_speed;
    private ArrayList<Double> h_score;
    private ArrayList<Double> h_fuel_eff;

    private Evaluator evaluator;

    public SmartFuel() {
        try{
            String fileName = "openxc_model/fuel_vs_speed-n-score.json";
            String path = Environment.getExternalStorageDirectory()+"/"+fileName;
            File file = new File(path);
            InputStream is = new FileInputStream(file);
            String jsonTxt = IOUtils.toString(is);
            h_data = new JSONArray(jsonTxt);
            Log.i("SmartFuel.Init", "load knn model success");

            h_speed = new ArrayList<Double>();
            h_score = new ArrayList<Double>();
            h_fuel_eff = new ArrayList<Double>();
            for (int i = 0; i < h_data.length(); i++) {
                JSONObject jsonData = h_data.getJSONObject(i);
                String speed_s = jsonData.getString("speed");
                String score_s = jsonData.getString("score");
                String fuel_efficiency_s = jsonData.getString("fuel_efficiency");
                h_speed.add(Double.valueOf(speed_s));
                h_score.add(Double.valueOf(score_s));
                h_fuel_eff.add(Double.valueOf(fuel_efficiency_s));
            }
            Log.i("SmartFuel.Init", "process knn model success");


            for (int i = 0; i < score_preserve_samples; i++) {
                eco_driving_scores.add(score_default);
            }
            Log.i("SmartFuel.Init", "scores record init finished");

            fileName = "openxc_model/est_100.pmml.ser";
            path = Environment.getExternalStorageDirectory()+"/"+fileName;
            file = new File(path);
            is = new FileInputStream(file);
            PMML pmml = org.jpmml.model.SerializationUtil.deserializePMML(is);
            ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
            ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelEvaluator(pmml);
            modelEvaluator.verify();
            evaluator = (Evaluator)modelEvaluator;
            Log.i("SmartFuel.Init", "load gbdt model success");

            Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
            int featureCount = 0;
            List<InputField> inputFields = evaluator.getInputFields();
            for(InputField inputField : inputFields){
                FieldName inputFieldName = inputField.getName();
                FieldValue inputFieldValue = inputField.prepare(
                        featureCount++);
                arguments.put(inputFieldName, inputFieldValue);
            }
            Map<FieldName, ?> evaluateResult = evaluator.evaluate(arguments);

            List<TargetField> targetFields = evaluator.getTargetFields();
            for(TargetField targetField : targetFields) {
                FieldName targetFieldName = targetField.getName();
                Object targetFieldValue = evaluateResult.get(targetFieldName);
                Double r = (Double) targetFieldValue;
                Log.i("SmartFuel.Init", "test run result: " + String.valueOf(r));
            }
            Log.i("SmartFuel.Init", "test run success");
        } catch (Exception e) {
            Log.e("SmartFuel.Init", "exception", e);
        }
    }

    public Object getRealMeasurementValue(Measurement measurement) {
        Object value;
        if (measurement.getGenericName() == "odometer") {
            Odometer odometer=(Odometer)measurement;
            value=odometer.getValue().doubleValue();
        }
        else if (measurement.getGenericName() == "fuel_consumed_since_restart") {
            FuelConsumed fuelConsumed=(FuelConsumed) measurement;
            value=fuelConsumed.getValue().doubleValue();
        }
        else if (measurement.getGenericName() == "steering_wheel_angle") {
            SteeringWheelAngle steeringWheelAngle = (SteeringWheelAngle) measurement;
            value=steeringWheelAngle.getValue().doubleValue();
        }
        else if (measurement.getGenericName() == "accelerator_pedal_position") {
            AcceleratorPedalPosition acceleratorPedalPosition = (AcceleratorPedalPosition)measurement;
            value=acceleratorPedalPosition.getValue().doubleValue();
        }
        else if (measurement.getGenericName() == "brake_pedal_status") {
            BrakePedalStatus brakePedalStatus=(BrakePedalStatus) measurement;
            value=brakePedalStatus.getValue().booleanValue();
        }
        else if (measurement.getGenericName() == "vehicle_speed") {
            VehicleSpeed vehicleSpeed=(VehicleSpeed)measurement;
            value=vehicleSpeed.getValue().doubleValue();
        }
        else if (measurement.getGenericName() == "engine_speed") {
            EngineSpeed engineSpeed = (EngineSpeed) measurement;
            value=engineSpeed.getValue().doubleValue();
        }
        else if (measurement.getGenericName() == "torque_at_transmission") {
            TorqueAtTransmission torqueAtTransmission=(TorqueAtTransmission)measurement;
            value=torqueAtTransmission.getValue().doubleValue();
        }
        else {
            value = measurement.getValue();
        }
        return value;
    }

    public void extract_data() {
        Map<String, Double> data = new HashMap();
        Map<String, Object> start = odometer_recent_evt.getFirst();
        Map<String, Object> end = odometer_recent_evt.getLast();
        data.put("distance", (Double)end.get("value") - (Double)start.get("value"));
        data.put("odom_time", (Double)end.get("timestamp") - (Double)start.get("timestamp"));
        data.put("odom_samples", (double)odometer_recent_evt.size());

        // add from matlab
        data.put("time", data.get("odom_time"));
        data.put("speed", data.get("distance") / (data.get("time") / 3600));

        double fuel_while_moving = 0;
        double fuel_while_parking = 0;
        for (Map t : fuel_recent_evt) {
            if ((Boolean)t.get("parking_status")) {
                fuel_while_parking += (Double)t.get("fuel_since_last_evt");
            } else {
                fuel_while_moving += (Double)t.get("fuel_since_last_evt");
            }
        }
        data.put("fuel_while_moving", fuel_while_moving);
        data.put("fuel_while_parking", fuel_while_parking);
        data.put("fuel_used", fuel_while_moving + fuel_while_parking);
        data.put("fuel_eff", data.get("fuel_used") / data.get("distance"));

        double total = 0;
        double number = 0;
        for (Map t : steer_recent_evt) {
            total += Math.abs((Double)t.get("value"));
            number += 1;
        }
        data.put("steer_abs_avg", total / number);

        double total_abs = 0;
        double total_valid = 0;
        number = 0;
        double number_while_moving = 0;
        double threshold = 1;
        for (Map t : accelerator_recent_evt) {
            total_abs += (Double)t.get("value");
            total_valid += ((Double)t.get("value") > threshold) ? 1 : 0;
            number += 1;
            number_while_moving += ((Boolean)t.get("parking_status") == false) ? 1 : 0;
        }
        data.put("accelerator_avg", total_abs / number_while_moving);
        data.put("accelerator_freq", total_valid / number_while_moving);

        double brake_time = 0;
        double moving_time = 0;
        double last_time = (Double)start.get("timestamp");
        for (Map t : speed_recent_evt) {
            if (!(Boolean) t.get("parking_status")) {
                moving_time += (Double)t.get("timestamp") - last_time;
                if ((Boolean) t.get("brake_status")) {
                    brake_time += (Double)t.get("timestamp") - last_time;
                }
            }
        }
        data.put("brake_time_pct", brake_time / moving_time);

        double start_speed = (Double)speed_recent_evt.getFirst().get("value");
        double end_speed = (Double)speed_recent_evt.getLast().get("value");
        data.put("kinetic_energy_gain", end_speed*end_speed - start_speed*start_speed);
        total = 0;
        for (Map t : speed_recent_evt) {
            total += Math.max(0, (Double)t.get("value_diff"));
        }
        data.put("speed_change", total);

        total = 0;
        number = 0;
        threshold = 1;
        for (Map t : speed_recent_evt) {
            total += ((Double)t.get("value") < threshold) ? 1 : 0;
            number += 1;
        }
        data.put("parking_time_pct", total / number);
        data.put("parking_time", (Double)data.get("parking_time_pct") * (Double)data.get("odom_time"));

        total = 0;
        number = 0;
        double total_while_moving = 0;
        number_while_moving = 0;
        double max_rpm = 0;
        for (Map t : engine_recent_evt) {
            total += (Double)t.get("value");
            number += 1;
            if (!(Boolean) t.get("parking_status")) {
                total_while_moving += (Double)t.get("value");
                number_while_moving += 1;
            }
            max_rpm = Math.max(max_rpm, (Double)t.get("value"));
        }
        data.put("engine_rpm_avg", total_while_moving / number_while_moving);
        data.put("engine_rpm_max", max_rpm);

        total = 0;
        number = 0;
        total_while_moving = 0;
        number_while_moving = 0;
        double max_torque = 0;
        for (Map t : torque_recent_evt) {
            total += Math.abs((Double)t.get("value"));
            number += 1;
            if (!(Boolean) t.get("parking_status")) {
                total_while_moving += Math.abs((Double)t.get("value"));
                number_while_moving += 1;
            }
            max_torque = Math.max(max_torque, Math.abs((Double)t.get("value")));
        }
        data.put("torque_avg", total_while_moving / number_while_moving);
        data.put("torque_max", max_torque);

        // calculate score
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
        int featureIndex = 0;
        List<InputField> inputFields = evaluator.getInputFields();
        for(InputField inputField : inputFields){
            FieldName inputFieldName = inputField.getName();
            double feature = 0;
            switch (featureIndex++) {
                case 0: feature = data.get("speed"); break;
                case 1: feature = data.get("time"); break;
                case 2: feature = data.get("accelerator_avg"); break;
                case 3: feature = data.get("accelerator_freq"); break;
                case 4: feature = data.get("brake_time_pct"); break;
                case 5: feature = data.get("kinetic_energy_gain"); break;
                case 6: feature = data.get("speed_change"); break;
                case 7: feature = data.get("engine_rpm_avg"); break;
                case 8: feature = data.get("engine_rpm_max"); break;
                case 9: feature = data.get("torque_avg"); break;
                case 10: feature = data.get("torque_max"); break;
            }
            FieldValue inputFieldValue = inputField.prepare(feature);
            arguments.put(inputFieldName, inputFieldValue);
        }
        Map<FieldName, ?> evaluateResult = evaluator.evaluate(arguments);

        double instant_eco_driving_score = 0;
        List<TargetField> targetFields = evaluator.getTargetFields();
        for(TargetField targetField : targetFields) { // only one data in targetFields
            FieldName targetFieldName = targetField.getName();
            Object targetFieldValue = evaluateResult.get(targetFieldName);
            Double r = (Double) targetFieldValue;
            instant_eco_driving_score = r;
            Log.i("SmartFuel.Score", "instant eco-driving score: " + String.valueOf(r));
        }
        score_sum += instant_eco_driving_score - eco_driving_scores.pop();
        eco_driving_scores.add(instant_eco_driving_score);
        eco_driving_score = score_sum / score_preserve_samples;
        Log.i("SmartFuel.Score", "long-term eco-driving score: " + String.valueOf(eco_driving_score));
    }

    public void onOpenXCEvent(Measurement measurement) {
        Map sample = new HashMap();
        sample.put("name", measurement.getGenericName());
        sample.put("timestamp", (double)measurement.getBirthtime() / 1000);
        sample.put("value", getRealMeasurementValue(measurement));
        sample.put("parking_status", parking_status);
        sample.put("brake_status", brake_status);

        if (measurement.getGenericName() == "odometer") {
            odometer_recent_evt.add(sample);

            /* check if current trace longer than SPLIT_CHUNK */
            if (((Double)sample.get("value") - (Double)odometer_recent_evt.getFirst().get("value")) > SPLIT_CHUNK) {
                Log.i("SmartFuel.Split", "analysis chunk data");
                extract_data();

                // deal with odometer queue
                double pop_odom_goal = (Double)odometer_recent_evt.getFirst().get("value") + SPLIT_GAP;
                double pop_time_goal = 0;
                while ((Double)odometer_recent_evt.getFirst().get("value") < pop_odom_goal) {
                    pop_time_goal = (Double)odometer_recent_evt.pop().get("timestamp");
                }
                // deal with other queues
                while (!fuel_recent_evt.isEmpty() && (Double)fuel_recent_evt.getFirst().get("timestamp") < pop_time_goal) {
                    fuel_recent_evt.pop();
                }
                while (!steer_recent_evt.isEmpty() && (Double)steer_recent_evt.getFirst().get("timestamp") < pop_time_goal) {
                    steer_recent_evt.pop();
                }
                while (!accelerator_recent_evt.isEmpty() && (Double)accelerator_recent_evt.getFirst().get("timestamp") < pop_time_goal) {
                    accelerator_recent_evt.pop();
                }
                while (!brake_recent_evt.isEmpty() && (Double)brake_recent_evt.getFirst().get("timestamp") < pop_time_goal) {
                    brake_recent_evt.pop();
                }
                while (!speed_recent_evt.isEmpty() && (Double)speed_recent_evt.getFirst().get("timestamp") < pop_time_goal) {
                    speed_recent_evt.pop();
                }
                while (!engine_recent_evt.isEmpty() && (Double)engine_recent_evt.getFirst().get("timestamp") < pop_time_goal) {
                    engine_recent_evt.pop();
                }
                while (!torque_recent_evt.isEmpty() && (Double)torque_recent_evt.getFirst().get("timestamp") < pop_time_goal) {
                    torque_recent_evt.pop();
                }
            }
        }
        else if (measurement.getGenericName() == "fuel_consumed_since_restart") {
            double tmp = last_fuel_value != 0 ? (Double)sample.get("value") - last_fuel_value : 0;
            sample.put("fuel_since_last_evt", tmp);
            last_fuel_value = (Double)sample.get("value");
            fuel_recent_evt.add(sample);
        }
        else if (measurement.getGenericName() == "steering_wheel_angle") {
            steer_recent_evt.add(sample);
        }
        else if (measurement.getGenericName() == "accelerator_pedal_position") {
            accelerator_recent_evt.add(sample);
        }
        else if (measurement.getGenericName() == "brake_pedal_status") {
            brake_recent_evt.add(sample);
            brake_status = (Boolean)sample.get("value");
        }
        else if (measurement.getGenericName() == "vehicle_speed") {
            sample.put("value_diff", (Double)sample.get("value") - last_speed_value);
            last_speed_value = (Double)sample.get("value");
            speed_recent_evt.add(sample);
            parking_status = (Double)sample.get("value") < 1;
        }
        else if (measurement.getGenericName() == "engine_speed") {
            engine_recent_evt.add(sample);
        }
        else if (measurement.getGenericName() == "torque_at_transmission") {
            torque_recent_evt.add(sample);
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

        // do statistics
        // note that this is not a pure k-NN, instead, closer score will have larger weight, in [1,2]
        double mean = 0;
        double weight_sum = 0;
        for (int i=l; i<=h; i++) {
            double w = 2 - Math.abs(h_score.get(i) - eco_driving_score) / 5;
            mean += w * h_fuel_eff.get(i);
            weight_sum += w;
        }
        mean /= weight_sum; // mean /= total_num;
        double variance = 0;
        for (int i=l; i<=h; i++) {
            double w = 2 - Math.abs(h_score.get(i) - eco_driving_score) / 5;
            variance += w * (h_fuel_eff.get(i) - mean) * (h_fuel_eff.get(i) - mean);
        }
        variance /= weight_sum; // variance /= total_num;
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
                            " fuel: "+String.valueOf(est.first * dist_km));
                }
            }
        } catch (Exception e) {
            Log.e("SmartFuel.Cal", "exception", e);
        }

        Log.d("SmartFuel.Cal", "Result: mean: "+String.valueOf(total_fuel_est)+" variance: "+String.valueOf(total_fuel_variance));

        return new Pair<>(total_fuel_est, total_fuel_variance);
    }
}
