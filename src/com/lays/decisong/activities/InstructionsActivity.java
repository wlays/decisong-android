package com.lays.decisong.activities;

import android.app.Activity;
import android.os.Bundle;

import com.lays.decisong.R;

/**
 * Activity gives user information on how to play the game.
 * 
 * @author wlays
 * 
 */
public class InstructionsActivity extends Activity {

	/** Activity tag */
	private static final String TAG = InstructionsActivity.class
			.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_instructions);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.slide_left_incoming,
				R.anim.slide_left_outgoing);
	}
}
