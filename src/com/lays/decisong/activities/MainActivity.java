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

	/** Activity tag */
	private static final String TAG = MainActivity.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	/**
	 * onClick handler for instruction button
	 * 
	 * @param v
	 */
	public void startInstructionsActivity(View v) {
		startActivity(new Intent(this, InstructionsActivity.class));
		overridePendingTransition(R.anim.slide_right_incoming,
				R.anim.slide_right_outgoing);
	}

	/**
	 * onClick handler for start new game button
	 * 
	 * @param v
	 */
	public void startInputActivity(View v) {
		startActivity(new Intent(this, InputActivity.class));
		overridePendingTransition(R.anim.slide_up_incoming,
				R.anim.slide_up_outgoing);
	}

	/**
	 * onClick handler for settings button
	 * 
	 * @param v
	 */
	public void startSettingsActivity(View v) {
		startActivity(new Intent(this, SettingsActivity.class));
		overridePendingTransition(R.anim.slide_left_incoming,
				R.anim.slide_left_outgoing);
	}
}
