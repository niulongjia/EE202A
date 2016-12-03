package com.openxc.openxcstarter;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by niulongjia on 2016/10/26.
 */

public class ReadUrl
{
    // This method retrieve data from urlString.
    public String readFromUrl(String urlString)
    {
        String data = "";
        //InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try
        {
            Log.d("debugMsg", "Complete0!");
            URL url = new URL(urlString);
            Log.d("debugMsg", "Complete1!");
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            //Log.d("debugMsg", "Ready to connect");
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            Log.d("debugMsg",urlConnection.getResponseMessage());
            Log.d("debugMsg",Integer.toString(urlConnection.getResponseCode()));
            Log.d("debugMsg", "Complete2!");

            // Reading data from url
            InputStream iStream = new BufferedInputStream( urlConnection.getInputStream() );

            Log.d("debugMsg", "getInputStream");
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();
            Log.d("debugMsg","Complete3!");
            String line ;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("debugMsg","Complete4!");
            Log.d("downloadUrl", data.toString());

            br.close();

        }
        catch (Exception e)
        {
            Log.d("Exception", e.toString());
        }
        finally
        {
            urlConnection.disconnect();
        }
        return data;
    }
}
