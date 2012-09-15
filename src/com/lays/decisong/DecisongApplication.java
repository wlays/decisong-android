package com.lays.decisong;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.util.Log;

import com.activeandroid.app.Application;

public class DecisongApplication extends Application {

    private static final String TAG = DecisongApplication.class.getSimpleName();
    public static final boolean DEVELOPER_MODE = true;
    
    public static final String RDIO_API_KEY = "pqqa7fv8egyhz6dskr8sc2uh";
    public static final String RDIO_SECRET_KEY = "Nqp6nvBj94";
    
    
    private static DecisongApplication mInstance;
    
    public void onCreate() {
	if (DEVELOPER_MODE) {
	    StrictMode.enableDefaults();
	}
	super.onCreate();
	mInstance = this;
    }
    
    /**
     * Singleton method
     * @return DecisongApplication
     */
    public static DecisongApplication getInstance() {
	return mInstance;
    }
    
    /**
     * Method to check if Internet connection is available
     * @return boolean
     */
    public boolean isOnline() {
	ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo netInfo = cm.getActiveNetworkInfo();
	if (netInfo != null && netInfo.isConnected()) {
	    Log.i(TAG, "Internet Connection: true");
	    return true;
	}
	Log.i(TAG, "Internet Connection: false");
	return false;
    }
}
