package com.openxc.openxcstarter;

/**
 * Created by niulongjia on 2016/10/26.
 */

public class Duration
{
    private String text;
    private int value; // in seconds.

    public Duration(String text, int value) {
        this.text = text;
        this.value = value;
    }
    //get method...
    public String getText()
    {
        return text;
    }
    public int getValue()
    {
        return value;
    }
    //set method...
    public void setText(String t)
    {
        text=t;
    }
    public void setValue(int v)
    {
        value=v;
    }
}
