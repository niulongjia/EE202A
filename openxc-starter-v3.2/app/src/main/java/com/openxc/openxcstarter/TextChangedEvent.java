package com.openxc.openxcstarter;

/**
 * Created by niulongjia on 2016/11/5.
 */

public class TextChangedEvent
{
    public double engineSpeed_val;
    public double fuellevel_val;
    public boolean brakePedalStatus_val;
    public double fuelConsumed_val;
    public double vehicleSpeed_val;
    public double latitude_val=34.052235;
    public double longitude_val=-118.243683;
    public long birthtime_val;

    public TextChangedEvent(double engineSpeed_val,
                            double fuellevel_val,
                            boolean brakePedalStatus_val,
                            double fuelConsumed_val,
                            double vehicleSpeed_val,
                            double latitude_val,
                            double longitude_val,
                            long birthtime_val)
    {
        this.engineSpeed_val=engineSpeed_val;
        this.fuellevel_val=fuellevel_val;
        this.brakePedalStatus_val=brakePedalStatus_val;
        this.fuelConsumed_val=fuelConsumed_val;
        this.vehicleSpeed_val=vehicleSpeed_val;
        this.latitude_val=latitude_val;
        this.longitude_val=longitude_val;
        this.birthtime_val=birthtime_val;
    }
}
