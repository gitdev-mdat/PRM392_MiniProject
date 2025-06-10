package org.osmdroid.bonuspack.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpConnection {

    private HttpURLConnection mConnection;
    private InputStream mStream;
    private String mUserAgent;

    public void setUserAgent(String userAgent) {
        mUserAgent = userAgent;
    }

    public boolean doGet(String urlString) {
        try {
            URL url = new URL(urlString);
            mConnection = (HttpURLConnection) url.openConnection();
            mConnection.setConnectTimeout(30000);
            mConnection.setReadTimeout(30000);
            mConnection.setRequestMethod("GET");

            if (mUserAgent != null) {
                mConnection.setRequestProperty("User-Agent", mUserAgent);
            }

            int responseCode = mConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return false;
            }

            mStream = mConnection.getInputStream();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public InputStream getStream() {
        return mStream;
    }

    public String getContentAsString() {
        if (mStream == null) return null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(mStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void close() {
        try {
            if (mStream != null) {
                mStream.close();
                mStream = null;
            }
            if (mConnection != null) {
                mConnection.disconnect();
                mConnection = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
