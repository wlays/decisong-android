package com.lays.decisong.adapters;

import java.util.ArrayList;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lays.decisong.R;
import com.lays.decisong.activities.InputActivity;
import com.lays.decisong.holders.PlayerViewHolder;

public class PlayersAdapter extends ArrayAdapter<String> {

    /** Associated InputActivity */
    private InputActivity mActivity;

    /** XML layout inflater */
    private static LayoutInflater mInflater;

    /** List of our mArticles objects */
    private ArrayList<String> mPlayers;

    public PlayersAdapter(InputActivity activity, ArrayList<String> players) {
	super(activity, R.layout.list_row_player, players);
	mActivity = activity;
	mInflater = activity.getLayoutInflater();
	mPlayers = players;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
	View row = convertView;

	if (row == null) {
	    row = mInflater.inflate(R.layout.list_row_player, parent, false);
	}

	PlayerViewHolder holder = (PlayerViewHolder) row.getTag();

	if (holder == null) {
	    holder = new PlayerViewHolder(row);
	    row.setTag(holder);
	}

	holder.getName().setText(mPlayers.get(position));
	holder.getCancel().setOnClickListener(mActivity);
	holder.getCancel().setTag(position);
	return row;
    }
}
