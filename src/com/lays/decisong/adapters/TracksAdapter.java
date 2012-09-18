package com.lays.decisong.adapters;

import java.util.ArrayList;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.lays.decisong.R;
import com.lays.decisong.activities.GameActivity;
import com.lays.decisong.holders.TrackViewHolder;
import com.lays.decisong.models.Track;

public class TracksAdapter extends ArrayAdapter<Track> {

    /** XML layout inflater */
    private static LayoutInflater mInflater;

    /** List of our mArticles objects */
    private ArrayList<Track> mTracks;

    public TracksAdapter(GameActivity activity, ArrayList<Track> tracks) {
	super(activity, R.layout.list_row_player, tracks);
	mInflater = activity.getLayoutInflater();
	mTracks = tracks;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
	View row = convertView;

	if (row == null) {
	    row = mInflater.inflate(R.layout.list_row_track, parent, false);
	}

	TrackViewHolder holder = (TrackViewHolder) row.getTag();

	if (holder == null) {
	    holder = new TrackViewHolder(row);
	    row.setTag(holder);
	}

	Track track = mTracks.get(position);
	holder.getAlbumArt().setImageUrl(track.getAlbumArt());
	holder.getAlbumArtist().setText("Artist: " + track.getArtistName());
	holder.getAlbumName().setText("Album: " + track.getAlbumName());
	return row;
    }
}
