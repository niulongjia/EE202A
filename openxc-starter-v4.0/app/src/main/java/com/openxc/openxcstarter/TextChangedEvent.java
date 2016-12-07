package com.openxc.openxcstarter;

import com.smartfuel.SmartFuel;

/**
 * Created by niulongjia on 2016/11/5.
 */

/*** transfer data from StarterActivity to fragment1 and fragment2 ***/
public class TextChangedEvent
{
    public SmartFuel smartFuel;
    public double steeringWheelAngle_val;
    public double torqueAtTransmission_val;
    public double engineSpeed_val;
    public double vehicleSpeed_val;
    public double acceleratorPedalPosition_val;
    public boolean brakePedalStatus_val;
    public double odometer_val;
    public double fuelConsumed_val;
    public double fuelLevel_val;
    public double latitude_val=34.052235;
    public double longitude_val=-118.243683;
    public double totalFuel_val;


    public TextChangedEvent(
            SmartFuel smartFuel,
            double steeringWheelAngle_val,
            double torqueAtTransmission_val,
            double engineSpeed_val,
            double vehicleSpeed_val,
            double acceleratorPedalPosition_val,
            boolean brakePedalStatus_val,
            double odometer_val,
            double fuelConsumed_val,
            double fuelLevel_val,
            double totalFuel_val,
            double latitude_val,
            double longitude_val)
    {
                this.smartFuel=smartFuel;
                this.steeringWheelAngle_val = steeringWheelAngle_val;
                this.torqueAtTransmission_val = torqueAtTransmission_val;
                this.engineSpeed_val = engineSpeed_val;
                this.vehicleSpeed_val = vehicleSpeed_val;
                this.acceleratorPedalPosition_val = acceleratorPedalPosition_val;
                this.brakePedalStatus_val = brakePedalStatus_val;
                this.odometer_val = odometer_val;
                this.fuelConsumed_val = fuelConsumed_val;
                this.fuelLevel_val=fuelLevel_val;
                this.totalFuel_val=totalFuel_val;
                this.latitude_val = latitude_val;
                this.longitude_val = longitude_val;
    }
}
