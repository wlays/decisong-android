package com.lays.decisong;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import com.lays.decisong.activities.GameActivity;
import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioListener;

/**
 * Application class to manage our application. Rdio object is allowed only be
 * instantiated once thus it must be kept as a static application variable
 * available for multiple game usage. Manages our current location as well.
 * 
 * @author wlays
 * 
 */
public class DecisongApplication extends Application implements RdioListener,
	LocationListener {

    /** Application tag */
    private static final String TAG = DecisongApplication.class.getSimpleName();

    /** Developer variable */
    public static final boolean DEVELOPER_MODE = false;

    /** Application constants */
    public static final String PLAYERS_KEY = "com.lays.decisong.activities.Players";
    public static final String RDIO_INITIALIZED_KEY = "com.lays.decisong.Rdio";
    public static final String RDIO_API_KEY = "pqqa7fv8egyhz6dskr8sc2uh";
    public static final String RDIO_SECRET_KEY = "Nqp6nvBj94";

    /** Application variables */
    private static DecisongApplication mInstance;
    private static Location mCurrentLocation;
    private static Rdio mRdio;
    private GameActivity mCurrentGame;

    public void onCreate() {
	if (DEVELOPER_MODE) {
	    StrictMode.enableDefaults();
	}
	super.onCreate();
	mInstance = this;
	LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 25.0f,
		this);
    }

    /**
     * Singleton method
     * 
     * @return DecisongApplication
     */
    public static DecisongApplication getInstance() {
	return mInstance;
    }

    /**
     * Returns true/false if data connection is available
     * 
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

    /**
     * Setups link between application and activity. Initializes Rdio object if
     * it's null. Acknowledge that Rdio object is initialized in shared
     * preferences so the attached GameActivity can find out and make
     * appropriate API calls. We hold a reference to the GameActivity parameter
     * so interface RdioListener's methods are executed in the referenced
     * activity rather than in the application.
     * 
     * @param activity
     * @return Rdio object
     */
    public Rdio setupGame(GameActivity activity) {

	SharedPreferences preferences = PreferenceManager
		.getDefaultSharedPreferences(this);
	Editor editor = preferences.edit();

	if (mRdio == null) {
	    // initial game setup
	    mRdio = new Rdio(DecisongApplication.RDIO_API_KEY,
		    DecisongApplication.RDIO_SECRET_KEY, null, null, this, this);
	    editor.putBoolean(RDIO_INITIALIZED_KEY, false);
	    editor.commit();
	} else {
	    // setup for subsequent games
	    editor.putBoolean(RDIO_INITIALIZED_KEY, true);
	    editor.commit();
	}
	mCurrentGame = activity;
	return mRdio;
    }

    /**
     * Method for preventing run-time memory leaks regarding long-held
     * references of activity objects
     */
    public void cleanUp() {
	mRdio.cleanup();
	mCurrentGame = null;
    }

    @Override
    public void onRdioAuthorised(String accessToken, String accessTokenSecret) {
	if (mCurrentGame != null) {
	    mCurrentGame.onRdioAuthorised(accessToken, accessTokenSecret);
	}
    }

    @Override
    public void onRdioReady() {
	if (mCurrentGame != null) {
	    mCurrentGame.onRdioReady();
	}
    }

    @Override
    public void onRdioUserAppApprovalNeeded(Intent authorisationIntent) {
	if (mCurrentGame != null) {
	    mCurrentGame.onRdioUserAppApprovalNeeded(authorisationIntent);
	}
    }

    @Override
    public void onRdioUserPlayingElsewhere() {
	if (mCurrentGame != null) {
	    mCurrentGame.onRdioUserPlayingElsewhere();
	}
    }

    /**
     * Returns a Location object with our current coordinates.
     * 
     * @return
     */
    public static Location getCurrentLocation() {
	return mCurrentLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
	if (location != null) {
	    mCurrentLocation = location; // current location updated
	    Log.i(TAG, "Latitude: " + mCurrentLocation.getLatitude()
		    + " Longitude: " + mCurrentLocation.getLongitude());
	}
    }

    @Override
    public void onProviderDisabled(String provider) {
	Log.w(TAG, "Location Provider Disabled");
    }

    @Override
    public void onProviderEnabled(String provider) {
	Log.w(TAG, "Location Provider Enabled");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
	Log.w(TAG, "Location Status Changed");
    }
}
