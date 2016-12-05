package com.openxc.openxcstarter;

/**
 * Created by niulongjia on 2016/12/4.
 */

public class ToDestPossibilityEvent
{
    public double toDestPossibility;
    public double mean;
    public double var;
    public ToDestPossibilityEvent(double toDestPossibility,double mean, double var)
    {
        this.toDestPossibility=toDestPossibility;
        this.mean=mean;
        this.var=var;
    }
}
