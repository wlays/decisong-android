package com.lays.decisong.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.lays.decisong.R;

/**
 * Activity shows user the main menu of the game.
 * 
 * @author wlays
 * 
 */
public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
    }

    /**
     * onClick handler for instruction button.
     * 
     * @param v
     */
    public void startInstructionsActivity(View v) {
	Intent intent = new Intent(this, InstructionsActivity.class);
	intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
	startActivity(intent);
	overridePendingTransition(R.anim.slide_right_incoming,
		R.anim.slide_right_outgoing);
    }

    /**
     * onClick handler for start new game button.
     * 
     * @param v
     */
    public void startInputActivity(View v) {
	Intent intent = new Intent(this, InputActivity.class);
	intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
	startActivity(intent);
	overridePendingTransition(R.anim.slide_left_incoming,
		R.anim.slide_left_outgoing);
    }

    /**
     * onClick handler for settings button in the future.
     * 
     * @param v
     */
    public void startSettingsActivity(View v) {

	// if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
	// startActivity(new Intent(this, SettingsActivity.class));
	// } else {
	// startActivity(new Intent(this, SettingsNewActivity.class));
	// }
	// overridePendingTransition(R.anim.slide_left_incoming,
	// R.anim.slide_left_outgoing);
    }
}
