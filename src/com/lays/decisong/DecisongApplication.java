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

public class DecisongApplication extends Application implements RdioListener, LocationListener {

	private static final String TAG = DecisongApplication.class.getSimpleName();
	public static final boolean DEVELOPER_MODE = true;

	public static final String PLAYERS_KEY = "com.lays.decisong.activities.Players";
	public static final String RDIO_KEY = "rdio.existence";
	public static final String RDIO_API_KEY = "pqqa7fv8egyhz6dskr8sc2uh";
	public static final String RDIO_SECRET_KEY = "Nqp6nvBj94";
	public static final String EXIT_INTENT_ACTION = "com.lays.decisong.ACTION_EXIT";

	private static DecisongApplication mInstance;
	private Location mCurrentLocation;
	private static Rdio mRdio;
	private GameActivity mCurrentGame;

	public void onCreate() {
		if (DEVELOPER_MODE) {
			StrictMode.enableDefaults();
		}
		super.onCreate();
		mInstance = this;
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 25.0f, this);
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
	 * Method to check if Internet connection is available
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

	public Rdio setupGame(GameActivity activity) {

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		Editor pref = sharedPreferences.edit();

		if (mRdio == null) {
			// setting up for first time
			mRdio = new Rdio(DecisongApplication.RDIO_API_KEY,DecisongApplication.RDIO_SECRET_KEY, null, null, this, this);
			pref.putBoolean(RDIO_KEY, false);
			pref.commit();
		} else {
			// preparing game for subsequent times
			pref.putBoolean(RDIO_KEY, true);
			pref.commit();
		}
		mCurrentGame = activity;
		return mRdio;
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
	
	public Location getCurrentLocation() {
		return mCurrentLocation;
	}

	@Override
	public void onLocationChanged(Location location) {
		if (location != null) {
			mCurrentLocation = location;
			Log.i(TAG, "Latitude: " + mCurrentLocation.getLatitude() + " Longitude: " + mCurrentLocation.getLongitude());
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	
	}

	@Override
	public void onProviderEnabled(String provider) {
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}
}
