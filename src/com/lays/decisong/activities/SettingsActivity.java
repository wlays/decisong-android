package com.lays.decisong.activities;

import android.app.Activity;
import android.os.Bundle;

import com.lays.decisong.R;

/**
 * Activity allows users to change game settings.
 * 
 * @author wlays
 * 
 */
public class SettingsActivity extends Activity {

    /** Activity tag */
    private static final String TAG = SettingsActivity.class.getSimpleName();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_settings);
    }
    
    @Override
    public void onBackPressed() {
	super.onBackPressed();
	overridePendingTransition(R.anim.slide_right_incoming, R.anim.slide_right_outgoing);
    }
}
