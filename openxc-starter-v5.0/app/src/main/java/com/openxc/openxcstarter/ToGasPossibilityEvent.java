package com.openxc.openxcstarter;

/**
 * Created by niulongjia on 2016/12/4.
 */

public class ToGasPossibilityEvent
{
    public double toGasPossibility;
    public double mean;
    public double var;
    public ToGasPossibilityEvent(double toGasPossibility,double mean,double var)
    {
        this.toGasPossibility=toGasPossibility;
        this.mean=mean;
        this.var=var;
    }
}
