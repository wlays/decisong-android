package com.lays.decisong.activities;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.lays.decisong.DecisongApplication;
import com.lays.decisong.R;
import com.lays.decisong.adapters.PlayersAdapter;

/**
 * Activity controls the actual game user plays
 * 
 * @author wlays
 * 
 */
public class InputActivity extends ListActivity implements OnClickListener {

	/** Activity tag */
	private static final String TAG = InputActivity.class.getSimpleName();

	private Context mContext;
	private AutoCompleteTextView mInput;

	private ArrayList<String> mPlayers;
	private PlayersAdapter mAdapter;

	private OnEditorActionListener mListener = new OnEditorActionListener() {
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				// check that text is valid
				String temp = mInput.getText().toString();
				if (temp.length() > 1) {
					mPlayers.add(temp);
					mAdapter.notifyDataSetChanged();
					mInput.setText("");
					return true;
				} else {
					Toast.makeText(mContext, "Invalid Player Name",
							Toast.LENGTH_SHORT).show();
				}
			}
			return false;
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_input);
		mContext = this;
		mInput = (AutoCompleteTextView) findViewById(R.id.auto_complete_player_input);
		mInput.setOnEditorActionListener(mListener);

		mPlayers = new ArrayList<String>();
		mAdapter = new PlayersAdapter(this, mPlayers);
		setListAdapter(mAdapter);
	}

	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.slide_down_incoming,
				R.anim.slide_down_outgoing);
	}

	/**
	 * onClick handler for done button
	 * 
	 * @param v
	 */
	public void startGame(View v) {
		// check if there's more than one player
		if (mPlayers.size() < 2) {
			Log.i(TAG, "Only 1 player");
			Toast.makeText(mContext, "At least 2 players needed to start game",
					Toast.LENGTH_SHORT).show();
			return;
		}

		Intent intent = new Intent(this, GameActivity.class);
		intent.putStringArrayListExtra(DecisongApplication.PLAYERS_KEY, mPlayers);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_up_incoming, R.anim.slide_up_outgoing);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.row_player_cancel:
			int positionToBeRemoved = ((Integer) v.getTag()).intValue();
			mAdapter.remove(mAdapter.getItem(positionToBeRemoved));
			break;
		}
	}
}
