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
            URL url = new URL(urlString);
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Reading data from url
            InputStream iStream = new BufferedInputStream( urlConnection.getInputStream() );
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();
            String line ;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("debugMsg","readFromUrl complete!");

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
