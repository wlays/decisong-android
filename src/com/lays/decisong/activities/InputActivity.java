package com.lays.decisong.activities;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.lays.decisong.DecisongApplication;
import com.lays.decisong.R;
import com.lays.decisong.adapters.PlayersAdapter;

/**
 * Activity that lets user input players of game.
 * 
 * @author wlays
 * 
 */
public class InputActivity extends ListActivity implements OnClickListener {

    /** Activity tag */
    private static final String TAG = InputActivity.class.getSimpleName();

    /** Views */
    private ListView mListView;
    private AutoCompleteTextView mInput;

    /** List variables */
    private ArrayList<String> mPlayers;
    private PlayersAdapter mAdapter;

    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_input);
	mListView = getListView();
	mInput = (AutoCompleteTextView) findViewById(R.id.auto_complete_player_input);
	mInput.setOnEditorActionListener(new OnEditorActionListener() {
	    public boolean onEditorAction(TextView v, int actionId,
		    KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE) {
		    // check that text is valid
		    String temp = mInput.getText().toString();
		    if (temp.length() > 1) {
			mPlayers.add(temp);
			mAdapter.notifyDataSetChanged();
			mListView.setSelection(mAdapter.getCount());
			mInput.setText("");
			return true;
		    } else {
			Toast.makeText(InputActivity.this,
				"Invalid Player Name", Toast.LENGTH_SHORT)
				.show();
		    }
		}
		return false;
	    }
	});

	mPlayers = new ArrayList<String>();
	mAdapter = new PlayersAdapter(this, mPlayers);
	setListAdapter(mAdapter);
    }

    public void onBackPressed() {
	super.onBackPressed();
	overridePendingTransition(R.anim.slide_right_incoming,
		R.anim.slide_right_outgoing);
    }

    /**
     * onClick handler for done button
     * 
     * @param v
     */
    public void startGame(View v) {
	// if there's more than one player: continue game
	if (mPlayers.size() < 2) {
	    Log.i(TAG, "Only 1 player");
	    Toast.makeText(this, "At least 2 players needed to start game",
		    Toast.LENGTH_SHORT).show();
	    return;
	}

	// hide soft keyboard
	InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
	imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);

	if (DecisongApplication.getInstance().isOnline()) {
	    // wireless connection is available: start game
	    finish();
	    Intent intent = new Intent(this, GameActivity.class);
	    intent.putStringArrayListExtra(DecisongApplication.PLAYERS_KEY,
		    mPlayers);
	    startActivity(intent);
	    overridePendingTransition(R.anim.slide_left_incoming,
		    R.anim.slide_left_outgoing);
	} else {
	    // wireless connection is unavailable: tell user
	    new AlertDialog.Builder(this)
		    .setIcon(R.drawable.ic_dialog_alert)
		    .setTitle(R.string.connectivity_dialog_title)
		    .setMessage(R.string.connectivity_dialog_text)
		    .setPositiveButton(R.string.connectivity_dialog_settings,
			    new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
					int whichButton) {
				    // wireless settings
				    startActivity(new Intent(
					    Settings.ACTION_WIFI_SETTINGS));
				}
			    })
		    .setNegativeButton(R.string.connectivity_dialog_exit,
			    new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
					int whichButton) {
				    // exit input activity
				    finish();
				}
			    }).create().show();
	}
    }

    /**
     * Removes a player with the cross on him/her is clicked upon
     */
    public void onClick(View v) {
	switch (v.getId()) {
	case R.id.row_player_cancel:
	    int positionToBeRemoved = ((Integer) v.getTag()).intValue();
	    mAdapter.remove(mAdapter.getItem(positionToBeRemoved));
	    break;
	}
    }
}
