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
import com.lays.decisong.models.Player;

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

    private ArrayList<Player> mPlayers;
    private PlayersAdapter mAdapter;

    private OnEditorActionListener mListener = new OnEditorActionListener() {
	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
	    if (actionId == EditorInfo.IME_ACTION_DONE) {
		// check that text is valid
		String temp = mInput.getText().toString();
		if (temp.length() > 1) {
		    mPlayers.add(Player.create(temp));
		    mAdapter.notifyDataSetChanged();
		    mInput.setText("");
		    return true;
		} else {
		    Toast.makeText(mContext, "Invalid Player Name", Toast.LENGTH_SHORT).show();
		}
	    }
	    return false;
	}
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_input);
	mContext = this;
	mInput = (AutoCompleteTextView) findViewById(R.id.auto_complete_player_input);
	mInput.setOnEditorActionListener(mListener);

	mPlayers = new ArrayList<Player>();
	mAdapter = new PlayersAdapter(this, mPlayers);
	setListAdapter(mAdapter);
    }

    @Override
    public void onBackPressed() {
	super.onBackPressed();
	overridePendingTransition(R.anim.slide_down_incoming, R.anim.slide_down_outgoing);
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
	    Toast.makeText(mContext, "At least 2 players needed to start game", Toast.LENGTH_SHORT).show();
	    return;
	}

	String[] players = new String[mPlayers.size()];
	for (int i = 0; i < mPlayers.size(); i++) {
	    players[i] = mPlayers.get(i).name;
	}
	
	Bundle extras = new Bundle();
	extras.putStringArray(DecisongApplication.PLAYERS_KEY, players);
	Intent intent = new Intent(this, GameActivity.class);
	intent.putExtras(extras);
	startActivity(intent);
	overridePendingTransition(R.anim.slide_up_incoming, R.anim.slide_up_outgoing);
    }

    @Override
    public void onClick(View v) {
	switch (v.getId()) {
	case R.id.row_player_cancel:
	    int positionToBeRemoved = ((Integer) v.getTag()).intValue();
	    Player player = mAdapter.getItem(positionToBeRemoved);
	    mAdapter.remove(player);
	    break;
	}
    }
}
