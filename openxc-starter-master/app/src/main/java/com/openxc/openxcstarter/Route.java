package com.openxc.openxcstarter;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by niulongjia on 2016/10/26.
 */

public class Route
{
    // Start and End are for overview
    private Distance distance;
    private Duration duration;

    private String StartAddress;
    private LatLng StartLatLng;

    private String EndAddress;
    private LatLng EndLatLng;

    private List<LatLng> points;
    public Route()
    {

    }
    public Route(Distance DI, Duration DU, String SA,LatLng SL, String EA, LatLng EL,List<LatLng> ps)
    {
        distance=DI;
        duration=DU;
        StartAddress=SA;
        StartLatLng=SL;
        EndAddress=EA;
        EndLatLng=EL;
        points=ps;
    }
    // ...get method...
    public Distance getDistance()
    {
        return distance;
    }
    public Duration getDuration()
    {
        return duration;
    }
    public List<LatLng> getPoints()
    {
        return points;
    }
    public String getStartAddress()
    {
        return StartAddress;
    }
    public LatLng getStartLatLng()
    {
        return StartLatLng;
    }
    public String getEndAddress()
    {
        return EndAddress;
    }
    public LatLng getEndLatLng()
    {
        return EndLatLng;
    }

    // ...set method...

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public void setPoints(List<LatLng> p)
    {
        points=p;
    }
    public void setStartAddress(String SA)
    {
        StartAddress=SA;
    }
    public void setStartLatLng(LatLng SL)
    {
        StartLatLng=SL;
    }
    public void setEndAddress(String EA)
    {
        EndAddress=EA;
    }
    public void setEndLatLng(LatLng EL)
    {
        EndLatLng=EL;
    }
}
