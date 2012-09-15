package com.lays.decisong.holders;

import android.view.View;
import android.widget.TextView;

import com.lays.decisong.R;
import com.loopj.android.image.SmartImageView;

public class TrackViewHolder {

    private SmartImageView albumArt;
    private TextView albumArtist;
    private TextView albumName;

    public TrackViewHolder(View base) {
	setAlbumArt((SmartImageView) base.findViewById(R.id.row_album_art));
	setAlbumArtist((TextView) base.findViewById(R.id.row_album_artist));
	setAlbumName((TextView) base.findViewById(R.id.row_album_name));
    }

    public SmartImageView getAlbumArt() {
	return albumArt;
    }

    public void setAlbumArt(SmartImageView albumArt) {
	this.albumArt = albumArt;
    }

    public TextView getAlbumArtist() {
	return albumArtist;
    }

    public void setAlbumArtist(TextView albumArtist) {
	this.albumArtist = albumArtist;
    }

    public TextView getAlbumName() {
	return albumName;
    }

    public void setAlbumName(TextView albumName) {
	this.albumName = albumName;
    }
}
